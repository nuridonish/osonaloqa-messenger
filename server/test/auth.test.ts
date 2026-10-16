import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { buildApp } from '../src/app.js';

let app: any;

async function authByPhone(phone: string) {
  const r1 = await app.inject({ method: 'POST', url: '/v1/auth/request-otp', payload: { phone } });
  expect(r1.statusCode).toBe(200);
  const code = r1.json().devCode;
  const r2 = await app.inject({
    method: 'POST',
    url: '/v1/auth/verify',
    payload: { phone, code, deviceName: 'test' },
  });
  expect(r2.statusCode).toBe(200);
  return r2.json();
}

beforeAll(async () => {
  ({ app } = await buildApp({ dbFile: ':memory:' }));
});
afterAll(async () => {
  await app.close();
});

describe('Auth: телефон + OTP (реҷаи dev)', () => {
  it('request-otp дар dev коди 000000 медиҳад', async () => {
    const r = await app.inject({ method: 'POST', url: '/v1/auth/request-otp', payload: { phone: '+992901110011' } });
    expect(r.statusCode).toBe(200);
    expect(r.json().devCode).toBe('000000');
  });
  it('cooldown: такрори фаврӣ 429 медиҳад', async () => {
    const r = await app.inject({ method: 'POST', url: '/v1/auth/request-otp', payload: { phone: '+998901110022' } });
    expect(r.statusCode).toBe(200);
    const r2 = await app.inject({ method: 'POST', url: '/v1/auth/request-otp', payload: { phone: '+998901110022' } });
    expect(r2.statusCode).toBe(429);
  });
  it('verify нодуруст → 401', async () => {
    const r = await app.inject({ method: 'POST', url: '/v1/auth/verify', payload: { phone: '+79011110033', code: '111111' } });
    expect(r.statusCode).toBe(401);
  });
  it('verify дуруст → токен + корбар', async () => {
    const a = await authByPhone('+989121110044');
    expect(a.accessToken).toBeTruthy();
    expect(a.isNew).toBe(true);
    expect(a.user.phone).toBe('+989121110044');
  });
  it('me бо Bearer кор мекунад, бе Bearer → 401', async () => {
    const a = await authByPhone('+491511110055');
    const ok = await app.inject({ method: 'GET', url: '/v1/users/me', headers: { authorization: `Bearer ${a.accessToken}` } });
    expect(ok.statusCode).toBe(200);
    expect(ok.json().user.phone).toBe('+491511110055');
    const bad = await app.inject({ method: 'GET', url: '/v1/users/me' });
    expect(bad.statusCode).toBe(401);
  });
  it('refresh токен кор мекунад (ротатсия)', async () => {
    const a = await authByPhone('+14155551066');
    const r = await app.inject({ method: 'POST', url: '/v1/auth/refresh', payload: { refreshToken: a.refreshToken } });
    expect(r.statusCode).toBe(200);
    expect(r.json().accessToken).toBeTruthy();
    const again = await app.inject({ method: 'POST', url: '/v1/auth/refresh', payload: { refreshToken: a.refreshToken } });
    expect(again.statusCode).toBe(401);
  });
});
