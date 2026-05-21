package net.minecraft.dialog.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.action.ParsedTemplate;
import net.minecraft.dialog.input.InputControl;

/**
 * {@code DialogInput}.
 */
public record DialogInput(String key, InputControl control) {

	public static final Codec<DialogInput> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(
							ParsedTemplate.NAME_CODEC.fieldOf("key").forGetter(DialogInput::key),
							InputControl.CODEC.forGetter(DialogInput::control)
					)
					.apply(instance, DialogInput::new)
	);
}
