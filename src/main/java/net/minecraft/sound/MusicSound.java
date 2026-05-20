package net.minecraft.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.dynamic.Codecs;

public record MusicSound(RegistryEntry<SoundEvent> sound, int minDelay, int maxDelay, boolean replaceCurrentMusic) {
   public static final Codec<MusicSound> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(MusicSound::sound),
            Codecs.NON_NEGATIVE_INT.fieldOf("min_delay").forGetter(MusicSound::minDelay),
            Codecs.NON_NEGATIVE_INT.fieldOf("max_delay").forGetter(MusicSound::maxDelay),
            Codec.BOOL.optionalFieldOf("replace_current_music", false).forGetter(MusicSound::replaceCurrentMusic)
         )
         .apply(instance, MusicSound::new)
   );
}
