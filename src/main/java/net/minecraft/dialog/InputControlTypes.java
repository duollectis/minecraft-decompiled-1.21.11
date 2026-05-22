package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.input.BooleanInputControl;
import net.minecraft.dialog.input.InputControl;
import net.minecraft.dialog.input.NumberRangeInputControl;
import net.minecraft.dialog.input.SingleOptionInputControl;
import net.minecraft.dialog.input.TextInputControl;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Регистратор типов элементов управления вводом диалогов.
 * <p>
 * Регистрирует все доступные типы {@link InputControl} в реестре и возвращает
 * тип по умолчанию ({@code text}).
 */
public class InputControlTypes {

	/**
	 * Регистрирует все типы элементов управления вводом и возвращает тип по умолчанию.
	 *
	 * @param registry реестр кодеков типов элементов управления
	 * @return кодек типа по умолчанию ({@code text})
	 */
	public static MapCodec<? extends InputControl> registerAndGetDefault(Registry<MapCodec<? extends InputControl>> registry) {
		Registry.register(registry, Identifier.ofVanilla("boolean"), BooleanInputControl.CODEC);
		Registry.register(registry, Identifier.ofVanilla("number_range"), NumberRangeInputControl.CODEC);
		Registry.register(registry, Identifier.ofVanilla("single_option"), SingleOptionInputControl.CODEC);
		return Registry.register(registry, Identifier.ofVanilla("text"), TextInputControl.CODEC);
	}
}
