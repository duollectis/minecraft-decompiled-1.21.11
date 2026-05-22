package net.minecraft.world.timer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.util.Identifier;

/**
 * Callback таймера, выполняющий одну функцию по имени при срабатывании.
 * Используется командой {@code /schedule} с именем функции.
 * Если функция не найдена — вызов молча игнорируется.
 */
public record FunctionTimerCallback(Identifier name) implements TimerCallback<MinecraftServer> {

	public static final MapCodec<FunctionTimerCallback> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Identifier.CODEC.fieldOf("Name").forGetter(FunctionTimerCallback::name))
			.apply(instance, FunctionTimerCallback::new)
	);

	@Override
	public void call(MinecraftServer server, Timer<MinecraftServer> timer, long time) {
		CommandFunctionManager manager = server.getCommandFunctionManager();
		manager.getFunction(name).ifPresent(
			function -> manager.execute(
				(CommandFunction<ServerCommandSource>) function,
				manager.getScheduledCommandSource()
			)
		);
	}

	@Override
	public MapCodec<FunctionTimerCallback> getCodec() {
		return CODEC;
	}
}
