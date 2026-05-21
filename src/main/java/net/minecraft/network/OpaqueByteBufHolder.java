package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

/**
 * Обёртка над {@link ByteBuf} с подсчётом ссылок для безопасной передачи через Netty-пайплайн.
 * <p>Используется для упаковки сырых буферов в типобезопасный контейнер,
 * который корректно управляет временем жизни буфера через {@link ReferenceCounted}.
 *
 * @param contents содержимое буфера
 */
public record OpaqueByteBufHolder(ByteBuf contents) implements ReferenceCounted {

	/**
	 * Создаёт держатель, проверяя доступность буфера.
	 *
	 * @param contents буфер для хранения
	 */
	public OpaqueByteBufHolder(ByteBuf contents) {
		this.contents = ByteBufUtil.ensureAccessible(contents);
	}

	/**
	 * Упаковывает объект в {@link OpaqueByteBufHolder}, если он является {@link ByteBuf}.
	 *
	 * @param buf объект для упаковки
	 * @return {@link OpaqueByteBufHolder} или исходный объект без изменений
	 */
	public static Object pack(Object buf) {
		return buf instanceof ByteBuf byteBuf ? new OpaqueByteBufHolder(byteBuf) : buf;
	}

	/**
	 * Распаковывает {@link OpaqueByteBufHolder} обратно в {@link ByteBuf}.
	 *
	 * @param holder объект для распаковки
	 * @return {@link ByteBuf} или исходный объект без изменений
	 */
	public static Object unpack(Object holder) {
		return holder instanceof OpaqueByteBufHolder opaque
		       ? ByteBufUtil.ensureAccessible(opaque.contents)
		       : holder;
	}

	@Override
	public int refCnt() {
		return contents.refCnt();
	}

	@Override
	public OpaqueByteBufHolder retain() {
		contents.retain();
		return this;
	}

	@Override
	public OpaqueByteBufHolder retain(int increment) {
		contents.retain(increment);
		return this;
	}

	@Override
	public OpaqueByteBufHolder touch() {
		contents.touch();
		return this;
	}

	@Override
	public OpaqueByteBufHolder touch(Object hint) {
		contents.touch(hint);
		return this;
	}

	@Override
	public boolean release() {
		return contents.release();
	}

	@Override
	public boolean release(int decrement) {
		return contents.release(decrement);
	}
}
