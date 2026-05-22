package net.minecraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Записывает результат сессии быстрого старта в JSON-файл.
 * Используется лаунчером для отображения статистики последней игровой сессии.
 * Экземпляр {@link #NOOP} используется, когда логирование отключено.
 */
@Environment(EnvType.CLIENT)
public class QuickPlayLogger {

	private static final QuickPlayLogger NOOP = new QuickPlayLogger("") {
		@Override
		public void save(MinecraftClient client) {
		}

		@Override
		public void setWorld(WorldType worldType, String id, String name) {
		}
	};

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().create();

	private final Path path;
	private @Nullable QuickPlayWorld world;

	QuickPlayLogger(String relativePath) {
		path = MinecraftClient.getInstance().runDirectory.toPath().resolve(relativePath);
	}

	public static QuickPlayLogger create(@Nullable String relativePath) {
		return relativePath == null ? NOOP : new QuickPlayLogger(relativePath);
	}

	public void setWorld(WorldType worldType, String id, String name) {
		world = new QuickPlayWorld(worldType, id, name);
	}

	/**
	 * Асинхронно сохраняет лог сессии в JSON-файл.
	 * Предварительно удаляет старый файл, затем записывает новый.
	 * Логирует ошибку, если данные о мире или режиме игры недоступны.
	 *
	 * @param client экземпляр клиента для получения текущего режима игры
	 */
	public void save(MinecraftClient client) {
		if (client.interactionManager == null || world == null) {
			LOGGER.error("Failed to log session for quickplay. Missing world data or gamemode");
			return;
		}

		Util.getIoWorkerExecutor().execute(() -> {
			try {
				Files.deleteIfExists(path);
			} catch (IOException exception) {
				LOGGER.error("Failed to delete quickplay log file {}", path, exception);
			}

			Log log = new Log(world, Instant.now(), client.interactionManager.getCurrentGameMode());
			Codec.list(Log.CODEC)
				.encodeStart(JsonOps.INSTANCE, List.of(log))
				.resultOrPartial(Util.addPrefix("Quick Play: ", LOGGER::error))
				.ifPresent(json -> {
					try {
						Files.createDirectories(path.getParent());
						Files.writeString(path, GSON.toJson(json));
					} catch (IOException exception) {
						LOGGER.error("Failed to write to quickplay log file {}", path, exception);
					}
				});
		});
	}

	@Environment(EnvType.CLIENT)
	record Log(QuickPlayWorld quickPlayWorld, Instant lastPlayedTime, GameMode gameMode) {

		static final Codec<Log> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				QuickPlayWorld.CODEC.forGetter(Log::quickPlayWorld),
				Codecs.INSTANT.fieldOf("lastPlayedTime").forGetter(Log::lastPlayedTime),
				GameMode.CODEC.fieldOf("gamemode").forGetter(Log::gameMode)
			).apply(instance, Log::new)
		);
	}

	@Environment(EnvType.CLIENT)
	record QuickPlayWorld(WorldType type, String id, String name) {

		static final MapCodec<QuickPlayWorld> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				WorldType.CODEC.fieldOf("type").forGetter(QuickPlayWorld::type),
				Codecs.ESCAPED_STRING.fieldOf("id").forGetter(QuickPlayWorld::id),
				Codec.STRING.fieldOf("name").forGetter(QuickPlayWorld::name)
			).apply(instance, QuickPlayWorld::new)
		);
	}

	/**
	 * Тип мира для лога быстрого старта.
	 */
	@Environment(EnvType.CLIENT)
	public enum WorldType implements StringIdentifiable {
		SINGLEPLAYER("singleplayer"),
		MULTIPLAYER("multiplayer"),
		REALMS("realms");

		static final Codec<WorldType> CODEC = StringIdentifiable.createCodec(WorldType::values);

		private final String id;

		WorldType(String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return id;
		}
	}
}
