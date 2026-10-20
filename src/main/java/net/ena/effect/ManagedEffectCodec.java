package net.ena.effect;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ManagedEffectCodec {
    private static final String OWNER_KEY = "enhancednetheritearmour:owned";

    private ManagedEffectCodec() {
    }

    public static Codec<MobEffectInstance> wrap(Codec<MobEffectInstance> vanilla) {
        return vanilla.mapResult(new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<MobEffectInstance, T>> apply(
                    DynamicOps<T> ops, T input, DataResult<Pair<MobEffectInstance, T>> result) {
                boolean owned = ops.get(input, OWNER_KEY).flatMap(ops::getBooleanValue).result().orElse(false);
                return result.map(pair -> pair.mapFirst(effect -> owned && canRestore(effect)
                        ? new ManagedFireResistance(effect.getDuration()) : effect));
            }

            @Override
            public <T> DataResult<T> coApply(
                    DynamicOps<T> ops, MobEffectInstance effect, DataResult<T> result) {
                if (!ManagedFireResistance.isOwned(effect)) {
                    return result;
                }
                return result.flatMap(encoded ->
                        ops.mergeToMap(encoded, ops.createString(OWNER_KEY), ops.createBoolean(true)));
            }
        });
    }

    private static boolean canRestore(MobEffectInstance effect) {
        return effect.is(MobEffects.FIRE_RESISTANCE)
                && effect.getAmplifier() == 0
                && !effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon()
                && effect.getDuration() > 0
                && effect.getDuration() <= ManagedFireResistance.DURATION_TICKS;
    }
}
