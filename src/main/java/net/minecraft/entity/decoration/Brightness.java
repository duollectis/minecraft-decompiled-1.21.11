package net.minecraft.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

/**
 * {@code Brightness}.
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

	/**
	 * Pack.
	 *
	 * @param block block
	 * @param sky sky
	 *
	 * @return int — результат операции
	 */
	public static int pack(int block, int sky) {
		return block << 4 | sky << 20;
	}

	/**
	 * Pack.
	 *
	 * @return int — результат операции
	 */
	public int pack() {
		return pack(this.block, this.sky);
	}

	/**
	 * Unpack block.
	 *
	 * @param packed packed
	 *
	 * @return int — результат операции
	 */
	public static int unpackBlock(int packed) {
		return packed >> 4 & 65535;
	}

	/**
	 * Unpack sky.
	 *
	 * @param packed packed
	 *
	 * @return int — результат операции
	 */
	public static int unpackSky(int packed) {
		return packed >> 20 & 65535;
	}

	/**
	 * Unpack.
	 *
	 * @param packed packed
	 *
	 * @return Brightness — результат операции
	 */
	public static Brightness unpack(int packed) {
		return new Brightness(unpackBlock(packed), unpackSky(packed));
	}
}
