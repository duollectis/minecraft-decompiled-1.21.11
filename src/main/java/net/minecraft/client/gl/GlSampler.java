package net.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL33C;

import java.util.OptionalDouble;

@Environment(EnvType.CLIENT)
/**
 * {@code GlSampler}.
 */
public class GlSampler extends GpuSampler {

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
		this.samplerId = GL33C.glGenSamplers();
		GL33C.glSamplerParameteri(this.samplerId, 10242, GlConst.toGl(addressModeU));
		GL33C.glSamplerParameteri(this.samplerId, 10243, GlConst.toGl(addressModeV));
		if (maxAnisotropy > 1) {
			GL33C.glSamplerParameterf(this.samplerId, 34046, maxAnisotropy);
		}

		switch (minFilterMode) {
			case NEAREST:
				GL33C.glSamplerParameteri(this.samplerId, 10241, 9986);
				break;
			case LINEAR:
				GL33C.glSamplerParameteri(this.samplerId, 10241, 9987);
		}

		switch (magFilterMode) {
			case NEAREST:
				GL33C.glSamplerParameteri(this.samplerId, 10240, 9728);
				break;
			case LINEAR:
				GL33C.glSamplerParameteri(this.samplerId, 10240, 9729);
		}

		if (maxLevelOfDetail.isPresent()) {
			GL33C.glSamplerParameterf(this.samplerId, 33083, (float) maxLevelOfDetail.getAsDouble());
		}
	}

	public int getSamplerId() {
		return this.samplerId;
	}

	@Override
	public AddressMode getAddressModeU() {
		return this.addressModeU;
	}

	@Override
	public AddressMode getAddressModeV() {
		return this.addressModeV;
	}

	@Override
	public FilterMode getMinFilterMode() {
		return this.minFilterMode;
	}

	@Override
	public FilterMode getMagFilterMode() {
		return this.magFilterMode;
	}

	@Override
	public int getMaxAnisotropy() {
		return this.maxAnisotropy;
	}

	@Override
	public OptionalDouble getMaxLevelOfDetail() {
		return this.maxLevelOfDetail;
	}

	@Override
	public void close() {
		if (!this.closed) {
			this.closed = true;
			GL33C.glDeleteSamplers(this.samplerId);
		}
	}

	public boolean isClosed() {
		return this.closed;
	}
}
