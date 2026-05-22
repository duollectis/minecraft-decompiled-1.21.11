package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Декоратор дерева, размещающий блоки, прикреплённые к листьям.
 * Для каждого листового блока случайно выбирается направление из разрешённого списка,
 * и если в этом направлении достаточно свободного пространства, размещается блок.
 * Вокруг размещённого блока создаётся зона исключения, предотвращающая скопление декораций.
 */
public class AttachedToLeavesTreeDecorator extends TreeDecorator {

	public static final MapCodec<AttachedToLeavesTreeDecorator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codec
							                    .floatRange(0.0F, 1.0F)
							                    .fieldOf("probability")
							                    .forGetter(treeDecorator -> treeDecorator.probability),
					                    Codec
							                    .intRange(0, 16)
							                    .fieldOf("exclusion_radius_xz")
							                    .forGetter(treeDecorator -> treeDecorator.exclusionRadiusXZ),
					                    Codec
							                    .intRange(0, 16)
							                    .fieldOf("exclusion_radius_y")
							                    .forGetter(treeDecorator -> treeDecorator.exclusionRadiusY),
					                    BlockStateProvider.TYPE_CODEC
							                    .fieldOf("block_provider")
							                    .forGetter(treeDecorator -> treeDecorator.blockProvider),
					                    Codec
							                    .intRange(1, 16)
							                    .fieldOf("required_empty_blocks")
							                    .forGetter(treeDecorator -> treeDecorator.requiredEmptyBlocks),
					                    Codecs
							                    .nonEmptyList(Direction.CODEC.listOf())
							                    .fieldOf("directions")
							                    .forGetter(treeDecorator -> treeDecorator.directions)
			                    )
			                    .apply(instance, AttachedToLeavesTreeDecorator::new)
	);
	protected final float probability;
	protected final int exclusionRadiusXZ;
	protected final int exclusionRadiusY;
	protected final BlockStateProvider blockProvider;
	protected final int requiredEmptyBlocks;
	protected final List<Direction> directions;

	public AttachedToLeavesTreeDecorator(
			float probability,
			int exclusionRadiusXZ,
			int exclusionRadiusY,
			BlockStateProvider blockProvider,
			int requiredEmptyBlocks,
			List<Direction> directions
	) {
		this.probability = probability;
		this.exclusionRadiusXZ = exclusionRadiusXZ;
		this.exclusionRadiusY = exclusionRadiusY;
		this.blockProvider = blockProvider;
		this.requiredEmptyBlocks = requiredEmptyBlocks;
		this.directions = directions;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		Set<BlockPos> set = new HashSet<>();
		Random random = generator.getRandom();

		for (BlockPos leafPos : Util.copyShuffled(generator.getLeavesPositions(), random)) {
			Direction direction = Util.getRandom(directions, random);
			BlockPos offsetPos = leafPos.offset(direction);

			if (set.contains(offsetPos) || random.nextFloat() >= probability || !meetsRequiredEmptyBlocks(generator, leafPos, direction)) {
				continue;
			}

			BlockPos exclusionMin = offsetPos.add(-exclusionRadiusXZ, -exclusionRadiusY, -exclusionRadiusXZ);
			BlockPos exclusionMax = offsetPos.add(exclusionRadiusXZ, exclusionRadiusY, exclusionRadiusXZ);

			for (BlockPos exclusionPos : BlockPos.iterate(exclusionMin, exclusionMax)) {
				set.add(exclusionPos.toImmutable());
			}

			generator.replace(offsetPos, blockProvider.get(random, offsetPos));
		}
	}

	private boolean meetsRequiredEmptyBlocks(TreeDecorator.Generator generator, BlockPos pos, Direction direction) {
		for (int step = 1; step <= requiredEmptyBlocks; step++) {
			if (!generator.isAir(pos.offset(direction, step))) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.ATTACHED_TO_LEAVES;
	}
}
