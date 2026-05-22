package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.type.ConfirmationDialog;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogListDialog;
import net.minecraft.dialog.type.MultiActionDialog;
import net.minecraft.dialog.type.NoticeDialog;
import net.minecraft.dialog.type.ServerLinksDialog;
import net.minecraft.registry.Registry;

/**
 * Регистратор типов диалогов.
 * <p>
 * Регистрирует все доступные типы {@link Dialog} в реестре и возвращает
 * тип по умолчанию ({@code confirmation}).
 */
public class DialogTypes {

	/**
	 * Регистрирует все типы диалогов и возвращает тип по умолчанию.
	 *
	 * @param registry реестр кодеков типов диалогов
	 * @return кодек типа диалога по умолчанию ({@code confirmation})
	 */
	public static MapCodec<? extends Dialog> registerAndGetDefault(Registry<MapCodec<? extends Dialog>> registry) {
		Registry.register(registry, "notice", NoticeDialog.CODEC);
		Registry.register(registry, "server_links", ServerLinksDialog.CODEC);
		Registry.register(registry, "dialog_list", DialogListDialog.CODEC);
		Registry.register(registry, "multi_action", MultiActionDialog.CODEC);
		return Registry.register(registry, "confirmation", ConfirmationDialog.CODEC);
	}
}
