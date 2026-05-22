package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.math.random.Random;

/**
 * Для зомби с флагом {@code IsVillager=true} устанавливает поле {@code ZombieType}
 * на основе {@code VillagerProfession}. Если профессия отсутствует или некорректна,
 * выбирается случайное значение в диапазоне {@code [0, TYPE_COUNT)}.
 * После обработки флаг {@code IsVillager} удаляется.
 */
public class EntityZombieVillagerTypeFix extends ChoiceFix {

	private static final int TYPE_COUNT = 6;
	private static final int INVALID_TYPE = -1;

	public EntityZombieVillagerTypeFix(Schema schema, boolean changesType) {
		super(schema, changesType, "EntityZombieVillagerTypeFix", TypeReferences.ENTITY, "Zombie");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixZombieType);
	}

	private Dynamic<?> fixZombieType(Dynamic<?> zombie) {
		if (!zombie.get("IsVillager").asBoolean(false)) {
			return zombie;
		}

		if (zombie.get("ZombieType").result().isEmpty()) {
			int profession = clampType(zombie.get("VillagerProfession").asInt(INVALID_TYPE));
			int zombieType = profession == INVALID_TYPE
				? clampType(Random.create().nextInt(TYPE_COUNT))
				: profession;

			zombie = zombie.set("ZombieType", zombie.createInt(zombieType));
		}

		return zombie.remove("IsVillager");
	}

	private int clampType(int type) {
		return type >= 0 && type < TYPE_COUNT ? type : INVALID_TYPE;
	}
}
