package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.command.permission.PermissionSource;
import net.minecraft.command.permission.PermissionSourcePredicate;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.dedicated.command.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Центральный менеджер команд сервера: регистрирует все команды Brigadier,
 * управляет выполнением команд и отправкой дерева команд игрокам.
 */
public class CommandManager {

	public static final String PREFIX = "/";

	private static final ThreadLocal<@Nullable CommandExecutionContext<ServerCommandSource>> CURRENT_CONTEXT =
			new ThreadLocal<>();
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final PermissionCheck ALWAYS_PASS_CHECK = PermissionCheck.AlwaysPass.INSTANCE;
	public static final PermissionCheck MODERATORS_CHECK = new PermissionCheck.Require(DefaultPermissions.MODERATORS);
	public static final PermissionCheck GAMEMASTERS_CHECK = new PermissionCheck.Require(DefaultPermissions.GAMEMASTERS);
	public static final PermissionCheck ADMINS_CHECK = new PermissionCheck.Require(DefaultPermissions.ADMINS);
	public static final PermissionCheck OWNERS_CHECK = new PermissionCheck.Require(DefaultPermissions.OWNERS);
	private static final CommandTreeS2CPacket.CommandNodeInspector<ServerCommandSource> INSPECTOR =
			new CommandTreeS2CPacket.CommandNodeInspector<ServerCommandSource>() {
				private final ServerCommandSource source = CommandManager.createSource(PermissionPredicate.NONE);

				@Override
				public @Nullable Identifier getSuggestionProviderId(ArgumentCommandNode<ServerCommandSource, ?> node) {
					SuggestionProvider<ServerCommandSource> suggestionProvider = node.getCustomSuggestions();
					return suggestionProvider != null ? SuggestionProviders.computeId(suggestionProvider) : null;
				}

				@Override
				public boolean isExecutable(CommandNode<ServerCommandSource> node) {
					return node.getCommand() != null;
				}

				@Override
				public boolean hasRequiredLevel(CommandNode<ServerCommandSource> node) {
					Predicate<ServerCommandSource> predicate = node.getRequirement();
					return !predicate.test(this.source);
				}
			};

	private final CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>();

	public CommandManager(
			CommandManager.RegistrationEnvironment environment,
			CommandRegistryAccess commandRegistryAccess
	) {
		AdvancementCommand.register(this.dispatcher);
		AttributeCommand.register(this.dispatcher, commandRegistryAccess);
		ExecuteCommand.register(this.dispatcher, commandRegistryAccess);
		BossBarCommand.register(this.dispatcher, commandRegistryAccess);
		ClearCommand.register(this.dispatcher, commandRegistryAccess);
		CloneCommand.register(this.dispatcher, commandRegistryAccess);
		DamageCommand.register(this.dispatcher, commandRegistryAccess);
		DataCommand.register(this.dispatcher);
		DatapackCommand.register(this.dispatcher, commandRegistryAccess);
		DebugCommand.register(this.dispatcher);
		DefaultGameModeCommand.register(this.dispatcher);
		DialogCommand.register(this.dispatcher, commandRegistryAccess);
		DifficultyCommand.register(this.dispatcher);
		EffectCommand.register(this.dispatcher, commandRegistryAccess);
		MeCommand.register(this.dispatcher);
		EnchantCommand.register(this.dispatcher, commandRegistryAccess);
		ExperienceCommand.register(this.dispatcher);
		FillCommand.register(this.dispatcher, commandRegistryAccess);
		FillBiomeCommand.register(this.dispatcher, commandRegistryAccess);
		ForceLoadCommand.register(this.dispatcher);
		FunctionCommand.register(this.dispatcher);
		GameModeCommand.register(this.dispatcher);
		GameRuleCommand.register(this.dispatcher, commandRegistryAccess);
		GiveCommand.register(this.dispatcher, commandRegistryAccess);
		HelpCommand.register(this.dispatcher);
		ItemCommand.register(this.dispatcher, commandRegistryAccess);
		KickCommand.register(this.dispatcher);
		KillCommand.register(this.dispatcher);
		ListCommand.register(this.dispatcher);
		LocateCommand.register(this.dispatcher, commandRegistryAccess);
		LootCommand.register(this.dispatcher, commandRegistryAccess);
		MessageCommand.register(this.dispatcher);
		ParticleCommand.register(this.dispatcher, commandRegistryAccess);
		PlaceCommand.register(this.dispatcher);
		PlaySoundCommand.register(this.dispatcher);
		RandomCommand.register(this.dispatcher);
		ReloadCommand.register(this.dispatcher);
		RecipeCommand.register(this.dispatcher);
		FetchProfileCommand.register(this.dispatcher);
		ReturnCommand.register(this.dispatcher);
		RideCommand.register(this.dispatcher);
		RotateCommand.register(this.dispatcher);
		SayCommand.register(this.dispatcher);
		ScheduleCommand.register(this.dispatcher);
		ScoreboardCommand.register(this.dispatcher, commandRegistryAccess);
		SeedCommand.register(this.dispatcher, environment != CommandManager.RegistrationEnvironment.INTEGRATED);
		VersionCommand.register(this.dispatcher, environment != CommandManager.RegistrationEnvironment.INTEGRATED);
		SetBlockCommand.register(this.dispatcher, commandRegistryAccess);
		SpawnPointCommand.register(this.dispatcher);
		SetWorldSpawnCommand.register(this.dispatcher);
		SpectateCommand.register(this.dispatcher);
		SpreadPlayersCommand.register(this.dispatcher);
		StopSoundCommand.register(this.dispatcher);
		StopwatchCommand.register(this.dispatcher);
		SummonCommand.register(this.dispatcher, commandRegistryAccess);
		TagCommand.register(this.dispatcher);
		TeamCommand.register(this.dispatcher, commandRegistryAccess);
		TeamMsgCommand.register(this.dispatcher);
		TeleportCommand.register(this.dispatcher);
		TellRawCommand.register(this.dispatcher, commandRegistryAccess);
		TestCommand.register(this.dispatcher, commandRegistryAccess);
		TickCommand.register(this.dispatcher);
		TimeCommand.register(this.dispatcher);
		TitleCommand.register(this.dispatcher, commandRegistryAccess);
		TriggerCommand.register(this.dispatcher);
		WaypointCommand.register(this.dispatcher, commandRegistryAccess);
		WeatherCommand.register(this.dispatcher);
		WorldBorderCommand.register(this.dispatcher);
		if (FlightProfiler.INSTANCE.isAvailable()) {
			JfrCommand.register(this.dispatcher);
		}

		if (SharedConstants.CHASE_COMMAND) {
			ChaseCommand.register(this.dispatcher);
		}

		if (SharedConstants.DEV_COMMANDS || SharedConstants.isDevelopment) {
			RaidCommand.register(this.dispatcher, commandRegistryAccess);
			DebugPathCommand.register(this.dispatcher);
			DebugMobSpawningCommand.register(this.dispatcher);
			WardenSpawnTrackerCommand.register(this.dispatcher);
			SpawnArmorTrimsCommand.register(this.dispatcher);
			ServerPackCommand.register(this.dispatcher);
			if (environment.dedicated) {
				DebugConfigCommand.register(this.dispatcher, commandRegistryAccess);
			}
		}

		if (environment.dedicated) {
			BanIpCommand.register(this.dispatcher);
			BanListCommand.register(this.dispatcher);
			BanCommand.register(this.dispatcher);
			DeOpCommand.register(this.dispatcher);
			OpCommand.register(this.dispatcher);
			PardonCommand.register(this.dispatcher);
			PardonIpCommand.register(this.dispatcher);
			PerfCommand.register(this.dispatcher);
			SaveAllCommand.register(this.dispatcher);
			SaveOffCommand.register(this.dispatcher);
			SaveOnCommand.register(this.dispatcher);
			SetIdleTimeoutCommand.register(this.dispatcher);
			StopCommand.register(this.dispatcher);
			TransferCommand.register(this.dispatcher);
			WhitelistCommand.register(this.dispatcher);
		}

		if (environment.integrated) {
			PublishCommand.register(this.dispatcher);
		}

		this.dispatcher.setConsumer(AbstractServerCommandSource.asResultConsumer());
	}

	public static <S> ParseResults<S> withCommandSource(ParseResults<S> parseResults, UnaryOperator<S> sourceMapper) {
		CommandContextBuilder<S> context = parseResults.getContext();
		CommandContextBuilder<S> mappedContext = context.withSource(sourceMapper.apply((S) context.getSource()));
		return new ParseResults(mappedContext, parseResults.getReader(), parseResults.getExceptions());
	}

	public void parseAndExecute(ServerCommandSource source, String command) {
		command = stripLeadingSlash(command);
		execute(dispatcher.parse(command, source), command);
	}

	public static String stripLeadingSlash(String command) {
		return command.startsWith("/") ? command.substring(1) : command;
	}

	public void execute(ParseResults<ServerCommandSource> parseResults, String command) {
		ServerCommandSource source = (ServerCommandSource) parseResults.getContext().getSource();
		Profilers.get().push(() -> "/" + command);
		ContextChain<ServerCommandSource> contextChain = checkCommand(parseResults, command, source);

		try {
			if (contextChain != null) {
				callWithContext(
						source,
						context -> CommandExecutionContext.enqueueCommand(
								context,
								command,
								contextChain,
								source,
								ReturnValueConsumer.EMPTY
						)
				);
			}
		}
		catch (Exception exception) {
			MutableText errorText = Text.literal(
					exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()
			);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.error("Command exception: /{}", command, exception);
				StackTraceElement[] stackTrace = exception.getStackTrace();

				for (int stackTraceIndex = 0; stackTraceIndex < Math.min(stackTrace.length, 3); stackTraceIndex++) {
					errorText.append("\n\n")
					         .append(stackTrace[stackTraceIndex].getMethodName())
					         .append("\n ")
					         .append(stackTrace[stackTraceIndex].getFileName())
					         .append(":")
					         .append(String.valueOf(stackTrace[stackTraceIndex].getLineNumber()));
				}
			}

			source.sendError(Text
					.translatable("command.failed")
					.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(errorText))));

			if (SharedConstants.VERBOSE_COMMAND_ERRORS || SharedConstants.isDevelopment) {
				source.sendError(Text.literal(Util.getInnermostMessage(exception)));
				LOGGER.error("'/{}' threw an exception", command, exception);
			}
		}
		finally {
			Profilers.get().pop();
		}
	}

	private static @Nullable ContextChain<ServerCommandSource> checkCommand(
			ParseResults<ServerCommandSource> parseResults, String command, ServerCommandSource source
	) {
		try {
			throwException(parseResults);
			return (ContextChain<ServerCommandSource>) ContextChain.tryFlatten(parseResults.getContext().build(command))
			                                                       .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS
					                                                       .dispatcherUnknownCommand()
					                                                       .createWithContext(parseResults.getReader()));
		}
		catch (CommandSyntaxException exception) {
			source.sendError(Texts.toText(exception.getRawMessage()));

			if (exception.getInput() == null || exception.getCursor() < 0) {
				return null;
			}

			int cursorPos = Math.min(exception.getInput().length(), exception.getCursor());
			MutableText errorContext = Text.empty()
			                              .formatted(Formatting.GRAY)
			                              .styled(style -> style.withClickEvent(
					                              new ClickEvent.SuggestCommand("/" + command)
			                              ));

			if (cursorPos > 10) {
				errorContext.append(ScreenTexts.ELLIPSIS);
			}

			errorContext.append(exception.getInput().substring(Math.max(0, cursorPos - 10), cursorPos));

			if (cursorPos < exception.getInput().length()) {
				errorContext.append(
						Text.literal(exception.getInput().substring(cursorPos))
						    .formatted(Formatting.RED, Formatting.UNDERLINE)
				);
			}

			errorContext.append(
					Text.translatable("command.context.here").formatted(Formatting.RED, Formatting.ITALIC)
			);
			source.sendError(errorContext);

			return null;
		}
	}

	/**
	 * Выполняет {@code callback} в контексте выполнения команды.
	 * Если контекст уже существует (вложенный вызов), переиспользует его;
	 * иначе создаёт новый с лимитами из правил игры и запускает очередь.
	 */
	public static void callWithContext(
			ServerCommandSource commandSource,
			Consumer<CommandExecutionContext<ServerCommandSource>> callback
	) {
		CommandExecutionContext<ServerCommandSource> existingContext = CURRENT_CONTEXT.get();

		if (existingContext != null) {
			callback.accept(existingContext);
			return;
		}

		GameRules gameRules = commandSource.getWorld().getGameRules();
		int maxSequenceLength = Math.max(1, gameRules.getValue(GameRules.MAX_COMMAND_SEQUENCE_LENGTH));
		int maxForks = gameRules.getValue(GameRules.MAX_COMMAND_FORKS);

		try (CommandExecutionContext<ServerCommandSource> context = new CommandExecutionContext<>(
				maxSequenceLength,
				maxForks,
				Profilers.get()
		)) {
			CURRENT_CONTEXT.set(context);
			callback.accept(context);
			context.run();
		}
		finally {
			CURRENT_CONTEXT.set(null);
		}
	}

	public void sendCommandTree(ServerPlayerEntity player) {
		Map<CommandNode<ServerCommandSource>, CommandNode<ServerCommandSource>> nodeMap = new HashMap<>();
		RootCommandNode<ServerCommandSource> filteredRoot = new RootCommandNode<>();
		nodeMap.put(dispatcher.getRoot(), filteredRoot);
		deepCopyNodes(dispatcher.getRoot(), filteredRoot, player.getCommandSource(), nodeMap);
		player.networkHandler.sendPacket(new CommandTreeS2CPacket(filteredRoot, INSPECTOR));
	}

	private static <S> void deepCopyNodes(
			CommandNode<S> root,
			CommandNode<S> newRoot,
			S source,
			Map<CommandNode<S>, CommandNode<S>> nodes
	) {
		for (CommandNode<S> commandNode : root.getChildren()) {
			if (commandNode.canUse(source)) {
				ArgumentBuilder<S, ?> argumentBuilder = commandNode.createBuilder();

				if (argumentBuilder.getRedirect() != null) {
					argumentBuilder.redirect(nodes.get(argumentBuilder.getRedirect()));
				}

				CommandNode<S> copiedNode = argumentBuilder.build();
				nodes.put(commandNode, copiedNode);
				newRoot.addChild(copiedNode);

				if (!commandNode.getChildren().isEmpty()) {
					deepCopyNodes(commandNode, copiedNode, source, nodes);
				}
			}
		}
	}

	public static LiteralArgumentBuilder<ServerCommandSource> literal(String literal) {
		return LiteralArgumentBuilder.literal(literal);
	}

	public static <T> RequiredArgumentBuilder<ServerCommandSource, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
	}

	public static Predicate<String> getCommandValidator(CommandManager.CommandParser parser) {
		return string -> {
			try {
				parser.parse(new StringReader(string));
				return true;
			}
			catch (CommandSyntaxException ignored) {
				return false;
			}
		};
	}

	public CommandDispatcher<ServerCommandSource> getDispatcher() {
		return dispatcher;
	}

	public static <S> void throwException(ParseResults<S> parse) throws CommandSyntaxException {
		CommandSyntaxException exception = getException(parse);
		if (exception != null) {
			throw exception;
		}
	}

	public static <S> @Nullable CommandSyntaxException getException(ParseResults<S> parse) {
		if (!parse.getReader().canRead()) {
			return null;
		}

		if (parse.getExceptions().size() == 1) {
			return (CommandSyntaxException) parse.getExceptions().values().iterator().next();
		}

		return parse.getContext().getRange().isEmpty()
				? CommandSyntaxException.BUILT_IN_EXCEPTIONS
				  .dispatcherUnknownCommand()
				  .createWithContext(parse.getReader())
				: CommandSyntaxException.BUILT_IN_EXCEPTIONS
				  .dispatcherUnknownArgument()
				  .createWithContext(parse.getReader());
	}

	public static CommandRegistryAccess createRegistryAccess(RegistryWrapper.WrapperLookup registries) {
		return new CommandRegistryAccess() {
			@Override
			public FeatureSet getEnabledFeatures() {
				return FeatureFlags.FEATURE_MANAGER.getFeatureSet();
			}

			@Override
			public Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
				return registries.streamAllRegistryKeys();
			}

			@Override
			public <T> Optional<RegistryWrapper.Impl<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryRef) {
				return registries.getOptional(registryRef).map(this::createTagCreatingLookup);
			}

			private <T> RegistryWrapper.Impl.Delegating<T> createTagCreatingLookup(RegistryWrapper.Impl<T> original) {
				return new RegistryWrapper.Impl.Delegating<T>() {
					@Override
					public RegistryWrapper.Impl<T> getBase() {
						return original;
					}

					@Override
					public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
						return Optional.of(this.getOrThrow(tag));
					}

					@Override
					public RegistryEntryList.Named<T> getOrThrow(TagKey<T> tag) {
						Optional<RegistryEntryList.Named<T>> optional = this.getBase().getOptional(tag);
						return optional.orElseGet(() -> RegistryEntryList.of(this.getBase(), tag));
					}
				};
			}
		};
	}

	/**
	 * Проверяет, что все используемые типы аргументов зарегистрированы в {@link ArgumentTypes}.
	 * Также логирует неоднозначности в дереве команд. Вызывается при старте сервера.
	 */
	public static void checkMissing() {
		CommandRegistryAccess registryAccess = createRegistryAccess(BuiltinRegistries.createWrapperLookup());
		CommandDispatcher<ServerCommandSource> commandDispatcher = new CommandManager(
				CommandManager.RegistrationEnvironment.ALL,
				registryAccess
		).getDispatcher();
		RootCommandNode<ServerCommandSource> root = commandDispatcher.getRoot();

		commandDispatcher.findAmbiguities(
				(parent, child, sibling, inputs) -> LOGGER.warn(
						"Ambiguity between arguments {} and {} with inputs: {}",
						new Object[]{commandDispatcher.getPath(child), commandDispatcher.getPath(sibling), inputs}
				)
		);

		Set<ArgumentType<?>> usedTypes = ArgumentHelper.collectUsedArgumentTypes(root);
		Set<ArgumentType<?>> unregisteredTypes = usedTypes.stream()
				.filter(type -> !ArgumentTypes.has(type.getClass()))
				.collect(Collectors.toSet());

		if (unregisteredTypes.isEmpty()) {
			return;
		}

		LOGGER.warn(
				"Missing type registration for following arguments:\n {}",
				unregisteredTypes.stream().map(type -> "\t" + type).collect(Collectors.joining(",\n"))
		);
		throw new IllegalStateException("Unregistered argument types");
	}

	public static <T extends PermissionSource> PermissionSourcePredicate<T> requirePermissionLevel(PermissionCheck check) {
		return new PermissionSourcePredicate<>(check);
	}

	public static ServerCommandSource createSource(PermissionPredicate permissions) {
		return new ServerCommandSource(
				CommandOutput.DUMMY,
				Vec3d.ZERO,
				Vec2f.ZERO,
				null,
				permissions,
				"",
				ScreenTexts.EMPTY,
				null,
				null
		);
	}

	/**
	 * Функциональный интерфейс для валидации строки команды через Brigadier-парсер.
	 */
	@FunctionalInterface
	public interface CommandParser {

		void parse(StringReader reader) throws CommandSyntaxException;
	}

	public enum RegistrationEnvironment {
		ALL(true, true),
		DEDICATED(false, true),
		INTEGRATED(true, false);

		public final boolean integrated;
		public final boolean dedicated;

		RegistrationEnvironment(boolean integrated, boolean dedicated) {
			this.integrated = integrated;
			this.dedicated = dedicated;
		}
	}
}
