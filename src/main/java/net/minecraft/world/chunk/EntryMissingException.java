package net.minecraft.world.chunk;

/**
 * {@code EntryMissingException}.
 */
public class EntryMissingException extends RuntimeException {

	public EntryMissingException(int index) {
		super("Missing Palette entry for index " + index + ".");
	}
}
