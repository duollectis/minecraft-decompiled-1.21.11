package net.minecraft.stat;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.path.PathUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Серверная реализация обработчика статистики игрока с поддержкой
 * персистентности (JSON-файл) и синхронизации с клиентом.
 *
 * <p>При создании экземпляра автоматически загружает статистику из файла,
 * применяя DataFixer для миграции устаревших форматов. Изменённые статистики
 * накапливаются в {@code pendingStats} и отправляются клиенту пакетом
 * {@link StatisticsS2CPacket} при вызове {@link #sendStats}.
 *
 * <p>Формат файла — JSON с полями {@code "stats"} (карта статистик) и
 * {@code "DataVersion"} (версия данных для DataFixer).
 */
public class ServerStatHandler extends StatHandler {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Кодек для сериализации карты {@code Stat → Integer}.
	 *
	 * <p>Сначала группирует статистики по их {@link StatType} (dispatch по типу),
	 * затем внутри каждой группы кодирует пары {@code Stat → int}.
	 * При декодировании объединяет все группы обратно в плоскую карту.
	 */
	private static final Codec<Map<Stat<?>, Integer>> CODEC =
		Codec.dispatchedMap(
			Registries.STAT_TYPE.getCodec(),
			Util.memoize(ServerStatHandler::createCodec)
		).xmap(
			statsByType -> {
				Map<Stat<?>, Integer> merged = new HashMap<>();
				statsByType.forEach((type, typeStats) -> merged.putAll((Map<? extends Stat<?>, ? extends Integer>) typeStats));
				return merged;
			},
			stats -> stats.entrySet()
				.stream()
				.collect(Collectors.groupingBy(
					entry -> ((Stat<?>) entry.getKey()).getType(),
					Util.toMap()
				))
		);

	private final Path path;
	private final Set<Stat<?>> pendingStats = Sets.newHashSet();

	/**
	 * Создаёт обработчик и загружает статистику из файла, если он существует.
	 *
	 * @param server сервер (используется для получения {@link DataFixer})
	 * @param path   путь к JSON-файлу статистики игрока
	 */
	public ServerStatHandler(MinecraftServer server, Path path) {
		this.path = path;

		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				JsonElement json = StrictJsonParser.parse(reader);
				parse(server.getDataFixer(), json);
			} catch (IOException exception) {
				LOGGER.error("Couldn't read statistics file {}", path, exception);
			} catch (JsonParseException exception) {
				LOGGER.error("Couldn't parse statistics file {}", path, exception);
			}
		}
	}

	/**
	 * Сохраняет текущую статистику в JSON-файл.
	 * Создаёт родительские директории при необходимости.
	 */
	public void save() {
		try {
			PathUtil.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(asJson(), GSON.newJsonWriter(writer));
			}
		} catch (JsonIOException | IOException exception) {
			LOGGER.error("Couldn't save stats to {}", path, exception);
		}
	}

	@Override
	public void setStat(PlayerEntity player, Stat<?> stat, int value) {
		super.setStat(player, stat, value);
		pendingStats.add(stat);
	}

	/**
	 * Разбирает JSON-данные статистики, применяя DataFixer для миграции
	 * устаревших форматов данных.
	 *
	 * <p>Версия данных извлекается из поля {@code DataVersion} через
	 * {@link NbtHelper#getDataVersion}; при отсутствии используется версия 1343
	 * (первая версия с поддержкой DataFixer для статистик).
	 *
	 * @param dataFixer фиксер для миграции данных
	 * @param json      корневой JSON-элемент файла статистики
	 */
	public void parse(DataFixer dataFixer, JsonElement json) {
		Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, json);
		dynamic = DataFixTypes.STATS.update(dataFixer, dynamic, NbtHelper.getDataVersion(dynamic, 1343));

		statMap.putAll(
			CODEC.parse(dynamic.get("stats").orElseEmptyMap())
				.resultOrPartial(error -> LOGGER.error("Failed to parse statistics for {}: {}", path, error))
				.orElse(Map.of())
		);
	}

	/**
	 * Сериализует текущую статистику в JSON-объект для записи в файл.
	 *
	 * @return JSON-объект с полями {@code "stats"} и {@code "DataVersion"}
	 */
	protected JsonElement asJson() {
		JsonObject root = new JsonObject();
		root.add("stats", (JsonElement) CODEC.encodeStart(JsonOps.INSTANCE, statMap).getOrThrow());
		root.addProperty("DataVersion", SharedConstants.getGameVersion().dataVersion().id());
		return root;
	}

	/**
	 * Помечает все известные статистики как ожидающие отправки клиенту.
	 * Используется при первом подключении игрока, чтобы синхронизировать
	 * полное состояние статистики.
	 */
	public void updateStatSet() {
		pendingStats.addAll(statMap.keySet());
	}

	/**
	 * Отправляет накопленные изменения статистики указанному игроку
	 * через пакет {@link StatisticsS2CPacket}.
	 *
	 * <p>После отправки список ожидающих статистик очищается.
	 *
	 * @param player игрок, которому нужно отправить обновление
	 */
	public void sendStats(ServerPlayerEntity player) {
		Object2IntMap<Stat<?>> statsToSend = new Object2IntOpenHashMap<>();

		for (Stat<?> stat : takePendingStats()) {
			statsToSend.put(stat, getStat(stat));
		}

		player.networkHandler.sendPacket(new StatisticsS2CPacket(statsToSend));
	}

	/**
	 * Извлекает и очищает набор статистик, ожидающих отправки клиенту.
	 *
	 * @return снимок набора ожидающих статистик
	 */
	private Set<Stat<?>> takePendingStats() {
		Set<Stat<?>> snapshot = Sets.newHashSet(pendingStats);
		pendingStats.clear();
		return snapshot;
	}

	/**
	 * Создаёт кодек для карты {@code Stat<T> → Integer} внутри одного {@link StatType}.
	 *
	 * <p>При декодировании значение реестра преобразуется в {@link Stat} через
	 * {@link StatType#getOrCreateStat}. При кодировании проверяется, что тип
	 * статистики совпадает с ожидаемым — иначе возвращается ошибка.
	 *
	 * @param statType тип статистики, для которого создаётся кодек
	 * @param <T>      тип значения реестра
	 * @return кодек карты статистик данного типа
	 */
	private static <T> Codec<Map<Stat<?>, Integer>> createCodec(StatType<T> statType) {
		Codec<T> registryCodec = statType.getRegistry().getCodec();
		Codec<Stat<?>> statCodec = registryCodec.flatComapMap(
			statType::getOrCreateStat,
			stat -> stat.getType() == statType
				? DataResult.success((T) stat.getValue())
				: DataResult.error(() -> "Expected type " + statType + ", but got " + stat.getType())
		);
		return Codec.unboundedMap(statCodec, Codec.INT);
	}
}
