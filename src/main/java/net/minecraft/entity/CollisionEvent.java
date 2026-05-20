package net.minecraft.entity;

import java.util.function.Consumer;
import net.minecraft.block.AbstractFireBlock;

public enum CollisionEvent {
   FREEZE(entity -> {
      entity.setInPowderSnow(true);
      if (entity.canFreeze()) {
         entity.setFrozenTicks(Math.min(entity.getMinFreezeDamageTicks(), entity.getFrozenTicks() + 1));
      }
   }),
   CLEAR_FREEZE(Entity::defrost),
   FIRE_IGNITE(AbstractFireBlock::igniteEntity),
   LAVA_IGNITE(Entity::igniteByLava),
   EXTINGUISH(Entity::extinguish);

   private final Consumer<Entity> action;

   private CollisionEvent(final Consumer<Entity> action) {
      this.action = action;
   }

   public Consumer<Entity> getAction() {
      return this.action;
   }
}
