"""Rebuild the mine-translator language files from Mojang's official assets.

usage: python scripts/build-translations.py <output dir> [minecraft version]

Writes <output dir>/Languages/<code>.json for every language of that version (default: the
latest release), keeping only the keys ChatGames shows: blocks, items, mobs, enchantments and
effects. Each file carries a "mine-translator.version" key, and Languages/version.json names
the version, so the plugin re-downloads files built for an older one.

Files already in the output dir are merged, never shrunk: the new names win, but a key that
was published before is kept, and so is a language Mojang no longer ships. ChatGames 1.9.9
downloads the same files and fails on a missing mob key, and servers older than the build
still use keys Mojang may have dropped.
<output dir>/language_names.txt lists the codes with each language's own name.

The output dir is meant to be a clone of github.com/nsider4/mine-translator; commit and push
it there yourself.
"""
import http.client, io, json, os, sys, time, urllib.request, zipfile
from concurrent.futures import ThreadPoolExecutor

MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
RESOURCES = 'https://resources.download.minecraft.net/'
PREFIXES = ('block.minecraft.', 'item.minecraft.', 'entity.minecraft.', 'enchantment.minecraft.',
            'effect.minecraft.')
VERSION_KEY = 'mine-translator.version'
# Listed first in language_names.txt, in this order (the order the file has always had)
COMMON = ['en_us', 'en_gb', 'de_de', 'es_es', 'es_mx', 'fr_fr', 'fr_ca', 'it_it', 'ja_jp', 'ko_kr', 'pt_br', 'pt_pt',
          'ru_ru', 'zh_cn', 'zh_tw', 'nl_nl', 'bg_bg', 'cs_cz', 'da_dk', 'el_gr', 'fi_fi', 'hu_hu', 'id_id', 'nb_no',
          'pl_pl', 'sk_sk', 'sv_se', 'tr_tr', 'uk_ua', 'tl_ph']


def fetch(url, attempts=5):
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(url, timeout=120) as response:
                return response.read()
        except (OSError, http.client.HTTPException):
            # Mojang's CDN drops the odd connection
            if attempt == attempts - 1:
                raise
            time.sleep(2 * (attempt + 1))


def fetch_json(url):
    return json.loads(fetch(url).decode('utf-8'))


def keep(lang):
    return {k: v for k, v in lang.items() if k.startswith(PREFIXES)}


def previous(path):
    if not os.path.exists(path):
        return {}
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def main(out_dir, version=None):
    manifest = fetch_json(MANIFEST)
    version = version or manifest['latest']['release']
    entry = next((v for v in manifest['versions'] if v['id'] == version), None)
    if entry is None:
        sys.exit('Unknown Minecraft version: ' + version)
    meta = fetch_json(entry['url'])
    objects = fetch_json(meta['assetIndex']['url'])['objects']

    hashes = {path[len('minecraft/lang/'):-len('.json')]: obj['hash'] for path, obj in objects.items()
              if path.startswith('minecraft/lang/') and path.endswith('.json')}

    def download(code):
        digest = hashes[code]
        lang = json.loads(fetch(RESOURCES + digest[:2] + '/' + digest).decode('utf-8'))
        print('  ' + code, flush=True)
        return code, lang

    with ThreadPoolExecutor(8) as pool:
        languages = dict(pool.map(download, sorted(hashes)))
    # en_us isn't an asset: it ships inside the client jar
    print('  en_us (client jar)', flush=True)
    with zipfile.ZipFile(io.BytesIO(fetch(meta['downloads']['client']['url']))) as jar:
        languages['en_us'] = json.loads(jar.read('assets/minecraft/lang/en_us.json').decode('utf-8'))

    folder = os.path.join(out_dir, 'Languages')
    os.makedirs(folder, exist_ok=True)
    for name in os.listdir(folder):
        code = name[:-len('.json')]
        if name.endswith('.json') and name != 'version.json' and code not in languages:
            languages[code] = {}
    names = {}
    for code, lang in sorted(languages.items()):
        kept = keep(previous(os.path.join(folder, code + '.json')))
        kept.update(keep(lang))
        kept[VERSION_KEY] = version
        with open(os.path.join(folder, code + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            # One key per line keeps the git diffs between versions readable
            json.dump(kept, f, ensure_ascii=False, indent=0, sort_keys=True)
            f.write('\n')
        if 'language.name' in lang:
            names[code] = '%s\t%s (%s)' % (code, lang['language.name'], lang.get('language.region', ''))
    with open(os.path.join(folder, 'version.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'version': version, 'languages': sorted(languages)}, f, indent=2)
        f.write('\n')
    common = [names[code] for code in COMMON if code in names]
    with open(os.path.join(out_dir, 'language_names.txt'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('All currently supported languages (Minecraft %s):\n\nCommon Languages\n\n' % version)
        f.write('\n'.join(common) + '\n\nAll Languages\n\n' + '\n'.join(names[code] for code in sorted(names)) + '\n')
    print('%d languages for %s written to %s' % (len(languages), version, folder))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
