package net.ena.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ena.EnhancedNetheriteArmour;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModrinthUpdateChecker {

    private static final String PROJECT_ID = "lZiW97rL";
    private static final String RELEASE = "release";
    private static final String FABRIC = "fabric";
    private static final String MINECRAFT_MOD_ID = "minecraft";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private static final AtomicBoolean CHECK_STARTED = new AtomicBoolean(false);

    private ModrinthUpdateChecker() {
    }

    public static void checkOnceAsync() {
        if (!CHECK_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(ModrinthUpdateChecker::checkForUpdate, "enhancednetheritearmour-modrinth-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private static void checkForUpdate() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", EnhancedNetheriteArmour.LOG_NAME + "/" + EnhancedNetheriteArmour.MOD_VERSION)
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                EnhancedNetheriteArmour.LOGGER.debug("{} Update check returned HTTP {}.",
                        EnhancedNetheriteArmour.prefix(), response.statusCode());
                return;
            }

            Optional<VersionCandidate> latestVersion = extractLatestVersion(response.body(), currentMinecraftVersion());
            if (latestVersion.isEmpty()) {
                EnhancedNetheriteArmour.LOGGER.debug("{} Update check returned no usable versions.",
                        EnhancedNetheriteArmour.prefix());
                return;
            }

            String newestVersion = latestVersion.get().versionNumber();
            if (isNewerVersion(newestVersion, EnhancedNetheriteArmour.MOD_VERSION)) {
                EnhancedNetheriteArmour.LOGGER.warn("{} New version available: {} (current: {}).",
                        EnhancedNetheriteArmour.prefix(), newestVersion, EnhancedNetheriteArmour.MOD_VERSION);
            } else {
                EnhancedNetheriteArmour.LOGGER.debug("{} No newer version available (current: {}, latest compatible: {}).",
                        EnhancedNetheriteArmour.prefix(), EnhancedNetheriteArmour.MOD_VERSION, newestVersion);
            }
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            EnhancedNetheriteArmour.LOGGER.debug("{} Update check failed.",
                    EnhancedNetheriteArmour.prefix(), exception);
        }
    }

    private static Optional<VersionCandidate> extractLatestVersion(String responseBody, String minecraftVersion) {
        JsonElement root = JsonParser.parseString(responseBody);
        if (!root.isJsonArray()) {
            return Optional.empty();
        }

        JsonArray versions = root.getAsJsonArray();
        VersionCandidate newestCompatibleRelease = null;
        VersionCandidate newestRelease = null;

        for (JsonElement versionElement : versions) {
            if (!versionElement.isJsonObject()) {
                continue;
            }

            JsonObject versionObject = versionElement.getAsJsonObject();
            String versionNumber = getString(versionObject, "version_number");
            if (versionNumber == null || versionNumber.isBlank()) {
                continue;
            }

            Instant publishedAt = getPublishedAt(versionObject);
            if (publishedAt == null) {
                continue;
            }

            String versionType = getString(versionObject, "version_type");
            if (!RELEASE.equalsIgnoreCase(versionType)) {
                continue;
            }

            VersionCandidate candidate = new VersionCandidate(versionNumber, publishedAt);
            newestRelease = newestOf(newestRelease, candidate);

            if (jsonArrayContains(versionObject, "loaders", FABRIC)
                    && jsonArrayContains(versionObject, "game_versions", minecraftVersion)) {
                newestCompatibleRelease = newestOf(newestCompatibleRelease, candidate);
            }
        }

        return Optional.ofNullable(newestCompatibleRelease != null ? newestCompatibleRelease : newestRelease);
    }

    private static String getString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }

        return value.getAsString();
    }

    private static boolean jsonArrayContains(JsonObject object, String key, String expectedValue) {
        if (expectedValue == null || expectedValue.isBlank()) {
            return false;
        }

        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            return false;
        }

        for (JsonElement element : value.getAsJsonArray()) {
            if (element != null && element.isJsonPrimitive() && expectedValue.equalsIgnoreCase(element.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private static Instant getPublishedAt(JsonObject object) {
        String publishedAt = getString(object, "date_published");
        if (publishedAt == null || publishedAt.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(publishedAt);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static VersionCandidate newestOf(VersionCandidate first, VersionCandidate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        return second.publishedAt().isAfter(first.publishedAt()) ? second : first;
    }

    private static String currentMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MINECRAFT_MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static boolean isNewerVersion(String candidate, String current) {
        try {
            Version candidateVersion = Version.parse(candidate);
            Version currentVersion = Version.parse(current);
            return candidateVersion.compareTo(currentVersion) > 0;
        } catch (VersionParsingException exception) {
            EnhancedNetheriteArmour.LOGGER.debug("{} Could not compare versions '{}' and '{}'.",
                    EnhancedNetheriteArmour.prefix(), candidate, current, exception);
            return false;
        }
    }

    private record VersionCandidate(String versionNumber, Instant publishedAt) {
    }
}
