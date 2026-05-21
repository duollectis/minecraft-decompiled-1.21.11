package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.action.DialogAction;

import java.util.Optional;

/**
 * {@code ColumnsDialog}.
 */
public interface ColumnsDialog extends Dialog {

	@Override
	MapCodec<? extends ColumnsDialog> getCodec();

	int columns();

	Optional<DialogActionButtonData> exitAction();

	@Override
	default Optional<DialogAction> getCancelAction() {
		return this.exitAction().flatMap(DialogActionButtonData::action);
	}
}
