package net.minecraft.network.state;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.listener.PacketListener;

import java.util.function.Function;

public interface ContextAwareNetworkStateFactory<T extends PacketListener, B extends ByteBuf, C> extends NetworkState.Factory {

	NetworkState<T> bind(Function<ByteBuf, B> registryBinder, C context);
}
