package net.minecraft.data.advancement;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Провайдер данных для генерации JSON-файлов достижений.
 * Делегирует генерацию набору {@link AdvancementTabGenerator}-ов,
 * проверяя уникальность идентификаторов достижений.
 */
public class AdvancementProvider implements DataProvider {

	private final DataOutput.PathResolver pathResolver;
	private final List<AdvancementTabGenerator> tabGenerators;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public AdvancementProvider(
			DataOutput output,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture,
			List<AdvancementTabGenerator> tabGenerators
	) {
		this.pathResolver = output.getResolver(RegistryKeys.ADVANCEMENT);
		this.tabGenerators = tabGenerators;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return registriesFuture.thenCompose(registries -> {
			Set<Identifier> registeredIds = new HashSet<>();
			List<CompletableFuture<?>> futures = new ArrayList<>();

			Consumer<AdvancementEntry> exporter = advancement -> {
				if (!registeredIds.add(advancement.id())) {
					throw new IllegalStateException("Duplicate advancement " + advancement.id());
				}

				Path path = pathResolver.resolveJson(advancement.id());
				futures.add(DataProvider.writeCodecToPath(
						writer,
						registries,
						Advancement.CODEC,
						advancement.value(),
						path
				));
			};

			for (AdvancementTabGenerator tabGenerator : tabGenerators) {
				tabGenerator.accept(registries, exporter);
			}

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		});
	}

	@Override
	public String getName() {
		return "Advancements";
	}
}
