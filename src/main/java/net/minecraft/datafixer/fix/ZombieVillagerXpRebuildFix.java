package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class ZombieVillagerXpRebuildFix extends ChoiceFix {

	public ZombieVillagerXpRebuildFix(Schema schema, boolean bl) {
		super(schema, bl, "Zombie Villager XP rebuild", TypeReferences.ENTITY, "minecraft:zombie_villager");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(
				DSL.remainderFinder(), zombieVillagerDynamic -> {
					Optional<Number> optional = zombieVillagerDynamic.get("Xp").asNumber().result();
					if (optional.isEmpty()) {
						int i = zombieVillagerDynamic.get("VillagerData").get("level").asInt(1);
						return zombieVillagerDynamic.set(
								"Xp",
								zombieVillagerDynamic.createInt(VillagerExperienceRebuildFix.levelToExperience(i))
						);
					}
					else {
						return zombieVillagerDynamic;
					}
				}
		);
	}
}
