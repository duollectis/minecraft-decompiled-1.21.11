package net.minecraft.command;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;

public interface CommandRegistryAccess extends RegistryWrapper.WrapperLookup {
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
