package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.action.DialogAction;
import net.minecraft.dialog.action.DynamicCustomDialogAction;
import net.minecraft.dialog.action.DynamicRunCommandDialogAction;
import net.minecraft.dialog.action.SimpleDialogAction;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Регистратор типов действий диалогов.
 * <p>
 * Регистрирует все доступные типы {@link DialogAction} в реестре и возвращает
 * тип по умолчанию ({@code dynamic/custom}).
 */
public class DialogActionTypes {

	/**
	 * Регистрирует все типы действий диалогов и возвращает тип по умолчанию.
	 *
	 * @param registry реестр кодеков типов действий
	 * @return кодек типа действия по умолчанию ({@code dynamic/custom})
	 */
	public static MapCodec<? extends DialogAction> registerAndGetDefault(Registry<MapCodec<? extends DialogAction>> registry) {
		SimpleDialogAction.CODECS.forEach((clickEventAction, codec) -> Registry.register(
			registry,
			Identifier.ofVanilla(clickEventAction.asString()),
			codec
		));
		Registry.register(registry, Identifier.ofVanilla("dynamic/run_command"), DynamicRunCommandDialogAction.CODEC);
		return Registry.register(registry, Identifier.ofVanilla("dynamic/custom"), DynamicCustomDialogAction.CODEC);
	}
}
