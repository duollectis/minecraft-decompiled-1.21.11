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
 * Декорация сообщения чата — описывает, как оборачивать сообщение в переводимый текст
 * с заданными параметрами (отправитель, получатель, содержимое) и стилем.
 * Используется в {@link MessageType} для форматирования входящих/исходящих сообщений.
 */
public record Decoration(String translationKey, List<Parameter> parameters, Style style) {

	public static final Codec<Decoration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.STRING.fieldOf("translation_key").forGetter(Decoration::translationKey),
			Parameter.CODEC.listOf().fieldOf("parameters").forGetter(Decoration::parameters),
			Style.Codecs.CODEC.optionalFieldOf("style", Style.EMPTY).forGetter(Decoration::style)
		).apply(instance, Decoration::new)
	);

	public static final PacketCodec<RegistryByteBuf, Decoration> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING,
		Decoration::translationKey,
		Parameter.PACKET_CODEC.collect(PacketCodecs.toList()),
		Decoration::parameters,
		Style.Codecs.PACKET_CODEC,
		Decoration::style,
		Decoration::new
	);

	/**
	 * Создаёт декорацию для обычного сообщения чата (отправитель + содержимое, без стиля).
	 */
	public static Decoration ofChat(String translationKey) {
		return new Decoration(
			translationKey,
			List.of(Parameter.SENDER, Parameter.CONTENT),
			Style.EMPTY
		);
	}

	/**
	 * Создаёт декорацию для входящего личного сообщения (серый курсив, отправитель + содержимое).
	 */
	public static Decoration ofIncomingMessage(String translationKey) {
		Style style = Style.EMPTY.withColor(Formatting.GRAY).withItalic(true);
		return new Decoration(
			translationKey,
			List.of(Parameter.SENDER, Parameter.CONTENT),
			style
		);
	}

	/**
	 * Создаёт декорацию для исходящего личного сообщения (серый курсив, получатель + содержимое).
	 */
	public static Decoration ofOutgoingMessage(String translationKey) {
		Style style = Style.EMPTY.withColor(Formatting.GRAY).withItalic(true);
		return new Decoration(
			translationKey,
			List.of(Parameter.TARGET, Parameter.CONTENT),
			style
		);
	}

	/**
	 * Создаёт декорацию для командного чата (получатель + отправитель + содержимое, без стиля).
	 */
	public static Decoration ofTeamMessage(String translationKey) {
		return new Decoration(
			translationKey,
			List.of(Parameter.TARGET, Parameter.SENDER, Parameter.CONTENT),
			Style.EMPTY
		);
	}

	/**
	 * Применяет декорацию к содержимому сообщения, подставляя аргументы из {@code params}
	 * согласно списку {@link Parameter} и оборачивая результат в переводимый текст со стилем.
	 */
	public Text apply(Text content, MessageType.Parameters params) {
		Object[] arguments = collectArguments(content, params);
		return Text.translatable(translationKey, arguments).fillStyle(style);
	}

	private Text[] collectArguments(Text content, MessageType.Parameters params) {
		Text[] texts = new Text[parameters.size()];

		for (int index = 0; index < texts.length; index++) {
			texts[index] = parameters.get(index).apply(content, params);
		}

		return texts;
	}

	/**
	 * Параметр декорации — определяет, какую часть сообщения подставить на данную позицию.
	 */
	enum Parameter implements StringIdentifiable {
		SENDER(0, "sender", (content, params) -> params.name()),
		TARGET(1, "target", (content, params) -> params.targetName().orElse(ScreenTexts.EMPTY)),
		CONTENT(2, "content", (content, params) -> content);

		private static final IntFunction<Parameter> BY_ID = ValueLists.createIndexToValueFunction(
			(Parameter parameter) -> parameter.id, values(), ValueLists.OutOfBoundsHandling.ZERO
		);

		public static final Codec<Parameter> CODEC = StringIdentifiable.createCodec(Parameter::values);
		public static final PacketCodec<ByteBuf, Parameter> PACKET_CODEC =
			PacketCodecs.indexed(BY_ID, parameter -> parameter.id);

		private final int id;
		private final String name;
		private final Selector selector;

		Parameter(int id, String name, Selector selector) {
			this.id = id;
			this.name = name;
			this.selector = selector;
		}

		public Text apply(Text content, MessageType.Parameters params) {
			return selector.select(content, params);
		}

		@Override
		public String asString() {
			return name;
		}

		/**
		 * Стратегия выбора текстового значения из параметров сообщения.
		 */
		interface Selector {

			Text select(Text content, MessageType.Parameters params);
		}
	}
}
