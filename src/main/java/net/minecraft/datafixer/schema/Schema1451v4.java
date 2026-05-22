package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1451v4: регистрирует тип {@code BLOCK_NAME} как константный
 * идентификатор, обеспечивая строгую типизацию имён блоков в системе DataFixer.
 */
public class Schema1451v4 extends IdentifierNormalizingSchema {

	public Schema1451v4(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(false, TypeReferences.BLOCK_NAME, () -> DSL.constType(getIdentifierType()));
	}
}
