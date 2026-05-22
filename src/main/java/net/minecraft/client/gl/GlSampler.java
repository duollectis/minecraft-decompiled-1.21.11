package net.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL33C;

import java.util.OptionalDouble;

/**
 * Объект сэмплера OpenGL, инкапсулирующий параметры фильтрации и адресации текстур.
 * Создаётся через {@code GL33C.glGenSamplers()} и настраивается один раз при конструировании.
 */
@Environment(EnvType.CLIENT)
public class GlSampler extends GpuSampler {

	private static final int GL_TEXTURE_WRAP_S = 10242;
	private static final int GL_TEXTURE_WRAP_T = 10243;
	private static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 34046;
	private static final int GL_TEXTURE_MIN_FILTER = 10241;
	private static final int GL_TEXTURE_MAG_FILTER = 10240;
	private static final int GL_NEAREST_MIPMAP_LINEAR = 9986;
	private static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
	private static final int GL_NEAREST = 9728;
	private static final int GL_LINEAR = 9729;
	private static final int GL_TEXTURE_MAX_LOD = 33083;

	private final int samplerId;
	private final AddressMode addressModeU;
	private final AddressMode addressModeV;
	private final FilterMode minFilterMode;
	private final FilterMode magFilterMode;
	private final int maxAnisotropy;
	private final OptionalDouble maxLevelOfDetail;
	private boolean closed;

	public GlSampler(
		AddressMode addressModeU,
		AddressMode addressModeV,
		FilterMode minFilterMode,
		FilterMode magFilterMode,
		int maxAnisotropy,
		OptionalDouble maxLevelOfDetail
	) {
		this.addressModeU = addressModeU;
		this.addressModeV = addressModeV;
		this.minFilterMode = minFilterMode;
		this.magFilterMode = magFilterMode;
		this.maxAnisotropy = maxAnisotropy;
		this.maxLevelOfDetail = maxLevelOfDetail;
		samplerId = GL33C.glGenSamplers();

		GL33C.glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_S, GlConst.toGl(addressModeU));
		GL33C.glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_T, GlConst.toGl(addressModeV));

		if (maxAnisotropy > 1) {
			GL33C.glSamplerParameterf(samplerId, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
		}

		switch (minFilterMode) {
			case NEAREST -> GL33C.glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
			case LINEAR -> GL33C.glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		}

		switch (magFilterMode) {
			case NEAREST -> GL33C.glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			case LINEAR -> GL33C.glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		}

		if (maxLevelOfDetail.isPresent()) {
			GL33C.glSamplerParameterf(samplerId, GL_TEXTURE_MAX_LOD, (float) maxLevelOfDetail.getAsDouble());
		}
	}

	public int getSamplerId() {
		return samplerId;
	}

	@Override
	public AddressMode getAddressModeU() {
		return addressModeU;
	}

	@Override
	public AddressMode getAddressModeV() {
		return addressModeV;
	}

	@Override
	public FilterMode getMinFilterMode() {
		return minFilterMode;
	}

	@Override
	public FilterMode getMagFilterMode() {
		return magFilterMode;
	}

	@Override
	public int getMaxAnisotropy() {
		return maxAnisotropy;
	}

	@Override
	public OptionalDouble getMaxLevelOfDetail() {
		return maxLevelOfDetail;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}

		closed = true;
		GL33C.glDeleteSamplers(samplerId);
	}

	public boolean isClosed() {
		return closed;
	}
}
