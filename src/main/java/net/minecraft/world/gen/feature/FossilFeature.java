package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Генерирует ископаемое (fossil) из NBT-шаблона структуры под землёй.
 * Выбирает случайный шаблон из конфига, определяет минимальную высоту поверхности
 * в пределах структуры и размещает её на безопасной глубине.
 * Если слишком много угловых блоков структуры оказываются в воздухе или жидкости —
 * генерация отменяется, чтобы избежать плавающих ископаемых.
 */
public class FossilFeature extends Feature<FossilFeatureConfig> {

	private static final int DEPTH_BELOW_SURFACE = 15;
	private static final int DEPTH_RANDOM_EXTRA = 10;
	private static final int MIN_Y_ABOVE_BOTTOM = 10;
	private static final int PLACE_FLAGS = 260;

	public FossilFeature(Codec<FossilFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<FossilFeatureConfig> context) {
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		BlockRotation rotation = BlockRotation.random(random);
		FossilFeatureConfig config = context.getConfig();

		int structureIdx = random.nextInt(config.fossilStructures.size());
		StructureTemplateManager templateManager = world.toServerWorld().getServer().getStructureTemplateManager();
		StructureTemplate fossilTemplate = templateManager.getTemplateOrBlank(config.fossilStructures.get(structureIdx));
		StructureTemplate overlayTemplate = templateManager.getTemplateOrBlank(config.overlayStructures.get(structureIdx));

		ChunkPos chunkPos = new ChunkPos(origin);
		BlockBox bounds = new BlockBox(
			chunkPos.getStartX() - 16,
			world.getBottomY(),
			chunkPos.getStartZ() - 16,
			chunkPos.getEndX() + 16,
			world.getTopYInclusive(),
			chunkPos.getEndZ() + 16
		);
		StructurePlacementData placement = new StructurePlacementData()
			.setRotation(rotation)
			.setBoundingBox(bounds)
			.setRandom(random);

		Vec3i size = fossilTemplate.getRotatedSize(rotation);
		BlockPos corner = origin.add(-size.getX() / 2, 0, -size.getZ() / 2);
		int minY = origin.getY();

		for (int dx = 0; dx < size.getX(); dx++) {
			for (int dz = 0; dz < size.getZ(); dz++) {
				minY = Math.min(
					minY,
					world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, corner.getX() + dx, corner.getZ() + dz)
				);
			}
		}

		int placeY = Math.max(
			minY - DEPTH_BELOW_SURFACE - random.nextInt(DEPTH_RANDOM_EXTRA),
			world.getBottomY() + MIN_Y_ABOVE_BOTTOM
		);
		BlockPos placePos = fossilTemplate.offsetByTransformedSize(corner.withY(placeY), BlockMirror.NONE, rotation);

		if (getEmptyCorners(world, fossilTemplate.calculateBoundingBox(placement, placePos)) > config.maxEmptyCorners) {
			return false;
		}

		placement.clearProcessors();
		config.fossilProcessors.value().getList().forEach(placement::addProcessor);
		fossilTemplate.place(world, placePos, placePos, placement, random, PLACE_FLAGS);

		placement.clearProcessors();
		config.overlayProcessors.value().getList().forEach(placement::addProcessor);
		overlayTemplate.place(world, placePos, placePos, placement, random, PLACE_FLAGS);

		return true;
	}

	private static int getEmptyCorners(StructureWorldAccess world, BlockBox box) {
		MutableInt count = new MutableInt(0);
		box.forEachVertex(pos -> {
			BlockState state = world.getBlockState(pos);

			if (state.isAir() || state.isOf(Blocks.LAVA) || state.isOf(Blocks.WATER)) {
				count.add(1);
			}
		});

		return count.intValue();
	}
}
