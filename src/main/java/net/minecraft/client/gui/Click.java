package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.input.MouseInput;

@Environment(EnvType.CLIENT)
/**
 * {@code Click}.
 */
public record Click(double x, double y, MouseInput buttonInfo) implements AbstractInput {

	@Override
	public int getKeycode() {
		return this.button();
	}

	@MouseInput.ButtonCode
	/**
	 * Button.
	 *
	 * @return int — результат операции
	 */
	public int button() {
		return this.buttonInfo().button();
	}

	@AbstractInput.Modifier
	@Override
	public int modifiers() {
		return this.buttonInfo().modifiers();
	}
}
