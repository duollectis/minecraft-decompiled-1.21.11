package net.minecraft.world.timer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.util.Identifier;

/**
 * {@code FunctionTagTimerCallback}.
 */
public record FunctionTagTimerCallback(Identifier name) implements TimerCallback<MinecraftServer> {

	public static final MapCodec<FunctionTagTimerCallback> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Identifier.CODEC.fieldOf("Name").forGetter(FunctionTagTimerCallback::name))
					.apply(instance, FunctionTagTimerCallback::new)
	);

	/**
	 * Call.
	 *
	 * @param minecraftServer minecraft server
	 * @param timer timer
	 * @param l l
	 */
	public void call(MinecraftServer minecraftServer, Timer<MinecraftServer> timer, long l) {
		CommandFunctionManager commandFunctionManager = minecraftServer.getCommandFunctionManager();

		for (CommandFunction<ServerCommandSource> commandFunction : commandFunctionManager.getTag(this.name)) {
			commandFunctionManager.execute(commandFunction, commandFunctionManager.getScheduledCommandSource());
		}
	}

	@Override
	public MapCodec<FunctionTagTimerCallback> getCodec() {
		return CODEC;
	}
}
