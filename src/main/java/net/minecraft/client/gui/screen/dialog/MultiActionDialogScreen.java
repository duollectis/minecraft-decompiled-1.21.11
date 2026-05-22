package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.type.MultiActionDialog;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Экран диалога с несколькими действиями, отображаемыми в виде кнопок-колонок.
 */
@Environment(EnvType.CLIENT)
public class MultiActionDialogScreen extends ColumnsDialogScreen<MultiActionDialog> {

	public MultiActionDialogScreen(
			@Nullable Screen parent,
			MultiActionDialog dialog,
			DialogNetworkAccess networkAccess
	) {
		super(parent, dialog, networkAccess);
	}

	protected Stream<DialogActionButtonData> streamActionButtonData(
			MultiActionDialog multiActionDialog,
			DialogNetworkAccess dialogNetworkAccess
	) {
		return multiActionDialog.actions().stream();
	}
}
