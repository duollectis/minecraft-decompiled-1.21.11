package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

/**
 * Набор текстур для кнопки в зависимости от её состояния (активна/неактивна, в фокусе/нет).
 */
@Environment(EnvType.CLIENT)
public record ButtonTextures(
		Identifier enabled,
		Identifier disabled,
		Identifier enabledFocused,
		Identifier disabledFocused
) {

	public ButtonTextures(Identifier texture) {
		this(texture, texture, texture, texture);
	}

	public ButtonTextures(Identifier unfocused, Identifier focused) {
		this(unfocused, unfocused, focused, focused);
	}

	public ButtonTextures(Identifier enabled, Identifier disabled, Identifier focused) {
		this(enabled, disabled, focused, disabled);
	}

	public Identifier get(boolean enabled, boolean focused) {
		if (enabled) {
			return focused ? enabledFocused : enabled();
		} else {
			return focused ? disabledFocused : disabled();
		}
	}
}
