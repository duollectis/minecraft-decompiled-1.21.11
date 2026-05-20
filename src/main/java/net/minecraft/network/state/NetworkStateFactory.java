package net.minecraft.network.state;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.network.listener.PacketListener;

public interface NetworkStateFactory<T extends PacketListener, B extends ByteBuf> extends NetworkState.Factory {
   NetworkState<T> bind(Function<ByteBuf, B> registryBinder);
}
