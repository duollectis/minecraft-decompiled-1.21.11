package net.minecraft.network.listener;

/**
 * Интерфейс tickable packet listener.
 */
public interface TickablePacketListener extends PacketListener {

	void tick();
}
