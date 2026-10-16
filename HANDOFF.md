# HANDOFF — Осон Алоқа (OSON ALOQA)

Санаи ниҳоӣ: 2026-10-16 · versionName 0.1.0 · versionCode 1

## 1. Чӣ ТАЙЁР АСТ (санҷидашуда)

### Backend (`server/`) — КОРАКОН ва ОНЛАЙН
- Node.js 20+ (дар ин компютер v24 санҷида шуд), TypeScript, Fastify.
- Реҷаи dev бе Docker: better-sqlite3 (файли `osonaloqa.db`).
- Иҷрои сервер:
  ```
  cd server
  npm install
  npm run dev          # http://localhost:8080
  ```
- Санҷиш: `npm test` → 22/22 сабз; `npm run e2e` → 16/16 сабз (ду клиенти WS: A→B, ✓/✓✓/хондашуда, навбати офлайн, гурӯҳ).
- API: `/health`, `/v1/auth/*` (OTP, verify, refresh), `/v1/users/*` (me, lookup, updateProfile), `/v1/chats` (+ messages, read), `/v1/statuses`, `/v1/meta/country-codes`, WS `/v1/ws` (auth, message.send/new, ack sent/delivered/read, typing, presence, навбати офлайн).
- OTP: дар dev танҳо `000000` (ва дар лог). Дар production қатъӣ барнамегардад.
- Телефонҳо: E.164 — ҳамаи кодҳои ҷаҳон (libphonenumber-js), санҷиш барои TJ/UZ/RU/IR/DE/US гузашт.

### Клиенти Android (`android/`) — КОДИ ПУРРА, ДАР ИН МУҲИТ СОХТА НАШАВУАСТ
Барномаи Compose Material 3: splash → welcome → телефон (кодҳои ҳамаи кишварҳо, парчамҳо) → OTP → профил → 4 ҷадвал (Чатҳо/Статус/Ҷамоаҳо/Зангҳо) → чат бо ✓/✓✓, typing, онлайн, навбати офлайн, 6 забон (тоҷ/ӯз/рус/форсӣ RTL/англ/араб RTL), тарҳи сабз/сафед, реҷаи торик.

## 2. Чаро дар ин компютер APK сохта нашуд
Ҳамаи хостҳои Google (dl.google.com, maven.google.com) дар ин шабака бастаанд (404-и сохтӣ). Барои сохтани Android лозим аст: AGP, androidx.* (Compose), SDK — ҳама аз Google. Бинобар ин бо қоидаи 0-и промт кор идома ёфт ва дастури сохтан дар поён аст.

## 3. Дастури ДАҚИҚИ сохтани APK (дар компютери дигар ё VPN)
1) JDK 17 насб кунед (Temurin: https://adoptium.net) — ё аз Android Studio.
2) Android Studio (Ladybug+) насб кунед ва SDK Platform 36 + Build-Tools-ро кушоед (SDK Manager).
3) Папкаи `android/`-ро кушоед дар Android Studio → Sync Gradle (AGP 8.10.1, Gradle 8.14 — wrapper-.properties ҳаст; Studio худаш wrapper-ро мекашад).
4) Агар хатои KSP: дар `gradle/libs.versions.toml` версияи `ksp`-ро бо тавсияи Studio иваз кунед.
5) Адреси сервер: `android/gradle.properties` → `OSONALOQA_DEV_URL` ба адреси сервери худ (масалан tunnel) гузоред. **Ҳеҷ IP-и маҳаллӣ дар код нест** — танҳо ин як ҷо.
6) Сохтан: `Build > Build APK(s)` ё:
   ```
   cd android
   gradle wrapper        # як бор
   ./gradlew assembleDebug
   ```
7) Сервери ҷамъияти барои 2 телефон: `cloudflared tunnel --url http://localhost:8080` → URL-и HTTPS-и пайдошударо ба `OSONALOQA_DEV_URL` гузоред ва дубора APK бисозед.
8) Имзо барои release: `keytool -genkey -v -keystore osonaloqa.keystore -alias osonaloqa -keyalg RSA -keysize 4096 -validity 10000`, сипас `./gradlew assembleRelease` (imzo дар `build.gradle.kts` илова кардан лозим аст).

## 4. Он чи КОРБАР бояд кунад (барои production)
- [ ] Домен + VPS ва `docker compose up` ( compose-файл дар M10) — ё дар аввал cloudflared tunnel.
- [ ] SMS-провайдер ( adapter дар `server/src/auth.ts` — ҷои `console.log` [OTP]).
- [ ] `JWT_SECRET`-и тасодуфӣ дар `.env`.
- [ ] (Дар M4) Firebase → `google-services.json` барои FCM push.

## 5. Маҳдудиятҳои МАЪЛУМ (рость)
- Медиа (акс/видео/файл/овоз) — UI-тоѩр, REST-и боркунӣ дар сервер ҳанӯз нест (M5).
- Зангҳои WebRTC — экран ҳаст, signaling дар сервер нест (M8).
- Ҷамоаҳо — ҷадвали UI; гурӯҳҳо дар backend кор мекунанд.
- E2E-рамзгузорӣ: сервер body-ро «шуша» нигоҳ медорад, вале клиент ҳанӯз рамз намекунад (M9 — X25519+AES-GCM/libsignal).
- Rate-limit, helmet, HTTPS — дар production-деплой (M10).
- Иконаи launcher — векторӣ, сохта нашудааст (баъди аввалин build дар Studio санҷед).
- Коди Android дар ин муҳит компилятсия НАШУДААСТ — эҳтимоли хатоҳои хурди компилятсия (>0) воқеист; Studio онҳоро фавран нишон медиҳад.

## 6. Санҷиши зуд баъди оғоз
1. `npm run dev` дар server → `/health` `{"ok":true}`.
2. Ду телефон: телефонҳои гуногунмамлакат (+992..., +998...) → OTP 000000 → чат → паём → ✓✓ кабуд.
