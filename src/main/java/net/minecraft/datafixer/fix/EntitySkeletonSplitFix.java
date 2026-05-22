package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

import java.util.Objects;

/**
 * Разделяет единый тип {@code Skeleton} на три отдельных сущности
 * в зависимости от числового поля {@code SkeletonType}:
 * {@code 1} → {@code WitherSkeleton}, {@code 2} → {@code Stray},
 * остальные значения остаются {@code Skeleton}.
 */
public class EntitySkeletonSplitFix extends EntitySimpleTransformFix {

	private static final int WITHER_SKELETON_TYPE = 1;
	private static final int STRAY_TYPE = 2;

	public EntitySkeletonSplitFix(Schema schema, boolean changesType) {
		super("EntitySkeletonSplitFix", schema, changesType);
	}

	@Override
	protected Pair<String, Dynamic<?>> transform(String choice, Dynamic<?> entity) {
		if (Objects.equals(choice, "Skeleton")) {
			int skeletonType = entity.get("SkeletonType").asInt(0);
			choice = switch (skeletonType) {
				case WITHER_SKELETON_TYPE -> "WitherSkeleton";
				case STRAY_TYPE -> "Stray";
				default -> choice;
			};
		}

		return Pair.of(choice, entity);
	}
}
