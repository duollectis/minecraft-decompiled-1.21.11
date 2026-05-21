package net.minecraft.client.texture;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

@Environment(EnvType.CLIENT)
/**
 * {@code MipmapStrategy}.
 */
public enum MipmapStrategy implements StringIdentifiable {
	AUTO("auto"),
	MEAN("mean"),
	CUTOUT("cutout"),
	STRICT_CUTOUT("strict_cutout"),
	DARK_CUTOUT("dark_cutout");

	public static final Codec<MipmapStrategy> CODEC = StringIdentifiable.createBasicCodec(MipmapStrategy::values);
	private final String name;

	private MipmapStrategy(final String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return this.name;
	}
}
