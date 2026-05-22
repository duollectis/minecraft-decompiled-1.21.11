package net.minecraft.datafixer.fix;

import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;
import java.util.Set;

/**
 * Нормализует поле здоровья сущности: устаревшее поле {@code HealF} (float) переносится
 * в {@code Health}, а само значение приводится к типу float для единообразия формата.
 */
public class EntityHealthFix extends DataFix {

	private static final Set<String> ENTITIES = Sets.newHashSet(
			"ArmorStand",
			"Bat",
			"Blaze",
			"CaveSpider",
			"Chicken",
			"Cow",
			"Creeper",
			"EnderDragon",
			"Enderman",
			"Endermite",
			"EntityHorse",
			"Ghast",
			"Giant",
			"Guardian",
			"LavaSlime",
			"MushroomCow",
			"Ozelot",
			"Pig",
			"PigZombie",
			"Rabbit",
			"Sheep",
			"Shulker",
			"Silverfish",
			"Skeleton",
			"Slime",
			"SnowMan",
			"Spider",
			"Squid",
			"Villager",
			"VillagerGolem",
			"Witch",
			"WitherBoss",
			"Wolf",
			"Zombie"
	);

	public EntityHealthFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	private Dynamic<?> fixHealth(Dynamic<?> entity) {
		Optional<Number> healF = entity.get("HealF").asNumber().result();
		Optional<Number> health = entity.get("Health").asNumber().result();

		float healthValue;

		if (healF.isPresent()) {
			healthValue = healF.get().floatValue();
			entity = entity.remove("HealF");
		} else {
			if (health.isEmpty()) {
				return entity;
			}

			healthValue = health.get().floatValue();
		}

		return entity.set("Health", entity.createFloat(healthValue));
	}

	@Override
	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"EntityHealthFix",
				getInputSchema().getType(TypeReferences.ENTITY),
				entityTyped -> entityTyped.update(DSL.remainderFinder(), this::fixHealth)
		);
	}
}
