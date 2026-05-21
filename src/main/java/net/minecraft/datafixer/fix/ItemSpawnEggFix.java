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
 * {@code ItemSpawnEggFix}.
 */
public class ItemSpawnEggFix extends DataFix {

	private static final @Nullable String[] DAMAGE_TO_ENTITY_IDS = (String[]) DataFixUtils.make(
			new String[256], ids -> {
				ids[1] = "Item";
				ids[2] = "XPOrb";
				ids[7] = "ThrownEgg";
				ids[8] = "LeashKnot";
				ids[9] = "Painting";
				ids[10] = "Arrow";
				ids[11] = "Snowball";
				ids[12] = "Fireball";
				ids[13] = "SmallFireball";
				ids[14] = "ThrownEnderpearl";
				ids[15] = "EyeOfEnderSignal";
				ids[16] = "ThrownPotion";
				ids[17] = "ThrownExpBottle";
				ids[18] = "ItemFrame";
				ids[19] = "WitherSkull";
				ids[20] = "PrimedTnt";
				ids[21] = "FallingSand";
				ids[22] = "FireworksRocketEntity";
				ids[23] = "TippedArrow";
				ids[24] = "SpectralArrow";
				ids[25] = "ShulkerBullet";
				ids[26] = "DragonFireball";
				ids[30] = "ArmorStand";
				ids[41] = "Boat";
				ids[42] = "MinecartRideable";
				ids[43] = "MinecartChest";
				ids[44] = "MinecartFurnace";
				ids[45] = "MinecartTNT";
				ids[46] = "MinecartHopper";
				ids[47] = "MinecartSpawner";
				ids[40] = "MinecartCommandBlock";
				ids[50] = "Creeper";
				ids[51] = "Skeleton";
				ids[52] = "Spider";
				ids[53] = "Giant";
				ids[54] = "Zombie";
				ids[55] = "Slime";
				ids[56] = "Ghast";
				ids[57] = "PigZombie";
				ids[58] = "Enderman";
				ids[59] = "CaveSpider";
				ids[60] = "Silverfish";
				ids[61] = "Blaze";
				ids[62] = "LavaSlime";
				ids[63] = "EnderDragon";
				ids[64] = "WitherBoss";
				ids[65] = "Bat";
				ids[66] = "Witch";
				ids[67] = "Endermite";
				ids[68] = "Guardian";
				ids[69] = "Shulker";
				ids[90] = "Pig";
				ids[91] = "Sheep";
				ids[92] = "Cow";
				ids[93] = "Chicken";
				ids[94] = "Squid";
				ids[95] = "Wolf";
				ids[96] = "MushroomCow";
				ids[97] = "SnowMan";
				ids[98] = "Ozelot";
				ids[99] = "VillagerGolem";
				ids[100] = "EntityHorse";
				ids[101] = "Rabbit";
				ids[120] = "Villager";
				ids[200] = "EnderCrystal";
			}
	);

	public ItemSpawnEggFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		Schema schema = this.getInputSchema();
		Type<?> type = schema.getType(TypeReferences.ITEM_STACK);
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		OpticFinder<String> opticFinder2 = DSL.fieldFinder("id", DSL.string());
		OpticFinder<?> opticFinder3 = type.findField("tag");
		OpticFinder<?> opticFinder4 = opticFinder3.type().findField("EntityTag");
		OpticFinder<?> opticFinder5 = DSL.typeFinder(schema.getTypeRaw(TypeReferences.ENTITY));
		return this.fixTypeEverywhereTyped(
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
}
