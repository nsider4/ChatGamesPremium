# Getting Started

## 1. Add the dependency

The API is published on [JitPack](https://jitpack.io/#nsider4/ChatGames-API). It only needs the
Spigot API, and it compiles for Java 8, so it fits any plugin from 1.8.8 to 26.x.

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.nsider4</groupId>
        <artifactId>ChatGames-API</artifactId>
        <version>2.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    compileOnly("com.github.nsider4:ChatGames-API:2.0.0")
}
```

> **Never shade the API into your jar.** ChatGames is already on the server and provides these
> classes. Use `provided` (Maven) or `compileOnly` (Gradle). A shaded copy gives you a second,
> empty `GameType` registry and confusing errors.

## 2. Depend on ChatGames in plugin.yml

```yaml
name: MyPlugin
main: com.example.myplugin.MyPlugin
version: 1.0.0
api-version: '1.13'
depend: [ChatGames-Premium]   # or softdepend, see below
folia-supported: true          # only if the rest of your plugin supports Folia
```

`depend` makes Bukkit enable ChatGames before your plugin, so the API is ready in your `onEnable`.

## 3. Get the API and register your content

```java
public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ChatGamesAPI api = ChatGamesAPI.get();
        api.registerGame(this, /* a ChatGame */);
        api.registerRace(this, /* a RaceGame */);
        api.registerRewardAction(this, "mycoins", (player, amount) -> { /* ... */ });
        api.registerChallengeType(this, "boss_kills");
    }
}
```

`ChatGamesAPI.get()` throws `IllegalStateException` if ChatGames isn't enabled. You don't need to keep
the instance; calling `get()` again is cheap.

### Optional support (softdepend)

If your plugin should also run without ChatGames, use `softdepend: [ChatGames-Premium]` and keep every
reference to the API in a separate class, so the classes are only loaded when ChatGames is there:

```java
// MyPlugin.java
@Override
public void onEnable() {
    if (getServer().getPluginManager().isPluginEnabled("ChatGames-Premium")) {
        ChatGamesHook.register(this);   // the only class that imports org.nsider.chatgames.api
    }
}
```

## 4. Cleaning up is automatic

Everything you register belongs to your plugin. When your plugin disables (server stop, `/reload`,
a plugin manager), ChatGames removes your games, races, reward actions and challenge types, and stops
your game if it is running. You don't need an `onDisable` for it.

The server owner's config sections for your games are **kept**, and so are players' stats, so
everything is still there when your plugin comes back.

## 5. Ids

Games and races get an **id**: 2 to 32 lower-case letters, digits or `_`, starting with a letter,
e.g. `capitals`, `boss_hunt`, `quiz2`. The id is used in:

- the config files (`games.yml` / `races.yml` / `messages.yml` / `rewards.yml` sections);
- the command `/chatgames <id>` and the permission `chatgames.start.<id>`;
- placeholders like `%chatgames_wins_<id>%`.

Pick ids that won't clash with other plugins, and never change them later: a new id is a new game,
with new config sections and empty stats. An id that is already a game (built-in or another
plugin's) or a `/chatgames` sub-command (`top`, `stop`, `menu`...) is refused with an
`IllegalArgumentException`.

## Next

- Make a [chat game](Chat-Games) or a [race](Races).
- Read [Threads and Folia](Threads-and-Folia) before calling the API from async code.
