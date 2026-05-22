package net.minecraft.datafixer.fix;

import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Мигрирует UUID-поля блок-сущностей из строкового/составного формата в массив int[4].
 * Обрабатывает кондуит ({@code target_uuid}) и череп ({@code Owner} → {@code SkullOwner}).
 */
public class BlockEntityUuidFix extends AbstractUuidFix {

	public BlockEntityUuidFix(Schema outputSchema) {
		super(outputSchema, TypeReferences.BLOCK_ENTITY);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"BlockEntityUUIDFix", getInputSchema().getType(typeReference), typed -> {
					typed = updateTyped(typed, "minecraft:conduit", this::updateConduit);
					return updateTyped(typed, "minecraft:skull", this::updateSkull);
				}
		);
	}

	@SuppressWarnings("unchecked")
	private Dynamic<?> updateSkull(Dynamic<?> skullDynamic) {
		return (Dynamic<?>) skullDynamic.get("Owner")
				.get()
				.map(ownerDynamic -> updateStringUuid(ownerDynamic, "Id", "Id")
						.orElse((Dynamic) ownerDynamic))
				.map(ownerDynamic -> skullDynamic
						.remove("Owner")
						.set("SkullOwner", ownerDynamic))
				.result()
				.orElse((Dynamic) skullDynamic);
	}

	private Dynamic<?> updateConduit(Dynamic<?> conduitDynamic) {
		return updateCompoundUuid(conduitDynamic, "target_uuid", "Target").orElse(conduitDynamic);
	}
}
