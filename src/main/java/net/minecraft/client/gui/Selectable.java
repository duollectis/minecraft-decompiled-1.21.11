package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.Navigable;

import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code Selectable}.
 */
public interface Selectable extends Navigable, Narratable {

	Selectable.SelectionType getType();

	default boolean isInteractable() {
		return true;
	}

	default Collection<? extends Selectable> getNarratedParts() {
		return List.of(this);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SelectionType}.
	 */
	public static enum SelectionType {
		NONE,
		HOVERED,
		FOCUSED;

		public boolean isFocused() {
			return this == FOCUSED;
		}
	}
}
