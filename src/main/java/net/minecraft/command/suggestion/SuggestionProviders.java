package net.minecraft.command.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Реестр именованных провайдеров подсказок для команд Brigadier.
 * Провайдеры регистрируются по {@link Identifier} и могут быть сериализованы в NBT/JSON.
 * {@link #ASK_SERVER} — специальный провайдер, делегирующий подсказки серверу.
 */
public class SuggestionProviders {

	private static final Map<Identifier, SuggestionProvider<CommandSource>> REGISTRY = new HashMap<>();
	private static final Identifier ASK_SERVER_ID = Identifier.ofVanilla("ask_server");
	public static final SuggestionProvider<CommandSource> ASK_SERVER = register(
			ASK_SERVER_ID, (context, builder) -> ((CommandSource) context.getSource()).getCompletions(context)
	);
	public static final SuggestionProvider<CommandSource> AVAILABLE_SOUNDS = register(
			Identifier.ofVanilla("available_sounds"),
			(context, builder) -> CommandSource.suggestIdentifiers(
					((CommandSource) context.getSource()).getSoundIds(),
					builder
			)
	);
	public static final SuggestionProvider<CommandSource> SUMMONABLE_ENTITIES = register(
			Identifier.ofVanilla("summonable_entities"),
			(context, builder) -> CommandSource.suggestFromIdentifier(
					Registries.ENTITY_TYPE
							.stream()
							.filter(entityType ->
									entityType.isEnabled(((CommandSource) context.getSource()).getEnabledFeatures())
											&& entityType.isSummonable()),
					builder,
					EntityType::getId,
					EntityType::getName
			)
	);

	/**
	 * Регистрирует провайдер подсказок под заданным идентификатором.
	 * Бросает исключение, если провайдер с таким ID уже зарегистрирован.
	 */
	@SuppressWarnings("unchecked")
	public static <S extends CommandSource> SuggestionProvider<S> register(
			Identifier id,
			SuggestionProvider<CommandSource> provider
	) {
		SuggestionProvider<CommandSource> existing = REGISTRY.putIfAbsent(id, provider);
		if (existing != null) {
			throw new IllegalArgumentException(
					"A command suggestion provider is already registered with the name '" + id + "'");
		}

		return (SuggestionProvider<S>) new LocalProvider(id, provider);
	}

	@SuppressWarnings("unchecked")
	public static <S extends CommandSource> SuggestionProvider<S> cast(SuggestionProvider<CommandSource> suggestionProvider) {
		return (SuggestionProvider<S>) suggestionProvider;
	}

	public static <S extends CommandSource> SuggestionProvider<S> byId(Identifier id) {
		return cast(REGISTRY.getOrDefault(id, ASK_SERVER));
	}

	public static Identifier computeId(SuggestionProvider<?> provider) {
		return provider instanceof LocalProvider localProvider ? localProvider.id : ASK_SERVER_ID;
	}

	record LocalProvider(
			Identifier id,
			SuggestionProvider<CommandSource> provider
	) implements SuggestionProvider<CommandSource> {

		public CompletableFuture<Suggestions> getSuggestions(
				CommandContext<CommandSource> context,
				SuggestionsBuilder builder
		) throws CommandSyntaxException {
			return provider.getSuggestions(context, builder);
		}
	}
}
