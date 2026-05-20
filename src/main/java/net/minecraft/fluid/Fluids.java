package net.minecraft.fluid;

import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class Fluids {
   public static final Fluid EMPTY = register("empty", new EmptyFluid());
   public static final FlowableFluid FLOWING_WATER = register("flowing_water", new WaterFluid.Flowing());
   public static final FlowableFluid WATER = register("water", new WaterFluid.Still());
   public static final FlowableFluid FLOWING_LAVA = register("flowing_lava", new LavaFluid.Flowing());
   public static final FlowableFluid LAVA = register("lava", new LavaFluid.Still());

   private static <T extends Fluid> T register(String id, T value) {
      return Registry.register(Registries.FLUID, id, value);
   }

   static {
      for (Fluid fluid : Registries.FLUID) {
         UnmodifiableIterator var2 = fluid.getStateManager().getStates().iterator();

         while (var2.hasNext()) {
            FluidState fluidState = (FluidState)var2.next();
            Fluid.STATE_IDS.add(fluidState);
         }
      }
   }
}
