package net.minecraft.datafixer.fix;

import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Мигрирует UUID всех сущностей из формата пары полей {@code UUIDMost}/{@code UUIDLeast}
 * (или строкового {@code UUID}) в новый формат — массив из четырёх int-значений.
 * Обрабатывает специфичные поля для каждой категории сущностей:
 * ручные/прирученные, размножаемые, привязываемые, снаряды и прочие.
 */
public class EntityUuidFix extends AbstractUuidFix {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Set<String> RIDEABLE_TAMEABLES = Sets.newHashSet();
	private static final Set<String> TAMEABLE_PETS = Sets.newHashSet();
	private static final Set<String> BREEDABLES = Sets.newHashSet();
	private static final Set<String> LEASHABLES = Sets.newHashSet();
	private static final Set<String> OTHER_LIVINGS = Sets.newHashSet();
	private static final Set<String> PROJECTILES = Sets.newHashSet();

	static {
		RIDEABLE_TAMEABLES.add("minecraft:donkey");
		RIDEABLE_TAMEABLES.add("minecraft:horse");
		RIDEABLE_TAMEABLES.add("minecraft:llama");
		RIDEABLE_TAMEABLES.add("minecraft:mule");
		RIDEABLE_TAMEABLES.add("minecraft:skeleton_horse");
		RIDEABLE_TAMEABLES.add("minecraft:trader_llama");
		RIDEABLE_TAMEABLES.add("minecraft:zombie_horse");

		TAMEABLE_PETS.add("minecraft:cat");
		TAMEABLE_PETS.add("minecraft:parrot");
		TAMEABLE_PETS.add("minecraft:wolf");

		BREEDABLES.add("minecraft:bee");
		BREEDABLES.add("minecraft:chicken");
		BREEDABLES.add("minecraft:cow");
		BREEDABLES.add("minecraft:fox");
		BREEDABLES.add("minecraft:mooshroom");
		BREEDABLES.add("minecraft:ocelot");
		BREEDABLES.add("minecraft:panda");
		BREEDABLES.add("minecraft:pig");
		BREEDABLES.add("minecraft:polar_bear");
		BREEDABLES.add("minecraft:rabbit");
		BREEDABLES.add("minecraft:sheep");
		BREEDABLES.add("minecraft:turtle");
		BREEDABLES.add("minecraft:hoglin");

		LEASHABLES.add("minecraft:bat");
		LEASHABLES.add("minecraft:blaze");
		LEASHABLES.add("minecraft:cave_spider");
		LEASHABLES.add("minecraft:cod");
		LEASHABLES.add("minecraft:creeper");
		LEASHABLES.add("minecraft:dolphin");
		LEASHABLES.add("minecraft:drowned");
		LEASHABLES.add("minecraft:elder_guardian");
		LEASHABLES.add("minecraft:ender_dragon");
		LEASHABLES.add("minecraft:enderman");
		LEASHABLES.add("minecraft:endermite");
		LEASHABLES.add("minecraft:evoker");
		LEASHABLES.add("minecraft:ghast");
		LEASHABLES.add("minecraft:giant");
		LEASHABLES.add("minecraft:guardian");
		LEASHABLES.add("minecraft:husk");
		LEASHABLES.add("minecraft:illusioner");
		LEASHABLES.add("minecraft:magma_cube");
		LEASHABLES.add("minecraft:pufferfish");
		LEASHABLES.add("minecraft:zombified_piglin");
		LEASHABLES.add("minecraft:salmon");
		LEASHABLES.add("minecraft:shulker");
		LEASHABLES.add("minecraft:silverfish");
		LEASHABLES.add("minecraft:skeleton");
		LEASHABLES.add("minecraft:slime");
		LEASHABLES.add("minecraft:snow_golem");
		LEASHABLES.add("minecraft:spider");
		LEASHABLES.add("minecraft:squid");
		LEASHABLES.add("minecraft:stray");
		LEASHABLES.add("minecraft:tropical_fish");
		LEASHABLES.add("minecraft:vex");
		LEASHABLES.add("minecraft:villager");
		LEASHABLES.add("minecraft:iron_golem");
		LEASHABLES.add("minecraft:vindicator");
		LEASHABLES.add("minecraft:pillager");
		LEASHABLES.add("minecraft:wandering_trader");
		LEASHABLES.add("minecraft:witch");
		LEASHABLES.add("minecraft:wither");
		LEASHABLES.add("minecraft:wither_skeleton");
		LEASHABLES.add("minecraft:zombie");
		LEASHABLES.add("minecraft:zombie_villager");
		LEASHABLES.add("minecraft:phantom");
		LEASHABLES.add("minecraft:ravager");
		LEASHABLES.add("minecraft:piglin");

		OTHER_LIVINGS.add("minecraft:armor_stand");

		PROJECTILES.add("minecraft:arrow");
		PROJECTILES.add("minecraft:dragon_fireball");
		PROJECTILES.add("minecraft:firework_rocket");
		PROJECTILES.add("minecraft:fireball");
		PROJECTILES.add("minecraft:llama_spit");
		PROJECTILES.add("minecraft:small_fireball");
		PROJECTILES.add("minecraft:snowball");
		PROJECTILES.add("minecraft:spectral_arrow");
		PROJECTILES.add("minecraft:egg");
		PROJECTILES.add("minecraft:ender_pearl");
		PROJECTILES.add("minecraft:experience_bottle");
		PROJECTILES.add("minecraft:potion");
		PROJECTILES.add("minecraft:trident");
		PROJECTILES.add("minecraft:wither_skull");
	}

	public EntityUuidFix(Schema outputSchema) {
		super(outputSchema, TypeReferences.ENTITY);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			"EntityUUIDFixes",
			getInputSchema().getType(typeReference),
			typed -> {
				typed = typed.update(DSL.remainderFinder(), EntityUuidFix::updateSelfUuid);

				for (String entityId : RIDEABLE_TAMEABLES) {
					typed = updateTyped(typed, entityId, EntityUuidFix::updateTameable);
				}

				for (String entityId : TAMEABLE_PETS) {
					typed = updateTyped(typed, entityId, EntityUuidFix::updateTameable);
				}

				for (String entityId : BREEDABLES) {
					typed = updateTyped(typed, entityId, EntityUuidFix::updateBreedable);
				}

				for (String entityId : LEASHABLES) {
					typed = updateTyped(typed, entityId, EntityUuidFix::updateLeashable);
				}

				for (String entityId : OTHER_LIVINGS) {
					typed = updateTyped(typed, entityId, EntityUuidFix::updateLiving);
				}

				for (String entityId : PROJECTILES) {
					typed = updateTyped(typed, entityId, EntityUuidFix::updateProjectile);
				}

				typed = updateTyped(typed, "minecraft:bee", EntityUuidFix::updateZombifiedPiglin);
				typed = updateTyped(typed, "minecraft:zombified_piglin", EntityUuidFix::updateZombifiedPiglin);
				typed = updateTyped(typed, "minecraft:fox", EntityUuidFix::updateFox);
				typed = updateTyped(typed, "minecraft:item", EntityUuidFix::updateItemEntity);
				typed = updateTyped(typed, "minecraft:shulker_bullet", EntityUuidFix::updateShulkerBullet);
				typed = updateTyped(typed, "minecraft:area_effect_cloud", EntityUuidFix::updateAreaEffectCloud);
				typed = updateTyped(typed, "minecraft:zombie_villager", EntityUuidFix::updateZombieVillager);
				typed = updateTyped(typed, "minecraft:evoker_fangs", EntityUuidFix::updateEvokerFangs);

				return updateTyped(typed, "minecraft:piglin", EntityUuidFix::updateAngryAtMemory);
			}
		);
	}

	public static Dynamic<?> updateSelfUuid(Dynamic<?> entity) {
		return updateRegularMostLeast(entity, "UUID", "UUID").orElse(entity);
	}

	public static Dynamic<?> updateLiving(Dynamic<?> entity) {
		return entity.update(
			"Attributes",
			attributes -> entity.createList(
				attributes.asStream().map(attribute ->
					attribute.update(
						"Modifiers",
						modifiers -> attribute.createList(
							modifiers.asStream().map(modifier ->
								(Dynamic<?>) updateRegularMostLeast(modifier, "UUID", "UUID").orElse(modifier)
							)
						)
					)
				)
			)
		);
	}

	private static Dynamic<?> updateTameable(Dynamic<?> entity) {
		Dynamic<?> breedable = updateBreedable(entity);
		return updateStringUuid(breedable, "OwnerUUID", "Owner").orElse(breedable);
	}

	private static Dynamic<?> updateBreedable(Dynamic<?> entity) {
		Dynamic<?> leashable = updateLeashable(entity);
		return updateRegularMostLeast(leashable, "LoveCause", "LoveCause").orElse(leashable);
	}

	private static Dynamic<?> updateLeashable(Dynamic<?> entity) {
		return updateLiving(entity).update(
			"Leash",
			leash -> updateRegularMostLeast(leash, "UUID", "UUID").orElse(leash)
		);
	}

	private static Dynamic<?> updateProjectile(Dynamic<?> entity) {
		return (Dynamic<?>) DataFixUtils.orElse(
			entity.get("OwnerUUID")
				.result()
				.map(ownerUuid -> entity.remove("OwnerUUID").set("Owner", ownerUuid)),
			entity
		);
	}

	private static Dynamic<?> updateZombifiedPiglin(Dynamic<?> entity) {
		return updateStringUuid(entity, "HurtBy", "HurtBy").orElse(entity);
	}

	private static Dynamic<?> updateFox(Dynamic<?> entity) {
		Optional<Dynamic<?>> trusted = entity.get("TrustedUUIDs")
			.result()
			.map(uuids -> entity.createList(
				uuids.asStream().map(uuid ->
					(Dynamic<?>) createArrayFromCompoundUuid(uuid).orElseGet(() -> {
						LOGGER.warn("Trusted contained invalid data.");
						return uuid;
					})
				)
			));

		return (Dynamic<?>) DataFixUtils.orElse(
			trusted.map(trustedArray -> entity.remove("TrustedUUIDs").set("Trusted", trustedArray)),
			entity
		);
	}

	private static Dynamic<?> updateItemEntity(Dynamic<?> entity) {
		Dynamic<?> withOwner = updateCompoundUuid(entity, "Owner", "Owner").orElse(entity);
		return updateCompoundUuid(withOwner, "Thrower", "Thrower").orElse(withOwner);
	}

	private static Dynamic<?> updateShulkerBullet(Dynamic<?> entity) {
		Dynamic<?> withOwner = updateCompoundUuid(entity, "Owner", "Owner").orElse(entity);
		return updateCompoundUuid(withOwner, "Target", "Target").orElse(withOwner);
	}

	private static Dynamic<?> updateAreaEffectCloud(Dynamic<?> entity) {
		return updateRegularMostLeast(entity, "OwnerUUID", "Owner").orElse(entity);
	}

	private static Dynamic<?> updateZombieVillager(Dynamic<?> entity) {
		return updateRegularMostLeast(entity, "ConversionPlayer", "ConversionPlayer").orElse(entity);
	}

	private static Dynamic<?> updateEvokerFangs(Dynamic<?> entity) {
		return updateRegularMostLeast(entity, "OwnerUUID", "Owner").orElse(entity);
	}

	private static Dynamic<?> updateAngryAtMemory(Dynamic<?> entity) {
		return entity.update(
			"Brain",
			brain -> brain.update(
				"memories",
				memories -> memories.update(
					"minecraft:angry_at",
					angryAt -> updateStringUuid(angryAt, "value", "value").orElseGet(() -> {
						LOGGER.warn("angry_at has no value.");
						return angryAt;
					})
				)
			)
		);
	}
}
