package net.minecraft.world.chunk;

import java.util.List;

/**
 * Описывает конфигурацию палитры: сколько бит используется в памяти и при хранении,
 * нужно ли перепаковывать данные при десериализации.
 */
public interface PaletteType {

	boolean shouldRepack();

	int bitsInMemory();

	int bitsInStorage();

	<T> Palette<T> createPalette(PaletteProvider<T> provider, List<T> values);

	/**
	 * Динамическая палитра — использует глобальный реестр идентификаторов.
	 * Требует перепаковки при загрузке, так как биты хранения могут отличаться от памяти.
	 */
	record Dynamic(int bitsInMemory, int bitsInStorage) implements PaletteType {

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
	 * Статическая палитра с фиксированным числом бит.
	 * Не требует перепаковки — биты памяти и хранения совпадают.
	 */
	record Static(Palette.Factory factory, int bits) implements PaletteType {

		@Override
		public boolean shouldRepack() {
			return false;
		}

		@Override
		public <T> Palette<T> createPalette(PaletteProvider<T> provider, List<T> values) {
			return factory.create(bits, values);
		}

		@Override
		public int bitsInMemory() {
			return bits;
		}

		@Override
		public int bitsInStorage() {
			return bits;
		}
	}
}
