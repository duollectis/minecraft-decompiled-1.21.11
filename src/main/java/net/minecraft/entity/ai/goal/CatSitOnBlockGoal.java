package net.minecraft.entity.ai.goal;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

/**
 * Цель, заставляющая кошку искать и садиться на привлекательные блоки:
 * сундуки, горящие печи и кровати. Наследует логику навигации к целевой позиции
 * от {@link MoveToTargetPosGoal}.
 */
public class CatSitOnBlockGoal extends MoveToTargetPosGoal {

	private final CatEntity cat;

	public CatSitOnBlockGoal(CatEntity cat, double speed) {
		super(cat, speed, 8);
		this.cat = cat;
	}

	@Override
	public boolean canStart() {
		return cat.isTamed() && !cat.isSitting() && super.canStart();
	}

	@Override
	public void start() {
		super.start();
		cat.setInSittingPose(false);
	}

	@Override
	public void stop() {
		super.stop();
		cat.setInSittingPose(false);
	}

	@Override
	public void tick() {
		super.tick();
		cat.setInSittingPose(hasReached());
	}

	@Override
	protected boolean isTargetPos(WorldView world, BlockPos pos) {
		if (!world.isAir(pos.up())) {
			return false;
		}

		BlockState blockState = world.getBlockState(pos);

		if (blockState.isOf(Blocks.CHEST)) {
			return ChestBlockEntity.getPlayersLookingInChestCount(world, pos) < 1;
		}

		return (blockState.isOf(Blocks.FURNACE) && blockState.get(FurnaceBlock.LIT))
			|| blockState.isIn(
				BlockTags.BEDS,
				state -> state.getOrEmpty(BedBlock.PART).map(part -> part != BedPart.HEAD).orElse(true)
			);
	}
}
