package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

import java.util.Objects;

/**
 * Разделяет оцелота ({@code minecraft:ocelot}) на кота ({@code minecraft:cat}):
 * если {@code CatType} от 1 до 3 — создаёт кота; если 0 и есть владелец — помечает как доверяющего.
 */
public class EntityCatSplitFix extends EntitySimpleTransformFix {

	private static final int OCELOT_CAT_TYPE_MIN = 1;
	private static final int OCELOT_CAT_TYPE_MAX = 3;

	public EntityCatSplitFix(Schema schema, boolean changesType) {
		super("EntityCatSplitFix", schema, changesType);
	}

	@Override
	protected Pair<String, Dynamic<?>> transform(String choice, Dynamic<?> entity) {
		if (!Objects.equals("minecraft:ocelot", choice)) {
			return Pair.of(choice, entity);
		}

		int catType = entity.get("CatType").asInt(0);

		if (catType == 0) {
			String owner = entity.get("Owner").asString("");
			String ownerUuid = entity.get("OwnerUUID").asString("");

			if (!owner.isEmpty() || !ownerUuid.isEmpty()) {
				// Оцелот с владельцем становится доверяющим, но остаётся оцелотом
				entity = entity.set("Trusting", entity.createBoolean(true));
			}

			return Pair.of(choice, entity);
		}

		if (catType >= OCELOT_CAT_TYPE_MIN && catType <= OCELOT_CAT_TYPE_MAX) {
			entity = entity.set("CatType", entity.createInt(catType));
			entity = entity.set("OwnerUUID", entity.createString(entity.get("OwnerUUID").asString("")));
			return Pair.of("minecraft:cat", entity);
		}

		return Pair.of(choice, entity);
	}
}
