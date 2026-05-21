package net.minecraft.client.network;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.handler.LegacyQueries;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Обработчик пинга серверов по устаревшему протоколу Minecraft (до 1.7).
 * <p>Отправляет специальный пакет {@code 0xFE 0x01 0xFA} при активации канала
 * и разбирает ответ сервера, содержащий версию протокола, версию игры,
 * MOTD и количество игроков.
 */
@Environment(EnvType.CLIENT)
public class LegacyServerPinger extends SimpleChannelInboundHandler<ByteBuf> {

	private static final byte PING_PACKET_ID = (byte) 0xFE;
	private static final byte PING_PAYLOAD = (byte) 0x01;
	private static final byte PLUGIN_MESSAGE_ID = (byte) 0xFA;
	private static final byte PROTOCOL_VERSION = (byte) 0x7F;
	private static final short DISCONNECT_PACKET_ID = 0xFF;

	private static final Splitter RESPONSE_SPLITTER = Splitter.on('\u0000').limit(6);

	private final ServerAddress serverAddress;
	private final ResponseHandler handler;

	/**
	 * Создаёт пингер для указанного адреса сервера.
	 *
	 * @param serverAddress адрес сервера для пинга
	 * @param handler       обработчик ответа сервера
	 */
	public LegacyServerPinger(ServerAddress serverAddress, ResponseHandler handler) {
		this.serverAddress = serverAddress;
		this.handler = handler;
	}

	@Override
	public void channelActive(ChannelHandlerContext context) throws Exception {
		super.channelActive(context);
		ByteBuf buf = context.alloc().buffer();

		try {
			buf.writeByte(PING_PACKET_ID);
			buf.writeByte(PING_PAYLOAD);
			buf.writeByte(PLUGIN_MESSAGE_ID);
			LegacyQueries.write(buf, "MC|PingHost");

			int lengthIndex = buf.writerIndex();
			buf.writeShort(0);

			int payloadStart = buf.writerIndex();
			buf.writeByte(PROTOCOL_VERSION);
			LegacyQueries.write(buf, serverAddress.getAddress());
			buf.writeInt(serverAddress.getPort());

			buf.setShort(lengthIndex, buf.writerIndex() - payloadStart);
			context.channel().writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		}
		catch (Exception e) {
			buf.release();
			throw e;
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext context, ByteBuf buf) {
		short packetId = buf.readUnsignedByte();

		if (packetId != DISCONNECT_PACKET_ID) {
			context.close();
			return;
		}

		String response = LegacyQueries.read(buf);
		List<String> parts = RESPONSE_SPLITTER.splitToList(response);

		if ("§1".equals(parts.get(0))) {
			int protocolVersion = MathHelper.parseInt(parts.get(1), 0);
			String version = parts.get(2);
			String label = parts.get(3);
			int currentPlayers = MathHelper.parseInt(parts.get(4), -1);
			int maxPlayers = MathHelper.parseInt(parts.get(5), -1);
			handler.handleResponse(protocolVersion, version, label, currentPlayers, maxPlayers);
		}

		context.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
		context.close();
	}

	/**
	 * Колбэк для обработки ответа сервера на пинг по устаревшему протоколу.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface ResponseHandler {

		/**
		 * Вызывается при получении ответа от сервера.
		 *
		 * @param protocolVersion версия протокола сервера
		 * @param version         строка версии игры
		 * @param label           MOTD сервера
		 * @param currentPlayers  текущее количество игроков
		 * @param maxPlayers      максимальное количество игроков
		 */
		void handleResponse(int protocolVersion, String version, String label, int currentPlayers, int maxPlayers);
	}
}
