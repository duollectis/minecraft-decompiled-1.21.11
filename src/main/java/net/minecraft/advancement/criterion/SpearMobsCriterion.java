package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.dynamic.Codecs;

public class SpearMobsCriterion extends AbstractCriterion<SpearMobsCriterion.Conditions> {
   @Override
   public Codec<SpearMobsCriterion.Conditions> getConditionsCodec() {
      return SpearMobsCriterion.Conditions.CODEC;
   }

   public void trigger(ServerPlayerEntity player, int count) {
      this.trigger(player, conditions -> conditions.test(count));
   }

   public record Conditions(Optional<LootContextPredicate> player, Optional<Integer> count) implements AbstractCriterion.Conditions {
      public static final Codec<SpearMobsCriterion.Conditions> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(SpearMobsCriterion.Conditions::player),
               Codecs.POSITIVE_INT.optionalFieldOf("count").forGetter(SpearMobsCriterion.Conditions::count)
            )
            .apply(instance, SpearMobsCriterion.Conditions::new)
      );

      public static AdvancementCriterion<SpearMobsCriterion.Conditions> method_76462(int i) {
         return Criteria.SPEAR_MOBS.create(new SpearMobsCriterion.Conditions(Optional.empty(), Optional.of(i)));
      }

      public boolean test(int count) {
         return this.count.isEmpty() || count >= this.count.get();
      }
   }
}
