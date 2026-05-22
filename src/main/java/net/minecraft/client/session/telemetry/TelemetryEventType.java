package net.minecraft.client.session.telemetry;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Тип события телеметрии. Описывает структуру события: набор свойств,
 * ключ экспорта и признак опциональности (требует согласия пользователя).
 * Все зарегистрированные типы хранятся в {@link #TYPES} и доступны через {@link #CODEC}.
 */
@Environment(EnvType.CLIENT)
public class TelemetryEventType {

	static final Map<String, TelemetryEventType> TYPES = new Object2ObjectLinkedOpenHashMap<>();

	public static final Codec<TelemetryEventType> CODEC = Codec.STRING.comapFlatMap(
		id -> {
			TelemetryEventType type = TYPES.get(id);
			return type != null
				? DataResult.success(type)
				: DataResult.error(() -> "No TelemetryEventType with key: '" + id + "'");
		},
		TelemetryEventType::getId
	);

	private static final List<TelemetryEventProperty<?>> BASIC_PROPERTIES = List.of(
		TelemetryEventProperty.USER_ID,
		TelemetryEventProperty.CLIENT_ID,
		TelemetryEventProperty.MINECRAFT_SESSION_ID,
		TelemetryEventProperty.GAME_VERSION,
		TelemetryEventProperty.OPERATING_SYSTEM,
		TelemetryEventProperty.PLATFORM,
		TelemetryEventProperty.CLIENT_MODDED,
		TelemetryEventProperty.LAUNCHER_NAME,
		TelemetryEventProperty.EVENT_TIMESTAMP_UTC,
		TelemetryEventProperty.OPT_IN
	);

	private static final List<TelemetryEventProperty<?>> REQUIRED_PROPERTIES = Stream.concat(
		BASIC_PROPERTIES.stream(),
		Stream.of(
			TelemetryEventProperty.WORLD_SESSION_ID,
			TelemetryEventProperty.SERVER_MODDED,
			TelemetryEventProperty.SERVER_TYPE
		)
	).toList();

	public static final TelemetryEventType WORLD_LOADED = builder("world_loaded", "WorldLoaded")
		.properties(REQUIRED_PROPERTIES)
		.properties(TelemetryEventProperty.GAME_MODE)
		.properties(TelemetryEventProperty.REALMS_MAP_CONTENT)
		.build();

	public static final TelemetryEventType PERFORMANCE_METRICS = builder("performance_metrics", "PerformanceMetrics")
		.properties(REQUIRED_PROPERTIES)
		.properties(TelemetryEventProperty.FRAME_RATE_SAMPLES)
		.properties(TelemetryEventProperty.RENDER_TIME_SAMPLES)
		.properties(TelemetryEventProperty.USED_MEMORY_SAMPLES)
		.properties(TelemetryEventProperty.NUMBER_OF_SAMPLES)
		.properties(TelemetryEventProperty.RENDER_DISTANCE)
		.properties(TelemetryEventProperty.DEDICATED_MEMORY_KB)
		.optional()
		.build();

	public static final TelemetryEventType WORLD_LOAD_TIMES = builder("world_load_times", "WorldLoadTimes")
		.properties(REQUIRED_PROPERTIES)
		.properties(TelemetryEventProperty.WORLD_LOAD_TIME_MS)
		.properties(TelemetryEventProperty.NEW_WORLD)
		.optional()
		.build();

	public static final TelemetryEventType WORLD_UNLOADED = builder("world_unloaded", "WorldUnloaded")
		.properties(REQUIRED_PROPERTIES)
		.properties(TelemetryEventProperty.SECONDS_SINCE_LOAD)
		.properties(TelemetryEventProperty.TICKS_SINCE_LOAD)
		.build();

	public static final TelemetryEventType ADVANCEMENT_MADE = builder("advancement_made", "AdvancementMade")
		.properties(REQUIRED_PROPERTIES)
		.properties(TelemetryEventProperty.ADVANCEMENT_ID)
		.properties(TelemetryEventProperty.ADVANCEMENT_GAME_TIME)
		.optional()
		.build();

	public static final TelemetryEventType GAME_LOAD_TIMES = builder("game_load_times", "GameLoadTimes")
		.properties(BASIC_PROPERTIES)
		.properties(TelemetryEventProperty.LOAD_TIME_TOTAL_TIME_MS)
		.properties(TelemetryEventProperty.LOAD_TIME_PRE_WINDOW_MS)
		.properties(TelemetryEventProperty.LOAD_TIME_BOOTSTRAP_MS)
		.properties(TelemetryEventProperty.LOAD_TIME_LOADING_OVERLAY_MS)
		.optional()
		.build();

	private final String id;
	private final String exportKey;
	private final List<TelemetryEventProperty<?>> properties;
	private final boolean optional;
	private final MapCodec<SentTelemetryEvent> codec;

	TelemetryEventType(String id, String exportKey, List<TelemetryEventProperty<?>> properties, boolean optional) {
		this.id = id;
		this.exportKey = exportKey;
		this.properties = properties;
		this.optional = optional;
		this.codec = PropertyMap
			.createCodec(properties)
			.xmap(map -> new SentTelemetryEvent(this, map), SentTelemetryEvent::properties);
	}

	public static TelemetryEventType.Builder builder(String id, String sentEventId) {
		return new TelemetryEventType.Builder(id, sentEventId);
	}

	public String getId() {
		return id;
	}

	public List<TelemetryEventProperty<?>> getProperties() {
		return properties;
	}

	public MapCodec<SentTelemetryEvent> getCodec() {
		return codec;
	}

	public boolean isOptional() {
		return optional;
	}

	/**
	 * Создаёт и заполняет {@link TelemetryEvent} для отправки через {@link TelemetrySession}.
	 * Перебирает все зарегистрированные свойства типа и записывает их значения
	 * из {@code properties} в событие.
	 *
	 * @param session    активная сессия телеметрии
	 * @param properties карта значений свойств для этого события
	 * @return заполненное событие, готовое к отправке
	 */
	public TelemetryEvent createEvent(TelemetrySession session, PropertyMap properties) {
		TelemetryEvent event = session.createNewEvent(exportKey);

		for (TelemetryEventProperty<?> property : this.properties) {
			property.addTo(properties, event);
		}

		return event;
	}

	public <T> boolean hasProperty(TelemetryEventProperty<T> property) {
		return properties.contains(property);
	}

	@Override
	public String toString() {
		return "TelemetryEventType[" + id + "]";
	}

	public MutableText getTitle() {
		return getText("title");
	}

	public MutableText getDescription() {
		return getText("description");
	}

	private MutableText getText(String key) {
		return Text.translatable("telemetry.event." + id + "." + key);
	}

	public static List<TelemetryEventType> getTypes() {
		return List.copyOf(TYPES.values());
	}

	/**
	 * Строитель для регистрации нового типа события телеметрии.
	 * После вызова {@link #build()} тип регистрируется в глобальном реестре {@link #TYPES}.
	 * Повторная регистрация с тем же {@code id} выбрасывает {@link IllegalStateException}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final String id;
		private final String exportKey;
		private final List<TelemetryEventProperty<?>> properties = new ArrayList<>();
		private boolean optional;

		Builder(String id, String exportKey) {
			this.id = id;
			this.exportKey = exportKey;
		}

		public TelemetryEventType.Builder properties(List<TelemetryEventProperty<?>> properties) {
			this.properties.addAll(properties);
			return this;
		}

		public <T> TelemetryEventType.Builder properties(TelemetryEventProperty<T> property) {
			properties.add(property);
			return this;
		}

		public TelemetryEventType.Builder optional() {
			optional = true;
			return this;
		}

		public TelemetryEventType build() {
			TelemetryEventType type = new TelemetryEventType(id, exportKey, List.copyOf(properties), optional);

			if (TelemetryEventType.TYPES.putIfAbsent(id, type) != null) {
				throw new IllegalStateException("Duplicate TelemetryEventType with key: '" + id + "'");
			}

			return type;
		}
	}
}
