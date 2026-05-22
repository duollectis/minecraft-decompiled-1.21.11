package net.minecraft.registry;

import com.mojang.datafixers.DataFixUtils;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.PlacedFeature;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Валидирует экспериментальные реестры, проверяя корректность биомов
 * и размещённых фич перед применением патчей из {@link RegistryBuilder}.
 * <p>
 * Используется в datagen-пайплайне для раннего обнаружения ошибок конфигурации
 * экспериментальных фич (feature flags).
 */
public class ExperimentalRegistriesValidator {

	/**
	 * Применяет патчи из {@code builder} к реестрам и валидирует результат.
	 * Если в патчах присутствуют биомы или размещённые фичи — проверяет их
	 * через {@link BuiltinRegistries#validate}, используя патченные версии
	 * или оригинальные реестры как fallback.
	 *
	 * @param registriesFuture будущий lookup базовых реестров
	 * @param builder          билдер с экспериментальными патчами
	 * @return будущая пара (полный lookup + патчи)
	 */
	public static CompletableFuture<RegistryBuilder.FullPatchesRegistriesPair> validate(
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture,
			RegistryBuilder builder
	) {
		return registriesFuture.thenApply(registries -> {
			DynamicRegistryManager.Immutable immutable = DynamicRegistryManager.of(Registries.REGISTRIES);
			RegistryCloner.CloneableRegistries cloneableRegistries = new RegistryCloner.CloneableRegistries();
			RegistryLoader.DYNAMIC_REGISTRIES.forEach(entry -> entry.addToCloner(cloneableRegistries::add));

			RegistryBuilder.FullPatchesRegistriesPair fullPatchesRegistriesPair =
					builder.createWrapperLookup(immutable, registries, cloneableRegistries);

			RegistryWrapper.WrapperLookup wrapperLookup = fullPatchesRegistriesPair.full();
			Optional<? extends RegistryWrapper.Impl<Biome>> biomeWrapper = wrapperLookup.getOptional(RegistryKeys.BIOME);
			Optional<? extends RegistryWrapper.Impl<PlacedFeature>> featureWrapper =
					wrapperLookup.getOptional(RegistryKeys.PLACED_FEATURE);

			if (biomeWrapper.isPresent() || featureWrapper.isPresent()) {
				BuiltinRegistries.validate(
						(RegistryEntryLookup<PlacedFeature>) DataFixUtils.orElseGet(
								featureWrapper,
								() -> registries.getOrThrow(RegistryKeys.PLACED_FEATURE)
						),
						(RegistryWrapper<Biome>) DataFixUtils.orElseGet(
								biomeWrapper,
								() -> registries.getOrThrow(RegistryKeys.BIOME)
						)
				);
			}

			return fullPatchesRegistriesPair;
		});
	}
}
