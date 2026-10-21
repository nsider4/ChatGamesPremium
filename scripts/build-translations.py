"""Rebuild the mine-translator language files from Mojang's official assets.

usage: python scripts/build-translations.py <output dir> [minecraft version]

Writes <output dir>/Languages/<code>.json for every language of that version (default: the
latest release), keeping only the keys ChatGames shows: blocks, items, mobs, enchantments and
effects. Each file carries a "mine-translator.version" key, and Languages/version.json names
the version, so the plugin re-downloads files built for an older one.
<output dir>/language_names.txt lists the codes with each language's own name.

The output dir is meant to be a clone of github.com/nsider4/mine-translator; commit and push
it there yourself.
"""
import io, json, os, sys, urllib.request, zipfile

MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
RESOURCES = 'https://resources.download.minecraft.net/'
PREFIXES = ('block.minecraft.', 'item.minecraft.', 'entity.minecraft.', 'enchantment.minecraft.',
            'effect.minecraft.')
VERSION_KEY = 'mine-translator.version'


def fetch(url):
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()


def fetch_json(url):
    return json.loads(fetch(url).decode('utf-8'))


def keep(lang):
    return {k: v for k, v in lang.items() if k.startswith(PREFIXES)}


def main(out_dir, version=None):
    manifest = fetch_json(MANIFEST)
    version = version or manifest['latest']['release']
    entry = next((v for v in manifest['versions'] if v['id'] == version), None)
    if entry is None:
        sys.exit('Unknown Minecraft version: ' + version)
    meta = fetch_json(entry['url'])
    objects = fetch_json(meta['assetIndex']['url'])['objects']

    languages = {}
    for path, obj in sorted(objects.items()):
        if path.startswith('minecraft/lang/') and path.endswith('.json'):
            code = path[len('minecraft/lang/'):-len('.json')]
            digest = obj['hash']
            languages[code] = json.loads(fetch(RESOURCES + digest[:2] + '/' + digest).decode('utf-8'))
            print('  ' + code, flush=True)
    # en_us isn't an asset: it ships inside the client jar
    print('  en_us (client jar)', flush=True)
    with zipfile.ZipFile(io.BytesIO(fetch(meta['downloads']['client']['url']))) as jar:
        languages['en_us'] = json.loads(jar.read('assets/minecraft/lang/en_us.json').decode('utf-8'))

    folder = os.path.join(out_dir, 'Languages')
    os.makedirs(folder, exist_ok=True)
    names = []
    for code, lang in sorted(languages.items()):
        kept = keep(lang)
        kept[VERSION_KEY] = version
        with open(os.path.join(folder, code + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            # One key per line keeps the git diffs between versions readable
            json.dump(kept, f, ensure_ascii=False, indent=0, sort_keys=True)
            f.write('\n')
        names.append('%s\t%s (%s)' % (code, lang.get('language.name', code), lang.get('language.region', '')))
    with open(os.path.join(folder, 'version.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'version': version, 'languages': sorted(languages)}, f, indent=2)
        f.write('\n')
    with open(os.path.join(out_dir, 'language_names.txt'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(names) + '\n')
    print('%d languages for %s written to %s' % (len(languages), version, folder))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
