package net.minecraft.network.listener;

import com.mojang.logging.LogUtils;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.crash.CrashException;
import org.slf4j.Logger;

/**
 * Расширение {@link ServerPacketListener}, которое перехватывает исключения при обработке пакетов
 * и логирует их вместо краша сервера. Используется для всех серверных слушателей,
 * где недопустимо прерывать работу сервера из-за некорректного пакета клиента.
 */
public interface ServerCrashSafePacketListener extends ServerPacketListener {

	Logger LOGGER = LogUtils.getLogger();

	@Override
	default void onPacketException(Packet packet, Exception exception) throws CrashException {
		LOGGER.error("Failed to handle packet {}, suppressing error", packet, exception);
	}
}
