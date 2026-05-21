package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.s2c.config.*;

/**
 * Интерфейс client configuration packet listener.
 */
public interface ClientConfigurationPacketListener extends ClientCommonPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.CONFIGURATION;
	}

	void onCodeOfConduct(CodeOfConductS2CPacket packet);

	void onReady(ReadyS2CPacket packet);

	void onDynamicRegistries(DynamicRegistriesS2CPacket packet);

	void onFeatures(FeaturesS2CPacket packet);

	void onSelectKnownPacks(SelectKnownPacksS2CPacket packet);

	void onResetChat(ResetChatS2CPacket packet);
}
