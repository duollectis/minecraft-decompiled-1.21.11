package net.minecraft.client.render.model.json;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.*;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AxisRotation;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Незапечённая модель для предметов, генерируемая процедурно из спрайтов слоёв.
 * Создаёт геометрию куба для каждого непрозрачного пикселя каждого слоя ({@code layer0..layer4}).
 */
@Environment(EnvType.CLIENT)
public class GeneratedItemModel implements UnbakedModel {

	public static final Identifier GENERATED = Identifier.ofVanilla("builtin/generated");
	public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
	private static final float LAYER_OFFSET_MIN = 7.5F;
	private static final float LAYER_OFFSET_MAX = 8.5F;
	private static final ModelTextures.Textures
			TEXTURES =
			new ModelTextures.Textures.Builder().addTextureReference("particle", "layer0").build();
	private static final ModelElementFace.UV FACING_SOUTH_UV = new ModelElementFace.UV(0.0F, 0.0F, 16.0F, 16.0F);
	private static final ModelElementFace.UV FACING_NORTH_UV = new ModelElementFace.UV(16.0F, 0.0F, 0.0F, 16.0F);
	private static final float LAYER_DEPTH = 0.1F;

	@Override
	public ModelTextures.Textures textures() {
		return TEXTURES;
	}

	@Override
	public Geometry geometry() {
		return GeneratedItemModel::bakeGeometry;
	}

	@Override
	public UnbakedModel.@Nullable GuiLight guiLight() {
		return UnbakedModel.GuiLight.ITEM;
	}

	private static BakedGeometry bakeGeometry(
			ModelTextures textures,
			Baker baker,
			ModelBakeSettings settings,
			SimpleModel model
	) {
		List<ModelElement> list = new ArrayList<>();

		for (int i = 0; i < LAYERS.size(); i++) {
			String string = LAYERS.get(i);
			SpriteIdentifier spriteIdentifier = textures.get(string);
			if (spriteIdentifier == null) {
				break;
			}

			SpriteContents spriteContents = baker.getSpriteGetter().get(spriteIdentifier, model).getContents();
			list.addAll(addLayerElements(i, string, spriteContents));
		}

		return UnbakedGeometry.bakeGeometry(list, textures, baker, settings, model);
	}

	private static List<ModelElement> addLayerElements(int tintIndex, String name, SpriteContents spriteContents) {
		Map<Direction, ModelElementFace> map = Map.of(
				Direction.SOUTH,
				new ModelElementFace(null, tintIndex, name, FACING_SOUTH_UV, AxisRotation.R0),
				Direction.NORTH,
				new ModelElementFace(null, tintIndex, name, FACING_NORTH_UV, AxisRotation.R0)
		);
		List<ModelElement> list = new ArrayList<>();
		list.add(new ModelElement(new Vector3f(0.0F, 0.0F, LAYER_OFFSET_MIN), new Vector3f(16.0F, 16.0F, LAYER_OFFSET_MAX), map));
		list.addAll(addSubComponents(spriteContents, name, tintIndex));
		return list;
	}

	private static List<ModelElement> addSubComponents(SpriteContents spriteContents, String layerName, int tintIndex) {
		float scaleX = 16.0F / spriteContents.getWidth();
		float scaleY = 16.0F / spriteContents.getHeight();
		List<ModelElement> elements = new ArrayList<>();

		for (GeneratedItemModel.PixelFaceCoord coord : getFrames(spriteContents)) {
			float px = coord.x();
			float py = coord.y();
			GeneratedItemModel.Side side = coord.facing();
			float innerMin = px + 0.1F;
			float innerMax = px + 1.0F - 0.1F;
			float depthMin;
			float depthMax;

			if (side.isVertical()) {
				depthMin = py + LAYER_DEPTH;
				depthMax = py + 1.0F - LAYER_DEPTH;
			}
			else {
				depthMin = py + 1.0F - LAYER_DEPTH;
				depthMax = py + LAYER_DEPTH;
			}

			float u0 = px;
			float v0 = py;
			float u1 = px;
			float v1 = py;

			switch (side) {
				case UP:
					u1 = px + 1.0F;
					break;
				case DOWN:
					u1 = px + 1.0F;
					v0 = py + 1.0F;
					v1 = py + 1.0F;
					break;
				case LEFT:
					v1 = py + 1.0F;
					break;
				case RIGHT:
					u0 = px + 1.0F;
					u1 = px + 1.0F;
					v1 = py + 1.0F;
			}

			u0 *= scaleX;
			u1 *= scaleX;
			v0 *= scaleY;
			v1 *= scaleY;
			v0 = 16.0F - v0;
			v1 = 16.0F - v1;

			Map<Direction, ModelElementFace> faces = Map.of(
					side.getDirection(),
					new ModelElementFace(
							null,
							tintIndex,
							layerName,
							new ModelElementFace.UV(innerMin * scaleX, depthMin * scaleX, innerMax * scaleY, depthMax * scaleY),
							AxisRotation.R0
					)
			);

			switch (side) {
				case UP:
					elements.add(new ModelElement(
							new Vector3f(u0, v0, LAYER_OFFSET_MIN),
							new Vector3f(u1, v0, LAYER_OFFSET_MAX),
							faces
					));
					break;
				case DOWN:
					elements.add(new ModelElement(
							new Vector3f(u0, v1, LAYER_OFFSET_MIN),
							new Vector3f(u1, v1, LAYER_OFFSET_MAX),
							faces
					));
					break;
				case LEFT:
					elements.add(new ModelElement(
							new Vector3f(u0, v0, LAYER_OFFSET_MIN),
							new Vector3f(u0, v1, LAYER_OFFSET_MAX),
							faces
					));
					break;
				case RIGHT:
					elements.add(new ModelElement(
							new Vector3f(u1, v0, LAYER_OFFSET_MIN),
							new Vector3f(u1, v1, LAYER_OFFSET_MAX),
							faces
					));
			}
		}

		return elements;
	}

	private static Collection<GeneratedItemModel.PixelFaceCoord> getFrames(SpriteContents spriteContents) {
		int width = spriteContents.getWidth();
		int height = spriteContents.getHeight();
		Set<GeneratedItemModel.PixelFaceCoord> faces = new HashSet<>();

		spriteContents.getDistinctFrameCount().forEach(frame -> {
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					if (!isPixelTransparent(spriteContents, frame, col, row, width, height)) {
						buildCube(GeneratedItemModel.Side.UP, faces, spriteContents, frame, col, row, width, height);
						buildCube(GeneratedItemModel.Side.DOWN, faces, spriteContents, frame, col, row, width, height);
						buildCube(GeneratedItemModel.Side.LEFT, faces, spriteContents, frame, col, row, width, height);
						buildCube(GeneratedItemModel.Side.RIGHT, faces, spriteContents, frame, col, row, width, height);
					}
				}
			}
		});

		return faces;
	}

	private static void buildCube(
			GeneratedItemModel.Side side,
			Set<GeneratedItemModel.PixelFaceCoord> faces,
			SpriteContents spriteContents,
			int frame,
			int col,
			int row,
			int width,
			int height
	) {
		if (isPixelTransparent(
				spriteContents,
				frame,
				col - side.direction.getOffsetX(),
				row - side.direction.getOffsetY(),
				width,
				height
		)) {
			faces.add(new GeneratedItemModel.PixelFaceCoord(side, col, row));
		}
	}

	private static boolean isPixelTransparent(SpriteContents spriteContents, int frame, int col, int row, int width, int height) {
		return col >= 0 && row >= 0 && col < width && row < height
		       ? spriteContents.isPixelTransparent(frame, col, row)
		       : true;
	}

	/**
	 * Направление грани пикселя при генерации геометрии предмета.
	 */
	@Environment(EnvType.CLIENT)
	enum Side {
		UP(Direction.UP),
		DOWN(Direction.DOWN),
		LEFT(Direction.EAST),
		RIGHT(Direction.WEST);

		final Direction direction;

		private Side(final Direction direction) {
			this.direction = direction;
		}

		public Direction getDirection() {
			return this.direction;
		}

		boolean isVertical() {
			return this == DOWN || this == UP;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * Координата пикселя с направлением грани для генерации геометрии предмета.
	 */
	record PixelFaceCoord(GeneratedItemModel.Side facing, int x, int y) {
	}
}
