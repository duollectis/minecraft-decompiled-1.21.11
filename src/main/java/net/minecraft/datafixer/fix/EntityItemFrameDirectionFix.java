package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Конвертирует старый числовой формат направления рамки для предмета {@code Facing}
 * из системы координат блоков (0–3) в систему направлений сущностей (2–5).
 */
public class EntityItemFrameDirectionFix extends ChoiceFix {

	public EntityItemFrameDirectionFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType, "EntityItemFrameDirectionFix", TypeReferences.ENTITY, "minecraft:item_frame");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixDirection);
	}

	private Dynamic<?> fixDirection(Dynamic<?> itemFrame) {
		return itemFrame.set(
				"Facing",
				itemFrame.createByte(remapDirection(itemFrame.get("Facing").asByte((byte) 0)))
		);
	}

	private static byte remapDirection(byte oldDirection) {
		return switch (oldDirection) {
			case 0 -> 3;
			case 1 -> 4;
			case 3 -> 5;
			default -> 2;
		};
	}
}
