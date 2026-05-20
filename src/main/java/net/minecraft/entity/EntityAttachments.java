package net.minecraft.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public class EntityAttachments {
   private final Map<EntityAttachmentType, List<Vec3d>> points;

   EntityAttachments(Map<EntityAttachmentType, List<Vec3d>> points) {
      this.points = points;
   }

   public static EntityAttachments of(float width, float height) {
      return builder().build(width, height);
   }

   public static EntityAttachments.Builder builder() {
      return new EntityAttachments.Builder();
   }

   public EntityAttachments scale(float xScale, float yScale, float zScale) {
      return new EntityAttachments(Util.mapEnum(EntityAttachmentType.class, type -> {
         List<Vec3d> list = new ArrayList<>();

         for (Vec3d vec3d : this.points.get(type)) {
            list.add(vec3d.multiply(xScale, yScale, zScale));
         }

         return list;
      }));
   }

   public @Nullable Vec3d getPointNullable(EntityAttachmentType type, int index, float yaw) {
      List<Vec3d> list = this.points.get(type);
      return index >= 0 && index < list.size() ? rotatePoint(list.get(index), yaw) : null;
   }

   public Vec3d getPoint(EntityAttachmentType type, int index, float yaw) {
      Vec3d vec3d = this.getPointNullable(type, index, yaw);
      if (vec3d == null) {
         throw new IllegalStateException("Had no attachment point of type: " + type + " for index: " + index);
      } else {
         return vec3d;
      }
   }

   public Vec3d getPointOrDefault(EntityAttachmentType type) {
      List<Vec3d> list = this.points.get(type);
      if (list != null && !list.isEmpty()) {
         Vec3d vec3d = Vec3d.ZERO;

         for (Vec3d vec3d2 : list) {
            vec3d = vec3d.add(vec3d2);
         }

         return vec3d.multiply(1.0F / list.size());
      } else {
         throw new IllegalStateException("No attachment points of type: PASSENGER");
      }
   }

   public Vec3d getPointOrDefault(EntityAttachmentType type, int index, float yaw) {
      List<Vec3d> list = this.points.get(type);
      if (list.isEmpty()) {
         throw new IllegalStateException("Had no attachment points of type: " + type);
      } else {
         Vec3d vec3d = list.get(MathHelper.clamp(index, 0, list.size() - 1));
         return rotatePoint(vec3d, yaw);
      }
   }

   private static Vec3d rotatePoint(Vec3d point, float yaw) {
      return point.rotateY(-yaw * (float) (Math.PI / 180.0));
   }

   public static class Builder {
      private final Map<EntityAttachmentType, List<Vec3d>> points = new EnumMap<>(EntityAttachmentType.class);

      Builder() {
      }

      public EntityAttachments.Builder add(EntityAttachmentType type, float x, float y, float z) {
         return this.add(type, new Vec3d(x, y, z));
      }

      public EntityAttachments.Builder add(EntityAttachmentType type, Vec3d point) {
         this.points.computeIfAbsent(type, list -> new ArrayList<>(1)).add(point);
         return this;
      }

      public EntityAttachments build(float width, float height) {
         Map<EntityAttachmentType, List<Vec3d>> map = Util.mapEnum(EntityAttachmentType.class, type -> {
            List<Vec3d> list = this.points.get(type);
            return list == null ? type.createPoint(width, height) : List.copyOf(list);
         });
         return new EntityAttachments(map);
      }
   }
}
