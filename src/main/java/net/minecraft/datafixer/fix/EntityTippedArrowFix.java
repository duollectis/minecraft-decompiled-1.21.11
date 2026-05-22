package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;

import java.util.Objects;

/**
 * Переименовывает устаревший тип сущности {@code TippedArrow} в {@code Arrow},
 * так как в новых версиях зелейные стрелы объединены с обычными.
 */
public class EntityTippedArrowFix extends EntityRenameFix {

	public EntityTippedArrowFix(Schema schema, boolean changesType) {
		super("EntityTippedArrowFix", schema, changesType);
	}

	@Override
	protected String rename(String oldName) {
		return Objects.equals(oldName, "TippedArrow") ? "Arrow" : oldName;
	}
}
