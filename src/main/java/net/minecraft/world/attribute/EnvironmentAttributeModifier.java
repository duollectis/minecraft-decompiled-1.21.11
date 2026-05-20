package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Interpolator;

public interface EnvironmentAttributeModifier<Subject, Argument> {
   Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Boolean, ?>> BOOLEAN_MODIFIERS = Map.of(
      EnvironmentAttributeModifier.Type.AND,
      BooleanModifier.AND,
      EnvironmentAttributeModifier.Type.NAND,
      BooleanModifier.NAND,
      EnvironmentAttributeModifier.Type.OR,
      BooleanModifier.OR,
      EnvironmentAttributeModifier.Type.NOR,
      BooleanModifier.NOR,
      EnvironmentAttributeModifier.Type.XOR,
      BooleanModifier.XOR,
      EnvironmentAttributeModifier.Type.XNOR,
      BooleanModifier.XNOR
   );
   Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Float, ?>> FLOAT_MODIFIERS = Map.of(
      EnvironmentAttributeModifier.Type.ALPHA_BLEND,
      FloatModifier.ALPHA_BLEND,
      EnvironmentAttributeModifier.Type.ADD,
      FloatModifier.ADD,
      EnvironmentAttributeModifier.Type.SUBTRACT,
      FloatModifier.SUBTRACT,
      EnvironmentAttributeModifier.Type.MULTIPLY,
      FloatModifier.MULTIPLY,
      EnvironmentAttributeModifier.Type.MINIMUM,
      FloatModifier.MINIMUM,
      EnvironmentAttributeModifier.Type.MAXIMUM,
      FloatModifier.MAXIMUM
   );
   Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Integer, ?>> RGB = Map.of(
      EnvironmentAttributeModifier.Type.ALPHA_BLEND,
      ColorModifier.ALPHA_BLEND,
      EnvironmentAttributeModifier.Type.ADD,
      ColorModifier.ADD,
      EnvironmentAttributeModifier.Type.SUBTRACT,
      ColorModifier.SUBTRACT,
      EnvironmentAttributeModifier.Type.MULTIPLY,
      ColorModifier.MULTIPLY_RGB,
      EnvironmentAttributeModifier.Type.BLEND_TO_GRAY,
      ColorModifier.BLEND_TO_GRAY
   );
   Map<EnvironmentAttributeModifier.Type, EnvironmentAttributeModifier<Integer, ?>> ARGB = Map.of(
      EnvironmentAttributeModifier.Type.ALPHA_BLEND,
      ColorModifier.ALPHA_BLEND,
      EnvironmentAttributeModifier.Type.ADD,
      ColorModifier.ADD,
      EnvironmentAttributeModifier.Type.SUBTRACT,
      ColorModifier.SUBTRACT,
      EnvironmentAttributeModifier.Type.MULTIPLY,
      ColorModifier.MULTIPLY_ARGB,
      EnvironmentAttributeModifier.Type.BLEND_TO_GRAY,
      ColorModifier.BLEND_TO_GRAY
   );

   @SuppressWarnings("unchecked")
   static <Value> EnvironmentAttributeModifier<Value, Value> override() {
      return (EnvironmentAttributeModifier<Value, Value>)EnvironmentAttributeModifier.OverrideModifier.INSTANCE;
   }

   Subject apply(Subject value, Argument argument);

   Codec<Argument> argumentCodec(EnvironmentAttribute<Subject> attribute);

   Interpolator<Argument> argumentKeyframeLerp(EnvironmentAttribute<Subject> attribute);

   public record OverrideModifier<Value>() implements EnvironmentAttributeModifier<Value, Value> {
      static final EnvironmentAttributeModifier.OverrideModifier<?> INSTANCE = new EnvironmentAttributeModifier.OverrideModifier();

      @Override
      public Value apply(Value object, Value object2) {
         return object2;
      }

      @Override
      public Codec<Value> argumentCodec(EnvironmentAttribute<Value> environmentAttribute) {
         return environmentAttribute.getCodec();
      }

      @Override
      public Interpolator<Value> argumentKeyframeLerp(EnvironmentAttribute<Value> environmentAttribute) {
         return environmentAttribute.getType().keyframeLerp();
      }
   }

   public static enum Type implements StringIdentifiable {
      OVERRIDE("override"),
      ALPHA_BLEND("alpha_blend"),
      ADD("add"),
      SUBTRACT("subtract"),
      MULTIPLY("multiply"),
      BLEND_TO_GRAY("blend_to_gray"),
      MINIMUM("minimum"),
      MAXIMUM("maximum"),
      AND("and"),
      NAND("nand"),
      OR("or"),
      NOR("nor"),
      XOR("xor"),
      XNOR("xnor");

      public static final Codec<EnvironmentAttributeModifier.Type> CODEC = StringIdentifiable.createCodec(EnvironmentAttributeModifier.Type::values);
      private final String name;

      private Type(final String name) {
         this.name = name;
      }

      @Override
      public String asString() {
         return this.name;
      }
   }
}
