package net.minecraft.world.timer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.util.Identifier;

/**
 * {@code FunctionTimerCallback}.
 */
public record FunctionTimerCallback(Identifier name) implements TimerCallback<MinecraftServer> {

	public static final MapCodec<FunctionTimerCallback> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Identifier.CODEC.fieldOf("Name").forGetter(FunctionTimerCallback::name))
					.apply(instance, FunctionTimerCallback::new)
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
		commandFunctionManager.getFunction(this.name)
		                      .ifPresent(
				                      function -> commandFunctionManager.execute(
						                      (CommandFunction<ServerCommandSource>) function,
						                      commandFunctionManager.getScheduledCommandSource()
				                      )
		                      );
	}

	@Override
	public MapCodec<FunctionTimerCallback> getCodec() {
		return CODEC;
	}
}
