package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import java.util.Optional;
import java.util.Set;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.Util;

public class ItemRemoveBlockEntityTagFix extends DataFix {
   private final Set<String> itemIds;

   public ItemRemoveBlockEntityTagFix(Schema outputSchema, Set<String> itemIds) {
      super(outputSchema, true);
      this.itemIds = itemIds;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(TypeReferences.ITEM_STACK);
      OpticFinder<?> opticFinder = type.findField("tag");
      OpticFinder<?> opticFinder2 = opticFinder.type().findField("BlockEntityTag");
      Type<?> type2 = this.getInputSchema().getType(TypeReferences.ENTITY);
      OpticFinder<?> opticFinder3 = DSL.namedChoice(
         "minecraft:falling_block", this.getInputSchema().getChoiceType(TypeReferences.ENTITY, "minecraft:falling_block")
      );
      OpticFinder<?> opticFinder4 = opticFinder3.type().findField("TileEntityData");
      Type<?> type3 = this.getInputSchema().getType(TypeReferences.STRUCTURE);
      OpticFinder<?> opticFinder5 = type3.findField("blocks");
      OpticFinder<?> opticFinder6 = DSL.typeFinder(((ListType)opticFinder5.type()).getElement());
      OpticFinder<?> opticFinder7 = opticFinder6.type().findField("nbt");
      OpticFinder<String> opticFinder8 = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "ItemRemoveBlockEntityTagFix",
            type,
            typed -> typed.updateTyped(opticFinder, typedx -> this.method_71758(typedx, opticFinder2, opticFinder8, "BlockEntityTag"))
         ),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(
               "FallingBlockEntityRemoveBlockEntityTagFix",
               type2,
               typed -> typed.updateTyped(opticFinder3, typedx -> this.method_71758(typedx, opticFinder4, opticFinder8, "TileEntityData"))
            ),
            this.fixTypeEverywhereTyped(
               "StructureRemoveBlockEntityTagFix",
               type3,
               typed -> typed.updateTyped(
                  opticFinder5, typedx -> typedx.updateTyped(opticFinder6, typedxx -> this.method_71758(typedxx, opticFinder7, opticFinder8, "nbt"))
               )
            ),
            this.convertUnchecked(
               "ItemRemoveBlockEntityTagFix - update block entity type",
               this.getInputSchema().getType(TypeReferences.BLOCK_ENTITY),
               this.getOutputSchema().getType(TypeReferences.BLOCK_ENTITY)
            )
         }
      );
   }

   private Typed<?> method_71758(Typed<?> typed, OpticFinder<?> opticFinder, OpticFinder<String> opticFinder2, String string) {
      Optional<? extends Typed<?>> optional = typed.getOptionalTyped(opticFinder);
      if (optional.isEmpty()) {
         return typed;
      } else {
         String string2 = optional.get().getOptional(opticFinder2).orElse("");
         return !this.itemIds.contains(string2) ? typed : Util.apply(typed, typed.getType(), dynamic -> dynamic.remove(string));
      }
   }
}
