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
 * Конвертирует поле {@code CustomName} блок-сущностей из простой строки
 * в JSON-компонент текста. Обрабатывает только именуемые блок-сущности
 * (сундуки, печи, маяки и т.д.).
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
		OpticFinder<String> idFinder = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
		Type<?> inputType = getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> outputType = getOutputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> transitionalType = FixUtil.withTypeChanged(inputType, inputType, outputType);

		return fixTypeEverywhereTyped(
				"BlockEntityCustomNameToComponentFix",
				inputType,
				outputType,
				typed -> {
					Optional<String> entityId = typed.getOptional(idFinder);
					return entityId.isPresent() && !NAMEABLE_BLOCK_ENTITY_IDS.contains(entityId.get())
					       ? FixUtil.withType(outputType, typed)
					       : Util.apply(
							       FixUtil.withType(transitionalType, typed),
							       outputType,
							       BlockEntityCustomNameToTextFix::fixCustomName
					       );
				}
		);
	}

	/**
	 * Конвертирует поле {@code CustomName} из простой строки в компонент текста (JSON-формат).
	 * Если поле пустое — удаляет его.
	 */
	public static <T> Dynamic<T> fixCustomName(Dynamic<T> dynamic) {
		String customName = dynamic.get("CustomName").asString("");
		return customName.isEmpty()
		       ? dynamic.remove("CustomName")
		       : dynamic.set("CustomName", TextFixes.text(dynamic.getOps(), customName));
	}
}
