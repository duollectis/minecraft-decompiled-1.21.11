package net.minecraft.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

/**
 * Элемент тела диалога с простым текстовым сообщением.
 * <p>
 * Поддерживает альтернативный кодек {@link #ALTERNATIVE_CODEC}, позволяющий
 * задавать тело диалога как строку текста без явного указания ширины.
 *
 * @param contents текстовое содержимое
 * @param width    ширина блока текста в пикселях (по умолчанию {@link #DEFAULT_WIDTH})
 */
public record PlainMessageDialogBody(Text contents, int width) implements DialogBody {

	public static final int DEFAULT_WIDTH = 200;

	public static final MapCodec<PlainMessageDialogBody> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			TextCodecs.CODEC.fieldOf("contents").forGetter(PlainMessageDialogBody::contents),
			Dialog.WIDTH_CODEC.optionalFieldOf("width", DEFAULT_WIDTH).forGetter(PlainMessageDialogBody::width)
		).apply(instance, PlainMessageDialogBody::new)
	);

	/**
	 * Альтернативный кодек: принимает либо полный объект с полем {@code contents},
	 * либо просто текст напрямую (с шириной по умолчанию).
	 */
	public static final Codec<PlainMessageDialogBody> ALTERNATIVE_CODEC = Codec.withAlternative(
		CODEC.codec(),
		TextCodecs.CODEC,
		contents -> new PlainMessageDialogBody(contents, DEFAULT_WIDTH)
	);

	@Override
	public MapCodec<PlainMessageDialogBody> getTypeCodec() {
		return CODEC;
	}
}
