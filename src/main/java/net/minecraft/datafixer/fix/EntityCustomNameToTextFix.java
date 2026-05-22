package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.Util;

import java.util.Optional;

/**
 * Конвертирует строковое поле {@code CustomName} сущности в компонент текста.
 * Для командного вагончика имя остаётся строкой, для остальных — оборачивается в JSON-компонент.
 */
public class EntityCustomNameToTextFix extends DataFix {

	public EntityCustomNameToTextFix(Schema schema) {
		super(schema, true);
	}

	@SuppressWarnings("unchecked")
	public TypeRewriteRule makeRule() {
		Type<?> inputEntityType = getInputSchema().getType(TypeReferences.ENTITY);
		Type<?> outputEntityType = getOutputSchema().getType(TypeReferences.ENTITY);
		OpticFinder<String> idFinder = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
		OpticFinder<String> customNameFinder = (OpticFinder<String>) inputEntityType.findField("CustomName");
		Type<?> outputCustomNameType = outputEntityType.findFieldType("CustomName");

		return fixTypeEverywhereTyped(
				"EntityCustomNameToComponentFix",
				inputEntityType,
				outputEntityType,
				typed -> updateCustomName(typed, outputEntityType, idFinder, customNameFinder, outputCustomNameType)
		);
	}

	private static <T> Typed<?> updateCustomName(
			Typed<?> typed,
			Type<?> outputType,
			OpticFinder<String> idFinder,
			OpticFinder<String> customNameFinder,
			Type<T> outputCustomNameType
	) {
		Optional<String> customName = typed.getOptional(customNameFinder);

		if (customName.isEmpty()) {
			return FixUtil.withType(outputType, (Typed<T>) typed);
		}

		if (customName.get().isEmpty()) {
			return Util.apply(typed, outputType, dynamic -> dynamic.remove("CustomName"));
		}

		String entityId = typed.getOptional(idFinder).orElse("");
		Dynamic<?> nameDynamic = createNameDynamic(typed.getOps(), customName.get(), entityId);
		return typed.set(customNameFinder, Util.readTyped(outputCustomNameType, nameDynamic));
	}

	private static <T> Dynamic<T> createNameDynamic(DynamicOps<T> ops, String name, String entityId) {
		return "minecraft:commandblock_minecart".equals(entityId)
				? new Dynamic(ops, ops.createString(name))
				: TextFixes.text(ops, name);
	}
}
