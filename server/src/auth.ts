import jwt from 'jsonwebtoken';
import { createHash, randomBytes, randomInt, timingSafeEqual } from 'node:crypto';
import type { Db } from './db.js';
import { now, uuid } from './db.js';
import { config, isDev } from './config.js';

export interface AccessPayload { sub: string; phone: string }

export function signAccessToken(u: { id: string; phone: string }): string {
  return jwt.sign({ sub: u.id, phone: u.phone }, config.jwtSecret, { expiresIn: config.accessTtlSec });
}

export function verifyAccessToken(token: string): AccessPayload | null {
  try {
    const p = jwt.verify(token, config.jwtSecret) as any;
    return { sub: String(p.sub), phone: String(p.phone) };
  } catch {
    return null;
  }
}

const sha256 = (s: string) => createHash('sha256').update(s).digest('hex');

export function issueRefreshToken(db: Db, userId: string, deviceName: string): string {
  const token = randomBytes(32).toString('hex');
  db.prepare(
    'INSERT INTO refresh_tokens (id, user_id, token_hash, device_name, created_at) VALUES (?,?,?,?,?)'
  ).run(uuid(), userId, sha256(token), deviceName ?? '', now());
  return token;
}

export function rotateRefreshToken(db: Db, token: string): { userId: string; refresh: string } | null {
  const row = db
    .prepare('SELECT id, user_id FROM refresh_tokens WHERE token_hash = ? AND revoked = 0')
    .get(sha256(token)) as { id: string; user_id: string } | undefined;
  if (!row) return null;
  db.prepare('UPDATE refresh_tokens SET revoked = 1 WHERE id = ?').run(row.id);
  const user = db.prepare('SELECT id, phone FROM users WHERE id = ?').get(row.user_id) as
    | { id: string; phone: string }
    | undefined;
  if (!user) return null;
  return { userId: user.id, refresh: issueRefreshToken(db, user.id, '') };
}

export function revokeRefreshToken(db: Db, token: string): void {
  db.prepare('UPDATE refresh_tokens SET revoked = 1 WHERE token_hash = ?').run(sha256(token));
}

export function revokeAllUserTokens(db: Db, userId: string): void {
  db.prepare('UPDATE refresh_tokens SET revoked = 1 WHERE user_id = ?').run(userId);
}

// ---------- OTP ----------

const OTP_TTL = 5 * 60 * 1000;
const OTP_RESEND_COOLDOWN = 60 * 1000;
const OTP_MAX_ATTEMPTS = 5;

export interface OtpResult { sent: boolean; devCode?: string; error?: string }

export function requestOtp(db: Db, phone: string): OtpResult {
  const last = db
    .prepare('SELECT created_at FROM otp_requests WHERE phone = ? ORDER BY created_at DESC LIMIT 1')
    .get(phone) as { created_at: number } | undefined;
  if (last && now() - last.created_at < OTP_RESEND_COOLDOWN) {
    return { sent: false, error: 'cooldown' };
  }
  const code = isDev() ? '000000' : String(randomInt(0, 1_000_000)).padStart(6, '0');
  db.prepare(
    'INSERT INTO otp_requests (id, phone, code_hash, expires_at, created_at) VALUES (?,?,?,?,?)'
  ).run(uuid(), phone, sha256(code), now() + OTP_TTL, now());
  // TODO(production): ин ҷо SMS-провайдер (adapter) даъват мешавад. Дар dev танҳо лог:
  if (isDev()) console.log(`[OTP] ${phone} -> ${code}`);
  else console.log(`[OTP] ${phone} -> ***`);
  return { sent: true, ...(isDev() ? { devCode: code } : {}) };
}

export function verifyOtp(db: Db, phone: string, code: string): boolean {
  const row = db
    .prepare('SELECT id, code_hash, expires_at, attempts FROM otp_requests WHERE phone = ? ORDER BY created_at DESC LIMIT 1')
    .get(phone) as { id: string; code_hash: string; expires_at: number; attempts: number } | undefined;
  if (!row) return false;
  if (now() > row.expires_at) return false;
  if (row.attempts >= OTP_MAX_ATTEMPTS) return false;
  const ok = safeEqual(row.code_hash, sha256(code));
  if (ok) {
    db.prepare('DELETE FROM otp_requests WHERE phone = ?').run(phone);
  } else {
    db.prepare('UPDATE otp_requests SET attempts = attempts + 1 WHERE id = ?').run(row.id);
  }
  return ok;
}

function safeEqual(a: string, b: string): boolean {
  const ba = Buffer.from(a);
  const bb = Buffer.from(b);
  return ba.length === bb.length && timingSafeEqual(ba, bb);
}
