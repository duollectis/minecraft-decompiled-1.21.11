package net.minecraft.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Базовый загрузчик данных из JSON-файлов ресурс-пака.
 * Использует {@link ResourceFinder} для поиска файлов и {@link Codec} для их декодирования.
 *
 * @param <T> тип загружаемых данных
 */
public abstract class JsonDataLoader<T> extends SinglePreparationResourceReloader<Map<Identifier, T>> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final DynamicOps<JsonElement> ops;
	private final Codec<T> codec;
	private final ResourceFinder finder;

	protected JsonDataLoader(
		RegistryWrapper.WrapperLookup registries,
		Codec<T> codec,
		RegistryKey<? extends Registry<T>> registryRef
	) {
		this(registries.getOps(JsonOps.INSTANCE), codec, ResourceFinder.json(registryRef));
	}

	protected JsonDataLoader(Codec<T> codec, ResourceFinder finder) {
		this(JsonOps.INSTANCE, codec, finder);
	}

	private JsonDataLoader(DynamicOps<JsonElement> ops, Codec<T> codec, ResourceFinder finder) {
		this.ops = ops;
		this.codec = codec;
		this.finder = finder;
	}

	@Override
	protected Map<Identifier, T> prepare(ResourceManager resourceManager, Profiler profiler) {
		Map<Identifier, T> results = new HashMap<>();
		load(resourceManager, finder, ops, codec, results);
		return results;
	}

	/**
	 * Загружает все JSON-ресурсы из реестра в переданную карту результатов.
	 *
	 * @param manager     менеджер ресурсов
	 * @param registryRef ключ реестра для построения {@link ResourceFinder}
	 * @param ops         операции сериализации
	 * @param codec       кодек для декодирования данных
	 * @param results     карта для записи результатов
	 */
	public static <T> void load(
		ResourceManager manager,
		RegistryKey<? extends Registry<T>> registryRef,
		DynamicOps<JsonElement> ops,
		Codec<T> codec,
		Map<Identifier, T> results
	) {
		load(manager, ResourceFinder.json(registryRef), ops, codec, results);
	}

	/**
	 * Загружает все JSON-ресурсы, найденные через {@code finder}, в переданную карту результатов.
	 * Дубликаты идентификаторов вызывают исключение.
	 *
	 * @param manager менеджер ресурсов
	 * @param finder  поисковик ресурсов
	 * @param ops     операции сериализации
	 * @param codec   кодек для декодирования данных
	 * @param results карта для записи результатов
	 */
	public static <T> void load(
		ResourceManager manager,
		ResourceFinder finder,
		DynamicOps<JsonElement> ops,
		Codec<T> codec,
		Map<Identifier, T> results
	) {
		for (Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
			Identifier resourcePath = entry.getKey();
			Identifier resourceId = finder.toResourceId(resourcePath);

			try (Reader reader = entry.getValue().getReader()) {
				codec.parse(ops, StrictJsonParser.parse(reader))
					.ifSuccess(value -> {
						if (results.putIfAbsent(resourceId, value) != null) {
							throw new IllegalStateException("Duplicate data file ignored with ID " + resourceId);
						}
					})
					.ifError(error -> LOGGER.error(
						"Couldn't parse data file '{}' from '{}': {}",
						resourceId, resourcePath, error
					));
			} catch (IllegalArgumentException | IOException | JsonParseException exception) {
				LOGGER.error(
					"Couldn't parse data file '{}' from '{}'",
					resourceId, resourcePath, exception
				);
			}
		}
	}
}
