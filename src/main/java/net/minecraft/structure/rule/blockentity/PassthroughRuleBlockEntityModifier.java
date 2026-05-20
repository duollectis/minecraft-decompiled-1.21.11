package net.minecraft.structure.rule.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

public class PassthroughRuleBlockEntityModifier implements RuleBlockEntityModifier {
   public static final PassthroughRuleBlockEntityModifier INSTANCE = new PassthroughRuleBlockEntityModifier();
   public static final MapCodec<PassthroughRuleBlockEntityModifier> CODEC = MapCodec.unit(INSTANCE);

   @Override
   public @Nullable NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
      return nbt;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.PASSTHROUGH;
   }
}
