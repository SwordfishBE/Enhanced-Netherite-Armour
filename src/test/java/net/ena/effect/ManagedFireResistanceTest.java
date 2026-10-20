package net.ena.effect;

import net.minecraft.SharedConstants;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagedFireResistanceTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void identicalForeignEffectsAreNotOwned() {
        assertFalse(ManagedFireResistance.isOwned(null));
        assertFalse(ManagedFireResistance.isOwned(
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 340, 0, false, false, false)));
        assertTrue(ManagedFireResistance.isOwned(new ManagedFireResistance(340)));
    }

    @Test
    void vanillaInPlacePotionMergeRelinquishesOwnership() {
        ManagedFireResistance effect = new ManagedFireResistance(340);
        assertTrue(effect.update(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200)));
        assertFalse(ManagedFireResistance.isOwned(effect));
        assertEquals(1200, effect.getDuration());
    }

    @Test
    void evenIdenticalOrShorterHiddenUpdatesRelinquishOwnership() {
        for (int duration : new int[]{100, 340, 1200}) {
            ManagedFireResistance effect = new ManagedFireResistance(340);
            effect.update(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false, false));
            assertFalse(ManagedFireResistance.isOwned(effect));
        }
    }

    @Test
    void copiedEffectsAreNotClaimed() {
        assertFalse(ManagedFireResistance.isOwned(new MobEffectInstance(new ManagedFireResistance(340))));
    }

    private MobEffectInstance roundTrip(MobEffectInstance effect) {
        Codec<MobEffectInstance> codec = ManagedEffectCodec.wrap(MobEffectInstance.CODEC);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        JsonElement encoded = codec.encodeStart(ops, effect).getOrThrow();
        return codec.parse(ops, encoded).getOrThrow();
    }

    @Test
    void ownershipSurvivesSavingAndLoadingWithoutExtendingDuration() {
        MobEffectInstance restored = roundTrip(new ManagedFireResistance(173));
        assertTrue(ManagedFireResistance.isOwned(restored));
        assertEquals(173, restored.getDuration());
        restored.update(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));
        assertFalse(ManagedFireResistance.isOwned(restored));
        assertFalse(ManagedFireResistance.isOwned(roundTrip(restored)));
    }

    @Test
    void serializationNeverClaimsForeignOrLegacyEffects() {
        MobEffectInstance external = new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 340, 0, false, false, false);
        assertFalse(ManagedFireResistance.isOwned(roundTrip(external)));
        MobEffectInstance speed = roundTrip(new MobEffectInstance(MobEffects.SPEED, 200, 1));
        assertTrue(speed.is(MobEffects.SPEED));
        assertEquals(1, speed.getAmplifier());
        assertFalse(ManagedFireResistance.isOwned(speed));
    }

    @Test
    void ownershipSurvivesNbtWorldStorage() {
        Codec<MobEffectInstance> codec = ManagedEffectCodec.wrap(MobEffectInstance.CODEC);
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        Tag encoded = codec.encodeStart(ops, new ManagedFireResistance(91)).getOrThrow();
        MobEffectInstance restored = codec.parse(ops, encoded).getOrThrow();
        assertTrue(ManagedFireResistance.isOwned(restored));
        assertEquals(91, restored.getDuration());
    }
}
