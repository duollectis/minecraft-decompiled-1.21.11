package net.minecraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Tracer;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для команд, осведомлённых о потоке управления выполнения.
 * Позволяет команде самостоятельно управлять очередью и ветвлением.
 *
 * @param <T> тип источника команды
 */
public interface ControlFlowAware<T> {

	void execute(T source, ContextChain<T> contextChain, ExecutionFlags flags, ExecutionControl<T> control);

	/**
	 * Команда с поддержкой потока управления, реализующая brigadier-контракт.
	 */
	interface Command<T> extends com.mojang.brigadier.Command<T>, ControlFlowAware<T> {

		default int run(CommandContext<T> context) throws CommandSyntaxException {
			throw new UnsupportedOperationException("This function should not run");
		}
	}

	/**
	 * Базовый класс-помощник, перехватывающий {@link CommandSyntaxException}
	 * и делегирующий обработку ошибок источнику команды.
	 */
	abstract class Helper<T extends AbstractServerCommandSource<T>> implements ControlFlowAware<T> {

		public final void execute(
				T source,
				ContextChain<T> contextChain,
				ExecutionFlags flags,
				ExecutionControl<T> control
		) {
			try {
				executeInner(source, contextChain, flags, control);
			} catch (CommandSyntaxException exception) {
				sendError(exception, source, flags, control.getTracer());
				source.getReturnValueConsumer().onFailure();
			}
		}

		protected void sendError(
				CommandSyntaxException exception,
				T source,
				ExecutionFlags flags,
				@Nullable Tracer tracer
		) {
			source.handleException(exception, flags.isSilent(), tracer);
		}

		protected abstract void executeInner(
				T source,
				ContextChain<T> contextChain,
				ExecutionFlags flags,
				ExecutionControl<T> control
		) throws CommandSyntaxException;
	}
}
