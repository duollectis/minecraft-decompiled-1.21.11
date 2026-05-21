package net.minecraft.world.timer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.function.Function;

/**
 * {@code TimerCallbackSerializer}.
 */
public class TimerCallbackSerializer<C> {

	public static final TimerCallbackSerializer<MinecraftServer>
			INSTANCE =
			new TimerCallbackSerializer<MinecraftServer>()
					.registerSerializer(Identifier.ofVanilla("function"), FunctionTimerCallback.CODEC)
					.registerSerializer(Identifier.ofVanilla("function_tag"), FunctionTagTimerCallback.CODEC);
	private final Codecs.IdMapper<Identifier, MapCodec<? extends TimerCallback<C>>> idMapper = new Codecs.IdMapper<>();
	private final Codec<TimerCallback<C>>
			codec =
			this.idMapper.getCodec(Identifier.CODEC).dispatch("Type", TimerCallback::getCodec, Function.identity());

	/**
	 * Регистрирует serializer.
	 *
	 * @param id id
	 * @param codec codec
	 *
	 * @return TimerCallbackSerializer — результат операции
	 */
	public TimerCallbackSerializer<C> registerSerializer(Identifier id, MapCodec<? extends TimerCallback<C>> codec) {
		this.idMapper.put(id, codec);
		return this;
	}

	public Codec<TimerCallback<C>> getCodec() {
		return this.codec;
	}
}
