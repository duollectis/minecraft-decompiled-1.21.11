package net.minecraft.network.packet;

import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.s2c.common.*;
import net.minecraft.util.Identifier;

/**
 * Класс common packets.
 */
public class CommonPackets {

	public static final PacketType<ClearDialogS2CPacket> CLEAR_DIALOG = s2c("clear_dialog");
	public static final PacketType<CustomPayloadS2CPacket> CUSTOM_PAYLOAD_S2C = s2c("custom_payload");
	public static final PacketType<CustomReportDetailsS2CPacket> CUSTOM_REPORT_DETAILS = s2c("custom_report_details");
	public static final PacketType<DisconnectS2CPacket> DISCONNECT = s2c("disconnect");
	public static final PacketType<KeepAliveS2CPacket> KEEP_ALIVE_S2C = s2c("keep_alive");
	public static final PacketType<CommonPingS2CPacket> PING = s2c("ping");
	public static final PacketType<ResourcePackRemoveS2CPacket> RESOURCE_PACK_POP = s2c("resource_pack_pop");
	public static final PacketType<ResourcePackSendS2CPacket> RESOURCE_PACK_PUSH = s2c("resource_pack_push");
	public static final PacketType<ServerLinksS2CPacket> SERVER_LINKS = s2c("server_links");
	public static final PacketType<ShowDialogS2CPacket> SHOW_DIALOG = s2c("show_dialog");
	public static final PacketType<StoreCookieS2CPacket> STORE_COOKIE = s2c("store_cookie");
	public static final PacketType<ServerTransferS2CPacket> TRANSFER = s2c("transfer");
	public static final PacketType<SynchronizeTagsS2CPacket> UPDATE_TAGS = s2c("update_tags");
	public static final PacketType<ClientOptionsC2SPacket> CLIENT_INFORMATION = c2s("client_information");
	public static final PacketType<CustomPayloadC2SPacket> CUSTOM_PAYLOAD_C2S = c2s("custom_payload");
	public static final PacketType<KeepAliveC2SPacket> KEEP_ALIVE_C2S = c2s("keep_alive");
	public static final PacketType<CommonPongC2SPacket> PONG = c2s("pong");
	public static final PacketType<ResourcePackStatusC2SPacket> RESOURCE_PACK = c2s("resource_pack");
	public static final PacketType<CustomClickActionC2SPacket> CUSTOM_CLICK_ACTION = c2s("custom_click_action");

	private static <T extends Packet<ClientCommonPacketListener>> PacketType<T> s2c(String id) {
		return new PacketType<>(NetworkSide.CLIENTBOUND, Identifier.ofVanilla(id));
	}

	private static <T extends Packet<ServerCommonPacketListener>> PacketType<T> c2s(String id) {
		return new PacketType<>(NetworkSide.SERVERBOUND, Identifier.ofVanilla(id));
	}
}
