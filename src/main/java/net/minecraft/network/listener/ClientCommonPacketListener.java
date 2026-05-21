package net.minecraft.network.listener;

import net.minecraft.network.packet.s2c.common.*;

public interface ClientCommonPacketListener extends ClientCookieRequestPacketListener {

	void onKeepAlive(KeepAliveS2CPacket packet);

	void onPing(CommonPingS2CPacket packet);

	void onCustomPayload(CustomPayloadS2CPacket packet);

	void onDisconnect(DisconnectS2CPacket packet);

	void onResourcePackSend(ResourcePackSendS2CPacket packet);

	void onResourcePackRemove(ResourcePackRemoveS2CPacket packet);

	void onSynchronizeTags(SynchronizeTagsS2CPacket packet);

	void onStoreCookie(StoreCookieS2CPacket packet);

	void onServerTransfer(ServerTransferS2CPacket packet);

	void onCustomReportDetails(CustomReportDetailsS2CPacket packet);

	void onServerLinks(ServerLinksS2CPacket packet);

	void onClearDialog(ClearDialogS2CPacket packet);

	void onShowDialog(ShowDialogS2CPacket packet);
}
