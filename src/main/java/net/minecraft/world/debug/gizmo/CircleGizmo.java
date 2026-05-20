package net.minecraft.world.debug.gizmo;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Vec3d;

public record CircleGizmo(Vec3d pos, float radius, DrawStyle style) implements Gizmo {
   private static final int NUM_VERTICES = 20;
   private static final float ANGLE_INTERVAL = (float) (Math.PI / 10);

   @Override
   public void draw(GizmoDrawer consumer, float opacity) {
      if (this.style.hasStroke() || this.style.hasFill()) {
         Vec3d[] vec3ds = new Vec3d[21];

         for (int i = 0; i < 20; i++) {
            float f = i * (float) (Math.PI / 10);
            Vec3d vec3d = this.pos.add((float)(this.radius * Math.cos(f)), 0.0, (float)(this.radius * Math.sin(f)));
            vec3ds[i] = vec3d;
         }

         vec3ds[20] = vec3ds[0];
         if (this.style.hasFill()) {
            int i = this.style.fill(opacity);
            consumer.addPolygon(vec3ds, i);
         }

         if (this.style.hasStroke()) {
            int i = this.style.stroke(opacity);

            for (int j = 0; j < 20; j++) {
               consumer.addLine(vec3ds[j], vec3ds[j + 1], i, this.style.strokeWidth());
            }
         }
      }
   }
}
