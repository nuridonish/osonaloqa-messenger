import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import type { Db } from '../db.js';
import { now } from '../db.js';
import { authenticate } from '../plugins/auth.js';
import { hub } from '../ws-hub.js';
import { sharedUserIds } from '../ws.js';

const PatchAvatar = z.object({ avatar_url: z.string().max(300).nullable() });

export function accountRoutes(app: FastifyInstance, db: Db): void {
  // Рӯйхати корбарон (барои гурӯҳ/рассылка дар реҷаи dev)
  app.get('/v1/users/list', { preHandler: [authenticate] }, async (req) => {
    const rows = db
      .prepare('SELECT id, phone, name, about, username, avatar_url, last_seen_at FROM users WHERE id != ? ORDER BY name')
      .all(req.user!.id);
    return {
      users: (rows as any[]).map((u) => ({ ...u, online: hub.isOnline(u.id) })),
    };
  });

  // Аватар
  app.patch('/v1/users/avatar', { preHandler: [authenticate] }, async (req, reply) => {
    const p = PatchAvatar.safeParse(req.body);
    if (!p.success) return reply.code(400).send({ error: 'bad_request' });
    db.prepare('UPDATE users SET avatar_url = ? WHERE id = ?').run(p.data.avatar_url, req.user!.id);
    hub.broadcastToUsers(sharedUserIds(db, req.user!.id), { type: 'chats.updated' });
    return { ok: true, avatar_url: p.data.avatar_url };
  });

  // Дастгоҳҳои пайваст (сессияҳо)
  app.get('/v1/devices', { preHandler: [authenticate] }, async (req) => {
    const rows = db
      .prepare(
        `SELECT id, device_name, created_at,
                (token_hash = (SELECT token_hash FROM refresh_tokens WHERE user_id = ? ORDER BY created_at DESC LIMIT 1)) AS current
         FROM refresh_tokens WHERE user_id = ? AND revoked = 0 ORDER BY created_at DESC`
      )
      .all(req.user!.id, req.user!.id);
    return { devices: rows };
  });

  app.delete('/v1/devices/:id', { preHandler: [authenticate] }, async (req, reply) => {
    const { id } = req.params as { id: string };
    const r = db.prepare('UPDATE refresh_tokens SET revoked = 1 WHERE id = ? AND user_id = ?').run(id, req.user!.id);
    if (r.changes === 0) return reply.code(404).send({ error: 'not_found' });
    return { ok: true, at: now() };
  });
}
