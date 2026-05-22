package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class ItemSpawnEggFix extends DataFix {

	private static final @Nullable String[] DAMAGE_TO_ENTITY_IDS = buildDamageToEntityIds();

	public ItemSpawnEggFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		Schema schema = getInputSchema();
		Type<?> type = schema.getType(TypeReferences.ITEM_STACK);
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		OpticFinder<String> opticFinder2 = DSL.fieldFinder("id", DSL.string());
		OpticFinder<?> opticFinder3 = type.findField("tag");
		OpticFinder<?> opticFinder4 = opticFinder3.type().findField("EntityTag");
		OpticFinder<?> opticFinder5 = DSL.typeFinder(schema.getTypeRaw(TypeReferences.ENTITY));
		return fixTypeEverywhereTyped(
				"ItemSpawnEggFix",
				type,
				typed -> {
					Optional<Pair<String, String>> optional = typed.getOptional(opticFinder);
					if (optional.isPresent() && Objects.equals(optional.get().getSecond(), "minecraft:spawn_egg")) {
						Dynamic<?> dynamic = (Dynamic<?>) typed.get(DSL.remainderFinder());
						short s = dynamic.get("Damage").asShort((short) 0);
						Optional<? extends Typed<?>> optional2 = typed.getOptionalTyped(opticFinder3);
						Optional<? extends Typed<?>>
								optional3 =
								optional2.flatMap(tagTyped -> tagTyped.getOptionalTyped(opticFinder4));
						Optional<? extends Typed<?>>
								optional4 =
								optional3.flatMap(entityTagTyped -> entityTagTyped.getOptionalTyped(opticFinder5));
						Optional<String>
								optional5 =
								optional4.flatMap(entityTyped -> entityTyped.getOptional(opticFinder2));
						Typed<?> typed2 = typed;
						String string = DAMAGE_TO_ENTITY_IDS[s & 255];
						if (string != null && (optional5.isEmpty() || !Objects.equals(optional5.get(), string))) {
							Typed<?> typed3 = typed.getOrCreateTyped(opticFinder3);
							Dynamic<?> dynamic2 = (Dynamic<?>) DataFixUtils.orElse(
									typed3
											.getOptionalTyped(opticFinder4)
											.map(typedx -> (Dynamic) typedx.write().getOrThrow()), dynamic.emptyMap()
							);
							dynamic2 = dynamic2.set("id", dynamic2.createString(string));
							typed2 = typed.set(opticFinder3, FixUtil.setTypedFromDynamic(typed3, opticFinder4, dynamic2));
						}

						if (s != 0) {
							dynamic = dynamic.set("Damage", dynamic.createShort((short) 0));
							typed2 = typed2.set(DSL.remainderFinder(), dynamic);
						}

						return typed2;
					}
					else {
						return typed;
					}
				}
		);
	}

	private static String[] buildDamageToEntityIds() {
		String[] arr = new String[256];
		arr[1] = "Item";
		arr[2] = "XPOrb";
		arr[7] = "ThrownEgg";
		arr[8] = "LeashKnot";
		arr[9] = "Painting";
		arr[10] = "Arrow";
		arr[11] = "Snowball";
		arr[12] = "Fireball";
		arr[13] = "SmallFireball";
		arr[14] = "ThrownEnderpearl";
		arr[15] = "EyeOfEnderSignal";
		arr[16] = "ThrownPotion";
		arr[17] = "ThrownExpBottle";
		arr[18] = "ItemFrame";
		arr[19] = "WitherSkull";
		arr[20] = "PrimedTnt";
		arr[21] = "FallingSand";
		arr[22] = "FireworksRocketEntity";
		arr[23] = "TippedArrow";
		arr[24] = "SpectralArrow";
		arr[25] = "ShulkerBullet";
		arr[26] = "DragonFireball";
		arr[30] = "ArmorStand";
		arr[41] = "Boat";
		arr[42] = "MinecartRideable";
		arr[43] = "MinecartChest";
		arr[44] = "MinecartFurnace";
		arr[45] = "MinecartTNT";
		arr[46] = "MinecartHopper";
		arr[47] = "MinecartSpawner";
		arr[40] = "MinecartCommandBlock";
		arr[50] = "Creeper";
		arr[51] = "Skeleton";
		arr[52] = "Spider";
		arr[53] = "Giant";
		arr[54] = "Zombie";
		arr[55] = "Slime";
		arr[56] = "Ghast";
		arr[57] = "PigZombie";
		arr[58] = "Enderman";
		arr[59] = "CaveSpider";
		arr[60] = "Silverfish";
		arr[61] = "Blaze";
		arr[62] = "LavaSlime";
		arr[63] = "EnderDragon";
		arr[64] = "WitherBoss";
		arr[65] = "Bat";
		arr[66] = "Witch";
		arr[67] = "Endermite";
		arr[68] = "Guardian";
		arr[69] = "Shulker";
		arr[90] = "Pig";
		arr[91] = "Sheep";
		arr[92] = "Cow";
		arr[93] = "Chicken";
		arr[94] = "Squid";
		arr[95] = "Wolf";
		arr[96] = "MushroomCow";
		arr[97] = "SnowMan";
		arr[98] = "Ozelot";
		arr[99] = "VillagerGolem";
		arr[100] = "EntityHorse";
		arr[101] = "Rabbit";
		arr[120] = "Villager";
		arr[200] = "EnderCrystal";
		return arr;
	}
}
