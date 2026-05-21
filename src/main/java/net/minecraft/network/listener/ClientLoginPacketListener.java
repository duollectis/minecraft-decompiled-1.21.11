package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.s2c.login.*;

public interface ClientLoginPacketListener extends ClientCookieRequestPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.LOGIN;
	}

	void onHello(LoginHelloS2CPacket packet);

	void onSuccess(LoginSuccessS2CPacket packet);

	void onDisconnect(LoginDisconnectS2CPacket packet);

	void onCompression(LoginCompressionS2CPacket packet);

	void onQueryRequest(LoginQueryRequestS2CPacket packet);
}
