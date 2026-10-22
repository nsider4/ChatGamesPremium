"""Copies the developer API into a checkout of the public nsider4/ChatGames-API repository.

usage: python scripts/publish-api.py <ChatGames-API checkout>
       python scripts/publish-api.py --wiki <ChatGames-API.wiki checkout>

The API is the org.nsider.chatgames.api package (ApiBoundaryTest keeps it to Bukkit and the JDK).
The checkout gets those sources, plus everything in api-repo/ (pom, README, jitpack.yml and the
example plugin). API sources that no longer exist here are deleted there. The version in both
poms is set to this plugin's version, so tag the API repo with that version for JitPack.
Nothing is committed or pushed.

--wiki copies the pages in api-repo/wiki/ into a checkout of the repository's GitHub wiki
(https://github.com/nsider4/ChatGames-API.wiki.git) and deletes pages that no longer exist here.
"""
import os, re, shutil, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
API = os.path.join('src', 'main', 'java', 'org', 'nsider', 'chatgames', 'api')
TEMPLATE = os.path.join(ROOT, 'api-repo')
WIKI = os.path.join(TEMPLATE, 'wiki')


def plugin_version():
    pom = open(os.path.join(ROOT, 'pom.xml'), encoding='utf-8').read()
    # The project's own version: the first <version> right after an <artifactId>
    match = re.search(r'<artifactId>[^<]+</artifactId>\s*<version>([^<]+)</version>', pom)
    if not match:
        sys.exit('no version in pom.xml')
    return match.group(1)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as out:
        out.write(text.replace('\r\n', '\n'))


def publish_wiki(target):
    pages = set()
    for name in os.listdir(WIKI):
        if name.endswith('.md'):
            write(os.path.join(target, name), open(os.path.join(WIKI, name), encoding='utf-8').read())
            pages.add(name)
    removed = 0
    for name in os.listdir(target):
        if name.endswith('.md') and name not in pages:
            os.remove(os.path.join(target, name))
            removed += 1
    print('copied %d wiki pages, removed %d' % (len(pages), removed))


def main():
    wiki = len(sys.argv) == 3 and sys.argv[1] == '--wiki'
    if len(sys.argv) != 2 and not wiki:
        sys.exit(__doc__)
    target = sys.argv[-1]
    if not os.path.isdir(os.path.join(target, '.git')):
        sys.exit(target + ' is not a git checkout')
    if wiki:
        publish_wiki(target)
        return
    version = plugin_version()

    copied = set()
    for base, dirs, files in os.walk(TEMPLATE):
        # The wiki is its own repository (see --wiki)
        dirs[:] = [d for d in dirs if d != 'target' and os.path.join(base, d) != WIKI]
        for name in files:
            source = os.path.join(base, name)
            relative = os.path.relpath(source, TEMPLATE)
            text = open(source, encoding='utf-8').read()
            if name == 'pom.xml':
                # The API's own version, and the example's dependency on it
                text = re.sub(r'(<artifactId>ChatGames-API</artifactId>\s*<version>)[^<]+', r'\g<1>' + version, text)
            write(os.path.join(target, relative), text)
            copied.add(os.path.normcase(relative))

    sources = os.path.join(ROOT, API)
    for base, _, files in os.walk(sources):
        for name in files:
            source = os.path.join(base, name)
            relative = os.path.join(API, os.path.relpath(source, sources))
            write(os.path.join(target, relative), open(source, encoding='utf-8').read())
            copied.add(os.path.normcase(relative))

    removed = 0
    for base, _, files in os.walk(os.path.join(target, API)):
        for name in files:
            path = os.path.join(base, name)
            if os.path.normcase(os.path.relpath(path, target)) not in copied:
                os.remove(path)
                removed += 1
    print('copied %d files (version %s), removed %d stale API files' % (len(copied), version, removed))


if __name__ == '__main__':
    main()
