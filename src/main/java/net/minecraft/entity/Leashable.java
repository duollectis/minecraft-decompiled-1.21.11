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
 * Интерфейс для сущностей, которых можно привязать поводком.
 * Управляет состоянием привязки, физикой упругости и синхронизацией с клиентом.
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
		return getLeashData() != null && getLeashData().leashHolder != null;
	}

	default boolean mightBeLeashed() {
		return getLeashData() != null;
	}

	default boolean canBeLeashedTo(Entity entity) {
		if (this == entity) {
			return false;
		}

		return getDistanceToCenter(entity) <= getLeashSnappingDistance() && canBeLeashed();
	}

	default double getDistanceToCenter(Entity entity) {
		return entity.getBoundingBox().getCenter().distanceTo(((Entity) this).getBoundingBox().getCenter());
	}

	default boolean canBeLeashed() {
		return true;
	}

	default void setUnresolvedLeashHolderId(int unresolvedLeashHolderId) {
		setLeashData(new Leashable.LeashData(unresolvedLeashHolderId));
		detachLeash((Entity & Leashable) this, false, false);
	}

	default void readLeashData(ReadView view) {
		Leashable.LeashData leashData = view.<Leashable.LeashData>read("leash", Leashable.LeashData.CODEC).orElse(null);

		if (getLeashData() != null && leashData == null) {
			detachLeashWithoutDrop();
		}

		setLeashData(leashData);
	}

	default void writeLeashData(WriteView view, Leashable.@Nullable LeashData leashData) {
		view.putNullable("leash", Leashable.LeashData.CODEC, leashData);
	}

	private static <E extends Entity & Leashable> void resolveLeashData(E entity, Leashable.LeashData leashData) {
		if (leashData.unresolvedLeashData == null || !(entity.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Optional<UUID> holderUuid = leashData.unresolvedLeashData.left();
		Optional<BlockPos> knotPos = leashData.unresolvedLeashData.right();

		if (holderUuid.isPresent()) {
			Entity holder = serverWorld.getEntity(holderUuid.get());

			if (holder != null) {
				attachLeash(entity, holder, true);
				return;
			}
		}
		else if (knotPos.isPresent()) {
			attachLeash(entity, LeashKnotEntity.getOrCreate(serverWorld, knotPos.get()), true);
			return;
		}

		if (entity.age > 100) {
			entity.dropItem(serverWorld, Items.LEAD);
			entity.setLeashData(null);
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

		if (leashData == null || leashData.leashHolder == null) {
			return;
		}

		entity.setLeashData(null);
		entity.onLeashRemoved();

		if (!(entity.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

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

	/**
	 * Обновляет состояние поводка каждый тик на сервере.
	 * Разрешает неразрешённые данные привязки, проверяет допустимость расстояния
	 * и применяет физику упругости при натяжении.
	 */
	static <E extends Entity & Leashable> void tickLeash(ServerWorld world, E entity) {
		Leashable.LeashData leashData = entity.getLeashData();

		if (leashData != null && leashData.unresolvedLeashData != null) {
			resolveLeashData(entity, leashData);
		}

		if (leashData == null || leashData.leashHolder == null) {
			return;
		}

		if (!entity.isInteractable() || !leashData.leashHolder.isInteractable()) {
			if (world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
				entity.detachLeash();
			}
			else {
				entity.detachLeashWithoutDrop();
			}
		}

		Entity holder = entity.getLeashHolder();

		if (holder != null && holder.getEntityWorld() == entity.getEntityWorld()) {
			double distance = entity.getDistanceToCenter(holder);
			entity.beforeLeashTick(holder);

			if (distance > entity.getLeashSnappingDistance()) {
				world.playSound(
						null,
						holder.getX(),
						holder.getY(),
						holder.getZ(),
						SoundEvents.ITEM_LEAD_BREAK,
						SoundCategory.NEUTRAL,
						1.0F,
						1.0F
				);
				entity.snapLongLeash();
			}
			else if (distance > entity.getElasticLeashDistance() - holder.getWidth() - entity.getWidth()
					&& entity.applyElasticity(holder, leashData)) {
				entity.onLongLeashTick();
			}
			else {
				entity.onShortLeashTick(holder);
			}

			entity.setYaw((float) (entity.getYaw() - leashData.momentum));
			leashData.momentum = leashData.momentum * getSlipperiness(entity);
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

		return entity.isInFluid() ? 0.8F : 0.91F;
	}

	default void beforeLeashTick(Entity leashHolder) {
		leashHolder.tickHeldLeash(this);
	}

	default void snapLongLeash() {
		detachLeash();
	}

	default void onShortLeashTick(Entity entity) {
	}

	default boolean applyElasticity(Entity leashHolder, Leashable.LeashData leashData) {
		boolean useQuad = leashHolder.hasQuadLeashAttachmentPoints() && canUseQuadLeashAttachmentPoint();
		List<Leashable.Elasticity> elasticities = calculateLeashElasticities(
				(Entity & Leashable) this,
				leashHolder,
				useQuad ? QUAD_LEASH_ATTACHMENT_POINTS : HELD_ENTITY_ATTACHMENT_POINT,
				useQuad ? QUAD_LEASH_ATTACHMENT_POINTS : LEASH_HOLDER_ATTACHMENT_POINT
		);

		if (elasticities.isEmpty()) {
			return false;
		}

		Leashable.Elasticity totalElasticity = Leashable.Elasticity.sumOf(elasticities).multiply(useQuad ? 0.25 : 1.0);
		leashData.momentum = leashData.momentum + 10.0 * totalElasticity.torque();
		Vec3d holderMovementDelta = getLeashHolderMovement(leashHolder).subtract(((Entity) this).getMovement());
		((Entity) this).addVelocityInternal(totalElasticity
				.force()
				.multiply(ELASTICITY_MULTIPLIER)
				.add(holderMovementDelta.multiply(ELASTIC_FORCE)));

		return true;
	}

	private static Vec3d getLeashHolderMovement(Entity leashHolder) {
		return leashHolder instanceof MobEntity mobEntity && mobEntity.isAiDisabled()
				? Vec3d.ZERO
				: leashHolder.getMovement();
	}

	private static <E extends Entity & Leashable> List<Leashable.Elasticity> calculateLeashElasticities(
			E heldEntity,
			Entity leashHolder,
			List<Vec3d> heldEntityAttachmentPoints,
			List<Vec3d> leashHolderAttachmentPoints
	) {
		double elasticDistance = heldEntity.getElasticLeashDistance();
		Vec3d heldEntityMovement = getLeashHolderMovement(heldEntity);
		float heldYawRad = heldEntity.getYaw() * (float) (Math.PI / 180.0);
		Vec3d heldEntitySize = new Vec3d(heldEntity.getWidth(), heldEntity.getHeight(), heldEntity.getWidth());
		float holderYawRad = leashHolder.getYaw() * (float) (Math.PI / 180.0);
		Vec3d holderSize = new Vec3d(leashHolder.getWidth(), leashHolder.getHeight(), leashHolder.getWidth());
		List<Leashable.Elasticity> result = new ArrayList<>();

		for (int index = 0; index < heldEntityAttachmentPoints.size(); index++) {
			Vec3d heldAttachOffset = heldEntityAttachmentPoints.get(index).multiply(heldEntitySize).rotateY(-heldYawRad);
			Vec3d heldAttachPos = heldEntity.getEntityPos().add(heldAttachOffset);
			Vec3d holderAttachOffset = leashHolderAttachmentPoints.get(index).multiply(holderSize).rotateY(-holderYawRad);
			Vec3d holderAttachPos = leashHolder.getEntityPos().add(holderAttachOffset);
			calculateLeashElasticity(holderAttachPos, heldAttachPos, elasticDistance, heldEntityMovement, heldAttachOffset)
					.ifPresent(result::add);
		}

		return result;
	}

	private static Optional<Leashable.Elasticity> calculateLeashElasticity(
			Vec3d leashHolderAttachmentPos,
			Vec3d heldEntityAttachmentPos,
			double elasticDistance,
			Vec3d heldEntityMovement,
			Vec3d heldEntityAttachmentPoint
	) {
		double distance = heldEntityAttachmentPos.distanceTo(leashHolderAttachmentPos);

		if (distance < elasticDistance) {
			return Optional.empty();
		}

		Vec3d force = leashHolderAttachmentPos
				.subtract(heldEntityAttachmentPos)
				.normalize()
				.multiply(distance - elasticDistance);
		double torque = Leashable.Elasticity.calculateTorque(heldEntityAttachmentPoint, force);
		boolean movingTowardsHolder = heldEntityMovement.dotProduct(force) >= 0.0;

		if (movingTowardsHolder) {
			force = force.multiply(0.3F);
		}

		return Optional.of(new Leashable.Elasticity(force, torque));
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
		float width = leashedEntity.getWidth();
		double scaledAddedZ = addedZOffset * width;
		double scaledZ = zOffset * width;
		double scaledX = xOffset * width;
		double scaledY = yOffset * leashedEntity.getHeight();

		return new Vec3d[]{
				new Vec3d(-scaledX, scaledY, scaledZ + scaledAddedZ),
				new Vec3d(-scaledX, scaledY, -scaledZ + scaledAddedZ),
				new Vec3d(scaledX, scaledY, -scaledZ + scaledAddedZ),
				new Vec3d(scaledX, scaledY, scaledZ + scaledAddedZ)
		};
	}

	default Vec3d getLeashOffset(float tickProgress) {
		return getLeashOffset();
	}

	default Vec3d getLeashOffset() {
		Entity entity = (Entity) this;
		return new Vec3d(0.0, entity.getStandingEyeHeight(), entity.getWidth() * 0.4F);
	}

	default void attachLeash(Entity leashHolder, boolean sendPacket) {
		if (this == leashHolder) {
			return;
		}

		attachLeash((Entity & Leashable) this, leashHolder, sendPacket);
	}

	private static <E extends Entity & Leashable> void attachLeash(E entity, Entity leashHolder, boolean sendPacket) {
		Leashable.LeashData leashData = entity.getLeashData();

		if (leashData == null) {
			leashData = new Leashable.LeashData(leashHolder);
			entity.setLeashData(leashData);
		}
		else {
			Entity previousHolder = leashData.leashHolder;
			leashData.setLeashHolder(leashHolder);

			if (previousHolder != null && previousHolder != leashHolder) {
				previousHolder.onHeldLeashUpdate(entity);
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

		if (leashData.unresolvedLeashHolderId != 0 && entity.getEntityWorld().isClient()) {
			Entity resolved = entity.getEntityWorld().getEntityById(leashData.unresolvedLeashHolderId);

			if (resolved != null) {
				leashData.setLeashHolder(resolved);
			}
		}

		return leashData.leashHolder;
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
		Box searchBox = Box.of(pos, MAX_LEASH_DISTANCE * 2, MAX_LEASH_DISTANCE * 2, MAX_LEASH_DISTANCE * 2);

		return world
				.getEntitiesByClass(
						Entity.class,
						searchBox,
						entity -> entity instanceof Leashable leashable && leashablePredicate.test(leashable)
				)
				.stream()
				.map(Leashable.class::cast)
				.toList();
	}

	/**
	 * Физическая сила упругости поводка: вектор силы и момент вращения (torque).
	 */
	public record Elasticity(Vec3d force, double torque) {

		static final Leashable.Elasticity ZERO = new Leashable.Elasticity(Vec3d.ZERO, 0.0);

		static double calculateTorque(Vec3d attachmentPoint, Vec3d force) {
			return attachmentPoint.z * force.x - attachmentPoint.x * force.z;
		}

		static Leashable.Elasticity sumOf(List<Leashable.Elasticity> elasticities) {
			if (elasticities.isEmpty()) {
				return ZERO;
			}

			double sumX = 0.0;
			double sumY = 0.0;
			double sumZ = 0.0;
			double sumTorque = 0.0;

			for (Leashable.Elasticity elasticity : elasticities) {
				Vec3d force = elasticity.force;
				sumX += force.x;
				sumY += force.y;
				sumZ += force.z;
				sumTorque += elasticity.torque;
			}

			return new Leashable.Elasticity(new Vec3d(sumX, sumY, sumZ), sumTorque);
		}

		public Leashable.Elasticity multiply(double value) {
			return new Leashable.Elasticity(force.multiply(value), torque * value);
		}
	}

	/**
	 * Данные о текущей привязке сущности поводком.
	 * Может содержать либо разрешённого держателя ({@link #leashHolder}),
	 * либо неразрешённые данные для восстановления после загрузки чанка.
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

							     return data.leashHolder != null
							            ? Either.left(data.leashHolder.getUuid())
							            : Objects.requireNonNull(
									            data.unresolvedLeashData,
									            "Invalid LeashData had no attachment"
							            );
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
			unresolvedLeashData = null;
			unresolvedLeashHolderId = 0;
		}
	}
}
