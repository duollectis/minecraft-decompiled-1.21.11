package net.minecraft.block;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@code InteractibleSlotContainer}.
 */
public interface InteractibleSlotContainer {

	int getRows();

	int getColumns();

	default OptionalInt getHitSlot(BlockHitResult hitResult, Direction facing) {
		return getHitPosOnFront(hitResult, facing).map(vec2f -> {
			int i = getSlotAlongAxis(1.0F - vec2f.y, this.getRows());
			int j = getSlotAlongAxis(vec2f.x, this.getColumns());
			return OptionalInt.of(j + i * this.getColumns());
		}).orElseGet(OptionalInt::empty);
	}

	private static Optional<Vec2f> getHitPosOnFront(BlockHitResult hitResult, Direction facing) {
		Direction direction = hitResult.getSide();
		if (facing != direction) {
			return Optional.empty();
		}
		else {
			BlockPos blockPos = hitResult.getBlockPos().offset(direction);
			Vec3d vec3d = hitResult.getPos().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			double d = vec3d.getX();
			double e = vec3d.getY();
			double f = vec3d.getZ();

			return switch (direction) {
				case NORTH -> Optional.of(new Vec2f((float) (1.0 - d), (float) e));
				case SOUTH -> Optional.of(new Vec2f((float) d, (float) e));
				case WEST -> Optional.of(new Vec2f((float) f, (float) e));
				case EAST -> Optional.of(new Vec2f((float) (1.0 - f), (float) e));
				case DOWN, UP -> Optional.empty();
			};
		}
	}

	private static int getSlotAlongAxis(float pos, int max) {
		float f = pos * 16.0F;
		float g = 16.0F / max;
		return MathHelper.clamp(MathHelper.floor(f / g), 0, max - 1);
	}
}
