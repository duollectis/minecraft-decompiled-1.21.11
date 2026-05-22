package net.minecraft.world.chunk;

/** Бросается при обращении к несуществующему индексу в палитре. */
public class EntryMissingException extends RuntimeException {

	public EntryMissingException(int index) {
		super("Missing Palette entry for index " + index + ".");
	}
}
