package net.minecraft.registry.entry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.VersionedIdentifier;

import java.util.Optional;

/**
 * {@code RegistryEntryInfo}.
 */
public record RegistryEntryInfo(Optional<VersionedIdentifier> knownPackInfo, Lifecycle lifecycle) {

	public static final RegistryEntryInfo DEFAULT = new RegistryEntryInfo(Optional.empty(), Lifecycle.stable());
}
