package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Событие нажатия/отпускания кнопки мыши.
 * Содержит GLFW-код кнопки и битовую маску модификаторов клавиатуры.
 */
@Environment(EnvType.CLIENT)
public record MouseInput(
		@MouseInput.ButtonCode int button,
		@AbstractInput.Modifier int modifiers
) implements AbstractInput {

	@MouseInput.ButtonCode
	@Override
	public int getKeycode() {
		return button;
	}

	/** Аннотация-маркер для GLFW-кодов кнопок мыши (GLFW_MOUSE_BUTTON_*). */
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
	public @interface ButtonCode {
	}

	/** Аннотация-маркер для значений действия кнопки мыши (нажата/отпущена). */
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
	public @interface MouseAction {
	}
}
