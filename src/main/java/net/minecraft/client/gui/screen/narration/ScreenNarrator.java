package net.minecraft.client.gui.screen.narration;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Накапливает нарративные сообщения от элементов экрана и формирует итоговую строку
 * для синтезатора речи. Сообщения сортируются по {@link NarrationPart} и глубине вложенности.
 */
@Environment(EnvType.CLIENT)
public class ScreenNarrator {

	int currentMessageIndex;
	final Map<ScreenNarrator.PartIndex, ScreenNarrator.Message> narrations = Maps.newTreeMap(
		Comparator
			.<ScreenNarrator.PartIndex, NarrationPart>comparing(partIndex -> partIndex.part)
			.thenComparing(partIndex -> partIndex.depth)
	);

	public void buildNarrations(Consumer<NarrationMessageBuilder> builderConsumer) {
		currentMessageIndex++;
		builderConsumer.accept(new ScreenNarrator.MessageBuilder(0));
	}

	/**
	 * Собирает все актуальные нарративные сообщения в одну строку, разделяя их точкой с пробелом.
	 * Если {@code includeUnchanged} равен {@code false}, пропускает уже озвученные сообщения.
	 *
	 * @param includeUnchanged включать ли уже использованные сообщения
	 * @return итоговая строка для синтезатора речи
	 */
	public String buildNarratorText(boolean includeUnchanged) {
		StringBuilder builder = new StringBuilder();
		Consumer<String> consumer = new Consumer<>() {
			private boolean first = true;

			public void accept(String string) {
				if (first) {
					first = false;
				} else {
					builder.append(". ");
				}

				builder.append(string);
			}
		};

		narrations.forEach((partIndex, message) -> {
			if (message.index == currentMessageIndex && (includeUnchanged || !message.used)) {
				message.narration.forEachSentence(consumer);
				message.used = true;
			}
		});

		return builder.toString();
	}

	/**
	 * {@code Message} — хранит нарратив и его состояние (использован/не использован).
	 */
	@Environment(EnvType.CLIENT)
	static class Message {

		Narration<?> narration = Narration.EMPTY;
		int index = -1;
		boolean used;

		public ScreenNarrator.Message setNarration(int index, Narration<?> narration) {
			if (narration.equals(this.narration)) {
				if (this.index + 1 != index) {
					used = false;
				}
			} else {
				this.narration = narration;
				used = false;
			}

			this.index = index;
			return this;
		}
	}

	/**
	 * {@code MessageBuilder} — реализация {@link NarrationMessageBuilder} с поддержкой глубины вложенности.
	 */
	@Environment(EnvType.CLIENT)
	class MessageBuilder implements NarrationMessageBuilder {

		private final int depth;

		MessageBuilder(final int depth) {
			this.depth = depth;
		}

		@Override
		public void put(NarrationPart part, Narration<?> narration) {
			narrations
				.computeIfAbsent(
					new ScreenNarrator.PartIndex(part, depth),
					partIndex -> new ScreenNarrator.Message()
				)
				.setNarration(currentMessageIndex, narration);
		}

		@Override
		public NarrationMessageBuilder nextMessage() {
			return ScreenNarrator.this.new MessageBuilder(depth + 1);
		}
	}

	/**
	 * {@code PartIndex} — составной ключ для сортировки нарративов по типу и глубине.
	 */
	@Environment(EnvType.CLIENT)
	record PartIndex(NarrationPart part, int depth) {
	}
}
