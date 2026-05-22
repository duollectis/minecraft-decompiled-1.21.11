package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Провайдер данных для генерации JSON-определений моделей снаряжения (брони, сёдел,
 * элитр и прочего). Каждый ключ {@link EquipmentAsset} получает свой файл с описанием
 * слоёв рендеринга для различных типов существ.
 */
@Environment(EnvType.CLIENT)
public class EquipmentAssetProvider implements DataProvider {

	private final DataOutput.PathResolver pathResolver;

	public EquipmentAssetProvider(DataOutput output) {
		pathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "equipment");
	}

	private static void bootstrap(BiConsumer<RegistryKey<EquipmentAsset>, EquipmentModel> equipmentBiConsumer) {
		equipmentBiConsumer.accept(
				EquipmentAssetKeys.LEATHER,
				EquipmentModel.builder()
				              .addHumanoidLayers(Identifier.ofVanilla("leather"), true)
				              .addHumanoidLayers(Identifier.ofVanilla("leather_overlay"), false)
				              .addLayers(
						              EquipmentModel.LayerType.HORSE_BODY,
						              EquipmentModel.Layer.createWithLeatherColor(
								              Identifier.ofVanilla("leather"),
								              true
						              ),
						              EquipmentModel.Layer.createWithLeatherColor(
								              Identifier.ofVanilla("leather_overlay"),
								              false
						              )
				              )
				              .build()
		);
		equipmentBiConsumer.accept(EquipmentAssetKeys.CHAINMAIL, createHumanoidOnlyModel("chainmail"));
		equipmentBiConsumer.accept(EquipmentAssetKeys.COPPER, createHumanoidAndHorseModel("copper"));
		equipmentBiConsumer.accept(EquipmentAssetKeys.IRON, createHumanoidAndHorseModel("iron"));
		equipmentBiConsumer.accept(EquipmentAssetKeys.GOLD, createHumanoidAndHorseModel("gold"));
		equipmentBiConsumer.accept(EquipmentAssetKeys.DIAMOND, createHumanoidAndHorseModel("diamond"));
		equipmentBiConsumer.accept(
				EquipmentAssetKeys.TURTLE_SCUTE,
				EquipmentModel.builder().addMainHumanoidLayer(Identifier.ofVanilla("turtle_scute"), false).build()
		);
		equipmentBiConsumer.accept(EquipmentAssetKeys.NETHERITE, createHumanoidAndHorseModel("netherite"));
		equipmentBiConsumer.accept(
				EquipmentAssetKeys.ARMADILLO_SCUTE,
				EquipmentModel.builder()
				              .addLayers(
						              EquipmentModel.LayerType.WOLF_BODY,
						              EquipmentModel.Layer.create(Identifier.ofVanilla("armadillo_scute"), false)
				              )
				              .addLayers(
						              EquipmentModel.LayerType.WOLF_BODY,
						              EquipmentModel.Layer.create(Identifier.ofVanilla("armadillo_scute_overlay"), true)
				              )
				              .build()
		);
		equipmentBiConsumer.accept(
				EquipmentAssetKeys.ELYTRA,
				EquipmentModel.builder()
				              .addLayers(
						              EquipmentModel.LayerType.WINGS,
						              new EquipmentModel.Layer(Identifier.ofVanilla("elytra"), Optional.empty(), true)
				              )
				              .build()
		);
		EquipmentModel.Layer layer = new EquipmentModel.Layer(Identifier.ofVanilla("saddle"));
		equipmentBiConsumer.accept(
				EquipmentAssetKeys.SADDLE,
				EquipmentModel.builder()
				              .addLayers(EquipmentModel.LayerType.PIG_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.STRIDER_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.CAMEL_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.CAMEL_HUSK_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.HORSE_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.DONKEY_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.MULE_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.SKELETON_HORSE_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.ZOMBIE_HORSE_SADDLE, layer)
				              .addLayers(EquipmentModel.LayerType.NAUTILUS_SADDLE, layer)
				              .build()
		);

		for (Entry<DyeColor, RegistryKey<EquipmentAsset>> entry : EquipmentAssetKeys.HARNESS_FROM_COLOR.entrySet()) {
			DyeColor dyeColor = entry.getKey();
			RegistryKey<EquipmentAsset> registryKey = entry.getValue();
			equipmentBiConsumer.accept(
					registryKey,
					EquipmentModel.builder()
					              .addLayers(
							              EquipmentModel.LayerType.HAPPY_GHAST_BODY,
							              EquipmentModel.Layer.create(
									              Identifier.ofVanilla(
											              dyeColor.asString() + "_harness"), false
							              )
					              )
					              .build()
			);
		}

		for (Entry<DyeColor, RegistryKey<EquipmentAsset>> entry : EquipmentAssetKeys.CARPET_FROM_COLOR.entrySet()) {
			DyeColor dyeColor = entry.getKey();
			RegistryKey<EquipmentAsset> registryKey = entry.getValue();
			equipmentBiConsumer.accept(
					registryKey,
					EquipmentModel.builder()
					              .addLayers(
							              EquipmentModel.LayerType.LLAMA_BODY,
							              new EquipmentModel.Layer(Identifier.ofVanilla(dyeColor.asString()))
					              )
					              .build()
			);
		}

		equipmentBiConsumer.accept(
				EquipmentAssetKeys.TRADER_LLAMA,
				EquipmentModel
						.builder()
						.addLayers(
								EquipmentModel.LayerType.LLAMA_BODY,
								new EquipmentModel.Layer(Identifier.ofVanilla("trader_llama"))
						)
						.build()
		);
	}

	private static EquipmentModel createHumanoidOnlyModel(String id) {
		return EquipmentModel.builder().addHumanoidLayers(Identifier.ofVanilla(id)).build();
	}

	private static EquipmentModel createHumanoidAndHorseModel(String id) {
		return EquipmentModel.builder()
		                     .addHumanoidLayers(Identifier.ofVanilla(id))
		                     .addLayers(
				                     EquipmentModel.LayerType.HORSE_BODY,
				                     EquipmentModel.Layer.createWithLeatherColor(Identifier.ofVanilla(id), false)
		                     )
		                     .addLayers(
				                     EquipmentModel.LayerType.NAUTILUS_BODY,
				                     EquipmentModel.Layer.createWithLeatherColor(Identifier.ofVanilla(id), false)
		                     )
		                     .build();
	}

	/**
	 * Запускает генерацию всех определений снаряжения.
	 *
	 * @param writer писатель данных
	 * @return будущее записи всех файлов
	 */
	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Map<RegistryKey<EquipmentAsset>, EquipmentModel> map = new HashMap<>();
		bootstrap((key, model) -> {
			if (map.putIfAbsent(key, model) != null) {
				throw new IllegalStateException("Tried to register equipment asset twice for id: " + key);
			}
		});
		return DataProvider.writeAllToPath(writer, EquipmentModel.CODEC, pathResolver::resolveJson, map);
	}

	@Override
	public String getName() {
		return "Equipment Asset Definitions";
	}
}
