package net.ena.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** This entrypoint deliberately has no references to optional Cloth Config classes. */
public final class ClothNoticeClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var parent = new TitleScreen();
            var screen = new EnhancedNetheriteArmourModMenuIntegration().getModConfigScreenFactory().create(parent);
            if (!(screen instanceof AlertScreen)) {
                throw new AssertionError("Missing or old Cloth Config must show a safe notice");
            }
            client.setScreenAndShow(screen);
        });
        context.waitTicks(3);
        context.runOnClient(client -> {
            client.gui.screen().onClose();
            if (!(client.gui.screen() instanceof TitleScreen)) {
                throw new AssertionError("Notice must return to its parent screen");
            }
        });
        try {
            String version = FabricLoader.getInstance().getModContainer("cloth-config")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("absent");
            Files.writeString(Path.of("cloth-notice-test-passed.txt"), "PASS: rendered notice and returned to parent; Cloth Config=" + version);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
