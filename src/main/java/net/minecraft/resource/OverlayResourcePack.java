package net.minecraft.resource;

import com.google.common.collect.Lists;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ресурс-пак, объединяющий базовый пак и список оверлеев.
 * Оверлеи имеют более высокий приоритет: ресурс ищется сначала в них,
 * затем в базовом паке.
 */
public class OverlayResourcePack implements ResourcePack {

	private final ResourcePack base;
	/** Список паков в порядке убывания приоритета: сначала оверлеи (в обратном порядке), затем базовый. */
	private final List<ResourcePack> overlaysAndBase;

	public OverlayResourcePack(ResourcePack base, List<ResourcePack> overlays) {
		this.base = base;
		List<ResourcePack> ordered = new ArrayList<>(overlays.size() + 1);
		ordered.addAll(Lists.reverse(overlays));
		ordered.add(base);
		overlaysAndBase = List.copyOf(ordered);
	}

	@Override
	public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
		return base.openRoot(segments);
	}

	@Override
	public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
		for (ResourcePack pack : overlaysAndBase) {
			InputSupplier<InputStream> supplier = pack.open(type, id);
			if (supplier != null) {
				return supplier;
			}
		}

		return null;
	}

	@Override
	public void findResources(
		ResourceType type,
		String namespace,
		String prefix,
		ResourcePack.ResultConsumer consumer
	) {
		Map<Identifier, InputSupplier<InputStream>> merged = new HashMap<>();

		for (ResourcePack pack : overlaysAndBase) {
			pack.findResources(type, namespace, prefix, merged::putIfAbsent);
		}

		merged.forEach(consumer);
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		Set<String> namespaces = new HashSet<>();

		for (ResourcePack pack : overlaysAndBase) {
			namespaces.addAll(pack.getNamespaces(type));
		}

		return namespaces;
	}

	@Override
	public <T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException {
		return base.parseMetadata(metadataSerializer);
	}

	@Override
	public ResourcePackInfo getInfo() {
		return base.getInfo();
	}

	@Override
	public void close() {
		overlaysAndBase.forEach(ResourcePack::close);
	}
}
