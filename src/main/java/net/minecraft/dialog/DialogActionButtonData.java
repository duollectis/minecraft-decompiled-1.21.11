package net.minecraft.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.action.DialogAction;

import java.util.Optional;

/**
 * Данные кнопки диалога, объединяющие визуальное описание и опциональное действие.
 *
 * @param data   визуальные данные кнопки (текст, подсказка, ширина)
 * @param action опциональное действие, выполняемое при нажатии кнопки
 */
public record DialogActionButtonData(DialogButtonData data, Optional<DialogAction> action) {

	public static final Codec<DialogActionButtonData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			DialogButtonData.CODEC.forGetter(DialogActionButtonData::data),
			DialogAction.CODEC.optionalFieldOf("action").forGetter(DialogActionButtonData::action)
		).apply(instance, DialogActionButtonData::new)
	);
}
