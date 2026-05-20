package net.minecraft.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Objects;
import net.minecraft.loot.slot.ItemStream;
import org.jspecify.annotations.Nullable;

public interface StackReferenceGetter {
   @Nullable StackReference getStackReference(int slot);

   default ItemStream getStackReferences(IntList slots) {
      List<StackReference> list = slots.intStream().mapToObj(this::getStackReference).filter(Objects::nonNull).toList();
      return ItemStream.of(list);
   }
}
