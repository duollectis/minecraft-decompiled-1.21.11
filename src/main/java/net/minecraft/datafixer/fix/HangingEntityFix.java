package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет позицию подвешенных сущностей (картин и рамок для предметов):
 * переводит устаревшее поле {@code Direction}/{@code Dir} в {@code Facing}
 * и корректирует координаты {@code TileX/Y/Z} с учётом смещения по направлению.
 * Для рамок также удваивает {@code ItemRotation} (изменение шага поворота).
 */
public class HangingEntityFix extends DataFix {

	private static final int[][] DIRECTION_OFFSETS = {{0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}};

	public HangingEntityFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	public TypeRewriteRule makeRule() {
		Type<?> paintingType = getInputSchema().getChoiceType(TypeReferences.ENTITY, "Painting");
		OpticFinder<?> paintingFinder = DSL.namedChoice("Painting", paintingType);

		Type<?> itemFrameType = getInputSchema().getChoiceType(TypeReferences.ENTITY, "ItemFrame");
		OpticFinder<?> itemFrameFinder = DSL.namedChoice("ItemFrame", itemFrameType);

		Type<?> entityType = getInputSchema().getType(TypeReferences.ENTITY);

		TypeRewriteRule paintingRule = fixTypeEverywhereTyped(
			"EntityPaintingFix",
			entityType,
			entity -> entity.updateTyped(
				paintingFinder,
				paintingType,
				painting -> painting.update(
					DSL.remainderFinder(),
					dynamic -> fixDecorationPosition(dynamic, true, false)
				)
			)
		);

		TypeRewriteRule itemFrameRule = fixTypeEverywhereTyped(
			"EntityItemFrameFix",
			entityType,
			entity -> entity.updateTyped(
				itemFrameFinder,
				itemFrameType,
				itemFrame -> itemFrame.update(
					DSL.remainderFinder(),
					dynamic -> fixDecorationPosition(dynamic, false, true)
				)
			)
		);

		return TypeRewriteRule.seq(paintingRule, itemFrameRule);
	}

	private Dynamic<?> fixDecorationPosition(Dynamic<?> entity, boolean isPainting, boolean isItemFrame) {
		if ((isPainting == false && isItemFrame == false)
			|| entity.get("Facing").asNumber().result().isPresent()
		) {
			return entity;
		}

		int facingIndex;

		if (entity.get("Direction").asNumber().result().isPresent()) {
			facingIndex = entity.get("Direction").asByte((byte) 0) % DIRECTION_OFFSETS.length;
			int[] offset = DIRECTION_OFFSETS[facingIndex];

			entity = entity
				.set("TileX", entity.createInt(entity.get("TileX").asInt(0) + offset[0]))
				.set("TileY", entity.createInt(entity.get("TileY").asInt(0) + offset[1]))
				.set("TileZ", entity.createInt(entity.get("TileZ").asInt(0) + offset[2]))
				.remove("Direction");

			if (isItemFrame && entity.get("ItemRotation").asNumber().result().isPresent()) {
				entity = entity.set(
					"ItemRotation",
					entity.createByte((byte) (entity.get("ItemRotation").asByte((byte) 0) * 2))
				);
			}
		} else {
			facingIndex = entity.get("Dir").asByte((byte) 0) % DIRECTION_OFFSETS.length;
			entity = entity.remove("Dir");
		}

		return entity.set("Facing", entity.createByte((byte) facingIndex));
	}
}
