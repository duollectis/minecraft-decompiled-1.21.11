package net.minecraft.resource;

import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * {@code ResourcePack}.
 */
public interface ResourcePack extends AutoCloseable {

	String METADATA_PATH_SUFFIX = ".mcmeta";

	String PACK_METADATA_NAME = "pack.mcmeta";

	@Nullable InputSupplier<InputStream> openRoot(String... segments);

	@Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id);

	void findResources(ResourceType type, String namespace, String prefix, ResourcePack.ResultConsumer consumer);

	Set<String> getNamespaces(ResourceType type);

	<T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException;

	ResourcePackInfo getInfo();

	default String getId() {
		return this.getInfo().id();
	}

	default Optional<VersionedIdentifier> getKnownPackInfo() {
		return this.getInfo().knownPackInfo();
	}

	@Override
	void close();

	@FunctionalInterface
	/**
	 * {@code ResultConsumer}.
	 */
	public interface ResultConsumer extends BiConsumer<Identifier, InputSupplier<InputStream>> {
	}
}
