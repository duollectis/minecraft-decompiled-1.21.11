package net.minecraft.network.message;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
/**
 * Интерфейс message decorator.
 */
public interface MessageDecorator {

	MessageDecorator NOOP = (sender, message) -> message;

	Text decorate(@Nullable ServerPlayerEntity sender, Text message);
}
