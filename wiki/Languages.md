# Languages

There are two separate things: the names of items, blocks and mobs, and the config files.

## Item, block and mob names

```yaml
# config.yml
lang: 'en_us'
```

Races and Memorize show names like "Grass Block" or "Zombie". They're translated into `lang`, in
over 140 languages ([the codes](https://github.com/nsider4/mine-translator/blob/main/language_names.txt)).
The names are downloaded into `translations/` and updated by themselves when a newer Minecraft
version comes out. A change applies on `/chatgames reload`.

## Config files

The messages, menus, word lists, trivia and comments come fully translated in 20 languages:

`en`, `es`, `fr`, `pt`, `de`, `it`, `nl`, `pl`, `ru`, `tr`, `zh` (Simplified Chinese), `zh_tw`
(Traditional Chinese), `ja`, `ko`, `uk`, `cs`, `sv`, `hu`, `vi`, `id`

```yaml
# config.yml
config-language: 'auto'
```

| Value | What happens |
|---|---|
| `auto` | the files switch when `lang` changes, if that language is fully translated |
| `none` | the files never change language |
| a code above | the files are in that language, whatever `lang` is |

When the files switch:
- your settings, and every change you made to them, are kept;
- your own messages are replaced by the translation;
- the old files are copied to `backups/` first.

If your `lang` isn't one of the 20, item, block and mob names are still translated, and the
config files keep their language (English by default).

Translations come from [mine-translator](https://github.com/nsider4/mine-translator) (folder
`Configs`) and are updated by themselves.
