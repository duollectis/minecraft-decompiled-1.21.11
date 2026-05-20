package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;

@Environment(EnvType.CLIENT)
public interface UniformValue {
   Codec<UniformValue> CODEC = UniformValue.Type.CODEC.dispatch(UniformValue::getType, val -> val.mapCodec);

   void write(Std140Builder builder);

   void addSize(Std140SizeCalculator calculator);

   UniformValue.Type getType();

   @Environment(EnvType.CLIENT)
   public record FloatValue(float value) implements UniformValue {
      public static final Codec<UniformValue.FloatValue> CODEC = Codec.FLOAT.xmap(UniformValue.FloatValue::new, UniformValue.FloatValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putFloat(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putFloat();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.FLOAT;
      }
   }

   @Environment(EnvType.CLIENT)
   public record IntValue(int value) implements UniformValue {
      public static final Codec<UniformValue.IntValue> CODEC = Codec.INT.xmap(UniformValue.IntValue::new, UniformValue.IntValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putInt(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putInt();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.INT;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Matrix4fValue(Matrix4fc value) implements UniformValue {
      public static final Codec<UniformValue.Matrix4fValue> CODEC = Codecs.MATRIX_4F.xmap(UniformValue.Matrix4fValue::new, UniformValue.Matrix4fValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putMat4f(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putMat4f();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.MATRIX4X4;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Type implements StringIdentifiable {
      INT("int", UniformValue.IntValue.CODEC),
      IVEC3("ivec3", UniformValue.Vec3iValue.CODEC),
      FLOAT("float", UniformValue.FloatValue.CODEC),
      VEC2("vec2", UniformValue.Vec2fValue.CODEC),
      VEC3("vec3", UniformValue.Vec3fValue.CODEC),
      VEC4("vec4", UniformValue.Vec4fValue.CODEC),
      MATRIX4X4("matrix4x4", UniformValue.Matrix4fValue.CODEC);

      public static final StringIdentifiable.EnumCodec<UniformValue.Type> CODEC = StringIdentifiable.createCodec(UniformValue.Type::values);
      private final String name;
      final MapCodec<? extends UniformValue> mapCodec;

      private Type(final String name, final Codec<? extends UniformValue> codec) {
         this.name = name;
         this.mapCodec = codec.fieldOf("value");
      }

      @Override
      public String asString() {
         return this.name;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Vec2fValue(Vector2fc value) implements UniformValue {
      public static final Codec<UniformValue.Vec2fValue> CODEC = Codecs.VECTOR_2F.xmap(UniformValue.Vec2fValue::new, UniformValue.Vec2fValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putVec2(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putVec2();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.VEC2;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Vec3fValue(Vector3fc value) implements UniformValue {
      public static final Codec<UniformValue.Vec3fValue> CODEC = Codecs.VECTOR_3F.xmap(UniformValue.Vec3fValue::new, UniformValue.Vec3fValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putVec3(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putVec3();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.VEC3;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Vec3iValue(Vector3ic value) implements UniformValue {
      public static final Codec<UniformValue.Vec3iValue> CODEC = Codecs.VECTOR_3I.xmap(UniformValue.Vec3iValue::new, UniformValue.Vec3iValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putIVec3(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putIVec3();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.IVEC3;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Vec4fValue(Vector4fc value) implements UniformValue {
      public static final Codec<UniformValue.Vec4fValue> CODEC = Codecs.VECTOR_4F.xmap(UniformValue.Vec4fValue::new, UniformValue.Vec4fValue::value);

      @Override
      public void write(Std140Builder builder) {
         builder.putVec4(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator calculator) {
         calculator.putVec4();
      }

      @Override
      public UniformValue.Type getType() {
         return UniformValue.Type.VEC4;
      }
   }
}
