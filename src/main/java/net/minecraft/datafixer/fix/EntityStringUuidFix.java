package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;
import java.util.UUID;

/**
 * Мигрирует UUID сущности из строкового формата (поле {@code UUID})
 * в пару числовых полей {@code UUIDMost} и {@code UUIDLeast},
 * которые хранят старший и младший биты UUID соответственно.
 */
public class EntityStringUuidFix extends DataFix {

	public EntityStringUuidFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			"EntityStringUuidFix",
			getInputSchema().getType(TypeReferences.ENTITY),
			entityTyped -> entityTyped.update(DSL.remainderFinder(), this::migrateUuid)
		);
	}

	private Dynamic<?> migrateUuid(Dynamic<?> entity) {
		Optional<String> uuidString = entity.get("UUID").asString().result();

		if (uuidString.isEmpty()) {
			return entity;
		}

		UUID uuid = UUID.fromString(uuidString.get());

		return entity.remove("UUID")
			.set("UUIDMost", entity.createLong(uuid.getMostSignificantBits()))
			.set("UUIDLeast", entity.createLong(uuid.getLeastSignificantBits()));
	}
}
