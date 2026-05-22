package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class ItemInstanceSpawnEggFix extends DataFix {

	private final String spawnEggId;
	private static final Map<String, String> ENTITY_SPAWN_EGGS = Map.ofEntries(
		Map.entry("minecraft:bat", "minecraft:bat_spawn_egg"),
		Map.entry("minecraft:blaze", "minecraft:blaze_spawn_egg"),
		Map.entry("minecraft:cave_spider", "minecraft:cave_spider_spawn_egg"),
		Map.entry("minecraft:chicken", "minecraft:chicken_spawn_egg"),
		Map.entry("minecraft:cow", "minecraft:cow_spawn_egg"),
		Map.entry("minecraft:creeper", "minecraft:creeper_spawn_egg"),
		Map.entry("minecraft:donkey", "minecraft:donkey_spawn_egg"),
		Map.entry("minecraft:elder_guardian", "minecraft:elder_guardian_spawn_egg"),
		Map.entry("minecraft:ender_dragon", "minecraft:ender_dragon_spawn_egg"),
		Map.entry("minecraft:enderman", "minecraft:enderman_spawn_egg"),
		Map.entry("minecraft:endermite", "minecraft:endermite_spawn_egg"),
		Map.entry("minecraft:evocation_illager", "minecraft:evocation_illager_spawn_egg"),
		Map.entry("minecraft:ghast", "minecraft:ghast_spawn_egg"),
		Map.entry("minecraft:guardian", "minecraft:guardian_spawn_egg"),
		Map.entry("minecraft:horse", "minecraft:horse_spawn_egg"),
		Map.entry("minecraft:husk", "minecraft:husk_spawn_egg"),
		Map.entry("minecraft:iron_golem", "minecraft:iron_golem_spawn_egg"),
		Map.entry("minecraft:llama", "minecraft:llama_spawn_egg"),
		Map.entry("minecraft:magma_cube", "minecraft:magma_cube_spawn_egg"),
		Map.entry("minecraft:mooshroom", "minecraft:mooshroom_spawn_egg"),
		Map.entry("minecraft:mule", "minecraft:mule_spawn_egg"),
		Map.entry("minecraft:ocelot", "minecraft:ocelot_spawn_egg"),
		Map.entry("minecraft:pufferfish", "minecraft:pufferfish_spawn_egg"),
		Map.entry("minecraft:parrot", "minecraft:parrot_spawn_egg"),
		Map.entry("minecraft:pig", "minecraft:pig_spawn_egg"),
		Map.entry("minecraft:polar_bear", "minecraft:polar_bear_spawn_egg"),
		Map.entry("minecraft:rabbit", "minecraft:rabbit_spawn_egg"),
		Map.entry("minecraft:sheep", "minecraft:sheep_spawn_egg"),
		Map.entry("minecraft:shulker", "minecraft:shulker_spawn_egg"),
		Map.entry("minecraft:silverfish", "minecraft:silverfish_spawn_egg"),
		Map.entry("minecraft:skeleton", "minecraft:skeleton_spawn_egg"),
		Map.entry("minecraft:skeleton_horse", "minecraft:skeleton_horse_spawn_egg"),
		Map.entry("minecraft:slime", "minecraft:slime_spawn_egg"),
		Map.entry("minecraft:snow_golem", "minecraft:snow_golem_spawn_egg"),
		Map.entry("minecraft:spider", "minecraft:spider_spawn_egg"),
		Map.entry("minecraft:squid", "minecraft:squid_spawn_egg"),
		Map.entry("minecraft:stray", "minecraft:stray_spawn_egg"),
		Map.entry("minecraft:turtle", "minecraft:turtle_spawn_egg"),
		Map.entry("minecraft:vex", "minecraft:vex_spawn_egg"),
		Map.entry("minecraft:villager", "minecraft:villager_spawn_egg"),
		Map.entry("minecraft:vindication_illager", "minecraft:vindication_illager_spawn_egg"),
		Map.entry("minecraft:witch", "minecraft:witch_spawn_egg"),
		Map.entry("minecraft:wither", "minecraft:wither_spawn_egg"),
		Map.entry("minecraft:wither_skeleton", "minecraft:wither_skeleton_spawn_egg"),
		Map.entry("minecraft:wolf", "minecraft:wolf_spawn_egg"),
		Map.entry("minecraft:zombie", "minecraft:zombie_spawn_egg"),
		Map.entry("minecraft:zombie_horse", "minecraft:zombie_horse_spawn_egg"),
		Map.entry("minecraft:zombie_pigman", "minecraft:zombie_pigman_spawn_egg"),
		Map.entry("minecraft:zombie_villager", "minecraft:zombie_villager_spawn_egg")
	);

	public ItemInstanceSpawnEggFix(Schema outputSchema, boolean changesType, String spawnEggId) {
		super(outputSchema, changesType);
		this.spawnEggId = spawnEggId;
	}

	public TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.ITEM_STACK);
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		OpticFinder<String> opticFinder2 = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
		OpticFinder<?> opticFinder3 = type.findField("tag");
		OpticFinder<?> opticFinder4 = opticFinder3.type().findField("EntityTag");
		return fixTypeEverywhereTyped(
				"ItemInstanceSpawnEggFix" + getOutputSchema().getVersionKey(),
				type,
				stack -> {
					Optional<Pair<String, String>> optional = stack.getOptional(opticFinder);
					if (optional.isPresent() && Objects.equals(optional.get().getSecond(), this.spawnEggId)) {
						Typed<?> typed = stack.getOrCreateTyped(opticFinder3);
						Typed<?> typed2 = typed.getOrCreateTyped(opticFinder4);
						Optional<String> optional2 = typed2.getOptional(opticFinder2);
						if (optional2.isPresent()) {
							return stack.set(
									opticFinder,
									Pair.of(
											TypeReferences.ITEM_NAME.typeName(),
											ENTITY_SPAWN_EGGS.getOrDefault(optional2.get(), "minecraft:pig_spawn_egg")
									)
							);
						}
					}

					return stack;
				}
		);
	}
}
