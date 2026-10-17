const WebSocket = require('ws');

const BASE = 'http://localhost:8080';
const A = '+79647271719'; // мустафо (Redmi)
const B = '+79936271719'; // мсф (Samsung)

async function rest(method, path, token, body) {
  const r = await fetch(BASE + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: 'Bearer ' + token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  return r.json();
}

async function login(phone) {
  const otp = await rest('POST', '/v1/auth/request-otp', null, { phone });
  const code = otp.devCode || '000000';
  const v = await rest('POST', '/v1/auth/verify', null, { phone, code, deviceName: 'bootstrap' });
  if (!v.accessToken) throw new Error('login fail ' + phone + ': ' + JSON.stringify(v));
  return v;
}

function wsSend(token, chatId, text, clientId) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(BASE.replace('http', 'ws') + '/v1/ws');
    const timer = setTimeout(() => reject(new Error('ws timeout')), 8000);
    ws.on('error', (e) => { clearTimeout(timer); reject(e); });
    ws.on('message', (raw) => {
      const m = JSON.parse(raw.toString());
      if (m.type === 'ready') {
        ws.send(JSON.stringify({ type: 'message.send', clientId, chatId, body: text }));
      }
      if (m.type === 'ack.sent') {
        clearTimeout(timer);
        ws.close();
        resolve(m);
      }
    });
    ws.on('open', () => ws.send(JSON.stringify({ type: 'auth', token })));
  });
}

(async () => {
  const a = await login(A);
  const b = await login(B);
  console.log('A (мустафо):', a.user.id.slice(0, 8), '| B (мсф):', b.user.id.slice(0, 8));

  const chat = await rest('POST', '/v1/chats', a.accessToken, { type: 'private', peerId: b.user.id });
  const chatId = chat.chatId;
  console.log('чат:', chatId);

  const s1 = await wsSend(a.accessToken, chatId, 'Салом! Ин OSON ALOQA аст 🟢', 'boot-1');
  console.log('A→B фиристода шуд:', s1.messageId.slice(0, 8));
  await new Promise(r => setTimeout(r, 500));
  const s2 = await wsSend(b.accessToken, chatId, 'Ҳа, қабул шуд! Кор мекунад ✅', 'boot-2');
  console.log('B→A фиристода шуд:', s2.messageId.slice(0, 8));

  const hist = await rest('GET', `/v1/chats/${chatId}/messages`, a.accessToken);
  console.log('таърих:', hist.messages.length, 'паём');
  console.log('✅ Чат омода аст — дар ҳарду телефон кушода шавад!');
  process.exit(0);
})().catch(e => { console.error('❌', e.message); process.exit(1); });
