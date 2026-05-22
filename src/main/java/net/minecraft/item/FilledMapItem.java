package net.minecraft.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Предмет «Заполненная карта». Хранит ссылку на {@link MapState} через компонент {@link MapIdComponent}.
 * Обновляет цвета пикселей карты в реальном времени по мере перемещения игрока,
 * а также поддерживает масштабирование и копирование через крафт.
 */
public class FilledMapItem extends Item {

	public static final int MAP_WIDTH = 128;
	public static final int MAP_HEIGHT = 128;

	/** Размер массива биомов для карты исследования (128 * 128). */
	private static final int BIOME_ARRAY_SIZE = 16384;

	/**
	 * Хэш-множитель для псевдослучайного выбора блока в измерении с потолком (Нижний мир).
	 * Используется для имитации случайного вида поверхности под потолком.
	 */
	private static final int NETHER_HASH_MULTIPLIER_A = 231871;
	private static final int NETHER_HASH_MULTIPLIER_B = 31287121;
	private static final int NETHER_HASH_ADDEND = 11;
	private static final int NETHER_HASH_BIT_SHIFT = 20;

	/** Количество блоков «грязи» при хэше 0 в Нижнем мире. */
	private static final int NETHER_DIRT_WEIGHT = 10;
	/** Количество блоков «камня» при хэше 1 в Нижнем мире. */
	private static final int NETHER_STONE_WEIGHT = 100;
	/** Базовая высота рельефа при рендере Нижнего мира. */
	private static final double NETHER_BASE_HEIGHT = 100.0;

	/** Пороги яркости для воды: ниже — HIGH, выше — LOW. */
	private static final double WATER_BRIGHTNESS_HIGH_THRESHOLD = 0.5;
	private static final double WATER_BRIGHTNESS_LOW_THRESHOLD = 0.9;

	/** Пороги яркости для суши: выше — HIGH, ниже — LOW. */
	private static final double LAND_BRIGHTNESS_HIGH_THRESHOLD = 0.6;
	private static final double LAND_BRIGHTNESS_LOW_THRESHOLD = -0.6;

	/** Коэффициент наклона рельефа для расчёта яркости суши. */
	private static final double LAND_SLOPE_FACTOR = 4.0;
	private static final double LAND_SLOPE_CHECKER_OFFSET = 0.4;
	private static final double WATER_DEPTH_FACTOR = 0.1;
	private static final double CHECKER_FACTOR = 0.2;

	/** Пороги количества соседних водных биомов для отрисовки береговой линии. */
	private static final int SHORE_WAVE_THRESHOLD = 7;
	private static final int SHORE_OUTER_THRESHOLD = 5;
	private static final int SHORE_MID_THRESHOLD = 3;
	private static final int SHORE_INNER_THRESHOLD = 1;
	private static final int SHORE_LAND_THRESHOLD = 3;

	/** Период волны береговой линии. */
	private static final float SHORE_WAVE_PERIOD = 7.0F;
	private static final int SHORE_WAVE_DIVISOR = 8;
	private static final int SHORE_WAVE_MODULO = 5;

	/** Граничные значения диапазона пикселей карты исследования (без крайних пикселей). */
	private static final int EXPLORATION_MAP_MIN = 1;
	private static final int EXPLORATION_MAP_MAX = 127;

	public FilledMapItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Создаёт новый стек заполненной карты с выделенным {@link MapIdComponent}.
	 *
	 * @param world             серверный мир, в котором создаётся карта
	 * @param x                 центр карты по X
	 * @param z                 центр карты по Z
	 * @param scale             масштаб карты (0–4)
	 * @param showIcons         отображать ли иконки игроков
	 * @param unlimitedTracking неограниченное отслеживание игроков
	 * @return стек предмета с привязанным состоянием карты
	 */
	public static ItemStack createMap(
			ServerWorld world,
			int x,
			int z,
			byte scale,
			boolean showIcons,
			boolean unlimitedTracking
	) {
		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		MapIdComponent mapId = allocateMapId(world, x, z, scale, showIcons, unlimitedTracking, world.getRegistryKey());
		stack.set(DataComponentTypes.MAP_ID, mapId);
		return stack;
	}

	public static @Nullable MapState getMapState(@Nullable MapIdComponent id, World world) {
		return id == null ? null : world.getMapState(id);
	}

	public static @Nullable MapState getMapState(ItemStack map, World world) {
		MapIdComponent mapId = map.get(DataComponentTypes.MAP_ID);
		return getMapState(mapId, world);
	}

	private static MapIdComponent allocateMapId(
			ServerWorld world,
			int x,
			int z,
			int scale,
			boolean showIcons,
			boolean unlimitedTracking,
			RegistryKey<World> dimension
	) {
		MapState mapState = MapState.of(x, z, (byte) scale, showIcons, unlimitedTracking, dimension);
		MapIdComponent mapId = world.increaseAndGetMapId();
		world.putMapState(mapId, mapState);
		return mapId;
	}

	/**
	 * Обновляет цвета пикселей карты вокруг позиции игрока.
	 * Вызывается каждый тик инвентаря для карт, находящихся в руке.
	 * Обрабатывает по одной «полосе» пикселей за тик, чтобы не нагружать сервер.
	 *
	 * @param world  мир, в котором находится игрок
	 * @param entity сущность-наблюдатель (обрабатывается только {@link PlayerEntity})
	 * @param state  состояние карты, которое нужно обновить
	 */
	public void updateColors(World world, Entity entity, MapState state) {
		if (world.getRegistryKey() != state.dimension || !(entity instanceof PlayerEntity player)) {
			return;
		}

		int blockScale = 1 << state.scale;
		int centerX = state.centerX;
		int centerZ = state.centerZ;
		int playerPixelX = MathHelper.floor(entity.getX() - centerX) / blockScale + 64;
		int playerPixelZ = MathHelper.floor(entity.getZ() - centerZ) / blockScale + 64;
		int renderRadius = MAP_WIDTH / blockScale;

		if (world.getDimension().hasCeiling()) {
			renderRadius /= 2;
		}

		MapState.PlayerUpdateTracker tracker = state.getPlayerSyncData(player);
		tracker.updateTick++;

		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		BlockPos.Mutable fluidCheckPos = new BlockPos.Mutable();
		boolean needsUpdate = false;

		for (int pixelX = playerPixelX - renderRadius + 1; pixelX < playerPixelX + renderRadius; pixelX++) {
			if ((pixelX & 15) != (tracker.updateTick & 15) && !needsUpdate) {
				continue;
			}

			needsUpdate = false;
			double prevAvgHeight = 0.0;

			for (int pixelZ = playerPixelZ - renderRadius - 1; pixelZ < playerPixelZ + renderRadius; pixelZ++) {
				if (pixelX < 0 || pixelZ < -1 || pixelX >= MAP_WIDTH || pixelZ >= MAP_WIDTH) {
					continue;
				}

				int distSq = MathHelper.square(pixelX - playerPixelX) + MathHelper.square(pixelZ - playerPixelZ);
				boolean isEdgePixel = distSq > (renderRadius - 2) * (renderRadius - 2);
				int worldX = (centerX / blockScale + pixelX - 64) * blockScale;
				int worldZ = (centerZ / blockScale + pixelZ - 64) * blockScale;
				Multiset<MapColor> colorSamples = LinkedHashMultiset.create();
				WorldChunk chunk = world.getChunk(
						ChunkSectionPos.getSectionCoord(worldX),
						ChunkSectionPos.getSectionCoord(worldZ)
				);

				if (chunk.isEmpty()) {
					continue;
				}

				int waterDepthSum = 0;
				double avgHeight = 0.0;

				if (world.getDimension().hasCeiling()) {
					int netherHash = worldX + worldZ * NETHER_HASH_MULTIPLIER_A;
					netherHash = netherHash * netherHash * NETHER_HASH_MULTIPLIER_B + netherHash * NETHER_HASH_ADDEND;

					if ((netherHash >> NETHER_HASH_BIT_SHIFT & 1) == 0) {
						colorSamples.add(
								Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN),
								NETHER_DIRT_WEIGHT
						);
					} else {
						colorSamples.add(
								Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN),
								NETHER_STONE_WEIGHT
						);
					}

					avgHeight = NETHER_BASE_HEIGHT;
				} else {
					for (int subX = 0; subX < blockScale; subX++) {
						for (int subZ = 0; subZ < blockScale; subZ++) {
							blockPos.set(worldX + subX, 0, worldZ + subZ);
							int surfaceY = chunk.sampleHeightmap(
									Heightmap.Type.WORLD_SURFACE,
									blockPos.getX(),
									blockPos.getZ()
							) + 1;

							BlockState blockState;

							if (surfaceY <= world.getBottomY()) {
								blockState = Blocks.BEDROCK.getDefaultState();
							} else {
								do {
									blockPos.setY(--surfaceY);
									blockState = chunk.getBlockState(blockPos);
								} while (blockState.getMapColor(world, blockPos) == MapColor.CLEAR
										&& surfaceY > world.getBottomY());

								if (surfaceY > world.getBottomY() && !blockState.getFluidState().isEmpty()) {
									int fluidY = surfaceY - 1;
									fluidCheckPos.set(blockPos);

									BlockState fluidBlock;
									do {
										fluidCheckPos.setY(fluidY--);
										fluidBlock = chunk.getBlockState(fluidCheckPos);
										waterDepthSum++;
									} while (fluidY > world.getBottomY() && !fluidBlock.getFluidState().isEmpty());

									blockState = getFluidStateIfVisible(world, blockState, blockPos);
								}
							}

							state.removeBanner(world, blockPos.getX(), blockPos.getZ());
							avgHeight += (double) surfaceY / (blockScale * blockScale);
							colorSamples.add(blockState.getMapColor(world, blockPos));
						}
					}
				}

				waterDepthSum /= blockScale * blockScale;
				MapColor dominantColor = (MapColor) Iterables.getFirst(
						Multisets.copyHighestCountFirst(colorSamples),
						MapColor.CLEAR
				);

				MapColor.Brightness brightness;

				if (dominantColor == MapColor.WATER_BLUE) {
					double waterFactor = waterDepthSum * WATER_DEPTH_FACTOR + (pixelX + pixelZ & 1) * CHECKER_FACTOR;
					if (waterFactor < WATER_BRIGHTNESS_HIGH_THRESHOLD) {
						brightness = MapColor.Brightness.HIGH;
					} else if (waterFactor > WATER_BRIGHTNESS_LOW_THRESHOLD) {
						brightness = MapColor.Brightness.LOW;
					} else {
						brightness = MapColor.Brightness.NORMAL;
					}
				} else {
					double slopeFactor = (avgHeight - prevAvgHeight) * LAND_SLOPE_FACTOR / (blockScale + 4)
							+ ((pixelX + pixelZ & 1) - 0.5) * LAND_SLOPE_CHECKER_OFFSET;
					if (slopeFactor > LAND_BRIGHTNESS_HIGH_THRESHOLD) {
						brightness = MapColor.Brightness.HIGH;
					} else if (slopeFactor < LAND_BRIGHTNESS_LOW_THRESHOLD) {
						brightness = MapColor.Brightness.LOW;
					} else {
						brightness = MapColor.Brightness.NORMAL;
					}
				}

				prevAvgHeight = avgHeight;

				if (pixelZ >= 0 && distSq < renderRadius * renderRadius && (!isEdgePixel || (pixelX + pixelZ & 1) != 0)) {
					needsUpdate |= state.putColor(pixelX, pixelZ, dominantColor.getRenderColorByte(brightness));
				}
			}
		}
	}

	private BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
		FluidState fluidState = state.getFluidState();
		return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP)
				? fluidState.getBlockState()
				: state;
	}

	private static boolean isAquaticBiome(boolean[] biomes, int x, int z) {
		return biomes[z * MAP_WIDTH + x];
	}

	/**
	 * Заполняет карту исследования береговыми линиями водных биомов.
	 * Рисует оранжевые и коричневые пиксели по границам водных биомов,
	 * имитируя анимированные волны на береговой линии.
	 *
	 * @param world серверный мир
	 * @param map   стек карты для заполнения
	 */
	public static void fillExplorationMap(ServerWorld world, ItemStack map) {
		MapState mapState = getMapState(map, world);

		if (mapState == null || world.getRegistryKey() != mapState.dimension) {
			return;
		}

		int blockScale = 1 << mapState.scale;
		int originX = mapState.centerX / blockScale - 64;
		int originZ = mapState.centerZ / blockScale - 64;
		boolean[] aquaticBiomes = new boolean[BIOME_ARRAY_SIZE];
		BlockPos.Mutable blockPos = new BlockPos.Mutable();

		for (int pixelZ = 0; pixelZ < MAP_WIDTH; pixelZ++) {
			for (int pixelX = 0; pixelX < MAP_WIDTH; pixelX++) {
				RegistryEntry<Biome> biome = world.getBiome(
						blockPos.set((originX + pixelX) * blockScale, 0, (originZ + pixelZ) * blockScale)
				);
				aquaticBiomes[pixelZ * MAP_WIDTH + pixelX] = biome.isIn(BiomeTags.WATER_ON_MAP_OUTLINES);
			}
		}

		for (int pixelZ = EXPLORATION_MAP_MIN; pixelZ < EXPLORATION_MAP_MAX; pixelZ++) {
			for (int pixelX = EXPLORATION_MAP_MIN; pixelX < EXPLORATION_MAP_MAX; pixelX++) {
				int aquaticNeighbors = 0;

				for (int dz = -1; dz < 2; dz++) {
					for (int dx = -1; dx < 2; dx++) {
						if ((dz != 0 || dx != 0) && isAquaticBiome(aquaticBiomes, pixelX + dx, pixelZ + dz)) {
							aquaticNeighbors++;
						}
					}
				}

				MapColor.Brightness brightness = MapColor.Brightness.LOWEST;
				MapColor mapColor = MapColor.CLEAR;

				if (isAquaticBiome(aquaticBiomes, pixelX, pixelZ)) {
					mapColor = MapColor.ORANGE;

					if (aquaticNeighbors > SHORE_WAVE_THRESHOLD && pixelZ % 2 == 0) {
						int wavePhase = (pixelX + (int) (MathHelper.sin(pixelZ + 0.0F) * SHORE_WAVE_PERIOD))
								/ SHORE_WAVE_DIVISOR % SHORE_WAVE_MODULO;
						brightness = switch (wavePhase) {
							case 0, 4 -> MapColor.Brightness.LOW;
							case 1, 3 -> MapColor.Brightness.NORMAL;
							default -> MapColor.Brightness.HIGH;
						};
					} else if (aquaticNeighbors > SHORE_WAVE_THRESHOLD) {
						mapColor = MapColor.CLEAR;
					} else if (aquaticNeighbors > SHORE_OUTER_THRESHOLD) {
						brightness = MapColor.Brightness.NORMAL;
					} else if (aquaticNeighbors > SHORE_MID_THRESHOLD) {
						brightness = MapColor.Brightness.LOW;
					} else if (aquaticNeighbors > SHORE_INNER_THRESHOLD) {
						brightness = MapColor.Brightness.LOW;
					}
				} else if (aquaticNeighbors > 0) {
					mapColor = MapColor.BROWN;
					brightness = aquaticNeighbors > SHORE_LAND_THRESHOLD
							? MapColor.Brightness.NORMAL
							: MapColor.Brightness.LOWEST;
				}

				if (mapColor != MapColor.CLEAR) {
					mapState.setColor(pixelX, pixelZ, mapColor.getRenderColorByte(brightness));
				}
			}
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
		MapState mapState = getMapState(stack, world);

		if (mapState == null) {
			return;
		}

		if (entity instanceof PlayerEntity player) {
			mapState.update(player, stack);
		}

		if (!mapState.locked && slot != null && slot.getType() == EquipmentSlot.Type.HAND) {
			updateColors(world, entity, mapState);
		}
	}

	@Override
	public void onCraft(ItemStack stack, World world) {
		MapPostProcessingComponent postProcessing = stack.remove(DataComponentTypes.MAP_POST_PROCESSING);

		if (postProcessing == null || !(world instanceof ServerWorld serverWorld)) {
			return;
		}

		switch (postProcessing) {
			case LOCK -> copyMap(stack, serverWorld);
			case SCALE -> scale(stack, serverWorld);
		}
	}

	private static void scale(ItemStack map, ServerWorld world) {
		MapState mapState = getMapState(map, world);

		if (mapState == null) {
			return;
		}

		MapIdComponent newId = world.increaseAndGetMapId();
		world.putMapState(newId, mapState.zoomOut());
		map.set(DataComponentTypes.MAP_ID, newId);
	}

	private static void copyMap(ItemStack stack, ServerWorld world) {
		MapState mapState = getMapState(stack, world);

		if (mapState == null) {
			return;
		}

		MapIdComponent newId = world.increaseAndGetMapId();
		world.putMapState(newId, mapState.copy());
		stack.set(DataComponentTypes.MAP_ID, newId);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());

		if (blockState.isIn(BlockTags.BANNERS)) {
			if (!context.getWorld().isClient()) {
				MapState mapState = getMapState(context.getStack(), context.getWorld());

				if (mapState != null && !mapState.addBanner(context.getWorld(), context.getBlockPos())) {
					return ActionResult.FAIL;
				}
			}

			return ActionResult.SUCCESS;
		}

		return super.useOnBlock(context);
	}
}
