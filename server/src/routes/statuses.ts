import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import type { Db } from '../db.js';
import { now, uuid } from '../db.js';
import { authenticate } from '../plugins/auth.js';

const DAY = 24 * 60 * 60 * 1000;

const CreateStatus = z.object({
  type: z.enum(['text', 'image', 'video']).default('text'),
  body: z.string().min(1).max(4096),
  bg: z.string().max(16).optional(),
});

export function statusRoutes(app: FastifyInstance, db: Db): void {
  app.post('/v1/statuses', { preHandler: [authenticate] }, async (req, reply) => {
    const p = CreateStatus.safeParse(req.body);
    if (!p.success) return reply.code(400).send({ error: 'bad_request', details: p.error.flatten() });
    const id = uuid();
    const t = now();
    db.prepare('INSERT INTO statuses (id, user_id, type, body, created_at, expires_at) VALUES (?,?,?,?,?,?)').run(
      id, req.user!.id, p.data.type, p.data.body, t, t + DAY
    );
    return { statusId: id, expiresAt: t + DAY };
  });

  // Статусҳои фаъоли ҳама (24 соат)
  app.get('/v1/statuses', { preHandler: [authenticate] }, async (req) => {
    const t = now();
    const rows = db
      .prepare(
        `SELECT s.id, s.user_id, s.type, s.body, s.bg, s.created_at, s.expires_at,
                u.name AS author_name, u.phone AS author_phone, u.avatar_url AS author_avatar
         FROM statuses s JOIN users u ON u.id = s.user_id
         WHERE s.expires_at > ? ORDER BY s.created_at DESC`
      )
      .all(t);
    return { statuses: rows, serverTime: t };
  });

  app.delete('/v1/statuses/:id', { preHandler: [authenticate] }, async (req, reply) => {
    const { id } = req.params as { id: string };
    const r = db.prepare('DELETE FROM statuses WHERE id = ? AND user_id = ?').run(id, req.user!.id);
    if (r.changes === 0) return reply.code(404).send({ error: 'not_found' });
    return { ok: true };
  });
}
