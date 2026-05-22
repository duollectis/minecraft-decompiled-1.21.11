package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;

import java.util.Map;
import java.util.Objects;

/**
 * Переименовывает сущность {@code minecraft:zombie_pigman} в {@code minecraft:zombified_piglin}
 * и яйцо призыва {@code minecraft:zombie_pigman_spawn_egg} в {@code minecraft:zombified_piglin_spawn_egg}.
 */
public class EntityZombifiedPiglinRenameFix extends EntityRenameFix {

	public static final Map<String, String> RENAMES = ImmutableMap.of(
		"minecraft:zombie_pigman_spawn_egg", "minecraft:zombified_piglin_spawn_egg"
	);

	public EntityZombifiedPiglinRenameFix(Schema outputSchema) {
		super("EntityZombifiedPiglinRenameFix", outputSchema, true);
	}

	@Override
	protected String rename(String oldName) {
		return Objects.equals("minecraft:zombie_pigman", oldName) ? "minecraft:zombified_piglin" : oldName;
	}
}
