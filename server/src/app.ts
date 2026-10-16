import Fastify from 'fastify';
import websocket from '@fastify/websocket';
import fastifyStatic from '@fastify/static';
import { openDb, type Db } from './db.js';
import { config } from './config.js';
import { authRoutes } from './routes/auth.js';
import { userRoutes } from './routes/users.js';
import { chatRoutes } from './routes/chats.js';
import { statusRoutes } from './routes/statuses.js';
import { wsRoutes } from './ws.js';
import { allCountryCodes } from './phone.js';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

export async function buildApp(opts?: { dbFile?: string; logger?: boolean }) {
  const app = Fastify({ logger: opts?.logger ?? false });
  const db = openDb(opts?.dbFile ?? config.dbFile);

  // Ҳатман пеш аз роутҳо — то hook-и onRoute-и websocket татбиқ шавад
  await app.register(websocket, { options: { maxPayload: 1024 * 1024 } });

  app.get('/health', async () => ({ ok: true, name: 'osonaloqa-server', time: Date.now() }));
  app.get('/v1/meta/country-codes', async () => ({ codes: allCountryCodes() }));

  const publicDir = fileURLToPath(new URL('../public', import.meta.url));
  if (existsSync(publicDir)) {
    await app.register(fastifyStatic, { root: publicDir });
  }

  authRoutes(app, db);
  userRoutes(app, db);
  chatRoutes(app, db);
  statusRoutes(app, db);
  wsRoutes(app, db);

  return { app, db };
}
