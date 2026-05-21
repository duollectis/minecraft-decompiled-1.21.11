package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code TextureTransform}.
 */
public class TextureTransform {

	public static final double ATLAS_SCALE = 8.0;
	private final String name;
	private final Supplier<Matrix4f> transformSupplier;
	public static final TextureTransform DEFAULT_TEXTURING = new TextureTransform("default_texturing", Matrix4f::new);
	public static final TextureTransform
			GLINT_TEXTURING =
			new TextureTransform("glint_texturing", () -> getGlintTransformation(8.0F));
	public static final TextureTransform
			ENTITY_GLINT_TEXTURING =
			new TextureTransform("entity_glint_texturing", () -> getGlintTransformation(0.5F));
	public static final TextureTransform
			ARMOR_ENTITY_GLINT_TEXTURING =
			new TextureTransform("armor_entity_glint_texturing", () -> getGlintTransformation(0.16F));

	public TextureTransform(String name, Supplier<Matrix4f> transformSupplier) {
		this.name = name;
		this.transformSupplier = transformSupplier;
	}

	public Matrix4f getTransformSupplier() {
		return this.transformSupplier.get();
	}

	@Override
	public String toString() {
		return "TexturingStateShard[" + this.name + "]";
	}

	private static Matrix4f getGlintTransformation(float scale) {
		long
				l =
				(long) (Util.getMeasuringTimeMs() * MinecraftClient.getInstance().options.getGlintSpeed().getValue()
						* 8.0
				);
		float f = (float) (l % 110000L) / 110000.0F;
		float g = (float) (l % 30000L) / 30000.0F;
		Matrix4f matrix4f = new Matrix4f().translation(-f, g, 0.0F);
		matrix4f.rotateZ((float) (Math.PI / 18)).scale(scale);
		return matrix4f;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code OffsetTexturing}.
	 */
	public static final class OffsetTexturing extends TextureTransform {

		public OffsetTexturing(float du, float dv) {
			super("offset_texturing", () -> new Matrix4f().translation(du, dv, 0.0F));
		}
	}
}
