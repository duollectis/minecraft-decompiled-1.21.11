package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelPartBuilder}.
 */
public class ModelPartBuilder {

	private static final Set<Direction> ALL_DIRECTIONS = EnumSet.allOf(Direction.class);
	private final List<ModelCuboidData> cuboidData = Lists.newArrayList();
	private int textureX;
	private int textureY;
	private boolean mirror;

	/**
	 * Uv.
	 *
	 * @param textureX texture x
	 * @param textureY texture y
	 *
	 * @return ModelPartBuilder — результат операции
	 */
	public ModelPartBuilder uv(int textureX, int textureY) {
		this.textureX = textureX;
		this.textureY = textureY;
		return this;
	}

	/**
	 * Mirrored.
	 *
	 * @return ModelPartBuilder — результат операции
	 */
	public ModelPartBuilder mirrored() {
		return this.mirrored(true);
	}

	/**
	 * Mirrored.
	 *
	 * @param mirror mirror
	 *
	 * @return ModelPartBuilder — результат операции
	 */
	public ModelPartBuilder mirrored(boolean mirror) {
		this.mirror = mirror;
		return this;
	}

	public ModelPartBuilder cuboid(
			String name,
			float offsetX,
			float offsetY,
			float offsetZ,
			int sizeX,
			int sizeY,
			int sizeZ,
			Dilation extra,
			int textureX,
			int textureY
	) {
		this.uv(textureX, textureY);
		this.cuboidData
				.add(
						new ModelCuboidData(
								name,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								extra,
								this.mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			String name,
			float offsetX,
			float offsetY,
			float offsetZ,
			int sizeX,
			int sizeY,
			int sizeZ,
			int textureX,
			int textureY
	) {
		this.uv(textureX, textureY);
		this.cuboidData
				.add(
						new ModelCuboidData(
								name,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								Dilation.NONE,
								this.mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	/**
	 * Cuboid.
	 *
	 * @param offsetX offset x
	 * @param offsetY offset y
	 * @param offsetZ offset z
	 * @param sizeX size x
	 * @param sizeY size y
	 * @param sizeZ size z
	 *
	 * @return ModelPartBuilder — результат операции
	 */
	public ModelPartBuilder cuboid(float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								null,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								Dilation.NONE,
								this.mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			float offsetX,
			float offsetY,
			float offsetZ,
			float sizeX,
			float sizeY,
			float sizeZ,
			Set<Direction> directions
	) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								null,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								Dilation.NONE,
								this.mirror,
								1.0F,
								1.0F,
								directions
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			String name,
			float offsetX,
			float offsetY,
			float offsetZ,
			float sizeX,
			float sizeY,
			float sizeZ
	) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								name,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								Dilation.NONE,
								this.mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			String name,
			float offsetX,
			float offsetY,
			float offsetZ,
			float sizeX,
			float sizeY,
			float sizeZ,
			Dilation extra
	) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								name,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								extra,
								this.mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			float offsetX,
			float offsetY,
			float offsetZ,
			float sizeX,
			float sizeY,
			float sizeZ,
			boolean mirror
	) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								null,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								Dilation.NONE,
								mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			float offsetX,
			float offsetY,
			float offsetZ,
			float sizeX,
			float sizeY,
			float sizeZ,
			Dilation extra,
			float textureScaleX,
			float textureScaleY
	) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								null,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								extra,
								this.mirror,
								textureScaleX,
								textureScaleY,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	public ModelPartBuilder cuboid(
			float offsetX,
			float offsetY,
			float offsetZ,
			float sizeX,
			float sizeY,
			float sizeZ,
			Dilation extra
	) {
		this.cuboidData
				.add(
						new ModelCuboidData(
								null,
								this.textureX,
								this.textureY,
								offsetX,
								offsetY,
								offsetZ,
								sizeX,
								sizeY,
								sizeZ,
								extra,
								this.mirror,
								1.0F,
								1.0F,
								ALL_DIRECTIONS
						)
				);
		return this;
	}

	/**
	 * Build.
	 *
	 * @return List — результат операции
	 */
	public List<ModelCuboidData> build() {
		return ImmutableList.copyOf(this.cuboidData);
	}

	/**
	 * Create.
	 *
	 * @return ModelPartBuilder — результат операции
	 */
	public static ModelPartBuilder create() {
		return new ModelPartBuilder();
	}
}
