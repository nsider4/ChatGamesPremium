// A scripted test player for boot-test.py.
//
// usage: node bot.js <port> <username> <version|auto> <action> [action ...]
// actions: wait:<ms>  chat:<text>  quit  join
// Prints every chat line the bot receives as "[BOT <name>] <text>".
// Needs mineflayer (npm install mineflayer@4); point NODE_PATH at its node_modules.

const mineflayer = require('mineflayer');

const [port, username, version, ...actions] = process.argv.slice(2);
let bot = null;

function connect() {
  return new Promise((resolve, reject) => {
    bot = mineflayer.createBot({
      host: '127.0.0.1',
      port: Number(port),
      username,
      auth: 'offline',
      version: version === 'auto' ? false : version,
      hideErrors: false,
    });
    bot.once('spawn', () => {
      console.log(`[BOT ${username}] joined`);
      resolve();
    });
    bot.once('kicked', (reason) => console.log(`[BOT ${username}] kicked: ${JSON.stringify(reason)}`));
    bot.once('error', reject);
    bot.on('messagestr', (text) => console.log(`[BOT ${username}] ${text}`));
  });
}

function quit() {
  return new Promise((resolve) => {
    bot.once('end', () => {
      console.log(`[BOT ${username}] left`);
      resolve();
    });
    bot.quit();
  });
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

(async () => {
  await connect();
  for (const action of actions) {
    const [kind, ...rest] = action.split(':');
    const arg = rest.join(':');
    if (kind === 'wait') await sleep(Number(arg));
    else if (kind === 'chat') bot.chat(arg);
    else if (kind === 'quit') await quit();
    else if (kind === 'join') await connect();
    else console.log(`[BOT ${username}] unknown action ${action}`);
  }
  if (bot && bot.player) await quit();
  process.exit(0);
})().catch((error) => {
  console.log(`[BOT ${username}] error: ${error.message}`);
  process.exit(1);
});
