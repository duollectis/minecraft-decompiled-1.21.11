package net.minecraft.datafixer.fix;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.function.Supplier;

/**
 * Разделяет единый тип {@code Zombie} на три отдельных сущности
 * в зависимости от числового поля {@code ZombieType}:
 * {@code 1–5} → {@code ZombieVillager} (с профессией {@code ZombieType - 1}),
 * {@code 6} → {@code Husk}, остальные → {@code Zombie}.
 * Поле {@code ZombieType} удаляется из NBT после миграции.
 */
public class EntityZombieSplitFix extends EntityTransformFix {

	private static final int ZOMBIE_VILLAGER_TYPE_MIN = 1;
	private static final int ZOMBIE_VILLAGER_TYPE_MAX = 5;
	private static final int HUSK_TYPE = 6;

	private final Supplier<Type<?>> zombieVillagerType =
		Suppliers.memoize(() -> getOutputSchema().getChoiceType(TypeReferences.ENTITY, "ZombieVillager"));

	public EntityZombieSplitFix(Schema outputSchema) {
		super("EntityZombieSplitFix", outputSchema, true);
	}

	@Override
	protected Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped) {
		if (!choice.equals("Zombie")) {
			return Pair.of(choice, entityTyped);
		}

		Dynamic<?> entity = (Dynamic<?>) entityTyped.getOptional(DSL.remainderFinder()).orElseThrow();
		int zombieType = entity.get("ZombieType").asInt(0);

		String newEntityId;
		Typed<?> newTyped;

		if (zombieType >= ZOMBIE_VILLAGER_TYPE_MIN && zombieType <= ZOMBIE_VILLAGER_TYPE_MAX) {
			newEntityId = "ZombieVillager";
			newTyped = setZombieVillagerProfession(entityTyped, zombieType - 1);
		} else if (zombieType == HUSK_TYPE) {
			newEntityId = "Husk";
			newTyped = entityTyped;
		} else {
			newEntityId = "Zombie";
			newTyped = entityTyped;
		}

		return Pair.of(
			newEntityId,
			newTyped.update(DSL.remainderFinder(), e -> e.remove("ZombieType"))
		);
	}

	private Typed<?> setZombieVillagerProfession(Typed<?> entityTyped, int profession) {
		return Util.apply(
			entityTyped,
			zombieVillagerType.get(),
			zombieVillager -> zombieVillager.set("Profession", zombieVillager.createInt(profession))
		);
	}
}
