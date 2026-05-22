package net.minecraft.command;

import com.google.common.collect.Queues;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Procedure;
import net.minecraft.server.function.Tracer;
import net.minecraft.util.profiler.Profiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Deque;
import java.util.List;

/**
 * Контекст выполнения команд — управляет очередью, лимитами и трассировкой.
 * Реализует {@link AutoCloseable} для корректного закрытия трассировщика.
 *
 * @param <T> тип источника команды
 */
public class CommandExecutionContext<T> implements AutoCloseable {

	private static final int MAX_COMMAND_QUEUE_LENGTH = 10_000_000;
	private static final Logger LOGGER = LogUtils.getLogger();

	private final int maxCommandChainLength;
	private final int forkLimit;
	private final Profiler profiler;
	private @Nullable Tracer tracer;
	private int commandsRemaining;
	private boolean queueOverflowed;
	private final Deque<CommandQueueEntry<T>> commandQueue = Queues.newArrayDeque();
	private final List<CommandQueueEntry<T>> pendingCommands = new ObjectArrayList<>();
	private int currentDepth;

	public CommandExecutionContext(int maxCommandChainLength, int maxCommandForkCount, Profiler profiler) {
		this.maxCommandChainLength = maxCommandChainLength;
		forkLimit = maxCommandForkCount;
		this.profiler = profiler;
		commandsRemaining = maxCommandChainLength;
	}

	/**
	 * Создаёт фрейм для вызова процедуры. На нулевой глубине очередь очищается полностью,
	 * на остальных — только до текущей глубины.
	 */
	private static <T extends AbstractServerCommandSource<T>> Frame frame(
			CommandExecutionContext<T> context,
			ReturnValueConsumer returnValueConsumer
	) {
		if (context.currentDepth == 0) {
			return new Frame(0, returnValueConsumer, context.commandQueue::clear);
		}

		int depth = context.currentDepth + 1;
		return new Frame(depth, returnValueConsumer, context.getEscapeControl(depth));
	}

	public static <T extends AbstractServerCommandSource<T>> void enqueueProcedureCall(
			CommandExecutionContext<T> context,
			Procedure<T> procedure,
			T source,
			ReturnValueConsumer returnValueConsumer
	) {
		context.enqueueCommand(
				new CommandQueueEntry<>(
						frame(context, returnValueConsumer),
						new CommandFunctionAction<>(procedure, source.getReturnValueConsumer(), false).bind(source)
				)
		);
	}

	public static <T extends AbstractServerCommandSource<T>> void enqueueCommand(
			CommandExecutionContext<T> context,
			String command,
			ContextChain<T> contextChain,
			T source,
			ReturnValueConsumer returnValueConsumer
	) {
		context.enqueueCommand(
				new CommandQueueEntry<>(
						frame(context, returnValueConsumer),
						new SingleCommandAction.SingleSource<>(command, contextChain, source)
				)
		);
	}

	private void markQueueOverflowed() {
		queueOverflowed = true;
		pendingCommands.clear();
		commandQueue.clear();
	}

	public void enqueueCommand(CommandQueueEntry<T> entry) {
		if (pendingCommands.size() + commandQueue.size() > MAX_COMMAND_QUEUE_LENGTH) {
			markQueueOverflowed();
		}

		if (!queueOverflowed) {
			pendingCommands.add(entry);
		}
	}

	public void escape(int depth) {
		while (!commandQueue.isEmpty() && commandQueue.peek().frame().depth() >= depth) {
			commandQueue.removeFirst();
		}
	}

	public Frame.Control getEscapeControl(int depth) {
		return () -> escape(depth);
	}

	/**
	 * Запускает основной цикл выполнения очереди команд до исчерпания лимита
	 * или опустошения очереди.
	 */
	public void run() {
		queuePendingCommands();

		while (true) {
			if (commandsRemaining <= 0) {
				LOGGER.info("Command execution stopped due to limit (executed {} commands)", maxCommandChainLength);
				break;
			}

			CommandQueueEntry<T> entry = commandQueue.pollFirst();
			if (entry == null) {
				return;
			}

			currentDepth = entry.frame().depth();
			entry.execute(this);

			if (queueOverflowed) {
				LOGGER.error("Command execution stopped due to command queue overflow (max {})", MAX_COMMAND_QUEUE_LENGTH);
				break;
			}

			queuePendingCommands();
		}

		currentDepth = 0;
	}

	private void queuePendingCommands() {
		for (int index = pendingCommands.size() - 1; index >= 0; index--) {
			commandQueue.addFirst(pendingCommands.get(index));
		}

		pendingCommands.clear();
	}

	public void setTracer(@Nullable Tracer tracer) {
		this.tracer = tracer;
	}

	public @Nullable Tracer getTracer() {
		return tracer;
	}

	public Profiler getProfiler() {
		return profiler;
	}

	public int getForkLimit() {
		return forkLimit;
	}

	public void decrementCommandQuota() {
		commandsRemaining--;
	}

	@Override
	public void close() {
		if (tracer != null) {
			tracer.close();
		}
	}
}
