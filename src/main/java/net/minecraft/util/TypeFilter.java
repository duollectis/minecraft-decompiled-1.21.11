package net.minecraft.util;

import org.jspecify.annotations.Nullable;

public interface TypeFilter<B, T extends B> {
   static <B, T extends B> TypeFilter<B, T> instanceOf(Class<T> cls) {
      return new TypeFilter<B, T>() {
         @Override
         public @Nullable T downcast(B obj) {
            return (T)(cls.isInstance(obj) ? obj : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return cls;
         }
      };
   }

   static <B, T extends B> TypeFilter<B, T> equals(Class<T> cls) {
      return new TypeFilter<B, T>() {
         @Override
         public @Nullable T downcast(B obj) {
            return (T)(cls.equals(obj.getClass()) ? obj : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return cls;
         }
      };
   }

   @Nullable T downcast(B obj);

   Class<? extends B> getBaseClass();
}
