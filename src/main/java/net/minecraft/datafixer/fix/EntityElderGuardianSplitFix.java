package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

import java.util.Objects;

/**
 * Разделяет старый тип сущности {@code Guardian} на {@code Guardian} и {@code ElderGuardian}.
 * До версии 1.8 старший страж хранился как {@code Guardian} с флагом {@code Elder = true}.
 */
public class EntityElderGuardianSplitFix extends EntitySimpleTransformFix {

	public EntityElderGuardianSplitFix(Schema outputSchema, boolean changesType) {
		super("EntityElderGuardianSplitFix", outputSchema, changesType);
	}

	@Override
	protected Pair<String, Dynamic<?>> transform(String choice, Dynamic<?> entity) {
		boolean isElderGuardian = Objects.equals(choice, "Guardian") && entity.get("Elder").asBoolean(false);

		return Pair.of(
				isElderGuardian ? "ElderGuardian" : choice,
				entity
		);
	}
}
