package net.minecraft.server.command;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

@FunctionalInterface
/**
 * Вспомогательный тип для задания цели взгляда в команде {@code /rotate}.
 */
public interface LookTarget {

	void look(ServerCommandSource source, Entity entity);

	public record LookAtEntity(Entity entity, EntityAnchorArgumentType.EntityAnchor anchor) implements LookTarget {

		@Override
		public void look(ServerCommandSource source, Entity entity) {
			if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
				serverPlayerEntity.lookAtEntity(source.getEntityAnchor(), this.entity, this.anchor);
			}
			else {
				entity.lookAt(source.getEntityAnchor(), this.anchor.positionAt(this.entity));
			}
		}
	}

	public record LookAtPosition(Vec3d position) implements LookTarget {

		@Override
		public void look(ServerCommandSource source, Entity entity) {
			entity.lookAt(source.getEntityAnchor(), this.position);
		}
	}
}
