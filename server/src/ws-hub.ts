import type { WebSocket } from 'ws';

export interface Conn {
  socket: WebSocket;
  userId: string;
}

/** Реҷистри пайвастшавиҳо ва паҳнкунии рӯйдодҳо (in-memory; дар production → Redis pub/sub) */
class Hub {
  private byUser = new Map<string, Set<Conn>>();

  add(userId: string, conn: Conn): void {
    let set = this.byUser.get(userId);
    if (!set) this.byUser.set(userId, (set = new Set()));
    set.add(conn);
  }

  remove(userId: string, conn: Conn): boolean {
    const set = this.byUser.get(userId);
    if (!set) return false;
    set.delete(conn);
    if (set.size === 0) {
      this.byUser.delete(userId);
      return true; // корбар пурра ҷудо шуд
    }
    return false;
  }

  isOnline(userId: string): boolean {
    return this.byUser.has(userId);
  }

  onlineUserIds(): string[] {
    return [...this.byUser.keys()];
  }

  sendToUser(userId: string, payload: unknown): boolean {
    const set = this.byUser.get(userId);
    if (!set || set.size === 0) return false;
    const data = JSON.stringify(payload);
    let sent = false;
    for (const c of set) {
      if (c.socket.readyState === c.socket.OPEN) {
        c.socket.send(data);
        sent = true;
      }
    }
    return sent;
  }

  broadcastToUsers(userIds: string[], payload: unknown): void {
    for (const id of new Set(userIds)) this.sendToUser(id, payload);
  }

  /** Ба ҳамаи аъзои чат фиристодан (бе `exceptUserId`) */
  broadcastToChat(db: any, chatId: string, payload: unknown, exceptUserId?: string): void {
    const members = db.prepare('SELECT user_id FROM chat_members WHERE chat_id = ?').all(chatId) as { user_id: string }[];
    for (const m of members) if (m.user_id !== exceptUserId) this.sendToUser(m.user_id, payload);
  }
}

export const hub = new Hub();
