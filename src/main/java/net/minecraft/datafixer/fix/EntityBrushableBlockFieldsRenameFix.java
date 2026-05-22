package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Переименовывает поля блок-энтити {@code minecraft:brushable_block}:
 * {@code loot_table} → {@code LootTable} и {@code loot_table_seed} → {@code LootTableSeed}.
 */
public class EntityBrushableBlockFieldsRenameFix extends ChoiceFix {

	public EntityBrushableBlockFieldsRenameFix(Schema outputSchema) {
		super(
				outputSchema,
				false,
				"EntityBrushableBlockFieldsRenameFix",
				TypeReferences.BLOCK_ENTITY,
				"minecraft:brushable_block"
		);
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::renameFields);
	}

	private Dynamic<?> renameFields(Dynamic<?> dynamic) {
		return dynamic
				.renameField("loot_table", "LootTable")
				.renameField("loot_table_seed", "LootTableSeed");
	}
}
