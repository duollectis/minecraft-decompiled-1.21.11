package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.command.permission.PermissionSource;
import net.minecraft.server.function.Tracer;
import org.jspecify.annotations.Nullable;

/**
 * Базовый контракт источника серверной команды: управление потребителем возвращаемого значения,
 * диспетчером команд и обработкой исключений. Реализуется {@link ServerCommandSource}.
 */
public interface AbstractServerCommandSource<T extends AbstractServerCommandSource<T>> extends PermissionSource {

	T withReturnValueConsumer(ReturnValueConsumer returnValueConsumer);

	ReturnValueConsumer getReturnValueConsumer();

	default T withDummyReturnValueConsumer() {
		return this.withReturnValueConsumer(ReturnValueConsumer.EMPTY);
	}

	CommandDispatcher<T> getDispatcher();

	void handleException(CommandExceptionType type, Message message, boolean silent, @Nullable Tracer tracer);

	boolean isSilent();

	default void handleException(CommandSyntaxException exception, boolean silent, @Nullable Tracer tracer) {
		this.handleException(exception.getType(), exception.getRawMessage(), silent, tracer);
	}

	static <T extends AbstractServerCommandSource<T>> ResultConsumer<T> asResultConsumer() {
		return (context, success, result) -> ((AbstractServerCommandSource) context.getSource())
				.getReturnValueConsumer()
				.onResult(success, result);
	}
}
