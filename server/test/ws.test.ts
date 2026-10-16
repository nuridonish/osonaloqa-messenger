import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import WebSocket from 'ws';
import { buildApp } from '../src/app.js';

let app: any;
let baseUrl: string;
let tokenA: string;
let tokenB: string;
let idA: string;
let idB: string;
let chatId: string;

async function auth(phone: string) {
  await app.inject({ method: 'POST', url: '/v1/auth/request-otp', payload: { phone } });
  const r = await app.inject({ method: 'POST', url: '/v1/auth/verify', payload: { phone, code: '000000' } });
  return r.json();
}

function connect(token: string) {
  const ws = new WebSocket(baseUrl.replace('http', 'ws') + '/v1/ws');
  const inbox: any[] = [];
  const pending: { type?: string; resolve: (v: any) => void; timer: any }[] = [];

  const flush = () => {
    for (let i = 0; i < pending.length; i++) {
      const p = pending[i];
      const idx = p.type ? inbox.findIndex((m) => m.type === p.type) : inbox.length ? 0 : -1;
      if (idx >= 0) {
        const [msg] = inbox.splice(idx, 1);
        clearTimeout(p.timer);
        pending.splice(i, 1);
        i--;
        p.resolve(msg);
      }
    }
  };

  ws.on('message', (raw) => {
    inbox.push(JSON.parse(raw.toString()));
    flush();
  });

  const next = (type?: string, timeoutMs = 5000): Promise<any> =>
    new Promise((resolve, reject) => {
      const p: any = { type, resolve, timer: null };
      p.timer = setTimeout(() => {
        const i = pending.indexOf(p);
        if (i >= 0) pending.splice(i, 1);
        reject(new Error('timeout: интизорӣ барои ' + (type ?? 'ҳар паём')));
      }, timeoutMs);
      pending.push(p);
      flush();
    });

  ws.on('open', () => ws.send(JSON.stringify({ type: 'auth', token })));
  return { ws, inbox, next, send: (o: any) => ws.send(JSON.stringify(o)) };
}

beforeAll(async () => {
  ({ app } = await buildApp({ dbFile: ':memory:' }));
  await app.listen({ port: 0, host: '127.0.0.1' });
  baseUrl = `http://127.0.0.1:${(app.server.address() as any).port}`;
  const a = await auth('+992900000101');
  const b = await auth('+992900000102');
  tokenA = a.accessToken;
  tokenB = b.accessToken;
  idA = a.user.id;
  idB = b.user.id;
  const cr = await app.inject({ method: 'POST', url: '/v1/chats', headers: { authorization: `Bearer ${tokenA}` }, payload: { type: 'private', peerId: idB } });
  chatId = cr.json().chatId;
}, 20000);

afterAll(async () => {
  await app.close();
});

describe('WebSocket: паёмҳо ва ҳолатҳои ✓/✓✓', () => {
  it('A паём фиристад → B мегирад → ack delivered/read → A ack.update мегирад', async () => {
    const ca = connect(tokenA);
    const cb = connect(tokenB);
    expect((await ca.next('ready')).type).toBe('ready');
    expect((await cb.next('ready')).type).toBe('ready');
    // presence-и B ба A меояд (чати муштарак доранд)
    expect((await ca.next('presence')).online).toBe(true);

    const clientId = 'client-msg-1';
    ca.send({ type: 'message.send', clientId, chatId, body: 'Салом, ин озмоиш аст!' });

    const sentAck = await ca.next('ack.sent');
    expect(sentAck.clientId).toBe(clientId);
    const messageId = sentAck.messageId;

    const got = await cb.next('message.new');
    expect(got.message.body).toBe('Салом, ин озмоиш аст!');
    expect(got.message.senderId).toBe(idA);

    // B delivered
    cb.send({ type: 'ack', messageId, state: 'delivered' });
    expect((await ca.next('ack.update')).state).toBe('delivered');

    // B read
    cb.send({ type: 'ack', messageId, state: 'read' });
    expect((await ca.next('ack.update')).state).toBe('read');

    // typing
    cb.send({ type: 'typing', chatId, on: true });
    expect((await ca.next('typing')).on).toBe(true);

    // idempotency: ҳамон clientId дубора → ack.sent бо ҳамон messageId
    ca.send({ type: 'message.send', clientId, chatId, body: 'Салом, ин озмоиш аст!' });
    const dupAck = await ca.next('ack.sent');
    expect(dupAck.messageId).toBe(messageId);
    expect(dupAck.duplicate).toBe(true);

    // presence-и офлайн: A кашида мекунад → B presence offline мегирад
    ca.ws.close();
    expect((await cb.next('presence', 5000)).online).toBe(false);

    cb.ws.close();
  }, 20000);

  it('бе auth пайвастшавӣ баста мешавад (4001)', async () => {
    const ws = new WebSocket(baseUrl.replace('http', 'ws') + '/v1/ws');
    const closed = new Promise<number>((res) => ws.on('close', (code) => res(code)));
    ws.on('open', () => ws.send(JSON.stringify({ type: 'ping' })));
    expect(await closed).toBe(4001);
  }, 20000);
});
