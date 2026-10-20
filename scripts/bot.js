// A scripted test player for boot-test.py.
//
// usage: node bot.js <port> <username> <version|auto> <action> [action ...]
// actions: wait:<ms>  chat:<text>  quit  join
//          autoplay:<seconds>  plays the chat games for that long (see autoplay below)
// Prints every chat line the bot receives as "[BOT <name>] <text>", and action-bar text as "[BOT <name>] [bar] <text>".
// Needs mineflayer (npm install mineflayer@4); point NODE_PATH at its node_modules.

const mineflayer = require('mineflayer');

const [port, username, version, ...actions] = process.argv.slice(2);
let bot = null;
let autoplay = false;
// Answer like a person: well after the 400 ms anti-macro limit
const ANSWER_DELAY_MS = 900;

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
    bot.on('messagestr', (text, position) => {
      if (text.trim()) console.log(`[BOT ${username}] ${position === 'game_info' ? '[bar] ' : ''}${text}`);
    });
    bot.on('message', (message, position) => {
      if (autoplay && position !== 'game_info') play(message);
    });
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

// ---------------------------------------------------------------------------------------------
// autoplay: answers the games whose answer can be read or worked out from chat.
//  - Clickable: clicks the text (runs its /chatgames command)
//  - Reaction / Random: types the shown text. Reaction first tries all-lowercase (to trigger the
//    wrong-case message), Random first answers instantly (to trigger the anti-macro check).
//  - Math: evaluates "solve" questions and brute-forces "missing value" ones
//  - Sequence: works out the pattern
//  - Memorize: remembers the list and types it back once chat is cleared
let expectList = false;
let expectSequence = false;
let rememberedList = null;

function answer(text, delay = ANSWER_DELAY_MS) {
  setTimeout(() => {
    if (!autoplay) return;
    console.log(`[BOT ${username}] >> ${text}`);
    bot.chat(text);
  }, delay);
}

function findClick(node) {
  if (!node || typeof node !== 'object') return null;
  const click = node.clickEvent || node.click_event;
  if (click && (click.value || click.command)) return click.value || click.command;
  for (const key of ['extra', 'with']) {
    if (Array.isArray(node[key])) {
      for (const child of node[key]) {
        const found = findClick(child);
        if (found) return found;
      }
    }
  }
  return null;
}

function evaluate(expression) {
  // Only digits, spaces and + - x / reach here
  if (!/^[\d\s+\-x/]+$/.test(expression)) return null;
  return Function(`return (${expression.replace(/x/g, '*')});`)();
}

function nextInSequence(terms) {
  const n = terms.length;
  const diffs = terms.slice(1).map((t, i) => t - terms[i]);
  if (diffs.every((d) => d === diffs[0])) return terms[n - 1] + diffs[0];
  if (terms.every((t, i) => i < 2 || t === terms[i - 1] + terms[i - 2])) return terms[n - 1] + terms[n - 2];
  const root = Math.round(Math.sqrt(terms[n - 1]));
  if (terms.every((t, i) => Math.round(Math.sqrt(t)) ** 2 === t && (i === 0 || Math.sqrt(t) === Math.sqrt(terms[i - 1]) + 1))) {
    return (root + 1) ** 2;
  }
  if (terms[0] !== 0 && terms.every((t, i) => i === 0 || t * terms[0] === terms[i - 1] * terms[1])) {
    return terms[n - 1] * (terms[1] / terms[0]);
  }
  if (diffs.every((d, i) => i < 2 || d === diffs[i - 2])) return terms[n - 1] + diffs[diffs.length - 2];
  return null;
}

function play(message) {
  const click = findClick(message.json);
  if (click && click.startsWith('/chatgames ')) {
    answer(click);
    return;
  }
  const text = message.toString();
  const quoted = (text.match(/`([^`]*)`/) || [])[1];

  if (/to memorize/i.test(text)) { expectList = true; return; }
  if (expectList && quoted) { rememberedList = quoted; expectList = false; return; }
  if (/write the list back/i.test(text) && rememberedList) { answer(rememberedList); rememberedList = null; return; }
  if (/complete the sequence/i.test(text)) { expectSequence = true; return; }
  if (expectSequence && quoted) {
    expectSequence = false;
    const terms = quoted.split(',').map((t) => t.trim()).filter((t) => t !== '?').map(Number);
    const next = nextInSequence(terms);
    if (next !== null) answer(String(next));
    return;
  }
  if (!quoted) return;

  if (/write out the word/i.test(text)) {
    if (quoted.toLowerCase() !== quoted) answer(quoted.toLowerCase(), 600);
    answer(quoted, 1500);
  } else if (/write out/i.test(text)) {
    answer(quoted, 50);
    answer(quoted);
  } else if (/solve/i.test(text)) {
    if (quoted.includes('?')) {
      const [left, right] = quoted.split(' = ');
      for (let guess = -10000; guess <= 10000; guess++) {
        if (evaluate(left.replace('?', String(guess))) === Number(right)) { answer(String(guess)); break; }
      }
    } else {
      answer(String(evaluate(quoted)));
    }
  }
}

(async () => {
  await connect();
  for (const action of actions) {
    const [kind, ...rest] = action.split(':');
    const arg = rest.join(':');
    if (kind === 'wait') await sleep(Number(arg));
    else if (kind === 'chat') bot.chat(arg);
    else if (kind === 'quit') await quit();
    else if (kind === 'join') await connect();
    else if (kind === 'autoplay') {
      autoplay = true;
      await sleep(Number(arg) * 1000);
      autoplay = false;
    }
    else console.log(`[BOT ${username}] unknown action ${action}`);
  }
  if (bot && bot.player) await quit();
  process.exit(0);
})().catch((error) => {
  console.log(`[BOT ${username}] error: ${error.message}`);
  process.exit(1);
});
