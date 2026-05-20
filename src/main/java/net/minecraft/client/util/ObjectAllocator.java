package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ObjectAllocator {
   ObjectAllocator TRIVIAL = new ObjectAllocator() {
      @Override
      public <T> T acquire(ClosableFactory<T> factory) {
         T object = factory.create();
         factory.prepare(object);
         return object;
      }

      @Override
      public <T> void release(ClosableFactory<T> factory, T value) {
         factory.close(value);
      }
   };

   <T> T acquire(ClosableFactory<T> factory);

   <T> void release(ClosableFactory<T> factory, T value);
}
