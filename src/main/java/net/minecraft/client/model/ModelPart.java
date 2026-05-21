package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.joml.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelPart}.
 */
public final class ModelPart {

	public static final float SCALE_FACTOR = 1.0F;
	public float originX;
	public float originY;
	public float originZ;
	public float pitch;
	public float yaw;
	public float roll;
	public float xScale = 1.0F;
	public float yScale = 1.0F;
	public float zScale = 1.0F;
	public boolean visible = true;
	public boolean hidden;
	private final List<ModelPart.Cuboid> cuboids;
	private final Map<String, ModelPart> children;
	private ModelTransform defaultTransform = ModelTransform.NONE;

	public ModelPart(List<ModelPart.Cuboid> cuboids, Map<String, ModelPart> children) {
		this.cuboids = cuboids;
		this.children = children;
	}

	public ModelTransform getTransform() {
		return ModelTransform.of(this.originX, this.originY, this.originZ, this.pitch, this.yaw, this.roll);
	}

	public ModelTransform getDefaultTransform() {
		return this.defaultTransform;
	}

	public void setDefaultTransform(ModelTransform transform) {
		this.defaultTransform = transform;
	}

	public void resetTransform() {
		this.setTransform(this.defaultTransform);
	}

	public void setTransform(ModelTransform transform) {
		this.originX = transform.x();
		this.originY = transform.y();
		this.originZ = transform.z();
		this.pitch = transform.pitch();
		this.yaw = transform.yaw();
		this.roll = transform.roll();
		this.xScale = transform.xScale();
		this.yScale = transform.yScale();
		this.zScale = transform.zScale();
	}

	public boolean hasChild(String child) {
		return this.children.containsKey(child);
	}

	public ModelPart getChild(String name) {
		ModelPart modelPart = this.children.get(name);
		if (modelPart == null) {
			throw new NoSuchElementException("Can't find part " + name);
		}
		else {
			return modelPart;
		}
	}

	public void setOrigin(float x, float y, float z) {
		this.originX = x;
		this.originY = y;
		this.originZ = z;
	}

	public void setAngles(float pitch, float yaw, float roll) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
	}

	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.render(matrices, vertices, light, overlay, -1);
	}

	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		if (this.visible) {
			if (!this.cuboids.isEmpty() || !this.children.isEmpty()) {
				matrices.push();
				this.applyTransform(matrices);
				if (!this.hidden) {
					this.renderCuboids(matrices.peek(), vertices, light, overlay, color);
				}

				for (ModelPart modelPart : this.children.values()) {
					modelPart.render(matrices, vertices, light, overlay, color);
				}

				matrices.pop();
			}
		}
	}

	public void rotate(Quaternionf quaternion) {
		Matrix3f matrix3f = new Matrix3f().rotationZYX(this.roll, this.yaw, this.pitch);
		Matrix3f matrix3f2 = matrix3f.rotate(quaternion);
		Vector3f vector3f = matrix3f2.getEulerAnglesZYX(new Vector3f());
		this.setAngles(vector3f.x, vector3f.y, vector3f.z);
	}

	public void collectVertices(MatrixStack matrices, Consumer<Vector3fc> collector) {
		this.forEachCuboid(
				matrices, (matrix, path, index, cuboid) -> {
					for (ModelPart.Quad quad : cuboid.sides) {
						for (ModelPart.Vertex vertex : quad.vertices()) {
							float f = vertex.worldX();
							float g = vertex.worldY();
							float h = vertex.worldZ();
							Vector3f vector3f = matrix.getPositionMatrix().transformPosition(f, g, h, new Vector3f());
							collector.accept(vector3f);
						}
					}
				}
		);
	}

	public void forEachCuboid(MatrixStack matrices, ModelPart.CuboidConsumer consumer) {
		this.forEachCuboid(matrices, consumer, "");
	}

	private void forEachCuboid(MatrixStack matrices, ModelPart.CuboidConsumer consumer, String path) {
		if (!this.cuboids.isEmpty() || !this.children.isEmpty()) {
			matrices.push();
			this.applyTransform(matrices);
			MatrixStack.Entry entry = matrices.peek();

			for (int i = 0; i < this.cuboids.size(); i++) {
				consumer.accept(entry, path, i, this.cuboids.get(i));
			}

			String string = path + "/";
			this.children.forEach((name, part) -> part.forEachCuboid(matrices, consumer, string + name));
			matrices.pop();
		}
	}

	public void applyTransform(MatrixStack matrices) {
		matrices.translate(this.originX / 16.0F, this.originY / 16.0F, this.originZ / 16.0F);
		if (this.pitch != 0.0F || this.yaw != 0.0F || this.roll != 0.0F) {
			matrices.multiply(new Quaternionf().rotationZYX(this.roll, this.yaw, this.pitch));
		}

		if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
			matrices.scale(this.xScale, this.yScale, this.zScale);
		}
	}

	private void renderCuboids(
			MatrixStack.Entry entry,
			VertexConsumer vertexConsumer,
			int light,
			int overlay,
			int color
	) {
		for (ModelPart.Cuboid cuboid : this.cuboids) {
			cuboid.renderCuboid(entry, vertexConsumer, light, overlay, color);
		}
	}

	public ModelPart.Cuboid getRandomCuboid(Random random) {
		return this.cuboids.get(random.nextInt(this.cuboids.size()));
	}

	public boolean isEmpty() {
		return this.cuboids.isEmpty();
	}

	public void moveOrigin(Vector3f vec3f) {
		this.originX = this.originX + vec3f.x();
		this.originY = this.originY + vec3f.y();
		this.originZ = this.originZ + vec3f.z();
	}

	public void rotate(Vector3f vec3f) {
		this.pitch = this.pitch + vec3f.x();
		this.yaw = this.yaw + vec3f.y();
		this.roll = this.roll + vec3f.z();
	}

	public void scale(Vector3f vec3f) {
		this.xScale = this.xScale + vec3f.x();
		this.yScale = this.yScale + vec3f.y();
		this.zScale = this.zScale + vec3f.z();
	}

	public List<ModelPart> traverse() {
		List<ModelPart> list = new ArrayList<>();
		list.add(this);
		this.forEachChild((key, part) -> list.add(part));
		return List.copyOf(list);
	}

	public Function<String, @Nullable ModelPart> createPartGetter() {
		Map<String, ModelPart> map = new HashMap<>();
		map.put("root", this);
		this.forEachChild(map::putIfAbsent);
		return map::get;
	}

	private void forEachChild(BiConsumer<String, ModelPart> partBiConsumer) {
		for (Entry<String, ModelPart> entry : this.children.entrySet()) {
			partBiConsumer.accept(entry.getKey(), entry.getValue());
		}

		for (ModelPart modelPart : this.children.values()) {
			modelPart.forEachChild(partBiConsumer);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Cuboid}.
	 */
	public static class Cuboid {

		public final ModelPart.Quad[] sides;
		public final float minX;
		public final float minY;
		public final float minZ;
		public final float maxX;
		public final float maxY;
		public final float maxZ;

		public Cuboid(
				int u,
				int v,
				float x,
				float y,
				float z,
				float sizeX,
				float sizeY,
				float sizeZ,
				float extraX,
				float extraY,
				float extraZ,
				boolean mirror,
				float textureWidth,
				float textureHeight,
				Set<Direction> sides
		) {
			this.minX = x;
			this.minY = y;
			this.minZ = z;
			this.maxX = x + sizeX;
			this.maxY = y + sizeY;
			this.maxZ = z + sizeZ;
			this.sides = new ModelPart.Quad[sides.size()];
			float f = x + sizeX;
			float g = y + sizeY;
			float h = z + sizeZ;
			x -= extraX;
			y -= extraY;
			z -= extraZ;
			f += extraX;
			g += extraY;
			h += extraZ;
			if (mirror) {
				float i = f;
				f = x;
				x = i;
			}

			ModelPart.Vertex vertex = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
			ModelPart.Vertex vertex2 = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
			ModelPart.Vertex vertex3 = new ModelPart.Vertex(f, g, z, 8.0F, 8.0F);
			ModelPart.Vertex vertex4 = new ModelPart.Vertex(x, g, z, 8.0F, 0.0F);
			ModelPart.Vertex vertex5 = new ModelPart.Vertex(x, y, h, 0.0F, 0.0F);
			ModelPart.Vertex vertex6 = new ModelPart.Vertex(f, y, h, 0.0F, 8.0F);
			ModelPart.Vertex vertex7 = new ModelPart.Vertex(f, g, h, 8.0F, 8.0F);
			ModelPart.Vertex vertex8 = new ModelPart.Vertex(x, g, h, 8.0F, 0.0F);
			float j = u;
			float k = u + sizeZ;
			float l = u + sizeZ + sizeX;
			float m = u + sizeZ + sizeX + sizeX;
			float n = u + sizeZ + sizeX + sizeZ;
			float o = u + sizeZ + sizeX + sizeZ + sizeX;
			float p = v;
			float q = v + sizeZ;
			float r = v + sizeZ + sizeY;
			int s = 0;
			if (sides.contains(Direction.DOWN)) {
				this.sides[s++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{vertex6, vertex5, vertex, vertex2},
						k,
						p,
						l,
						q,
						textureWidth,
						textureHeight,
						mirror,
						Direction.DOWN
				);
			}

			if (sides.contains(Direction.UP)) {
				this.sides[s++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{vertex3, vertex4, vertex8, vertex7},
						l,
						q,
						m,
						p,
						textureWidth,
						textureHeight,
						mirror,
						Direction.UP
				);
			}

			if (sides.contains(Direction.WEST)) {
				this.sides[s++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{vertex, vertex5, vertex8, vertex4},
						j,
						q,
						k,
						r,
						textureWidth,
						textureHeight,
						mirror,
						Direction.WEST
				);
			}

			if (sides.contains(Direction.NORTH)) {
				this.sides[s++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{vertex2, vertex, vertex4, vertex3},
						k,
						q,
						l,
						r,
						textureWidth,
						textureHeight,
						mirror,
						Direction.NORTH
				);
			}

			if (sides.contains(Direction.EAST)) {
				this.sides[s++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{vertex6, vertex2, vertex3, vertex7},
						l,
						q,
						n,
						r,
						textureWidth,
						textureHeight,
						mirror,
						Direction.EAST
				);
			}

			if (sides.contains(Direction.SOUTH)) {
				this.sides[s] = new ModelPart.Quad(
						new ModelPart.Vertex[]{vertex5, vertex6, vertex7, vertex8},
						n,
						q,
						o,
						r,
						textureWidth,
						textureHeight,
						mirror,
						Direction.SOUTH
				);
			}
		}

		public void renderCuboid(
				MatrixStack.Entry entry,
				VertexConsumer vertexConsumer,
				int light,
				int overlay,
				int color
		) {
			Matrix4f matrix4f = entry.getPositionMatrix();
			Vector3f vector3f = new Vector3f();

			for (ModelPart.Quad quad : this.sides) {
				Vector3f vector3f2 = entry.transformNormal(quad.direction, vector3f);
				float f = vector3f2.x();
				float g = vector3f2.y();
				float h = vector3f2.z();

				for (ModelPart.Vertex vertex : quad.vertices) {
					float i = vertex.worldX();
					float j = vertex.worldY();
					float k = vertex.worldZ();
					Vector3f vector3f3 = matrix4f.transformPosition(i, j, k, vector3f);
					vertexConsumer.vertex(
							vector3f3.x(),
							vector3f3.y(),
							vector3f3.z(),
							color,
							vertex.u,
							vertex.v,
							overlay,
							light,
							f,
							g,
							h
					);
				}
			}
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code CuboidConsumer}.
	 */
	public interface CuboidConsumer {

		void accept(MatrixStack.Entry matrix, String path, int index, ModelPart.Cuboid cuboid);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Quad}.
	 */
	public record Quad(ModelPart.Vertex[] vertices, Vector3fc direction) {

		public Quad(
				ModelPart.Vertex[] vertices,
				float u1,
				float v1,
				float u2,
				float v2,
				float squishU,
				float squishV,
				boolean flip,
				Direction direction
		) {
			this(vertices, (flip ? getMirrorDirection(direction) : direction).getFloatVector());
			float f = 0.0F / squishU;
			float g = 0.0F / squishV;
			vertices[0] = vertices[0].remap(u2 / squishU - f, v1 / squishV + g);
			vertices[1] = vertices[1].remap(u1 / squishU + f, v1 / squishV + g);
			vertices[2] = vertices[2].remap(u1 / squishU + f, v2 / squishV - g);
			vertices[3] = vertices[3].remap(u2 / squishU - f, v2 / squishV - g);
			if (flip) {
				int i = vertices.length;

				for (int j = 0; j < i / 2; j++) {
					ModelPart.Vertex vertex = vertices[j];
					vertices[j] = vertices[i - 1 - j];
					vertices[i - 1 - j] = vertex;
				}
			}
		}

		private static Direction getMirrorDirection(Direction direction) {
			return direction.getAxis() == Direction.Axis.X ? direction.getOpposite() : direction;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Vertex}.
	 */
	public record Vertex(float x, float y, float z, float u, float v) {

		public static final float SCALE_FACTOR = 16.0F;

		public ModelPart.Vertex remap(float u, float v) {
			return new ModelPart.Vertex(this.x, this.y, this.z, u, v);
		}

		public float worldX() {
			return this.x / 16.0F;
		}

		public float worldY() {
			return this.y / 16.0F;
		}

		public float worldZ() {
			return this.z / 16.0F;
		}
	}
}
