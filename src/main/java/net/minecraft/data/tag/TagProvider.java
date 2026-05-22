package net.minecraft.data.tag;

import com.google.common.collect.Maps;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagBuilder;
import net.minecraft.registry.tag.TagEntry;
import net.minecraft.registry.tag.TagFile;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Абстрактный провайдер данных для генерации тегов реестра.
 * <p>
 * Подклассы переопределяют метод {@link #configure(RegistryWrapper.WrapperLookup)},
 * в котором через {@link #getTagBuilder(TagKey)} наполняют теги записями.
 * При запуске провайдер проверяет корректность всех ссылок (записи и теги должны существовать
 * в реестре или в родительском провайдере) и записывает JSON-файлы тегов на диск.
 *
 * @param <T> тип объекта реестра, для которого генерируются теги
 */
public abstract class TagProvider<T> implements DataProvider {

	protected final DataOutput.PathResolver pathResolver;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;
	private final CompletableFuture<Void> registryLoadFuture = new CompletableFuture<>();
	private final CompletableFuture<TagProvider.TagLookup<T>> parentTagLookupFuture;
	protected final RegistryKey<? extends Registry<T>> registryRef;
	private final Map<Identifier, TagBuilder> tagBuilders = Maps.newLinkedHashMap();

	protected TagProvider(
			DataOutput output,
			RegistryKey<? extends Registry<T>> registryRef,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		this(output, registryRef, registriesFuture, CompletableFuture.completedFuture(TagProvider.TagLookup.empty()));
	}

	protected TagProvider(
			DataOutput output,
			RegistryKey<? extends Registry<T>> registryRef,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture,
			CompletableFuture<TagProvider.TagLookup<T>> parentTagLookupFuture
	) {
		this.pathResolver = output.getTagResolver(registryRef);
		this.registryRef = registryRef;
		this.parentTagLookupFuture = parentTagLookupFuture;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public String getName() {
		return "Tags for " + registryRef.getValue();
	}

	protected abstract void configure(RegistryWrapper.WrapperLookup registries);

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		record RegistryInfo<T>(RegistryWrapper.WrapperLookup contents, TagProvider.TagLookup<T> parent) {
		}

		return getRegistriesFuture()
				.thenApply(registries -> {
					registryLoadFuture.complete(null);
					return (RegistryWrapper.WrapperLookup) registries;
				})
				.thenCombineAsync(
						parentTagLookupFuture,
						(registries, parent) -> new RegistryInfo<>(registries, (TagProvider.TagLookup<T>) parent),
						Util.getMainWorkerExecutor()
				)
				.thenCompose(info -> {
					RegistryWrapper.Impl<T> registryWrapper = info.contents.getOrThrow(registryRef);
					Predicate<Identifier> entryExistsInRegistry =
							id -> registryWrapper.getOptional(RegistryKey.of(registryRef, id)).isPresent();
					Predicate<Identifier> tagExistsInParent =
							id -> tagBuilders.containsKey(id)
									|| info.parent.contains(TagKey.of(registryRef, id));

					return CompletableFuture.allOf(
							tagBuilders.entrySet()
									.stream()
									.map(entry -> {
										Identifier tagId = entry.getKey();
										TagBuilder tagBuilder = entry.getValue();
										List<TagEntry> tagEntries = tagBuilder.build();
										List<TagEntry> invalidEntries = tagEntries
												.stream()
												.filter(tagEntry -> !tagEntry.canAdd(
														entryExistsInRegistry,
														tagExistsInParent
												))
												.toList();

										if (!invalidEntries.isEmpty()) {
											throw new IllegalArgumentException(
													String.format(
															Locale.ROOT,
															"Couldn't define tag %s as it is missing following references: %s",
															tagId,
															invalidEntries.stream()
																	.map(Objects::toString)
																	.collect(Collectors.joining(","))
													)
											);
										}

										Path path = pathResolver.resolveJson(tagId);

										return DataProvider.writeCodecToPath(
												writer,
												info.contents,
												TagFile.CODEC,
												new TagFile(tagEntries, false),
												path
										);
									})
									.toArray(CompletableFuture[]::new)
					);
				});
	}

	protected TagBuilder getTagBuilder(TagKey<T> tag) {
		return tagBuilders.computeIfAbsent(tag.id(), id -> TagBuilder.create());
	}

	public CompletableFuture<TagProvider.TagLookup<T>> getTagLookupFuture() {
		return registryLoadFuture.thenApply(void_ -> tag -> Optional.ofNullable(tagBuilders.get(tag.id())));
	}

	protected CompletableFuture<RegistryWrapper.WrapperLookup> getRegistriesFuture() {
		return registriesFuture.thenApply(registries -> {
			tagBuilders.clear();
			configure(registries);
			return (RegistryWrapper.WrapperLookup) registries;
		});
	}

	/**
	 * Функциональный интерфейс для поиска {@link TagBuilder} по ключу тега.
	 * Используется для проверки существования тегов в родительском провайдере.
	 */
	@FunctionalInterface
	public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {

		static <T> TagProvider.TagLookup<T> empty() {
			return tag -> Optional.empty();
		}

		default boolean contains(TagKey<T> tag) {
			return this.apply(tag).isPresent();
		}
	}
}
