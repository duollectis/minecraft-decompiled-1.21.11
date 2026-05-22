package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextParameter;

import java.util.List;
import java.util.Set;

/**
 * Функция лута, добавляющая случайный эффект статуса в подозрительное рагу.
 * Для мгновенных эффектов длительность не умножается на 20 (тики).
 */
public class SetStewEffectLootFunction extends ConditionalLootFunction {

	private static final Codec<List<SetStewEffectLootFunction.StewEffect>> STEW_EFFECT_LIST_CODEC =
		SetStewEffectLootFunction.StewEffect.CODEC
			.listOf()
			.validate(stewEffects -> {
				Set<RegistryEntry<StatusEffect>> seen = new ObjectOpenHashSet<>();
				for (SetStewEffectLootFunction.StewEffect stewEffect : stewEffects) {
					if (!seen.add(stewEffect.effect())) {
						return DataResult.error(() -> "Encountered duplicate mob effect: '" + stewEffect.effect() + "'");
					}
				}
				return DataResult.success(stewEffects);
			});

	public static final MapCodec<SetStewEffectLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(STEW_EFFECT_LIST_CODEC
				.optionalFieldOf("effects", List.of())
				.forGetter(function -> function.stewEffects))
			.apply(instance, SetStewEffectLootFunction::new)
	);

	private final List<SetStewEffectLootFunction.StewEffect> stewEffects;

	SetStewEffectLootFunction(List<LootCondition> conditions, List<SetStewEffectLootFunction.StewEffect> stewEffects) {
		super(conditions);
		this.stewEffects = stewEffects;
	}

	@Override
	public LootFunctionType<SetStewEffectLootFunction> getType() {
		return LootFunctionTypes.SET_STEW_EFFECT;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return stewEffects
			.stream()
			.flatMap(stewEffect -> stewEffect.duration().getAllowedParameters().stream())
			.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (!stack.isOf(Items.SUSPICIOUS_STEW) || stewEffects.isEmpty()) {
			return stack;
		}

		SetStewEffectLootFunction.StewEffect chosen = Util.getRandom(stewEffects, context.getRandom());
		RegistryEntry<StatusEffect> effect = chosen.effect();
		int durationTicks = chosen.duration().nextInt(context);
		if (!effect.value().isInstant()) {
			durationTicks *= 20;
		}

		SuspiciousStewEffectsComponent.StewEffect componentEffect =
			new SuspiciousStewEffectsComponent.StewEffect(effect, durationTicks);
		stack.apply(
			DataComponentTypes.SUSPICIOUS_STEW_EFFECTS,
			SuspiciousStewEffectsComponent.DEFAULT,
			componentEffect,
			SuspiciousStewEffectsComponent::with
		);
		return stack;
	}

	public static SetStewEffectLootFunction.Builder builder() {
		return new SetStewEffectLootFunction.Builder();
	}

	/** Строитель функции установки эффекта подозрительного рагу. */
	public static class Builder extends ConditionalLootFunction.Builder<SetStewEffectLootFunction.Builder> {

		private final ImmutableList.Builder<SetStewEffectLootFunction.StewEffect> effects = ImmutableList.builder();

		@Override
		protected SetStewEffectLootFunction.Builder getThisBuilder() {
			return this;
		}

		public SetStewEffectLootFunction.Builder withEffect(
			RegistryEntry<StatusEffect> effect,
			LootNumberProvider durationRange
		) {
			effects.add(new SetStewEffectLootFunction.StewEffect(effect, durationRange));
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetStewEffectLootFunction(getConditions(), effects.build());
		}
	}

	/** Пара эффект-длительность для подозрительного рагу. */
	record StewEffect(RegistryEntry<StatusEffect> effect, LootNumberProvider duration) {

		public static final Codec<SetStewEffectLootFunction.StewEffect> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				StatusEffect.ENTRY_CODEC
					.fieldOf("type")
					.forGetter(SetStewEffectLootFunction.StewEffect::effect),
				LootNumberProviderTypes.CODEC
					.fieldOf("duration")
					.forGetter(SetStewEffectLootFunction.StewEffect::duration)
			)
			.apply(instance, SetStewEffectLootFunction.StewEffect::new)
		);
	}
}
