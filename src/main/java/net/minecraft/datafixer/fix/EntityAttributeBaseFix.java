package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.function.DoubleUnaryOperator;

/**
 * Применяет числовое преобразование к базовому значению конкретного атрибута сущности.
 * Используется для исправления некорректных значений атрибутов при смене формата.
 */
public class EntityAttributeBaseFix extends ChoiceFix {

	private final String attributeId;
	private final DoubleUnaryOperator fixOperator;

	public EntityAttributeBaseFix(
			Schema outputSchema,
			String name,
			String entityId,
			String attributeId,
			DoubleUnaryOperator fixOperator
	) {
		super(outputSchema, false, name, TypeReferences.ENTITY, entityId);
		this.attributeId = attributeId;
		this.fixOperator = fixOperator;
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixAttributes);
	}

	private Dynamic<?> fixAttributes(Dynamic<?> entity) {
		return entity.update(
				"attributes",
				attributes -> entity.createList(attributes.asStream().map(attribute -> {
					String id = IdentifierNormalizingSchema.normalize(attribute.get("id").asString(""));

					if (!id.equals(attributeId)) {
						return attribute;
					}

					double baseValue = attribute.get("base").asDouble(0.0);
					return attribute.set("base", attribute.createDouble(fixOperator.applyAsDouble(baseValue)));
				}))
		);
	}
}
