package net.minecraft.server.dedicated.management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * {@code RpcKickReason}.
 */
public record RpcKickReason(
		Optional<String> literal,
		Optional<String> translatable,
		Optional<List<String>> translatableParams
) {

	public static final Codec<RpcKickReason> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec.STRING.optionalFieldOf("literal").forGetter(RpcKickReason::literal),
					                    Codec.STRING.optionalFieldOf("translatable").forGetter(RpcKickReason::translatable),
					                    Codec.STRING
							                    .listOf()
							                    .lenientOptionalFieldOf("translatableParams")
							                    .forGetter(RpcKickReason::translatableParams)
			                    )
			                    .apply(instance, RpcKickReason::new)
	);

	public Optional<Text> toText() {
		if (this.translatable.isPresent()) {
			String string = this.translatable.get();
			if (this.translatableParams.isPresent()) {
				List<String> list = this.translatableParams.get();
				return Optional.of(Text.translatable(string, list.toArray()));
			}
			else {
				return Optional.of(Text.translatable(string));
			}
		}
		else {
			return this.literal.map(Text::literal);
		}
	}
}
