package net.minecraft.command;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Расширение {@link RegistryWrapper.WrapperLookup} с поддержкой фильтрации
 * по включённым фичам. Используется при регистрации аргументов команд.
 */
public interface CommandRegistryAccess extends RegistryWrapper.WrapperLookup {

	/**
	 * Создаёт {@code CommandRegistryAccess}, оборачивающий переданный реестр
	 * и фильтрующий записи по набору включённых фич.
	 */
	static CommandRegistryAccess of(RegistryWrapper.WrapperLookup registries, FeatureSet enabledFeatures) {
		return new CommandRegistryAccess() {
			@Override
			public Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
				return registries.streamAllRegistryKeys();
			}

			@Override
			public <T> Optional<RegistryWrapper.Impl<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryRef) {
				return registries.getOptional(registryRef).map(wrapper -> wrapper.withFeatureFilter(enabledFeatures));
			}

			@Override
			public FeatureSet getEnabledFeatures() {
				return enabledFeatures;
			}
		};
	}

	FeatureSet getEnabledFeatures();
}
