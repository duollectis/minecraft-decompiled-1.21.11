package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.DyeColor;

/**
 * {@code DyedCarpetBlock}.
 */
public class DyedCarpetBlock extends CarpetBlock {

	public static final MapCodec<DyedCarpetBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							DyeColor.CODEC.fieldOf("color").forGetter(DyedCarpetBlock::getDyeColor),
							createSettingsCodec()
					)
					.apply(instance, DyedCarpetBlock::new)
	);
	private final DyeColor dyeColor;

	@Override
	public MapCodec<DyedCarpetBlock> getCodec() {
		return CODEC;
	}

	public DyedCarpetBlock(DyeColor dyeColor, AbstractBlock.Settings settings) {
		super(settings);
		this.dyeColor = dyeColor;
	}

	public DyeColor getDyeColor() {
		return this.dyeColor;
	}
}
