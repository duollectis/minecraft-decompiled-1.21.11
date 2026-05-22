package net.minecraft.command;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.permission.PermissionSource;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Базовый интерфейс источника команды с утилитами для автодополнения.
 */
public interface CommandSource extends PermissionSource {

	/** Символы, считающиеся разделителями при поиске подстроки в идентификаторе. */
	CharMatcher SUGGESTION_MATCH_PREFIX = CharMatcher.anyOf("._/");

	/** ASCII-код символа {@code :} — разделителя namespace:path в идентификаторах. */
	int COLON_CHAR_CODE = ':';

	Collection<String> getPlayerNames();

	default Collection<String> getChatSuggestions() {
		return getPlayerNames();
	}

	default Collection<String> getEntitySuggestions() {
		return Collections.emptyList();
	}

	Collection<String> getTeamNames();

	Stream<Identifier> getSoundIds();

	CompletableFuture<Suggestions> getCompletions(CommandContext<?> context);

	default Collection<RelativePosition> getBlockPositionSuggestions() {
		return Collections.singleton(RelativePosition.ZERO_WORLD);
	}

	default Collection<RelativePosition> getPositionSuggestions() {
		return Collections.singleton(RelativePosition.ZERO_WORLD);
	}

	Set<RegistryKey<World>> getWorldKeys();

	DynamicRegistryManager getRegistryManager();

	FeatureSet getEnabledFeatures();

	default void suggestIdentifiers(
			RegistryWrapper<?> registry,
			SuggestedIdType suggestedIdType,
			SuggestionsBuilder builder
	) {
		if (suggestedIdType.canSuggestTags()) {
			suggestIdentifiers(registry.streamTagKeys().map(TagKey::id), builder, "#");
		}

		if (suggestedIdType.canSuggestElements()) {
			suggestIdentifiers(registry.streamKeys().map(RegistryKey::getValue), builder);
		}
	}

	static <S> CompletableFuture<Suggestions> listSuggestions(
			CommandContext<S> context,
			SuggestionsBuilder builder,
			RegistryKey<? extends Registry<?>> registryRef,
			SuggestedIdType suggestedIdType
	) {
		return context.getSource() instanceof CommandSource commandSource
				? commandSource.listIdSuggestions(registryRef, suggestedIdType, builder, context)
				: builder.buildFuture();
	}

	CompletableFuture<Suggestions> listIdSuggestions(
			RegistryKey<? extends Registry<?>> registryRef,
			SuggestedIdType suggestedIdType,
			SuggestionsBuilder builder,
			CommandContext<?> context
	);

	/**
	 * Перебирает кандидатов и вызывает {@code action} для тех, чей идентификатор
	 * совпадает с {@code remaining}. Если {@code remaining} содержит {@code :},
	 * сравнивается полная строка; иначе — namespace или path по отдельности.
	 */
	static <T> void forEachMatching(
			Iterable<T> candidates,
			String remaining,
			Function<T, Identifier> identifier,
			Consumer<T> action
	) {
		boolean hasColon = remaining.indexOf(COLON_CHAR_CODE) > -1;

		for (T candidate : candidates) {
			Identifier id = identifier.apply(candidate);
			if (hasColon) {
				if (shouldSuggest(remaining, id.toString())) {
					action.accept(candidate);
				}
			} else if (shouldSuggest(remaining, id.getNamespace()) || shouldSuggest(remaining, id.getPath())) {
				action.accept(candidate);
			}
		}
	}

	static <T> void forEachMatching(
			Iterable<T> candidates,
			String remaining,
			String prefix,
			Function<T, Identifier> identifier,
			Consumer<T> action
	) {
		if (remaining.isEmpty()) {
			candidates.forEach(action);
			return;
		}

		String commonPrefix = Strings.commonPrefix(remaining, prefix);
		if (!commonPrefix.isEmpty()) {
			String stripped = remaining.substring(commonPrefix.length());
			forEachMatching(candidates, stripped, identifier, action);
		}
	}

	static CompletableFuture<Suggestions> suggestIdentifiers(
			Iterable<Identifier> candidates,
			SuggestionsBuilder builder,
			String prefix
	) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		forEachMatching(candidates, remaining, prefix, id -> id, id -> builder.suggest(prefix + id));
		return builder.buildFuture();
	}

	static CompletableFuture<Suggestions> suggestIdentifiers(
			Stream<Identifier> candidates,
			SuggestionsBuilder builder,
			String prefix
	) {
		return suggestIdentifiers(candidates::iterator, builder, prefix);
	}

	static CompletableFuture<Suggestions> suggestIdentifiers(
			Iterable<Identifier> candidates,
			SuggestionsBuilder builder
	) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		forEachMatching(candidates, remaining, id -> id, id -> builder.suggest(id.toString()));
		return builder.buildFuture();
	}

	static <T> CompletableFuture<Suggestions> suggestFromIdentifier(
			Iterable<T> candidates,
			SuggestionsBuilder builder,
			Function<T, Identifier> identifier,
			Function<T, Message> tooltip
	) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		forEachMatching(
				candidates,
				remaining,
				identifier,
				candidate -> builder.suggest(identifier.apply(candidate).toString(), tooltip.apply(candidate))
		);
		return builder.buildFuture();
	}

	static CompletableFuture<Suggestions> suggestIdentifiers(
			Stream<Identifier> candidates,
			SuggestionsBuilder builder
	) {
		return suggestIdentifiers(candidates::iterator, builder);
	}

	static <T> CompletableFuture<Suggestions> suggestFromIdentifier(
			Stream<T> candidates,
			SuggestionsBuilder builder,
			Function<T, Identifier> identifier,
			Function<T, Message> tooltip
	) {
		return suggestFromIdentifier(candidates::iterator, builder, identifier, tooltip);
	}

	/**
	 * Генерирует подсказки для трёхкомпонентных позиций (x y z).
	 * Если {@code remaining} пустой — предлагает все варианты из {@code candidates};
	 * иначе дополняет уже введённые компоненты.
	 */
	static CompletableFuture<Suggestions> suggestPositions(
			String remaining,
			Collection<RelativePosition> candidates,
			SuggestionsBuilder builder,
			Predicate<String> predicate
	) {
		List<String> suggestions = Lists.newArrayList();
		if (Strings.isNullOrEmpty(remaining)) {
			for (RelativePosition position : candidates) {
				String full = position.x + " " + position.y + " " + position.z;
				if (predicate.test(full)) {
					suggestions.add(position.x);
					suggestions.add(position.x + " " + position.y);
					suggestions.add(full);
				}
			}
		} else {
			String[] parts = remaining.split(" ");
			if (parts.length == 1) {
				for (RelativePosition position : candidates) {
					String full = parts[0] + " " + position.y + " " + position.z;
					if (predicate.test(full)) {
						suggestions.add(parts[0] + " " + position.y);
						suggestions.add(full);
					}
				}
			} else if (parts.length == 2) {
				for (RelativePosition position : candidates) {
					String full = parts[0] + " " + parts[1] + " " + position.z;
					if (predicate.test(full)) {
						suggestions.add(full);
					}
				}
			}
		}

		return suggestMatching(suggestions, builder);
	}

	/**
	 * Генерирует подсказки для двухкомпонентных позиций (x z).
	 */
	static CompletableFuture<Suggestions> suggestColumnPositions(
			String remaining,
			Collection<RelativePosition> candidates,
			SuggestionsBuilder builder,
			Predicate<String> predicate
	) {
		List<String> suggestions = Lists.newArrayList();
		if (Strings.isNullOrEmpty(remaining)) {
			for (RelativePosition position : candidates) {
				String full = position.x + " " + position.z;
				if (predicate.test(full)) {
					suggestions.add(position.x);
					suggestions.add(full);
				}
			}
		} else {
			String[] parts = remaining.split(" ");
			if (parts.length == 1) {
				for (RelativePosition position : candidates) {
					String full = parts[0] + " " + position.z;
					if (predicate.test(full)) {
						suggestions.add(full);
					}
				}
			}
		}

		return suggestMatching(suggestions, builder);
	}

	static CompletableFuture<Suggestions> suggestMatching(Iterable<String> candidates, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

		for (String candidate : candidates) {
			if (shouldSuggest(remaining, candidate.toLowerCase(Locale.ROOT))) {
				builder.suggest(candidate);
			}
		}

		return builder.buildFuture();
	}

	static CompletableFuture<Suggestions> suggestMatching(Stream<String> candidates, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		candidates
				.filter(candidate -> shouldSuggest(remaining, candidate.toLowerCase(Locale.ROOT)))
				.forEach(builder::suggest);
		return builder.buildFuture();
	}

	static CompletableFuture<Suggestions> suggestMatching(String[] candidates, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

		for (String candidate : candidates) {
			if (shouldSuggest(remaining, candidate.toLowerCase(Locale.ROOT))) {
				builder.suggest(candidate);
			}
		}

		return builder.buildFuture();
	}

	static <T> CompletableFuture<Suggestions> suggestMatching(
			Iterable<T> candidates,
			SuggestionsBuilder builder,
			Function<T, String> suggestionText,
			Function<T, Message> tooltip
	) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

		for (T candidate : candidates) {
			String text = suggestionText.apply(candidate);
			if (shouldSuggest(remaining, text.toLowerCase(Locale.ROOT))) {
				builder.suggest(text, tooltip.apply(candidate));
			}
		}

		return builder.buildFuture();
	}

	/**
	 * Проверяет, является ли {@code remaining} подстрокой {@code candidate},
	 * начинающейся после одного из символов-разделителей {@link #SUGGESTION_MATCH_PREFIX}.
	 */
	static boolean shouldSuggest(String remaining, String candidate) {
		int offset = 0;

		while (!candidate.startsWith(remaining, offset)) {
			int separatorPos = SUGGESTION_MATCH_PREFIX.indexIn(candidate, offset);
			if (separatorPos < 0) {
				return false;
			}

			offset = separatorPos + 1;
		}

		return true;
	}

	/**
	 * Относительная позиция для подсказок команд (например, {@code ~ ~ ~} или {@code ^ ^ ^}).
	 */
	class RelativePosition {

		public static final RelativePosition ZERO_LOCAL = new RelativePosition("^", "^", "^");
		public static final RelativePosition ZERO_WORLD = new RelativePosition("~", "~", "~");

		public final String x;
		public final String y;
		public final String z;

		public RelativePosition(String x, String y, String z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	/**
	 * Тип предлагаемых идентификаторов: теги, элементы или оба варианта.
	 */
	enum SuggestedIdType {
		TAGS,
		ELEMENTS,
		ALL;

		public boolean canSuggestTags() {
			return this == TAGS || this == ALL;
		}

		public boolean canSuggestElements() {
			return this == ELEMENTS || this == ALL;
		}
	}
}
