package net.minecraft.network.handler;

import io.netty.handler.codec.DecoderException;

/**
 * Исключение декодирования пакета, которое не оборачивается дополнительным контекстом
 * в {@link PacketCodecDispatcher} благодаря маркерному интерфейсу {@link PacketCodecDispatcher.UndecoratedException}.
 */
public class PacketDecoderException extends DecoderException implements PacketCodecDispatcher.UndecoratedException, PacketException {

	public PacketDecoderException(String message) {
		super(message);
	}

	public PacketDecoderException(Throwable cause) {
		super(cause);
	}
}
