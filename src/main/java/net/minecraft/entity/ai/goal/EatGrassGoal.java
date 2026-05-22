package net.minecraft.entity.ai.goal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Цель поедания травы: моб останавливается и поедает траву или блок дёрна под ногами,
 * при включённом правиле {@code DO_MOB_GRIEFING} разрушает блок.
 */
public class EatGrassGoal extends Goal {

	private static final int MAX_TIMER = 40;
	private static final int EAT_TICK = 4;
	private static final byte EAT_STATUS = 10;
	private static final int GRASS_BREAK_EVENT = 2001;
	private static final int DOOR_BREAK_EVENT = 1019;
	private static final Predicate<BlockState> EDIBLE_PREDICATE = state -> state.isIn(BlockTags.EDIBLE_FOR_SHEEP);

	private final MobEntity mob;
	private final World world;
	private int timer;

	public EatGrassGoal(MobEntity mob) {
		this.mob = mob;
		this.world = mob.getEntityWorld();
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
	}

	@Override
	public boolean canStart() {
		int chance = mob.isBaby() ? 50 : 1000;
		if (mob.getRandom().nextInt(getTickCount(chance)) != 0) {
			return false;
		}

		BlockPos blockPos = mob.getBlockPos();
		return EDIBLE_PREDICATE.test(world.getBlockState(blockPos))
				|| world.getBlockState(blockPos.down()).isOf(Blocks.GRASS_BLOCK);
	}

	@Override
	public void start() {
		timer = getTickCount(MAX_TIMER);
		world.sendEntityStatus(mob, EAT_STATUS);
		mob.getNavigation().stop();
	}

	@Override
	public void stop() {
		timer = 0;
	}

	@Override
	public boolean shouldContinue() {
		return timer > 0;
	}

	public int getTimer() {
		return timer;
	}

	@Override
	public void tick() {
		timer = Math.max(0, timer - 1);
		if (timer != getTickCount(EAT_TICK)) {
			return;
		}

		BlockPos blockPos = mob.getBlockPos();
		if (EDIBLE_PREDICATE.test(world.getBlockState(blockPos))) {
			if (castToServerWorld(world).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				world.breakBlock(blockPos, false);
			}

			mob.onEatingGrass();
			return;
		}

		BlockPos below = blockPos.down();
		if (world.getBlockState(below).isOf(Blocks.GRASS_BLOCK)) {
			if (castToServerWorld(world).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				world.syncWorldEvent(GRASS_BREAK_EVENT, below, Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
				world.setBlockState(below, Blocks.DIRT.getDefaultState(), 2);
			}

			mob.onEatingGrass();
		}
	}
}
