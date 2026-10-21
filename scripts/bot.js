// A scripted test player for boot-test.py.
//
// usage: node bot.js <port> <username> <version|auto> <action> [action ...]
// actions: wait:<ms>  chat:<text>  quit  join
//          at:<ms>  waits until that long after the bot started (lines bots up however long actions take)
//          tab:<text>  prints the server's tab completions for <text>, as "[tab] <text> -> a, b";
//                      a trailing "_" stands for a space (the test harness trims actions)
//          play:on / play:off  autoplay in the background while later actions run
//          click:<slot>  clicks a slot of the open menu (opened by a chat command), then waits 1.5 s
//          rclick:<slot> the same with a right click
//          close         closes the open menu
//          Every menu that opens is printed as "[menu] <title>" and one "[slot N] name | lore | lore" line per item
//          (the gray glass-pane filler is left out).
//          autoplay:<seconds>  plays the chat games for that long (see autoplay below)
//          race actions (for the races; <names> lists alternatives for different versions, e.g. grass_block,grass):
//          dig:<names>:<count>     breaks the nearest blocks of that kind
//          place:<names>:<count>   places that item from the inventory on the ground nearby
//          eat:<names>:<count>     eats that item (golden apples can be eaten on a full stomach)
//          craft:<names>:<count>   crafts that item in the inventory's 2x2 grid
//          attack:<names>:<secs>   hits the nearest mob of that kind until it dies or time runs out
//          smelt:<input>:<count>   places a furnace from the inventory, smelts <count> of <input> with coal, takes the result
// Prints every chat line the bot receives as "[BOT <name>] <text>", and action-bar text as "[BOT <name>] [bar] <text>".
// Needs mineflayer (npm install mineflayer@4); point NODE_PATH at its node_modules.

const mineflayer = require('mineflayer');

const STARTED = Date.now();

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
    bot.once('end', (reason) => console.log(`[BOT ${username}] disconnected: ${reason}`));
    bot.on('error', (error) => console.log(`[BOT ${username}] error: ${error && error.stack ? error.stack.split('\n').slice(0, 4).join(' / ') : error}`));
    bot.once('error', reject);
    bot.on('messagestr', (text, position) => {
      if (text.trim()) console.log(`[BOT ${username}] ${position === 'game_info' ? '[bar] ' : ''}${text}`);
    });
    bot.on('message', (message, position) => {
      if (autoplay && position !== 'game_info') play(message);
    });
    // Items arrive just after the window opens
    bot.on('windowOpen', (window) => setTimeout(() => printMenu(window), 500));
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

// Item names and lore come as JSON text, NBT or plain strings depending on the version
function plain(value) {
  if (value === undefined || value === null) return '';
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (trimmed.startsWith('{') || trimmed.startsWith('[') || trimmed.startsWith('"')) {
      try { return plain(JSON.parse(trimmed)); } catch (e) { /* not JSON */ }
    }
    return value.replace(/§./g, '');
  }
  if (Array.isArray(value)) return value.map(plain).join('');
  if (typeof value === 'object') {
    if ('value' in value && 'type' in value) return plain(value.value);
    let text = plain(value.text !== undefined ? value.text : (value[''] !== undefined ? value[''] : ''));
    if (value.extra) text += plain(value.extra);
    return text;
  }
  return String(value);
}

function printMenu(window) {
  if (!window || bot.currentWindow !== window) return;
  console.log(`[BOT ${username}] [menu] ${plain(window.title)}`);
  const end = window.inventoryStart !== undefined ? window.inventoryStart : window.slots.length;
  for (let slot = 0; slot < end; slot++) {
    const item = window.slots[slot];
    if (!item || item.name === 'gray_stained_glass_pane' || item.name === 'stained_glass_pane') continue;
    const name = plain(item.customName) || item.displayName;
    const lore = (item.customLore || []).map(plain).filter((line) => line.trim());
    console.log(`[BOT ${username}] [slot ${slot}] ${item.name} "${name}"${lore.length ? ' | ' + lore.join(' | ') : ''}`);
  }
}

// ---------------------------------------------------------------------------------------------
// Race actions. Block and item names differ between versions, so each takes a comma-separated
// list and uses the first name this version knows.
function pick(table, names) {
  for (const name of names.split(',')) if (table[name]) return table[name];
  return null;
}

function log(text) {
  console.log(`[BOT ${username}] ${text}`);
}

async function equip(names) {
  const item = pick(bot.registry.itemsByName, names);
  const stack = item && bot.inventory.items().find((i) => i.type === item.id);
  if (!stack) throw new Error(`no ${names} in the inventory`);
  await bot.equip(stack, 'hand');
}

async function dig(names, count) {
  const block = pick(bot.registry.blocksByName, names);
  if (!block) throw new Error(`unknown block ${names}`);
  for (let i = 0; i < count; i++) {
    const target = bot.findBlock({ matching: block.id, maxDistance: 4 });
    if (!target) throw new Error(`no ${names} within reach`);
    await bot.dig(target, true);
    log(`dug ${block.name} ${i + 1}/${count}`);
  }
}

async function place(names, count) {
  const { Vec3 } = require('vec3');
  await equip(names);
  const base = bot.entity.position.floored();
  const spots = [[2, 0], [-2, 0], [0, 2], [0, -2], [2, 2], [-2, -2], [2, -2], [-2, 2]];
  let placed = 0;
  for (const [dx, dz] of spots) {
    if (placed >= count) break;
    // The surface: a solid block with air above, around the bot's feet
    let ground = null;
    for (const dy of [0, -1, 1]) {
      const below = bot.blockAt(base.offset(dx, dy - 1, dz));
      const above = bot.blockAt(base.offset(dx, dy, dz));
      if (below && below.boundingBox === 'block' && above && above.boundingBox === 'empty') { ground = below; break; }
    }
    if (!ground) continue;
    try {
      await bot.placeBlock(ground, new Vec3(0, 1, 0));
    } catch (e) {
      // Some versions don't confirm the placement in time; the server still placed it
    }
    placed++;
    log(`placed ${names.split(',')[0]} ${placed}/${count}`);
    await sleep(400);
  }
}

async function eat(names, count) {
  for (let i = 0; i < count; i++) {
    await equip(names);
    await bot.consume();
    log(`ate ${names.split(',')[0]} ${i + 1}/${count}`);
  }
}

async function craft(names, count) {
  const item = pick(bot.registry.itemsByName, names);
  if (!item) throw new Error(`unknown item ${names}`);
  const recipe = bot.recipesFor(item.id, null, 1, null)[0];
  if (!recipe) throw new Error(`no recipe for ${names} with this inventory`);
  await bot.craft(recipe, count, null);
  log(`crafted ${item.name} x${count}`);
}

async function attack(names, seconds) {
  // 1.8 names mobs "Chicken", newer versions "chicken"
  const wanted = names.toLowerCase().split(',');
  const end = Date.now() + seconds * 1000;
  let hit = false;
  while (Date.now() < end) {
    const mob = bot.nearestEntity((e) => wanted.includes((e.name || '').toLowerCase()) && e.position.distanceTo(bot.entity.position) < 5);
    if (!mob && !hit && Date.now() < end - 1000) {
      // Not tracked yet (just summoned): look again shortly
      await sleep(500);
      continue;
    }
    if (!mob) {
      const near = Object.values(bot.entities).filter((e) => e !== bot.entity && e.position.distanceTo(bot.entity.position) < 8);
      log(`no ${names} left nearby (near: ${near.map((e) => `${e.name}@${e.position.distanceTo(bot.entity.position).toFixed(1)}`).join(', ') || 'nothing'})`);
      return;
    }
    await bot.lookAt(mob.position.offset(0, mob.height / 2, 0));
    bot.attack(mob);
    hit = true;
    await sleep(600);
  }
}

async function smelt(names, count) {
  const { Vec3 } = require('vec3');
  await place('furnace,lit_furnace', 1);
  const furnaceBlock = pick(bot.registry.blocksByName, 'furnace');
  const block = bot.findBlock({ matching: furnaceBlock.id, maxDistance: 4 });
  if (!block) throw new Error('the furnace was not placed');
  const furnace = await bot.openFurnace(block);
  const input = pick(bot.registry.itemsByName, names);
  await furnace.putFuel(pick(bot.registry.itemsByName, 'coal').id, null, 1);
  await furnace.putInput(input.id, null, count);
  // An item takes 10 seconds; take each one out as it is done, like a player waiting at the furnace
  let total = 0;
  const end = Date.now() + count * 15000;
  while (total < count && Date.now() < end) {
    await sleep(2000);
    if (!furnace.outputItem()) continue;
    const taken = await furnace.takeOutput();
    if (taken) {
      total += taken.count;
      log(`took ${taken.count}x ${taken.name} (${total}/${count})`);
    }
  }
  furnace.close();
}

async function race(kind, arg) {
  const [names, amount] = arg.split(':');
  const n = Number(amount || 1);
  try {
    if (kind === 'dig') await dig(names, n);
    else if (kind === 'place') await place(names, n);
    else if (kind === 'eat') await eat(names, n);
    else if (kind === 'craft') await craft(names, n);
    else if (kind === 'attack') await attack(names, n);
    else if (kind === 'smelt') await smelt(names, n);
  } catch (e) {
    log(`${kind} failed: ${e.message}`);
  }
}

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
    else if (kind === 'at') await sleep(Math.max(0, STARTED + Number(arg) - Date.now()));
    else if (kind === 'chat') bot.chat(arg);
    else if (kind === 'play') autoplay = arg === 'on';
    else if (kind === 'tab') {
      const text = arg.endsWith('_') ? `${arg.slice(0, -1)} ` : arg;
      const matches = await bot.tabComplete(text, true, false, 3000).catch((e) => [`error: ${e.message}`]);
      const names = matches.map((m) => (typeof m === 'string' ? m : m.match));
      console.log(`[BOT ${username}] [tab] ${text} -> ${names.join(', ')}`);
    }
    else if (kind === 'click' || kind === 'rclick') {
      if (!bot.currentWindow) console.log(`[BOT ${username}] no menu open to click`);
      else await bot.clickWindow(Number(arg), kind === 'rclick' ? 1 : 0, 0).catch((e) => console.log(`[BOT ${username}] click failed: ${e.message}`));
      await sleep(1500);
    }
    else if (kind === 'close') {
      if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
    }
    else if (['dig', 'place', 'eat', 'craft', 'attack', 'smelt'].includes(kind)) await race(kind, arg);
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
