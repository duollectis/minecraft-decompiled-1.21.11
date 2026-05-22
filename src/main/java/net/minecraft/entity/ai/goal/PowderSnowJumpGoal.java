package net.minecraft.entity.ai.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Цель прыжка из порошкового снега: активируется, если моб находится в снегу
 * и над ним есть ещё один слой снега или пустое пространство.
 */
public class PowderSnowJumpGoal extends Goal {

	private final MobEntity entity;
	private final World world;

	public PowderSnowJumpGoal(MobEntity entity, World world) {
		this.entity = entity;
		this.world = world;
		this.setControls(EnumSet.of(Goal.Control.JUMP));
	}

	@Override
	public boolean canStart() {
		boolean inSnow = entity.wasInPowderSnow || entity.inPowderSnow;

		if (!inSnow || !entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
			return false;
		}

		BlockPos above = entity.getBlockPos().up();
		BlockState aboveState = world.getBlockState(above);
		return aboveState.isOf(Blocks.POWDER_SNOW)
			|| aboveState.getCollisionShape(world, above) == VoxelShapes.empty();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		entity.getJumpControl().setActive();
	}
}
