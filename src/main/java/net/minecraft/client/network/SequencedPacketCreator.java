package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

/**
 * Фабрика пакетов с порядковым номером для оптимистичных обновлений блоков.
 * <p>Используется в {@link ClientPlayerInteractionManager#sendSequencedPacket} —
 * порядковый номер позволяет серверу подтвердить или откатить клиентское изменение.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface SequencedPacketCreator {

	/**
	 * Создаёт пакет с заданным порядковым номером транзакции.
	 *
	 * @param sequence порядковый номер для синхронизации с сервером
	 * @return пакет для отправки
	 */
	Packet<ServerPlayPacketListener> predict(int sequence);
}
