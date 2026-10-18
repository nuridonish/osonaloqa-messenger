import type { FastifyInstance } from 'fastify';
import type { Db } from '../db.js';
import { authenticate } from '../plugins/auth.js';
import { hub } from '../ws-hub.js';

export function searchRoutes(app: FastifyInstance, db: Db): void {
  // Ҷустуҷӯи корбарон: ном, @username ё рақам
  app.get('/v1/users/search', { preHandler: [authenticate] }, async (req) => {
    const q = String((req.query as any)?.q ?? '').trim();
    if (q.length < 1) return { users: [] };
    const like = `%${q.replace(/^@/, '').toLowerCase()}%`;
    const rows = db
      .prepare(
        `SELECT id, phone, name, about, username, avatar_url, last_seen_at FROM users
         WHERE id != ? AND (
           LOWER(name) LIKE ? OR
           LOWER(COALESCE(username, '')) LIKE ? OR
           phone LIKE ?
         )
         ORDER BY CASE WHEN LOWER(COALESCE(username,'')) = LOWER(?) THEN 0 ELSE 1 END, name
         LIMIT 50`
      )
      .all(req.user!.id, like, like, `%${q}%`, q.replace(/^@/, ''));
    return {
      users: (rows as any[]).map((u) => ({ ...u, online: hub.isOnline(u.id) })),
    };
  });
}
