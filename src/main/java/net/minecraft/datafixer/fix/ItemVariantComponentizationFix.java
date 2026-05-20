package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.Util;

public class ItemVariantComponentizationFix extends DataFix {
   public ItemVariantComponentizationFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   @SuppressWarnings("unchecked")
   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(TypeReferences.ITEM_STACK);
      OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
         "id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
      );
      OpticFinder<Object> opticFinder2 = (OpticFinder<Object>) type.findField("components");
      return this.fixTypeEverywhereTyped(
         "ItemStack bucket_entity_data variants to separate components",
         type,
         typed -> {
            String string = typed.getOptional(opticFinder).<String>map(Pair::getSecond).orElse("");

            return switch (string) {
               case "minecraft:salmon_bucket" -> typed.updateTyped(opticFinder2, (class_10622) ItemVariantComponentizationFix::fixSalmonBucket);
               case "minecraft:axolotl_bucket" -> typed.updateTyped(opticFinder2, (class_10622) ItemVariantComponentizationFix::fixAxolotlBucket);
               case "minecraft:tropical_fish_bucket" -> typed.updateTyped(opticFinder2, (class_10622) ItemVariantComponentizationFix::fixTropicalFishBucket);
               case "minecraft:painting" -> typed.updateTyped(
                  opticFinder2, typedx -> Util.apply(typedx, typedx.getType(), ItemVariantComponentizationFix::fixPainting)
               );
               default -> typed;
            };
         }
      );
   }

   private static String getTropicalFishBaseColorName(int variant) {
      return FixUtil.getColorName(variant >> 16 & 0xFF);
   }

   private static String getTropicalFishPatternColorName(int variant) {
      return FixUtil.getColorName(variant >> 24 & 0xFF);
   }

   private static String getTropicalFishPatternName(int variant) {
      return switch (variant & 65535) {
         case 1 -> "flopper";
         case 256 -> "sunstreak";
         case 257 -> "stripey";
         case 512 -> "snooper";
         case 513 -> "glitter";
         case 768 -> "dasher";
         case 769 -> "blockfish";
         case 1024 -> "brinely";
         case 1025 -> "betty";
         case 1280 -> "spotty";
         case 1281 -> "clayfish";
         default -> "kob";
      };
   }

   private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> dynamic, Dynamic<T> dynamic2) {
      Optional<Number> optional = dynamic2.get("BucketVariantTag").asNumber().result();
      if (optional.isEmpty()) {
         return dynamic;
      } else {
         int i = optional.get().intValue();
         String string = getTropicalFishPatternName(i);
         String string2 = getTropicalFishBaseColorName(i);
         String string3 = getTropicalFishPatternColorName(i);
         return dynamic.update("minecraft:bucket_entity_data", dynamicx -> dynamicx.remove("BucketVariantTag"))
            .set("minecraft:tropical_fish/pattern", dynamic.createString(string))
            .set("minecraft:tropical_fish/base_color", dynamic.createString(string2))
            .set("minecraft:tropical_fish/pattern_color", dynamic.createString(string3));
      }
   }

   private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> dynamic, Dynamic<T> dynamic2) {
      Optional<Number> optional = dynamic2.get("Variant").asNumber().result();
      if (optional.isEmpty()) {
         return dynamic;
      } else {
         String string = switch (optional.get().intValue()) {
            case 1 -> "wild";
            case 2 -> "gold";
            case 3 -> "cyan";
            case 4 -> "blue";
            default -> "lucy";
         };
         return dynamic.update("minecraft:bucket_entity_data", dynamicx -> dynamicx.remove("Variant"))
            .set("minecraft:axolotl/variant", dynamic.createString(string));
      }
   }

   private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> dynamic, Dynamic<T> dynamic2) {
      Optional<Dynamic<T>> optional = dynamic2.get("type").result();
      return optional.isEmpty()
         ? dynamic
         : dynamic.update("minecraft:bucket_entity_data", dynamicx -> dynamicx.remove("type")).set("minecraft:salmon/size", optional.get());
   }

   private static <T> Dynamic<T> fixPainting(Dynamic<T> dynamic) {
      Optional<Dynamic<T>> optional = dynamic.get("minecraft:entity_data").result();
      if (optional.isEmpty()) {
         return dynamic;
      } else if (optional.get().get("id").asString().result().filter(string -> string.equals("minecraft:painting")).isEmpty()) {
         return dynamic;
      } else {
         Optional<Dynamic<T>> optional2 = optional.get().get("variant").result();
         Dynamic<T> dynamic2 = optional.get().remove("variant");
         if (dynamic2.remove("id").equals(dynamic2.emptyMap())) {
            dynamic = dynamic.remove("minecraft:entity_data");
         } else {
            dynamic = dynamic.set("minecraft:entity_data", dynamic2);
         }

         if (optional2.isPresent()) {
            dynamic = dynamic.set("minecraft:painting/variant", optional2.get());
         }

         return dynamic;
      }
   }

   @FunctionalInterface
   interface class_10622 extends Function<Typed<?>, Typed<?>> {
      default Typed<?> apply(Typed<?> typed) {
         return typed.update(DSL.remainderFinder(), this::fixRemainder);
      }

      default <T> Dynamic<T> fixRemainder(Dynamic<T> dynamic) {
         return dynamic.get("minecraft:bucket_entity_data").result().map(dynamic2 -> this.fixRemainder(dynamic, (Dynamic<T>)dynamic2)).orElse(dynamic);
      }

      <T> Dynamic<T> fixRemainder(Dynamic<T> dynamic, Dynamic<T> dynamic2);
   }
}
