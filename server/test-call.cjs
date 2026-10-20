// Санҷиши signaling-и занг: invite/sdp/ice тавассути сервер ба дигар корбар мерасад?
const WebSocket = require('ws');
const BASE = 'http://localhost:8080';

async function rest(method, path, token, body) {
  const r = await fetch(BASE + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: 'Bearer ' + token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  return r.json();
}

function wsConn(token) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(BASE.replace('http', 'ws') + '/v1/ws');
    const inbox = [];
    const waiters = [];
    ws.on('message', (raw) => {
      const msg = JSON.parse(raw.toString());
      const w = waiters.shift();
      if (w) w(msg); else inbox.push(msg);
    });
    ws.on('open', () => ws.send(JSON.stringify({ type: 'auth', token })));
    const next = (type) => new Promise((res, rej) => {
      const i = inbox.findIndex((x) => !type || x.type === type);
      if (i >= 0) return res(inbox.splice(i, 1)[0]);
      const t = setTimeout(() => rej(new Error('timeout ' + type)), 5000);
      waiters.push((msg) => { clearTimeout(t); res(msg); });
    });
    ws.on('open', () => {});
    resolve({ ws, next, send: (o) => ws.send(JSON.stringify(o)) });
    ws.on('error', reject);
  });
}

(async () => {
  const A = '+79647271719';
  const B = '+79936271719';
  const otpA = await rest('POST', '/v1/auth/request-otp', null, { phone: A });
  const a = await rest('POST', '/v1/auth/verify', null, { phone: A, code: otpA.devCode || '000000' });
  const otpB = await rest('POST', '/v1/auth/request-otp', null, { phone: B });
  const b = await rest('POST', '/v1/auth/verify', null, { phone: B, code: otpB.devCode || '000000' });

  const chats = await rest('GET', '/v1/chats', a.accessToken);
  const chat = chats.chats.find((c) => c.type === 'private' && c.peer && c.peer.phone === B);
  const chatId = chat ? chat.id : (await rest('POST', '/v1/chats', a.accessToken, { type: 'private', peerId: b.user.id })).chatId;

  const WA = await wsConn(a.accessToken);
  const WB = await wsConn(b.accessToken);
  console.log('A ready:', (await WA.next('ready')).type);
  console.log('B ready:', (await WB.next('ready')).type);

  WA.send({ type: 'call.invite', chatId, video: true, sdp: 'v=0 TEST-OFFER' });
  const got = await WB.next('call.invite');
  console.log('B got invite:', got.type, '| sdp:', got.sdp, '| from:', !!got.from);

  WB.send({ type: 'call.accept', chatId, sdp: 'v=0 TEST-ANSWER' });
  const acc = await WA.next('call.accept');
  console.log('A got accept:', acc.type, '| sdp:', acc.sdp);

  WA.send({ type: 'call.ice', chatId, candidate: 'candidate-1' });
  const ice = await WB.next('call.ice');
  console.log('B got ice:', ice.candidate);

  WA.send({ type: 'call.end', chatId });
  const end = await WB.next('call.end');
  console.log('B got end:', end.type);
  console.log('✅ SIGNALING КОРИ МЕКУНАД');
  process.exit(0);
})().catch((e) => { console.error('❌', e.message); process.exit(1); });
