package net.minecraft.resource;

import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.metadata.ResourceMetadata;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Представляет один ресурс из ресурс-пака: поток данных и опциональные метаданные.
 * Метаданные загружаются лениво при первом обращении.
 */
public class Resource {

	private final ResourcePack pack;
	private final InputSupplier<InputStream> inputSupplier;
	private final InputSupplier<ResourceMetadata> metadataSupplier;
	private @Nullable ResourceMetadata metadata;

	public Resource(
		ResourcePack pack,
		InputSupplier<InputStream> inputSupplier,
		InputSupplier<ResourceMetadata> metadataSupplier
	) {
		this.pack = pack;
		this.inputSupplier = inputSupplier;
		this.metadataSupplier = metadataSupplier;
	}

	/** Создаёт ресурс без метаданных. */
	public Resource(ResourcePack pack, InputSupplier<InputStream> inputSupplier) {
		this.pack = pack;
		this.inputSupplier = inputSupplier;
		metadataSupplier = ResourceMetadata.NONE_SUPPLIER;
		metadata = ResourceMetadata.NONE;
	}

	public ResourcePack getPack() {
		return pack;
	}

	public String getPackId() {
		return pack.getId();
	}

	public Optional<VersionedIdentifier> getKnownPackInfo() {
		return pack.getKnownPackInfo();
	}

	public InputStream getInputStream() throws IOException {
		return inputSupplier.get();
	}

	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
	}

	/**
	 * Возвращает метаданные ресурса, загружая их при первом обращении.
	 *
	 * @return метаданные ресурса
	 * @throws IOException при ошибке чтения метаданных
	 */
	public ResourceMetadata getMetadata() throws IOException {
		if (metadata == null) {
			metadata = metadataSupplier.get();
		}

		return metadata;
	}
}
