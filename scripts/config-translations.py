"""Builds the translated copies of the default config files for the mine-translator repository.

usage: python scripts/config-translations.py extract <out.json>
       python scripts/config-translations.py build <translations dir> <out dir> [language ...]
       python scripts/config-translations.py check <out dir> [language ...]

The English files in src/main/resources are the source. 'extract' lists every piece of text a
player or server owner reads: comments, messages, menu names and lore, word lists, trivia...
(see TEXT_RULES) as {"english text": ""}.

A translation is <translations dir>/<language>.json, the same map with each text translated.
A key "<file>:<path>" translates the text at that one place, for an English word that needs two
different translations (the Fish race and the fish that tames cats).
'build' swaps each translated text into the English file, line by line, so keys, settings,
spacing and the order of everything stay exactly as in English; a text missing from the map
stays English. It writes <out dir>/<language>/<file>.yml for each language, plus
<out dir>/version.json. A language's version only goes up when its built files changed, since
servers re-download a language when its version rises; so build straight into the
mine-translator checkout (Configs/2.0), whose version.json holds the current numbers.

'check' compares each built file with English: the same keys and settings, valid YAML, and the
same %placeholders%, <center> tags and colour codes in every translated value.
"""
import hashlib, json, os, re, sys

import yaml

RESOURCES = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
FILES = ['config.yml', 'games.yml', 'messages.yml', 'menus.yml', 'words.yml', 'races.yml', 'global_games.yml',
         'challenges.yml', 'streaks.yml', 'events.yml', 'rewards.yml', 'add-ons.yml', 'presets.yml', 'milestones.yml',
         'discord.yml']

# The text values of each file, as path patterns ('*' is one key, '**' any number, '[]' a list item).
# Everything else (materials, actions, commands, ids, numbers...) is a setting and never changes.
TEXT_RULES = {
    'messages.yml': [r'**'],
    'menus.yml': [r'game-names.*', r'formats.*', r'messages.**', r'menus.*.title', r'**.name', r'**.lore[]',
                  r'**.formats.*'],
    'words.yml': [r'*.Words[]', r'memorize.pools.shopping_list[]', r'memorize.pools.words[]',
                  r'memorize.pools.colors[]', r'trivia.data.*.question', r'trivia.data.*.answers[]',
                  r'word_association.data.*.clues[]', r'word_association.data.*.answers[]'],
    'games.yml': [r'*.titleMessage', r'*.subtitleMessage'],
    'races.yml': [r'**.titleMessage', r'**.subtitleMessage', r'**.message', r'**.messages.**'],
    'global_games.yml': [r'messages.**', r'**.messages.**', r'**.name', r'bar.**', r'words.*'],
    'challenges.yml': [r'**.name', r'**.description', r'messages.**'],
    'streaks.yml': [r'**.name', r'messages.**'],
    'events.yml': [r'events.*.name', r'messages.**'],
    'rewards.yml': [r'**.data[]'],
    'config.yml': [],
    'add-ons.yml': [],
    'discord.yml': [r'**.content', r'**.content[]', r'**.title', r'**.description', r'**.description[]', r'**.author',
                    r'**.author.name', r'**.footer', r'**.footer.text', r'**.fields.name', r'**.fields.value',
                    r'games.hidden-question', r'lists.*', r'lists.medals[]',
                    r'challenges.periods.*'],
    'presets.yml': [],
    'milestones.yml': [],
}
# Reward lines: only what follows these tags is text; commands stay as they are
REWARD_TEXT_TAGS = ('[playermsg]', '[broadcast]', '[message]', '[actionbar]', '[title]')

COMMENT = re.compile(r'^(\s*)#(\s*)(.*?)(\s*)$')
KEY = re.compile(r"^(\s*)(- )?('(?:[^']|'')*'|\"[^\"]*\"|[^\s#'\"][^:#]*?):(?:(\s+)(.*?))?\s*$")
ITEM = re.compile(r'^(\s*)- (.*?)\s*$')
MAP_ITEM = re.compile(r'^[\w.-]+:(\s|$)')
LETTER = re.compile(r'[^\W\d_]')


def rule_regex(pattern):
    out = ''
    i = 0
    while i < len(pattern):
        if pattern.startswith('**', i):
            out += r'.*'
            i += 2
        elif pattern[i] == '*':
            out += r'[^.\[\]]+'
            i += 1
        elif pattern[i] in '.[]':
            out += '\\' + pattern[i]
            i += 1
        else:
            out += re.escape(pattern[i])
            i += 1
    return re.compile('^' + out + '$')


RULES = {name: [rule_regex(p) for p in patterns] for name, patterns in TEXT_RULES.items()}


def unquote(raw):
    """Returns (text, quote) for a scalar as written, or None for anything that isn't one."""
    if raw.startswith("'") and raw.endswith("'") and len(raw) >= 2:
        return raw[1:-1].replace("''", "'"), "'"
    if raw.startswith('"') and raw.endswith('"') and len(raw) >= 2:
        return raw[1:-1].replace('\\"', '"').replace('\\\\', '\\'), '"'
    if raw == '' or raw[0] in '[{|>&*!' or raw in ('true', 'false', '~', 'null'):
        return None
    return raw, ''


def quote(text, quote_char):
    if quote_char == '"':
        return '"' + text.replace('\\', '\\\\').replace('"', '\\"') + '"'
    # Plain scalars become single-quoted: a translation may add ': ' or a leading special character
    return "'" + text.replace("'", "''") + "'"


def flow_items(raw):
    """Splits a one-line ['a', 'b'] list into its raw items, or None."""
    if not (raw.startswith('[') and raw.endswith(']')):
        return None
    items, current, quote_char = [], '', None
    body = raw[1:-1]
    i = 0
    while i < len(body):
        c = body[i]
        if quote_char:
            current += c
            if c == quote_char:
                if quote_char == "'" and i + 1 < len(body) and body[i + 1] == "'":
                    current += "'"
                    i += 1
                else:
                    quote_char = None
        elif c in "'\"":
            quote_char = c
            current += c
        elif c == ',':
            items.append(current.strip())
            current = ''
        else:
            current += c
        i += 1
    if current.strip():
        items.append(current.strip())
    return items


def segments(name, text):
    """Yields (line number, kind, path, english, rebuild) for every translatable piece of a file.

    rebuild(translation) returns the line with that piece replaced.
    """
    rules = RULES[name]
    stack = []  # (indent, key)
    lines = text.split('\n')
    for number, line in enumerate(lines):
        comment = COMMENT.match(line)
        if comment:
            body = comment.group(3)
            if LETTER.search(body):
                prefix = comment.group(1) + '#' + comment.group(2)
                yield number, 'comment', None, body, (lambda t, p=prefix: p + t)
            continue
        if not line.strip():
            continue
        item = ITEM.match(line)
        # '- key: value' starts a map inside a list; any other '- ...' is a list item, even with ': ' in it
        key = KEY.match(line) if not item or MAP_ITEM.match(item.group(2)) else None
        if key:
            item = None
        if key:
            indent = len(key.group(1)) + (2 if key.group(2) else 0)
            name_ = key.group(3).strip("'\"")
            while stack and stack[-1][0] >= indent:
                stack.pop()
            path = '.'.join(k for _, k in stack + [(indent, name_)])
            raw = key.group(5) or ''
            head = line[:len(line) - len(line.lstrip()) ] + (key.group(2) or '') + key.group(3) + ':' + (key.group(4) or ' ')
            if raw == '':
                stack.append((indent, name_))
                continue
            inline = INLINE_COMMENT.match(raw)
            if inline and LETTER.search(inline.group(3)):
                # 'minimum-players: 2 #explanation': the setting stays, the comment is text
                yield number, 'comment', None, inline.group(3), (lambda t, h=head + inline.group(1) + inline.group(2): h + t)
                continue
        elif item:
            indent = len(item.group(1))
            while stack and stack[-1][0] > indent:
                stack.pop()
            parent = '.'.join(k for _, k in stack)
            path = parent + '[]'
            raw = item.group(2)
            head = item.group(1) + '- '
        else:
            continue
        flow = flow_items(raw)
        reward = reward_text(raw) if flow is None else None
        if reward:
            # A message reward, in any file: only its text is translated, the chance, tag and permission stay
            before, english, after, quote_char = reward
            yield number, 'value', path, english, (lambda t, h=head, b=before, a=after, q=quote_char: h + quote(b + t + a, q))
            continue
        if flow is None and not any(r.match(path) for r in rules):
            continue
        if flow is not None:
            item_path = path + '[]'
            if not any(r.match(item_path) for r in rules):
                continue
            for index, entry in enumerate(flow):
                parsed = unquote(entry)
                if parsed and LETTER.search(parsed[0]):
                    def rebuild(t, flow=flow, index=index, quote_char=parsed[1], head=head):
                        items = list(flow)
                        items[index] = quote(t, quote_char)
                        return head + '[' + ', '.join(items) + ']'
                    yield number, 'value', item_path, parsed[0], rebuild
            continue
        parsed = unquote(raw)
        if not parsed or not LETTER.search(parsed[0]):
            continue
        value, quote_char = parsed
        if name == 'rewards.yml':
            continue  # only its message rewards are text, and those were handled above
        yield number, 'value', path, value, (lambda t, h=head, q=quote_char: h + quote(t, q))


# Console commands whose text players read, as in milestones.yml: 'say <text>', '[consolecmd] broadcast <text>'
COMMAND_TEXT = re.compile(r'^((?:\[consolecmd\]\s*)?(?:minecraft:)?(?:say|broadcast|bc) )(.*?)$')
INLINE_COMMENT = re.compile(r'''^([^'"#][^#]*?)(\s+#\s*)(.*)$''')


REWARD_LINE = re.compile(r'^((?:\d+(?:\.\d+)?%~ )?(?:' + '|'.join(re.escape(t) for t in REWARD_TEXT_TAGS)
                         + r')\s*)(.*?)((?:\s+has:\S+)?)$')


def reward_text(raw):
    """(before, text, after, quote) for a reward line that sends a message, else None."""
    parsed = unquote(raw)
    if not parsed:
        return None
    match = REWARD_LINE.match(parsed[0])
    if match and LETTER.search(match.group(2)):
        return match.group(1), match.group(2), match.group(3), parsed[1]
    match = COMMAND_TEXT.match(parsed[0])
    if match and LETTER.search(match.group(2)):
        return match.group(1), match.group(2), '', parsed[1]
    return None


def read(name):
    with open(os.path.join(RESOURCES, name), encoding='utf-8') as f:
        return f.read().replace('\r\n', '\n')


def extract(out):
    found = {}
    for name in FILES:
        for _, kind, path, english, _ in segments(name, read(name)):
            found.setdefault(english, {'kind': kind, 'where': []})['where'].append(name + (':' + path if path else ''))
    with open(out, 'w', encoding='utf-8') as f:
        json.dump(found, f, ensure_ascii=False, indent=1)
    print(len(found), 'texts,', sum(1 for v in found.values() if v['kind'] == 'value'), 'of them values ->', out)


def lookup(translations, name, path, english):
    """A text's translation: one for that exact place ("file:path") wins over the text's own."""
    if path and (name + ':' + path) in translations:
        return translations[name + ':' + path]
    return translations.get(english)


def build_file(name, translations):
    lines = read(name).split('\n')
    missing = 0
    for number, _, path, english, rebuild in list(segments(name, '\n'.join(lines))):
        translated = lookup(translations, name, path, english)
        if translated is None or translated == '':
            missing += 1
            continue
        if translated != english:
            lines[number] = rebuild(translated)
    # Several values on one line (flow lists): rebuild again from the updated line
    text = '\n'.join(lines)
    for _ in range(4):
        changed = False
        for number, kind, path, english, rebuild in list(segments(name, text)):
            translated = lookup(translations, name, path, english)
            if translated and kind == 'value' and translated != english:
                current = text.split('\n')
                new_line = rebuild(translated)
                if new_line != current[number]:
                    current[number] = new_line
                    text = '\n'.join(current)
                    changed = True
        if not changed:
            break
    return text, missing


def build(translations_dir, out_dir, languages):
    version_path = os.path.join(out_dir, 'version.json')
    version = {}
    if os.path.exists(version_path):
        with open(version_path, encoding='utf-8') as f:
            version = json.load(f)
    known = version.setdefault('languages', {})
    for language in languages:
        with open(os.path.join(translations_dir, language + '.json'), encoding='utf-8') as f:
            data = json.load(f)
        translations = data['texts']
        target = os.path.join(out_dir, language)
        os.makedirs(target, exist_ok=True)
        total_missing = 0
        digest = hashlib.sha256()
        for name in FILES:
            text, missing = build_file(name, translations)
            total_missing += missing
            digest.update(name.encode('utf-8') + b'\0' + text.encode('utf-8'))
            with open(os.path.join(target, name), 'w', encoding='utf-8', newline='\n') as f:
                f.write(text)
        # A server re-downloads a language when its version goes up, so only bump it when the output changed.
        old = known.get(language, {})
        number = int(old.get('version', 0))
        if old.get('hash') != digest.hexdigest():
            number += 1
        known[language] = {'name': data['name'], 'lang': data['lang'], 'version': number, 'hash': digest.hexdigest()}
        print(language, 'built,', total_missing, 'texts left in English, version', number)
    version.pop('version', None)
    version['files'] = FILES
    with open(version_path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(version, f, ensure_ascii=False, indent=2)
        f.write('\n')


PLACEHOLDER = re.compile(r'%[A-Za-z0-9_<>|-]+%')
COLOUR = re.compile(r'&[0-9a-fk-orA-FK-OR]|&#[0-9A-Fa-f]{6}')


def leaves(node, path=''):
    if isinstance(node, dict):
        for k, v in node.items():
            yield from leaves(v, path + '.' + str(k) if path else str(k))
    elif isinstance(node, list):
        if all(not isinstance(v, (dict, list)) for v in node):
            yield path, node
        else:
            for i, v in enumerate(node):
                yield from leaves(v, path + '[%d]' % i)
    else:
        yield path, node


def check(out_dir, languages):
    problems = 0
    for language in languages:
        for name in FILES:
            english = dict(leaves(yaml.safe_load(read(name))))
            try:
                with open(os.path.join(out_dir, language, name), encoding='utf-8') as f:
                    translated = dict(leaves(yaml.safe_load(f.read())))
            except yaml.YAMLError as e:
                print(language, name, 'is not valid YAML:', e)
                problems += 1
                continue
            if set(english) != set(translated):
                print(language, name, 'keys differ:', sorted(set(english) ^ set(translated))[:5])
                problems += 1
            for path, value in english.items():
                other = translated.get(path)
                values = list(zip(value, other)) if isinstance(value, list) and isinstance(other, list) else [(value, other)]
                if isinstance(value, list) and isinstance(other, list) and len(value) != len(other):
                    print(language, name, path, 'list length', len(value), '->', len(other))
                    problems += 1
                for a, b in values:
                    if not isinstance(a, str) or not isinstance(b, str):
                        if a != b:
                            print(language, name, path, 'setting changed:', a, '->', b)
                            problems += 1
                        continue
                    if sorted(PLACEHOLDER.findall(a)) != sorted(PLACEHOLDER.findall(b)):
                        print(language, name, path, 'placeholders differ:', a, '->', b)
                        problems += 1
                    if a.count('<center>') != b.count('<center>'):
                        print(language, name, path, '<center> differs:', b)
                        problems += 1
                    if sorted(COLOUR.findall(a)) != sorted(COLOUR.findall(b)):
                        print(language, name, path, 'colours differ:', a, '->', b)
                        problems += 1
    print(problems, 'problems')
    return problems


if __name__ == '__main__':
    command = sys.argv[1]
    if command == 'extract':
        extract(sys.argv[2])
    elif command == 'build':
        langs = sys.argv[4:] or [f[:-5] for f in os.listdir(sys.argv[2]) if f.endswith('.json')]
        build(sys.argv[2], sys.argv[3], langs)
    elif command == 'check':
        langs = sys.argv[3:] or [d for d in os.listdir(sys.argv[2]) if os.path.isdir(os.path.join(sys.argv[2], d))]
        sys.exit(1 if check(sys.argv[2], langs) else 0)
