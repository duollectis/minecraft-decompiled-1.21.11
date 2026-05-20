package net.minecraft.client.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class Vec3fArray {
   private final float[] array;

   public Vec3fArray(int size) {
      this.array = new float[3 * size];
   }

   public int size() {
      return this.array.length / 3;
   }

   public void set(int index, Vector3fc vec) {
      this.set(index, vec.x(), vec.y(), vec.z());
   }

   public void set(int index, float x, float y, float z) {
      this.array[3 * index + 0] = x;
      this.array[3 * index + 1] = y;
      this.array[3 * index + 2] = z;
   }

   public Vector3f get(int index, Vector3f vec) {
      return vec.set(this.array[3 * index + 0], this.array[3 * index + 1], this.array[3 * index + 2]);
   }

   public float getX(int index) {
      return this.array[3 * index + 0];
   }

   public float getY(int index) {
      return this.array[3 * index + 1];
   }

   public float getZ(int index) {
      return this.array[3 * index + 1];
   }
}
