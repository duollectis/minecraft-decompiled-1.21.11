package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.type.SimpleDialog;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code SimpleDialogScreen}.
 */
public class SimpleDialogScreen<T extends SimpleDialog> extends DialogScreen<T> {

	public SimpleDialogScreen(@Nullable Screen parent, T dialog, DialogNetworkAccess networkAccess) {
		super(parent, dialog, networkAccess);
	}

	protected void initHeaderAndFooter(
			ThreePartsLayoutWidget threePartsLayoutWidget,
			DialogControls dialogControls,
			T simpleDialog,
			DialogNetworkAccess dialogNetworkAccess
	) {
		super.initHeaderAndFooter(threePartsLayoutWidget, dialogControls, simpleDialog, dialogNetworkAccess);
		DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.horizontal().spacing(8);

		for (DialogActionButtonData dialogActionButtonData : simpleDialog.getButtons()) {
			directionalLayoutWidget.add(dialogControls.createButton(dialogActionButtonData).build());
		}

		threePartsLayoutWidget.addFooter(directionalLayoutWidget);
	}
}
