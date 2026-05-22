package net.minecraft.data.report;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Провайдер данных для генерации отчётов о параметрах биомов.
 * Создаёт JSON-файлы в {@code reports/biome_parameters/} для каждого пресета
 * {@link MultiNoiseBiomeSourceParameterList}.
 */
public class BiomeParametersProvider implements DataProvider {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final MapCodec<RegistryKey<Biome>> BIOME_KEY_CODEC =
			RegistryKey.createCodec(RegistryKeys.BIOME).fieldOf("biome");

	private static final Codec<MultiNoiseUtil.Entries<RegistryKey<Biome>>> BIOME_ENTRY_CODEC =
			MultiNoiseUtil.Entries.createCodec(BIOME_KEY_CODEC).fieldOf("biomes").codec();

	private final Path basePath;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public BiomeParametersProvider(
			DataOutput output,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		this.basePath = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("biome_parameters");
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return registriesFuture.thenCompose(registries -> {
			DynamicOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);
			List<CompletableFuture<?>> futures = new ArrayList<>();

			MultiNoiseBiomeSourceParameterList.getPresetToEntriesMap().forEach((preset, entries) ->
					futures.add(writeEntries(resolvePath(preset.id()), writer, ops, BIOME_ENTRY_CODEC, entries))
			);

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		});
	}

	private static <E> CompletableFuture<?> writeEntries(
			Path path,
			DataWriter writer,
			DynamicOps<JsonElement> ops,
			Encoder<E> encoder,
			E value
	) {
		Optional<JsonElement> encoded = encoder.encodeStart(ops, value)
				.resultOrPartial(error -> LOGGER.error("Couldn't serialize element {}: {}", path, error));
		return encoded.isPresent()
				? DataProvider.writeToPath(writer, encoded.get(), path)
				: CompletableFuture.completedFuture(null);
	}

	private Path resolvePath(Identifier id) {
		return basePath.resolve(id.getNamespace()).resolve(id.getPath() + ".json");
	}

	@Override
	public String getName() {
		return "Biome Parameters";
	}
}
