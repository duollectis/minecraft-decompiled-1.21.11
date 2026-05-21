package net.minecraft.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * {@code Leashable}.
 */
public interface Leashable {

	String LEASH_NBT_KEY = "leash";

	double DEFAULT_SNAPPING_DISTANCE = 12.0;

	double DEFAULT_ELASTIC_DISTANCE = 6.0;

	double MAX_LEASH_DISTANCE = 16.0;

	Vec3d ELASTICITY_MULTIPLIER = new Vec3d(0.8, 0.2, 0.8);

	float ELASTICITY_FACTOR = 0.7F;

	double ELASTIC_START_DISTANCE = 10.0;

	double ELASTIC_FORCE = 0.11;

	List<Vec3d> HELD_ENTITY_ATTACHMENT_POINT = ImmutableList.of(new Vec3d(0.0, 0.5, 0.5));

	List<Vec3d> LEASH_HOLDER_ATTACHMENT_POINT = ImmutableList.of(new Vec3d(0.0, 0.5, 0.0));

	List<Vec3d> QUAD_LEASH_ATTACHMENT_POINTS = ImmutableList.of(
			new Vec3d(-0.5, 0.5, 0.5), new Vec3d(-0.5, 0.5, -0.5), new Vec3d(0.5, 0.5, -0.5), new Vec3d(0.5, 0.5, 0.5)
	);

	Leashable.@Nullable LeashData getLeashData();

	void setLeashData(Leashable.@Nullable LeashData leashData);

	default boolean isLeashed() {
		return this.getLeashData() != null && this.getLeashData().leashHolder != null;
	}

	default boolean mightBeLeashed() {
		return this.getLeashData() != null;
	}

	default boolean canBeLeashedTo(Entity entity) {
		if (this == entity) {
			return false;
		}
		else {
			return this.getDistanceToCenter(entity) > this.getLeashSnappingDistance() ? false : this.canBeLeashed();
		}
	}

	default double getDistanceToCenter(Entity entity) {
		return entity.getBoundingBox().getCenter().distanceTo(((Entity) this).getBoundingBox().getCenter());
	}

	default boolean canBeLeashed() {
		return true;
	}

	default void setUnresolvedLeashHolderId(int unresolvedLeashHolderId) {
		this.setLeashData(new Leashable.LeashData(unresolvedLeashHolderId));
		detachLeash((Entity & Leashable) this, false, false);
	}

	default void readLeashData(ReadView view) {
		Leashable.LeashData leashData = view.<Leashable.LeashData>read("leash", Leashable.LeashData.CODEC).orElse(null);
		if (this.getLeashData() != null && leashData == null) {
			this.detachLeashWithoutDrop();
		}

		this.setLeashData(leashData);
	}

	default void writeLeashData(WriteView view, Leashable.@Nullable LeashData leashData) {
		view.putNullable("leash", Leashable.LeashData.CODEC, leashData);
	}

	private static <E extends Entity & Leashable> void resolveLeashData(E entity, Leashable.LeashData leashData) {
		if (leashData.unresolvedLeashData != null && entity.getEntityWorld() instanceof ServerWorld serverWorld) {
			Optional<UUID> optional = leashData.unresolvedLeashData.left();
			Optional<BlockPos> optional2 = leashData.unresolvedLeashData.right();
			if (optional.isPresent()) {
				Entity entity2 = serverWorld.getEntity(optional.get());
				if (entity2 != null) {
					attachLeash(entity, entity2, true);
					return;
				}
			}
			else if (optional2.isPresent()) {
				attachLeash(entity, LeashKnotEntity.getOrCreate(serverWorld, optional2.get()), true);
				return;
			}

			if (entity.age > 100) {
				entity.dropItem(serverWorld, Items.LEAD);
				entity.setLeashData(null);
			}
		}
	}

	default void detachLeash() {
		detachLeash((Entity & Leashable) this, true, true);
	}

	default void detachLeashWithoutDrop() {
		detachLeash((Entity & Leashable) this, true, false);
	}

	default void onLeashRemoved() {
	}

	private static <E extends Entity & Leashable> void detachLeash(E entity, boolean sendPacket, boolean dropItem) {
		Leashable.LeashData leashData = entity.getLeashData();
		if (leashData != null && leashData.leashHolder != null) {
			entity.setLeashData(null);
			entity.onLeashRemoved();
			if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
				if (dropItem) {
					entity.dropItem(serverWorld, Items.LEAD);
				}

				if (sendPacket) {
					serverWorld
							.getChunkManager()
							.sendToOtherNearbyPlayers(entity, new EntityAttachS2CPacket(entity, null));
				}

				leashData.leashHolder.onHeldLeashUpdate(entity);
			}
		}
	}

	static <E extends Entity & Leashable> void tickLeash(ServerWorld world, E entity) {
		Leashable.LeashData leashData = entity.getLeashData();
		if (leashData != null && leashData.unresolvedLeashData != null) {
			resolveLeashData(entity, leashData);
		}

		if (leashData != null && leashData.leashHolder != null) {
			if (!entity.isInteractable() || !leashData.leashHolder.isInteractable()) {
				if (world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
					entity.detachLeash();
				}
				else {
					entity.detachLeashWithoutDrop();
				}
			}

			Entity entity2 = entity.getLeashHolder();
			if (entity2 != null && entity2.getEntityWorld() == entity.getEntityWorld()) {
				double d = entity.getDistanceToCenter(entity2);
				entity.beforeLeashTick(entity2);
				if (d > entity.getLeashSnappingDistance()) {
					world.playSound(
							null,
							entity2.getX(),
							entity2.getY(),
							entity2.getZ(),
							SoundEvents.ITEM_LEAD_BREAK,
							SoundCategory.NEUTRAL,
							1.0F,
							1.0F
					);
					entity.snapLongLeash();
				}
				else if (d > entity.getElasticLeashDistance() - entity2.getWidth() - entity.getWidth()
						&& entity.applyElasticity(entity2, leashData)) {
					entity.onLongLeashTick();
				}
				else {
					entity.onShortLeashTick(entity2);
				}

				entity.setYaw((float) (entity.getYaw() - leashData.momentum));
				leashData.momentum = leashData.momentum * getSlipperiness(entity);
			}
		}
	}

	default void onLongLeashTick() {
		Entity entity = (Entity) this;
		entity.limitFallDistance();
	}

	default double getLeashSnappingDistance() {
		return 12.0;
	}

	default double getElasticLeashDistance() {
		return 6.0;
	}

	static <E extends Entity & Leashable> float getSlipperiness(E entity) {
		if (entity.isOnGround()) {
			return entity.getEntityWorld().getBlockState(entity.getVelocityAffectingPos()).getBlock().getSlipperiness()
					* 0.91F;
		}
		else {
			return entity.isInFluid() ? 0.8F : 0.91F;
		}
	}

	default void beforeLeashTick(Entity leashHolder) {
		leashHolder.tickHeldLeash(this);
	}

	default void snapLongLeash() {
		this.detachLeash();
	}

	default void onShortLeashTick(Entity entity) {
	}

	default boolean applyElasticity(Entity leashHolder, Leashable.LeashData leashData) {
		boolean bl = leashHolder.hasQuadLeashAttachmentPoints() && this.canUseQuadLeashAttachmentPoint();
		List<Leashable.Elasticity> list = calculateLeashElasticities(
				(Entity & Leashable) this,
				leashHolder,
				bl ? QUAD_LEASH_ATTACHMENT_POINTS : HELD_ENTITY_ATTACHMENT_POINT,
				bl ? QUAD_LEASH_ATTACHMENT_POINTS : LEASH_HOLDER_ATTACHMENT_POINT
		);
		if (list.isEmpty()) {
			return false;
		}
		else {
			Leashable.Elasticity elasticity = Leashable.Elasticity.sumOf(list).multiply(bl ? 0.25 : 1.0);
			leashData.momentum = leashData.momentum + 10.0 * elasticity.torque();
			Vec3d vec3d = getLeashHolderMovement(leashHolder).subtract(((Entity) this).getMovement());
			((Entity) this).addVelocityInternal(elasticity
					.force()
					.multiply(ELASTICITY_MULTIPLIER)
					.add(vec3d.multiply(0.11)));
			return true;
		}
	}

	private static Vec3d getLeashHolderMovement(Entity leashHolder) {
		return leashHolder instanceof MobEntity mobEntity && mobEntity.isAiDisabled() ? Vec3d.ZERO
		                                                                              : leashHolder.getMovement();
	}

	private static <E extends Entity & Leashable> List<Leashable.Elasticity> calculateLeashElasticities(
			E heldEntity,
			Entity leashHolder,
			List<Vec3d> heldEntityAttachmentPoints,
			List<Vec3d> leashHolderAttachmentPoints
	) {
		double d = heldEntity.getElasticLeashDistance();
		Vec3d vec3d = getLeashHolderMovement(heldEntity);
		float f = heldEntity.getYaw() * (float) (Math.PI / 180.0);
		Vec3d vec3d2 = new Vec3d(heldEntity.getWidth(), heldEntity.getHeight(), heldEntity.getWidth());
		float g = leashHolder.getYaw() * (float) (Math.PI / 180.0);
		Vec3d vec3d3 = new Vec3d(leashHolder.getWidth(), leashHolder.getHeight(), leashHolder.getWidth());
		List<Leashable.Elasticity> list = new ArrayList<>();

		for (int i = 0; i < heldEntityAttachmentPoints.size(); i++) {
			Vec3d vec3d4 = heldEntityAttachmentPoints.get(i).multiply(vec3d2).rotateY(-f);
			Vec3d vec3d5 = heldEntity.getEntityPos().add(vec3d4);
			Vec3d vec3d6 = leashHolderAttachmentPoints.get(i).multiply(vec3d3).rotateY(-g);
			Vec3d vec3d7 = leashHolder.getEntityPos().add(vec3d6);
			calculateLeashElasticity(vec3d7, vec3d5, d, vec3d, vec3d4).ifPresent(list::add);
		}

		return list;
	}

	private static Optional<Leashable.Elasticity> calculateLeashElasticity(
			Vec3d leashHolderAttachmentPos,
			Vec3d heldEntityAttachmentPos,
			double elasticDistance,
			Vec3d heldEntityMovement,
			Vec3d heldEntityAttachmentPoint
	) {
		double d = heldEntityAttachmentPos.distanceTo(leashHolderAttachmentPos);
		if (d < elasticDistance) {
			return Optional.empty();
		}
		else {
			Vec3d
					vec3d =
					leashHolderAttachmentPos
							.subtract(heldEntityAttachmentPos)
							.normalize()
							.multiply(d - elasticDistance);
			double e = Leashable.Elasticity.calculateTorque(heldEntityAttachmentPoint, vec3d);
			boolean bl = heldEntityMovement.dotProduct(vec3d) >= 0.0;
			if (bl) {
				vec3d = vec3d.multiply(0.3F);
			}

			return Optional.of(new Leashable.Elasticity(vec3d, e));
		}
	}

	default boolean canUseQuadLeashAttachmentPoint() {
		return false;
	}

	default Vec3d[] getQuadLeashOffsets() {
		return createQuadLeashOffsets((Entity) this, 0.0, 0.5, 0.5, 0.5);
	}

	static Vec3d[] createQuadLeashOffsets(
			Entity leashedEntity,
			double addedZOffset,
			double zOffset,
			double xOffset,
			double yOffset
	) {
		float f = leashedEntity.getWidth();
		double d = addedZOffset * f;
		double e = zOffset * f;
		double g = xOffset * f;
		double h = yOffset * leashedEntity.getHeight();
		return new Vec3d[]{
				new Vec3d(-g, h, e + d),
				new Vec3d(-g, h, -e + d),
				new Vec3d(g, h, -e + d),
				new Vec3d(g, h, e + d)
		};
	}

	default Vec3d getLeashOffset(float tickProgress) {
		return this.getLeashOffset();
	}

	default Vec3d getLeashOffset() {
		Entity entity = (Entity) this;
		return new Vec3d(0.0, entity.getStandingEyeHeight(), entity.getWidth() * 0.4F);
	}

	default void attachLeash(Entity leashHolder, boolean sendPacket) {
		if (this != leashHolder) {
			attachLeash((Entity & Leashable) this, leashHolder, sendPacket);
		}
	}

	private static <E extends Entity & Leashable> void attachLeash(E entity, Entity leashHolder, boolean sendPacket) {
		Leashable.LeashData leashData = entity.getLeashData();
		if (leashData == null) {
			leashData = new Leashable.LeashData(leashHolder);
			entity.setLeashData(leashData);
		}
		else {
			Entity entity2 = leashData.leashHolder;
			leashData.setLeashHolder(leashHolder);
			if (entity2 != null && entity2 != leashHolder) {
				entity2.onHeldLeashUpdate(entity);
			}
		}

		if (sendPacket && entity.getEntityWorld() instanceof ServerWorld serverWorld) {
			serverWorld
					.getChunkManager()
					.sendToOtherNearbyPlayers(entity, new EntityAttachS2CPacket(entity, leashHolder));
		}

		if (entity.hasVehicle()) {
			entity.stopRiding();
		}
	}

	default @Nullable Entity getLeashHolder() {
		return getLeashHolder((Entity & Leashable) this);
	}

	private static <E extends Entity & Leashable> @Nullable Entity getLeashHolder(E entity) {
		Leashable.LeashData leashData = entity.getLeashData();
		if (leashData == null) {
			return null;
		}
		else {
			if (leashData.unresolvedLeashHolderId != 0 && entity.getEntityWorld().isClient()) {
				Entity var3 = entity.getEntityWorld().getEntityById(leashData.unresolvedLeashHolderId);
				if (var3 instanceof Entity) {
					leashData.setLeashHolder(var3);
				}
			}

			return leashData.leashHolder;
		}
	}

	static List<Leashable> collectLeashablesHeldBy(Entity leashHolder) {
		return collectLeashablesAround(leashHolder, leashable -> leashable.getLeashHolder() == leashHolder);
	}

	static List<Leashable> collectLeashablesAround(Entity entity, Predicate<Leashable> leashablePredicate) {
		return collectLeashablesAround(
				entity.getEntityWorld(),
				entity.getBoundingBox().getCenter(),
				leashablePredicate
		);
	}

	static List<Leashable> collectLeashablesAround(World world, Vec3d pos, Predicate<Leashable> leashablePredicate) {
		double d = 32.0;
		Box box = Box.of(pos, 32.0, 32.0, 32.0);
		return world
				.getEntitiesByClass(
						Entity.class,
						box,
						entity -> entity instanceof Leashable leashable && leashablePredicate.test(leashable)
				)
				.stream()
				.map(Leashable.class::cast)
				.toList();
	}

	/**
	 * {@code Elasticity}.
	 */
	public record Elasticity(Vec3d force, double torque) {

		static Leashable.Elasticity ZERO = new Leashable.Elasticity(Vec3d.ZERO, 0.0);

		static double calculateTorque(Vec3d force, Vec3d force2) {
			return force.z * force2.x - force.x * force2.z;
		}

		static Leashable.Elasticity sumOf(List<Leashable.Elasticity> elasticities) {
			if (elasticities.isEmpty()) {
				return ZERO;
			}
			else {
				double d = 0.0;
				double e = 0.0;
				double f = 0.0;
				double g = 0.0;

				for (Leashable.Elasticity elasticity : elasticities) {
					Vec3d vec3d = elasticity.force;
					d += vec3d.x;
					e += vec3d.y;
					f += vec3d.z;
					g += elasticity.torque;
				}

				return new Leashable.Elasticity(new Vec3d(d, e, f), g);
			}
		}

		public Leashable.Elasticity multiply(double value) {
			return new Leashable.Elasticity(this.force.multiply(value), this.torque * value);
		}
	}

	/**
	 * {@code LeashData}.
	 */
	public static final class LeashData {

		public static final Codec<Leashable.LeashData>
				CODEC =
				Codec.xor(Uuids.INT_STREAM_CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
				     .xmap(
						     Leashable.LeashData::new,
						     data -> {
							     if (data.leashHolder instanceof LeashKnotEntity leashKnotEntity) {
								     return Either.right(leashKnotEntity.getAttachedBlockPos());
							     }
							     else {
								     return data.leashHolder != null
								            ? Either.left(data.leashHolder.getUuid())
								            : Objects.requireNonNull(
										            data.unresolvedLeashData,
										            "Invalid LeashData had no attachment"
								            );
							     }
						     }
				     );
		int unresolvedLeashHolderId;
		public @Nullable Entity leashHolder;
		public @Nullable Either<UUID, BlockPos> unresolvedLeashData;
		public double momentum;

		private LeashData(Either<UUID, BlockPos> unresolvedLeashData) {
			this.unresolvedLeashData = unresolvedLeashData;
		}

		LeashData(Entity leashHolder) {
			this.leashHolder = leashHolder;
		}

		LeashData(int unresolvedLeashHolderId) {
			this.unresolvedLeashHolderId = unresolvedLeashHolderId;
		}

		public void setLeashHolder(Entity leashHolder) {
			this.leashHolder = leashHolder;
			this.unresolvedLeashData = null;
			this.unresolvedLeashHolderId = 0;
		}
	}
}
