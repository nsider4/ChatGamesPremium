# Config file translations

The translated default config files that servers download when `lang` (or `config-language`) in
config.yml picks another language. Each `<code>.json` here is one language. It holds the language's
`name`, the Minecraft `lang` codes it covers and `texts`, a map from each English text to its
translation. A key `"<file>:<path>"` translates the text at one place only, for an English word
that needs two translations (the Fish race and the fish in the trivia).

Block, item and mob names in `words.yml` that are Minecraft names were filled in from Mojang's own
names for that language. A text missing from the map stays English.

The built files are published in [mine-translator](https://github.com/nsider4/mine-translator) under
`Configs/2.0/` (the `2.0` is the config format; a release that changes the file layout uses a new
folder, so older plugins keep downloading files they understand).

## Updating or adding a language

1. Edit `<code>.json`, or copy `en.json` to a new `<code>.json` and fill in `name`, `lang` and `texts`.
   `python scripts/config-translations.py extract <out.json>` lists every English text.
2. Build into the mine-translator checkout, then check it:

   ```
   python scripts/config-translations.py build translations/configs <mine-translator>/Configs/2.0
   python scripts/config-translations.py check <mine-translator>/Configs/2.0
   ```

   `check` makes sure every file keeps the English keys and settings, and every text keeps its
   `%placeholders%`, `<center>` tags and colour codes.
3. Commit and push mine-translator. Only the languages whose files changed get a new version, and
   servers using them update on their next start or `/chatgames reload`.

When a default config file changes in the plugin, rebuild every language the same way: new or
changed English texts stay English until their translation is added here.
