package net.ena.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ena.util.AnimalLavaProtection;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GroundPathNavigation.class)
public abstract class GroundPathNavigationMixin extends PathNavigation {
    protected GroundPathNavigationMixin(Mob mob, Level level) {
        super(mob, level);
    }

    @WrapOperation(method = "getSurfaceY", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;isInFloatableFluid()Z"))
    private boolean ena$useLavaSurface(Mob entity, Operation<Boolean> original) {
        return original.call(entity)
                || entity.isInLava() && AnimalLavaProtection.isProtected(entity);
    }

    @WrapOperation(method = "getSurfaceY", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean ena$scanProtectedLavaColumn(FluidState state, TagKey<Fluid> tag, Operation<Boolean> original) {
        // Keep vanilla's surface scan and depth limit, including mixed water/lava boundaries.
        return original.call(state, tag)
                || tag.equals(FluidTags.ENTITY_FLOATABLE) && state.is(FluidTags.LAVA)
                && AnimalLavaProtection.isProtected(mob);
    }
}
