package net.minecraft.world.chunk;

import java.util.List;

/**
 * {@code PaletteType}.
 */
public interface PaletteType {

	boolean shouldRepack();

	int bitsInMemory();

	int bitsInStorage();

	<T> Palette<T> createPalette(PaletteProvider<T> provider, List<T> values);

	/**
	 * {@code Dynamic}.
	 */
	public record Dynamic(int bitsInMemory, int bitsInStorage) implements PaletteType {

		@Override
		public boolean shouldRepack() {
			return true;
		}

		@Override
		public <T> Palette<T> createPalette(PaletteProvider<T> provider, List<T> values) {
			return provider.getPalette();
		}
	}

	/**
	 * {@code Static}.
	 */
	public record Static(Palette.Factory factory, int bits) implements PaletteType {

		@Override
		public boolean shouldRepack() {
			return false;
		}

		@Override
		public <T> Palette<T> createPalette(PaletteProvider<T> provider, List<T> values) {
			return this.factory.create(this.bits, values);
		}

		@Override
		public int bitsInMemory() {
			return this.bits;
		}

		@Override
		public int bitsInStorage() {
			return this.bits;
		}
	}
}
