package net.minecraft.network.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

/**
 * Управляет потоковым шифрованием/дешифрованием пакетов через {@link Cipher} (AES/CFB8).
 * Переиспользует внутренние буферы для минимизации аллокаций.
 */
public class PacketEncryptionManager {

	private final Cipher cipher;
	private byte[] conversionBuffer = new byte[0];
	private byte[] encryptionBuffer = new byte[0];

	protected PacketEncryptionManager(Cipher cipher) {
		this.cipher = cipher;
	}

	private byte[] toByteArray(ByteBuf buf) {
		int length = buf.readableBytes();
		if (conversionBuffer.length < length) {
			conversionBuffer = new byte[length];
		}

		buf.readBytes(conversionBuffer, 0, length);
		return conversionBuffer;
	}

	protected ByteBuf decrypt(ChannelHandlerContext context, ByteBuf buf) throws ShortBufferException {
		int length = buf.readableBytes();
		byte[] bytes = toByteArray(buf);
		ByteBuf result = context.alloc().heapBuffer(cipher.getOutputSize(length));
		result.writerIndex(cipher.update(bytes, 0, length, result.array(), result.arrayOffset()));
		return result;
	}

	protected void encrypt(ByteBuf buf, ByteBuf result) throws ShortBufferException {
		int length = buf.readableBytes();
		byte[] bytes = toByteArray(buf);
		int outputSize = cipher.getOutputSize(length);
		if (encryptionBuffer.length < outputSize) {
			encryptionBuffer = new byte[outputSize];
		}

		result.writeBytes(encryptionBuffer, 0, cipher.update(bytes, 0, length, encryptionBuffer));
	}
}
