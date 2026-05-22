package net.minecraft.world.chunk;

import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.MathHelper;

/**
 * Стратегия выбора типа палитры в зависимости от числа бит на элемент.
 * Определяет, какую реализацию {@link Palette} использовать для блок-стейтов
 * и биомов, а также вычисляет линейный индекс по координатам (x, y, z).
 */
public abstract class PaletteProvider<T> {

	private static final Palette.Factory SINGULAR = SingularPalette::create;
	private static final Palette.Factory ARRAY = ArrayPalette::create;
	private static final Palette.Factory BI_MAP = BiMapPalette::create;

	static final PaletteType SINGULAR_TYPE = new PaletteType.Static(SINGULAR, 0);
	static final PaletteType ARRAY_1_TYPE = new PaletteType.Static(ARRAY, 1);
	static final PaletteType ARRAY_2_TYPE = new PaletteType.Static(ARRAY, 2);
	static final PaletteType ARRAY_3_TYPE = new PaletteType.Static(ARRAY, 3);
	static final PaletteType ARRAY_4_TYPE = new PaletteType.Static(ARRAY, 4);
	static final PaletteType BI_MAP_5_TYPE = new PaletteType.Static(BI_MAP, 5);
	static final PaletteType BI_MAP_6_TYPE = new PaletteType.Static(BI_MAP, 6);
	static final PaletteType BI_MAP_7_TYPE = new PaletteType.Static(BI_MAP, 7);
	static final PaletteType BI_MAP_8_TYPE = new PaletteType.Static(BI_MAP, 8);

	private final IndexedIterable<T> idList;
	private final IdListPalette<T> palette;
	protected final int bitsInMemory;
	private final int bitsPerAxis;
	private final int size;

	PaletteProvider(IndexedIterable<T> idList, int bitsPerAxis) {
		this.idList = idList;
		this.palette = new IdListPalette<>(idList);
		this.bitsInMemory = toBits(idList.size());
		this.bitsPerAxis = bitsPerAxis;
		this.size = 1 << bitsPerAxis * 3;
	}

	/**
	 * Создаёт провайдер для блок-стейтов (секция 16×16×16, 4 бита на ось).
	 * Использует линейную палитру до 4 бит, BiMap до 8 бит, затем глобальный реестр.
	 */
	public static <T> PaletteProvider<T> forBlockStates(IndexedIterable<T> idList) {
		return new PaletteProvider<T>(idList, 4) {
			@Override
			public PaletteType createType(int bitsInStorage) {
				return switch (bitsInStorage) {
					case 0 -> SINGULAR_TYPE;
					case 1, 2, 3, 4 -> ARRAY_4_TYPE;
					case 5 -> BI_MAP_5_TYPE;
					case 6 -> BI_MAP_6_TYPE;
					case 7 -> BI_MAP_7_TYPE;
					case 8 -> BI_MAP_8_TYPE;
					default -> new PaletteType.Dynamic(bitsInMemory, bitsInStorage);
				};
			}
		};
	}

	/**
	 * Создаёт провайдер для биомов (секция 4×4×4, 2 бита на ось).
	 * Использует линейную палитру до 3 бит, затем глобальный реестр.
	 */
	public static <T> PaletteProvider<T> forBiomes(IndexedIterable<T> idList) {
		return new PaletteProvider<T>(idList, 2) {
			@Override
			public PaletteType createType(int bitsInStorage) {
				return switch (bitsInStorage) {
					case 0 -> SINGULAR_TYPE;
					case 1 -> ARRAY_1_TYPE;
					case 2 -> ARRAY_2_TYPE;
					case 3 -> ARRAY_3_TYPE;
					default -> new PaletteType.Dynamic(bitsInMemory, bitsInStorage);
				};
			}
		};
	}

	public int getSize() {
		return size;
	}

	/**
	 * Вычисляет линейный индекс элемента по трёхмерным координатам внутри секции.
	 * Формула: {@code (y << bitsPerAxis | z) << bitsPerAxis | x}.
	 */
	public int computeIndex(int x, int y, int z) {
		return (y << bitsPerAxis | z) << bitsPerAxis | x;
	}

	public IndexedIterable<T> getIdList() {
		return idList;
	}

	public IdListPalette<T> getPalette() {
		return palette;
	}

	protected abstract PaletteType createType(int bitsInStorage);

	protected PaletteType createTypeFromSize(int size) {
		return createType(toBits(size));
	}

	private static int toBits(int size) {
		return MathHelper.ceilLog2(size);
	}
}
