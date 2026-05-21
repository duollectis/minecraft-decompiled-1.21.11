package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

/**
 * {@code BooleanInputControl}.
 */
public record BooleanInputControl(Text label, boolean initial, String onTrue, String onFalse) implements InputControl {

	public static final MapCodec<BooleanInputControl> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    TextCodecs.CODEC.fieldOf("label").forGetter(BooleanInputControl::label),
					                    Codec.BOOL.optionalFieldOf("initial", false).forGetter(BooleanInputControl::initial),
					                    Codec.STRING.optionalFieldOf("on_true", "true").forGetter(BooleanInputControl::onTrue),
					                    Codec.STRING.optionalFieldOf("on_false", "false").forGetter(BooleanInputControl::onFalse)
			                    )
			                    .apply(instance, BooleanInputControl::new)
	);

	@Override
	public MapCodec<BooleanInputControl> getCodec() {
		return CODEC;
	}
}
