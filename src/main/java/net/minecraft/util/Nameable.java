package net.minecraft.util;

import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * {@code Nameable}.
 */
public interface Nameable {

	Text getName();

	default String getStringifiedName() {
		return this.getName().getString();
	}

	default boolean hasCustomName() {
		return this.getCustomName() != null;
	}

	default Text getDisplayName() {
		return this.getName();
	}

	default @Nullable Text getCustomName() {
		return null;
	}
}
