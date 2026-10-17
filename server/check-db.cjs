const Database = require('better-sqlite3');
const db = new Database('osonaloqa.db', { readonly: true });
const users = db.prepare('SELECT id, phone, name, created_at FROM users ORDER BY created_at').all();
console.log('КОРБАРОН:');
users.forEach(u => console.log(' -', u.phone, '|', u.name || '(бе ном)', '|', new Date(u.created_at).toLocaleTimeString()));
const chats = db.prepare('SELECT c.id, c.type FROM chats c').all();
console.log('ЧАТҲО:', chats.length);
