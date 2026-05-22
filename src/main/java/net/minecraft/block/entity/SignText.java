package net.minecraft.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Иммутабельный контейнер текста одной стороны таблички.
 * Хранит четыре строки в двух вариантах: оригинальном и отфильтрованном (для чат-фильтра).
 * Поддерживает цвет текста и эффект свечения.
 */
public class SignText {

	public static final int LINE_COUNT = 4;
	private static final Codec<Text[]> MESSAGES_CODEC = TextCodecs.CODEC
		.listOf()
		.comapFlatMap(
			messages -> Util.decodeFixedLengthList(messages, LINE_COUNT)
				.map(list -> new Text[]{
					(Text) list.get(0),
					(Text) list.get(1),
					(Text) list.get(2),
					(Text) list.get(3)
				}),
			messages -> List.of(messages[0], messages[1], messages[2], messages[3])
		);
	public static final Codec<SignText> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			MESSAGES_CODEC.fieldOf("messages").forGetter(signText -> signText.messages),
			MESSAGES_CODEC.lenientOptionalFieldOf("filtered_messages").forGetter(SignText::getFilteredMessages),
			DyeColor.CODEC.fieldOf("color").orElse(DyeColor.BLACK).forGetter(signText -> signText.color),
			Codec.BOOL.fieldOf("has_glowing_text").orElse(false).forGetter(signText -> signText.glowing)
		).apply(instance, SignText::create)
	);

	private final Text[] messages;
	private final Text[] filteredMessages;
	private final DyeColor color;
	private final boolean glowing;
	private OrderedText @Nullable [] orderedMessages;
	private boolean filtered;

	public SignText() {
		this(getDefaultText(), getDefaultText(), DyeColor.BLACK, false);
	}

	public SignText(Text[] messages, Text[] filteredMessages, DyeColor color, boolean glowing) {
		this.messages = messages;
		this.filteredMessages = filteredMessages;
		this.color = color;
		this.glowing = glowing;
	}

	private static Text[] getDefaultText() {
		return new Text[]{ScreenTexts.EMPTY, ScreenTexts.EMPTY, ScreenTexts.EMPTY, ScreenTexts.EMPTY};
	}

	private static SignText create(
		Text[] messages,
		Optional<Text[]> filteredMessages,
		DyeColor color,
		boolean glowing
	) {
		return new SignText(
			messages,
			filteredMessages.orElse(Arrays.copyOf(messages, messages.length)),
			color,
			glowing
		);
	}

	public boolean isGlowing() {
		return glowing;
	}

	public SignText withGlowing(boolean newGlowing) {
		return newGlowing == glowing ? this : new SignText(messages, filteredMessages, color, newGlowing);
	}

	public DyeColor getColor() {
		return color;
	}

	public SignText withColor(DyeColor newColor) {
		return newColor == color ? this : new SignText(messages, filteredMessages, newColor, glowing);
	}

	public Text getMessage(int line, boolean useFiltered) {
		return getMessages(useFiltered)[line];
	}

	public SignText withMessage(int line, Text message) {
		return withMessage(line, message, message);
	}

	public SignText withMessage(int line, Text message, Text filteredMessage) {
		Text[] newMessages = Arrays.copyOf(messages, messages.length);
		Text[] newFiltered = Arrays.copyOf(filteredMessages, filteredMessages.length);
		newMessages[line] = message;
		newFiltered[line] = filteredMessage;
		return new SignText(newMessages, newFiltered, color, glowing);
	}

	public boolean hasText(PlayerEntity player) {
		return Arrays.stream(getMessages(player.shouldFilterText())).anyMatch(text -> !text.getString().isEmpty());
	}

	public Text[] getMessages(boolean useFiltered) {
		return useFiltered ? filteredMessages : messages;
	}

	/**
	 * Возвращает кешированный массив {@link OrderedText} для рендеринга.
	 * Кеш инвалидируется при смене режима фильтрации или первом вызове.
	 */
	public OrderedText[] getOrderedMessages(boolean useFiltered, Function<Text, OrderedText> messageOrderer) {
		if (orderedMessages == null || filtered != useFiltered) {
			filtered = useFiltered;
			orderedMessages = new OrderedText[LINE_COUNT];

			for (int i = 0; i < LINE_COUNT; i++) {
				orderedMessages[i] = messageOrderer.apply(getMessage(i, useFiltered));
			}
		}

		return orderedMessages;
	}

	private Optional<Text[]> getFilteredMessages() {
		for (int i = 0; i < LINE_COUNT; i++) {
			if (!filteredMessages[i].equals(messages[i])) {
				return Optional.of(filteredMessages);
			}
		}

		return Optional.empty();
	}

	public boolean hasRunCommandClickEvent(PlayerEntity player) {
		for (Text text : getMessages(player.shouldFilterText())) {
			ClickEvent clickEvent = text.getStyle().getClickEvent();
			if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
				return true;
			}
		}

		return false;
	}
}
