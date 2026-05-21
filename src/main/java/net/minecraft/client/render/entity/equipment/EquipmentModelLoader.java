package net.minecraft.client.render.entity.equipment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code EquipmentModelLoader}.
 */
public class EquipmentModelLoader extends JsonDataLoader<EquipmentModel> {

	public static final EquipmentModel EMPTY = new EquipmentModel(Map.of());
	private static final ResourceFinder FINDER = ResourceFinder.json("equipment");
	private Map<RegistryKey<EquipmentAsset>, EquipmentModel> models = Map.of();

	public EquipmentModelLoader() {
		super(EquipmentModel.CODEC, FINDER);
	}

	/**
	 * Apply.
	 *
	 * @param map map
	 * @param resourceManager resource manager
	 * @param profiler profiler
	 */
	protected void apply(Map<Identifier, EquipmentModel> map, ResourceManager resourceManager, Profiler profiler) {
		this.models = map.entrySet()
		                 .stream()
		                 .collect(Collectors.toUnmodifiableMap(
				                 entry -> RegistryKey.of(
						                 EquipmentAssetKeys.REGISTRY_KEY,
						                 entry.getKey()
				                 ), Entry::getValue
		                 ));
	}

	/**
	 * Get.
	 *
	 * @param assetKey asset key
	 *
	 * @return EquipmentModel — 
	 */
	public EquipmentModel get(RegistryKey<EquipmentAsset> assetKey) {
		return this.models.getOrDefault(assetKey, EMPTY);
	}
}
