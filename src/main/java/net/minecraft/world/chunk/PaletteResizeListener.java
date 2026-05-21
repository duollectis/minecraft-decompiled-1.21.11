package net.minecraft.world.chunk;

/**
 * {@code PaletteResizeListener}.
 */
public interface PaletteResizeListener<T> {

	int onResize(int newBits, T object);

	static <T> PaletteResizeListener<T> throwing() {
		return (newBits, object) -> {
			throw new IllegalArgumentException(
					"Unexpected palette resize, bits = " + newBits + ", added value = " + object);
		};
	}
}
