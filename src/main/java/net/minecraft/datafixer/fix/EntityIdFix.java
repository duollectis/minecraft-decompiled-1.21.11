package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;

/**
 * Переименовывает все старые строковые идентификаторы сущностей в формат {@code minecraft:snake_case}.
 * Применяется при переходе на пространства имён в идентификаторах сущностей.
 */
public class EntityIdFix extends DataFix {

	private static final Map<String, String> RENAMED_ENTITIES = Map.ofEntries(
			Map.entry("AreaEffectCloud", "minecraft:area_effect_cloud"),
			Map.entry("ArmorStand", "minecraft:armor_stand"),
			Map.entry("Arrow", "minecraft:arrow"),
			Map.entry("Bat", "minecraft:bat"),
			Map.entry("Blaze", "minecraft:blaze"),
			Map.entry("Boat", "minecraft:boat"),
			Map.entry("CaveSpider", "minecraft:cave_spider"),
			Map.entry("Chicken", "minecraft:chicken"),
			Map.entry("Cow", "minecraft:cow"),
			Map.entry("Creeper", "minecraft:creeper"),
			Map.entry("Donkey", "minecraft:donkey"),
			Map.entry("DragonFireball", "minecraft:dragon_fireball"),
			Map.entry("ElderGuardian", "minecraft:elder_guardian"),
			Map.entry("EnderCrystal", "minecraft:ender_crystal"),
			Map.entry("EnderDragon", "minecraft:ender_dragon"),
			Map.entry("Enderman", "minecraft:enderman"),
			Map.entry("Endermite", "minecraft:endermite"),
			Map.entry("EyeOfEnderSignal", "minecraft:eye_of_ender_signal"),
			Map.entry("FallingSand", "minecraft:falling_block"),
			Map.entry("Fireball", "minecraft:fireball"),
			Map.entry("FireworksRocketEntity", "minecraft:fireworks_rocket"),
			Map.entry("Ghast", "minecraft:ghast"),
			Map.entry("Giant", "minecraft:giant"),
			Map.entry("Guardian", "minecraft:guardian"),
			Map.entry("Horse", "minecraft:horse"),
			Map.entry("Husk", "minecraft:husk"),
			Map.entry("Item", "minecraft:item"),
			Map.entry("ItemFrame", "minecraft:item_frame"),
			Map.entry("LavaSlime", "minecraft:magma_cube"),
			Map.entry("LeashKnot", "minecraft:leash_knot"),
			Map.entry("MinecartChest", "minecraft:chest_minecart"),
			Map.entry("MinecartCommandBlock", "minecraft:commandblock_minecart"),
			Map.entry("MinecartFurnace", "minecraft:furnace_minecart"),
			Map.entry("MinecartHopper", "minecraft:hopper_minecart"),
			Map.entry("MinecartRideable", "minecraft:minecart"),
			Map.entry("MinecartSpawner", "minecraft:spawner_minecart"),
			Map.entry("MinecartTNT", "minecraft:tnt_minecart"),
			Map.entry("Mule", "minecraft:mule"),
			Map.entry("MushroomCow", "minecraft:mooshroom"),
			Map.entry("Ozelot", "minecraft:ocelot"),
			Map.entry("Painting", "minecraft:painting"),
			Map.entry("Pig", "minecraft:pig"),
			Map.entry("PigZombie", "minecraft:zombie_pigman"),
			Map.entry("PolarBear", "minecraft:polar_bear"),
			Map.entry("PrimedTnt", "minecraft:tnt"),
			Map.entry("Rabbit", "minecraft:rabbit"),
			Map.entry("Sheep", "minecraft:sheep"),
			Map.entry("Shulker", "minecraft:shulker"),
			Map.entry("ShulkerBullet", "minecraft:shulker_bullet"),
			Map.entry("Silverfish", "minecraft:silverfish"),
			Map.entry("Skeleton", "minecraft:skeleton"),
			Map.entry("SkeletonHorse", "minecraft:skeleton_horse"),
			Map.entry("Slime", "minecraft:slime"),
			Map.entry("SmallFireball", "minecraft:small_fireball"),
			Map.entry("SnowMan", "minecraft:snowman"),
			Map.entry("Snowball", "minecraft:snowball"),
			Map.entry("SpectralArrow", "minecraft:spectral_arrow"),
			Map.entry("Spider", "minecraft:spider"),
			Map.entry("Squid", "minecraft:squid"),
			Map.entry("Stray", "minecraft:stray"),
			Map.entry("ThrownEgg", "minecraft:egg"),
			Map.entry("ThrownEnderpearl", "minecraft:ender_pearl"),
			Map.entry("ThrownExpBottle", "minecraft:xp_bottle"),
			Map.entry("ThrownPotion", "minecraft:potion"),
			Map.entry("Villager", "minecraft:villager"),
			Map.entry("VillagerGolem", "minecraft:villager_golem"),
			Map.entry("Witch", "minecraft:witch"),
			Map.entry("WitherBoss", "minecraft:wither"),
			Map.entry("WitherSkeleton", "minecraft:wither_skeleton"),
			Map.entry("WitherSkull", "minecraft:wither_skull"),
			Map.entry("Wolf", "minecraft:wolf"),
			Map.entry("XPOrb", "minecraft:xp_orb"),
			Map.entry("Zombie", "minecraft:zombie"),
			Map.entry("ZombieHorse", "minecraft:zombie_horse"),
			Map.entry("ZombieVillager", "minecraft:zombie_villager")
	);

	public EntityIdFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public TypeRewriteRule makeRule() {
		TaggedChoiceType<String> inputChoiceType =
				(TaggedChoiceType<String>) getInputSchema().findChoiceType(TypeReferences.ENTITY);
		TaggedChoiceType<String> outputChoiceType =
				(TaggedChoiceType<String>) getOutputSchema().findChoiceType(TypeReferences.ENTITY);
		Type<?> inputItemType = getInputSchema().getType(TypeReferences.ITEM_STACK);
		Type<?> outputItemType = getOutputSchema().getType(TypeReferences.ITEM_STACK);

		return TypeRewriteRule.seq(
				convertUnchecked("item stack entity name hook converter", inputItemType, outputItemType),
				fixTypeEverywhere(
						"EntityIdFix",
						inputChoiceType,
						outputChoiceType,
						dynamicOps -> pair -> pair.mapFirst(id -> RENAMED_ENTITIES.getOrDefault(id, id))
				)
		);
	}
}
