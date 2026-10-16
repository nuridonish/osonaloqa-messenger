import { readFileSync } from 'node:fs';

// Мини-боргари .env (бе вобастагии иловагӣ)
try {
  const env = readFileSync(new URL('../../.env', import.meta.url), 'utf8');
  for (const line of env.split('\n')) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
} catch { /* .env нест — хуб */ }

export const config = {
  port: Number(process.env.PORT ?? 8080),
  host: process.env.HOST ?? '0.0.0.0',
  nodeEnv: process.env.NODE_ENV ?? 'development',
  jwtSecret: process.env.JWT_SECRET ?? 'dev-secret-change-me',
  accessTtlSec: 15 * 60,
  dbFile: process.env.DB_FILE ?? 'osonaloqa.db',
};

export const isDev = () => config.nodeEnv !== 'production';
