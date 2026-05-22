package net.minecraft.command.argument;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.*;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Тип аргумента команды для выбора одной или нескольких сущностей/игроков.
 *
 * <p>Поддерживает имена игроков, UUID и селекторы {@code @e}, {@code @a}, {@code @p} и т.д.
 * Флаги {@code singleTarget} и {@code playersOnly} ограничивают допустимые результаты.
 */
public class EntityArgumentType implements ArgumentType<EntitySelector> {

	private static final Collection<String> EXAMPLES =
			Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");

	public static final SimpleCommandExceptionType TOO_MANY_ENTITIES_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.entity.toomany"));

	public static final SimpleCommandExceptionType TOO_MANY_PLAYERS_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.player.toomany"));

	public static final SimpleCommandExceptionType PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.player.entities"));

	public static final SimpleCommandExceptionType ENTITY_NOT_FOUND_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.entity"));

	public static final SimpleCommandExceptionType PLAYER_NOT_FOUND_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.player"));

	public static final SimpleCommandExceptionType NOT_ALLOWED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.entity.selector.not_allowed"));

	final boolean singleTarget;
	final boolean playersOnly;

	protected EntityArgumentType(boolean singleTarget, boolean playersOnly) {
		this.singleTarget = singleTarget;
		this.playersOnly = playersOnly;
	}

	public static EntityArgumentType entity() {
		return new EntityArgumentType(true, false);
	}

	public static Entity getEntity(CommandContext<ServerCommandSource> context, String name)
	throws CommandSyntaxException {
		return ((EntitySelector) context.getArgument(name, EntitySelector.class))
				.getEntity((ServerCommandSource) context.getSource());
	}

	public static EntityArgumentType entities() {
		return new EntityArgumentType(false, false);
	}

	public static Collection<? extends Entity> getEntities(
			CommandContext<ServerCommandSource> context,
			String name
	) throws CommandSyntaxException {
		Collection<? extends Entity> entities = getOptionalEntities(context, name);

		if (entities.isEmpty()) {
			throw ENTITY_NOT_FOUND_EXCEPTION.create();
		}

		return entities;
	}

	public static Collection<? extends Entity> getOptionalEntities(
			CommandContext<ServerCommandSource> context,
			String name
	) throws CommandSyntaxException {
		return ((EntitySelector) context.getArgument(name, EntitySelector.class))
				.getEntities((ServerCommandSource) context.getSource());
	}

	public static Collection<ServerPlayerEntity> getOptionalPlayers(
			CommandContext<ServerCommandSource> context,
			String name
	) throws CommandSyntaxException {
		return ((EntitySelector) context.getArgument(name, EntitySelector.class))
				.getPlayers((ServerCommandSource) context.getSource());
	}

	public static EntityArgumentType player() {
		return new EntityArgumentType(true, true);
	}

	public static ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> context, String name)
	throws CommandSyntaxException {
		return ((EntitySelector) context.getArgument(name, EntitySelector.class))
				.getPlayer((ServerCommandSource) context.getSource());
	}

	public static EntityArgumentType players() {
		return new EntityArgumentType(false, true);
	}

	public static Collection<ServerPlayerEntity> getPlayers(
			CommandContext<ServerCommandSource> context,
			String name
	) throws CommandSyntaxException {
		List<ServerPlayerEntity> players = ((EntitySelector) context.getArgument(name, EntitySelector.class))
				.getPlayers((ServerCommandSource) context.getSource());

		if (players.isEmpty()) {
			throw PLAYER_NOT_FOUND_EXCEPTION.create();
		}

		return players;
	}

	@Override
	public EntitySelector parse(StringReader reader) throws CommandSyntaxException {
		return parse(reader, true);
	}

	public <S> EntitySelector parse(StringReader reader, S object) throws CommandSyntaxException {
		return parse(reader, EntitySelectorReader.shouldAllowAtSelectors(object));
	}

	/**
	 * Разбирает строку в {@link EntitySelector}, проверяя ограничения {@code singleTarget}
	 * и {@code playersOnly}. При нарушении сбрасывает курсор и бросает исключение.
	 */
	private EntitySelector parse(StringReader reader, boolean allowAtSelectors) throws CommandSyntaxException {
		EntitySelectorReader selectorReader = new EntitySelectorReader(reader, allowAtSelectors);
		EntitySelector selector = selectorReader.read();

		if (selector.getLimit() > 1 && singleTarget) {
			reader.setCursor(0);

			if (playersOnly) {
				throw TOO_MANY_PLAYERS_EXCEPTION.createWithContext(reader);
			}

			throw TOO_MANY_ENTITIES_EXCEPTION.createWithContext(reader);
		}

		if (selector.includesNonPlayers() && playersOnly && !selector.isSenderOnly()) {
			reader.setCursor(0);
			throw PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(reader);
		}

		return selector;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof CommandSource commandSource)) {
			return Suggestions.empty();
		}

		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		EntitySelectorReader selectorReader = new EntitySelectorReader(
				reader,
				commandSource.getPermissions().hasPermission(DefaultPermissions.ENTITY_SELECTORS)
		);

		try {
			selectorReader.read();
		}
		catch (CommandSyntaxException ignored) {
		}

		return selectorReader.listSuggestions(
				builder, innerBuilder -> {
					Collection<String> playerNames = commandSource.getPlayerNames();
					Iterable<String> suggestions = playersOnly
							? playerNames
							: Iterables.concat(playerNames, commandSource.getEntitySuggestions());
					CommandSource.suggestMatching(suggestions, innerBuilder);
				}
		);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/**
	 * Сериализатор для передачи параметров {@link EntityArgumentType} по сети.
	 * Кодирует флаги {@code single} и {@code playersOnly} в один байт.
	 */
	public static class Serializer
			implements ArgumentSerializer<EntityArgumentType, EntityArgumentType.Serializer.Properties> {

		private static final byte SINGLE_FLAG = 1;
		private static final byte PLAYERS_ONLY_FLAG = 2;

		@Override
		public void writePacket(EntityArgumentType.Serializer.Properties properties, PacketByteBuf buf) {
			int flags = 0;

			if (properties.single) {
				flags |= SINGLE_FLAG;
			}

			if (properties.playersOnly) {
				flags |= PLAYERS_ONLY_FLAG;
			}

			buf.writeByte(flags);
		}

		@Override
		public EntityArgumentType.Serializer.Properties fromPacket(PacketByteBuf buf) {
			byte flags = buf.readByte();
			return new EntityArgumentType.Serializer.Properties((flags & SINGLE_FLAG) != 0, (flags & PLAYERS_ONLY_FLAG) != 0);
		}

		@Override
		public void writeJson(EntityArgumentType.Serializer.Properties properties, JsonObject jsonObject) {
			jsonObject.addProperty("amount", properties.single ? "single" : "multiple");
			jsonObject.addProperty("type", properties.playersOnly ? "players" : "entities");
		}

		@Override
		public EntityArgumentType.Serializer.Properties getArgumentTypeProperties(EntityArgumentType type) {
			return new EntityArgumentType.Serializer.Properties(type.singleTarget, type.playersOnly);
		}

		public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<EntityArgumentType> {

			final boolean single;
			final boolean playersOnly;

			Properties(final boolean single, final boolean playersOnly) {
				this.single = single;
				this.playersOnly = playersOnly;
			}

			@Override
			public EntityArgumentType createType(CommandRegistryAccess registryAccess) {
				return new EntityArgumentType(single, playersOnly);
			}

			@Override
			public ArgumentSerializer<EntityArgumentType, ?> getSerializer() {
				return Serializer.this;
			}

		}

	}

}
