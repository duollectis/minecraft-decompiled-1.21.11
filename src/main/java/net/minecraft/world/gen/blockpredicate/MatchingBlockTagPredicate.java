package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3i;

/**
 * Предикат, проверяющий, принадлежит ли блок по смещённой позиции заданному тегу блоков.
 */
public class MatchingBlockTagPredicate extends OffsetPredicate {

	public static final MapCodec<MatchingBlockTagPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> registerOffsetField(instance)
			.and(
				TagKey
					.unprefixedCodec(RegistryKeys.BLOCK)
					.fieldOf("tag")
					.forGetter(predicate -> predicate.tag)
			)
			.apply(instance, MatchingBlockTagPredicate::new)
	);

	final TagKey<Block> tag;

	protected MatchingBlockTagPredicate(Vec3i offset, TagKey<Block> tag) {
		super(offset);
		this.tag = tag;
	}

	@Override
	protected boolean test(BlockState state) {
		return state.isIn(tag);
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.MATCHING_BLOCK_TAG;
	}
}
