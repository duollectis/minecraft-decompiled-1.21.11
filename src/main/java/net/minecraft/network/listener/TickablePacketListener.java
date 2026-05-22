package net.minecraft.network.listener;

/**
 * Расширение {@link PacketListener} для слушателей, которым требуется периодическое обновление
 * в игровом тике (например, обработка очереди входящих пакетов на главном потоке).
 */
public interface TickablePacketListener extends PacketListener {

	void tick();
}
