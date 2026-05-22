package net.minecraft.util.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

/**
 * Последовательный читатель лог-файла, десериализующий записи через {@link Codec}.
 * Каждый вызов {@link #read()} возвращает следующую запись или {@code null} при достижении конца.
 */
public interface LogReader<T> extends Closeable {

	/**
	 * Создаёт читатель, парсящий JSON-строки из переданного {@link Reader}
	 * и декодирующий их через указанный кодек.
	 *
	 * @param codec  кодек для десериализации записей
	 * @param reader источник данных
	 * @return читатель лог-записей
	 */
	static <T> LogReader<T> create(Codec<T> codec, Reader reader) {
		JsonReader jsonReader = new JsonReader(reader);
		jsonReader.setStrictness(Strictness.LENIENT);

		return new LogReader<>() {
			@Override
			public @Nullable T read() throws IOException {
				try {
					if (!jsonReader.hasNext()) {
						return null;
					}

					JsonElement element = JsonParser.parseReader(jsonReader);
					return codec.parse(JsonOps.INSTANCE, element).getOrThrow(IOException::new);
				} catch (JsonParseException e) {
					throw new IOException(e);
				} catch (EOFException e) {
					return null;
				}
			}

			@Override
			public void close() throws IOException {
				jsonReader.close();
			}
		};
	}

	@Nullable T read() throws IOException;
}
