package net.minecraft.client.gui.screen.narration;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Иммутабельный контейнер нарративного сообщения, хранящий значение и функцию его преобразования
 * в строки для синтезатора речи. Поддерживает три источника: строка, {@link Text} и список текстов.
 */
@Environment(EnvType.CLIENT)
public class Narration<T> {

	public static final Narration<?> EMPTY = new Narration<>(Unit.INSTANCE, (consumer, text) -> {});

	private final T value;
	private final BiConsumer<Consumer<String>, T> transformer;

	private Narration(T value, BiConsumer<Consumer<String>, T> transformer) {
		this.value = value;
		this.transformer = transformer;
	}

	public static Narration<?> string(String string) {
		return new Narration<>(string, Consumer::accept);
	}

	public static Narration<?> text(Text text) {
		return new Narration<>(text, (consumer, t) -> consumer.accept(t.getString()));
	}

	public static Narration<?> texts(List<Text> texts) {
		return new Narration<>(texts, (consumer, list) -> list.stream().map(Text::getString).forEach(consumer));
	}

	public void forEachSentence(Consumer<String> consumer) {
		transformer.accept(consumer, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof Narration<?> narration) {
			return narration.transformer == transformer && narration.value.equals(value);
		}

		return false;
	}

	@Override
	public int hashCode() {
		int valueHash = value.hashCode();
		return 31 * valueHash + transformer.hashCode();
	}
}
