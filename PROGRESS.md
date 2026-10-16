# PROGRESS — Осон Алоқа (OSON ALOQA)

Лоиҳа: мессенҷери воқеии онлайн «Осон Алоқа» (osonaloqa-messenger).

## Ҳолати ҷорӣ
| Марҳила | Ҳолат | Шарҳ |
|---|---|---|
| M0 скелет | ✅ Тайёр | git init, папкаҳои /server /android /docs |
| M2 Backend | 🔄 Дар ҷараён | Fastify + TS + SQLite(dev) + WebSocket |
| M3 Клиент | ⏳ | Kotlin + Compose |
| M4 Push | ⏳ | FCM + fallback |
| M5 Медиа | ⏳ | |
| M6 Гурӯҳ/Статус | ⏳ | |
| M7 Контактҳо/QR | ⏳ | |
| M8 Зангҳо WebRTC | ⏳ | |
| M9 E2E | ⏳ | |
| M10 Release | ⏳ | |

## Қарорҳои муҳим
- minSdk 26, compile/target 36 — танҳо дастгоҳҳои замонавӣ (қоидаи 0.1).
- Бренд: «OSON ALOQA» / «Осон Алоқа» — монограммаи OA дар пуфаки сабз.
- Забонҳо: тоҷ (пешфарз), ӯз, рус, форсӣ (RTL), англисӣ, арабӣ (RTL).
- Телефон: E.164, ҳамаи кодҳои ҷаҳонӣ (libphonenumber-js дар сервер).
- Сервери dev: SQLite тавассути better-sqlite3; production: PostgreSQL.
