package net.minecraft.world;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.datafixer.DataFixTypes;

public record PersistentStateType<T extends PersistentState>(String id, Supplier<T> constructor, Codec<T> codec, DataFixTypes dataFixType) {
   @Override
   public boolean equals(Object o) {
      return o instanceof PersistentStateType<?> persistentStateType && this.id.equals(persistentStateType.id);
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return "SavedDataType[" + this.id + "]";
   }
}
