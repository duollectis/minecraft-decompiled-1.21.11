package net.minecraft.predicate.entity;

import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.scoreboard.AbstractTeam;

import java.util.function.Predicate;

/**
 * Набор стандартных предикатов для фильтрации сущностей.
 * Используется в поиске сущностей, коллизиях и прочих системах.
 */
public final class EntityPredicates {

	public static final Predicate<Entity> VALID_ENTITY = Entity::isAlive;

	public static final Predicate<Entity> VALID_LIVING_ENTITY =
			entity -> entity.isAlive() && entity instanceof LivingEntity;

	public static final Predicate<Entity> NOT_MOUNTED =
			entity -> entity.isAlive() && !entity.hasPassengers() && !entity.hasVehicle();

	public static final Predicate<Entity> VALID_INVENTORIES =
			entity -> entity instanceof Inventory && entity.isAlive();

	public static final Predicate<Entity> EXCEPT_CREATIVE_OR_SPECTATOR =
			entity -> !(entity instanceof PlayerEntity playerEntity
					&& (entity.isSpectator() || playerEntity.isCreative()));

	public static final Predicate<Entity> EXCEPT_SPECTATOR = entity -> !entity.isSpectator();

	public static final Predicate<Entity> CAN_COLLIDE = EXCEPT_SPECTATOR.and(entity -> entity.isCollidable(null));

	public static final Predicate<Entity> CAN_HIT = EXCEPT_SPECTATOR.and(Entity::canHit);

	private EntityPredicates() {
	}

	public static Predicate<Entity> maxDistance(double x, double y, double z, double max) {
		double maxSquared = max * max;
		return entity -> entity.squaredDistanceTo(x, y, z) <= maxSquared;
	}

	/**
	 * Создаёт предикат, проверяющий, может ли данная сущность быть вытолкнута указанной.
	 * Учитывает правила коллизий команд обеих сущностей.
	 */
	public static Predicate<Entity> canBePushedBy(Entity pusher) {
		AbstractTeam pusherTeam = pusher.getScoreboardTeam();
		AbstractTeam.CollisionRule pusherRule =
				pusherTeam == null ? AbstractTeam.CollisionRule.ALWAYS : pusherTeam.getCollisionRule();

		if (pusherRule == AbstractTeam.CollisionRule.NEVER) {
			return Predicates.alwaysFalse();
		}

		return EXCEPT_SPECTATOR.and(candidate -> {
			if (!candidate.isPushable()) {
				return false;
			}

			if (pusher.getEntityWorld().isClient()
					&& !(candidate instanceof PlayerEntity playerEntity && playerEntity.isMainPlayer())
			) {
				return false;
			}

			AbstractTeam candidateTeam = candidate.getScoreboardTeam();
			AbstractTeam.CollisionRule candidateRule =
					candidateTeam == null ? AbstractTeam.CollisionRule.ALWAYS : candidateTeam.getCollisionRule();

			if (candidateRule == AbstractTeam.CollisionRule.NEVER) {
				return false;
			}

			boolean sameTeam = pusherTeam != null && pusherTeam.isEqual(candidateTeam);

			if ((pusherRule == AbstractTeam.CollisionRule.PUSH_OWN_TEAM
					|| candidateRule == AbstractTeam.CollisionRule.PUSH_OWN_TEAM)
					&& sameTeam
			) {
				return false;
			}

			return pusherRule != AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS
					&& candidateRule != AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS
					|| sameTeam;
		});
	}

	public static Predicate<Entity> rides(Entity entity) {
		return testedEntity -> {
			while (testedEntity.hasVehicle()) {
				testedEntity = testedEntity.getVehicle();

				if (testedEntity == entity) {
					return false;
				}
			}

			return true;
		};
	}
}
