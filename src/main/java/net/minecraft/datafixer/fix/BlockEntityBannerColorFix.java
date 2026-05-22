package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Инвертирует цвет баннера и его узоров: старый формат хранил цвет как {@code 15 - color},
 * новый формат использует прямой индекс цвета (0 = белый, 15 = чёрный).
 */
public class BlockEntityBannerColorFix extends ChoiceFix {

	private static final int MAX_COLOR_INDEX = 15;

	public BlockEntityBannerColorFix(Schema schema, boolean changesType) {
		super(schema, changesType, "BlockEntityBannerColorFix", TypeReferences.BLOCK_ENTITY, "minecraft:banner");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixBannerColor);
	}

	private Dynamic<?> fixBannerColor(Dynamic<?> banner) {
		banner = banner.update("Base", base -> base.createInt(MAX_COLOR_INDEX - base.asInt(0)));

		return banner.update(
			"Patterns",
			patterns -> (Dynamic<?>) DataFixUtils.orElse(
				patterns.asStreamOpt()
					.map(stream -> stream.map(
						pattern -> pattern.update(
							"Color",
							color -> color.createInt(MAX_COLOR_INDEX - color.asInt(0))
						)
					))
					.map(patterns::createList)
					.result(),
				patterns
			)
		);
	}
}
