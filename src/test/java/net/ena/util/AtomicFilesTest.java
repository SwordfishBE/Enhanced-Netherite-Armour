package net.ena.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AtomicFilesTest {
    @TempDir
    Path directory;

    @Test
    void replacesExistingFileAndCleansTemporaryFiles() throws Exception {
        Path target = directory.resolve("settings.json");
        Files.writeString(target, "old");
        AtomicFiles.writeString(target, "new");
        assertEquals("new", Files.readString(target));
        try (var entries = Files.list(directory)) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    void failedReplacementPreservesTargetAndCleansTemporaryFiles() throws Exception {
        Path target = directory.resolve("settings.json");
        Files.createDirectory(target);
        Files.writeString(target.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> AtomicFiles.writeString(target, "new"));
        assertEquals("keep", Files.readString(target.resolve("keep.txt")));
        try (var entries = Files.list(directory)) {
            assertEquals(1, entries.count());
        }
    }
}
