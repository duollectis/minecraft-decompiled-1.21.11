package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.message.MessageType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.List;
import java.util.function.IntFunction;

/**
 * {@code Decoration}.
 */
public record Decoration(String translationKey, List<Decoration.Parameter> parameters, Style style) {

	public static final Codec<Decoration> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec.STRING.fieldOf("translation_key").forGetter(Decoration::translationKey),
					                    Decoration.Parameter.CODEC.listOf().fieldOf("parameters").forGetter(Decoration::parameters),
					                    Style.Codecs.CODEC.optionalFieldOf("style", Style.EMPTY).forGetter(Decoration::style)
			                    )
			                    .apply(instance, Decoration::new)
	);
	public static final PacketCodec<RegistryByteBuf, Decoration> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.STRING,
			Decoration::translationKey,
			Decoration.Parameter.PACKET_CODEC.collect(PacketCodecs.toList()),
			Decoration::parameters,
			Style.Codecs.PACKET_CODEC,
			Decoration::style,
			Decoration::new
	);

	/**
	 * Of chat.
	 *
	 * @param translationKey translation key
	 *
	 * @return Decoration — результат операции
	 */
	public static Decoration ofChat(String translationKey) {
		return new Decoration(
				translationKey,
				List.of(Decoration.Parameter.SENDER, Decoration.Parameter.CONTENT),
				Style.EMPTY
		);
	}

	/**
	 * Of incoming message.
	 *
	 * @param translationKey translation key
	 *
	 * @return Decoration — результат операции
	 */
	public static Decoration ofIncomingMessage(String translationKey) {
		Style style = Style.EMPTY.withColor(Formatting.GRAY).withItalic(true);
		return new Decoration(
				translationKey,
				List.of(Decoration.Parameter.SENDER, Decoration.Parameter.CONTENT),
				style
		);
	}

	/**
	 * Of outgoing message.
	 *
	 * @param translationKey translation key
	 *
	 * @return Decoration — результат операции
	 */
	public static Decoration ofOutgoingMessage(String translationKey) {
		Style style = Style.EMPTY.withColor(Formatting.GRAY).withItalic(true);
		return new Decoration(
				translationKey,
				List.of(Decoration.Parameter.TARGET, Decoration.Parameter.CONTENT),
				style
		);
	}

	/**
	 * Of team message.
	 *
	 * @param translationKey translation key
	 *
	 * @return Decoration — результат операции
	 */
	public static Decoration ofTeamMessage(String translationKey) {
		return new Decoration(
				translationKey,
				List.of(Decoration.Parameter.TARGET, Decoration.Parameter.SENDER, Decoration.Parameter.CONTENT),
				Style.EMPTY
		);
	}

	/**
	 * Apply.
	 *
	 * @param content content
	 * @param params params
	 *
	 * @return Text — результат операции
	 */
	public Text apply(Text content, MessageType.Parameters params) {
		Object[] objects = this.collectArguments(content, params);
		return Text.translatable(this.translationKey, objects).fillStyle(this.style);
	}

	private Text[] collectArguments(Text content, MessageType.Parameters params) {
		Text[] texts = new Text[this.parameters.size()];

		for (int i = 0; i < texts.length; i++) {
			Decoration.Parameter parameter = this.parameters.get(i);
			texts[i] = parameter.apply(content, params);
		}

		return texts;
	}

	/**
	 * {@code Parameter}.
	 */
	public static enum Parameter implements StringIdentifiable {
		SENDER(0, "sender", (content, params) -> params.name()),
		TARGET(1, "target", (content, params) -> params.targetName().orElse(ScreenTexts.EMPTY)),
		CONTENT(2, "content", (content, params) -> content);

		private static final IntFunction<Decoration.Parameter> BY_ID = ValueLists.createIndexToValueFunction(
				(Decoration.Parameter parameter) -> parameter.id, values(), ValueLists.OutOfBoundsHandling.ZERO
		);
		public static final Codec<Decoration.Parameter>
				CODEC =
				StringIdentifiable.createCodec(Decoration.Parameter::values);
		public static final PacketCodec<ByteBuf, Decoration.Parameter>
				PACKET_CODEC =
				PacketCodecs.indexed(BY_ID, parameter -> parameter.id);
		private final int id;
		private final String name;
		private final Decoration.Parameter.Selector selector;

		private Parameter(final int id, final String name, final Decoration.Parameter.Selector selector) {
			this.id = id;
			this.name = name;
			this.selector = selector;
		}

		/**
		 * Apply.
		 *
		 * @param content content
		 * @param params params
		 *
		 * @return Text — результат операции
		 */
		public Text apply(Text content, MessageType.Parameters params) {
			return this.selector.select(content, params);
		}

		@Override
		public String asString() {
			return this.name;
		}

		/**
		 * {@code Selector}.
		 */
		public interface Selector {

			Text select(Text content, MessageType.Parameters params);
		}
	}
}
