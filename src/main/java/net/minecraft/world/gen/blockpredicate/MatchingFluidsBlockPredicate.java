package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.Vec3i;

/**
 * Предикат, проверяющий, содержит ли блок по смещённой позиции один из заданных флюидов.
 */
class MatchingFluidsBlockPredicate extends OffsetPredicate {

	public static final MapCodec<MatchingFluidsBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> registerOffsetField(instance)
			.and(
				RegistryCodecs
					.entryList(RegistryKeys.FLUID)
					.fieldOf("fluids")
					.forGetter(predicate -> predicate.fluids)
			)
			.apply(instance, MatchingFluidsBlockPredicate::new)
	);

	private final RegistryEntryList<Fluid> fluids;

	public MatchingFluidsBlockPredicate(Vec3i offset, RegistryEntryList<Fluid> fluids) {
		super(offset);
		this.fluids = fluids;
	}

	@Override
	protected boolean test(BlockState state) {
		return state.getFluidState().isIn(fluids);
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.MATCHING_FLUIDS;
	}
}
