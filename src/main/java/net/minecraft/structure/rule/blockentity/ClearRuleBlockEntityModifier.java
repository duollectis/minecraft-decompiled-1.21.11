package net.minecraft.structure.rule.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Реализация {@link RuleBlockEntityModifier}, полностью очищающая NBT блок-сущности.
 * Возвращает пустой {@link NbtCompound} независимо от входных данных.
 */
public class ClearRuleBlockEntityModifier implements RuleBlockEntityModifier {

	private static final ClearRuleBlockEntityModifier INSTANCE = new ClearRuleBlockEntityModifier();
	public static final MapCodec<ClearRuleBlockEntityModifier> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
		return new NbtCompound();
	}

	@Override
	public RuleBlockEntityModifierType<?> getType() {
		return RuleBlockEntityModifierType.CLEAR;
	}
}
