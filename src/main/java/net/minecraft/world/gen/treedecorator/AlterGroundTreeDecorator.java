package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;

/**
 * Декоратор дерева: заменяет блоки почвы вокруг основания дерева на блоки из {@code provider}.
 * Используется для создания мохового покрова под деревьями в болотах и джунглях.
 */
public class AlterGroundTreeDecorator extends TreeDecorator {

	public static final MapCodec<AlterGroundTreeDecorator> CODEC = BlockStateProvider.TYPE_CODEC
			.fieldOf("provider")
			.xmap(AlterGroundTreeDecorator::new, decorator -> decorator.provider);
	private final BlockStateProvider provider;

	public AlterGroundTreeDecorator(BlockStateProvider provider) {
		this.provider = provider;
	}

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.ALTER_GROUND;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		List<BlockPos> litterPositions = TreeFeature.getLeafLitterPositions(generator);

		if (litterPositions.isEmpty()) {
			return;
		}

		int baseY = litterPositions.get(0).getY();
		litterPositions.stream().filter(pos -> pos.getY() == baseY).forEach(pos -> {
			setArea(generator, pos.west().north());
			setArea(generator, pos.east(2).north());
			setArea(generator, pos.west().south(2));
			setArea(generator, pos.east(2).south(2));

			for (int attempt = 0; attempt < 5; attempt++) {
				int randomIndex = generator.getRandom().nextInt(64);
				int offsetX = randomIndex % 8;
				int offsetZ = randomIndex / 8;

				if (offsetX == 0 || offsetX == 7 || offsetZ == 0 || offsetZ == 7) {
					setArea(generator, pos.add(-3 + offsetX, 0, -3 + offsetZ));
				}
			}
		});
	}

	private void setArea(TreeDecorator.Generator generator, BlockPos origin) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) != 2 || Math.abs(dz) != 2) {
					setColumn(generator, origin.add(dx, 0, dz));
				}
			}
		}
	}

	private void setColumn(TreeDecorator.Generator generator, BlockPos origin) {
		for (int dy = 2; dy >= -3; dy--) {
			BlockPos checkPos = origin.up(dy);

			if (Feature.isSoil(generator.getWorld(), checkPos)) {
				generator.replace(checkPos, provider.get(generator.getRandom(), origin));
				break;
			}

			if (!generator.isAir(checkPos) && dy < 0) {
				break;
			}
		}
	}
}
