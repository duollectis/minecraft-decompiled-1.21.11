package net.minecraft.client.render;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DecoratedPotPattern;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.SpriteMapper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code TexturedRenderLayers}.
 */
public class TexturedRenderLayers {

	public static final Identifier
			SHULKER_BOXES_ATLAS_TEXTURE =
			Identifier.ofVanilla("textures/atlas/shulker_boxes.png");
	public static final Identifier BEDS_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/beds.png");
	public static final Identifier
			BANNER_PATTERNS_ATLAS_TEXTURE =
			Identifier.ofVanilla("textures/atlas/banner_patterns.png");
	public static final Identifier
			SHIELD_PATTERNS_ATLAS_TEXTURE =
			Identifier.ofVanilla("textures/atlas/shield_patterns.png");
	public static final Identifier SIGNS_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/signs.png");
	public static final Identifier CHEST_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/chest.png");
	public static final Identifier ARMOR_TRIMS_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/armor_trims.png");
	public static final Identifier
			DECORATED_POT_ATLAS_TEXTURE =
			Identifier.ofVanilla("textures/atlas/decorated_pot.png");
	public static final Identifier GUI_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/gui.png");
	public static final Identifier
			MAP_DECORATIONS_ATLAS_TEXTURE =
			Identifier.ofVanilla("textures/atlas/map_decorations.png");
	public static final Identifier PAINTINGS_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/paintings.png");
	public static final Identifier CELESTIALS_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/celestials.png");
	private static final RenderLayer
			SHULKER_BOXES_RENDER_LAYER =
			RenderLayers.entityCutoutNoCull(SHULKER_BOXES_ATLAS_TEXTURE);
	private static final RenderLayer BEDS_RENDER_LAYER = RenderLayers.entitySolid(BEDS_ATLAS_TEXTURE);
	private static final RenderLayer
			BANNER_PATTERNS_RENDER_LAYER =
			RenderLayers.entityNoOutline(BANNER_PATTERNS_ATLAS_TEXTURE);
	private static final RenderLayer
			SHIELD_PATTERNS_RENDER_LAYER =
			RenderLayers.entityNoOutline(SHIELD_PATTERNS_ATLAS_TEXTURE);
	private static final RenderLayer SIGN_RENDER_LAYER = RenderLayers.entityCutoutNoCull(SIGNS_ATLAS_TEXTURE);
	private static final RenderLayer CHEST_RENDER_LAYER = RenderLayers.entityCutout(CHEST_ATLAS_TEXTURE);
	private static final RenderLayer
			ARMOR_TRIMS_RENDER_LAYER =
			RenderLayers.armorCutoutNoCull(ARMOR_TRIMS_ATLAS_TEXTURE);
	private static final RenderLayer
			ARMOR_TRIMS_DECAL_RENDER_LAYER =
			RenderLayers.armorDecalCutoutNoCull(ARMOR_TRIMS_ATLAS_TEXTURE);
	private static final RenderLayer ENTITY_SOLID = RenderLayers.entitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
	private static final RenderLayer ENTITY_CUTOUT = RenderLayers.entityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
	private static final RenderLayer
			BLOCK_TRANSLUCENT_CULL =
			RenderLayers.itemEntityTranslucentCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
	private static final RenderLayer
			ITEM_TRANSLUCENT_CULL =
			RenderLayers.itemEntityTranslucentCull(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE);
	public static final SpriteMapper
			ITEM_SPRITE_MAPPER =
			new SpriteMapper(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE, "item");
	public static final SpriteMapper
			BLOCK_SPRITE_MAPPER =
			new SpriteMapper(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, "block");
	public static final SpriteMapper
			ENTITY_SPRITE_MAPPER =
			new SpriteMapper(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, "entity");
	public static final SpriteMapper
			BANNER_PATTERN_SPRITE_MAPPER =
			new SpriteMapper(BANNER_PATTERNS_ATLAS_TEXTURE, "entity/banner");
	public static final SpriteMapper
			SHIELD_PATTERN_SPRITE_MAPPER =
			new SpriteMapper(SHIELD_PATTERNS_ATLAS_TEXTURE, "entity/shield");
	public static final SpriteMapper CHEST_SPRITE_MAPPER = new SpriteMapper(CHEST_ATLAS_TEXTURE, "entity/chest");
	public static final SpriteMapper
			DECORATED_POT_SPRITE_MAPPER =
			new SpriteMapper(DECORATED_POT_ATLAS_TEXTURE, "entity/decorated_pot");
	public static final SpriteMapper BED_SPRITE_MAPPER = new SpriteMapper(BEDS_ATLAS_TEXTURE, "entity/bed");
	public static final SpriteMapper
			SHULKER_SPRITE_MAPPER =
			new SpriteMapper(SHULKER_BOXES_ATLAS_TEXTURE, "entity/shulker");
	public static final SpriteMapper SIGN_SPRITE_MAPPER = new SpriteMapper(SIGNS_ATLAS_TEXTURE, "entity/signs");
	public static final SpriteMapper
			HANGING_SIGN_SPRITE_MAPPER =
			new SpriteMapper(SIGNS_ATLAS_TEXTURE, "entity/signs/hanging");
	public static final SpriteIdentifier SHULKER_TEXTURE_ID = SHULKER_SPRITE_MAPPER.mapVanilla("shulker");
	public static final List<SpriteIdentifier> COLORED_SHULKER_BOXES_TEXTURES = Arrays.stream(DyeColor.values())
	                                                                                  .sorted(Comparator.comparingInt(
			                                                                                  DyeColor::getIndex))
	                                                                                  .map(TexturedRenderLayers::createShulkerBoxTextureId)
	                                                                                  .collect(ImmutableList.toImmutableList());
	public static final Map<WoodType, SpriteIdentifier> SIGN_TYPE_TEXTURES = WoodType.stream()
	                                                                                 .collect(Collectors.toMap(
			                                                                                 Function.identity(),
			                                                                                 TexturedRenderLayers::createSignTextureId
	                                                                                 ));
	public static final Map<WoodType, SpriteIdentifier> HANGING_SIGN_TYPE_TEXTURES = WoodType.stream()
	                                                                                         .collect(Collectors.toMap(
			                                                                                         Function.identity(),
			                                                                                         TexturedRenderLayers::createHangingSignTextureId
	                                                                                         ));
	public static final SpriteIdentifier BANNER_BASE = BANNER_PATTERN_SPRITE_MAPPER.mapVanilla("base");
	public static final SpriteIdentifier SHIELD_BASE = SHIELD_PATTERN_SPRITE_MAPPER.mapVanilla("base");
	private static final Map<Identifier, SpriteIdentifier> BANNER_PATTERN_TEXTURES = new HashMap<>();
	private static final Map<Identifier, SpriteIdentifier> SHIELD_PATTERN_TEXTURES = new HashMap<>();
	public static final Map<RegistryKey<DecoratedPotPattern>, SpriteIdentifier>
			DECORATED_POT_PATTERN_TEXTURES =
			Registries.DECORATED_POT_PATTERN
					.streamEntries()
					.collect(Collectors.toMap(
							RegistryEntry.Reference::registryKey,
							pattern -> DECORATED_POT_SPRITE_MAPPER.map(pattern.value().assetId())
					));
	public static final SpriteIdentifier
			DECORATED_POT_BASE =
			DECORATED_POT_SPRITE_MAPPER.mapVanilla("decorated_pot_base");
	public static final SpriteIdentifier
			DECORATED_POT_SIDE =
			DECORATED_POT_SPRITE_MAPPER.mapVanilla("decorated_pot_side");
	private static final SpriteIdentifier[] BED_TEXTURES = Arrays.stream(DyeColor.values())
	                                                             .sorted(Comparator.comparingInt(DyeColor::getIndex))
	                                                             .map(TexturedRenderLayers::createBedTextureId)
	                                                             .toArray(SpriteIdentifier[]::new);
	public static final SpriteIdentifier TRAPPED_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("trapped");
	public static final SpriteIdentifier TRAPPED_CHEST_LEFT = CHEST_SPRITE_MAPPER.mapVanilla("trapped_left");
	public static final SpriteIdentifier TRAPPED_CHEST_RIGHT = CHEST_SPRITE_MAPPER.mapVanilla("trapped_right");
	public static final SpriteIdentifier CHRISTMAS_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("christmas");
	public static final SpriteIdentifier CHRISTMAS_CHEST_LEFT = CHEST_SPRITE_MAPPER.mapVanilla("christmas_left");
	public static final SpriteIdentifier CHRISTMAS_CHEST_RIGHT = CHEST_SPRITE_MAPPER.mapVanilla("christmas_right");
	public static final SpriteIdentifier CHEST = CHEST_SPRITE_MAPPER.mapVanilla("normal");
	public static final SpriteIdentifier CHEST_LEFT = CHEST_SPRITE_MAPPER.mapVanilla("normal_left");
	public static final SpriteIdentifier CHEST_RIGHT = CHEST_SPRITE_MAPPER.mapVanilla("normal_right");
	public static final SpriteIdentifier ENDER_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("ender");
	public static final SpriteIdentifier COPPER_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("copper");
	public static final SpriteIdentifier COPPER_CHEST_LEFT = CHEST_SPRITE_MAPPER.mapVanilla("copper_left");
	public static final SpriteIdentifier COPPER_CHEST_RIGHT = CHEST_SPRITE_MAPPER.mapVanilla("copper_right");
	public static final SpriteIdentifier EXPOSED_COPPER_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("copper_exposed");
	public static final SpriteIdentifier
			EXPOSED_COPPER_CHEST_LEFT =
			CHEST_SPRITE_MAPPER.mapVanilla("copper_exposed_left");
	public static final SpriteIdentifier
			EXPOSED_COPPER_CHEST_RIGHT =
			CHEST_SPRITE_MAPPER.mapVanilla("copper_exposed_right");
	public static final SpriteIdentifier WEATHERED_COPPER_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("copper_weathered");
	public static final SpriteIdentifier
			WEATHERED_COPPER_CHEST_LEFT =
			CHEST_SPRITE_MAPPER.mapVanilla("copper_weathered_left");
	public static final SpriteIdentifier
			WEATHERED_COPPER_CHEST_RIGHT =
			CHEST_SPRITE_MAPPER.mapVanilla("copper_weathered_right");
	public static final SpriteIdentifier OXIDIZED_COPPER_CHEST = CHEST_SPRITE_MAPPER.mapVanilla("copper_oxidized");
	public static final SpriteIdentifier
			OXIDIZED_COPPER_CHEST_LEFT =
			CHEST_SPRITE_MAPPER.mapVanilla("copper_oxidized_left");
	public static final SpriteIdentifier
			OXIDIZED_COPPER_CHEST_RIGHT =
			CHEST_SPRITE_MAPPER.mapVanilla("copper_oxidized_right");

	public static RenderLayer getBannerPatterns() {
		return BANNER_PATTERNS_RENDER_LAYER;
	}

	public static RenderLayer getShieldPatterns() {
		return SHIELD_PATTERNS_RENDER_LAYER;
	}

	public static RenderLayer getBeds() {
		return BEDS_RENDER_LAYER;
	}

	public static RenderLayer getShulkerBoxes() {
		return SHULKER_BOXES_RENDER_LAYER;
	}

	public static RenderLayer getSign() {
		return SIGN_RENDER_LAYER;
	}

	public static RenderLayer getHangingSign() {
		return SIGN_RENDER_LAYER;
	}

	public static RenderLayer getChest() {
		return CHEST_RENDER_LAYER;
	}

	public static RenderLayer getArmorTrims(boolean decal) {
		return decal ? ARMOR_TRIMS_DECAL_RENDER_LAYER : ARMOR_TRIMS_RENDER_LAYER;
	}

	public static RenderLayer getEntitySolid() {
		return ENTITY_SOLID;
	}

	public static RenderLayer getEntityCutout() {
		return ENTITY_CUTOUT;
	}

	public static RenderLayer getItemTranslucentCull() {
		return ITEM_TRANSLUCENT_CULL;
	}

	public static RenderLayer getBlockTranslucentCull() {
		return BLOCK_TRANSLUCENT_CULL;
	}

	public static SpriteIdentifier getBedTextureId(DyeColor color) {
		return BED_TEXTURES[color.getIndex()];
	}

	/**
	 * Создаёт color id.
	 *
	 * @param color color
	 *
	 * @return Identifier — результат операции
	 */
	public static Identifier createColorId(DyeColor color) {
		return Identifier.ofVanilla(color.getId());
	}

	/**
	 * Создаёт bed texture id.
	 *
	 * @param color color
	 *
	 * @return SpriteIdentifier — результат операции
	 */
	public static SpriteIdentifier createBedTextureId(DyeColor color) {
		return BED_SPRITE_MAPPER.map(createColorId(color));
	}

	public static SpriteIdentifier getShulkerBoxTextureId(DyeColor color) {
		return COLORED_SHULKER_BOXES_TEXTURES.get(color.getIndex());
	}

	/**
	 * Создаёт shulker id.
	 *
	 * @param color color
	 *
	 * @return Identifier — результат операции
	 */
	public static Identifier createShulkerId(DyeColor color) {
		return Identifier.ofVanilla("shulker_" + color.getId());
	}

	/**
	 * Создаёт shulker box texture id.
	 *
	 * @param color color
	 *
	 * @return SpriteIdentifier — результат операции
	 */
	public static SpriteIdentifier createShulkerBoxTextureId(DyeColor color) {
		return SHULKER_SPRITE_MAPPER.map(createShulkerId(color));
	}

	private static SpriteIdentifier createSignTextureId(WoodType type) {
		return SIGN_SPRITE_MAPPER.mapVanilla(type.name());
	}

	private static SpriteIdentifier createHangingSignTextureId(WoodType type) {
		return HANGING_SIGN_SPRITE_MAPPER.mapVanilla(type.name());
	}

	public static SpriteIdentifier getSignTextureId(WoodType signType) {
		return SIGN_TYPE_TEXTURES.get(signType);
	}

	public static SpriteIdentifier getHangingSignTextureId(WoodType signType) {
		return HANGING_SIGN_TYPE_TEXTURES.get(signType);
	}

	public static SpriteIdentifier getBannerPatternTextureId(RegistryEntry<BannerPattern> pattern) {
		return BANNER_PATTERN_TEXTURES.computeIfAbsent(pattern.value().assetId(), BANNER_PATTERN_SPRITE_MAPPER::map);
	}

	public static SpriteIdentifier getShieldPatternTextureId(RegistryEntry<BannerPattern> pattern) {
		return SHIELD_PATTERN_TEXTURES.computeIfAbsent(pattern.value().assetId(), SHIELD_PATTERN_SPRITE_MAPPER::map);
	}

	public static @Nullable SpriteIdentifier getDecoratedPotPatternTextureId(@Nullable RegistryKey<DecoratedPotPattern> potPatternKey) {
		return potPatternKey == null ? null : DECORATED_POT_PATTERN_TEXTURES.get(potPatternKey);
	}

	public static SpriteIdentifier getChestTextureId(ChestBlockEntityRenderState.Variant variant, ChestType type) {
		return switch (variant) {
			case ENDER_CHEST -> ENDER_CHEST;
			case CHRISTMAS -> getChestTextureId(type, CHRISTMAS_CHEST, CHRISTMAS_CHEST_LEFT, CHRISTMAS_CHEST_RIGHT);
			case TRAPPED -> getChestTextureId(type, TRAPPED_CHEST, TRAPPED_CHEST_LEFT, TRAPPED_CHEST_RIGHT);
			case COPPER_UNAFFECTED -> getChestTextureId(type, COPPER_CHEST, COPPER_CHEST_LEFT, COPPER_CHEST_RIGHT);
			case COPPER_EXPOSED -> getChestTextureId(
					type,
					EXPOSED_COPPER_CHEST,
					EXPOSED_COPPER_CHEST_LEFT,
					EXPOSED_COPPER_CHEST_RIGHT
			);
			case COPPER_WEATHERED -> getChestTextureId(
					type,
					WEATHERED_COPPER_CHEST,
					WEATHERED_COPPER_CHEST_LEFT,
					WEATHERED_COPPER_CHEST_RIGHT
			);
			case COPPER_OXIDIZED -> getChestTextureId(
					type,
					OXIDIZED_COPPER_CHEST,
					OXIDIZED_COPPER_CHEST_LEFT,
					OXIDIZED_COPPER_CHEST_RIGHT
			);
			case REGULAR -> getChestTextureId(type, CHEST, CHEST_LEFT, CHEST_RIGHT);
		};
	}

	private static SpriteIdentifier getChestTextureId(
			ChestType type,
			SpriteIdentifier single,
			SpriteIdentifier left,
			SpriteIdentifier right
	) {
		switch (type) {
			case LEFT:
				return left;
			case RIGHT:
				return right;
			case SINGLE:
			default:
				return single;
		}
	}
}
