package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Конвертирует устаревший числовой ID пластинки (поле {@code Record}) в полноценный
 * ItemStack-объект в поле {@code RecordItem}, используя таблицу маппинга предметов.
 */
public class BlockEntityJukeboxFix extends ChoiceFix {

	public BlockEntityJukeboxFix(Schema schema, boolean changesType) {
		super(schema, changesType, "BlockEntityJukeboxFix", TypeReferences.BLOCK_ENTITY, "minecraft:jukebox");
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Typed<?> transform(Typed<?> inputTyped) {
		Type<?> jukeboxType = getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:jukebox");
		Type<?> recordItemFieldType = jukeboxType.findFieldType("RecordItem");
		OpticFinder<?> recordItemFinder = DSL.fieldFinder("RecordItem", recordItemFieldType);

		Dynamic<?> remainder = (Dynamic<?>) inputTyped.get(DSL.remainderFinder());
		int recordId = remainder.get("Record").asInt(0);

		if (recordId > 0) {
			// Dynamic иммутабелен — результат remove() нужно присвоить обратно
			remainder = remainder.remove("Record");
			String itemId = ItemInstanceTheFlatteningFix.getItem(ItemIdFix.fromId(recordId), 0);

			if (itemId != null) {
				Dynamic<?> recordItem = remainder.emptyMap();
				recordItem = recordItem.set("id", recordItem.createString(itemId));
				recordItem = recordItem.set("Count", recordItem.createByte((byte) 1));

				Typed<?> recordItemTyped = (Typed<?>) ((Pair<?, ?>) recordItemFieldType
						.readTyped(recordItem)
						.result()
						.orElseThrow(() -> new IllegalStateException("Could not create record item stack."))
				).getFirst();

				return inputTyped
						.set(recordItemFinder, recordItemTyped)
						.set(DSL.remainderFinder(), remainder);
			}
		}

		return inputTyped;
	}
}
