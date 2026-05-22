package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.entity.DamageSourcePredicate;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.Set;

/**
 * Условие, проверяющее источник урона через {@link DamageSourcePredicate}.
 *
 * <p>Требует параметры {@link LootContextParameters#ORIGIN} и
 * {@link LootContextParameters#DAMAGE_SOURCE}. Если оба отсутствуют — условие не выполняется.</p>
 */
public record DamageSourcePropertiesLootCondition(Optional<DamageSourcePredicate> predicate) implements LootCondition {

	public static final MapCodec<DamageSourcePropertiesLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				DamageSourcePredicate.CODEC
					.optionalFieldOf("predicate")
					.forGetter(DamageSourcePropertiesLootCondition::predicate)
			)
			.apply(instance, DamageSourcePropertiesLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.DAMAGE_SOURCE_PROPERTIES;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.ORIGIN, LootContextParameters.DAMAGE_SOURCE);
	}

	public boolean test(LootContext lootContext) {
		DamageSource damageSource = lootContext.get(LootContextParameters.DAMAGE_SOURCE);
		Vec3d origin = lootContext.get(LootContextParameters.ORIGIN);

		if (damageSource == null || origin == null) {
			return false;
		}

		return predicate.isEmpty() || predicate.get().test(lootContext.getWorld(), origin, damageSource);
	}

	public static LootCondition.Builder builder(DamageSourcePredicate.Builder builder) {
		return () -> new DamageSourcePropertiesLootCondition(Optional.of(builder.build()));
	}
}
