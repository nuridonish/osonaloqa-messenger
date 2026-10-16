import type { FastifyRequest, FastifyReply } from 'fastify';
import { verifyAccessToken } from '../auth.js';

export interface ReqUser { id: string; phone: string }

declare module 'fastify' {
  interface FastifyRequest {
    user?: ReqUser;
  }
}

export async function authenticate(req: FastifyRequest, reply: FastifyReply): Promise<void> {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    await reply.code(401).send({ error: 'unauthorized' });
    return;
  }
  const payload = verifyAccessToken(header.slice(7));
  if (!payload) {
    await reply.code(401).send({ error: 'token_expired_or_invalid' });
    return;
  }
  req.user = { id: payload.sub, phone: payload.phone };
}
