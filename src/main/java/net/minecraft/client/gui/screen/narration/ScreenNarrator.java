package net.minecraft.client.gui.screen.narration;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code ScreenNarrator}.
 */
public class ScreenNarrator {

	int currentMessageIndex;
	final Map<ScreenNarrator.PartIndex, ScreenNarrator.Message> narrations = Maps.newTreeMap(
			Comparator
					.<ScreenNarrator.PartIndex, NarrationPart>comparing(partIndex -> partIndex.part)
					.thenComparing(partIndex -> partIndex.depth)
	);

	public void buildNarrations(Consumer<NarrationMessageBuilder> builderConsumer) {
		this.currentMessageIndex++;
		builderConsumer.accept(new ScreenNarrator.MessageBuilder(0));
	}

	public String buildNarratorText(boolean includeUnchanged) {
		final StringBuilder stringBuilder = new StringBuilder();
		Consumer<String> consumer = new Consumer<String>() {
			private boolean first = true;

			public void accept(String string) {
				if (!this.first) {
					stringBuilder.append(". ");
				}

				this.first = false;
				stringBuilder.append(string);
			}
		};
		this.narrations.forEach((partIndex, message) -> {
			if (message.index == this.currentMessageIndex && (includeUnchanged || !message.used)) {
				message.narration.forEachSentence(consumer);
				message.used = true;
			}
		});
		return stringBuilder.toString();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Message}.
	 */
	static class Message {

		Narration<?> narration = Narration.EMPTY;
		int index = -1;
		boolean used;

		public ScreenNarrator.Message setNarration(int index, Narration<?> narration) {
			if (!this.narration.equals(narration)) {
				this.narration = narration;
				this.used = false;
			}
			else if (this.index + 1 != index) {
				this.used = false;
			}

			this.index = index;
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code MessageBuilder}.
	 */
	class MessageBuilder implements NarrationMessageBuilder {

		private final int depth;

		MessageBuilder(final int depth) {
			this.depth = depth;
		}

		@Override
		public void put(NarrationPart part, Narration<?> narration) {
			ScreenNarrator.this.narrations
					.computeIfAbsent(
							new ScreenNarrator.PartIndex(part, this.depth),
							partIndex -> new ScreenNarrator.Message()
					)
					.setNarration(ScreenNarrator.this.currentMessageIndex, narration);
		}

		@Override
		public NarrationMessageBuilder nextMessage() {
			return ScreenNarrator.this.new MessageBuilder(this.depth + 1);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PartIndex}.
	 */
	record PartIndex(NarrationPart part, int depth) {
	}
}
