package net.minecraft.client.render.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code GeometryBakedModel}.
 */
public record GeometryBakedModel(
		BakedGeometry quads,
		boolean useAmbientOcclusion,
		Sprite particleSprite
) implements BlockModelPart {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Create.
	 *
	 * @param baker baker
	 * @param id id
	 * @param bakeSettings bake settings
	 *
	 * @return BlockModelPart — результат операции
	 */
	public static BlockModelPart create(Baker baker, Identifier id, ModelBakeSettings bakeSettings) {
		BakedSimpleModel bakedSimpleModel = baker.getModel(id);
		ModelTextures modelTextures = bakedSimpleModel.getTextures();
		boolean bl = bakedSimpleModel.getAmbientOcclusion();
		Sprite sprite = bakedSimpleModel.getParticleTexture(modelTextures, baker);
		BakedGeometry bakedGeometry = bakedSimpleModel.bakeGeometry(modelTextures, baker, bakeSettings);
		Multimap<Identifier, Identifier> multimap = null;

		for (BakedQuad bakedQuad : bakedGeometry.getAllQuads()) {
			Sprite sprite2 = bakedQuad.sprite();
			if (!sprite2.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
				if (multimap == null) {
					multimap = HashMultimap.create();
				}

				multimap.put(sprite2.getAtlasId(), sprite2.getContents().getId());
			}
		}

		if (multimap != null) {
			LOGGER.warn(
					"Rejecting block model {}, since it contains sprites from outside of supported atlas: {}",
					id,
					multimap
			);
			return baker.getMissingBlockPart();
		}
		else {
			return new GeometryBakedModel(bakedGeometry, bl, sprite);
		}
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable Direction side) {
		return this.quads.getQuads(side);
	}
}
