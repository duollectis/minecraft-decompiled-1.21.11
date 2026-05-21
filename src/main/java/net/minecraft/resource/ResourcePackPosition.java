package net.minecraft.resource;

/**
 * {@code ResourcePackPosition}.
 */
public record ResourcePackPosition(
		boolean required,
		ResourcePackProfile.InsertionPosition defaultPosition,
		boolean fixedPosition
) {
}
