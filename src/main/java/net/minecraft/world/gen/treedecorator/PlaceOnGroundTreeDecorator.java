package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;

/**
 * Декоратор дерева, размещающий блоки на земле вокруг основания дерева.
 * Вычисляет ограничивающий прямоугольник листового опада на уровне земли,
 * расширяет его на заданный радиус и высоту, затем случайным образом
 * пробует разместить блоки внутри этой области поверх непрозрачных блоков.
 */
public class PlaceOnGroundTreeDecorator extends TreeDecorator {

	public static final MapCodec<PlaceOnGroundTreeDecorator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codecs.POSITIVE_INT.fieldOf("tries").orElse(128).forGetter(treeDecorator -> treeDecorator.tries),
					                    Codecs.NON_NEGATIVE_INT
							                    .fieldOf("radius")
							                    .orElse(2)
							                    .forGetter(treeDecorator -> treeDecorator.radius),
					                    Codecs.NON_NEGATIVE_INT
							                    .fieldOf("height")
							                    .orElse(1)
							                    .forGetter(treeDecorator -> treeDecorator.height),
					                    BlockStateProvider.TYPE_CODEC
							                    .fieldOf("block_state_provider")
							                    .forGetter(treeDecorator -> treeDecorator.blockStateProvider)
			                    )
			                    .apply(instance, PlaceOnGroundTreeDecorator::new)
	);
	private final int tries;
	private final int radius;
	private final int height;
	private final BlockStateProvider blockStateProvider;

	public PlaceOnGroundTreeDecorator(int tries, int radius, int height, BlockStateProvider blockStateProvider) {
		this.tries = tries;
		this.radius = radius;
		this.height = height;
		this.blockStateProvider = blockStateProvider;
	}

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.PLACE_ON_GROUND;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		List<BlockPos> litterPositions = TreeFeature.getLeafLitterPositions(generator);

		if (litterPositions.isEmpty()) {
			return;
		}

		BlockPos first = litterPositions.getFirst();
		int baseY = first.getY();
		int minX = first.getX();
		int maxX = first.getX();
		int minZ = first.getZ();
		int maxZ = first.getZ();

		for (BlockPos pos : litterPositions) {
			if (pos.getY() == baseY) {
				minX = Math.min(minX, pos.getX());
				maxX = Math.max(maxX, pos.getX());
				minZ = Math.min(minZ, pos.getZ());
				maxZ = Math.max(maxZ, pos.getZ());
			}
		}

		Random random = generator.getRandom();
		BlockBox bounds = new BlockBox(minX, baseY, minZ, maxX, baseY, maxZ).expand(radius, height, radius);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int attempt = 0; attempt < tries; attempt++) {
			mutable.set(
					random.nextBetween(bounds.getMinX(), bounds.getMaxX()),
					random.nextBetween(bounds.getMinY(), bounds.getMaxY()),
					random.nextBetween(bounds.getMinZ(), bounds.getMaxZ())
			);
			tryPlaceBlock(generator, mutable);
		}
	}

	private void tryPlaceBlock(TreeDecorator.Generator generator, BlockPos pos) {
		BlockPos above = pos.up();

		if (generator.getWorld().testBlockState(above, state -> state.isAir() || state.isOf(Blocks.VINE))
				&& generator.matches(pos, AbstractBlock.AbstractBlockState::isOpaqueFullCube)
				&& generator.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos).getY() <= above.getY()
		) {
			generator.replace(above, blockStateProvider.get(generator.getRandom(), above));
		}
	}
}
