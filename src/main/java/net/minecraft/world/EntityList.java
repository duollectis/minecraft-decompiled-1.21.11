package net.minecraft.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import org.jspecify.annotations.Nullable;

public class EntityList {
   private Int2ObjectMap<Entity> entities = new Int2ObjectLinkedOpenHashMap();
   private Int2ObjectMap<Entity> temp = new Int2ObjectLinkedOpenHashMap();
   private @Nullable Int2ObjectMap<Entity> iterating;

   private void ensureSafe() {
      if (this.iterating == this.entities) {
         this.temp.clear();
         ObjectIterator int2ObjectMap = Int2ObjectMaps.fastIterable(this.entities).iterator();

         while (int2ObjectMap.hasNext()) {
            Entry<Entity> entry = (Entry<Entity>)int2ObjectMap.next();
            this.temp.put(entry.getIntKey(), (Entity)entry.getValue());
         }

         Int2ObjectMap<Entity> int2ObjectMapx = this.entities;
         this.entities = this.temp;
         this.temp = int2ObjectMapx;
      }
   }

   public void add(Entity entity) {
      this.ensureSafe();
      this.entities.put(entity.getId(), entity);
   }

   public void remove(Entity entity) {
      this.ensureSafe();
      this.entities.remove(entity.getId());
   }

   public boolean has(Entity entity) {
      return this.entities.containsKey(entity.getId());
   }

   public void forEach(Consumer<Entity> action) {
      if (this.iterating != null) {
         throw new UnsupportedOperationException("Only one concurrent iteration supported");
      } else {
         this.iterating = this.entities;

         try {
            ObjectIterator var2 = this.entities.values().iterator();

            while (var2.hasNext()) {
               Entity entity = (Entity)var2.next();
               action.accept(entity);
            }
         } finally {
            this.iterating = null;
         }
      }
   }
}
