package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * Интерфейс sequenced packet creator.
 */
public interface SequencedPacketCreator {

	Packet<ServerPlayPacketListener> predict(int sequence);
}
