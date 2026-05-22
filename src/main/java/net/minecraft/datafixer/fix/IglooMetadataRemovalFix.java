package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет устаревшие метаданные иглу из структурных объектов:
 * если все дочерние элементы являются иглу, заменяет их на единый объект
 * с {@code id = "Igloo"}, иначе фильтрует иглу из списка дочерних элементов.
 */
public class IglooMetadataRemovalFix extends DataFix {

	public IglooMetadataRemovalFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> structureFeatureType = getInputSchema().getType(TypeReferences.STRUCTURE_FEATURE);

		return fixTypeEverywhereTyped(
			"IglooMetadataRemovalFix",
			structureFeatureType,
			structureFeature -> structureFeature.update(
				DSL.remainderFinder(),
				IglooMetadataRemovalFix::removeMetadata
			)
		);
	}

	private static <T> Dynamic<T> removeMetadata(Dynamic<T> structureFeature) {
		boolean allChildrenAreIgloos = structureFeature.get("Children")
			.asStreamOpt()
			.map(stream -> stream.allMatch(IglooMetadataRemovalFix::isIgloo))
			.result()
			.orElse(false);

		return allChildrenAreIgloos
			? structureFeature.set("id", structureFeature.createString("Igloo")).remove("Children")
			: structureFeature.update("Children", IglooMetadataRemovalFix::removeIgloos);
	}

	private static <T> Dynamic<T> removeIgloos(Dynamic<T> children) {
		return children.asStreamOpt()
			.map(stream -> stream.filter(child -> !isIgloo((Dynamic<?>) child)))
			.map(children::createList)
			.result()
			.orElse(children);
	}

	private static boolean isIgloo(Dynamic<?> child) {
		return child.get("id").asString("").equals("Iglu");
	}
}
