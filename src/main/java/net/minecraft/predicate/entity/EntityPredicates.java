package net.minecraft.predicate.entity;

import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.scoreboard.AbstractTeam;

import java.util.function.Predicate;

/**
 * {@code EntityPredicates}.
 */
public final class EntityPredicates {

	public static final Predicate<Entity> VALID_ENTITY = Entity::isAlive;
	public static final Predicate<Entity>
			VALID_LIVING_ENTITY =
			entity -> entity.isAlive() && entity instanceof LivingEntity;
	public static final Predicate<Entity>
			NOT_MOUNTED =
			entity -> entity.isAlive() && !entity.hasPassengers() && !entity.hasVehicle();
	public static final Predicate<Entity> VALID_INVENTORIES = entity -> entity instanceof Inventory && entity.isAlive();
	public static final Predicate<Entity> EXCEPT_CREATIVE_OR_SPECTATOR = entity -> !(
			entity instanceof PlayerEntity playerEntity && (entity.isSpectator() || playerEntity.isCreative())
	);
	public static final Predicate<Entity> EXCEPT_SPECTATOR = entity -> !entity.isSpectator();
	public static final Predicate<Entity> CAN_COLLIDE = EXCEPT_SPECTATOR.and(entity -> entity.isCollidable(null));
	public static final Predicate<Entity> CAN_HIT = EXCEPT_SPECTATOR.and(Entity::canHit);

	private EntityPredicates() {
	}

	public static Predicate<Entity> maxDistance(double x, double y, double z, double max) {
		double d = max * max;
		return entity -> entity.squaredDistanceTo(x, y, z) <= d;
	}

	public static Predicate<Entity> canBePushedBy(Entity entity) {
		AbstractTeam abstractTeam = entity.getScoreboardTeam();
		AbstractTeam.CollisionRule
				collisionRule =
				abstractTeam == null ? AbstractTeam.CollisionRule.ALWAYS : abstractTeam.getCollisionRule();
		return (Predicate<Entity>) (collisionRule == AbstractTeam.CollisionRule.NEVER
		                            ? Predicates.alwaysFalse()
		                            : EXCEPT_SPECTATOR.and(
				                            entityxx -> {
					                            if (!entityxx.isPushable()) {
						                            return false;
					                            }
					                            else if (!entity.getEntityWorld().isClient()
					                                     || entityxx instanceof PlayerEntity playerEntity
					                                        && playerEntity.isMainPlayer()) {
						                            AbstractTeam abstractTeam2 = entityxx.getScoreboardTeam();
						                            AbstractTeam.CollisionRule
						                            collisionRule2 =
						                            abstractTeam2 == null ? AbstractTeam.CollisionRule.ALWAYS
						                                                  : abstractTeam2.getCollisionRule();
						                            if (collisionRule2 == AbstractTeam.CollisionRule.NEVER) {
							                            return false;
						                            }
						                            else {
							                            boolean
							                            bl =
							                            abstractTeam != null && abstractTeam.isEqual(abstractTeam2);
							                            return
							                            (collisionRule == AbstractTeam.CollisionRule.PUSH_OWN_TEAM
									                            || collisionRule2
									                            == AbstractTeam.CollisionRule.PUSH_OWN_TEAM
							                            ) && bl
							                            ? false
							                            : collisionRule != AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS
							                              && collisionRule2
							                                 != AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS || bl;
						                            }
					                            }
					                            else {
						                            return false;
					                            }
				                            }
		                            )
		);
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
