package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;

/**
 * {@code MultiActionDialog}.
 */
public record MultiActionDialog(
		DialogCommonData common,
		List<DialogActionButtonData> actions,
		Optional<DialogActionButtonData> exitAction,
		int columns
)
		implements ColumnsDialog {

	public static final MapCodec<MultiActionDialog> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    DialogCommonData.CODEC.forGetter(MultiActionDialog::common),
					                    Codecs
							                    .nonEmptyList(DialogActionButtonData.CODEC.listOf())
							                    .fieldOf("actions")
							                    .forGetter(MultiActionDialog::actions),
					                    DialogActionButtonData.CODEC
							                    .optionalFieldOf("exit_action")
							                    .forGetter(MultiActionDialog::exitAction),
					                    Codecs.POSITIVE_INT.optionalFieldOf("columns", 2).forGetter(MultiActionDialog::columns)
			                    )
			                    .apply(instance, MultiActionDialog::new)
	);

	@Override
	public MapCodec<MultiActionDialog> getCodec() {
		return CODEC;
	}
}
