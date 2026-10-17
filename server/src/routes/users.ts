import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import type { Db } from '../db.js';
import { now } from '../db.js';
import { authenticate } from '../plugins/auth.js';
import { toE164 } from '../phone.js';

const PatchMe = z.object({
  name: z.string().min(1).max(64).optional(),
  about: z.string().max(140).optional(),
  username: z.string().regex(/^[a-z0-9_]{5,24}$/).optional(),
  avatar_url: z.string().max(300).nullable().optional(),
});

export function userRoutes(app: FastifyInstance, db: Db): void {
  app.get('/v1/users/me', { preHandler: [authenticate] }, async (req) => {
    const user = db.prepare('SELECT id, phone, name, about, username, avatar_url, created_at FROM users WHERE id = ?').get(req.user!.id);
    return { user };
  });

  app.patch('/v1/users/me', { preHandler: [authenticate] }, async (req, reply) => {
    const p = PatchMe.safeParse(req.body);
    if (!p.success) return reply.code(400).send({ error: 'bad_request', details: p.error.flatten() });
    const { name, about, username, avatar_url } = p.data;
    if (username) {
      const taken = db.prepare('SELECT id FROM users WHERE username = ? AND id != ?').get(username, req.user!.id);
      if (taken) return reply.code(409).send({ error: 'username_taken' });
    }
    db.prepare(
      'UPDATE users SET name = COALESCE(?, name), about = COALESCE(?, about), username = COALESCE(?, username), avatar_url = COALESCE(?, avatar_url) WHERE id = ?'
    ).run(name ?? null, about ?? null, username ?? null, avatar_url === undefined ? null : avatar_url, req.user!.id);
    const user = db.prepare('SELECT id, phone, name, about, username, avatar_url FROM users WHERE id = ?').get(req.user!.id);
    return { user };
  });

  // Ҳамоҳангсозии контактҳо: рӯйхати рақамҳо (E.164) → корбарони бақайдгарфта
  app.post('/v1/users/lookup', { preHandler: [authenticate] }, async (req, reply) => {
    const phonesRaw: unknown = (req.body as any)?.phones;
    if (!Array.isArray(phonesRaw) || phonesRaw.length === 0 || phonesRaw.length > 1000) {
      return reply.code(400).send({ error: 'bad_request' });
    }
    const e164s = phonesRaw.map((p) => toE164(String(p))).filter((p): p is string => !!p);
    if (e164s.length === 0) return { users: [] };
    const rows = db
      .prepare(
        `SELECT id, phone, name, about, username, avatar_url FROM users WHERE phone IN (${e164s.map(() => '?').join(',')})`
      )
      .all(...e164s);
    return { users: rows };
  });

  app.post('/v1/presence/heartbeat', { preHandler: [authenticate] }, async (req) => {
    db.prepare('UPDATE users SET last_seen_at = ? WHERE id = ?').run(now(), req.user!.id);
    return { ok: true };
  });
}
