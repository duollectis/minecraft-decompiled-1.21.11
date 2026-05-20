package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ClosableFactory;
import net.minecraft.client.util.Handle;

@Environment(EnvType.CLIENT)
public interface FramePass {
   <T> Handle<T> addRequiredResource(String name, ClosableFactory<T> factory);

   <T> void dependsOn(Handle<T> handle);

   <T> Handle<T> transfer(Handle<T> handle);

   void addRequired(FramePass pass);

   void markToBeVisited();

   void setRenderer(Runnable renderer);
}
