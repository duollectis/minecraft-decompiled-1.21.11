package net.minecraft.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

/**
 * Явно заданный уровень освещённости для Display-сущностей.
 * Упаковывается в одно int-значение: биты 4–19 — блочный свет, биты 20–35 — небесный свет.
 */
public record Brightness(int block, int sky) {

	public static final Codec<Integer> LIGHT_LEVEL_CODEC = Codecs.rangedInt(0, 15);
	public static final Codec<Brightness> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(
							LIGHT_LEVEL_CODEC.fieldOf("block").forGetter(Brightness::block),
							LIGHT_LEVEL_CODEC.fieldOf("sky").forGetter(Brightness::sky)
					)
					.apply(instance, Brightness::new)
	);
	public static final Brightness FULL = new Brightness(15, 15);

	/** Упаковывает уровни блочного и небесного света в одно int-значение. */
	public static int pack(int block, int sky) {
		return block << 4 | sky << 20;
	}

	public int pack() {
		return pack(block, sky);
	}

	public static int unpackBlock(int packed) {
		return packed >> 4 & 65535;
	}

	public static int unpackSky(int packed) {
		return packed >> 20 & 65535;
	}

	public static Brightness unpack(int packed) {
		return new Brightness(unpackBlock(packed), unpackSky(packed));
	}
}
