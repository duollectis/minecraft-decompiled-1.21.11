package net.minecraft.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Карта высот чанка — хранит Y-координату верхнего блока для каждой XZ-позиции (16×16).
 * Данные упакованы в {@link PaletteStorage} для экономии памяти.
 * Каждый тип карты высот использует свой предикат для определения «непрозрачного» блока.
 */
public class Heightmap {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Размер стороны секции чанка в блоках. */
	private static final int SECTION_SIZE = 16;

	static final Predicate<BlockState> NOT_AIR = state -> !state.isAir();
	static final Predicate<BlockState> SUFFOCATES = AbstractBlock.AbstractBlockState::blocksMovement;

	private final PaletteStorage storage;
	private final Predicate<BlockState> blockPredicate;
	private final Chunk chunk;

	public Heightmap(Chunk chunk, Heightmap.Type type) {
		blockPredicate = type.getBlockPredicate();
		this.chunk = chunk;
		int bitsPerEntry = MathHelper.ceilLog2(chunk.getHeight() + 1);
		storage = new PackedIntegerArray(bitsPerEntry, SECTION_SIZE * SECTION_SIZE);
	}

	/**
	 * Заполняет карты высот для заданного набора типов, проходя чанк сверху вниз.
	 * Для каждой XZ-колонки ищет первый блок, удовлетворяющий предикату каждого типа.
	 * Алгоритм использует ObjectList для отслеживания незаполненных типов и
	 * прерывает обход колонки, как только все типы заполнены.
	 */
	public static void populateHeightmaps(Chunk chunk, Set<Heightmap.Type> types) {
		if (types.isEmpty()) {
			return;
		}

		int typeCount = types.size();
		ObjectList<Heightmap> pendingHeightmaps = new ObjectArrayList<>(typeCount);
		int topY = chunk.getTopYInclusive() + 1;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = 0; x < SECTION_SIZE; x++) {
			for (int z = 0; z < SECTION_SIZE; z++) {
				for (Heightmap.Type type : types) {
					pendingHeightmaps.add(chunk.getHeightmap(type));
				}

				ObjectListIterator<Heightmap> iterator = pendingHeightmaps.iterator();

				for (int y = topY - 1; y >= chunk.getBottomY(); y--) {
					mutable.set(x, y, z);
					BlockState blockState = chunk.getBlockState(mutable);
					if (blockState.isOf(Blocks.AIR)) {
						continue;
					}

					while (iterator.hasNext()) {
						Heightmap heightmap = iterator.next();
						if (heightmap.blockPredicate.test(blockState)) {
							heightmap.set(x, z, y + 1);
							iterator.remove();
						}
					}

					if (pendingHeightmaps.isEmpty()) {
						break;
					}

					iterator.back(typeCount);
				}

				pendingHeightmaps.clear();
			}
		}
	}

	/**
	 * Обновляет карту высот при изменении блока в позиции (x, y, z).
	 * Если новый блок выше текущей высоты — обновляет вверх.
	 * Если блок удалён на текущей высоте — сканирует вниз для поиска нового верхнего блока.
	 *
	 * @return {@code true} если высота изменилась
	 */
	public boolean trackUpdate(int x, int y, int z, BlockState state) {
		int currentHeight = get(x, z);
		if (y <= currentHeight - 2) {
			return false;
		}

		if (blockPredicate.test(state)) {
			if (y >= currentHeight) {
				set(x, z, y + 1);
				return true;
			}
		} else if (currentHeight - 1 == y) {
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (int scanY = y - 1; scanY >= chunk.getBottomY(); scanY--) {
				mutable.set(x, scanY, z);
				if (blockPredicate.test(chunk.getBlockState(mutable))) {
					set(x, z, scanY + 1);
					return true;
				}
			}

			set(x, z, chunk.getBottomY());
			return true;
		}

		return false;
	}

	/** Возвращает Y верхнего блока + 1 для позиции (x, z) в локальных координатах чанка. */
	public int get(int x, int z) {
		return get(toIndex(x, z));
	}

	/** Возвращает Y верхнего блока (без +1) для позиции (x, z). */
	public int getOneLower(int x, int z) {
		return get(toIndex(x, z)) - 1;
	}

	private int get(int index) {
		return storage.get(index) + chunk.getBottomY();
	}

	private void set(int x, int z, int height) {
		storage.set(toIndex(x, z), height - chunk.getBottomY());
	}

	/**
	 * Загружает данные карты высот из массива long[].
	 * Если размер не совпадает — логирует предупреждение и пересчитывает карту.
	 */
	public void setTo(Chunk chunk, Heightmap.Type type, long[] values) {
		long[] existing = storage.getData();
		if (existing.length == values.length) {
			System.arraycopy(values, 0, existing, 0, values.length);
		} else {
			LOGGER.warn(
				"Ignoring heightmap data for chunk {}, size does not match; expected: {}, got: {}",
				chunk.getPos(), existing.length, values.length
			);
			populateHeightmaps(chunk, EnumSet.of(type));
		}
	}

	public long[] asLongArray() {
		return storage.getData();
	}

	private static int toIndex(int x, int z) {
		return x + z * SECTION_SIZE;
	}

	/** Назначение карты высот: генерация мира, живой мир или клиент. */
	public enum Purpose {
		WORLDGEN,
		LIVE_WORLD,
		CLIENT
	}

	/**
	 * Тип карты высот, определяющий предикат «непрозрачного» блока и область применения.
	 * Используется для выбора нужной карты при запросах высоты в разных контекстах.
	 */
	public enum Type implements StringIdentifiable {
		WORLD_SURFACE_WG(0, "WORLD_SURFACE_WG", Heightmap.Purpose.WORLDGEN, Heightmap.NOT_AIR),
		WORLD_SURFACE(1, "WORLD_SURFACE", Heightmap.Purpose.CLIENT, Heightmap.NOT_AIR),
		OCEAN_FLOOR_WG(2, "OCEAN_FLOOR_WG", Heightmap.Purpose.WORLDGEN, Heightmap.SUFFOCATES),
		OCEAN_FLOOR(3, "OCEAN_FLOOR", Heightmap.Purpose.LIVE_WORLD, Heightmap.SUFFOCATES),
		MOTION_BLOCKING(
			4,
			"MOTION_BLOCKING",
			Heightmap.Purpose.CLIENT,
			state -> state.blocksMovement() || !state.getFluidState().isEmpty()
		),
		MOTION_BLOCKING_NO_LEAVES(
			5,
			"MOTION_BLOCKING_NO_LEAVES",
			Heightmap.Purpose.CLIENT,
			state -> (state.blocksMovement() || !state.getFluidState().isEmpty())
				&& !(state.getBlock() instanceof LeavesBlock)
		);

		public static final Codec<Heightmap.Type> CODEC =
			StringIdentifiable.createCodec(Heightmap.Type::values);

		private static final IntFunction<Heightmap.Type> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			(Heightmap.Type type) -> type.index,
			values(),
			ValueLists.OutOfBoundsHandling.ZERO
		);

		public static final PacketCodec<ByteBuf, Heightmap.Type> PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, type -> type.index);

		private final int index;
		private final String id;
		private final Heightmap.Purpose purpose;
		private final Predicate<BlockState> blockPredicate;

		Type(
			final int index,
			final String id,
			final Heightmap.Purpose purpose,
			final Predicate<BlockState> blockPredicate
		) {
			this.index = index;
			this.id = id;
			this.purpose = purpose;
			this.blockPredicate = blockPredicate;
		}

		public String getId() {
			return id;
		}

		public boolean shouldSendToClient() {
			return purpose == Heightmap.Purpose.CLIENT;
		}

		public boolean isStoredServerSide() {
			return purpose != Heightmap.Purpose.WORLDGEN;
		}

		public Predicate<BlockState> getBlockPredicate() {
			return blockPredicate;
		}

		@Override
		public String asString() {
			return id;
		}
	}
}
