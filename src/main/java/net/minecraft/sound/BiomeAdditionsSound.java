package net.minecraft.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;

public record BiomeAdditionsSound(RegistryEntry<SoundEvent> sound, double tickChance) {
   public static final Codec<BiomeAdditionsSound> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(sound -> sound.sound), Codec.DOUBLE.fieldOf("tick_chance").forGetter(sound -> sound.tickChance)
         )
         .apply(instance, BiomeAdditionsSound::new)
   );
}
