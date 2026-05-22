package net.minecraft.client.sound;

import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Управляет набором активных OpenAL-источников звука ({@link Source}).
 * Все операции с источниками выполняются через {@link #executor} звукового потока.
 * Создание источников асинхронно — результат возвращается через {@link CompletableFuture}.
 */
@Environment(EnvType.CLIENT)
public class Channel {

	private final Set<Channel.SourceManager> sources = Sets.newIdentityHashSet();
	final SoundEngine soundEngine;
	final Executor executor;

	public Channel(SoundEngine soundEngine, Executor executor) {
		this.soundEngine = soundEngine;
		this.executor = executor;
	}

	/**
	 * Асинхронно создаёт новый {@link Source} в звуковом потоке.
	 * Возвращает {@code null} в {@link CompletableFuture}, если пул источников исчерпан.
	 *
	 * @param mode режим воспроизведения (статический или потоковый)
	 * @return future с менеджером источника или {@code null}
	 */
	public CompletableFuture<Channel.SourceManager> createSource(SoundEngine.RunMode mode) {
		CompletableFuture<Channel.SourceManager> future = new CompletableFuture<>();
		executor.execute(() -> {
			Source source = soundEngine.createSource(mode);
			if (source != null) {
				Channel.SourceManager manager = new Channel.SourceManager(source);
				sources.add(manager);
				future.complete(manager);
			} else {
				future.complete(null);
			}
		});
		return future;
	}

	public void execute(Consumer<Stream<Source>> sourcesConsumer) {
		executor.execute(() -> sourcesConsumer.accept(
			sources.stream()
				.map(manager -> manager.source)
				.filter(Objects::nonNull)
		));
	}

	public void tick() {
		executor.execute(() -> {
			var iterator = sources.iterator();
			while (iterator.hasNext()) {
				Channel.SourceManager manager = iterator.next();
				manager.source.tick();
				if (manager.source.isStopped()) {
					manager.close();
					iterator.remove();
				}
			}
		});
	}

	public void close() {
		sources.forEach(Channel.SourceManager::close);
		sources.clear();
	}

	/**
	 * Обёртка над {@link Source}, обеспечивающая безопасное выполнение операций
	 * в звуковом потоке и корректное освобождение ресурсов при остановке.
	 */
	@Environment(EnvType.CLIENT)
	public class SourceManager {

		@Nullable Source source;
		private boolean stopped;

		public boolean isStopped() {
			return stopped;
		}

		public SourceManager(Source source) {
			this.source = source;
		}

		public void run(Consumer<Source> action) {
			Channel.this.executor.execute(() -> {
				if (source != null) {
					action.accept(source);
				}
			});
		}

		public void close() {
			stopped = true;
			Channel.this.soundEngine.release(source);
			source = null;
		}
	}
}
