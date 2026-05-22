package net.minecraft.network.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

/**
 * Netty-обработчик входящего трафика: дешифрует каждый входящий {@link ByteBuf}
 * с помощью {@link PacketEncryptionManager}.
 */
public class PacketDecryptor extends MessageToMessageDecoder<ByteBuf> {

	private final PacketEncryptionManager manager;

	public PacketDecryptor(Cipher cipher) {
		manager = new PacketEncryptionManager(cipher);
	}

	@Override
	protected void decode(ChannelHandlerContext context, ByteBuf buf, List<Object> out) throws Exception {
		out.add(manager.decrypt(context, buf));
	}
}
