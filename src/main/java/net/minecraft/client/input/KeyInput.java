package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Событие нажатия/отпускания клавиши клавиатуры.
 * Содержит GLFW-код клавиши, скан-код и битовую маску модификаторов.
 */
@Environment(EnvType.CLIENT)
public record KeyInput(
		@InputUtil.Keycode int key,
		int scancode,
		@AbstractInput.Modifier int modifiers
) implements AbstractInput {

	@Override
	public int getKeycode() {
		return key;
	}

	/** Аннотация-маркер для значений действия клавиши (нажата/отпущена/удерживается). */
	@Retention(RetentionPolicy.CLASS)
	@Target(
			{
					ElementType.FIELD,
					ElementType.PARAMETER,
					ElementType.LOCAL_VARIABLE,
					ElementType.METHOD,
					ElementType.TYPE_USE
			}
	)
	@Environment(EnvType.CLIENT)
	public @interface KeyAction {
	}
}
