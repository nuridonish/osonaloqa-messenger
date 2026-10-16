import type { FastifyInstance } from 'fastify';
import type { WebSocket } from 'ws';
import type { Db } from './db.js';
import { now, uuid } from './db.js';
import { verifyAccessToken } from './auth.js';
import { hub, type Conn } from './ws-hub.js';

const AUTH_TIMEOUT = 10_000;

interface SendMsg {
  type: 'message.send';
  clientId: string;
  chatId: string;
  msgType?: 'text' | 'image' | 'video' | 'audio' | 'voice' | 'document' | 'location' | 'contact' | 'system';
  body: string;
}
interface AckMsg { type: 'ack'; messageId: string; state: 'delivered' | 'read' }
interface TypingMsg { type: 'typing'; chatId: string; on: boolean }
interface PingMsg { type: 'ping' }
type ClientMsg = SendMsg | AckMsg | TypingMsg | PingMsg | { type: 'auth'; token?: string } | { type: 'error' };

export function wsRoutes(app: FastifyInstance, db: Db): void {
  app.get('/v1/ws', { websocket: true }, (socket: WebSocket, req) => {
    let conn: Conn | null = null;
    let userId: string | null = null;

    const authTimer = setTimeout(() => {
      if (!userId) socket.close(4001, 'auth_timeout');
    }, AUTH_TIMEOUT);

    const send = (payload: unknown) => {
      if (socket.readyState === socket.OPEN) socket.send(JSON.stringify(payload));
    };

    socket.on('message', (raw: Buffer) => {
      let msg: ClientMsg & { token?: string };
      try {
        msg = JSON.parse(raw.toString());
      } catch {
        return send({ type: 'error', error: 'bad_json' });
      }

      if (!userId) {
        if (msg.type !== 'auth') return send({ type: 'error', error: 'auth_required' });
        const payload = verifyAccessToken(String((msg as any).token ?? ''));
        if (!payload) return send({ type: 'error', error: 'invalid_token' });
        userId = payload.sub;
        conn = { socket, userId };
        clearTimeout(authTimer);
        const firstConn = !hub.isOnline(userId);
        hub.add(userId, conn);
        db.prepare('UPDATE users SET last_seen_at = ? WHERE id = ?').run(now(), userId);
        send({ type: 'ready', userId, online: hub.onlineUserIds().length });
        // Навбати офлайн: паёмҳое, ки ба ин корбар нарасидаанд, фавран фиристода мешаванд
        const missed = db
          .prepare(
            `SELECT m.* FROM messages m
             JOIN chat_members cm ON cm.chat_id = m.chat_id AND cm.user_id = ?
             LEFT JOIN message_states ms ON ms.message_id = m.id AND ms.user_id = ?
             WHERE m.sender_id != ? AND ms.message_id IS NULL
             ORDER BY m.created_at ASC LIMIT 200`
          )
          .all(userId, userId, userId) as any[];
        const markDelivered = db.prepare(
          'INSERT INTO message_states (message_id, user_id, delivered_at) VALUES (?,?,?) ON CONFLICT(message_id, user_id) DO UPDATE SET delivered_at = COALESCE(delivered_at, excluded.delivered_at)'
        );
        for (const m of missed) {
          send({
            type: 'message.new',
            message: {
              id: m.id,
              chatId: m.chat_id,
              senderId: m.sender_id,
              seq: m.seq,
              clientId: m.client_id,
              msgType: m.type,
              body: m.body,
              createdAt: m.created_at,
            },
          });
          markDelivered.run(m.id, userId, now());
        }
        if (firstConn) {
          hub.broadcastToUsers(sharedUserIds(db, userId), {
            type: 'presence',
            userId,
            online: true,
            lastSeenAt: 0,
          });
        }
        return;
      }

      switch (msg.type) {
        case 'message.send': {
          const { clientId, chatId } = msg;
          const msgType = msg.msgType ?? 'text';
          if (!clientId || !chatId) return send({ type: 'error', error: 'bad_request' });
          const member = db.prepare('SELECT 1 FROM chat_members WHERE chat_id = ? AND user_id = ?').get(chatId, userId);
          if (!member) return send({ type: 'error', error: 'not_a_member' });
          const dup = db
            .prepare('SELECT id FROM messages WHERE chat_id = ? AND sender_id = ? AND client_id = ?')
            .get(chatId, userId, clientId) as { id: string } | undefined;
          if (dup) return send({ type: 'ack.sent', clientId, messageId: dup.id, chatId, duplicate: true });

          const txn = db.transaction(() => {
            const seqRow = db.prepare('UPDATE chats SET last_seq = last_seq + 1 WHERE id = ? RETURNING last_seq').get(chatId) as { last_seq: number };
            const id = uuid();
            db.prepare(
              'INSERT INTO messages (id, chat_id, sender_id, seq, client_id, type, body, created_at) VALUES (?,?,?,?,?,?,?,?)'
            ).run(id, chatId, userId!, seqRow.last_seq, clientId, msgType, String(msg.body ?? ''), now());
            return { id, seq: seqRow.last_seq };
          });
          let res: { id: string; seq: number };
          try {
            res = txn();
          } catch {
            return send({ type: 'error', error: 'send_failed' });
          }

          send({ type: 'ack.sent', clientId, messageId: res.id, chatId, seq: res.seq });
          const event = {
            type: 'message.new' as const,
            message: {
              id: res.id,
              chatId,
              senderId: userId,
              seq: res.seq,
              clientId,
              msgType,
              body: String(msg.body ?? ''),
              createdAt: Date.now(),
            },
          };
          hub.broadcastToChat(db, chatId, event, userId);
          break;
        }

        case 'ack': {
          const { messageId, state } = msg;
          if (!messageId || !state) return send({ type: 'error', error: 'bad_request' });
          const m = db.prepare('SELECT id, chat_id, seq FROM messages WHERE id = ?').get(messageId) as
            | { id: string; chat_id: string; seq: number }
            | undefined;
          if (!m) return send({ type: 'error', error: 'not_found' });
          const t = now();
          if (state === 'delivered') {
            db.prepare(
              'INSERT INTO message_states (message_id, user_id, delivered_at) VALUES (?,?,?) ON CONFLICT(message_id, user_id) DO UPDATE SET delivered_at = COALESCE(delivered_at, excluded.delivered_at)'
            ).run(m.id, userId, t);
          } else {
            db.prepare(
              'INSERT INTO message_states (message_id, user_id, delivered_at, read_at) VALUES (?,?,?,?) ON CONFLICT(message_id, user_id) DO UPDATE SET read_at = excluded.read_at'
            ).run(m.id, userId, t, t);
            db.prepare('UPDATE chat_members SET last_read_seq = MAX(last_read_seq, ?) WHERE chat_id = ? AND user_id = ?').run(m.seq, m.chat_id, userId);
          }
          hub.broadcastToChat(db, m.chat_id, { type: 'ack.update', chatId: m.chat_id, messageId: m.id, state, userId });
          break;
        }

        case 'typing': {
          const { chatId, on } = msg;
          if (!chatId) return send({ type: 'error', error: 'bad_request' });
          const member = db.prepare('SELECT 1 FROM chat_members WHERE chat_id = ? AND user_id = ?').get(chatId, userId);
          if (!member) return;
          hub.broadcastToChat(db, chatId, { type: 'typing', chatId, userId, on: !!on }, userId);
          break;
        }

        case 'ping':
          send({ type: 'pong', t: Date.now() });
          break;
      }
    });

    socket.on('close', () => {
      clearTimeout(authTimer);
      if (!userId || !conn) return;
      const fullyOffline = hub.remove(userId, conn);
      if (fullyOffline) {
        const t = now();
        db.prepare('UPDATE users SET last_seen_at = ? WHERE id = ?').run(t, userId);
        hub.broadcastToUsers(sharedUserIds(db, userId), {
          type: 'presence',
          userId,
          online: false,
          lastSeenAt: t,
        });
      }
    });
  });
}

/** Ид-ҳои корбароне, ки бо `userId` ҳадди ақал як чати муштарак доранд */
export function sharedUserIds(db: Db, userId: string): string[] {
  const rows = db
    .prepare(
      `SELECT DISTINCT m2.user_id AS uid FROM chat_members m1
       JOIN chat_members m2 ON m2.chat_id = m1.chat_id
       WHERE m1.user_id = ? AND m2.user_id != ?`
    )
    .all(userId, userId) as { uid: string }[];
  return rows.map((r) => r.uid);
}
