package net.minecraft.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.block.entity.AbstractEndPortalBlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code RenderLayers}.
 */
public class RenderLayers {

	static final BiFunction<Identifier, Boolean, RenderLayer> OUTLINE = Util.memoize(
			(texture, cull) -> RenderLayer.of(
					"outline",
					RenderSetup.builder(cull ? RenderPipelines.OUTLINE_CULL : RenderPipelines.OUTLINE_NO_CULL)
					           .texture("Sampler0", texture)
					           .outputTarget(OutputTarget.OUTLINE_TARGET)
					           .outlineMode(RenderSetup.OutlineMode.IS_OUTLINE)
					           .build()
			)
	);
	public static final Supplier<GpuSampler> BLOCK_SAMPLER = () -> RenderSystem.getSamplerCache()
	                                                                           .get(
			                                                                           AddressMode.CLAMP_TO_EDGE,
			                                                                           AddressMode.CLAMP_TO_EDGE,
			                                                                           FilterMode.LINEAR,
			                                                                           FilterMode.NEAREST,
			                                                                           true
	                                                                           );
	private static final RenderLayer SOLID = RenderLayer.of(
			"solid_moving_block",
			RenderSetup.builder(RenderPipelines.SOLID_BLOCK)
			           .useLightmap()
			           .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, BLOCK_SAMPLER)
			           .crumbling()
			           .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
			           .build()
	);
	private static final RenderLayer CUTOUT = RenderLayer.of(
			"cutout_moving_block",
			RenderSetup.builder(RenderPipelines.CUTOUT_BLOCK)
			           .useLightmap()
			           .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, BLOCK_SAMPLER)
			           .crumbling()
			           .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
			           .build()
	);
	private static final RenderLayer TRANSLUCENT_MOVING_BLOCK = RenderLayer.of(
			"translucent_moving_block",
			RenderSetup.builder(RenderPipelines.RENDERTYPE_TRANSLUCENT_MOVING_BLOCK)
			           .useLightmap()
			           .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, BLOCK_SAMPLER)
			           .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			           .translucent()
			           .expectedBufferSize(786432)
			           .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
			           .build()
	);
	private static final Function<Identifier, RenderLayer> ARMOR_CUTOUT_NO_CULL = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				                                     .crumbling()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("armor_cutout_no_cull", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ARMOR_TRANSLUCENT = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ARMOR_TRANSLUCENT)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				                                     .crumbling()
				                                     .translucent()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("armor_translucent", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_SOLID = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .crumbling()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("entity_solid", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_SOLID_Z_OFFSET_FORWARD = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_SOLID_OFFSET_FORWARD)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
				                                     .crumbling()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("entity_solid_z_offset_forward", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_CUTOUT = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .crumbling()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("entity_cutout", renderSetup);
			}
	);
	private static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_CUTOUT_NO_CULL = Util.memoize(
			(texture, affectsOutline) -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_NO_CULL)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .crumbling()
				                                     .outlineMode(
						                                     affectsOutline ? RenderSetup.OutlineMode.AFFECTS_OUTLINE
						                                                    : RenderSetup.OutlineMode.NONE)
				                                     .build();
				return RenderLayer.of("entity_cutout_no_cull", renderSetup);
			}
	);
	private static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
			(texture, affectsOutline) -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				                                     .crumbling()
				                                     .outlineMode(
						                                     affectsOutline ? RenderSetup.OutlineMode.AFFECTS_OUTLINE
						                                                    : RenderSetup.OutlineMode.NONE)
				                                     .build();
				return RenderLayer.of("entity_cutout_no_cull_z_offset", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ITEM_ENTITY_TRANSLUCENT_CULL = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL)
				                                     .texture("Sampler0", texture)
				                                     .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .crumbling()
				                                     .translucent()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("item_entity_translucent_cull", renderSetup);
			}
	);
	private static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_TRANSLUCENT = Util.memoize(
			(texture, affectsOutline) -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .crumbling()
				                                     .translucent()
				                                     .outlineMode(
						                                     affectsOutline ? RenderSetup.OutlineMode.AFFECTS_OUTLINE
						                                                    : RenderSetup.OutlineMode.NONE)
				                                     .build();
				return RenderLayer.of("entity_translucent", renderSetup);
			}
	);
	private static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
			(texture, affectsOutline) -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
				                                     .texture("Sampler0", texture)
				                                     .useOverlay()
				                                     .crumbling()
				                                     .translucent()
				                                     .outlineMode(
						                                     affectsOutline ? RenderSetup.OutlineMode.AFFECTS_OUTLINE
						                                                    : RenderSetup.OutlineMode.NONE)
				                                     .build();
				return RenderLayer.of("entity_translucent_emissive", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_SMOOTH_CUTOUT = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_SMOOTH_CUTOUT)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("entity_smooth_cutout", renderSetup);
			}
	);
	private static final BiFunction<Identifier, Boolean, RenderLayer> BEACON_BEAM = Util.memoize(
			(texture, translucent) -> {
				RenderSetup
						renderSetup =
						RenderSetup
								.builder(translucent ? RenderPipelines.BEACON_BEAM_TRANSLUCENT
								                     : RenderPipelines.BEACON_BEAM_OPAQUE)
								.texture("Sampler0", texture)
								.translucent()
								.build();
				return RenderLayer.of("beacon_beam", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_DECAL = Util.memoize(texture -> {
		RenderSetup
				renderSetup =
				RenderSetup
						.builder(RenderPipelines.RENDERTYPE_ENTITY_DECAL)
						.texture("Sampler0", texture)
						.useLightmap()
						.useOverlay()
						.build();
		return RenderLayer.of("entity_decal", renderSetup);
	});
	private static final Function<Identifier, RenderLayer> ENTITY_NO_OUTLINE = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ENTITY_NO_OUTLINE)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .translucent()
				                                     .build();
				return RenderLayer.of("entity_no_outline", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_SHADOW = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.RENDERTYPE_ENTITY_SHADOW)
				                                     .texture("Sampler0", texture)
				                                     .useLightmap()
				                                     .useOverlay()
				                                     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				                                     .build();
				return RenderLayer.of("entity_shadow", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> ENTITY_ALPHA = Util.memoize(
			texture -> {
				RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.RENDERTYPE_ENTITY_ALPHA)
				                                     .texture("Sampler0", texture)
				                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
				                                     .build();
				return RenderLayer.of("entity_alpha", renderSetup);
			}
	);
	private static final Function<Identifier, RenderLayer> EYES = Util.memoize(
			texture -> RenderLayer.of(
					"eyes",
					RenderSetup.builder(RenderPipelines.ENTITY_EYES).texture("Sampler0", texture).translucent().build()
			)
	);
	private static final RenderLayer
			LEASH =
			RenderLayer.of("leash", RenderSetup.builder(RenderPipelines.RENDERTYPE_LEASH).useLightmap().build());
	private static final RenderLayer
			WATER_MASK =
			RenderLayer.of("water_mask", RenderSetup.builder(RenderPipelines.RENDERTYPE_WATER_MASK).build());
	private static final RenderLayer ARMOR_ENTITY_GLINT = RenderLayer.of(
			"armor_entity_glint",
			RenderSetup.builder(RenderPipelines.GLINT)
			           .texture("Sampler0", ItemRenderer.ENTITY_ENCHANTMENT_GLINT)
			           .textureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
			           .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			           .build()
	);
	private static final RenderLayer GLINT_TRANSLUCENT = RenderLayer.of(
			"glint_translucent",
			RenderSetup.builder(RenderPipelines.GLINT)
			           .texture("Sampler0", ItemRenderer.ITEM_ENCHANTMENT_GLINT)
			           .textureTransform(TextureTransform.GLINT_TEXTURING)
			           .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			           .build()
	);
	private static final RenderLayer GLINT = RenderLayer.of(
			"glint",
			RenderSetup.builder(RenderPipelines.GLINT)
			           .texture("Sampler0", ItemRenderer.ITEM_ENCHANTMENT_GLINT)
			           .textureTransform(TextureTransform.GLINT_TEXTURING)
			           .build()
	);
	private static final RenderLayer ENTITY_GLINT = RenderLayer.of(
			"entity_glint",
			RenderSetup.builder(RenderPipelines.GLINT)
			           .texture("Sampler0", ItemRenderer.ITEM_ENCHANTMENT_GLINT)
			           .textureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
			           .build()
	);
	private static final Function<Identifier, RenderLayer> CRUMBLING = Util.memoize(
			texture -> RenderLayer.of(
					"crumbling",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_CRUMBLING)
							.texture("Sampler0", texture)
							.translucent()
							.build()
			)
	);
	private static final Function<Identifier, RenderLayer> TEXT = Util.memoize(
			texture -> RenderLayer.of(
					"text",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_TEXT)
							.texture("Sampler0", texture)
							.useLightmap()
							.expectedBufferSize(786432)
							.build()
			)
	);
	private static final RenderLayer TEXT_BACKGROUND = RenderLayer.of(
			"text_background",
			RenderSetup.builder(RenderPipelines.RENDERTYPE_TEXT_BG).useLightmap().translucent().build()
	);
	private static final Function<Identifier, RenderLayer> TEXT_INTENSITY = Util.memoize(
			texture -> RenderLayer.of(
					"text_intensity",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_TEXT_INTENSITY)
							.texture("Sampler0", texture)
							.useLightmap()
							.expectedBufferSize(786432)
							.build()
			)
	);
	private static final Function<Identifier, RenderLayer> TEXT_POLYGON_OFFSET = Util.memoize(
			texture -> RenderLayer.of(
					"text_polygon_offset",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_TEXT_POLYGON_OFFSET)
							.texture("Sampler0", texture)
							.useLightmap()
							.translucent()
							.build()
			)
	);
	private static final Function<Identifier, RenderLayer> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
			texture -> RenderLayer.of(
					"text_intensity_polygon_offset",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_TEXT_INTENSITY)
							.texture("Sampler0", texture)
							.useLightmap()
							.translucent()
							.build()
			)
	);
	private static final Function<Identifier, RenderLayer> TEXT_SEE_THROUGH = Util.memoize(
			texture -> RenderLayer.of(
					"text_see_through",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_TEXT_SEETHROUGH)
							.texture("Sampler0", texture)
							.useLightmap()
							.build()
			)
	);
	private static final RenderLayer TEXT_BACKGROUND_SEE_THROUGH = RenderLayer.of(
			"text_background_see_through",
			RenderSetup.builder(RenderPipelines.RENDERTYPE_TEXT_BG_SEETHROUGH).useLightmap().translucent().build()
	);
	private static final Function<Identifier, RenderLayer> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
			texture -> RenderLayer.of(
					"text_intensity_see_through",
					RenderSetup
							.builder(RenderPipelines.RENDERTYPE_TEXT_INTENSITY_SEETHROUGH)
							.texture("Sampler0", texture)
							.useLightmap()
							.translucent()
							.build()
			)
	);
	private static final RenderLayer LIGHTNING = RenderLayer.of(
			"lightning",
			RenderSetup
					.builder(RenderPipelines.RENDERTYPE_LIGHTNING)
					.outputTarget(OutputTarget.WEATHER_TARGET)
					.translucent()
					.build()
	);
	private static final RenderLayer
			DRAGON_RAYS =
			RenderLayer.of(
					"dragon_rays",
					RenderSetup.builder(RenderPipelines.RENDERTYPE_LIGHTNING_DRAGON_RAYS).build()
			);
	private static final RenderLayer DRAGON_RAYS_DEPTH = RenderLayer.of(
			"dragon_rays_depth", RenderSetup.builder(RenderPipelines.POSITION_DRAGON_RAYS_DEPTH).build()
	);
	private static final RenderLayer TRIPWIRE = RenderLayer.of(
			"tripwire_moving_block",
			RenderSetup.builder(RenderPipelines.TRIPWIRE_BLOCK)
			           .useLightmap()
			           .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, BLOCK_SAMPLER)
			           .outputTarget(OutputTarget.WEATHER_TARGET)
			           .crumbling()
			           .translucent()
			           .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
			           .build()
	);
	private static final RenderLayer END_PORTAL = RenderLayer.of(
			"end_portal",
			RenderSetup.builder(RenderPipelines.END_PORTAL)
			           .texture("Sampler0", AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE)
			           .texture("Sampler1", AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE)
			           .build()
	);
	private static final RenderLayer END_GATEWAY = RenderLayer.of(
			"end_gateway",
			RenderSetup.builder(RenderPipelines.END_GATEWAY)
			           .texture("Sampler0", AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE)
			           .texture("Sampler1", AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE)
			           .build()
	);
	public static final RenderLayer LINES = RenderLayer.of(
			"lines",
			RenderSetup.builder(RenderPipelines.LINES)
			           .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			           .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			           .build()
	);
	public static final RenderLayer LINES_TRANSLUCENT = RenderLayer.of(
			"lines_translucent",
			RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT)
			           .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			           .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			           .build()
	);
	public static final RenderLayer SECONDARY_BLOCK_OUTLINE = RenderLayer.of(
			"secondary_block_outline",
			RenderSetup.builder(RenderPipelines.SECOND_BLOCK_OUTLINE)
			           .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			           .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			           .build()
	);
	private static final RenderLayer DEBUG_FILLED_BOX = RenderLayer.of(
			"debug_filled_box",
			RenderSetup
					.builder(RenderPipelines.DEBUG_FILLED_BOX)
					.translucent()
					.layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
					.build()
	);
	private static final RenderLayer
			DEBUG_POINT =
			RenderLayer.of("debug_point", RenderSetup.builder(RenderPipelines.DEBUG_POINTS).build());
	private static final RenderLayer
			DEBUG_QUADS =
			RenderLayer.of("debug_quads", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).translucent().build());
	private static final RenderLayer DEBUG_TRIANGLE_FAN = RenderLayer.of(
			"debug_triangle_fan", RenderSetup.builder(RenderPipelines.DEBUG_TRIANGLE_FAN).translucent().build()
	);
	private static final Function<Identifier, RenderLayer>
			WEATHER_DEPTH =
			createWeatherFactory(RenderPipelines.WEATHER_DEPTH);
	private static final Function<Identifier, RenderLayer>
			WEATHER_NO_DEPTH =
			createWeatherFactory(RenderPipelines.WEATHER_NO_DEPTH);
	private static final Function<Identifier, RenderLayer> BLOCK_SCREEN_EFFECT = Util.memoize(
			texture -> RenderLayer.of(
					"block_screen_effect",
					RenderSetup.builder(RenderPipelines.BLOCK_SCREEN_EFFECT).texture("Sampler0", texture).build()
			)
	);
	private static final Function<Identifier, RenderLayer> FIRE_SCREEN_EFFECT = Util.memoize(
			texture -> RenderLayer.of(
					"fire_screen_effect",
					RenderSetup.builder(RenderPipelines.FIRE_SCREEN_EFFECT).texture("Sampler0", texture).build()
			)
	);

	/**
	 * Solid.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer solid() {
		return SOLID;
	}

	/**
	 * Cutout.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer cutout() {
		return CUTOUT;
	}

	/**
	 * Translucent moving block.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer translucentMovingBlock() {
		return TRANSLUCENT_MOVING_BLOCK;
	}

	/**
	 * Armor cutout no cull.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer armorCutoutNoCull(Identifier texture) {
		return ARMOR_CUTOUT_NO_CULL.apply(texture);
	}

	/**
	 * Armor decal cutout no cull.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer armorDecalCutoutNoCull(Identifier texture) {
		RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL)
		                                     .texture("Sampler0", texture)
		                                     .useLightmap()
		                                     .useOverlay()
		                                     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
		                                     .crumbling()
		                                     .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
		                                     .build();
		return RenderLayer.of("armor_decal_cutout_no_cull", renderSetup);
	}

	/**
	 * Armor translucent.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer armorTranslucent(Identifier texture) {
		return ARMOR_TRANSLUCENT.apply(texture);
	}

	/**
	 * Entity solid.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entitySolid(Identifier texture) {
		return ENTITY_SOLID.apply(texture);
	}

	/**
	 * Entity solid z offset forward.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entitySolidZOffsetForward(Identifier texture) {
		return ENTITY_SOLID_Z_OFFSET_FORWARD.apply(texture);
	}

	/**
	 * Entity cutout.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityCutout(Identifier texture) {
		return ENTITY_CUTOUT.apply(texture);
	}

	/**
	 * Entity cutout no cull.
	 *
	 * @param texture texture
	 * @param affectsOutline affects outline
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityCutoutNoCull(Identifier texture, boolean affectsOutline) {
		return ENTITY_CUTOUT_NO_CULL.apply(texture, affectsOutline);
	}

	/**
	 * Entity cutout no cull.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityCutoutNoCull(Identifier texture) {
		return entityCutoutNoCull(texture, true);
	}

	/**
	 * Entity cutout no cull z offset.
	 *
	 * @param texture texture
	 * @param affectsOutline affects outline
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityCutoutNoCullZOffset(Identifier texture, boolean affectsOutline) {
		return ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(texture, affectsOutline);
	}

	/**
	 * Entity cutout no cull z offset.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityCutoutNoCullZOffset(Identifier texture) {
		return entityCutoutNoCullZOffset(texture, true);
	}

	/**
	 * Item entity translucent cull.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer itemEntityTranslucentCull(Identifier texture) {
		return ITEM_ENTITY_TRANSLUCENT_CULL.apply(texture);
	}

	/**
	 * Entity translucent.
	 *
	 * @param texture texture
	 * @param affectsOutline affects outline
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityTranslucent(Identifier texture, boolean affectsOutline) {
		return ENTITY_TRANSLUCENT.apply(texture, affectsOutline);
	}

	/**
	 * Entity translucent.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityTranslucent(Identifier texture) {
		return entityTranslucent(texture, true);
	}

	/**
	 * Entity translucent emissive.
	 *
	 * @param texture texture
	 * @param affectsOutline affects outline
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityTranslucentEmissive(Identifier texture, boolean affectsOutline) {
		return ENTITY_TRANSLUCENT_EMISSIVE.apply(texture, affectsOutline);
	}

	/**
	 * Entity translucent emissive.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityTranslucentEmissive(Identifier texture) {
		return entityTranslucentEmissive(texture, true);
	}

	/**
	 * Entity smooth cutout.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entitySmoothCutout(Identifier texture) {
		return ENTITY_SMOOTH_CUTOUT.apply(texture);
	}

	/**
	 * Beacon beam.
	 *
	 * @param texture texture
	 * @param translucent translucent
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer beaconBeam(Identifier texture, boolean translucent) {
		return BEACON_BEAM.apply(texture, translucent);
	}

	/**
	 * Entity decal.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityDecal(Identifier texture) {
		return ENTITY_DECAL.apply(texture);
	}

	/**
	 * Entity no outline.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityNoOutline(Identifier texture) {
		return ENTITY_NO_OUTLINE.apply(texture);
	}

	/**
	 * Entity shadow.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityShadow(Identifier texture) {
		return ENTITY_SHADOW.apply(texture);
	}

	/**
	 * Entity alpha.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityAlpha(Identifier texture) {
		return ENTITY_ALPHA.apply(texture);
	}

	/**
	 * Eyes.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer eyes(Identifier texture) {
		return EYES.apply(texture);
	}

	/**
	 * Entity translucent emissive no outline.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityTranslucentEmissiveNoOutline(Identifier texture) {
		return ENTITY_TRANSLUCENT_EMISSIVE.apply(texture, false);
	}

	/**
	 * Breeze wind.
	 *
	 * @param texture texture
	 * @param du du
	 * @param dv dv
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer breezeWind(Identifier texture, float du, float dv) {
		return RenderLayer.of(
				"breeze_wind",
				RenderSetup.builder(RenderPipelines.BREEZE_WIND)
				           .texture("Sampler0", texture)
				           .textureTransform(new TextureTransform.OffsetTexturing(du, dv))
				           .useLightmap()
				           .translucent()
				           .build()
		);
	}

	/**
	 * Energy swirl.
	 *
	 * @param texture texture
	 * @param du du
	 * @param dv dv
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer energySwirl(Identifier texture, float du, float dv) {
		return RenderLayer.of(
				"energy_swirl",
				RenderSetup.builder(RenderPipelines.ENTITY_ENERGY_SWIRL)
				           .texture("Sampler0", texture)
				           .textureTransform(new TextureTransform.OffsetTexturing(du, dv))
				           .useLightmap()
				           .useOverlay()
				           .translucent()
				           .build()
		);
	}

	/**
	 * Leash.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer leash() {
		return LEASH;
	}

	/**
	 * Water mask.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer waterMask() {
		return WATER_MASK;
	}

	/**
	 * Outline no cull.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer outlineNoCull(Identifier texture) {
		return OUTLINE.apply(texture, false);
	}

	/**
	 * Armor entity glint.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer armorEntityGlint() {
		return ARMOR_ENTITY_GLINT;
	}

	/**
	 * Glint translucent.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer glintTranslucent() {
		return GLINT_TRANSLUCENT;
	}

	/**
	 * Glint.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer glint() {
		return GLINT;
	}

	/**
	 * Entity glint.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer entityGlint() {
		return ENTITY_GLINT;
	}

	/**
	 * Crumbling.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer crumbling(Identifier texture) {
		return CRUMBLING.apply(texture);
	}

	/**
	 * Text.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer text(Identifier texture) {
		return TEXT.apply(texture);
	}

	/**
	 * Text background.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textBackground() {
		return TEXT_BACKGROUND;
	}

	/**
	 * Text intensity.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textIntensity(Identifier texture) {
		return TEXT_INTENSITY.apply(texture);
	}

	/**
	 * Text polygon offset.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textPolygonOffset(Identifier texture) {
		return TEXT_POLYGON_OFFSET.apply(texture);
	}

	/**
	 * Text intensity polygon offset.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textIntensityPolygonOffset(Identifier texture) {
		return TEXT_INTENSITY_POLYGON_OFFSET.apply(texture);
	}

	/**
	 * Text see through.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textSeeThrough(Identifier texture) {
		return TEXT_SEE_THROUGH.apply(texture);
	}

	/**
	 * Text background see through.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textBackgroundSeeThrough() {
		return TEXT_BACKGROUND_SEE_THROUGH;
	}

	/**
	 * Text intensity see through.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer textIntensitySeeThrough(Identifier texture) {
		return TEXT_INTENSITY_SEE_THROUGH.apply(texture);
	}

	/**
	 * Lightning.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer lightning() {
		return LIGHTNING;
	}

	/**
	 * Dragon rays.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer dragonRays() {
		return DRAGON_RAYS;
	}

	/**
	 * Dragon rays depth.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer dragonRaysDepth() {
		return DRAGON_RAYS_DEPTH;
	}

	/**
	 * Tripwire.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer tripwire() {
		return TRIPWIRE;
	}

	/**
	 * End portal.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer endPortal() {
		return END_PORTAL;
	}

	/**
	 * End gateway.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer endGateway() {
		return END_GATEWAY;
	}

	/**
	 * Lines.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer lines() {
		return LINES;
	}

	/**
	 * Lines translucent.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer linesTranslucent() {
		return LINES_TRANSLUCENT;
	}

	/**
	 * Secondary block outline.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer secondaryBlockOutline() {
		return SECONDARY_BLOCK_OUTLINE;
	}

	/**
	 * Debug filled box.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer debugFilledBox() {
		return DEBUG_FILLED_BOX;
	}

	/**
	 * Debug point.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer debugPoint() {
		return DEBUG_POINT;
	}

	/**
	 * Debug quads.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer debugQuads() {
		return DEBUG_QUADS;
	}

	/**
	 * Debug triangle fan.
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer debugTriangleFan() {
		return DEBUG_TRIANGLE_FAN;
	}

	private static Function<Identifier, RenderLayer> createWeatherFactory(RenderPipeline pipeline) {
		return Util.memoize(
				texture -> RenderLayer.of(
						"weather",
						RenderSetup
								.builder(pipeline)
								.texture("Sampler0", texture)
								.outputTarget(OutputTarget.WEATHER_TARGET)
								.useLightmap()
								.build()
				)
		);
	}

	/**
	 * Weather.
	 *
	 * @param texture texture
	 * @param depth depth
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer weather(Identifier texture, boolean depth) {
		return (depth ? WEATHER_DEPTH : WEATHER_NO_DEPTH).apply(texture);
	}

	/**
	 * Block screen effect.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer blockScreenEffect(Identifier texture) {
		return BLOCK_SCREEN_EFFECT.apply(texture);
	}

	/**
	 * Fire screen effect.
	 *
	 * @param texture texture
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer fireScreenEffect(Identifier texture) {
		return FIRE_SCREEN_EFFECT.apply(texture);
	}
}
