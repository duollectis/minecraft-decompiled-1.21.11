package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Диалог, отображающий список других диалогов в виде кнопок.
 *
 * <p>Каждый диалог из списка {@code dialogs} представлен отдельной кнопкой.
 * Кнопки располагаются в {@code columns} колонок с шириной {@code buttonWidth}.</p>
 */
public record DialogListDialog(
	DialogCommonData common,
	RegistryEntryList<Dialog> dialogs,
	Optional<DialogActionButtonData> exitAction,
	int columns,
	int buttonWidth
) implements ColumnsDialog {

	private static final int DEFAULT_COLUMNS = 2;
	private static final int DEFAULT_BUTTON_WIDTH = 150;

	public static final MapCodec<DialogListDialog> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			DialogCommonData.CODEC.forGetter(DialogListDialog::common),
			Dialog.ENTRY_LIST_CODEC.fieldOf("dialogs").forGetter(DialogListDialog::dialogs),
			DialogActionButtonData.CODEC.optionalFieldOf("exit_action").forGetter(DialogListDialog::exitAction),
			Codecs.POSITIVE_INT.optionalFieldOf("columns", DEFAULT_COLUMNS).forGetter(DialogListDialog::columns),
			WIDTH_CODEC.optionalFieldOf("button_width", DEFAULT_BUTTON_WIDTH).forGetter(DialogListDialog::buttonWidth)
		)
		.apply(instance, DialogListDialog::new)
	);

	@Override
	public MapCodec<DialogListDialog> getCodec() {
		return CODEC;
	}
}
