package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.attribute.EnvironmentAttributes;

@Environment(EnvType.CLIENT)
public class TimeProperty extends NeedleAngleState implements NumericProperty {
   public static final MapCodec<TimeProperty> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleAngleState::hasWobble),
            TimeProperty.Source.CODEC.fieldOf("source").forGetter(property -> property.source)
         )
         .apply(instance, TimeProperty::new)
   );
   private final TimeProperty.Source source;
   private final Random random = Random.create();
   private final NeedleAngleState.Angler angler;

   public TimeProperty(boolean wobble, TimeProperty.Source source) {
      super(wobble);
      this.source = source;
      this.angler = this.createAngler(0.9F);
   }

   @Override
   protected float getAngle(ItemStack stack, ClientWorld world, int seed, HeldItemContext context) {
      float f = this.source.getAngle(world, stack, context, this.random);
      long l = world.getTime();
      if (this.angler.shouldUpdate(l)) {
         this.angler.update(l, f);
      }

      return this.angler.getAngle();
   }

   @Override
   public MapCodec<TimeProperty> getCodec() {
      return CODEC;
   }

   @Environment(EnvType.CLIENT)
   public static enum Source implements StringIdentifiable {
      RANDOM("random") {
         @Override
         public float getAngle(ClientWorld world, ItemStack stack, HeldItemContext heldItemContext, Random random) {
            return random.nextFloat();
         }
      },
      DAYTIME("daytime") {
         @Override
         public float getAngle(ClientWorld world, ItemStack stack, HeldItemContext heldItemContext, Random random) {
            return world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.SUN_ANGLE_VISUAL, heldItemContext.getEntityPos()) / 360.0F;
         }
      },
      MOON_PHASE("moon_phase") {
         @Override
         public float getAngle(ClientWorld world, ItemStack stack, HeldItemContext heldItemContext, Random random) {
            return (float)world.getEnvironmentAttributes()
                  .getAttributeValue(EnvironmentAttributes.MOON_PHASE_VISUAL, heldItemContext.getEntityPos())
                  .getIndex()
               / MoonPhase.COUNT;
         }
      };

      public static final Codec<TimeProperty.Source> CODEC = StringIdentifiable.createCodec(TimeProperty.Source::values);
      private final String name;

      Source(final String name) {
         this.name = name;
      }

      @Override
      public String asString() {
         return this.name;
      }

      abstract float getAngle(ClientWorld world, ItemStack stack, HeldItemContext heldItemContext, Random random);
   }
}
