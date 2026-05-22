package net.minecraft.data.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.component.ComponentMap;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Генерирует JSON-отчёт со списком всех зарегистрированных предметов и их компонентами.
 * Для каждого предмета сериализует {@link ComponentMap} через {@link ComponentMap#CODEC}
 * и записывает результат в {@code reports/items.json}.
 */
public class ItemListProvider implements DataProvider {

	private final DataOutput output;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public ItemListProvider(DataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		this.output = output;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Path path = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("items.json");

		return registriesFuture.thenCompose(registries -> {
			JsonObject root = new JsonObject();
			RegistryOps<JsonElement> registryOps = registries.getOps(JsonOps.INSTANCE);

			registries.getOrThrow(RegistryKeys.ITEM)
				.streamEntries()
				.forEach(entry -> {
					JsonElement components = ComponentMap.CODEC
						.encodeStart(registryOps, entry.value().getComponents())
						.getOrThrow(error -> new IllegalStateException("Failed to encode components: " + error));

					JsonObject itemJson = new JsonObject();
					itemJson.add("components", components);
					root.add(entry.getIdAsString(), itemJson);
				});

			return DataProvider.writeToPath(writer, root, path);
		});
	}

	@Override
	public String getName() {
		return "Item List";
	}
}
