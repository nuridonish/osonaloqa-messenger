const Database = require('better-sqlite3');
const fs = require('fs');
const db = new Database('osonaloqa.db', { readonly: true });
console.log('=== USERS ===');
db.prepare('SELECT phone, name, username, last_seen_at FROM users ORDER BY last_seen_at DESC').all()
  .forEach(u => console.log(u.phone, '|', u.name || '-', '| @' + (u.username || '-'), '|', new Date(u.last_seen_at).toLocaleTimeString()));
console.log('=== CHATS ===', db.prepare('SELECT COUNT(*) n FROM chats').get().n);
console.log('=== MESSAGES ===', db.prepare('SELECT COUNT(*) n FROM messages').get().n);
console.log('=== MEDIA ===', (fs.existsSync('data/media') ? fs.readdirSync('data/media') : []).length, 'files');

const log = fs.readFileSync('../server.log', 'utf8').split('\n').slice(-400);
const errs = log.filter(l => /"statusCode":(4\d\d|5\d\d)/.test(l) && !l.includes('"statusCode":429'));
const map = {};
errs.forEach(l => {
  const m = l.match(/"method":"(\w+)","url":"([^"?]+)(\?[^"]*)?"/);
  const s = l.match(/"statusCode":(\d+)/);
  if (m) { const k = m[1] + ' ' + m[2] + ' -> ' + (s ? s[1] : '?'); map[k] = (map[k] || 0) + 1; }
});
console.log('=== ERRORS (no 429) ===');
Object.entries(map).forEach(([k, v]) => console.log(v + 'x', k));
if (!Object.keys(map).length) console.log('none ✅');
