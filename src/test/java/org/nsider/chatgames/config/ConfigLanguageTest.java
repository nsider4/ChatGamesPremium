package org.nsider.chatgames.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Switching the config files' language: what is kept, what is translated. */
class ConfigLanguageTest {

    private static final String ENGLISH = lines(
            "# How long a round lasts",
            "math:",
            "  time: 20",
            "  title: 'Solve it!'",
            "  words:",
            "  - 'Apple'",
            "  - 'Bread'",
            "trivia:",
            "  '1':",
            "    question: 'Who made Minecraft?'",
            "  '2':",
            "    question: 'What is red?'");
    private static final String SPANISH = lines(
            "# Cuánto dura una ronda",
            "math:",
            "  time: 20",
            "  title: '¡Resuélvelo!'",
            "  words:",
            "  - 'Manzana'",
            "  - 'Pan'",
            "trivia:",
            "  '1':",
            "    question: '¿Quién creó Minecraft?'",
            "  '2':",
            "    question: '¿Qué es rojo?'");

    @Test
    void untouchedFilesBecomeTheTranslationExactly() throws IOException {
        ConfigLanguage.Merge merge = ConfigLanguage.merge(ENGLISH, ENGLISH, ENGLISH, SPANISH, true);
        assertEquals(SPANISH, merge.text);
        assertEquals(0, merge.replaced);
    }

    @Test
    void settingsTheOwnerChangedAreKeptAndCommentsStay() throws IOException {
        String mine = ENGLISH.replace("time: 20", "time: 45");
        ConfigLanguage.Merge merge = ConfigLanguage.merge(mine, ENGLISH, ENGLISH, SPANISH, true);
        assertEquals(SPANISH.replace("time: 20", "time: 45"), merge.text, "only that line changes, comments included");
    }

    @Test
    void switchingReplacesTheOwnersOwnTextsAndCountsThem() throws IOException {
        String mine = ENGLISH.replace("'Solve it!'", "'Be quick!'");
        ConfigLanguage.Merge merge = ConfigLanguage.merge(mine, ENGLISH, ENGLISH, SPANISH, true);
        assertEquals("¡Resuélvelo!", yaml(merge.text).getString("math.title"));
        assertEquals(1, merge.replaced);
    }

    @Test
    void anUpdateOfTheSameLanguageKeepsTheOwnersTexts() throws IOException {
        String improved = SPANISH.replace("'¡Resuélvelo!'", "'¡Resuélvelo ya!'").replace("'Pan'", "'Pan de molde'");
        String mine = SPANISH.replace("'¡Resuélvelo!'", "'¡Rápido!'");
        ConfigLanguage.Merge merge = ConfigLanguage.merge(mine, SPANISH, ENGLISH, improved, false);
        YamlConfiguration result = yaml(merge.text);
        assertEquals("¡Rápido!", result.getString("math.title"), "the owner's text wins in an update");
        assertEquals(Arrays.asList("Manzana", "Pan de molde"), result.getStringList("math.words"), "untouched texts get the fix");
        assertEquals(0, merge.replaced);
    }

    @Test
    void addedAndRemovedKeysStayThatWay() throws IOException {
        String mine = lines(
                "math:",
                "  time: 20",
                "  title: 'Solve it!'",
                "  words:",
                "  - 'Apple'",
                "  - 'Bread'",
                "  custom: 'mine'",
                "trivia:",
                "  '1':",
                "    question: 'Who made Minecraft?'");
        YamlConfiguration result = yaml(ConfigLanguage.merge(mine, ENGLISH, ENGLISH, SPANISH, true).text);
        assertEquals("mine", result.getString("math.custom"));
        assertEquals("¿Quién creó Minecraft?", result.getString("trivia.1.question"));
        assertFalse(result.contains("trivia.2"), "a question the owner deleted doesn't come back");
    }

    @Test
    void sectionsAnUpdateAddedInEnglishGetTranslated() throws IOException {
        String section = lines("shear:", "  title: 'Shear it!'", "  time: 30");
        String spanishSection = lines("shear:", "  title: '¡Esquílalo!'", "  time: 30");
        // A 2.0 Spanish file that the plugin update gave an English section; the owner changed its time
        String mine = SPANISH + section.replace("time: 30", "time: 40");
        ConfigLanguage.Merge merge = ConfigLanguage.merge(mine, SPANISH, ENGLISH + section, SPANISH + spanishSection, false);
        YamlConfiguration result = yaml(merge.text);
        assertEquals("¡Esquílalo!", result.getString("shear.title"));
        assertEquals(40, result.getInt("shear.time"), "the owner's change in it stays");
        assertEquals("¡Resuélvelo!", result.getString("math.title"));
    }

    @Test
    void aLangCodeFindsItsLanguage() {
        JsonObject version = parse("{\"version\": 1, \"languages\": {"
                + "\"es\": {\"name\": \"Español\", \"lang\": [\"es_es\", \"es_mx\"], \"version\": 3},"
                + "\"pt\": {\"name\": \"Português\", \"lang\": [\"pt_br\", \"pt_pt\"]}}}");
        Map<String, ConfigLanguage.Language> languages = ConfigLanguage.parseLanguages(version);
        assertEquals(3, languages.get("es").version);
        assertEquals(0, languages.get("pt").version);
        assertEquals("es", ConfigLanguage.codeFor("es_mx", languages));
        assertEquals("es", ConfigLanguage.codeFor("es_ar", languages), "by the start of the code");
        assertEquals("pt", ConfigLanguage.codeFor("pt_pt", languages));
        assertEquals("en", ConfigLanguage.codeFor("en_gb", languages), "English is always there (the jar)");
        assertNull(ConfigLanguage.codeFor("ja_jp", languages));
        assertTrue(ConfigLanguage.parseLanguages(parse("{\"languages\": {}}")).isEmpty());
        assertEquals(Collections.singletonList("es_es"),
                ConfigLanguage.parseLanguages(parse("{\"languages\": {\"es\": {\"name\": \"E\", \"lang\": [\"ES_ES\"]}}}")).get("es").lang);
    }

    @SuppressWarnings("deprecation") // the instance parser is the only one old Gson has
    private static JsonObject parse(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private static YamlConfiguration yaml(String text) throws IOException {
        return YamlText.parse(text, "test");
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }
}
