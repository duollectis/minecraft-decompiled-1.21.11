package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import net.minecraft.network.encoding.VarInts;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Netty-обработчик входящих данных: разбивает поток байт на отдельные пакеты
 * по VarInt-заголовку длины (максимум {@value #LENGTH_BYTES} байта = 21-битное число).
 * Если данных недостаточно для полного пакета, сбрасывает позицию чтения и ждёт следующего фрагмента.
 */
public class SplitterHandler extends ByteToMessageDecoder {

	private static final int LENGTH_BYTES = 3;

	private final ByteBuf reusableBuf = Unpooled.directBuffer(LENGTH_BYTES);
	private final @Nullable PacketSizeLogger packetSizeLogger;

	public SplitterHandler(@Nullable PacketSizeLogger packetSizeLogger) {
		this.packetSizeLogger = packetSizeLogger;
	}

	@Override
	protected void handlerRemoved0(ChannelHandlerContext ctx) {
		reusableBuf.release();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> output) {
		buf.markReaderIndex();
		reusableBuf.clear();

		if (!tryReadLengthHeader(buf, reusableBuf)) {
			buf.resetReaderIndex();
			return;
		}

		int frameLength = VarInts.read(reusableBuf);

		if (frameLength == 0) {
			throw new CorruptedFrameException("Frame length cannot be zero");
		}

		if (buf.readableBytes() < frameLength) {
			buf.resetReaderIndex();
			return;
		}

		if (packetSizeLogger != null) {
			packetSizeLogger.increment(frameLength + VarInts.getSizeInBytes(frameLength));
		}

		output.add(buf.readBytes(frameLength));
	}

	/**
	 * Считывает до {@value #LENGTH_BYTES} байт VarInt-заголовка длины из {@code source} в {@code sizeBuf}.
	 * Возвращает {@code true}, если заголовок прочитан полностью, {@code false} — если данных недостаточно.
	 */
	private static boolean tryReadLengthHeader(ByteBuf source, ByteBuf sizeBuf) {
		for (int byteIndex = 0; byteIndex < LENGTH_BYTES; byteIndex++) {
			if (!source.isReadable()) {
				return false;
			}

			byte current = source.readByte();
			sizeBuf.writeByte(current);

			if (!VarInts.shouldContinueRead(current)) {
				return true;
			}
		}

		throw new CorruptedFrameException("length wider than 21-bit");
	}
}
