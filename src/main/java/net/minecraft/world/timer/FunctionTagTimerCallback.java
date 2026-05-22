package net.minecraft.world.timer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.util.Identifier;

/**
 * Callback таймера, выполняющий все функции из тега функций при срабатывании.
 * Используется командой {@code /schedule} с тегом функций.
 */
public record FunctionTagTimerCallback(Identifier name) implements TimerCallback<MinecraftServer> {

	public static final MapCodec<FunctionTagTimerCallback> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Identifier.CODEC.fieldOf("Name").forGetter(FunctionTagTimerCallback::name))
			.apply(instance, FunctionTagTimerCallback::new)
	);

	@Override
	public void call(MinecraftServer server, Timer<MinecraftServer> timer, long time) {
		CommandFunctionManager manager = server.getCommandFunctionManager();

		for (CommandFunction<ServerCommandSource> function : manager.getTag(name)) {
			manager.execute(function, manager.getScheduledCommandSource());
		}
	}

	@Override
	public MapCodec<FunctionTagTimerCallback> getCodec() {
		return CODEC;
	}
}
