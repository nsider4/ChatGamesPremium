"""Checks the XSeries classes the pom leaves out of the shaded jar are still safe to leave out.

minimizeJar keeps every class a kept class names. The pom then drops a few XSeries classes that
are only named from code nothing calls. This lists every reference from a class in the jar to an
XSeries class that isn't there, and fails if any comes from somewhere other than the known places.
Run it after changing the XSeries (or InventoryManager) version.

usage: python scripts/xseries-check.py [jar]     needs jdeps (any JDK) on the PATH
"""
import re
import subprocess
import sys
import zipfile

JAR = sys.argv[1] if len(sys.argv) > 1 else 'target/ChatGames-Premium-2.0.0.jar'
XSERIES = 'org.nsider.chatgames.lib.xseries.'

# Where a missing class may be named from, and the method that names it (none of them is called)
ALLOWED = {
    'base.XRegistry': 'ensureLoadedRegistries(), only reached through registryOf()/rawRegistryOf()',
    'reflection.XReflection': 'proxify()',
}

in_jar = {name[:-6].replace('/', '.') for name in zipfile.ZipFile(JAR).namelist() if name.endswith('.class')}
out = subprocess.run(['jdeps', '-verbose:class', '-filter:none', JAR], capture_output=True, text=True).stdout

problems, allowed = [], set()
source = None
for line in out.splitlines():
    header = re.match(r'^   (\S+) \(', line)
    if header:
        source = header.group(1)
        continue
    dep = re.match(r'^      -> (\S+)', line)
    if not dep or not source or not dep.group(1).startswith(XSERIES) or dep.group(1) in in_jar:
        continue
    owner = source.split('$')[0][len(XSERIES):] if source.startswith(XSERIES) else source
    if owner in ALLOWED:
        allowed.add('%s -> %s' % (owner, dep.group(1)[len(XSERIES):]))
    else:
        problems.append('%s -> %s' % (source, dep.group(1)))

for ref in sorted(allowed):
    print('ok (never called)', ref)
for ref in problems:
    print('MISSING', ref)
print('%d known references, %d problems' % (len(allowed), len(problems)))
sys.exit(1 if problems else 0)
