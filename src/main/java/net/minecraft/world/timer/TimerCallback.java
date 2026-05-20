package net.minecraft.world.timer;

import com.mojang.serialization.MapCodec;

public interface TimerCallback<T> {
   void call(T server, Timer<T> events, long time);

   MapCodec<? extends TimerCallback<T>> getCodec();
}
