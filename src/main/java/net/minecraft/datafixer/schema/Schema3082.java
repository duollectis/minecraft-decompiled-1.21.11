package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3082 (Minecraft 1.19 — The Wild Update).
 * <p>
 * Регистрирует тип данных для сущности лодки с сундуком ({@code minecraft:chest_boat}),
 * добавленной в обновлении 1.19. Лодка хранит инвентарь предметов в поле {@code Items}.
 */
public class Schema3082 extends IdentifierNormalizingSchema {

	public Schema3082(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:chest_boat",
			name -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return entityTypes;
	}
}
