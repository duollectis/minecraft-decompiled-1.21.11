package net.minecraft.entity.ai.brain.task;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Расширение {@link LongJumpTask}, предпочитающее приземляться на блоки из тега {@code favoredBlocks}.
 * С вероятностью {@code biasChance} откладывает нежелательные цели и сначала проверяет предпочтительные.
 */
public class BiasedLongJumpTask<E extends MobEntity> extends LongJumpTask<E> {

	private final TagKey<Block> favoredBlocks;
	private final float biasChance;
	private final List<LongJumpTask.Target> unfavoredTargets = new ArrayList<>();
	private boolean useBias;

	public BiasedLongJumpTask(
			UniformIntProvider cooldownRange,
			int verticalRange,
			int horizontalRange,
			float maxRange,
			Function<E, SoundEvent> entityToSound,
			TagKey<Block> favoredBlocks,
			float biasChance,
			BiPredicate<E, BlockPos> jumpToPredicate
	) {
		super(cooldownRange, verticalRange, horizontalRange, maxRange, entityToSound, jumpToPredicate);
		this.favoredBlocks = favoredBlocks;
		this.biasChance = biasChance;
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		super.run(world, entity, time);
		unfavoredTargets.clear();
		useBias = entity.getRandom().nextFloat() < biasChance;
	}

	@Override
	protected Optional<LongJumpTask.Target> removeRandomTarget(ServerWorld world) {
		if (!useBias) {
			return super.removeRandomTarget(world);
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();

		while (!potentialTargets.isEmpty()) {
			Optional<LongJumpTask.Target> candidate = super.removeRandomTarget(world);
			if (candidate.isPresent()) {
				LongJumpTask.Target target = candidate.get();
				if (world.getBlockState(mutable.set(target.pos(), Direction.DOWN)).isIn(favoredBlocks)) {
					return candidate;
				}

				unfavoredTargets.add(target);
			}
		}

		return unfavoredTargets.isEmpty()
				? Optional.empty()
				: Optional.of(unfavoredTargets.remove(0));
	}
}
