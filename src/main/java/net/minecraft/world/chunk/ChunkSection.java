package net.minecraft.world.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.function.Predicate;

/**
 * Секция чанка размером 16×16×16 блоков. Хранит состояния блоков и биомы
 * в палитризованных контейнерах. Ведёт счётчики непустых блоков и жидкостей
 * для быстрой проверки пустоты и наличия случайных тиков.
 */
public class ChunkSection {

	public static final int SIZE = 16;
	public static final int SIZE_BITS = 16;
	public static final int BLOCK_COUNT = 4096;
	public static final int BIOME_COORD_BITS = 2;
	// Размер биомной сетки внутри секции (4×4×4)
	private static final int BIOME_GRID_SIZE = 4;

	private short nonEmptyBlockCount;
	private short randomTickableBlockCount;
	private short nonEmptyFluidCount;
	private final PalettedContainer<BlockState> blockStateContainer;
	private ReadableContainer<RegistryEntry<Biome>> biomeContainer;

	private ChunkSection(ChunkSection source) {
		this.nonEmptyBlockCount = source.nonEmptyBlockCount;
		this.randomTickableBlockCount = source.randomTickableBlockCount;
		this.nonEmptyFluidCount = source.nonEmptyFluidCount;
		this.blockStateContainer = source.blockStateContainer.copy();
		this.biomeContainer = source.biomeContainer.copy();
	}

	public ChunkSection(
		PalettedContainer<BlockState> blockStateContainer,
		ReadableContainer<RegistryEntry<Biome>> biomeContainer
	) {
		this.blockStateContainer = blockStateContainer;
		this.biomeContainer = biomeContainer;
		calculateCounts();
	}

	public ChunkSection(PalettesFactory palettesFactory) {
		this.blockStateContainer = palettesFactory.getBlockStateContainer();
		this.biomeContainer = palettesFactory.getBiomeContainer();
	}

	public BlockState getBlockState(int x, int y, int z) {
		return blockStateContainer.get(x, y, z);
	}

	public FluidState getFluidState(int x, int y, int z) {
		return blockStateContainer.get(x, y, z).getFluidState();
	}

	public void lock() {
		blockStateContainer.lock();
	}

	public void unlock() {
		blockStateContainer.unlock();
	}

	public BlockState setBlockState(int x, int y, int z, BlockState state) {
		return setBlockState(x, y, z, state, true);
	}

	/**
	 * Устанавливает состояние блока и обновляет счётчики непустых блоков и жидкостей.
	 *
	 * @param lock если {@code true} — использует потокобезопасный swap, иначе небезопасный
	 * @return предыдущее состояние блока
	 */
	public BlockState setBlockState(int x, int y, int z, BlockState state, boolean lock) {
		BlockState previous = lock
			? blockStateContainer.swap(x, y, z, state)
			: blockStateContainer.swapUnsafe(x, y, z, state);

		FluidState previousFluid = previous.getFluidState();
		FluidState newFluid = state.getFluidState();

		if (!previous.isAir()) {
			nonEmptyBlockCount--;
			if (previous.hasRandomTicks()) {
				randomTickableBlockCount--;
			}
		}

		if (!previousFluid.isEmpty()) {
			nonEmptyFluidCount--;
		}

		if (!state.isAir()) {
			nonEmptyBlockCount++;
			if (state.hasRandomTicks()) {
				randomTickableBlockCount++;
			}
		}

		if (!newFluid.isEmpty()) {
			nonEmptyFluidCount++;
		}

		return previous;
	}

	public boolean isEmpty() {
		return nonEmptyBlockCount == 0;
	}

	public boolean hasRandomTicks() {
		return hasRandomBlockTicks() || hasRandomFluidTicks();
	}

	public boolean hasRandomBlockTicks() {
		return randomTickableBlockCount > 0;
	}

	public boolean hasRandomFluidTicks() {
		return nonEmptyFluidCount > 0;
	}

	/**
	 * Пересчитывает счётчики непустых блоков, случайно-тикающих блоков и жидкостей
	 * путём полного обхода палитры. Вызывается после десериализации секции.
	 */
	public void calculateCounts() {
		class BlockStateCounter implements PalettedContainer.Counter<BlockState> {

			int nonEmptyBlockCount;
			int randomTickableBlockCount;
			int nonEmptyFluidCount;

			public void accept(BlockState blockState, int count) {
				FluidState fluidState = blockState.getFluidState();

				if (!blockState.isAir()) {
					nonEmptyBlockCount += count;
					if (blockState.hasRandomTicks()) {
						randomTickableBlockCount += count;
					}
				}

				if (!fluidState.isEmpty()) {
					nonEmptyBlockCount += count;
					if (fluidState.hasRandomTicks()) {
						nonEmptyFluidCount += count;
					}
				}
			}
		}

		BlockStateCounter counter = new BlockStateCounter();
		blockStateContainer.count(counter);
		nonEmptyBlockCount = (short) counter.nonEmptyBlockCount;
		randomTickableBlockCount = (short) counter.randomTickableBlockCount;
		nonEmptyFluidCount = (short) counter.nonEmptyFluidCount;
	}

	public PalettedContainer<BlockState> getBlockStateContainer() {
		return blockStateContainer;
	}

	public ReadableContainer<RegistryEntry<Biome>> getBiomeContainer() {
		return biomeContainer;
	}

	public void readDataPacket(PacketByteBuf buf) {
		nonEmptyBlockCount = buf.readShort();
		blockStateContainer.readPacket(buf);
		PalettedContainer<RegistryEntry<Biome>> newBiomeContainer = biomeContainer.slice();
		newBiomeContainer.readPacket(buf);
		biomeContainer = newBiomeContainer;
	}

	public void readBiomePacket(PacketByteBuf buf) {
		PalettedContainer<RegistryEntry<Biome>> newBiomeContainer = biomeContainer.slice();
		newBiomeContainer.readPacket(buf);
		biomeContainer = newBiomeContainer;
	}

	public void toPacket(PacketByteBuf buf) {
		buf.writeShort(nonEmptyBlockCount);
		blockStateContainer.writePacket(buf);
		biomeContainer.writePacket(buf);
	}

	public int getPacketSize() {
		return 2 + blockStateContainer.getPacketSize() + biomeContainer.getPacketSize();
	}

	public boolean hasAny(Predicate<BlockState> predicate) {
		return blockStateContainer.hasAny(predicate);
	}

	public RegistryEntry<Biome> getBiome(int x, int y, int z) {
		return biomeContainer.get(x, y, z);
	}

	/**
	 * Заполняет биомный контейнер секции через {@link BiomeSupplier}.
	 * Итерирует биомную сетку 4×4×4 внутри секции.
	 *
	 * @param x начальная X-координата в биомных единицах
	 * @param y начальная Y-координата в биомных единицах
	 * @param z начальная Z-координата в биомных единицах
	 */
	public void populateBiomes(
		BiomeSupplier biomeSupplier,
		MultiNoiseUtil.MultiNoiseSampler sampler,
		int x,
		int y,
		int z
	) {
		PalettedContainer<RegistryEntry<Biome>> newBiomeContainer = biomeContainer.slice();

		for (int bx = 0; bx < BIOME_GRID_SIZE; bx++) {
			for (int by = 0; by < BIOME_GRID_SIZE; by++) {
				for (int bz = 0; bz < BIOME_GRID_SIZE; bz++) {
					newBiomeContainer.swapUnsafe(bx, by, bz, biomeSupplier.getBiome(x + bx, y + by, z + bz, sampler));
				}
			}
		}

		biomeContainer = newBiomeContainer;
	}

	public ChunkSection copy() {
		return new ChunkSection(this);
	}
}
