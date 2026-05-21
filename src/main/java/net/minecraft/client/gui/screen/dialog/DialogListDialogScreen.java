package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogButtonData;
import net.minecraft.dialog.action.SimpleDialogAction;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogListDialog;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.ClickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code DialogListDialogScreen}.
 */
public class DialogListDialogScreen extends ColumnsDialogScreen<DialogListDialog> {

	public DialogListDialogScreen(@Nullable Screen parent, DialogListDialog dialog, DialogNetworkAccess networkAccess) {
		super(parent, dialog, networkAccess);
	}

	protected Stream<DialogActionButtonData> streamActionButtonData(
			DialogListDialog dialogListDialog,
			DialogNetworkAccess dialogNetworkAccess
	) {
		return dialogListDialog
				.dialogs()
				.stream()
				.map(entry -> createButton(dialogListDialog, (RegistryEntry<Dialog>) entry));
	}

	private static DialogActionButtonData createButton(DialogListDialog dialog, RegistryEntry<Dialog> entry) {
		return new DialogActionButtonData(
				new DialogButtonData(entry.value().common().getExternalTitle(), dialog.buttonWidth()),
				Optional.of(new SimpleDialogAction(new ClickEvent.ShowDialog(entry)))
		);
	}
}
