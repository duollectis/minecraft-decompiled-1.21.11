package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code CharInput}.
 */
public record CharInput(int codepoint, @AbstractInput.Modifier int modifiers) {

	public String asString() {
		return Character.toString(this.codepoint);
	}

	public boolean isValidChar() {
		return StringHelper.isValidChar(this.codepoint);
	}
}
