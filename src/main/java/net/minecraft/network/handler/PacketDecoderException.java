package net.minecraft.network.handler;

import io.netty.handler.codec.DecoderException;

public class PacketDecoderException extends DecoderException implements PacketCodecDispatcher.UndecoratedException, PacketException {

	public PacketDecoderException(String message) {
		super(message);
	}

	public PacketDecoderException(Throwable cause) {
		super(cause);
	}
}
