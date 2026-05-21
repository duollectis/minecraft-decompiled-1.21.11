package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.Set;

/**
 * {@code BlockEntityCustomNameToTextFix}.
 */
public class BlockEntityCustomNameToTextFix extends DataFix {

	private static final Set<String> NAMEABLE_BLOCK_ENTITY_IDS = Set.of(
			"minecraft:beacon",
			"minecraft:banner",
			"minecraft:brewing_stand",
			"minecraft:chest",
			"minecraft:trapped_chest",
			"minecraft:dispenser",
			"minecraft:dropper",
			"minecraft:enchanting_table",
			"minecraft:furnace",
			"minecraft:hopper",
			"minecraft:shulker_box"
	);

	public BlockEntityCustomNameToTextFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	public TypeRewriteRule makeRule() {
		OpticFinder<String> opticFinder = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
		Type<?> type = this.getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> type2 = this.getOutputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> type3 = FixUtil.withTypeChanged(type, type, type2);
		return this.fixTypeEverywhereTyped(
				"BlockEntityCustomNameToComponentFix",
				type,
				type2,
				typed -> {
					Optional<String> optional = typed.getOptional(opticFinder);
					return optional.isPresent() && !NAMEABLE_BLOCK_ENTITY_IDS.contains(optional.get())
					       ? FixUtil.withType(type2, typed)
					       : Util.apply(
							       FixUtil.withType(type3, typed),
							       type2,
							       BlockEntityCustomNameToTextFix::fixCustomName
					       );
				}
		);
	}

	public static <T> Dynamic<T> fixCustomName(Dynamic<T> dynamic) {
		String string = dynamic.get("CustomName").asString("");
		return string.isEmpty() ? dynamic.remove("CustomName")
		                        : dynamic.set("CustomName", TextFixes.text(dynamic.getOps(), string));
	}
}
