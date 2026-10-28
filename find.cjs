const fs = require('fs');
const base = 'C:\\Мои проекты';
for (const d of fs.readdirSync(base, { withFileTypes: true })) {
  if (/промт/i.test(d.name)) {
    const full = base + '\\' + d.name;
    console.log('ЁФТ ШУД:', full, d.isDirectory() ? '[DIR]' : '[FILE]');
    if (d.isDirectory()) {
      for (const f of fs.readdirSync(full, { withFileTypes: true })) {
        console.log('  -', f.name, f.isDirectory() ? '[DIR]' : '');
        if (f.isDirectory()) {
          for (const g of fs.readdirSync(full + '\\' + f.name)) {
            console.log('      ·', g);
          }
        }
      }
    }
  }
}
