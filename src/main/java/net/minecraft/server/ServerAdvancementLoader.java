package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import net.minecraft.advancement.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

/**
 * {@code ServerAdvancementLoader}.
 */
public class ServerAdvancementLoader extends JsonDataLoader<Advancement> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private Map<Identifier, AdvancementEntry> advancements = Map.of();
	private AdvancementManager manager = new AdvancementManager();
	private final RegistryWrapper.WrapperLookup registries;

	public ServerAdvancementLoader(RegistryWrapper.WrapperLookup registries) {
		super(registries, Advancement.CODEC, RegistryKeys.ADVANCEMENT);
		this.registries = registries;
	}

	/**
	 * Apply.
	 *
	 * @param map map
	 * @param resourceManager resource manager
	 * @param profiler profiler
	 */
	protected void apply(Map<Identifier, Advancement> map, ResourceManager resourceManager, Profiler profiler) {
		Builder<Identifier, AdvancementEntry> builder = ImmutableMap.builder();
		map.forEach((id, advancement) -> {
			this.validate(id, advancement);
			builder.put(id, new AdvancementEntry(id, advancement));
		});
		this.advancements = builder.buildOrThrow();
		AdvancementManager advancementManager = new AdvancementManager();
		advancementManager.addAll(this.advancements.values());

		for (PlacedAdvancement placedAdvancement : advancementManager.getRoots()) {
			if (placedAdvancement.getAdvancementEntry().value().display().isPresent()) {
				AdvancementPositioner.arrangeForTree(placedAdvancement);
			}
		}

		this.manager = advancementManager;
	}

	private void validate(Identifier id, Advancement advancement) {
		ErrorReporter.Impl impl = new ErrorReporter.Impl();
		advancement.validate(impl, this.registries);
		if (!impl.isEmpty()) {
			LOGGER.warn("Found validation problems in advancement {}: \n{}", id, impl.getErrorsAsString());
		}
	}

	/**
	 * Get.
	 *
	 * @param id id
	 *
	 * @return @Nullable AdvancementEntry — 
	 */
	public @Nullable AdvancementEntry get(Identifier id) {
		return this.advancements.get(id);
	}

	public AdvancementManager getManager() {
		return this.manager;
	}

	public Collection<AdvancementEntry> getAdvancements() {
		return this.advancements.values();
	}
}
