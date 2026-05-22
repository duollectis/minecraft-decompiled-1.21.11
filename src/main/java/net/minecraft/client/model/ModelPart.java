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

/**
 * Часть иерархической модели сущности. Хранит список кубоидов и дочерних частей.
 * Поддерживает трансформации (позиция, углы Эйлера, масштаб), рендер и обход дерева.
 * Масштаб координат: 1 единица модели = 1/16 блока (см. {@link Vertex#SCALE_FACTOR}).
 */
@Environment(EnvType.CLIENT)
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
		return ModelTransform.of(originX, originY, originZ, pitch, yaw, roll);
	}

	public ModelTransform getDefaultTransform() {
		return defaultTransform;
	}

	public void setDefaultTransform(ModelTransform transform) {
		defaultTransform = transform;
	}

	public void resetTransform() {
		setTransform(defaultTransform);
	}

	public void setTransform(ModelTransform transform) {
		originX = transform.x();
		originY = transform.y();
		originZ = transform.z();
		pitch = transform.pitch();
		yaw = transform.yaw();
		roll = transform.roll();
		xScale = transform.xScale();
		yScale = transform.yScale();
		zScale = transform.zScale();
	}

	public boolean hasChild(String child) {
		return children.containsKey(child);
	}

	/**
	 * Возвращает дочернюю часть по имени.
	 *
	 * @throws NoSuchElementException если часть с таким именем не найдена
	 */
	public ModelPart getChild(String name) {
		ModelPart part = children.get(name);

		if (part == null) {
			throw new NoSuchElementException("Can't find part " + name);
		}

		return part;
	}

	public void setOrigin(float x, float y, float z) {
		originX = x;
		originY = y;
		originZ = z;
	}

	public void setAngles(float pitch, float yaw, float roll) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
	}

	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		render(matrices, vertices, light, overlay, -1);
	}

	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		if (!visible) {
			return;
		}

		if (cuboids.isEmpty() && children.isEmpty()) {
			return;
		}

		matrices.push();
		applyTransform(matrices);

		if (!hidden) {
			renderCuboids(matrices.peek(), vertices, light, overlay, color);
		}

		for (ModelPart child : children.values()) {
			child.render(matrices, vertices, light, overlay, color);
		}

		matrices.pop();
	}

	/**
	 * Применяет кватернион вращения к текущим углам Эйлера части.
	 * Используется для программной анимации (например, физика волос/плаща).
	 */
	public void rotate(Quaternionf quaternion) {
		Matrix3f rotationMatrix = new Matrix3f().rotationZYX(roll, yaw, pitch);
		Matrix3f rotated = rotationMatrix.rotate(quaternion);
		Vector3f eulerAngles = rotated.getEulerAnglesZYX(new Vector3f());
		setAngles(eulerAngles.x, eulerAngles.y, eulerAngles.z);
	}

	/**
	 * Обходит все кубоиды дерева и передаёт мировые координаты вершин в коллектор.
	 * Используется для вычисления хитбоксов и точек крепления.
	 */
	public void collectVertices(MatrixStack matrices, Consumer<Vector3fc> collector) {
		forEachCuboid(
				matrices, (matrix, path, index, cuboid) -> {
					for (ModelPart.Quad quad : cuboid.sides) {
						for (ModelPart.Vertex vertex : quad.vertices()) {
							Vector3f worldPos = matrix
									.getPositionMatrix()
									.transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
							collector.accept(worldPos);
						}
					}
				}
		);
	}

	public void forEachCuboid(MatrixStack matrices, ModelPart.CuboidConsumer consumer) {
		forEachCuboid(matrices, consumer, "");
	}

	private void forEachCuboid(MatrixStack matrices, ModelPart.CuboidConsumer consumer, String path) {
		if (cuboids.isEmpty() && children.isEmpty()) {
			return;
		}

		matrices.push();
		applyTransform(matrices);
		MatrixStack.Entry entry = matrices.peek();

		for (int index = 0; index < cuboids.size(); index++) {
			consumer.accept(entry, path, index, cuboids.get(index));
		}

		String childPath = path + "/";
		children.forEach((name, part) -> part.forEachCuboid(matrices, consumer, childPath + name));
		matrices.pop();
	}

	/**
	 * Применяет трансформацию части к стеку матриц: перенос, вращение ZYX, масштаб.
	 * Координаты делятся на {@link Vertex#SCALE_FACTOR} (16) для перевода в мировые единицы.
	 */
	public void applyTransform(MatrixStack matrices) {
		matrices.translate(originX / Vertex.SCALE_FACTOR, originY / Vertex.SCALE_FACTOR, originZ / Vertex.SCALE_FACTOR);

		if (pitch != 0.0F || yaw != 0.0F || roll != 0.0F) {
			matrices.multiply(new Quaternionf().rotationZYX(roll, yaw, pitch));
		}

		if (xScale != 1.0F || yScale != 1.0F || zScale != 1.0F) {
			matrices.scale(xScale, yScale, zScale);
		}
	}

	private void renderCuboids(MatrixStack.Entry entry, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		for (ModelPart.Cuboid cuboid : cuboids) {
			cuboid.renderCuboid(entry, vertexConsumer, light, overlay, color);
		}
	}

	public ModelPart.Cuboid getRandomCuboid(Random random) {
		return cuboids.get(random.nextInt(cuboids.size()));
	}

	public boolean isEmpty() {
		return cuboids.isEmpty();
	}

	public void moveOrigin(Vector3f delta) {
		originX += delta.x();
		originY += delta.y();
		originZ += delta.z();
	}

	public void rotate(Vector3f delta) {
		pitch += delta.x();
		yaw += delta.y();
		roll += delta.z();
	}

	public void scale(Vector3f delta) {
		xScale += delta.x();
		yScale += delta.y();
		zScale += delta.z();
	}

	/**
	 * Возвращает иммутабельный список всех частей дерева (BFS-обход, начиная с корня).
	 */
	public List<ModelPart> traverse() {
		List<ModelPart> result = new ArrayList<>();
		result.add(this);
		forEachChild((key, part) -> result.add(part));
		return List.copyOf(result);
	}

	/**
	 * Создаёт функцию поиска части по имени, включая корень под именем {@code "root"}.
	 * Возвращает {@code null} для неизвестных имён.
	 */
	public Function<String, @Nullable ModelPart> createPartGetter() {
		Map<String, ModelPart> partMap = new HashMap<>();
		partMap.put("root", this);
		forEachChild(partMap::putIfAbsent);
		return partMap::get;
	}

	private void forEachChild(BiConsumer<String, ModelPart> consumer) {
		for (Entry<String, ModelPart> entry : children.entrySet()) {
			consumer.accept(entry.getKey(), entry.getValue());
		}

		for (ModelPart child : children.values()) {
			child.forEachChild(consumer);
		}
	}

	// ─── Вложенные типы ─────────────────────────────────────────────────────────

	/**
	 * Кубоид модели: шесть граней (Quad) с UV-координатами, вычисленными из
	 * позиции в текстурном атласе. Поддерживает зеркалирование по оси X.
	 */
	@Environment(EnvType.CLIENT)
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
				Set<Direction> visibleSides
		) {
			minX = x;
			minY = y;
			minZ = z;
			maxX = x + sizeX;
			maxY = y + sizeY;
			maxZ = z + sizeZ;
			sides = new ModelPart.Quad[visibleSides.size()];

			float x2 = x + sizeX;
			float y2 = y + sizeY;
			float z2 = z + sizeZ;
			x -= extraX;
			y -= extraY;
			z -= extraZ;
			x2 += extraX;
			y2 += extraY;
			z2 += extraZ;

			if (mirror) {
				float temp = x2;
				x2 = x;
				x = temp;
			}

			ModelPart.Vertex v0 = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
			ModelPart.Vertex v1 = new ModelPart.Vertex(x2, y, z, 0.0F, 8.0F);
			ModelPart.Vertex v2 = new ModelPart.Vertex(x2, y2, z, 8.0F, 8.0F);
			ModelPart.Vertex v3 = new ModelPart.Vertex(x, y2, z, 8.0F, 0.0F);
			ModelPart.Vertex v4 = new ModelPart.Vertex(x, y, z2, 0.0F, 0.0F);
			ModelPart.Vertex v5 = new ModelPart.Vertex(x2, y, z2, 0.0F, 8.0F);
			ModelPart.Vertex v6 = new ModelPart.Vertex(x2, y2, z2, 8.0F, 8.0F);
			ModelPart.Vertex v7 = new ModelPart.Vertex(x, y2, z2, 8.0F, 0.0F);

			float uWest = u;
			float uNorth = u + sizeZ;
			float uEast = u + sizeZ + sizeX;
			float uEast2 = u + sizeZ + sizeX + sizeX;
			float uSouth = u + sizeZ + sizeX + sizeZ;
			float uSouth2 = u + sizeZ + sizeX + sizeZ + sizeX;
			float vTop = v;
			float vMid = v + sizeZ;
			float vBot = v + sizeZ + sizeY;

			int sideIndex = 0;

			if (visibleSides.contains(Direction.DOWN)) {
				sides[sideIndex++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{v5, v4, v0, v1},
						uNorth, vTop, uEast, vMid,
						textureWidth, textureHeight, mirror, Direction.DOWN
				);
			}

			if (visibleSides.contains(Direction.UP)) {
				sides[sideIndex++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{v2, v3, v7, v6},
						uEast, vMid, uEast2, vTop,
						textureWidth, textureHeight, mirror, Direction.UP
				);
			}

			if (visibleSides.contains(Direction.WEST)) {
				sides[sideIndex++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{v0, v4, v7, v3},
						uWest, vMid, uNorth, vBot,
						textureWidth, textureHeight, mirror, Direction.WEST
				);
			}

			if (visibleSides.contains(Direction.NORTH)) {
				sides[sideIndex++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{v1, v0, v3, v2},
						uNorth, vMid, uEast, vBot,
						textureWidth, textureHeight, mirror, Direction.NORTH
				);
			}

			if (visibleSides.contains(Direction.EAST)) {
				sides[sideIndex++] = new ModelPart.Quad(
						new ModelPart.Vertex[]{v5, v1, v2, v6},
						uEast, vMid, uSouth, vBot,
						textureWidth, textureHeight, mirror, Direction.EAST
				);
			}

			if (visibleSides.contains(Direction.SOUTH)) {
				sides[sideIndex] = new ModelPart.Quad(
						new ModelPart.Vertex[]{v4, v5, v6, v7},
						uSouth, vMid, uSouth2, vBot,
						textureWidth, textureHeight, mirror, Direction.SOUTH
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
			Matrix4f posMatrix = entry.getPositionMatrix();
			Vector3f normalBuf = new Vector3f();

			for (ModelPart.Quad quad : sides) {
				Vector3f normal = entry.transformNormal(quad.direction, normalBuf);
				float nx = normal.x();
				float ny = normal.y();
				float nz = normal.z();

				for (ModelPart.Vertex vertex : quad.vertices) {
					Vector3f pos = posMatrix.transformPosition(
							vertex.worldX(), vertex.worldY(), vertex.worldZ(), normalBuf
					);
					vertexConsumer.vertex(
							pos.x(), pos.y(), pos.z(),
							color,
							vertex.u, vertex.v,
							overlay, light,
							nx, ny, nz
					);
				}
			}
		}
	}

	/** Функциональный интерфейс для обхода кубоидов с контекстом матрицы и пути. */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface CuboidConsumer {

		void accept(MatrixStack.Entry matrix, String path, int index, ModelPart.Cuboid cuboid);
	}

	/**
	 * Четырёхугольная грань кубоида с UV-координатами и нормалью.
	 * Конструктор вычисляет UV из позиции в текстурном атласе и применяет зеркалирование.
	 */
	@Environment(EnvType.CLIENT)
	public record Quad(ModelPart.Vertex[] vertices, Vector3fc direction) {

		public Quad(
				ModelPart.Vertex[] vertices,
				float u1,
				float v1,
				float u2,
				float v2,
				float textureWidth,
				float textureHeight,
				boolean flip,
				Direction direction
		) {
			this(vertices, (flip ? getMirrorDirection(direction) : direction).getFloatVector());
			vertices[0] = vertices[0].remap(u2 / textureWidth, v1 / textureHeight);
			vertices[1] = vertices[1].remap(u1 / textureWidth, v1 / textureHeight);
			vertices[2] = vertices[2].remap(u1 / textureWidth, v2 / textureHeight);
			vertices[3] = vertices[3].remap(u2 / textureWidth, v2 / textureHeight);

			if (flip) {
				int count = vertices.length;

				for (int left = 0; left < count / 2; left++) {
					ModelPart.Vertex temp = vertices[left];
					vertices[left] = vertices[count - 1 - left];
					vertices[count - 1 - left] = temp;
				}
			}
		}

		private static Direction getMirrorDirection(Direction direction) {
			return direction.getAxis() == Direction.Axis.X ? direction.getOpposite() : direction;
		}
	}

	/** Вершина кубоида в пространстве модели. Координаты в единицах 1/16 блока. */
	@Environment(EnvType.CLIENT)
	public record Vertex(float x, float y, float z, float u, float v) {

		public static final float SCALE_FACTOR = 16.0F;

		public ModelPart.Vertex remap(float newU, float newV) {
			return new ModelPart.Vertex(x, y, z, newU, newV);
		}

		public float worldX() {
			return x / SCALE_FACTOR;
		}

		public float worldY() {
			return y / SCALE_FACTOR;
		}

		public float worldZ() {
			return z / SCALE_FACTOR;
		}
	}
}
