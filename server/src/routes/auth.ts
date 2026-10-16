import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import type { Db } from '../db.js';
import { now, uuid } from '../db.js';
import { isDev } from '../config.js';
import { requestOtp, verifyOtp, signAccessToken, issueRefreshToken, rotateRefreshToken } from '../auth.js';
import { toE164, phoneMeta } from '../phone.js';

const BodyRequest = z.object({
  phone: z.string().min(6).max(20),
  defaultCountry: z.string().length(2).optional(),
});

const BodyVerify = BodyRequest.extend({
  code: z.string().regex(/^\d{6}$/),
  deviceName: z.string().max(64).optional(),
});

export function upsertUserByPhone(db: Db, e164: string): { id: string; isNew: boolean } {
  const existing = db.prepare('SELECT id FROM users WHERE phone = ?').get(e164) as { id: string } | undefined;
  if (existing) return { id: existing.id, isNew: false };
  const id = uuid();
  db.prepare('INSERT INTO users (id, phone, created_at, last_seen_at) VALUES (?,?,?,?)').run(id, e164, now(), now());
  return { id, isNew: true };
}

export function authRoutes(app: FastifyInstance, db: Db): void {
  app.post('/v1/auth/request-otp', async (req, reply) => {
    const p = BodyRequest.safeParse(req.body);
    if (!p.success) return reply.code(400).send({ error: 'bad_request', details: p.error.flatten() });
    const e164 = toE164(p.data.phone, p.data.defaultCountry);
    if (!e164) return reply.code(400).send({ error: 'invalid_phone' });
    const res = requestOtp(db, e164);
    if (!res.sent) return reply.code(429).send({ error: res.error });
    return {
      ok: true,
      phone: e164,
      meta: phoneMeta(e164),
      // OTP ҳеҷ гоҳ дар production барнамегардад:
      ...(!isDev() ? {} : { devCode: res.devCode }),
    };
  });

  app.post('/v1/auth/verify', async (req, reply) => {
    const p = BodyVerify.safeParse(req.body);
    if (!p.success) return reply.code(400).send({ error: 'bad_request', details: p.error.flatten() });
    const e164 = toE164(p.data.phone, p.data.defaultCountry);
    if (!e164) return reply.code(400).send({ error: 'invalid_phone' });
    if (!verifyOtp(db, e164, p.data.code)) return reply.code(401).send({ error: 'invalid_code' });
    const { id, isNew } = upsertUserByPhone(db, e164);
    const user = db.prepare('SELECT id, phone, name, about, username, avatar_url FROM users WHERE id = ?').get(id);
    const accessToken = signAccessToken({ id, phone: e164 });
    const refreshToken = issueRefreshToken(db, id, p.data.deviceName ?? '');
    return { accessToken, refreshToken, isNew, user };
  });

  app.post('/v1/auth/refresh', async (req, reply) => {
    const refreshToken = String((req.body as any)?.refreshToken ?? '');
    if (!refreshToken) return reply.code(400).send({ error: 'bad_request' });
    const rotated = rotateRefreshToken(db, refreshToken);
    if (!rotated) return reply.code(401).send({ error: 'invalid_refresh' });
    const user = db.prepare('SELECT id, phone, name, about, username, avatar_url FROM users WHERE id = ?').get(rotated.userId);
    return { accessToken: signAccessToken(user as any), refreshToken: rotated.refresh, user };
  });
}
