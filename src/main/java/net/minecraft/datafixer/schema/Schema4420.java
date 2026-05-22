package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4420, обновляющая регистрацию сущности
 * {@code minecraft:area_effect_cloud}: поле частицы переименовано
 * с {@code Particle} на {@code custom_particle} и теперь ссылается на тип {@code PARTICLE}.
 */
public class Schema4420 extends IdentifierNormalizingSchema {

	public Schema4420(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.register(
				map,
				"minecraft:area_effect_cloud",
				string -> DSL.optionalFields("custom_particle", TypeReferences.PARTICLE.in(schema))
		);
		return map;
	}
}
