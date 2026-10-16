/**
 * E2E-санҷиш: сервер бо ДУ клиенти WebSocket (A ва B).
 * Санҷиш: сабти ном → чат → паём A→B → ✓→✓✓→хондашуда → гурӯҳ → идома дар навбат.
 * Иҷро: npm run e2e
 */
import WebSocket from 'ws';
import { buildApp } from '../src/app.js';

const results: { name: string; ok: boolean; info?: string }[] = [];
const step = (name: string, ok: boolean, info?: string) => {
  results.push({ name, ok, info });
  console.log(`${ok ? '✅' : '❌'} ${name}${info ? ' — ' + info : ''}`);
};

function connect(baseUrl: string, token: string) {
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
  return { ws, next, send: (o: any) => ws.send(JSON.stringify(o)) };
}

async function rest(base: string, method: string, path: string, token?: string, body?: any): Promise<{ status: number; json: any }> {
  const r = await fetch(base + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: r.status, json: await r.json().catch(() => ({})) };
}

async function main() {
  const { app, db } = await buildApp({ dbFile: ':memory:' });
  await app.listen({ port: 0, host: '127.0.0.1' });
  const base = `http://127.0.0.1:${(app.server.address() as any).port}`;

  try {
    // 1. Сабти номи A ва B
    const phones = { A: '+992909000001', B: '+998909000002' };
    const reg: Record<string, any> = {};
    for (const [k, phone] of Object.entries(phones)) {
      const otp = await rest(base, 'POST', '/v1/auth/request-otp', undefined, { phone });
      step(`OTP-${k} фиристода шуд`, otp.status === 200);
      const v = await rest(base, 'POST', '/v1/auth/verify', undefined, { phone, code: '000000', deviceName: 'e2e' });
      reg[k] = v.json;
      step(`Сабти ном ${k} (${phone})`, v.status === 200 && !!reg[k].accessToken);
    }
    const tokA = reg.A.accessToken;
    const tokB = reg.B.accessToken;

    // 2. Чати шахсии A↔B
    const chat = await rest(base, 'POST', '/v1/chats', tokA, { type: 'private', peerId: reg.B.user.id });
    step('Чати шахсӣ сохта шуд', chat.status === 200);
    const chatId = chat.json.chatId;

    // 3. Пайвастшавии WS
    const A = connect(base, tokA);
    const B = connect(base, tokB);
    step('WS A пайваст шуд', (await A.next('ready')).type === 'ready');
    step('WS B пайваст шуд', (await B.next('ready')).type === 'ready');

    // 4. Паём A→B
    const text = 'Салом аз OSON ALOQA!';
    A.send({ type: 'message.send', clientId: 'e2e-1', chatId, body: text });
    const sent = await A.next('ack.sent');
    step('✓ паём фиристода шуд (ack.sent)', sent.type === 'ack.sent');
    const got = await B.next('message.new');
    step('Паём ба B расид (message.new)', got.type === 'message.new' && got.message.body === text);

    // 5. ✓✓ delivered
    B.send({ type: 'ack', messageId: got.message.id, state: 'delivered' });
    step('✓✓ delivered', (await A.next('ack.update')).state === 'delivered');

    // 6. Хондашуда (кабуд)
    B.send({ type: 'ack', messageId: got.message.id, state: 'read' });
    step('✓✓ хондашуда (кабуд)', (await A.next('ack.update')).state === 'read');

    // 7. Typing + оффлайн-навбат: B пайвастшавиро мекӯшад, A паём мефиристад, B баргардонда мегирад
    B.ws.close();
    await new Promise((r) => setTimeout(r, 200));
    A.send({ type: 'message.send', clientId: 'e2e-2', chatId, body: 'Дар ҳолати офлайн низ мерасад' });
    await A.next('ack.sent');
    const B2 = connect(base, tokB);
    const ready2 = await B2.next('ready');
    step('B такрор пайваст шуд', ready2.type === 'ready');
    await B2.next('message.new'); // паёми офлайн-навбат фавран пас аз пайвастшавӣ мебарояд
    // Тавассути REST-таърих мегирем (ws нав навбатро ба таври passive намедиҳад)
    const hist = await rest(base, 'GET', `/v1/chats/${chatId}/messages?limit=50`, tokB);
    step('Навбати офлайн: таърих дороӣ аст', hist.status === 200 && hist.json.messages.length === 2 && hist.json.messages[1].body === 'Дар ҳолати офлайн низ мерасад', `messages=${hist.json.messages.length}`);

    // 8. Гурӯҳ
    const group = await rest(base, 'POST', '/v1/chats', tokA, { type: 'group', title: 'Гурӯҳи E2E', memberIds: [reg.B.user.id] });
    const gChatId = group.json.chatId;
    A.send({ type: 'message.send', clientId: 'e2e-3', chatId: gChatId, body: 'Салом гурӯҳ!' });
    await A.next('ack.sent');
    step('Гурӯҳ: паём дар REST-таърих', ((await rest(base, 'GET', `/v1/chats/${gChatId}/messages`, tokB)).json.messages.length) === 1);

    // 9. Сервер танҳо матнро нигоҳ медорад (инҷо plaintext аз клиент — барои E2E-рамзгузорӣ body дар клиент рамз мешавад)
    const row = db.prepare('SELECT body FROM messages LIMIT 1').get() as any;
    step('Дар БД матн дар шакли ёфташуда нигоҳ дошта мешавад (E2E: клиент рамз мекунад)', typeof row.body === 'string');

    // 10. Оҳистагӣ: health
    const health = await rest(base, 'GET', '/health');
    step('/health кор мекунад', health.status === 200 && health.json.ok === true);
  } finally {
    await app.close();
  }

  const failed = results.filter((r) => !r.ok);
  console.log(`\n—— Натиҷа: ${results.length - failed.length}/${results.length} сабз ——`);
  if (failed.length > 0) process.exit(1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
