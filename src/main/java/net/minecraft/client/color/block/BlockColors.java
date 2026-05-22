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

/**
 * Реестр провайдеров цвета для блоков на стороне клиента.
 * <p>
 * Хранит маппинг {@link Block} → {@link BlockColorProvider} и используется рендерером
 * для определения цвета тинта блока (трава, листья, вода, редстоун и т.д.) в зависимости
 * от биома и состояния блока. Также отслеживает, какие {@link Property} блока влияют
 * на его цвет, чтобы перестраивать чанк-меш при их изменении.
 */
@Environment(EnvType.CLIENT)
public class BlockColors {

	private static final int NO_COLOR = -1;

	/** Цвет кувшинки, размещённой в мире (с учётом освещения биома). */
	public static final int PLACED_LILY_PAD = -14647248;

	/** Дефолтный цвет кувшинки без контекста мира (для инвентаря и частиц). */
	public static final int LILY_PAD = -9321636;

	/** Цвет хвойных листьев (ель) — фиксированный, не зависит от биома. */
	private static final int SPRUCE_LEAVES_COLOR = -10380959;

	/** Цвет берёзовых листьев — фиксированный, не зависит от биома. */
	private static final int BIRCH_LEAVES_COLOR = -8345771;

	/** Дефолтный цвет листвы (дуб, джунгли и т.д.) при отсутствии данных биома. */
	private static final int DEFAULT_FOLIAGE_COLOR = -12012264;

	/** Дефолтный цвет сухой листвы (опавшие листья) при отсутствии данных биома. */
	private static final int DEFAULT_DRY_FOLIAGE_COLOR = -10732494;

	/** Цвет прикреплённых стеблей дыни/тыквы (полностью выросших). */
	private static final int ATTACHED_STEM_COLOR = -2046180;

	private final IdList<BlockColorProvider> providers = new IdList<>(32);
	private final Map<Block, Set<Property<?>>> properties = Maps.newHashMap();

	/**
	 * Создаёт и инициализирует реестр с провайдерами цвета для всех ванильных блоков,
	 * которые поддерживают биомное окрашивание или имеют нетривиальный цвет тинта.
	 *
	 * @return полностью инициализированный экземпляр {@link BlockColors}
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
			(state, world, pos, tintIndex) -> world != null && pos != null
				? BiomeColors.getGrassColor(world, pos)
				: GrassColors.getDefaultColor(),
			Blocks.GRASS_BLOCK,
			Blocks.FERN,
			Blocks.SHORT_GRASS,
			Blocks.POTTED_FERN,
			Blocks.BUSH
		);

		// tintIndex == 0 — стебель цветка (без биомного цвета), tintIndex == 1 — трава под ним
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> tintIndex != 0
				? (world != null && pos != null ? BiomeColors.getGrassColor(world, pos) : GrassColors.getDefaultColor())
				: NO_COLOR,
			Blocks.PINK_PETALS,
			Blocks.WILDFLOWERS
		);

		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> SPRUCE_LEAVES_COLOR,
			Blocks.SPRUCE_LEAVES
		);
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> BIRCH_LEAVES_COLOR,
			Blocks.BIRCH_LEAVES
		);
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> world != null && pos != null
				? BiomeColors.getFoliageColor(world, pos)
				: DEFAULT_FOLIAGE_COLOR,
			Blocks.OAK_LEAVES,
			Blocks.JUNGLE_LEAVES,
			Blocks.ACACIA_LEAVES,
			Blocks.DARK_OAK_LEAVES,
			Blocks.VINE,
			Blocks.MANGROVE_LEAVES
		);
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> world != null && pos != null
				? BiomeColors.getDryFoliageColor(world, pos)
				: DEFAULT_DRY_FOLIAGE_COLOR,
			Blocks.LEAF_LITTER
		);
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> world != null && pos != null
				? BiomeColors.getWaterColor(world, pos)
				: NO_COLOR,
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
			(state, world, pos, tintIndex) -> world != null && pos != null
				? BiomeColors.getGrassColor(world, pos)
				: NO_COLOR,
			Blocks.SUGAR_CANE
		);
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> ATTACHED_STEM_COLOR,
			Blocks.ATTACHED_MELON_STEM,
			Blocks.ATTACHED_PUMPKIN_STEM
		);

		// Цвет стебля меняется по мере роста: от зелёного (age=0) до оранжевого (age=7)
		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> {
				int age = state.get(StemBlock.AGE);
				return ColorHelper.getArgb(age * 32, 255 - age * 8, age * 4);
			},
			Blocks.MELON_STEM,
			Blocks.PUMPKIN_STEM
		);
		blockColors.registerColorProperty(StemBlock.AGE, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);

		blockColors.registerColorProvider(
			(state, world, pos, tintIndex) -> world != null && pos != null ? PLACED_LILY_PAD : LILY_PAD,
			Blocks.LILY_PAD
		);

		return blockColors;
	}

	/**
	 * Возвращает цвет блока для рендера частиц (без учёта биома).
	 * Если провайдер не зарегистрирован, возвращает цвет карты ({@link MapColor}).
	 *
	 * @param state состояние блока
	 * @param world мир, используется только для получения {@link MapColor}
	 * @param pos   позиция блока
	 * @return ARGB-цвет в виде int
	 */
	public int getParticleColor(BlockState state, World world, BlockPos pos) {
		BlockColorProvider provider = providers.get(Registries.BLOCK.getRawId(state.getBlock()));

		if (provider != null) {
			return provider.getColor(state, null, null, 0);
		}

		MapColor mapColor = state.getMapColor(world, pos);
		return mapColor != null ? mapColor.color : NO_COLOR;
	}

	/**
	 * Возвращает цвет тинта для блока с учётом биома и позиции.
	 * Возвращает {@code -1}, если провайдер для данного блока не зарегистрирован.
	 *
	 * @param state     состояние блока
	 * @param world     вид мира для чтения данных биома; может быть {@code null}
	 * @param pos       позиция блока; может быть {@code null}
	 * @param tintIndex индекс слоя тинта из модели блока
	 * @return ARGB-цвет или {@code -1}, если провайдер отсутствует
	 */
	public int getColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
		BlockColorProvider provider = providers.get(Registries.BLOCK.getRawId(state.getBlock()));
		return provider == null ? NO_COLOR : provider.getColor(state, world, pos, tintIndex);
	}

	/**
	 * Регистрирует {@link BlockColorProvider} для одного или нескольких блоков.
	 *
	 * @param provider провайдер цвета
	 * @param blocks   блоки, для которых применяется данный провайдер
	 */
	public void registerColorProvider(BlockColorProvider provider, Block... blocks) {
		for (Block block : blocks) {
			providers.set(provider, Registries.BLOCK.getRawId(block));
		}
	}

	private void registerColorProperties(Set<Property<?>> colorProperties, Block... blocks) {
		for (Block block : blocks) {
			properties.put(block, colorProperties);
		}
	}

	private void registerColorProperty(Property<?> property, Block... blocks) {
		registerColorProperties(ImmutableSet.of(property), blocks);
	}

	/**
	 * Возвращает набор свойств блока, изменение которых требует пересчёта цвета
	 * (и, следовательно, перестройки чанк-меша).
	 *
	 * @param block блок
	 * @return неизменяемый набор свойств или пустой набор, если блок не зарегистрирован
	 */
	public Set<Property<?>> getProperties(Block block) {
		return properties.getOrDefault(block, ImmutableSet.of());
	}
}
