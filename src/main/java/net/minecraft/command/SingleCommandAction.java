package net.minecraft.command;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.context.ContextChain.Stage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Tracer;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

/**
 * Действие, выполняющее одну команду для одного или нескольких источников.
 * Обрабатывает стадии подготовки (модификаторы/редиректы) и финального выполнения.
 *
 * @param <T> тип источника команды
 */
public class SingleCommandAction<T extends AbstractServerCommandSource<T>> {

	@VisibleForTesting
	public static final DynamicCommandExceptionType FORK_LIMIT_EXCEPTION = new DynamicCommandExceptionType(
			count -> Text.stringifiedTranslatable("command.forkLimit", count)
	);

	private final String command;
	private final ContextChain<T> contextChain;

	public SingleCommandAction(String command, ContextChain<T> contextChain) {
		this.command = command;
		this.contextChain = contextChain;
	}

	/**
	 * Выполняет команду: проходит стадии подготовки (модификаторы, редиректы),
	 * затем ставит в очередь финальное выполнение для каждого источника.
	 */
	protected void execute(
			T baseSource,
			List<T> sources,
			CommandExecutionContext<T> context,
			Frame frame,
			ExecutionFlags flags
	) {
		ContextChain<T> chain = contextChain;
		ExecutionFlags currentFlags = flags;
		List<T> currentSources = sources;

		if (chain.getStage() != Stage.EXECUTE) {
			context.getProfiler().push(() -> "prepare " + command);

			try {
				int forkLimit = context.getForkLimit();
				while (chain.getStage() != Stage.EXECUTE) {
					CommandContext<T> commandContext = chain.getTopContext();
					if (commandContext.isForked()) {
						currentFlags = currentFlags.setSilent();
					}

					RedirectModifier<T> redirectModifier = commandContext.getRedirectModifier();
					@SuppressWarnings("unchecked")
					Forkable<T> forkable = (redirectModifier instanceof Forkable) ? (Forkable<T>) redirectModifier : null;

					if (forkable != null) {
						forkable.execute(
								baseSource,
								currentSources,
								chain,
								currentFlags,
								ExecutionControl.of(context, frame)
						);
						return;
					}

					if (redirectModifier != null) {
						context.decrementCommandQuota();
						boolean silent = currentFlags.isSilent();
						List<T> nextSources = new ObjectArrayList<>();

						for (T source : currentSources) {
							try {
								Collection<T> modified = ContextChain.runModifier(
										commandContext,
										source,
										(ctx, successful, returnValue) -> {},
										silent
								);
								if (nextSources.size() + modified.size() >= forkLimit) {
									baseSource.handleException(FORK_LIMIT_EXCEPTION.create(forkLimit), silent, context.getTracer());
									return;
								}

								nextSources.addAll(modified);
							} catch (CommandSyntaxException exception) {
								source.handleException(exception, silent, context.getTracer());
								if (!silent) {
									return;
								}
							}
						}

						currentSources = nextSources;
					}

					chain = chain.nextStage();
				}
			} finally {
				context.getProfiler().pop();
			}
		}

		if (currentSources.isEmpty()) {
			if (currentFlags.isInsideReturnRun()) {
				context.enqueueCommand(new CommandQueueEntry<T>(frame, FallthroughCommandAction.getInstance()));
			}

			return;
		}

		CommandContext<T> execContext = chain.getTopContext();
		@SuppressWarnings("unchecked")
		ControlFlowAware<T> controlFlowAware = (execContext.getCommand() instanceof ControlFlowAware)
				? (ControlFlowAware<T>) execContext.getCommand()
				: null;

		if (controlFlowAware != null) {
			ExecutionControl<T> executionControl = ExecutionControl.of(context, frame);
			for (T source : currentSources) {
				controlFlowAware.execute(source, chain, currentFlags, executionControl);
			}

			return;
		}

		if (currentFlags.isInsideReturnRun()) {
			T firstSource = currentSources.get(0);
			firstSource = firstSource.withReturnValueConsumer(
					ReturnValueConsumer.chain(firstSource.getReturnValueConsumer(), frame.returnValueConsumer())
			);
			currentSources = List.of(firstSource);
		}

		FixedCommandAction<T> fixedAction = new FixedCommandAction<>(command, currentFlags, execContext);
		SteppedCommandAction.enqueueCommands(
				context,
				frame,
				currentSources,
				(framex, source) -> new CommandQueueEntry<>(framex, fixedAction.bind(source))
		);
	}

	protected void traceCommandStart(CommandExecutionContext<T> context, Frame frame) {
		Tracer tracer = context.getTracer();
		if (tracer != null) {
			tracer.traceCommandStart(frame.depth(), command);
		}
	}

	@Override
	public String toString() {
		return command;
	}

	/**
	 * Выполняет команду для нескольких источников с заданными флагами.
	 */
	public static class MultiSource<T extends AbstractServerCommandSource<T>> extends SingleCommandAction<T> implements CommandAction<T> {

		private final ExecutionFlags flags;
		private final T baseSource;
		private final List<T> sources;

		public MultiSource(
				String command,
				ContextChain<T> contextChain,
				ExecutionFlags flags,
				T baseSource,
				List<T> sources
		) {
			super(command, contextChain);
			this.baseSource = baseSource;
			this.sources = sources;
			this.flags = flags;
		}

		@Override
		public void execute(CommandExecutionContext<T> context, Frame frame) {
			execute(baseSource, sources, context, frame, flags);
		}
	}

	/**
	 * Выполняет команду для единственного источника без флагов.
	 */
	public static class SingleSource<T extends AbstractServerCommandSource<T>> extends SingleCommandAction<T> implements CommandAction<T> {

		private final T source;

		public SingleSource(String command, ContextChain<T> contextChain, T source) {
			super(command, contextChain);
			this.source = source;
		}

		@Override
		public void execute(CommandExecutionContext<T> context, Frame frame) {
			traceCommandStart(context, frame);
			execute(source, List.of(source), context, frame, ExecutionFlags.NONE);
		}
	}

	/**
	 * Реализует {@link SourcedCommandAction} — выполняет команду для переданного источника.
	 */
	public static class Sourced<T extends AbstractServerCommandSource<T>> extends SingleCommandAction<T> implements SourcedCommandAction<T> {

		public Sourced(String command, ContextChain<T> contextChain) {
			super(command, contextChain);
		}

		public void execute(T source, CommandExecutionContext<T> context, Frame frame) {
			traceCommandStart(context, frame);
			execute(source, List.of(source), context, frame, ExecutionFlags.NONE);
		}
	}
}
