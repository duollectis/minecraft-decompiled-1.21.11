package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Элемент управления текстовым вводом.
 * <p>
 * Поддерживает однострочный и многострочный режимы через {@link Multiline}.
 * Начальный текст не может превышать {@link #maxLength} символов.
 *
 * @param width     ширина поля ввода в пикселях
 * @param label     текст метки
 * @param labelVisible отображать ли метку
 * @param initial   начальный текст поля ввода
 * @param maxLength максимальная длина вводимого текста
 * @param multiline параметры многострочного режима (если задан)
 */
public record TextInputControl(
	int width,
	Text label,
	boolean labelVisible,
	String initial,
	int maxLength,
	Optional<Multiline> multiline
) implements InputControl {

	public static final MapCodec<TextInputControl> CODEC = RecordCodecBuilder.<TextInputControl>mapCodec(
		instance -> instance.group(
			Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(TextInputControl::width),
			TextCodecs.CODEC.fieldOf("label").forGetter(TextInputControl::label),
			Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(TextInputControl::labelVisible),
			Codec.STRING.optionalFieldOf("initial", "").forGetter(TextInputControl::initial),
			Codecs.POSITIVE_INT.optionalFieldOf("max_length", 32).forGetter(TextInputControl::maxLength),
			Multiline.CODEC.optionalFieldOf("multiline").forGetter(TextInputControl::multiline)
		).apply(instance, TextInputControl::new)
	).validate(
		inputControl -> inputControl.initial.length() > inputControl.maxLength()
			? DataResult.error(() -> "Default text length exceeds allowed size")
			: DataResult.success(inputControl)
	);

	@Override
	public MapCodec<TextInputControl> getCodec() {
		return CODEC;
	}

	/**
	 * Параметры многострочного режима текстового поля.
	 *
	 * @param maxLines максимальное количество строк (опционально)
	 * @param height   высота поля в пикселях, от 1 до {@link #MAX_HEIGHT} (опционально)
	 */
	public record Multiline(Optional<Integer> maxLines, Optional<Integer> height) {

		public static final int MAX_HEIGHT = 512;

		public static final Codec<Multiline> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codecs.POSITIVE_INT.optionalFieldOf("max_lines").forGetter(Multiline::maxLines),
				Codecs.rangedInt(1, MAX_HEIGHT).optionalFieldOf("height").forGetter(Multiline::height)
			).apply(instance, Multiline::new)
		);
	}
}
