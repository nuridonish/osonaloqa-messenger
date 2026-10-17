import type { FastifyInstance } from 'fastify';
import { createHash, randomUUID } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import type { Db } from '../db.js';
import { authenticate } from '../plugins/auth.js';

const MAX_SIZE = 50 * 1024 * 1024;

const EXT_BY_MIME: Record<string, string> = {
  'image/jpeg': 'jpg', 'image/png': 'png', 'image/webp': 'webp', 'image/gif': 'gif',
  'video/mp4': 'mp4', 'video/webm': 'webm', 'video/quicktime': 'mov',
  'audio/aac': 'aac', 'audio/mp4': 'm4a', 'audio/mpeg': 'mp3', 'audio/ogg': 'ogg',
  'audio/amr': 'amr', 'audio/3gpp': '3gp',
  'application/pdf': 'pdf', 'text/plain': 'txt',
};

export function mediaRoutes(app: FastifyInstance, db: Db): void {
  const dir = join(process.cwd(), 'data', 'media');
  mkdirSync(dir, { recursive: true });

  app.post('/v1/media', { preHandler: [authenticate] }, async (req, reply) => {
    const chunks: Buffer[] = [];
    let size = 0;
    for await (const chunk of req.raw) {
      size += chunk.length;
      if (size > MAX_SIZE) return reply.code(413).send({ error: 'too_large' });
      chunks.push(chunk as Buffer);
    }
    const buf = Buffer.concat(chunks);
    if (buf.length === 0) return reply.code(400).send({ error: 'empty' });
    const mime = String(req.headers['content-type'] ?? 'application/octet-stream').split(';')[0];
    const ext = EXT_BY_MIME[mime] ?? 'bin';
    const id = randomUUID();
    const file = `${id}.${ext}`;
    writeFileSync(join(dir, file), buf);
    return {
      url: `/media/${file}`,
      size: buf.length,
      mime,
      sha256: createHash('sha256').update(buf).digest('hex'),
    };
  });
}
