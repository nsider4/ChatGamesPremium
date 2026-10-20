"""Boot a test server with the plugin, run console commands, stop it, print the plugin's log lines.

usage: python scripts/boot-test.py <server dir> <java exe> <server jar> <plugin jar> [command ...]

The server dir gets eula.txt, a flat-world server.properties (port 25599) and the
plugin copied into plugins/. Each command is typed into the console 3 seconds apart.

Server jars used for the 2.0.0 test matrix:
  Spigot 1.8.8 / 1.13 (Java 8), Spigot 1.20.1 (Java 17), Paper 1.21.1 (Java 21)
  Paper 26.x and Folia 26.x (Java 25):
    https://fill.papermc.io/v3/projects/<paper|folia>/versions/<version>/builds/latest
  Java 25: https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse
"""
import os, shutil, subprocess, sys, threading, time

server_dir, java, server_jar, plugin_jar, *commands = sys.argv[1:]
os.makedirs(os.path.join(server_dir, 'plugins'), exist_ok=True)
shutil.copy(plugin_jar, os.path.join(server_dir, 'plugins', 'ChatGames-Premium.jar'))
with open(os.path.join(server_dir, 'eula.txt'), 'w') as f:
    f.write('eula=true\n')
props = os.path.join(server_dir, 'server.properties')
if not os.path.exists(props):
    with open(props, 'w') as f:
        f.write('online-mode=false\nserver-port=25599\nlevel-type=flat\nspawn-monsters=false\n'
                'generate-structures=false\nview-distance=3\nsimulation-distance=3\n')

proc = subprocess.Popen([java, '-Xmx1G', '-Dcom.mojang.eula.agree=true', '-jar', server_jar, 'nogui'],
                        cwd=server_dir, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace')
lines = []
done = threading.Event()

def reader():
    for line in proc.stdout:
        lines.append(line.rstrip())
        if 'Done (' in line:
            done.set()

threading.Thread(target=reader, daemon=True).start()

def send(cmd):
    proc.stdin.write(cmd + '\n')
    proc.stdin.flush()

if not done.wait(600):
    print('SERVER DID NOT START')
    proc.kill()
else:
    time.sleep(3)
    for cmd in commands:
        lines.append('>>> ' + cmd)
        send(cmd)
        time.sleep(3)
    send('stop')
    try:
        proc.wait(120)
    except subprocess.TimeoutExpired:
        proc.kill()
        lines.append('SERVER DID NOT STOP')

time.sleep(1)
keep = ('chatgames', 'ChatGames', '>>> ', 'Exception', 'Error', 'ERROR', 'SEVERE', 'WARN', 'Done (', '\tat ',
        'Unknown', 'reloaded', 'permission', 'Caused by', 'Disabling', 'Enabling', 'DID NOT')
for line in lines:
    if any(k in line for k in keep):
        print(line)
