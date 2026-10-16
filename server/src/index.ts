import { buildApp } from './app.js';
import { config } from './config.js';

const { app, db } = await buildApp({ logger: true });

app.listen({ port: config.port, host: config.host }).then((addr) => {
  console.log(`OSON ALOQA server: ${addr} (env=${config.nodeEnv})`);
});

for (const sig of ['SIGINT', 'SIGTERM'] as const) {
  process.on(sig, () => {
    app.close();
    db.close();
    process.exit(0);
  });
}
