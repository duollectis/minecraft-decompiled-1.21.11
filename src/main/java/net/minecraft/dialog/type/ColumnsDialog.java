package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.action.DialogAction;

import java.util.Optional;

/**
 * Диалог с поддержкой многоколоночного расположения кнопок.
 *
 * <p>Предоставляет опциональную кнопку выхода, действие которой
 * используется как действие отмены диалога.</p>
 */
public interface ColumnsDialog extends Dialog {

	@Override
	MapCodec<? extends ColumnsDialog> getCodec();

	int columns();

	Optional<DialogActionButtonData> exitAction();

	@Override
	default Optional<DialogAction> getCancelAction() {
		return exitAction().flatMap(DialogActionButtonData::action);
	}
}
