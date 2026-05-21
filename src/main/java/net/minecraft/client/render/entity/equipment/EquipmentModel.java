package net.minecraft.client.render.entity.equipment;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

import java.util.*;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code EquipmentModel}.
 */
public record EquipmentModel(Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>> layers) {

	private static final Codec<List<EquipmentModel.Layer>>
			LAYER_LIST_CODEC =
			Codecs.nonEmptyList(EquipmentModel.Layer.CODEC.listOf());
	public static final Codec<EquipmentModel> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codecs
							                    .nonEmptyMap(Codec.unboundedMap(EquipmentModel.LayerType.CODEC, LAYER_LIST_CODEC))
							                    .fieldOf("layers")
							                    .forGetter(EquipmentModel::layers)
			                    )
			                    .apply(instance, EquipmentModel::new)
	);

	public static EquipmentModel.Builder builder() {
		return new EquipmentModel.Builder();
	}

	public List<EquipmentModel.Layer> getLayers(EquipmentModel.LayerType layerType) {
		return this.layers.getOrDefault(layerType, List.of());
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>>
				layers =
				new EnumMap<>(EquipmentModel.LayerType.class);

		Builder() {
		}

		public EquipmentModel.Builder addHumanoidLayers(Identifier textureId) {
			return this.addHumanoidLayers(textureId, false);
		}

		public EquipmentModel.Builder addHumanoidLayers(Identifier textureId, boolean dyeable) {
			this.addLayers(
					EquipmentModel.LayerType.HUMANOID_LEGGINGS,
					EquipmentModel.Layer.createWithLeatherColor(textureId, dyeable)
			);
			this.addMainHumanoidLayer(textureId, dyeable);
			return this;
		}

		public EquipmentModel.Builder addMainHumanoidLayer(Identifier textureId, boolean dyeable) {
			return this.addLayers(
					EquipmentModel.LayerType.HUMANOID,
					EquipmentModel.Layer.createWithLeatherColor(textureId, dyeable)
			);
		}

		public EquipmentModel.Builder addLayers(EquipmentModel.LayerType layerType, EquipmentModel.Layer... layers) {
			Collections.addAll(this.layers.computeIfAbsent(layerType, layerTypex -> new ArrayList<>()), layers);
			return this;
		}

		/**
		 * Build.
		 *
		 * @return EquipmentModel — результат операции
		 */
		public EquipmentModel build() {
			return new EquipmentModel(
					this.layers
							.entrySet()
							.stream()
							.collect(ImmutableMap.toImmutableMap(
									Entry::getKey,
									entry -> List.copyOf((Collection) entry.getValue())
							))
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Dyeable}.
	 */
	public record Dyeable(Optional<Integer> colorWhenUndyed) {

		public static final Codec<EquipmentModel.Dyeable> CODEC = RecordCodecBuilder.create(
				instance -> instance
						.group(Codecs.RGB
								.optionalFieldOf("color_when_undyed")
								.forGetter(EquipmentModel.Dyeable::colorWhenUndyed))
						.apply(instance, EquipmentModel.Dyeable::new)
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Layer}.
	 */
	public record Layer(Identifier textureId, Optional<EquipmentModel.Dyeable> dyeable, boolean usePlayerTexture) {

		public static final Codec<EquipmentModel.Layer> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Identifier.CODEC.fieldOf("texture").forGetter(EquipmentModel.Layer::textureId),
						                    EquipmentModel.Dyeable.CODEC
								                    .optionalFieldOf("dyeable")
								                    .forGetter(EquipmentModel.Layer::dyeable),
						                    Codec.BOOL
								                    .optionalFieldOf("use_player_texture", false)
								                    .forGetter(EquipmentModel.Layer::usePlayerTexture)
				                    )
				                    .apply(instance, EquipmentModel.Layer::new)
		);

		public Layer(Identifier textureId) {
			this(textureId, Optional.empty(), false);
		}

		public static EquipmentModel.Layer createWithLeatherColor(Identifier textureId, boolean dyeable) {
			return new EquipmentModel.Layer(
					textureId,
					dyeable ? Optional.of(new EquipmentModel.Dyeable(Optional.of(-6265536))) : Optional.empty(),
					false
			);
		}

		public static EquipmentModel.Layer create(Identifier textureId, boolean dyeable) {
			return new EquipmentModel.Layer(
					textureId,
					dyeable ? Optional.of(new EquipmentModel.Dyeable(Optional.empty())) : Optional.empty(),
					false
			);
		}

		public Identifier getFullTextureId(EquipmentModel.LayerType layerType) {
			return this.textureId.withPath(textureName -> "textures/entity/equipment/" + layerType.asString() + "/"
					+ textureName + ".png");
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LayerType}.
	 */
	public static enum LayerType implements StringIdentifiable {
		HUMANOID("humanoid"),
		HUMANOID_LEGGINGS("humanoid_leggings"),
		WINGS("wings"),
		WOLF_BODY("wolf_body"),
		HORSE_BODY("horse_body"),
		LLAMA_BODY("llama_body"),
		PIG_SADDLE("pig_saddle"),
		STRIDER_SADDLE("strider_saddle"),
		CAMEL_SADDLE("camel_saddle"),
		CAMEL_HUSK_SADDLE("camel_husk_saddle"),
		HORSE_SADDLE("horse_saddle"),
		DONKEY_SADDLE("donkey_saddle"),
		MULE_SADDLE("mule_saddle"),
		ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
		SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
		HAPPY_GHAST_BODY("happy_ghast_body"),
		NAUTILUS_SADDLE("nautilus_saddle"),
		NAUTILUS_BODY("nautilus_body");

		public static final Codec<EquipmentModel.LayerType>
				CODEC =
				StringIdentifiable.createCodec(EquipmentModel.LayerType::values);
		private final String name;

		private LayerType(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}

		public String getTrimsDirectory() {
			return "trims/entity/" + this.name;
		}
	}
}
