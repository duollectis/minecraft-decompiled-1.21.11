package net.minecraft.client.gl;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.OptionalDouble;

@Environment(EnvType.CLIENT)
/**
 * {@code GpuSampler}.
 */
public abstract class GpuSampler implements AutoCloseable {

	public abstract AddressMode getAddressModeU();

	public abstract AddressMode getAddressModeV();

	public abstract FilterMode getMinFilterMode();

	public abstract FilterMode getMagFilterMode();

	public abstract int getMaxAnisotropy();

	public abstract OptionalDouble getMaxLevelOfDetail();

	@Override
	public abstract void close();
}
