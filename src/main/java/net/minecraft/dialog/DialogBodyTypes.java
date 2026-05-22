package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.ItemDialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Регистратор типов тела диалогов.
 * <p>
 * Регистрирует все доступные типы {@link DialogBody} в реестре и возвращает
 * тип по умолчанию ({@code plain_message}).
 */
public class DialogBodyTypes {

	/**
	 * Регистрирует все типы тела диалогов и возвращает тип по умолчанию.
	 *
	 * @param registry реестр кодеков типов тела
	 * @return кодек типа тела по умолчанию ({@code plain_message})
	 */
	public static MapCodec<? extends DialogBody> registerAndGetDefault(Registry<MapCodec<? extends DialogBody>> registry) {
		Registry.register(registry, Identifier.ofVanilla("item"), ItemDialogBody.CODEC);
		return Registry.register(registry, Identifier.ofVanilla("plain_message"), PlainMessageDialogBody.CODEC);
	}
}
