package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Диалог, отображающий ссылки сервера в виде кнопок.
 *
 * <p>Ссылки берутся из серверного пакета {@code ServerLinksS2CPacket}
 * и отображаются в {@code columns} колонок с шириной {@code buttonWidth}.</p>
 */
public record ServerLinksDialog(
	DialogCommonData common,
	Optional<DialogActionButtonData> exitAction,
	int columns,
	int buttonWidth
) implements ColumnsDialog {

	private static final int DEFAULT_COLUMNS = 2;
	private static final int DEFAULT_BUTTON_WIDTH = 150;

	public static final MapCodec<ServerLinksDialog> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			DialogCommonData.CODEC.forGetter(ServerLinksDialog::common),
			DialogActionButtonData.CODEC
				.optionalFieldOf("exit_action")
				.forGetter(ServerLinksDialog::exitAction),
			Codecs.POSITIVE_INT.optionalFieldOf("columns", DEFAULT_COLUMNS).forGetter(ServerLinksDialog::columns),
			WIDTH_CODEC.optionalFieldOf("button_width", DEFAULT_BUTTON_WIDTH).forGetter(ServerLinksDialog::buttonWidth)
		)
		.apply(instance, ServerLinksDialog::new)
	);

	@Override
	public MapCodec<ServerLinksDialog> getCodec() {
		return CODEC;
	}
}
