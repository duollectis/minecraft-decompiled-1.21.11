package net.minecraft.network.handler;

import io.netty.handler.codec.EncoderException;

public class PacketEncoderException extends EncoderException implements PacketCodecDispatcher.UndecoratedException, PacketException {

	public PacketEncoderException(String message) {
		super(message);
	}

	public PacketEncoderException(Throwable cause) {
		super(cause);
	}
}
