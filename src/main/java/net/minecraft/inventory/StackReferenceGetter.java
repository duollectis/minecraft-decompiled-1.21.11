package net.minecraft.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.loot.slot.ItemStream;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * {@code StackReferenceGetter}.
 */
public interface StackReferenceGetter {

	@Nullable StackReference getStackReference(int slot);

	default ItemStream getStackReferences(IntList slots) {
		List<StackReference>
				list =
				slots.intStream().mapToObj(this::getStackReference).filter(Objects::nonNull).toList();
		return ItemStream.of(list);
	}
}
