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

/**
 * Описание модели снаряжения: набор слоёв текстур для каждого типа слота.
 * <p>
 * Загружается из JSON-ресурса и используется {@code EquipmentRenderer} для
 * отрисовки брони, сёдел и другого снаряжения поверх модели сущности.
 * Каждый {@link LayerType} может содержать несколько {@link Layer} с разными
 * текстурами (например, основная + слой окраски кожи).
 */
@Environment(EnvType.CLIENT)
public record EquipmentModel(Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>> layers) {

	private static final Codec<List<EquipmentModel.Layer>> LAYER_LIST_CODEC =
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
		return layers.getOrDefault(layerType, List.of());
	}

	/** Строитель для пошагового конструирования {@link EquipmentModel}. */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>> layers =
				new EnumMap<>(EquipmentModel.LayerType.class);

		Builder() {
		}

		public EquipmentModel.Builder addHumanoidLayers(Identifier textureId) {
			return addHumanoidLayers(textureId, false);
		}

		public EquipmentModel.Builder addHumanoidLayers(Identifier textureId, boolean dyeable) {
			addLayers(
					EquipmentModel.LayerType.HUMANOID_LEGGINGS,
					EquipmentModel.Layer.createWithLeatherColor(textureId, dyeable)
			);
			addMainHumanoidLayer(textureId, dyeable);
			return this;
		}

		public EquipmentModel.Builder addMainHumanoidLayer(Identifier textureId, boolean dyeable) {
			return addLayers(
					EquipmentModel.LayerType.HUMANOID,
					EquipmentModel.Layer.createWithLeatherColor(textureId, dyeable)
			);
		}

		public EquipmentModel.Builder addLayers(EquipmentModel.LayerType layerType, EquipmentModel.Layer... newLayers) {
			Collections.addAll(layers.computeIfAbsent(layerType, key -> new ArrayList<>()), newLayers);
			return this;
		}

		public EquipmentModel build() {
			return new EquipmentModel(
					layers.entrySet()
					      .stream()
					      .collect(ImmutableMap.toImmutableMap(
							      Entry::getKey,
							      entry -> List.copyOf((Collection) entry.getValue())
					      ))
			);
		}
	}

	/** Параметры окрашиваемого слоя: цвет по умолчанию при отсутствии окраски. */
	@Environment(EnvType.CLIENT)
	public record Dyeable(Optional<Integer> colorWhenUndyed) {

		public static final Codec<EquipmentModel.Dyeable> CODEC = RecordCodecBuilder.create(
				instance -> instance
						.group(Codecs.RGB
								.optionalFieldOf("color_when_undyed")
								.forGetter(EquipmentModel.Dyeable::colorWhenUndyed))
						.apply(instance, EquipmentModel.Dyeable::new)
		);
	}

	/**
	 * Один текстурный слой снаряжения.
	 * <p>
	 * Может быть окрашиваемым ({@code dyeable}) и/или использовать текстуру
	 * скина игрока ({@code usePlayerTexture}).
	 */
	@Environment(EnvType.CLIENT)
	public record Layer(Identifier textureId, Optional<EquipmentModel.Dyeable> dyeable, boolean usePlayerTexture) {

		private static final int LEATHER_DEFAULT_COLOR = -6265536;

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

		/** Создаёт слой с цветом кожи по умолчанию (коричневый кожаный цвет). */
		public static EquipmentModel.Layer createWithLeatherColor(Identifier textureId, boolean dyeable) {
			return new EquipmentModel.Layer(
					textureId,
					dyeable
							? Optional.of(new EquipmentModel.Dyeable(Optional.of(LEATHER_DEFAULT_COLOR)))
							: Optional.empty(),
					false
			);
		}

		/** Создаёт окрашиваемый слой без цвета по умолчанию. */
		public static EquipmentModel.Layer create(Identifier textureId, boolean dyeable) {
			return new EquipmentModel.Layer(
					textureId,
					dyeable ? Optional.of(new EquipmentModel.Dyeable(Optional.empty())) : Optional.empty(),
					false
			);
		}

		public Identifier getFullTextureId(EquipmentModel.LayerType layerType) {
			return textureId.withPath(
					name -> "textures/entity/equipment/" + layerType.asString() + "/" + name + ".png"
			);
		}
	}

	/** Тип слота снаряжения, определяющий геометрию модели и директорию текстур. */
	@Environment(EnvType.CLIENT)
	public enum LayerType implements StringIdentifiable {
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

		public static final Codec<EquipmentModel.LayerType> CODEC =
				StringIdentifiable.createCodec(EquipmentModel.LayerType::values);

		private final String name;

		LayerType(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}

		public String getTrimsDirectory() {
			return "trims/entity/" + name;
		}
	}
}
