package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.Optional;

/**
 * Визуальные данные кнопки диалога: текст, подсказка и ширина.
 *
 * @param label   текст на кнопке
 * @param tooltip опциональная подсказка при наведении
 * @param width   ширина кнопки в пикселях (по умолчанию {@link #DEFAULT_WIDTH})
 */
public record DialogButtonData(Text label, Optional<Text> tooltip, int width) {

	public static final int DEFAULT_WIDTH = 150;

	public static final MapCodec<DialogButtonData> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			TextCodecs.CODEC.fieldOf("label").forGetter(DialogButtonData::label),
			TextCodecs.CODEC.optionalFieldOf("tooltip").forGetter(DialogButtonData::tooltip),
			Dialog.WIDTH_CODEC.optionalFieldOf("width", DEFAULT_WIDTH).forGetter(DialogButtonData::width)
		).apply(instance, DialogButtonData::new)
	);

	public DialogButtonData(Text label, int width) {
		this(label, Optional.empty(), width);
	}
}
