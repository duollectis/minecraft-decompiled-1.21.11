package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.Set;

/**
 * Условие, проверяющее свойства сущности через {@link EntityPredicate}.
 *
 * <p>Тип сущности задаётся через {@link LootContext.EntityReference} (например, THIS, KILLER).
 * Если предикат отсутствует — условие выполняется при наличии сущности в контексте.</p>
 */
public record EntityPropertiesLootCondition(
	Optional<EntityPredicate> predicate,
	LootContext.EntityReference entity
) implements LootCondition {

	public static final MapCodec<EntityPropertiesLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(EntityPropertiesLootCondition::predicate),
			LootContext.EntityReference.CODEC.fieldOf("entity").forGetter(EntityPropertiesLootCondition::entity)
		)
		.apply(instance, EntityPropertiesLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.ENTITY_PROPERTIES;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.ORIGIN, entity.contextParam());
	}

	public boolean test(LootContext lootContext) {
		Entity target = lootContext.get(entity.contextParam());
		Vec3d origin = lootContext.get(LootContextParameters.ORIGIN);
		return predicate.isEmpty() || predicate.get().test(lootContext.getWorld(), origin, target);
	}

	public static LootCondition.Builder create(LootContext.EntityReference entity) {
		return builder(entity, EntityPredicate.Builder.create());
	}

	public static LootCondition.Builder builder(
		LootContext.EntityReference entity,
		EntityPredicate.Builder predicateBuilder
	) {
		return () -> new EntityPropertiesLootCondition(Optional.of(predicateBuilder.build()), entity);
	}

	public static LootCondition.Builder builder(LootContext.EntityReference entity, EntityPredicate predicate) {
		return () -> new EntityPropertiesLootCondition(Optional.of(predicate), entity);
	}
}
