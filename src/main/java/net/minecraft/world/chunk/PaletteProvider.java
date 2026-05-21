package net.minecraft.world.chunk;

import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.MathHelper;

/**
 * {@code PaletteProvider}.
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

	public static <T> PaletteProvider<T> forBlockStates(IndexedIterable<T> idList) {
		return new PaletteProvider<T>(idList, 4) {
			@Override
			public PaletteType createType(int bitsInStorage) {
				return (PaletteType) (switch (bitsInStorage) {
					case 0 -> PaletteProvider.SINGULAR_TYPE;
					case 1, 2, 3, 4 -> PaletteProvider.ARRAY_4_TYPE;
					case 5 -> PaletteProvider.BI_MAP_5_TYPE;
					case 6 -> PaletteProvider.BI_MAP_6_TYPE;
					case 7 -> PaletteProvider.BI_MAP_7_TYPE;
					case 8 -> PaletteProvider.BI_MAP_8_TYPE;
					default -> new PaletteType.Dynamic(this.bitsInMemory, bitsInStorage);
				}
				);
			}
		};
	}

	public static <T> PaletteProvider<T> forBiomes(IndexedIterable<T> idList) {
		return new PaletteProvider<T>(idList, 2) {
			@Override
			public PaletteType createType(int bitsInStorage) {
				return (PaletteType) (switch (bitsInStorage) {
					case 0 -> PaletteProvider.SINGULAR_TYPE;
					case 1 -> PaletteProvider.ARRAY_1_TYPE;
					case 2 -> PaletteProvider.ARRAY_2_TYPE;
					case 3 -> PaletteProvider.ARRAY_3_TYPE;
					default -> new PaletteType.Dynamic(this.bitsInMemory, bitsInStorage);
				}
				);
			}
		};
	}

	public int getSize() {
		return this.size;
	}

	public int computeIndex(int x, int y, int z) {
		return (y << this.bitsPerAxis | z) << this.bitsPerAxis | x;
	}

	public IndexedIterable<T> getIdList() {
		return this.idList;
	}

	public IdListPalette<T> getPalette() {
		return this.palette;
	}

	protected abstract PaletteType createType(int bitsInStorage);

	protected PaletteType createTypeFromSize(int size) {
		int i = toBits(size);
		return this.createType(i);
	}

	private static int toBits(int size) {
		return MathHelper.ceilLog2(size);
	}
}
