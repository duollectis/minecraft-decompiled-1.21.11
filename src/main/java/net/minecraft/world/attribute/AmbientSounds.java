package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BiomeAdditionsSound;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;

/**
 * Описывает набор фоновых звуков окружения биома: зацикленный звук,
 * звук настроения (mood) и список дополнительных звуков (additions).
 */
public record AmbientSounds(
	Optional<RegistryEntry<SoundEvent>> loop,
	Optional<BiomeMoodSound> mood,
	List<BiomeAdditionsSound> additions
) {

	/** Пустой набор звуков — тишина. */
	public static final AmbientSounds DEFAULT = new AmbientSounds(Optional.empty(), Optional.empty(), List.of());

	/** Стандартный набор звуков пещеры с mood-звуком {@link BiomeMoodSound#CAVE}. */
	public static final AmbientSounds CAVE = new AmbientSounds(Optional.empty(), Optional.of(BiomeMoodSound.CAVE), List.of());

	public static final Codec<AmbientSounds> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			SoundEvent.ENTRY_CODEC.optionalFieldOf("loop").forGetter(AmbientSounds::loop),
			BiomeMoodSound.CODEC.optionalFieldOf("mood").forGetter(AmbientSounds::mood),
			Codecs.listOrSingle(BiomeAdditionsSound.CODEC)
				.optionalFieldOf("additions", List.of())
				.forGetter(AmbientSounds::additions)
		).apply(instance, AmbientSounds::new)
	);
}
