package net.minecraft.server.network;

import net.minecraft.network.packet.Packet;

import java.util.function.Consumer;

/**
 * {@code ServerPlayerConfigurationTask}.
 */
public interface ServerPlayerConfigurationTask {

	void sendPacket(Consumer<Packet<?>> sender);

	default boolean hasFinished() {
		return false;
	}

	ServerPlayerConfigurationTask.Key getKey();

	/**
	 * {@code Key}.
	 */
	public record Key(String id) {

		@Override
		public String toString() {
			return this.id;
		}
	}
}
