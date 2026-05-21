package net.minecraft.block;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code FluidDrainable}.
 */
public interface FluidDrainable {

	ItemStack tryDrainFluid(@Nullable LivingEntity drainer, WorldAccess world, BlockPos pos, BlockState state);

	Optional<SoundEvent> getBucketFillSound();
}
