package net.minecraft.server.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Управляет загрузкой и выполнением mcfunction-функций на сервере.
 * Обрабатывает теги {@code #minecraft:tick} (каждый тик) и {@code #minecraft:load} (при загрузке датапаков).
 */
public class CommandFunctionManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier TICK_TAG_ID = Identifier.ofVanilla("tick");
	private static final Identifier LOAD_TAG_ID = Identifier.ofVanilla("load");

	private final MinecraftServer server;
	private List<CommandFunction<ServerCommandSource>> tickFunctions = ImmutableList.of();
	private boolean justLoaded;
	private FunctionLoader loader;

	public CommandFunctionManager(MinecraftServer server, FunctionLoader loader) {
		this.server = server;
		this.loader = loader;
		load(loader);
	}

	public CommandDispatcher<ServerCommandSource> getDispatcher() {
		return server.getCommandManager().getDispatcher();
	}

	public void tick() {
		if (server.getTickManager().shouldTick()) {
			if (justLoaded) {
				justLoaded = false;
				Collection<CommandFunction<ServerCommandSource>> loadFunctions = loader.getTagOrEmpty(LOAD_TAG_ID);
				executeAll(loadFunctions, LOAD_TAG_ID);
			}

			executeAll(tickFunctions, TICK_TAG_ID);
		}
	}

	private void executeAll(Collection<CommandFunction<ServerCommandSource>> functions, Identifier label) {
		Profilers.get().push(label::toString);

		for (CommandFunction<ServerCommandSource> function : functions) {
			execute(function, getScheduledCommandSource());
		}

		Profilers.get().pop();
	}

	public void execute(CommandFunction<ServerCommandSource> function, ServerCommandSource source) {
		Profiler profiler = Profilers.get();
		profiler.push(() -> "function " + function.id());

		try {
			Procedure<ServerCommandSource> procedure = function.withMacroReplaced(null, getDispatcher());
			CommandManager.callWithContext(
					source,
					context -> CommandExecutionContext.enqueueProcedureCall(
							context,
							procedure,
							source,
							ReturnValueConsumer.EMPTY
					)
			);
		}
		catch (MacroException ignored) {
			// Функция требует аргументы макроса, но вызвана без них — молча пропускаем
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to execute function {}", function.id(), exception);
		}
		finally {
			profiler.pop();
		}
	}

	public void setFunctions(FunctionLoader loader) {
		this.loader = loader;
		load(loader);
	}

	private void load(FunctionLoader loader) {
		tickFunctions = List.copyOf(loader.getTagOrEmpty(TICK_TAG_ID));
		justLoaded = true;
	}

	public ServerCommandSource getScheduledCommandSource() {
		return server.getCommandSource().withPermissions(LeveledPermissionPredicate.GAMEMASTERS).withSilent();
	}

	public Optional<CommandFunction<ServerCommandSource>> getFunction(Identifier id) {
		return loader.get(id);
	}

	public List<CommandFunction<ServerCommandSource>> getTag(Identifier id) {
		return loader.getTagOrEmpty(id);
	}

	public Iterable<Identifier> getAllFunctions() {
		return loader.getFunctions().keySet();
	}

	public Iterable<Identifier> getFunctionTags() {
		return loader.getTags();
	}
}
