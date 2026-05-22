package net.minecraft.client.session.report.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Кольцевой буфер записей чата с ограниченным размером.
 * Хранит последние {@code maxSize} сообщений, автоматически вытесняя старые
 * при переполнении. Индексы записей монотонно возрастают и не сбрасываются.
 */
@Environment(EnvType.CLIENT)
public class ChatLog {

	private final ChatLogEntry[] entries;
	private int currentIndex;

	/**
	 * Создаёт codec для сериализации лога чата с проверкой максимального размера.
	 * При декодировании отклоняет списки, превышающие {@code maxSize} записей.
	 *
	 * @param maxSize максимально допустимое количество записей в буфере
	 * @return codec для {@link ChatLog}
	 */
	public static Codec<ChatLog> createCodec(int maxSize) {
		return Codec.list(ChatLogEntry.CODEC)
			.comapFlatMap(
				entries -> {
					int size = entries.size();
					return size > maxSize
						? DataResult.error(() -> "Expected: a buffer of size less than or equal to "
							+ maxSize + " but: " + size + " is greater than " + maxSize)
						: DataResult.success(new ChatLog(maxSize, entries));
				},
				ChatLog::toList
			);
	}

	public ChatLog(int maxSize) {
		this.entries = new ChatLogEntry[maxSize];
	}

	private ChatLog(int size, List<ChatLogEntry> entries) {
		this.entries = entries.toArray(ChatLogEntry[]::new);
		this.currentIndex = entries.size();
	}

	private List<ChatLogEntry> toList() {
		List<ChatLogEntry> list = new ArrayList<>(size());

		for (int i = getMinIndex(); i <= getMaxIndex(); i++) {
			list.add(get(i));
		}

		return list;
	}

	public void add(ChatLogEntry entry) {
		entries[wrapIndex(currentIndex++)] = entry;
	}

	/**
	 * Возвращает запись по абсолютному индексу или {@code null}, если индекс
	 * выходит за пределы текущего окна буфера.
	 *
	 * @param index абсолютный индекс записи
	 * @return запись лога или {@code null}
	 */
	public @Nullable ChatLogEntry get(int index) {
		return index >= getMinIndex() && index <= getMaxIndex()
			? entries[wrapIndex(index)]
			: null;
	}

	private int wrapIndex(int index) {
		return index % entries.length;
	}

	public int getMinIndex() {
		return Math.max(currentIndex - entries.length, 0);
	}

	public int getMaxIndex() {
		return currentIndex - 1;
	}

	private int size() {
		return getMaxIndex() - getMinIndex() + 1;
	}
}
