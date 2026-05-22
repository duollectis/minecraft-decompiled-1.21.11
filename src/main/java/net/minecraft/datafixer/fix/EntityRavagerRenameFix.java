package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;

import java.util.Map;
import java.util.Objects;

/**
 * Переименовывает сущность {@code minecraft:illager_beast} в {@code minecraft:ravager}
 * и спавн-яйцо {@code minecraft:illager_beast_spawn_egg} в {@code minecraft:ravager_spawn_egg}.
 */
public class EntityRavagerRenameFix extends EntityRenameFix {

	public static final Map<String, String> ITEMS = ImmutableMap.of(
			"minecraft:illager_beast_spawn_egg", "minecraft:ravager_spawn_egg"
	);

	public EntityRavagerRenameFix(Schema outputSchema, boolean changesType) {
		super("EntityRavagerRenameFix", outputSchema, changesType);
	}

	@Override
	protected String rename(String oldName) {
		return Objects.equals("minecraft:illager_beast", oldName) ? "minecraft:ravager" : oldName;
	}
}
