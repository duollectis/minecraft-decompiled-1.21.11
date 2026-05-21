package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.ColorHelper;
import org.joml.*;

@Environment(EnvType.CLIENT)
/**
 * {@code VertexConsumer}.
 */
public interface VertexConsumer {

	VertexConsumer vertex(float x, float y, float z);

	VertexConsumer color(int red, int green, int blue, int alpha);

	VertexConsumer color(int argb);

	VertexConsumer texture(float u, float v);

	VertexConsumer overlay(int u, int v);

	VertexConsumer light(int u, int v);

	VertexConsumer normal(float x, float y, float z);

	VertexConsumer lineWidth(float width);

	default void vertex(
			float x,
			float y,
			float z,
			int color,
			float u,
			float v,
			int overlay,
			int light,
			float normalX,
			float normalY,
			float normalZ
	) {
		this.vertex(x, y, z);
		this.color(color);
		this.texture(u, v);
		this.overlay(overlay);
		this.light(light);
		this.normal(normalX, normalY, normalZ);
	}

	default VertexConsumer color(float red, float green, float blue, float alpha) {
		return this.color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}

	default VertexConsumer light(int uv) {
		return this.light(uv & 65535, uv >> 16 & 65535);
	}

	default VertexConsumer overlay(int uv) {
		return this.overlay(uv & 65535, uv >> 16 & 65535);
	}

	default void quad(
			MatrixStack.Entry matrixEntry,
			BakedQuad quad,
			float red,
			float green,
			float blue,
			float alpha,
			int light,
			int overlay
	) {
		this.quad(
				matrixEntry,
				quad,
				new float[]{1.0F, 1.0F, 1.0F, 1.0F},
				red,
				green,
				blue,
				alpha,
				new int[]{light, light, light, light},
				overlay
		);
	}

	default void quad(
			MatrixStack.Entry matrixEntry,
			BakedQuad quad,
			float[] brightnesses,
			float red,
			float green,
			float blue,
			float alpha,
			int[] lights,
			int overlay
	) {
		Vector3fc vector3fc = quad.face().getFloatVector();
		Matrix4f matrix4f = matrixEntry.getPositionMatrix();
		Vector3f vector3f = matrixEntry.transformNormal(vector3fc, new Vector3f());
		int i = quad.lightEmission();

		for (int j = 0; j < 4; j++) {
			Vector3fc vector3fc2 = quad.getPosition(j);
			long l = quad.getTexcoords(j);
			float f = brightnesses[j];
			int k = ColorHelper.fromFloats(alpha, f * red, f * green, f * blue);
			int m = LightmapTextureManager.applyEmission(lights[j], i);
			Vector3f vector3f2 = matrix4f.transformPosition(vector3fc2, new Vector3f());
			float g = Vector2f.getX(l);
			float h = Vector2f.getY(l);
			this.vertex(
					vector3f2.x(),
					vector3f2.y(),
					vector3f2.z(),
					k,
					g,
					h,
					overlay,
					m,
					vector3f.x(),
					vector3f.y(),
					vector3f.z()
			);
		}
	}

	default VertexConsumer vertex(Vector3fc vec) {
		return this.vertex(vec.x(), vec.y(), vec.z());
	}

	default VertexConsumer vertex(MatrixStack.Entry matrix, Vector3f vec) {
		return this.vertex(matrix, vec.x(), vec.y(), vec.z());
	}

	default VertexConsumer vertex(MatrixStack.Entry matrix, float x, float y, float z) {
		return this.vertex(matrix.getPositionMatrix(), x, y, z);
	}

	default VertexConsumer vertex(Matrix4fc matrix, float x, float y, float z) {
		Vector3f vector3f = matrix.transformPosition(x, y, z, new Vector3f());
		return this.vertex(vector3f.x(), vector3f.y(), vector3f.z());
	}

	default VertexConsumer vertex(Matrix3x2fc matrix, float x, float y) {
		org.joml.Vector2f vector2f = matrix.transformPosition(x, y, new org.joml.Vector2f());
		return this.vertex(vector2f.x(), vector2f.y(), 0.0F);
	}

	default VertexConsumer normal(MatrixStack.Entry matrix, float x, float y, float z) {
		Vector3f vector3f = matrix.transformNormal(x, y, z, new Vector3f());
		return this.normal(vector3f.x(), vector3f.y(), vector3f.z());
	}

	default VertexConsumer normal(MatrixStack.Entry matrix, Vector3f vec) {
		return this.normal(matrix, vec.x(), vec.y(), vec.z());
	}
}
