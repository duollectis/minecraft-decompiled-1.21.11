package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.packet.Packet;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Netty-обработчик входящего трафика: собирает отдельные пакеты в {@link net.minecraft.network.packet.BundlePacket}
 * между двумя пакетами-сплиттерами.
 */
public class PacketBundler extends MessageToMessageDecoder<Packet<?>> {

	private final PacketBundleHandler handler;
	private PacketBundleHandler.@Nullable Bundler currentBundler;

	public PacketBundler(PacketBundleHandler handler) {
		this.handler = handler;
	}

	@Override
	protected void decode(ChannelHandlerContext context, Packet<?> packet, List<Object> out) throws Exception {
		if (currentBundler != null) {
			ensureNotTransitioning(packet);
			Packet<?> bundled = currentBundler.add(packet);
			if (bundled != null) {
				currentBundler = null;
				out.add(bundled);
			}

			return;
		}

		PacketBundleHandler.Bundler bundler = handler.createBundler(packet);
		if (bundler != null) {
			ensureNotTransitioning(packet);
			currentBundler = bundler;
		} else {
			out.add(packet);
			if (packet.transitionsNetworkState()) {
				context.pipeline().remove(context.name());
			}
		}
	}

	private static void ensureNotTransitioning(Packet<?> packet) {
		if (packet.transitionsNetworkState()) {
			throw new DecoderException("Terminal message received in bundle");
		}
	}
}
