package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.packet.Packet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Фабрика {@link ChannelFutureListener}-колбэков для обработки результатов отправки пакетов.
 * <p>Предоставляет готовые стратегии реакции на успех или провал отправки:
 * безусловное выполнение действия ({@link #always}) и отправку резервного пакета ({@link #of}).
 */
public class PacketCallbacks {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Создаёт колбэк, который всегда выполняет действие, а при ошибке — пробрасывает исключение в пайплайн.
	 *
	 * @param action действие, выполняемое независимо от результата отправки
	 * @return слушатель результата отправки
	 */
	public static ChannelFutureListener always(Runnable action) {
		return future -> {
			action.run();

			if (future.isSuccess() == false) {
				future.channel().pipeline().fireExceptionCaught(future.cause());
			}
		};
	}

	/**
	 * Создаёт колбэк, который при ошибке отправляет резервный пакет или пробрасывает исключение.
	 *
	 * @param fallbackPacket поставщик резервного пакета; может вернуть {@code null}
	 * @return слушатель результата отправки
	 */
	public static ChannelFutureListener of(Supplier<@Nullable Packet<?>> fallbackPacket) {
		return future -> {
			if (future.isSuccess()) {
				return;
			}

			Packet<?> packet = fallbackPacket.get();

			if (packet != null) {
				LOGGER.warn("Failed to deliver packet, sending fallback {}", packet.getPacketType(), future.cause());
				future.channel().writeAndFlush(packet, future.channel().voidPromise());
			}
			else {
				future.channel().pipeline().fireExceptionCaught(future.cause());
			}
		};
	}
}
