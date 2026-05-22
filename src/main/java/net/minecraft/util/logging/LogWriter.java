package net.minecraft.util.logging;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Потокобезопасный писатель лог-файла, сериализующий записи через {@link Codec} в JSON.
 * Поддерживает подсчёт ссылок: файловый канал закрывается только когда все
 * читатели и сам писатель освобождены.
 */
public class LogWriter<T> implements Closeable {

	private static final Gson GSON = new Gson();
	private static final int NEWLINE = 10;

	private final Codec<T> codec;
	final FileChannel channel;
	private final AtomicInteger refCount = new AtomicInteger(1);

	public LogWriter(Codec<T> codec, FileChannel channel) {
		this.codec = codec;
		this.channel = channel;
	}

	public static <T> LogWriter<T> create(Codec<T> codec, Path path) throws IOException {
		FileChannel fileChannel = FileChannel.open(
				path,
				StandardOpenOption.WRITE,
				StandardOpenOption.READ,
				StandardOpenOption.CREATE
		);
		return new LogWriter<>(codec, fileChannel);
	}

	public void write(T object) throws IOException {
		JsonElement json = (JsonElement) codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow(IOException::new);
		channel.position(channel.size());
		Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8);
		GSON.toJson(json, GSON.newJsonWriter(writer));
		writer.write(NEWLINE);
		writer.flush();
	}

	/**
	 * Создаёт читатель, разделяющий файловый канал с этим писателем.
	 * Канал закроется только после освобождения всех читателей и самого писателя.
	 *
	 * @return читатель, позиция которого независима от позиции писателя
	 * @throws IOException если писатель уже закрыт
	 */
	public LogReader<T> getReader() throws IOException {
		if (refCount.get() <= 0) {
			throw new IOException("Event log has already been closed");
		}

		refCount.incrementAndGet();
		LogReader<T> delegate = LogReader.create(codec, Channels.newReader(channel, StandardCharsets.UTF_8));

		return new LogReader<>() {
			private volatile long position;

			@Override
			public @Nullable T read() throws IOException {
				T result;
				try {
					channel.position(position);
					result = delegate.read();
				} finally {
					position = channel.position();
				}
				return result;
			}

			@Override
			public void close() throws IOException {
				closeIfNotReferenced();
			}
		};
	}

	@Override
	public void close() throws IOException {
		closeIfNotReferenced();
	}

	void closeIfNotReferenced() throws IOException {
		if (refCount.decrementAndGet() <= 0) {
			channel.close();
		}
	}
}
