package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public record SheepPredicate(Optional<Boolean> sheared) implements EntitySubPredicate {
   public static final MapCodec<SheepPredicate> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.BOOL.optionalFieldOf("sheared").forGetter(SheepPredicate::sheared)).apply(instance, SheepPredicate::new)
   );

   @Override
   public MapCodec<SheepPredicate> getCodec() {
      return EntitySubPredicateTypes.SHEEP;
   }

   @Override
   public boolean test(Entity entity, ServerWorld world, @Nullable Vec3d pos) {
      return entity instanceof SheepEntity sheepEntity ? !this.sheared.isPresent() || sheepEntity.isSheared() == this.sheared.get() : false;
   }

   public static SheepPredicate unsheared() {
      return new SheepPredicate(Optional.of(false));
   }
}
