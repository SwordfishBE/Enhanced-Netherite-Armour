package net.ena.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EnaConfigTest {
    @TempDir
    Path directory;

    @Test
    void missingFileCreatesEnabledDefaults() throws Exception {
        Path path = directory.resolve("ena.json");
        assertTrue(EnaConfig.load(path));
        assertTrue(EnaConfig.get().enabled);
        assertTrue(EnaConfig.get().horseFireProtection);
        assertFalse(EnaConfig.get().wolfFireProtection);
        assertTrue(Files.readString(path).contains("\"wolfFireProtection\": false"));
        assertTrue(Files.readString(path).contains("//"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{broken", "", "null", "[]", "{\"enabled\":null}",
            "{\"useLuckPerms\":\"false\"}", "{} /* unclosed", "{\"enabled\":tr/*note*/ue}"})
    void rejectedReloadPreservesPermissionsAndFile(String invalid) throws Exception {
        Path path = directory.resolve("ena.json");
        Files.writeString(path, "{\"enabled\":false,\"useLuckPerms\":true}");
        assertTrue(EnaConfig.load(path));
        EnaConfig previous = EnaConfig.get();
        Files.writeString(path, invalid);
        assertFalse(EnaConfig.load(path));
        assertSame(previous, EnaConfig.get());
        assertTrue(EnaConfig.get().useLuckPerms);
        assertFalse(EnaConfig.get().enabled);
        assertEquals(invalid, Files.readString(path));
    }

    @Test
    void commentsDoNotBreakQuotedStringsAndValidFilesAreNotRewritten() throws Exception {
        Path path = directory.resolve("ena.json");
        String text = "// settings\n{\"enabled\":false, /* note */ \"note\":\"https://example.test/*text*/\"}";
        Files.writeString(path, text);
        assertTrue(EnaConfig.load(path));
        assertFalse(EnaConfig.get().enabled);
        assertEquals(text, Files.readString(path));
        assertNotSame(EnaConfig.get(), EnaConfig.loadForEditing());
        assertFalse(EnaConfig.get().wolfFireProtection);
    }

    @Test
    void enabledWolfProtectionSurvivesLoadingAndEditorCopy() {
        assertTrue(EnaConfig.parse("{\"wolfFireProtection\":true}").copy().wolfFireProtection);
    }

    @Test
    void unreadablePathKeepsActiveConfig() throws Exception {
        Path path = directory.resolve("ena.json");
        Files.writeString(path, "{\"enabled\":false}");
        assertTrue(EnaConfig.load(path));
        EnaConfig previous = EnaConfig.get();
        assertFalse(EnaConfig.load(directory));
        assertSame(previous, EnaConfig.get());
    }
}
