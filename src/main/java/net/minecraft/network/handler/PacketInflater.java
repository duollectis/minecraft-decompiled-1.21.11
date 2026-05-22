package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.encoding.VarInts;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Netty-обработчик входящих пакетов: распаковывает данные алгоритмом INFLATE.
 * VarInt-заголовок {@code 0} означает, что пакет не сжат и передаётся как есть.
 * При {@code rejectsBadPackets = true} отклоняет пакеты с некорректным размером.
 */
public class PacketInflater extends ByteToMessageDecoder {

	public static final int MAX_UNCOMPRESSED_SIZE = 2097152;
	public static final int MAXIMUM_PACKET_SIZE = 8388608;

	private final Inflater inflater;
	private int compressionThreshold;
	private boolean rejectsBadPackets;

	public PacketInflater(int compressionThreshold, boolean rejectsBadPackets) {
		this.compressionThreshold = compressionThreshold;
		this.rejectsBadPackets = rejectsBadPackets;
		inflater = new Inflater();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> output) throws Exception {
		int declaredSize = VarInts.read(buf);

		if (declaredSize == 0) {
			output.add(buf.readBytes(buf.readableBytes()));
			return;
		}

		if (rejectsBadPackets) {
			if (declaredSize < compressionThreshold) {
				throw new DecoderException(
						"Badly compressed packet - size of " + declaredSize
								+ " is below server threshold of " + compressionThreshold
				);
			}

			if (declaredSize > MAXIMUM_PACKET_SIZE) {
				throw new DecoderException(
						"Badly compressed packet - size of " + declaredSize
								+ " is larger than protocol maximum of " + MAXIMUM_PACKET_SIZE
				);
			}
		}

		setInputBuf(buf);
		ByteBuf inflated = inflate(ctx, declaredSize);
		inflater.reset();
		output.add(inflated);
	}

	private void setInputBuf(ByteBuf buf) {
		ByteBuffer nioBuffer;

		if (buf.nioBufferCount() > 0) {
			nioBuffer = buf.nioBuffer();
			buf.skipBytes(buf.readableBytes());
		} else {
			nioBuffer = ByteBuffer.allocateDirect(buf.readableBytes());
			buf.readBytes(nioBuffer);
			nioBuffer.flip();
		}

		inflater.setInput(nioBuffer);
	}

	/**
	 * Распаковывает содержимое {@code inflater} в новый прямой буфер.
	 * Освобождает буфер при любом исключении, чтобы не допустить утечки памяти.
	 */
	private ByteBuf inflate(ChannelHandlerContext ctx, int expectedSize) throws DataFormatException {
		ByteBuf result = ctx.alloc().directBuffer(expectedSize);

		try {
			ByteBuffer nioBuffer = result.internalNioBuffer(0, expectedSize);
			int startPosition = nioBuffer.position();
			inflater.inflate(nioBuffer);
			int written = nioBuffer.position() - startPosition;

			if (written != expectedSize) {
				throw new DecoderException(
						"Badly compressed packet - actual length of uncompressed payload " + written
								+ " does not match declared size " + expectedSize
				);
			}

			result.writerIndex(result.writerIndex() + written);
			return result;
		} catch (Exception e) {
			result.release();
			throw e;
		}
	}

	public void setCompressionThreshold(int compressionThreshold, boolean rejectsBadPackets) {
		this.compressionThreshold = compressionThreshold;
		this.rejectsBadPackets = rejectsBadPackets;
	}
}
