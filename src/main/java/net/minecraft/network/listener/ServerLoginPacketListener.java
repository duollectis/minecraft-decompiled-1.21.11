package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;

/**
 * Слушатель серверных пакетов фазы {@link net.minecraft.network.NetworkPhase#LOGIN}.
 * Обрабатывает приветствие клиента, ключ шифрования, ответы на запросы плагинов
 * и переход в фазу конфигурации.
 */
public interface ServerLoginPacketListener extends ServerCookieResponsePacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.LOGIN;
	}

	void onHello(LoginHelloC2SPacket packet);

	void onKey(LoginKeyC2SPacket packet);

	void onQueryResponse(LoginQueryResponseC2SPacket packet);

	void onEnterConfiguration(EnterConfigurationC2SPacket packet);
}
