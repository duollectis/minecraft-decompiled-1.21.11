package net.minecraft.world.timer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.function.Function;

/**
 * Реестр сериализаторов {@link TimerCallback}, позволяющий кодировать и декодировать
 * callback-ы по их идентификатору типа.
 * Использует диспетчерный codec с полем {@code "Type"} для определения конкретного типа.
 *
 * @param <C> тип сервера, передаваемого в callback
 */
public class TimerCallbackSerializer<C> {

	/**
	 * Стандартный сериализатор для серверных callback-ов Minecraft.
	 * Регистрирует {@link FunctionTimerCallback} и {@link FunctionTagTimerCallback}.
	 */
	public static final TimerCallbackSerializer<MinecraftServer> INSTANCE =
		new TimerCallbackSerializer<MinecraftServer>()
			.registerSerializer(Identifier.ofVanilla("function"), FunctionTimerCallback.CODEC)
			.registerSerializer(Identifier.ofVanilla("function_tag"), FunctionTagTimerCallback.CODEC);

	private final Codecs.IdMapper<Identifier, MapCodec<? extends TimerCallback<C>>> idMapper = new Codecs.IdMapper<>();
	private final Codec<TimerCallback<C>> codec =
		idMapper.getCodec(Identifier.CODEC).dispatch("Type", TimerCallback::getCodec, Function.identity());

	/**
	 * Регистрирует новый тип callback-а по идентификатору.
	 *
	 * @param id    идентификатор типа (используется в поле {@code "Type"} при сериализации)
	 * @param codec codec для данного типа callback-а
	 * @return {@code this} для цепочки вызовов
	 */
	public TimerCallbackSerializer<C> registerSerializer(Identifier id, MapCodec<? extends TimerCallback<C>> codec) {
		idMapper.put(id, codec);
		return this;
	}

	public Codec<TimerCallback<C>> getCodec() {
		return codec;
	}
}
