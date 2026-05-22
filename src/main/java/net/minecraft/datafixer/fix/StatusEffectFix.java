package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Исправляет данные в формате DataFixer.
 */
public class StatusEffectFix extends DataFix {

	private static final Int2ObjectMap<String> OLD_TO_NEW_IDS = buildOldToNewIdsMap();
	private static final Set<String> POTION_ITEM_IDS = Set.of(
			"minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
	);

	public StatusEffectFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	private static <T> Optional<Dynamic<T>> updateId(Dynamic<T> dynamic, String idKey) {
		return dynamic
				.get(idKey)
				.asNumber()
				.result()
				.map(oldId -> (String) OLD_TO_NEW_IDS.get(oldId.intValue()))
				.map(dynamic::createString);
	}

	private static <T> Dynamic<T> renameKeyAndUpdateId(
			Dynamic<T> dynamic,
			String oldKey,
			Dynamic<T> dynamic2,
			String newKey
	) {
		Optional<Dynamic<T>> optional = updateId(dynamic, oldKey);
		return dynamic2.replaceField(oldKey, newKey, optional);
	}

	private static <T> Dynamic<T> renameKeyAndUpdateId(Dynamic<T> dynamic, String oldKey, String newKey) {
		return renameKeyAndUpdateId(dynamic, oldKey, dynamic, newKey);
	}

	private static <T> Dynamic<T> fixEffect(Dynamic<T> effectDynamic) {
		effectDynamic = renameKeyAndUpdateId(effectDynamic, "Id", "id");
		effectDynamic = effectDynamic.renameField("Ambient", "ambient");
		effectDynamic = effectDynamic.renameField("Amplifier", "amplifier");
		effectDynamic = effectDynamic.renameField("Duration", "duration");
		effectDynamic = effectDynamic.renameField("ShowParticles", "show_particles");
		effectDynamic = effectDynamic.renameField("ShowIcon", "show_icon");
		Optional<Dynamic<T>> optional = effectDynamic.get("HiddenEffect").result().map(StatusEffectFix::fixEffect);
		return effectDynamic.replaceField("HiddenEffect", "hidden_effect", optional);
	}

	private static <T> Dynamic<T> fixEffectList(Dynamic<T> dynamic, String oldEffectListKey, String newEffectListKey) {
		Optional<Dynamic<T>> optional = dynamic.get(oldEffectListKey)
		                                       .asStreamOpt()
		                                       .result()
		                                       .map(oldEffects -> dynamic.createList(oldEffects.map(StatusEffectFix::fixEffect)));
		return dynamic.replaceField(oldEffectListKey, newEffectListKey, optional);
	}

	private static <T> Dynamic<T> fixSuspiciousStewEffect(Dynamic<T> effectDynamicIn, Dynamic<T> effectDynamicOut) {
		effectDynamicOut = renameKeyAndUpdateId(effectDynamicIn, "EffectId", effectDynamicOut, "id");
		Optional<Dynamic<T>> optional = effectDynamicIn.get("EffectDuration").result();
		return effectDynamicOut.replaceField("EffectDuration", "duration", optional);
	}

	private static <T> Dynamic<T> fixSuspiciousStewEffect(Dynamic<T> effectDynamic) {
		return fixSuspiciousStewEffect(effectDynamic, effectDynamic);
	}

	private Typed<?> fixEntityEffects(
			Typed<?> entityTyped,
			TypeReference entityTypeReference,
			String entityId,
			Function<Dynamic<?>, Dynamic<?>> effectsFixer
	) {
		Type<?> type = getInputSchema().getChoiceType(entityTypeReference, entityId);
		Type<?> type2 = getOutputSchema().getChoiceType(entityTypeReference, entityId);
		return entityTyped.updateTyped(
				DSL.namedChoice(entityId, type),
				type2,
				matchingEntityTyped -> matchingEntityTyped.update(DSL.remainderFinder(), effectsFixer)
		);
	}

	private TypeRewriteRule makeBlockEntitiesRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		return fixTypeEverywhereTyped(
				"BlockEntityMobEffectIdFix", type, typed -> this.fixEntityEffects(
						typed, TypeReferences.BLOCK_ENTITY, "minecraft:beacon", dynamic -> {
							dynamic = renameKeyAndUpdateId(dynamic, "Primary", "primary_effect");
							return renameKeyAndUpdateId(dynamic, "Secondary", "secondary_effect");
						}
				)
		);
	}

	private static <T> Dynamic<T> fixStewEffectsKey(Dynamic<T> dynamic) {
		Dynamic<T> dynamic2 = dynamic.emptyMap();
		Dynamic<T> dynamic3 = fixSuspiciousStewEffect(dynamic, dynamic2);
		if (!dynamic3.equals(dynamic2)) {
			dynamic = dynamic.set("stew_effects", dynamic.createList(Stream.of(dynamic3)));
		}

		return dynamic.remove("EffectId").remove("EffectDuration");
	}

	private static <T> Dynamic<T> fixCustomPotionEffectsKey(Dynamic<T> dynamic) {
		return fixEffectList(dynamic, "CustomPotionEffects", "custom_potion_effects");
	}

	private static <T> Dynamic<T> fixEffectsKey(Dynamic<T> dynamic) {
		return fixEffectList(dynamic, "Effects", "effects");
	}

	private static Dynamic<?> fixActiveEffectsKey(Dynamic<?> dynamic) {
		return fixEffectList(dynamic, "ActiveEffects", "active_effects");
	}

	private TypeRewriteRule makeEntitiesRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.ENTITY);
		return fixTypeEverywhereTyped(
				"EntityMobEffectIdFix", type, entityTyped -> {
					entityTyped =
							this.fixEntityEffects(
									entityTyped,
									TypeReferences.ENTITY,
									"minecraft:mooshroom",
									StatusEffectFix::fixStewEffectsKey
							);
					entityTyped =
							this.fixEntityEffects(
									entityTyped,
									TypeReferences.ENTITY,
									"minecraft:arrow",
									StatusEffectFix::fixCustomPotionEffectsKey
							);
					entityTyped =
							this.fixEntityEffects(
									entityTyped,
									TypeReferences.ENTITY,
									"minecraft:area_effect_cloud",
									StatusEffectFix::fixEffectsKey
							);
					return entityTyped.update(DSL.remainderFinder(), StatusEffectFix::fixActiveEffectsKey);
				}
		);
	}

	private TypeRewriteRule makePlayersRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.PLAYER);
		return fixTypeEverywhereTyped(
				"PlayerMobEffectIdFix",
				type,
				typed -> typed.update(DSL.remainderFinder(), StatusEffectFix::fixActiveEffectsKey)
		);
	}

	private static <T> Dynamic<T> fixSuspiciousStewEffects(Dynamic<T> tagTyped) {
		Optional<Dynamic<T>> optional = tagTyped.get("Effects")
		                                        .asStreamOpt()
		                                        .result()
		                                        .map(effects -> tagTyped.createList(effects.map(StatusEffectFix::fixSuspiciousStewEffect)));
		return tagTyped.replaceField("Effects", "effects", optional);
	}

	private TypeRewriteRule makeItemStacksRule() {
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		Type<?> type = getInputSchema().getType(TypeReferences.ITEM_STACK);
		OpticFinder<?> opticFinder2 = type.findField("tag");
		return fixTypeEverywhereTyped(
				"ItemStackMobEffectIdFix",
				type,
				itemStackTyped -> {
					Optional<Pair<String, String>> optional = itemStackTyped.getOptional(opticFinder);
					if (optional.isPresent()) {
						String string = (String) optional.get().getSecond();
						if (string.equals("minecraft:suspicious_stew")) {
							return itemStackTyped.updateTyped(
									opticFinder2,
									tagTyped -> tagTyped.update(
											DSL.remainderFinder(),
											StatusEffectFix::fixSuspiciousStewEffects
									)
							);
						}

						if (POTION_ITEM_IDS.contains(string)) {
							return itemStackTyped.updateTyped(
									opticFinder2,
									tagTyped -> tagTyped.update(
											DSL.remainderFinder(),
											tagDynamic -> fixEffectList(
													tagDynamic,
													"CustomPotionEffects",
													"custom_potion_effects"
											)
									)
							);
						}
					}

					return itemStackTyped;
				}
		);
	}

	protected TypeRewriteRule makeRule() {
		return TypeRewriteRule.seq(
				this.makeBlockEntitiesRule(),
				new TypeRewriteRule[]{this.makeEntitiesRule(), this.makePlayersRule(), this.makeItemStacksRule()}
		);
	}

	private static Int2ObjectMap<String> buildOldToNewIdsMap() {
		Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<>();
		map.put(1, "minecraft:speed");
		map.put(2, "minecraft:slowness");
		map.put(3, "minecraft:haste");
		map.put(4, "minecraft:mining_fatigue");
		map.put(5, "minecraft:strength");
		map.put(6, "minecraft:instant_health");
		map.put(7, "minecraft:instant_damage");
		map.put(8, "minecraft:jump_boost");
		map.put(9, "minecraft:nausea");
		map.put(10, "minecraft:regeneration");
		map.put(11, "minecraft:resistance");
		map.put(12, "minecraft:fire_resistance");
		map.put(13, "minecraft:water_breathing");
		map.put(14, "minecraft:invisibility");
		map.put(15, "minecraft:blindness");
		map.put(16, "minecraft:night_vision");
		map.put(17, "minecraft:hunger");
		map.put(18, "minecraft:weakness");
		map.put(19, "minecraft:poison");
		map.put(20, "minecraft:wither");
		map.put(21, "minecraft:health_boost");
		map.put(22, "minecraft:absorption");
		map.put(23, "minecraft:saturation");
		map.put(24, "minecraft:glowing");
		map.put(25, "minecraft:levitation");
		map.put(26, "minecraft:luck");
		map.put(27, "minecraft:unluck");
		map.put(28, "minecraft:slow_falling");
		map.put(29, "minecraft:conduit_power");
		map.put(30, "minecraft:dolphins_grace");
		map.put(31, "minecraft:bad_omen");
		map.put(32, "minecraft:hero_of_the_village");
		map.put(33, "minecraft:darkness");
		return map;
	}
}
