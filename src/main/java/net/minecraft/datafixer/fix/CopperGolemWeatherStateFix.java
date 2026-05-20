package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

public class CopperGolemWeatherStateFix extends ChoiceFix {
   public CopperGolemWeatherStateFix(Schema outputSchema) {
      super(outputSchema, false, "CopperGolemWeatherStateFix", TypeReferences.ENTITY, "minecraft:copper_golem");
   }

   @Override
   protected Typed<?> transform(Typed<?> inputTyped) {
      return inputTyped.update(DSL.remainderFinder(), dynamic -> dynamic.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState));
   }

   private static Dynamic<?> fixWeatherState(Dynamic<?> weatherStateDynamic) {
      return switch (weatherStateDynamic.asInt(0)) {
         case 1 -> weatherStateDynamic.createString("exposed");
         case 2 -> weatherStateDynamic.createString("weathered");
         case 3 -> weatherStateDynamic.createString("oxidized");
         default -> weatherStateDynamic.createString("unaffected");
      };
   }
}
