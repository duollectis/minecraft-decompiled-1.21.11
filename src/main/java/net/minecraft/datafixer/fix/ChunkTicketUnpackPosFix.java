package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.stream.IntStream;
import net.minecraft.datafixer.TypeReferences;

public class ChunkTicketUnpackPosFix extends DataFix {
   private static final long field_56614 = 32L;
   private static final long field_56615 = 4294967295L;

   public ChunkTicketUnpackPosFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "ChunkTicketUnpackPosFix",
         this.getInputSchema().getType(TypeReferences.TICKETS_SAVED_DATA),
         typed -> typed.update(
            DSL.remainderFinder(),
            dynamic -> dynamic.update(
               "data",
               dataDynamic -> dataDynamic.update(
                  "tickets",
                  ticketsDynamic -> ticketsDynamic.createList(
                     ticketsDynamic.asStream().map(ticketDynamic -> ticketDynamic.update("chunk_pos", chunkPosDynamic -> {
                        long l = chunkPosDynamic.asLong(0L);
                        int i = (int)(l & 4294967295L);
                        int j = (int)(l >>> 32 & 4294967295L);
                        return chunkPosDynamic.createIntList(IntStream.of(i, j));
                     }))
                  )
               )
            )
         )
      );
   }
}
