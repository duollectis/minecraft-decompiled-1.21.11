package net.minecraft.data.tag;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricProvidedTagBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagBuilder;
import net.minecraft.registry.tag.TagKey;

public interface ProvidedTagBuilder<E, T> extends FabricProvidedTagBuilder<E, T> {
   ProvidedTagBuilder<E, T> add(E value);

   default ProvidedTagBuilder<E, T> add(E... values) {
      return this.add(Arrays.stream(values));
   }

   default ProvidedTagBuilder<E, T> add(Collection<E> values) {
      values.forEach(this::add);
      return this;
   }

   default ProvidedTagBuilder<E, T> add(Stream<E> values) {
      values.forEach(this::add);
      return this;
   }

   ProvidedTagBuilder<E, T> addOptional(E value);

   ProvidedTagBuilder<E, T> addTag(TagKey<T> tag);

   ProvidedTagBuilder<E, T> addOptionalTag(TagKey<T> tag);

   static <T> ProvidedTagBuilder<RegistryKey<T>, T> of(TagBuilder builder) {
      return new ProvidedTagBuilder<RegistryKey<T>, T>() {
         public ProvidedTagBuilder<RegistryKey<T>, T> add(RegistryKey<T> registryKey) {
            builder.add(registryKey.getValue());
            return this;
         }

         public ProvidedTagBuilder<RegistryKey<T>, T> addOptional(RegistryKey<T> registryKey) {
            builder.addOptional(registryKey.getValue());
            return this;
         }

         @Override
         public ProvidedTagBuilder<RegistryKey<T>, T> addTag(TagKey<T> tag) {
            builder.addTag(tag.id());
            return this;
         }

         @Override
         public ProvidedTagBuilder<RegistryKey<T>, T> addOptionalTag(TagKey<T> tag) {
            builder.addOptionalTag(tag.id());
            return this;
         }
      };
   }

   default <U> ProvidedTagBuilder<U, T> mapped(Function<U, E> mapper) {
      final ProvidedTagBuilder<E, T> providedTagBuilder = this;
      return new ProvidedTagBuilder<U, T>() {
         @Override
         public ProvidedTagBuilder<U, T> add(U value) {
            providedTagBuilder.add(mapper.apply(value));
            return this;
         }

         @Override
         public ProvidedTagBuilder<U, T> addOptional(U value) {
            providedTagBuilder.add(mapper.apply(value));
            return this;
         }

         @Override
         public ProvidedTagBuilder<U, T> addTag(TagKey<T> tag) {
            providedTagBuilder.addTag(tag);
            return this;
         }

         @Override
         public ProvidedTagBuilder<U, T> addOptionalTag(TagKey<T> tag) {
            providedTagBuilder.addOptionalTag(tag);
            return this;
         }
      };
   }
}
