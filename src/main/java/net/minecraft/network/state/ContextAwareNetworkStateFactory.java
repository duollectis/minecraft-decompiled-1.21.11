package net.minecraft.network.state;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.listener.PacketListener;

import java.util.function.Function;

/**
 * Фабрика {@link NetworkState}, принимающая контекст при каждой привязке к реестру.
 * Используется когда поведение кодека зависит от состояния конкретного соединения
 * (например, {@link net.minecraft.network.state.PlayStateFactories.PacketCodecModifierContext}).
 *
 * @param <T> тип слушателя пакетов
 * @param <B> тип буфера
 * @param <C> тип контекста
 */
public interface ContextAwareNetworkStateFactory<T extends PacketListener, B extends ByteBuf, C> extends NetworkState.Factory {

	NetworkState<T> bind(Function<ByteBuf, B> registryBinder, C context);
}
