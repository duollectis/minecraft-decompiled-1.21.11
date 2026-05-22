package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.type.ColumnsDialog;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Базовый экран диалога с кнопками, расположенными в несколько колонок.
 */
@Environment(EnvType.CLIENT)
public abstract class ColumnsDialogScreen<T extends ColumnsDialog> extends DialogScreen<T> {

	public static final int COLUMN_SPACING = 5;

	public ColumnsDialogScreen(@Nullable Screen parent, T dialog, DialogNetworkAccess networkAccess) {
		super(parent, dialog, networkAccess);
	}

	protected void initBody(
			DirectionalLayoutWidget directionalLayoutWidget,
			DialogControls dialogControls,
			T columnsDialog,
			DialogNetworkAccess dialogNetworkAccess
	) {
		super.initBody(directionalLayoutWidget, dialogControls, columnsDialog, dialogNetworkAccess);
		List<ButtonWidget> list = this.streamActionButtonData(columnsDialog, dialogNetworkAccess)
		                              .map(actionButtonData -> dialogControls.createButton(actionButtonData).build())
		                              .toList();
		directionalLayoutWidget.add(createGridWidget(list, columnsDialog.columns()));
	}

	protected abstract Stream<DialogActionButtonData> streamActionButtonData(
			T dialog,
			DialogNetworkAccess networkAccess
	);

	protected void initHeaderAndFooter(
			ThreePartsLayoutWidget threePartsLayoutWidget,
			DialogControls dialogControls,
			T columnsDialog,
			DialogNetworkAccess dialogNetworkAccess
	) {
		super.initHeaderAndFooter(threePartsLayoutWidget, dialogControls, columnsDialog, dialogNetworkAccess);
		columnsDialog.exitAction()
		             .ifPresentOrElse(
				             actionButtonData -> threePartsLayoutWidget.addFooter(dialogControls
						             .createButton(actionButtonData)
						             .build()),
				             () -> threePartsLayoutWidget.setFooterHeight(5)
		             );
	}
}
