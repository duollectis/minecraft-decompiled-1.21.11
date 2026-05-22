package net.minecraft.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.*;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Supplier;

/**
 * Тип аргумента команды Brigadier для выбора держателей очков (score holders).
 * <p>
 * Поддерживает три формата ввода:
 * <ul>
 *   <li>Селектор сущностей ({@code @a}, {@code @e}, {@code @p} и т.д.)</li>
 *   <li>Символ {@code *} — все известные держатели очков из скорборда</li>
 *   <li>Имя игрока или произвольная строка (включая UUID)</li>
 * </ul>
 */
public class ScoreHolderArgumentType implements ArgumentType<ScoreHolderArgumentType.ScoreHolders> {

	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		EntitySelectorReader entitySelectorReader = new EntitySelectorReader(
				stringReader,
				((ServerCommandSource) context.getSource())
						.getPermissions()
						.hasPermission(DefaultPermissions.ENTITY_SELECTORS)
		);

		try {
			entitySelectorReader.read();
		}
		catch (CommandSyntaxException var5) {
		}

		return entitySelectorReader.listSuggestions(
				builder,
				builderx -> CommandSource.suggestMatching(
						((ServerCommandSource) context.getSource()).getPlayerNames(),
						builderx
				)
		);
	};
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
	private static final SimpleCommandExceptionType EMPTY_SCORE_HOLDER_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("argument.scoreHolder.empty")
	);
	final boolean multiple;

	public ScoreHolderArgumentType(boolean multiple) {
		this.multiple = multiple;
	}

	public static ScoreHolder getScoreHolder(CommandContext<ServerCommandSource> context, String name)
	throws CommandSyntaxException {
		return getScoreHolders(context, name).iterator().next();
	}

	public static Collection<ScoreHolder> getScoreHolders(CommandContext<ServerCommandSource> context, String name)
	throws CommandSyntaxException {
		return getScoreHolders(context, name, Collections::emptyList);
	}

	public static Collection<ScoreHolder> getScoreboardScoreHolders(
			CommandContext<ServerCommandSource> context,
			String name
	) throws CommandSyntaxException {
		return getScoreHolders(
				context,
				name,
				((ServerCommandSource) context.getSource()).getServer().getScoreboard()::getKnownScoreHolders
		);
	}

	public static Collection<ScoreHolder> getScoreHolders(
			CommandContext<ServerCommandSource> context,
			String name,
			Supplier<Collection<ScoreHolder>> players
	) throws CommandSyntaxException {
		Collection<ScoreHolder>
				collection =
				((ScoreHolderArgumentType.ScoreHolders) context.getArgument(
						name,
						ScoreHolderArgumentType.ScoreHolders.class
				)
				)
						.getNames((ServerCommandSource) context.getSource(), players);
		if (collection.isEmpty()) {
			throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		else {
			return collection;
		}
	}

	public static ScoreHolderArgumentType scoreHolder() {
		return new ScoreHolderArgumentType(false);
	}

	public static ScoreHolderArgumentType scoreHolders() {
		return new ScoreHolderArgumentType(true);
	}

	public ScoreHolderArgumentType.ScoreHolders parse(StringReader stringReader) throws CommandSyntaxException {
		return this.parse(stringReader, true);
	}

	public <S> ScoreHolderArgumentType.ScoreHolders parse(StringReader stringReader, S object)
	throws CommandSyntaxException {
		return this.parse(stringReader, EntitySelectorReader.shouldAllowAtSelectors(object));
	}

	private ScoreHolderArgumentType.ScoreHolders parse(StringReader reader, boolean allowAtSelectors)
	throws CommandSyntaxException {
		if (reader.canRead() && reader.peek() == '@') {
			EntitySelectorReader entitySelectorReader = new EntitySelectorReader(reader, allowAtSelectors);
			EntitySelector entitySelector = entitySelectorReader.read();

			if (!multiple && entitySelector.getLimit() > 1) {
				throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.createWithContext(reader);
			}

			return new ScoreHolderArgumentType.SelectorScoreHolders(entitySelector);
		}

		int startCursor = reader.getCursor();

		while (reader.canRead() && reader.peek() != ' ') {
			reader.skip();
		}

		String rawInput = reader.getString().substring(startCursor, reader.getCursor());

		if (rawInput.equals("*")) {
			return (source, players) -> {
				Collection<ScoreHolder> allHolders = players.get();

				if (allHolders.isEmpty()) {
					throw EMPTY_SCORE_HOLDER_EXCEPTION.create();
				}

				return allHolders;
			};
		}

		List<ScoreHolder> singleHolder = List.of(ScoreHolder.fromName(rawInput));

		if (rawInput.startsWith("#")) {
			return (source, players) -> singleHolder;
		}

		try {
			UUID uuid = UUID.fromString(rawInput);

			return (source, holders) -> {
				MinecraftServer server = source.getServer();
				ScoreHolder firstFound = null;
				List<ScoreHolder> multipleFound = null;

				for (ServerWorld world : server.getWorlds()) {
					Entity entity = world.getEntity(uuid);

					if (entity == null) {
						continue;
					}

					if (firstFound == null) {
						firstFound = entity;
					}
					else {
						if (multipleFound == null) {
							multipleFound = new ArrayList<>();
							multipleFound.add(firstFound);
						}

						multipleFound.add(entity);
					}
				}

				if (multipleFound != null) {
					return multipleFound;
				}

				return firstFound != null ? List.of(firstFound) : singleHolder;
			};
		}
		catch (IllegalArgumentException ignored) {
			return (source, holders) -> {
				ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(rawInput);

				return player != null ? List.of(player) : singleHolder;
			};
		}
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/**
	 * Функциональный интерфейс, представляющий результат парсинга аргумента.
	 * Разрешает список держателей очков в момент выполнения команды, а не в момент парсинга.
	 */
	@FunctionalInterface
	public interface ScoreHolders {

		Collection<ScoreHolder> getNames(ServerCommandSource source, Supplier<Collection<ScoreHolder>> holders)
		throws CommandSyntaxException;
	}

	/**
	 * Реализация {@link ScoreHolders} на основе селектора сущностей ({@code @a}, {@code @e} и т.д.).
	 */
	public static class SelectorScoreHolders implements ScoreHolderArgumentType.ScoreHolders {

		private final EntitySelector selector;

		public SelectorScoreHolders(EntitySelector selector) {
			this.selector = selector;
		}

		@Override
		public Collection<ScoreHolder> getNames(
				ServerCommandSource serverCommandSource,
				Supplier<Collection<ScoreHolder>> supplier
		) throws CommandSyntaxException {
			List<? extends Entity> list = this.selector.getEntities(serverCommandSource);
			if (list.isEmpty()) {
				throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
			}
			else {
				return List.copyOf(list);
			}
		}
	}

	/**
	 * Сериализатор аргумента для передачи по сети и записи в JSON.
	 * Передаёт флаг режима: одиночный или множественный выбор.
	 */
	public static class Serializer implements ArgumentSerializer<ScoreHolderArgumentType, ScoreHolderArgumentType.Serializer.Properties> {

		private static final byte MULTIPLE_FLAG = 1;

		public void writePacket(ScoreHolderArgumentType.Serializer.Properties properties, PacketByteBuf packetByteBuf) {
			int flags = 0;

			if (properties.multiple) {
				flags |= MULTIPLE_FLAG;
			}

			packetByteBuf.writeByte(flags);
		}

		public ScoreHolderArgumentType.Serializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
			byte flags = packetByteBuf.readByte();
			boolean isMultiple = (flags & MULTIPLE_FLAG) != 0;

			return new ScoreHolderArgumentType.Serializer.Properties(isMultiple);
		}

		public void writeJson(ScoreHolderArgumentType.Serializer.Properties properties, JsonObject jsonObject) {
			jsonObject.addProperty("amount", properties.multiple ? "multiple" : "single");
		}

		public ScoreHolderArgumentType.Serializer.Properties getArgumentTypeProperties(ScoreHolderArgumentType scoreHolderArgumentType) {
			return new ScoreHolderArgumentType.Serializer.Properties(scoreHolderArgumentType.multiple);
		}

		/**
		 * Свойства сериализатора: хранит флаг режима выбора (одиночный/множественный).
		 */
		public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<ScoreHolderArgumentType> {

			final boolean multiple;

			Properties(final boolean multiple) {
				this.multiple = multiple;
			}

			public ScoreHolderArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
				return new ScoreHolderArgumentType(this.multiple);
			}

			@Override
			public ArgumentSerializer<ScoreHolderArgumentType, ?> getSerializer() {
				return Serializer.this;
			}
		}
	}
}
