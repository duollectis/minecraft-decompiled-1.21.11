package net.minecraft.network.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;

/**
 * Netty-обработчик исходящего трафика: шифрует каждый исходящий {@link ByteBuf}
 * с помощью {@link PacketEncryptionManager}.
 */
public class PacketEncryptor extends MessageToByteEncoder<ByteBuf> {

	private final PacketEncryptionManager manager;

	public PacketEncryptor(Cipher cipher) {
		manager = new PacketEncryptionManager(cipher);
	}

	@Override
	protected void encode(ChannelHandlerContext context, ByteBuf input, ByteBuf output) throws Exception {
		manager.encrypt(input, output);
	}
}
