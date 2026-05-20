package net.minecraft.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;

public record BiomeMoodSound(RegistryEntry<SoundEvent> sound, int tickDelay, int blockSearchExtent, double offset) {
   public static final Codec<BiomeMoodSound> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(sound -> sound.sound),
            Codec.INT.fieldOf("tick_delay").forGetter(sound -> sound.tickDelay),
            Codec.INT.fieldOf("block_search_extent").forGetter(sound -> sound.blockSearchExtent),
            Codec.DOUBLE.fieldOf("offset").forGetter(sound -> sound.offset)
         )
         .apply(instance, BiomeMoodSound::new)
   );
   public static final BiomeMoodSound CAVE = new BiomeMoodSound(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0);
}
