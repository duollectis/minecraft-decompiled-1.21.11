package net.minecraft.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.RegistryWrapper;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Провайдер данных для динамических реестров.
 * Сериализует все записи каждого динамического реестра в JSON-файлы
 * по пути {@code data/<namespace>/<registry_path>/<entry_path>.json}.
 */
public class DynamicRegistriesProvider implements DataProvider {

	private final DataOutput output;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public DynamicRegistriesProvider(
			DataOutput output,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		this.output = output;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return registriesFuture.thenCompose(registries -> {
			DynamicOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);
			return CompletableFuture.allOf(
					RegistryLoader.DYNAMIC_REGISTRIES
							.stream()
							.flatMap(entry -> writeRegistryEntries(writer, registries, ops, entry).stream())
							.toArray(CompletableFuture[]::new)
			);
		});
	}

	private <T> Optional<CompletableFuture<?>> writeRegistryEntries(
			DataWriter writer,
			RegistryWrapper.WrapperLookup registries,
			DynamicOps<JsonElement> ops,
			RegistryLoader.Entry<T> registry
	) {
		RegistryKey<? extends Registry<T>> registryKey = registry.key();
		return registries.getOptional(registryKey).map(wrapper -> {
			DataOutput.PathResolver pathResolver = output.getResolver(registryKey);
			return CompletableFuture.allOf(
					wrapper.streamEntries()
							.map(entry -> writeToPath(
									pathResolver.resolveJson(entry.registryKey().getValue()),
									writer,
									ops,
									registry.elementCodec(),
									entry.value()
							))
							.toArray(CompletableFuture[]::new)
			);
		});
	}

	private static <E> CompletableFuture<?> writeToPath(
			Path path,
			DataWriter writer,
			DynamicOps<JsonElement> ops,
			Encoder<E> encoder,
			E value
	) {
		return encoder.encodeStart(ops, value).mapOrElse(
				jsonElement -> DataProvider.writeToPath(writer, jsonElement, path),
				error -> CompletableFuture.failedFuture(
						new IllegalStateException("Couldn't generate file '" + path + "': " + error.message())
				)
		);
	}

	@Override
	public String getName() {
		return "Registries";
	}
}
