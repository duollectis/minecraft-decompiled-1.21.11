package net.minecraft.data.tag;

import net.minecraft.data.DataOutput;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagBuilder;
import net.minecraft.registry.tag.TagKey;

import java.util.concurrent.CompletableFuture;

/**
 * {@code SimpleTagProvider}.
 */
public abstract class SimpleTagProvider<T> extends TagProvider<T> {

	protected SimpleTagProvider(
			DataOutput dataOutput,
			RegistryKey<? extends Registry<T>> registryKey,
			CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture
	) {
		super(dataOutput, registryKey, completableFuture);
	}

	protected ProvidedTagBuilder<RegistryKey<T>, T> builder(TagKey<T> tag) {
		TagBuilder tagBuilder = getTagBuilder(tag);
		return ProvidedTagBuilder.of(tagBuilder);
	}
}
