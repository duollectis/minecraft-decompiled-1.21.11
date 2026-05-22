package net.minecraft.network.handler;

import io.netty.handler.codec.EncoderException;

/**
 * Исключение кодирования пакета, которое не оборачивается дополнительным контекстом
 * в {@link PacketCodecDispatcher} благодаря маркерному интерфейсу {@link PacketCodecDispatcher.UndecoratedException}.
 */
public class PacketEncoderException extends EncoderException implements PacketCodecDispatcher.UndecoratedException, PacketException {

	public PacketEncoderException(String message) {
		super(message);
	}

	public PacketEncoderException(Throwable cause) {
		super(cause);
	}
}
