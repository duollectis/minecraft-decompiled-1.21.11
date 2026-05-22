package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowyBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Замораживает верхний слой воды в лёд и покрывает поверхность снегом
 * в пределах чанка 16×16, если биом это допускает.
 * Также обновляет свойство {@link SnowyBlock#SNOWY} у блока под снегом.
 */
public class FreezeTopLayerFeature extends Feature<DefaultFeatureConfig> {

	private static final int CHUNK_SIZE = 16;

	public FreezeTopLayerFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		BlockPos.Mutable belowPos = new BlockPos.Mutable();

		for (int dx = 0; dx < CHUNK_SIZE; dx++) {
			for (int dz = 0; dz < CHUNK_SIZE; dz++) {
				int x = origin.getX() + dx;
				int z = origin.getZ() + dz;
				int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
				pos.set(x, topY, z);
				belowPos.set(pos).move(Direction.DOWN, 1);

				Biome biome = world.getBiome(pos).value();

				if (biome.canSetIce(world, belowPos, false)) {
					world.setBlockState(belowPos, Blocks.ICE.getDefaultState(), 2);
				}

				if (biome.canSetSnow(world, pos)) {
					world.setBlockState(pos, Blocks.SNOW.getDefaultState(), 2);

					BlockState belowState = world.getBlockState(belowPos);

					if (belowState.contains(SnowyBlock.SNOWY)) {
						world.setBlockState(belowPos, belowState.with(SnowyBlock.SNOWY, true), 2);
					}
				}
			}
		}

		return true;
	}
}
