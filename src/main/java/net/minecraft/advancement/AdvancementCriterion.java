package net.minecraft.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.util.dynamic.Codecs;

/**
 * {@code AdvancementCriterion}.
 */
public record AdvancementCriterion<T extends CriterionConditions>(Criterion<T> trigger, T conditions) {

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static final MapCodec<AdvancementCriterion<?>> MAP_CODEC = (MapCodec) buildMapCodec();
	public static final Codec<AdvancementCriterion<?>> CODEC = MAP_CODEC.codec();

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <C extends CriterionConditions> MapCodec<AdvancementCriterion<C>> buildMapCodec() {
		return Codecs.parameters(
				"trigger", "conditions",
				(Codec<Criterion<C>>) (Codec) Criteria.CODEC,
				(AdvancementCriterion<C> ac) -> ac.trigger(),
				AdvancementCriterion::getCodec
		);
	}

	@SuppressWarnings("unchecked")
	private static <T extends CriterionConditions> Codec<AdvancementCriterion<T>> getCodec(Criterion<T> criterion) {
		return criterion
				.getConditionsCodec()
				.xmap(
						conditions -> new AdvancementCriterion<>(criterion, (T) conditions),
						AdvancementCriterion::conditions
				);
	}
}
