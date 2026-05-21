package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.type.*;
import net.minecraft.registry.Registry;

/**
 * {@code DialogTypes}.
 */
public class DialogTypes {

	public static MapCodec<? extends Dialog> registerAndGetDefault(Registry<MapCodec<? extends Dialog>> registry) {
		Registry.register(registry, "notice", NoticeDialog.CODEC);
		Registry.register(registry, "server_links", ServerLinksDialog.CODEC);
		Registry.register(registry, "dialog_list", DialogListDialog.CODEC);
		Registry.register(registry, "multi_action", MultiActionDialog.CODEC);
		return Registry.register(registry, "confirmation", ConfirmationDialog.CODEC);
	}
}
