package net.minecraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code FluidModificationItem}.
 */
public interface FluidModificationItem {

	default void onEmptied(@Nullable LivingEntity user, World world, ItemStack stack, BlockPos pos) {
	}

	boolean placeFluid(@Nullable LivingEntity user, World world, BlockPos pos, @Nullable BlockHitResult hitResult);
}
