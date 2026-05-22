package net.minecraft.network.message;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Контейнер подписанных аргументов команды.
 * Позволяет получить подписанное сообщение по имени аргумента.
 */
public interface SignedCommandArguments {

	SignedCommandArguments EMPTY = argumentName -> null;

	@Nullable SignedMessage getMessage(String argumentName);

	/**
	 * Реализация на основе карты имя→сообщение.
	 */
	record Impl(Map<String, SignedMessage> arguments) implements SignedCommandArguments {

		@Override
		public @Nullable SignedMessage getMessage(String argumentName) {
			return arguments.get(argumentName);
		}
	}
}
