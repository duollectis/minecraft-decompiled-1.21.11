package net.minecraft.network.handler;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.QueryableServer;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.Locale;

/**
 * Netty-обработчик legacy-запросов пинга (протоколы Minecraft до 1.7).
 * Отвечает на пакет {@code 0xFE} строкой с информацией о сервере и закрывает соединение.
 */
public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int RESPONSE_PACKET_ID = 255;
	private static final int MIN_PROTOCOL_VERSION = 73;
	private static final int MAX_PORT = 65535;

	private final QueryableServer server;

	public LegacyQueryHandler(QueryableServer server) {
		this.server = server;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf buf = (ByteBuf) msg;
		buf.markReaderIndex();
		boolean shouldPassThrough = true;

		try {
			try {
				if (buf.readUnsignedByte() != LegacyQueries.QUERY_PACKET_ID) {
					return;
				}

				SocketAddress remoteAddress = ctx.channel().remoteAddress();
				int remaining = buf.readableBytes();

				if (remaining == 0) {
					LOGGER.debug("Ping: (<1.3.x) from {}", remoteAddress);
					reply(ctx, createBuf(ctx.alloc(), getResponseFor1_2(server)));
				} else {
					if (buf.readUnsignedByte() != LegacyQueries.LEGACY_PING_BYTE) {
						return;
					}

					if (buf.isReadable()) {
						if (!isLegacyQuery(buf)) {
							return;
						}

						LOGGER.debug("Ping: (1.6) from {}", remoteAddress);
					} else {
						LOGGER.debug("Ping: (1.4-1.5.x) from {}", remoteAddress);
					}

					reply(ctx, createBuf(ctx.alloc(), getResponse(server)));
				}

				buf.release();
				shouldPassThrough = false;
			} catch (RuntimeException ignored) {
			}
		} finally {
			if (shouldPassThrough) {
				buf.resetReaderIndex();
				ctx.channel().pipeline().remove(this);
				ctx.fireChannelRead(msg);
			}
		}
	}

	private static boolean isLegacyQuery(ByteBuf buf) {
		short header = buf.readUnsignedByte();
		if (header != LegacyQueries.HEADER) {
			return false;
		}

		String channel = LegacyQueries.read(buf);
		if (!LegacyQueries.PING_HOST.equals(channel)) {
			return false;
		}

		int payloadLength = buf.readUnsignedShort();
		if (buf.readableBytes() != payloadLength) {
			return false;
		}

		short protocolVersion = buf.readUnsignedByte();
		if (protocolVersion < MIN_PROTOCOL_VERSION) {
			return false;
		}

		LegacyQueries.read(buf);
		int port = buf.readInt();
		return port <= MAX_PORT;
	}

	private static String getResponseFor1_2(QueryableServer server) {
		return String.format(
				Locale.ROOT,
				"%s§%d§%d",
				server.getServerMotd(),
				server.getCurrentPlayerCount(),
				server.getMaxPlayerCount()
		);
	}

	private static String getResponse(QueryableServer server) {
		return String.format(
				Locale.ROOT,
				"§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
				LegacyQueries.PROTOCOL_VERSION,
				server.getVersion(),
				server.getServerMotd(),
				server.getCurrentPlayerCount(),
				server.getMaxPlayerCount()
		);
	}

	private static void reply(ChannelHandlerContext context, ByteBuf buf) {
		context.pipeline().firstContext().writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE);
	}

	private static ByteBuf createBuf(ByteBufAllocator allocator, String response) {
		ByteBuf buf = allocator.buffer();
		buf.writeByte(RESPONSE_PACKET_ID);
		LegacyQueries.write(buf, response);
		return buf;
	}
}
