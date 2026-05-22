package net.minecraft.server.command;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.command.*;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.function.MacroException;
import net.minecraft.server.function.Procedure;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Команда {@code /function}: выполнение mcfunction-файла.
 */
public class FunctionCommand {

	private static final DynamicCommandExceptionType ARGUMENT_NOT_COMPOUND_EXCEPTION = new DynamicCommandExceptionType(
			argument -> Text.stringifiedTranslatable("commands.function.error.argument_not_compound", argument)
	);
	static final DynamicCommandExceptionType NO_FUNCTIONS_EXCEPTION = new DynamicCommandExceptionType(
			argument -> Text.stringifiedTranslatable("commands.function.scheduled.no_functions", argument)
	);
	@VisibleForTesting
	public static final Dynamic2CommandExceptionType INSTANTIATION_FAILURE_EXCEPTION = new Dynamic2CommandExceptionType(
			(argument, argument2) -> Text.stringifiedTranslatable(
					"commands.function.instantiationFailure",
					argument,
					argument2
			)
	);
	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		CommandFunctionManager
				commandFunctionManager =
				((ServerCommandSource) context.getSource()).getServer().getCommandFunctionManager();
		CommandSource.suggestIdentifiers(commandFunctionManager.getFunctionTags(), builder, "#");
		return CommandSource.suggestIdentifiers(commandFunctionManager.getAllFunctions(), builder);
	};
	static final FunctionCommand.ResultConsumer<ServerCommandSource>
			RESULT_REPORTER =
			new FunctionCommand.ResultConsumer<ServerCommandSource>() {
				/**
				 * Accept.
				 *
				 * @param serverCommandSource server command source
				 * @param identifier identifier
				 * @param i i
				 */
				public void accept(ServerCommandSource serverCommandSource, Identifier identifier, int i) {
					serverCommandSource.sendFeedback(
							() -> Text.translatable(
									"commands.function.result",
									Text.of(identifier),
									i
							), true
					);
				}
			};

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("with");

		for (DataCommand.ObjectType objectType : DataCommand.SOURCE_OBJECT_TYPES) {
			objectType.addArgumentsToBuilder(
					literalArgumentBuilder, builder -> builder
							.executes(new FunctionCommand.Command() {
								@Override
								protected NbtCompound getArguments(CommandContext<ServerCommandSource> context)
								throws CommandSyntaxException {
									return objectType.getObject(context).getNbt();
								}
							})
							.then(CommandManager
									.argument("path", NbtPathArgumentType.nbtPath())
									.executes(new FunctionCommand.Command() {
										@Override
										protected NbtCompound getArguments(CommandContext<ServerCommandSource> context)
										throws CommandSyntaxException {
											return FunctionCommand.getArgument(
													NbtPathArgumentType.getNbtPath(
															context,
															"path"
													), objectType.getObject(context)
											);
										}
									}))
			);
		}

		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("function")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.GAMEMASTERS_CHECK))
				)
						.then(
								((RequiredArgumentBuilder) ((RequiredArgumentBuilder) CommandManager
										.argument("name", CommandFunctionArgumentType.commandFunction())
										.suggests(SUGGESTION_PROVIDER)
										.executes(new FunctionCommand.Command() {
											@Override
											protected @Nullable NbtCompound getArguments(CommandContext<ServerCommandSource> context) {
												return null;
											}
										})
								)
										.then(CommandManager
												.argument("arguments", NbtCompoundArgumentType.nbtCompound())
												.executes(new FunctionCommand.Command() {
													@Override
													protected NbtCompound getArguments(CommandContext<ServerCommandSource> context) {
														return NbtCompoundArgumentType.getNbtCompound(
																context,
																"arguments"
														);
													}
												}))
								)
										.then(literalArgumentBuilder)
						)
		);
	}

	static NbtCompound getArgument(NbtPathArgumentType.NbtPath path, DataCommandObject object)
	throws CommandSyntaxException {
		NbtElement nbtElement = DataCommand.getNbt(path, object);
		if (nbtElement instanceof NbtCompound nbtCompound) {
			return nbtCompound;
		}
		else {
			throw ARGUMENT_NOT_COMPOUND_EXCEPTION.create(nbtElement.getNbtType().getCrashReportName());
		}
	}

	/**
	 * Создаёт function command source.
	 *
	 * @param source source
	 *
	 * @return ServerCommandSource — результат операции
	 */
	public static ServerCommandSource createFunctionCommandSource(ServerCommandSource source) {
		return source.withSilent().withAdditionalPermissions(LeveledPermissionPredicate.GAMEMASTERS);
	}

	public static <T extends AbstractServerCommandSource<T>> void enqueueAction(
			Collection<CommandFunction<T>> commandFunctions,
			@Nullable NbtCompound args,
			T parentSource,
			T functionSource,
			ExecutionControl<T> control,
			FunctionCommand.ResultConsumer<T> resultConsumer,
			ExecutionFlags flags
	) throws CommandSyntaxException {
		if (flags.isInsideReturnRun()) {
			enqueueInReturnRun(commandFunctions, args, parentSource, functionSource, control, resultConsumer);
		}
		else {
			enqueueOutsideReturnRun(commandFunctions, args, parentSource, functionSource, control, resultConsumer);
		}
	}

	private static <T extends AbstractServerCommandSource<T>> void enqueueFunction(
			@Nullable NbtCompound args,
			ExecutionControl<T> control,
			CommandDispatcher<T> dispatcher,
			T source,
			CommandFunction<T> function,
			Identifier id,
			ReturnValueConsumer returnValueConsumer,
			boolean propagateReturn
	) throws CommandSyntaxException {
		try {
			Procedure<T> procedure = function.withMacroReplaced(args, dispatcher);
			control.enqueueAction(new CommandFunctionAction<>(procedure, returnValueConsumer, propagateReturn).bind(
					source));
		}
		catch (MacroException var9) {
			throw INSTANTIATION_FAILURE_EXCEPTION.create(id, var9.getMessage());
		}
	}

	private static <T extends AbstractServerCommandSource<T>> ReturnValueConsumer wrapReturnValueConsumer(
			T flags, FunctionCommand.ResultConsumer<T> resultConsumer, Identifier id, ReturnValueConsumer wrapped
	) {
		return flags.isSilent() ? wrapped : (successful, returnValue) -> {
			resultConsumer.accept(flags, id, returnValue);
			wrapped.onResult(successful, returnValue);
		};
	}

	private static <T extends AbstractServerCommandSource<T>> void enqueueInReturnRun(
			Collection<CommandFunction<T>> functions,
			@Nullable NbtCompound args,
			T parentSource,
			T functionSource,
			ExecutionControl<T> control,
			FunctionCommand.ResultConsumer<T> resultConsumer
	) throws CommandSyntaxException {
		CommandDispatcher<T> commandDispatcher = parentSource.getDispatcher();
		T abstractServerCommandSource = functionSource.withDummyReturnValueConsumer();
		ReturnValueConsumer
				returnValueConsumer =
				ReturnValueConsumer.chain(
						parentSource.getReturnValueConsumer(),
						control.getFrame().returnValueConsumer()
				);

		for (CommandFunction<T> commandFunction : functions) {
			Identifier identifier = commandFunction.id();
			ReturnValueConsumer
					returnValueConsumer2 =
					wrapReturnValueConsumer(parentSource, resultConsumer, identifier, returnValueConsumer);
			enqueueFunction(
					args,
					control,
					commandDispatcher,
					abstractServerCommandSource,
					commandFunction,
					identifier,
					returnValueConsumer2,
					true
			);
		}

		control.enqueueAction(FallthroughCommandAction.getInstance());
	}

	private static <T extends AbstractServerCommandSource<T>> void enqueueOutsideReturnRun(
			Collection<CommandFunction<T>> functions,
			@Nullable NbtCompound args,
			T parentSource,
			T functionSource,
			ExecutionControl<T> control,
			FunctionCommand.ResultConsumer<T> resultConsumer
	) throws CommandSyntaxException {
		CommandDispatcher<T> commandDispatcher = parentSource.getDispatcher();
		T abstractServerCommandSource = functionSource.withDummyReturnValueConsumer();
		ReturnValueConsumer returnValueConsumer = parentSource.getReturnValueConsumer();
		if (!functions.isEmpty()) {
			if (functions.size() == 1) {
				CommandFunction<T> commandFunction = functions.iterator().next();
				Identifier identifier = commandFunction.id();
				ReturnValueConsumer
						returnValueConsumer2 =
						wrapReturnValueConsumer(parentSource, resultConsumer, identifier, returnValueConsumer);
				enqueueFunction(
						args,
						control,
						commandDispatcher,
						abstractServerCommandSource,
						commandFunction,
						identifier,
						returnValueConsumer2,
						false
				);
			}
			else if (returnValueConsumer == ReturnValueConsumer.EMPTY) {
				for (CommandFunction<T> commandFunction2 : functions) {
					Identifier identifier2 = commandFunction2.id();
					ReturnValueConsumer
							returnValueConsumer3 =
							wrapReturnValueConsumer(parentSource, resultConsumer, identifier2, returnValueConsumer);
					enqueueFunction(
							args,
							control,
							commandDispatcher,
							abstractServerCommandSource,
							commandFunction2,
							identifier2,
							returnValueConsumer3,
							false
					);
				}
			}
			else {
				/**
				 * {@code ReturnValueAdder}.
				 */
				class ReturnValueAdder {

					boolean successful;
					int returnValue;

					/**
					 * Обрабатывает событие success.
					 *
					 * @param returnValue return value
					 */
					public void onSuccess(int returnValue) {
						this.successful = true;
						this.returnValue += returnValue;
					}
				}

				ReturnValueAdder returnValueAdder = new ReturnValueAdder();
				ReturnValueConsumer
						returnValueConsumer4 =
						(successful, returnValue) -> returnValueAdder.onSuccess(returnValue);

				for (CommandFunction<T> commandFunction3 : functions) {
					Identifier identifier3 = commandFunction3.id();
					ReturnValueConsumer
							returnValueConsumer5 =
							wrapReturnValueConsumer(parentSource, resultConsumer, identifier3, returnValueConsumer4);
					enqueueFunction(
							args,
							control,
							commandDispatcher,
							abstractServerCommandSource,
							commandFunction3,
							identifier3,
							returnValueConsumer5,
							false
					);
				}

				control.enqueueAction((context, frame) -> {
					if (returnValueAdder.successful) {
						returnValueConsumer.onSuccess(returnValueAdder.returnValue);
					}
				});
			}
		}
	}

	abstract static class Command extends ControlFlowAware.Helper<ServerCommandSource> implements ControlFlowAware.Command<ServerCommandSource> {

		protected abstract @Nullable NbtCompound getArguments(CommandContext<ServerCommandSource> context)
		throws CommandSyntaxException;

		public void executeInner(
				ServerCommandSource serverCommandSource,
				ContextChain<ServerCommandSource> contextChain,
				ExecutionFlags executionFlags,
				ExecutionControl<ServerCommandSource> executionControl
		) throws CommandSyntaxException {
			CommandContext<ServerCommandSource>
					commandContext =
					contextChain.getTopContext().copyFor(serverCommandSource);
			Pair<Identifier, Collection<CommandFunction<ServerCommandSource>>>
					pair =
					CommandFunctionArgumentType.getIdentifiedFunctions(commandContext, "name");
			Collection<CommandFunction<ServerCommandSource>>
					collection =
					(Collection<CommandFunction<ServerCommandSource>>) pair.getSecond();
			if (collection.isEmpty()) {
				throw FunctionCommand.NO_FUNCTIONS_EXCEPTION.create(Text.of((Identifier) pair.getFirst()));
			}
			else {
				NbtCompound nbtCompound = this.getArguments(commandContext);
				ServerCommandSource
						serverCommandSource2 =
						FunctionCommand.createFunctionCommandSource(serverCommandSource);
				if (collection.size() == 1) {
					serverCommandSource.sendFeedback(
							() -> Text.translatable(
									"commands.function.scheduled.single",
									Text.of(collection.iterator().next().id())
							), true
					);
				}
				else {
					serverCommandSource.sendFeedback(
							() -> Text.translatable(
									"commands.function.scheduled.multiple",
									Texts.join(collection.stream().map(CommandFunction::id).toList(), Text::of)
							),
							true
					);
				}

				FunctionCommand.enqueueAction(
						collection,
						nbtCompound,
						serverCommandSource,
						serverCommandSource2,
						executionControl,
						FunctionCommand.RESULT_REPORTER,
						executionFlags
				);
			}
		}
	}

	public interface ResultConsumer<T> {

		void accept(T source, Identifier id, int result);
	}
}
