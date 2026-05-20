package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class AffineTransformation {
   private final Matrix4fc matrix;
   public static final Codec<AffineTransformation> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codecs.VECTOR_3F.fieldOf("translation").forGetter(affineTransformation -> affineTransformation.translation),
            Codecs.ROTATION.fieldOf("left_rotation").forGetter(affineTransformation -> affineTransformation.leftRotation),
            Codecs.VECTOR_3F.fieldOf("scale").forGetter(affineTransformation -> affineTransformation.scale),
            Codecs.ROTATION.fieldOf("right_rotation").forGetter(affineTransformation -> affineTransformation.rightRotation)
         )
         .apply(instance, AffineTransformation::new)
   );
   public static final Codec<AffineTransformation> ANY_CODEC = Codec.withAlternative(
      CODEC, Codecs.MATRIX_4F.xmap(AffineTransformation::new, AffineTransformation::getMatrix)
   );
   private boolean initialized;
   private @Nullable Vector3fc translation;
   private @Nullable Quaternionfc leftRotation;
   private @Nullable Vector3fc scale;
   private @Nullable Quaternionfc rightRotation;
   private static final AffineTransformation IDENTITY = Util.make(() -> {
      AffineTransformation affineTransformation = new AffineTransformation(new Matrix4f());
      affineTransformation.translation = new Vector3f();
      affineTransformation.leftRotation = new Quaternionf();
      affineTransformation.scale = new Vector3f(1.0F, 1.0F, 1.0F);
      affineTransformation.rightRotation = new Quaternionf();
      affineTransformation.initialized = true;
      return affineTransformation;
   });

   public AffineTransformation(@Nullable Matrix4fc matrix) {
      if (matrix == null) {
         this.matrix = new Matrix4f();
      } else {
         this.matrix = matrix;
      }
   }

   public AffineTransformation(
      @Nullable Vector3fc translation, @Nullable Quaternionfc leftRotation, @Nullable Vector3fc scale, @Nullable Quaternionfc rightRotation
   ) {
      this.matrix = setup(translation, leftRotation, scale, rightRotation);
      this.translation = (Vector3fc)(translation != null ? translation : new Vector3f());
      this.leftRotation = (Quaternionfc)(leftRotation != null ? leftRotation : new Quaternionf());
      this.scale = (Vector3fc)(scale != null ? scale : new Vector3f(1.0F, 1.0F, 1.0F));
      this.rightRotation = (Quaternionfc)(rightRotation != null ? rightRotation : new Quaternionf());
      this.initialized = true;
   }

   public static AffineTransformation identity() {
      return IDENTITY;
   }

   public AffineTransformation multiply(AffineTransformation other) {
      Matrix4f matrix4f = this.copyMatrix();
      matrix4f.mul(other.getMatrix());
      return new AffineTransformation(matrix4f);
   }

   public @Nullable AffineTransformation invert() {
      if (this == IDENTITY) {
         return this;
      } else {
         Matrix4f matrix4f = this.copyMatrix().invertAffine();
         return matrix4f.isFinite() ? new AffineTransformation(matrix4f) : null;
      }
   }

   private void init() {
      if (!this.initialized) {
         float f = 1.0F / this.matrix.m33();
         Triple<Quaternionf, Vector3f, Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(this.matrix).scale(f));
         this.translation = this.matrix.getTranslation(new Vector3f()).mul(f);
         this.leftRotation = new Quaternionf((Quaternionfc)triple.getLeft());
         this.scale = new Vector3f((Vector3fc)triple.getMiddle());
         this.rightRotation = new Quaternionf((Quaternionfc)triple.getRight());
         this.initialized = true;
      }
   }

   private static Matrix4f setup(
      @Nullable Vector3fc translation, @Nullable Quaternionfc leftRotation, @Nullable Vector3fc scale, @Nullable Quaternionfc rightRotation
   ) {
      Matrix4f matrix4f = new Matrix4f();
      if (translation != null) {
         matrix4f.translation(translation);
      }

      if (leftRotation != null) {
         matrix4f.rotate(leftRotation);
      }

      if (scale != null) {
         matrix4f.scale(scale);
      }

      if (rightRotation != null) {
         matrix4f.rotate(rightRotation);
      }

      return matrix4f;
   }

   public Matrix4fc getMatrix() {
      return this.matrix;
   }

   public Matrix4f copyMatrix() {
      return new Matrix4f(this.matrix);
   }

   public Vector3fc getTranslation() {
      this.init();
      return this.translation;
   }

   public Quaternionfc getLeftRotation() {
      this.init();
      return this.leftRotation;
   }

   public Vector3fc getScale() {
      this.init();
      return this.scale;
   }

   public Quaternionfc getRightRotation() {
      this.init();
      return this.rightRotation;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AffineTransformation affineTransformation = (AffineTransformation)o;
         return Objects.equals(this.matrix, affineTransformation.matrix);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.matrix);
   }

   public AffineTransformation interpolate(AffineTransformation target, float factor) {
      return new AffineTransformation(
         this.getTranslation().lerp(target.getTranslation(), factor, new Vector3f()),
         this.getLeftRotation().slerp(target.getLeftRotation(), factor, new Quaternionf()),
         this.getScale().lerp(target.getScale(), factor, new Vector3f()),
         this.getRightRotation().slerp(target.getRightRotation(), factor, new Quaternionf())
      );
   }
}
