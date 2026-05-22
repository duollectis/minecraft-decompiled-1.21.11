package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.predicate.component.ComponentSubPredicate;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import java.util.Optional;

/**
 * Предикат для проверки компонента воспроизводимой пластинки (jukebox).
 * Проверяет, входит ли песня предмета в заданный список.
 */
public record JukeboxPlayablePredicate(Optional<RegistryEntryList<JukeboxSong>> song) implements ComponentSubPredicate<JukeboxPlayableComponent> {

	public static final Codec<JukeboxPlayablePredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(
							RegistryCodecs.entryList(RegistryKeys.JUKEBOX_SONG)
									.optionalFieldOf("song")
									.forGetter(JukeboxPlayablePredicate::song)
					)
					.apply(instance, JukeboxPlayablePredicate::new)
	);

	public static JukeboxPlayablePredicate empty() {
		return new JukeboxPlayablePredicate(Optional.empty());
	}

	@Override
	public ComponentType<JukeboxPlayableComponent> getComponentType() {
		return DataComponentTypes.JUKEBOX_PLAYABLE;
	}

	public boolean test(JukeboxPlayableComponent component) {
		if (song.isEmpty()) {
			return true;
		}

		Optional<RegistryKey<JukeboxSong>> componentSongKey = component.song().getKey();

		for (RegistryEntry<JukeboxSong> entry : song.get()) {
			Optional<RegistryKey<JukeboxSong>> entryKey = entry.getKey();

			if (entryKey.isPresent() && entryKey.equals(componentSongKey)) {
				return true;
			}
		}

		return false;
	}
}
