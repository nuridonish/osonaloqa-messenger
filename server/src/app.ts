import Fastify from 'fastify';
import websocket from '@fastify/websocket';
import { openDb, type Db } from './db.js';
import { config } from './config.js';
import { authRoutes } from './routes/auth.js';
import { userRoutes } from './routes/users.js';
import { chatRoutes } from './routes/chats.js';
import { wsRoutes } from './ws.js';
import { allCountryCodes } from './phone.js';

export async function buildApp(opts?: { dbFile?: string; logger?: boolean }) {
  const app = Fastify({ logger: opts?.logger ?? false });
  const db = openDb(opts?.dbFile ?? config.dbFile);

  // Ҳатман пеш аз роутҳо — то hook-и onRoute-и websocket татбиқ шавад
  await app.register(websocket, { options: { maxPayload: 1024 * 1024 } });

  app.get('/health', async () => ({ ok: true, name: 'osonaloqa-server', time: Date.now() }));
  app.get('/v1/meta/country-codes', async () => ({ codes: allCountryCodes() }));

  authRoutes(app, db);
  userRoutes(app, db);
  chatRoutes(app, db);
  wsRoutes(app, db);

  return { app, db };
}
