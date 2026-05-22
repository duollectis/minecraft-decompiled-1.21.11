package net.minecraft.data.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Генерирует JSON-дамп всех реестров игры с их записями и protocol_id.
 * Для каждого реестра из {@link Registries#REGISTRIES} создаёт объект с полями:
 * {@code default} (только для {@link DefaultedRegistry}), {@code protocol_id} и {@code entries}.
 * Результат записывается в {@code reports/registries.json}.
 */
public class RegistryDumpProvider implements DataProvider {

	private final DataOutput output;

	public RegistryDumpProvider(DataOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		JsonObject root = new JsonObject();

		Registries.REGISTRIES
			.streamEntries()
			.forEach(entry -> root.add(
				entry.registryKey().getValue().toString(),
				toJson(entry.value())
			));

		Path path = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("registries.json");
		return DataProvider.writeToPath(writer, root, path);
	}

	/**
	 * Сериализует один реестр в JSON-объект.
	 * Если реестр является {@link DefaultedRegistry}, добавляет поле {@code default} с идентификатором
	 * элемента по умолчанию. Для каждой записи реестра добавляет её {@code protocol_id}.
	 */
	private static <T> JsonElement toJson(Registry<T> registry) {
		JsonObject registryJson = new JsonObject();

		if (registry instanceof DefaultedRegistry<T> defaulted) {
			Identifier defaultId = defaulted.getDefaultId();
			registryJson.addProperty("default", defaultId.toString());
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		int protocolId = ((Registry) Registries.REGISTRIES).getRawId(registry);
		registryJson.addProperty("protocol_id", protocolId);

		JsonObject entriesJson = new JsonObject();
		registry.streamEntries().forEach(entry -> {
			int entryProtocolId = registry.getRawId(entry.value());
			JsonObject entryJson = new JsonObject();
			entryJson.addProperty("protocol_id", entryProtocolId);
			entriesJson.add(entry.registryKey().getValue().toString(), entryJson);
		});

		registryJson.add("entries", entriesJson);
		return registryJson;
	}

	@Override
	public String getName() {
		return "Registry Dump";
	}
}
