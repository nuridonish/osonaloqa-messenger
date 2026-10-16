import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { buildApp } from '../src/app.js';

let app: any;
let tokenA: string;
let tokenB: string;
let idA: string;
let idB: string;

async function auth(phone: string) {
  await app.inject({ method: 'POST', url: '/v1/auth/request-otp', payload: { phone } });
  const r = await app.inject({ method: 'POST', url: '/v1/auth/verify', payload: { phone, code: '000000' } });
  return r.json();
}

beforeAll(async () => {
  ({ app } = await buildApp({ dbFile: ':memory:' }));
  const a = await auth('+992900000001');
  tokenA = a.accessToken;
  idA = a.user.id;
  const b = await auth('+992900000002');
  tokenB = b.accessToken;
  idB = b.user.id;
});
afterAll(async () => {
  await app.close();
});

const hA = () => ({ authorization: `Bearer ${tokenA}` });
const hB = () => ({ authorization: `Bearer ${tokenB}` });

describe('Чатҳо (REST)', () => {
  it('lookup: B-ро бо рақам меёбад', async () => {
    const r = await app.inject({
      method: 'POST',
      url: '/v1/users/lookup',
      headers: hA(),
      payload: { phones: ['+992900000002', '+799900000999'] },
    });
    expect(r.statusCode).toBe(200);
    const users = r.json().users;
    expect(users).toHaveLength(1);
    expect(users[0].id).toBe(idB);
  });

  it('чати шахсӣ сохта мешавад ва такрор нахоҳад шуд', async () => {
    const r1 = await app.inject({ method: 'POST', url: '/v1/chats', headers: hA(), payload: { type: 'private', peerId: idB } });
    expect(r1.statusCode).toBe(200);
    const chatId = r1.json().chatId;
    const r2 = await app.inject({ method: 'POST', url: '/v1/chats', headers: hB(), payload: { type: 'private', peerId: idA } });
    expect(r2.json().chatId).toBe(chatId);
  });

  it('рӯйхати чатҳо бо peer-и дуруст', async () => {
    const r = await app.inject({ method: 'GET', url: '/v1/chats', headers: hA() });
    expect(r.statusCode).toBe(200);
    const chats = r.json().chats;
    expect(chats).toHaveLength(1);
    expect(chats[0].peer.id).toBe(idB);
    expect(chats[0].unread).toBe(0);
  });

  it('гурӯҳ сохта мешавад', async () => {
    const r = await app.inject({
      method: 'POST',
      url: '/v1/chats',
      headers: hA(),
      payload: { type: 'group', title: 'Дӯстон', memberIds: [idB] },
    });
    expect(r.statusCode).toBe(200);
  });

  it('нааъзо ба чати дигарон дастрасӣ надорад', async () => {
    const gr = await app.inject({
      method: 'POST',
      url: '/v1/chats',
      headers: hA(),
      payload: { type: 'group', title: 'Пинҳонӣ' },
    });
    const secretChat = gr.json().chatId;
    const r = await app.inject({ method: 'GET', url: `/v1/chats/${secretChat}/messages`, headers: hB() });
    expect(r.statusCode).toBe(403);
  });

  it('username-и такрорӣ 409 медиҳад', async () => {
    await app.inject({ method: 'PATCH', url: '/v1/users/me', headers: hA(), payload: { username: 'oson_user' } });
    const r = await app.inject({ method: 'PATCH', url: '/v1/users/me', headers: hB(), payload: { username: 'oson_user' } });
    expect(r.statusCode).toBe(409);
  });
});
