package net.minecraft.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public interface HeldItemContext {
   World getEntityWorld();

   Vec3d getEntityPos();

   float getBodyYaw();

   default @Nullable LivingEntity getEntity() {
      return null;
   }

   static HeldItemContext offseted(HeldItemContext context, Vec3d offset) {
      return new HeldItemContext.Offset(context, offset);
   }

   public record Offset(HeldItemContext owner, Vec3d offset) implements HeldItemContext {
      @Override
      public World getEntityWorld() {
         return this.owner.getEntityWorld();
      }

      @Override
      public Vec3d getEntityPos() {
         return this.owner.getEntityPos().add(this.offset);
      }

      @Override
      public float getBodyYaw() {
         return this.owner.getBodyYaw();
      }

      @Override
      public @Nullable LivingEntity getEntity() {
         return this.owner.getEntity();
      }
   }
}
