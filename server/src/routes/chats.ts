import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import type { Db } from '../db.js';
import { now, uuid } from '../db.js';
import { authenticate } from '../plugins/auth.js';
import { hub } from '../ws-hub.js';

export interface ChatSummary {
  id: string;
  type: string;
  title: string;
  avatar_url: string | null;
  last_seq: number;
  unread: number;
  peer: null | { id: string; phone: string; name: string; about: string; username: string | null; avatar_url: string | null; online: boolean; last_seen_at: number };
  last_message: null | { id: string; sender_id: string; type: string; body: string; seq: number; created_at: number };
  updated_at: number;
}

export function createPrivateChat(db: Db, a: string, b: string): string {
  const existing = db
    .prepare(
      `SELECT c.id FROM chats c
       JOIN chat_members m1 ON m1.chat_id = c.id AND m1.user_id = ?
       JOIN chat_members m2 ON m2.chat_id = c.id AND m2.user_id = ?
       WHERE c.type = 'private' LIMIT 1`
    )
    .get(a, b) as { id: string } | undefined;
  if (existing) return existing.id;
  const id = uuid();
  const t = now();
  db.prepare("INSERT INTO chats (id, type, created_by, created_at) VALUES (?, 'private', ?, ?)").run(id, a, t);
  const ins = db.prepare('INSERT INTO chat_members (chat_id, user_id, role, joined_at) VALUES (?,?,?,?)');
  ins.run(id, a, 'member', t);
  ins.run(id, b, 'member', t);
  return id;
}

export function assertMember(db: Db, chatId: string, userId: string): boolean {
  return !!db.prepare('SELECT 1 FROM chat_members WHERE chat_id = ? AND user_id = ?').get(chatId, userId);
}

export function chatSummaries(db: Db, userId: string): ChatSummary[] {
  const chats = db
    .prepare(
      `SELECT c.*, m.last_read_seq FROM chats c JOIN chat_members m ON m.chat_id = c.id AND m.user_id = ?
       ORDER BY c.id`
    )
    .all(userId) as any[];
  const out: ChatSummary[] = [];
  const memberOf = db.prepare('SELECT user_id FROM chat_members WHERE chat_id = ?');
  for (const c of chats) {
    const peerId = c.type === 'private' ? (memberOf.all(c.id) as any[]).map((r) => r.user_id).find((u) => u !== userId) : undefined;
    const peer = peerId
      ? (db.prepare('SELECT id, phone, name, about, username, avatar_url, last_seen_at FROM users WHERE id = ?').get(peerId) as any)
      : null;
    const last = db.prepare('SELECT id, sender_id, type, body, seq, created_at FROM messages WHERE chat_id = ? ORDER BY seq DESC LIMIT 1').get(c.id) as any;
    const unread = db
      .prepare('SELECT COUNT(*) AS n FROM messages WHERE chat_id = ? AND seq > ? AND sender_id != ?')
      .get(c.id, c.last_read_seq, userId) as any;
    out.push({
      id: c.id,
      type: c.type,
      title: c.type === 'private' ? peer?.name || peer?.phone || '' : c.title,
      avatar_url: c.type === 'private' ? peer?.avatar_url ?? null : c.avatar_url,
      last_seq: c.last_seq,
      unread: unread.n,
      peer: peer
        ? {
            id: peer.id,
            phone: peer.phone,
            name: peer.name,
            about: peer.about,
            username: peer.username,
            avatar_url: peer.avatar_url,
            online: hub.isOnline(peer.id),
            last_seen_at: peer.last_seen_at,
          }
        : null,
      last_message: last ?? null,
      updated_at: last?.created_at ?? c.created_at,
    });
  }
  out.sort((x, y) => y.updated_at - x.updated_at);
  return out;
}

const CreateChat = z.object({
  type: z.enum(['private', 'group', 'channel']),
  peerId: z.string().optional(),
  title: z.string().max(64).optional(),
  memberIds: z.array(z.string()).max(1024).optional(),
});

export function chatRoutes(app: FastifyInstance, db: Db): void {
  app.get('/v1/chats', { preHandler: [authenticate] }, async (req) => {
    return { chats: chatSummaries(db, req.user!.id) };
  });

  app.post('/v1/chats', { preHandler: [authenticate] }, async (req, reply) => {
    const p = CreateChat.safeParse(req.body);
    if (!p.success) return reply.code(400).send({ error: 'bad_request', details: p.error.flatten() });
    const me = req.user!.id;
    if (p.data.type === 'private') {
      if (!p.data.peerId) return reply.code(400).send({ error: 'peerId_required' });
      if (!db.prepare('SELECT 1 FROM users WHERE id = ?').get(p.data.peerId)) return reply.code(404).send({ error: 'peer_not_found' });
      const id = createPrivateChat(db, me, p.data.peerId);
      return { chatId: id };
    }
    if (!p.data.title) return reply.code(400).send({ error: 'title_required' });
    const id = uuid();
    const t = now();
    db.prepare('INSERT INTO chats (id, type, title, created_by, created_at) VALUES (?,?,?,?,?)').run(id, p.data.type, p.data.title, me, t);
    const ins = db.prepare('INSERT OR IGNORE INTO chat_members (chat_id, user_id, role, joined_at) VALUES (?,?,?,?)');
    ins.run(id, me, 'admin', t);
    for (const m of p.data.memberIds ?? []) ins.run(id, m, 'member', t);
    hub.broadcastToUsers([me, ...(p.data.memberIds ?? [])], { type: 'chats.updated' });
    return { chatId: id };
  });

  app.get('/v1/chats/:chatId', { preHandler: [authenticate] }, async (req, reply) => {
    const { chatId } = req.params as { chatId: string };
    if (!assertMember(db, chatId, req.user!.id)) return reply.code(403).send({ error: 'not_a_member' });
    const all = chatSummaries(db, req.user!.id);
    const chat = all.find((c) => c.id === chatId);
    if (!chat) return reply.code(404).send({ error: 'not_found' });
    return { chat };
  });

  app.get('/v1/chats/:chatId/messages', { preHandler: [authenticate] }, async (req, reply) => {
    const { chatId } = req.params as { chatId: string };
    const q = req.query as { limit?: string; beforeSeq?: string };
    if (!assertMember(db, chatId, req.user!.id)) return reply.code(403).send({ error: 'not_a_member' });
    const limit = Math.min(Math.max(Number(q.limit ?? 50), 1), 200);
    const beforeSeq = q.beforeSeq ? Number(q.beforeSeq) : Number.MAX_SAFE_INTEGER;
    const msgs = db
      .prepare(
        'SELECT id, sender_id, seq, client_id, type, body, created_at FROM messages WHERE chat_id = ? AND seq < ? ORDER BY seq DESC LIMIT ?'
      )
      .all(chatId, beforeSeq, limit) as any[];
    return { messages: msgs.reverse(), hasMore: msgs.length === limit };
  });

  app.post('/v1/chats/:chatId/read', { preHandler: [authenticate] }, async (req, reply) => {
    const { chatId } = req.params as { chatId: string };
    const seq = Number((req.body as any)?.seq ?? 0);
    if (!assertMember(db, chatId, req.user!.id)) return reply.code(403).send({ error: 'not_a_member' });
    db.prepare('UPDATE chat_members SET last_read_seq = MAX(last_read_seq, ?) WHERE chat_id = ? AND user_id = ?').run(seq, chatId, req.user!.id);
    hub.broadcastToChat(db, chatId, {
      type: 'ack.update',
      chatId,
      userId: req.user!.id,
      lastReadSeq: seq,
    });
    return { ok: true };
  });
}
