package net.minecraft.loot.condition;

import com.mojang.serialization.Codec;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextAware;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.function.Predicate;

/**
 * {@code LootCondition}.
 */
public interface LootCondition extends LootContextAware, Predicate<LootContext> {

	Codec<LootCondition>
			BASE_CODEC =
			Registries.LOOT_CONDITION_TYPE
					.getCodec()
					.dispatch("condition", LootCondition::getType, LootConditionType::codec);

	Codec<LootCondition>
			CODEC =
			Codec.lazyInitialized(() -> Codec.withAlternative(BASE_CODEC, AllOfLootCondition.INLINE_CODEC));

	Codec<RegistryEntry<LootCondition>> ENTRY_CODEC = RegistryElementCodec.of(RegistryKeys.PREDICATE, CODEC);

	LootConditionType getType();

	@FunctionalInterface
	/**
	 * {@code Builder}.
	 */
	public interface Builder {

		LootCondition build();

		default LootCondition.Builder invert() {
			return InvertedLootCondition.builder(this);
		}

		default AnyOfLootCondition.Builder or(LootCondition.Builder condition) {
			return AnyOfLootCondition.builder(this, condition);
		}

		default AllOfLootCondition.Builder and(LootCondition.Builder condition) {
			return AllOfLootCondition.builder(this, condition);
		}
	}
}
