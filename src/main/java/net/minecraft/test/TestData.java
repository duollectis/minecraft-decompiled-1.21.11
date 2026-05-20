package net.minecraft.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public record TestData<EnvironmentType>(
   EnvironmentType environment,
   Identifier structure,
   int maxTicks,
   int setupTicks,
   boolean required,
   BlockRotation rotation,
   boolean manualOnly,
   int maxAttempts,
   int requiredSuccesses,
   boolean skyAccess
) {
   public static final MapCodec<TestData<RegistryEntry<TestEnvironmentDefinition>>> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            TestEnvironmentDefinition.ENTRY_CODEC.fieldOf("environment").forGetter(TestData::environment),
            Identifier.CODEC.fieldOf("structure").forGetter(TestData::structure),
            Codecs.POSITIVE_INT.fieldOf("max_ticks").forGetter(TestData::maxTicks),
            Codecs.NON_NEGATIVE_INT.optionalFieldOf("setup_ticks", 0).forGetter(TestData::setupTicks),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(TestData::required),
            BlockRotation.CODEC.optionalFieldOf("rotation", BlockRotation.NONE).forGetter(TestData::rotation),
            Codec.BOOL.optionalFieldOf("manual_only", false).forGetter(TestData::manualOnly),
            Codecs.POSITIVE_INT.optionalFieldOf("max_attempts", 1).forGetter(TestData::maxAttempts),
            Codecs.POSITIVE_INT.optionalFieldOf("required_successes", 1).forGetter(TestData::requiredSuccesses),
            Codec.BOOL.optionalFieldOf("sky_access", false).forGetter(TestData::skyAccess)
         )
         .apply(instance, TestData::new)
   );

   public TestData(EnvironmentType environment, Identifier structure, int maxTicks, int setupTicks, boolean required, BlockRotation rotation) {
      this(environment, structure, maxTicks, setupTicks, required, rotation, false, 1, 1, false);
   }

   public TestData(EnvironmentType environment, Identifier structure, int maxTicks, int setupTicks, boolean required) {
      this(environment, structure, maxTicks, setupTicks, required, BlockRotation.NONE);
   }

   public <T> TestData<T> applyToEnvironment(Function<EnvironmentType, T> environmentFunction) {
      return new TestData<>(
         environmentFunction.apply(this.environment),
         this.structure,
         this.maxTicks,
         this.setupTicks,
         this.required,
         this.rotation,
         this.manualOnly,
         this.maxAttempts,
         this.requiredSuccesses,
         this.skyAccess
      );
   }
}
