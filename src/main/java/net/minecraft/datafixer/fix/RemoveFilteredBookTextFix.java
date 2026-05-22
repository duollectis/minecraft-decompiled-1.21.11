package net.minecraft.datafixer.fix;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.Util;

/**
 * Исправляет данные в формате DataFixer.
 */
public class RemoveFilteredBookTextFix extends ItemNbtFix {

	public RemoveFilteredBookTextFix(Schema outputSchema) {
		super(
				outputSchema,
				"Remove filtered text from books",
				itemId -> itemId.equals("minecraft:writable_book") || itemId.equals("minecraft:written_book")
		);
	}

	@Override
	protected Typed<?> fix(Typed<?> typed) {
		return Util.apply(typed, typed.getType(), nbt -> nbt.remove("filtered_title").remove("filtered_pages"));
	}
}
