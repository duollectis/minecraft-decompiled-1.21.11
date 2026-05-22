package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.loot.LootTable;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок генерирует лут из контейнера (сундука, бочки и т.д.),
 * привязанного к конкретной таблице лута.
 */
public class PlayerGeneratesContainerLootCriterion extends AbstractCriterion<PlayerGeneratesContainerLootCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, RegistryKey<LootTable> lootTable) {
		trigger(player, conditions -> conditions.test(lootTable));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			RegistryKey<LootTable> lootTable
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						LootTable.TABLE_KEY
								.fieldOf("loot_table")
								.forGetter(Conditions::lootTable)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(RegistryKey<LootTable> registryKey) {
			return Criteria.PLAYER_GENERATES_CONTAINER_LOOT.create(new Conditions(
					Optional.empty(),
					registryKey
			));
		}

		public boolean test(RegistryKey<LootTable> lootTable) {
			return this.lootTable == lootTable;
		}
	}
}
