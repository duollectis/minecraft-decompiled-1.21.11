package net.minecraft.network.state;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.listener.PacketListener;

import java.util.function.Function;

/**
 * Фабрика {@link NetworkState} с фиксированным контекстом.
 * Привязывает состояние протокола к конкретному реестру через {@link #bind}.
 *
 * @param <T> тип слушателя пакетов
 * @param <B> тип буфера
 */
public interface NetworkStateFactory<T extends PacketListener, B extends ByteBuf> extends NetworkState.Factory {

	NetworkState<T> bind(Function<ByteBuf, B> registryBinder);
}
