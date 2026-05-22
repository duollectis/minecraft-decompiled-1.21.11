package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.dialog.action.DialogAction;

import java.util.List;
import java.util.Optional;

/**
 * Диалог подтверждения с двумя кнопками: «Да» и «Нет».
 *
 * <p>Действие кнопки «Нет» используется как действие отмены диалога.</p>
 */
public record ConfirmationDialog(
	DialogCommonData common,
	DialogActionButtonData yesButton,
	DialogActionButtonData noButton
) implements SimpleDialog {

	public static final MapCodec<ConfirmationDialog> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			DialogCommonData.CODEC.forGetter(ConfirmationDialog::common),
			DialogActionButtonData.CODEC.fieldOf("yes").forGetter(ConfirmationDialog::yesButton),
			DialogActionButtonData.CODEC.fieldOf("no").forGetter(ConfirmationDialog::noButton)
		)
		.apply(instance, ConfirmationDialog::new)
	);

	@Override
	public MapCodec<ConfirmationDialog> getCodec() {
		return CODEC;
	}

	@Override
	public Optional<DialogAction> getCancelAction() {
		return noButton.action();
	}

	@Override
	public List<DialogActionButtonData> getButtons() {
		return List.of(yesButton, noButton);
	}
}
