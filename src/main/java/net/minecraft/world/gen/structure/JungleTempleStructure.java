package net.minecraft.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.structure.JungleTempleGenerator;

/**
 * Структура джунглевого храма — размещает один кусок {@link net.minecraft.structure.JungleTempleGenerator}
 * на поверхности джунглевого биома.
 */
public class JungleTempleStructure extends BasicTempleStructure {

	public static final MapCodec<JungleTempleStructure> CODEC = createCodec(JungleTempleStructure::new);

	public JungleTempleStructure(Structure.Config config) {
		super(JungleTempleGenerator::new, JungleTempleGenerator.TEMPLE_SIZE, 15, config);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.JUNGLE_TEMPLE;
	}
}
