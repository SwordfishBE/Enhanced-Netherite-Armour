package net.ena.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Ownership is explicit, including when restored by the ENA save codec. */
public final class ManagedFireResistance extends MobEffectInstance {
    public static final int DURATION_TICKS = 340;
    private boolean owned = true;

    public ManagedFireResistance(int duration) {
        super(MobEffects.FIRE_RESISTANCE, duration, 0, false, false, false);
    }

    @Override
    public boolean update(MobEffectInstance incoming) {
        // Vanilla merges potions into the existing object, so identity alone is not ownership.
        owned = false;
        return super.update(incoming);
    }

    public static boolean isOwned(MobEffectInstance effect) {
        return effect instanceof ManagedFireResistance managed && managed.owned;
    }
}
