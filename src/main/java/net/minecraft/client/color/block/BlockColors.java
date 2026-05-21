package net.minecraft.client.color.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.minecraft.world.biome.GrassColors;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockColors}.
 */
public class BlockColors {

	private static final int NO_COLOR = -1;
	public static final int PLACED_LILY_PAD = -14647248;
	public static final int LILY_PAD = -9321636;
	private final IdList<BlockColorProvider> providers = new IdList<>(32);
	private final Map<Block, Set<Property<?>>> properties = Maps.newHashMap();

	/**
	 * Create.
	 *
	 * @return BlockColors — результат операции
	 */
	public static BlockColors create() {
		BlockColors blockColors = new BlockColors();
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null
				                                  ? BiomeColors.getGrassColor(
						world,
						state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos
				)
				                                  : GrassColors.getDefaultColor(),
				Blocks.LARGE_FERN,
				Blocks.TALL_GRASS
		);
		blockColors.registerColorProperty(TallPlantBlock.HALF, Blocks.LARGE_FERN, Blocks.TALL_GRASS);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null ? BiomeColors.getGrassColor(world, pos)
				                                                               : GrassColors.getDefaultColor(),
				Blocks.GRASS_BLOCK,
				Blocks.FERN,
				Blocks.SHORT_GRASS,
				Blocks.POTTED_FERN,
				Blocks.BUSH
		);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> {
					if (tintIndex != 0) {
						return world != null && pos != null ? BiomeColors.getGrassColor(world, pos)
						                                    : GrassColors.getDefaultColor();
					}
					else {
						return -1;
					}
				}, Blocks.PINK_PETALS, Blocks.WILDFLOWERS
		);
		blockColors.registerColorProvider((state, world, pos, tintIndex) -> -10380959, Blocks.SPRUCE_LEAVES);
		blockColors.registerColorProvider((state, world, pos, tintIndex) -> -8345771, Blocks.BIRCH_LEAVES);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null ? BiomeColors.getFoliageColor(world, pos)
				                                                               : -12012264,
				Blocks.OAK_LEAVES,
				Blocks.JUNGLE_LEAVES,
				Blocks.ACACIA_LEAVES,
				Blocks.DARK_OAK_LEAVES,
				Blocks.VINE,
				Blocks.MANGROVE_LEAVES
		);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null ? BiomeColors.getDryFoliageColor(
						world,
						pos
				) : -10732494, Blocks.LEAF_LITTER
		);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null ? BiomeColors.getWaterColor(world, pos)
				                                                               : -1,
				Blocks.WATER,
				Blocks.BUBBLE_COLUMN,
				Blocks.WATER_CAULDRON
		);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> RedstoneWireBlock.getWireColor(state.get(RedstoneWireBlock.POWER)),
				Blocks.REDSTONE_WIRE
		);
		blockColors.registerColorProperty(RedstoneWireBlock.POWER, Blocks.REDSTONE_WIRE);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null ? BiomeColors.getGrassColor(world, pos)
				                                                               : -1, Blocks.SUGAR_CANE
		);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> -2046180,
				Blocks.ATTACHED_MELON_STEM,
				Blocks.ATTACHED_PUMPKIN_STEM
		);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> {
					int i = state.get(StemBlock.AGE);
					return ColorHelper.getArgb(i * 32, 255 - i * 8, i * 4);
				}, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM
		);
		blockColors.registerColorProperty(StemBlock.AGE, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
		blockColors.registerColorProvider(
				(state, world, pos, tintIndex) -> world != null && pos != null ? -14647248 : -9321636,
				Blocks.LILY_PAD
		);
		return blockColors;
	}

	public int getParticleColor(BlockState state, World world, BlockPos pos) {
		BlockColorProvider blockColorProvider = this.providers.get(Registries.BLOCK.getRawId(state.getBlock()));
		if (blockColorProvider != null) {
			return blockColorProvider.getColor(state, null, null, 0);
		}
		else {
			MapColor mapColor = state.getMapColor(world, pos);
			return mapColor != null ? mapColor.color : -1;
		}
	}

	public int getColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
		BlockColorProvider blockColorProvider = this.providers.get(Registries.BLOCK.getRawId(state.getBlock()));
		return blockColorProvider == null ? -1 : blockColorProvider.getColor(state, world, pos, tintIndex);
	}

	/**
	 * Регистрирует color provider.
	 *
	 * @param provider provider
	 * @param blocks blocks
	 */
	public void registerColorProvider(BlockColorProvider provider, Block... blocks) {
		for (Block block : blocks) {
			this.providers.set(provider, Registries.BLOCK.getRawId(block));
		}
	}

	private void registerColorProperties(Set<Property<?>> properties, Block... blocks) {
		for (Block block : blocks) {
			this.properties.put(block, properties);
		}
	}

	private void registerColorProperty(Property<?> property, Block... blocks) {
		this.registerColorProperties(ImmutableSet.of(property), blocks);
	}

	public Set<Property<?>> getProperties(Block block) {
		return this.properties.getOrDefault(block, ImmutableSet.of());
	}
}
