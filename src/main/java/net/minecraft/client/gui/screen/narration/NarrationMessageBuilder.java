package net.minecraft.client.gui.screen.narration;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

/**
 * Интерфейс построителя нарративных сообщений для экрана.
 * Позволяет добавлять части нарратива по типу {@link NarrationPart} и создавать вложенные сообщения.
 */
@Environment(EnvType.CLIENT)
public interface NarrationMessageBuilder {

	default void put(NarrationPart part, Text text) {
		put(part, Narration.string(text.getString()));
	}

	default void put(NarrationPart part, String string) {
		put(part, Narration.string(string));
	}

	default void put(NarrationPart part, Text... texts) {
		put(part, Narration.texts(ImmutableList.copyOf(texts)));
	}

	void put(NarrationPart part, Narration<?> narration);

	NarrationMessageBuilder nextMessage();
}
