package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.input.MouseInput;

/**
 * Представляет событие клика мышью: координаты курсора и информацию о нажатой кнопке.
 */
@Environment(EnvType.CLIENT)
public record Click(double x, double y, MouseInput buttonInfo) implements AbstractInput {

	@Override
	public int getKeycode() {
		return button();
	}

	@MouseInput.ButtonCode
	public int button() {
		return buttonInfo().button();
	}

	@AbstractInput.Modifier
	@Override
	public int modifiers() {
		return buttonInfo().modifiers();
	}
}
