package org.nsider.chatgames.managers;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageManagerTest {

    @Test
    void olderOrUnversionedFilesAreDownloadedAgain() {
        assertTrue(LanguageManager.needsUpdate(null, "26.3"), "files from before versions were added");
        assertTrue(LanguageManager.needsUpdate("26.1", "26.3"));
        assertFalse(LanguageManager.needsUpdate("26.3", "26.3"));
        assertFalse(LanguageManager.needsUpdate("26.3", null), "offline, or no version.json: keep the cached file");
        assertFalse(LanguageManager.needsUpdate(null, null));
    }

    @Test
    void versionIsReadFromTheFile() {
        JsonObject data = new JsonObject();
        data.addProperty("block.minecraft.stone", "Piedra");
        assertNull(LanguageManager.versionOf(data));
        data.addProperty(LanguageManager.VERSION_KEY, "26.3");
        assertEquals("26.3", LanguageManager.versionOf(data));
    }
}
