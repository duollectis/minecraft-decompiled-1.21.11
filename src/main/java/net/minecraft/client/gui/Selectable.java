package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.Navigable;

import java.util.Collection;
import java.util.List;

/**
 * Интерфейс для элементов GUI, поддерживающих выделение и нарративный доступ.
 */
@Environment(EnvType.CLIENT)
public interface Selectable extends Navigable, Narratable {

	Selectable.SelectionType getType();

	default boolean isInteractable() {
		return true;
	}

	default Collection<? extends Selectable> getNarratedParts() {
		return List.of(this);
	}

	/**
	 * Тип выделения элемента: не выделен, наведён курсор или сфокусирован клавиатурой.
	 */
	@Environment(EnvType.CLIENT)
	public enum SelectionType {
		NONE,
		HOVERED,
		FOCUSED;

		public boolean isFocused() {
			return this == FOCUSED;
		}
	}
}
