package net.minecraft.entity;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code ExperienceOrbEntity}.
 */
public class ExperienceOrbEntity extends Entity {

	protected static final TrackedData<Integer>
			VALUE =
			DataTracker.registerData(ExperienceOrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int DESPAWN_AGE = 6000;
	private static final int EXPENSIVE_UPDATE_INTERVAL = 20;
	private static final int PLAYER_TRACKING_RANGE = 8;
	private static final int MERGING_CHANCE_FRACTION = 40;
	private static final double MERGE_SEARCH_RADIUS = 0.5;
	private static final short DEFAULT_HEALTH = 5;
	private static final short DEFAULT_AGE = 0;
	private static final short DEFAULT_VALUE = 0;
	private static final int DEFAULT_COUNT = 1;
	private int orbAge = 0;
	private int health = 5;
	private int pickingCount = 1;
	private @Nullable PlayerEntity target;
	private final PositionInterpolator interpolator = new PositionInterpolator(this);

	public ExperienceOrbEntity(World world, double x, double y, double z, int amount) {
		this(world, new Vec3d(x, y, z), Vec3d.ZERO, amount);
	}

	public ExperienceOrbEntity(World world, Vec3d pos, Vec3d velocity, int amount) {
		this(EntityType.EXPERIENCE_ORB, world);
		this.setPosition(pos);
		if (!world.isClient()) {
			this.setYaw(this.random.nextFloat() * 360.0F);
			Vec3d vec3d = new Vec3d(
					(this.random.nextDouble() * 0.2 - 0.1) * 2.0,
					this.random.nextDouble() * 0.2 * 2.0,
					(this.random.nextDouble() * 0.2 - 0.1) * 2.0
			);
			if (velocity.lengthSquared() > 0.0 && velocity.dotProduct(vec3d) < 0.0) {
				vec3d = vec3d.multiply(-1.0);
			}

			double d = this.getBoundingBox().getAverageSideLength();
			this.setPosition(pos.add(velocity.normalize().multiply(d * 0.5)));
			this.setVelocity(vec3d);
			if (!world.isSpaceEmpty(this.getBoundingBox())) {
				this.tryMoveToOpenSpace(d);
			}
		}

		this.setValue(amount);
	}

	public ExperienceOrbEntity(EntityType<? extends ExperienceOrbEntity> entityType, World world) {
		super(entityType, world);
	}

	protected void tryMoveToOpenSpace(double boundingBoxLength) {
		Vec3d vec3d = this.getEntityPos().add(0.0, this.getHeight() / 2.0, 0.0);
		VoxelShape
				voxelShape =
				VoxelShapes.cuboid(Box.of(vec3d, boundingBoxLength, boundingBoxLength, boundingBoxLength));
		this.getEntityWorld()
		    .findClosestCollision(this, voxelShape, vec3d, this.getWidth(), this.getHeight(), this.getWidth())
		    .ifPresent(pos -> this.setPosition(pos.add(0.0, -this.getHeight() / 2.0, 0.0)));
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(VALUE, 0);
	}

	@Override
	protected double getGravity() {
		return 0.03;
	}

	@Override
	public void tick() {
		this.interpolator.tick();
		if (this.firstUpdate && this.getEntityWorld().isClient()) {
			this.firstUpdate = false;
		}
		else {
			super.tick();
			boolean bl = !this.getEntityWorld().isSpaceEmpty(this.getBoundingBox());
			if (this.isSubmergedIn(FluidTags.WATER)) {
				this.applyWaterMovement();
			}
			else if (!bl) {
				this.applyGravity();
			}

			if (this.getEntityWorld().getFluidState(this.getBlockPos()).isIn(FluidTags.LAVA)) {
				this.setVelocity(
						(this.random.nextFloat() - this.random.nextFloat()) * 0.2F,
						0.2F,
						(this.random.nextFloat() - this.random.nextFloat()) * 0.2F
				);
			}

			if (this.age % 20 == 1) {
				this.expensiveUpdate();
			}

			this.moveTowardsPlayer();
			if (this.target == null && !this.getEntityWorld().isClient() && bl) {
				boolean bl2 = !this.getEntityWorld().isSpaceEmpty(this.getBoundingBox().offset(this.getVelocity()));
				if (bl2) {
					this.pushOutOfBlocks(
							this.getX(),
							(this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0,
							this.getZ()
					);
					this.velocityDirty = true;
				}
			}

			double d = this.getVelocity().y;
			this.move(MovementType.SELF, this.getVelocity());
			this.tickBlockCollision();
			float f = 0.98F;
			if (this.isOnGround()) {
				f =
						this.getEntityWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getSlipperiness()
								* 0.98F;
			}

			this.setVelocity(this.getVelocity().multiply(f));
			if (this.groundCollision && d < -this.getFinalGravity()) {
				this.setVelocity(new Vec3d(this.getVelocity().x, -d * 0.4, this.getVelocity().z));
			}

			this.orbAge++;
			if (this.orbAge >= 6000) {
				this.discard();
			}
		}
	}

	private void moveTowardsPlayer() {
		if (this.target == null || this.target.isSpectator() || this.target.squaredDistanceTo(this) > 64.0) {
			PlayerEntity playerEntity = this.getEntityWorld().getClosestPlayer(this, 8.0);
			if (playerEntity != null && !playerEntity.isSpectator() && !playerEntity.isDead()) {
				this.target = playerEntity;
			}
			else {
				this.target = null;
			}
		}

		if (this.target != null) {
			Vec3d vec3d = new Vec3d(
					this.target.getX() - this.getX(),
					this.target.getY() + this.target.getStandingEyeHeight() / 2.0 - this.getY(),
					this.target.getZ() - this.getZ()
			);
			double d = vec3d.lengthSquared();
			double e = 1.0 - Math.sqrt(d) / 8.0;
			this.setVelocity(this.getVelocity().add(vec3d.normalize().multiply(e * e * 0.1)));
		}
	}

	@Override
	public BlockPos getVelocityAffectingPos() {
		return this.getPosWithYOffset(0.999999F);
	}

	private void expensiveUpdate() {
		if (this.getEntityWorld() instanceof ServerWorld) {
			for (ExperienceOrbEntity experienceOrbEntity : this.getEntityWorld()
			                                                   .getEntitiesByType(
					                                                   TypeFilter.instanceOf(ExperienceOrbEntity.class),
					                                                   this.getBoundingBox().expand(0.5),
					                                                   this::isMergeable
			                                                   )) {
				this.merge(experienceOrbEntity);
			}
		}
	}

	public static void spawn(ServerWorld world, Vec3d pos, int amount) {
		spawn(world, pos, Vec3d.ZERO, amount);
	}

	public static void spawn(ServerWorld world, Vec3d pos, Vec3d velocity, int amount) {
		while (amount > 0) {
			int i = roundToOrbSize(amount);
			amount -= i;
			if (!wasMergedIntoExistingOrb(world, pos, i)) {
				world.spawnEntity(new ExperienceOrbEntity(world, pos, velocity, i));
			}
		}
	}

	private static boolean wasMergedIntoExistingOrb(ServerWorld world, Vec3d pos, int amount) {
		Box box = Box.of(pos, 1.0, 1.0, 1.0);
		int i = world.getRandom().nextInt(40);
		List<ExperienceOrbEntity>
				list =
				world.getEntitiesByType(
						TypeFilter.instanceOf(ExperienceOrbEntity.class),
						box,
						orb -> isMergeable(orb, i, amount)
				);
		if (!list.isEmpty()) {
			ExperienceOrbEntity experienceOrbEntity = list.get(0);
			experienceOrbEntity.pickingCount++;
			experienceOrbEntity.orbAge = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isMergeable(ExperienceOrbEntity other) {
		return other != this && isMergeable(other, this.getId(), this.getValue());
	}

	private static boolean isMergeable(ExperienceOrbEntity orb, int seed, int amount) {
		return !orb.isRemoved() && (orb.getId() - seed) % 40 == 0 && orb.getValue() == amount;
	}

	private void merge(ExperienceOrbEntity other) {
		this.pickingCount = this.pickingCount + other.pickingCount;
		this.orbAge = Math.min(this.orbAge, other.orbAge);
		other.discard();
	}

	private void applyWaterMovement() {
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(vec3d.x * 0.99F, Math.min(vec3d.y + 5.0E-4F, 0.06F), vec3d.z * 0.99F);
	}

	@Override
	protected void onSwimmingStart() {
	}

	@Override
	public final boolean clientDamage(DamageSource source) {
		return !this.isAlwaysInvulnerableTo(source);
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isAlwaysInvulnerableTo(source)) {
			return false;
		}
		else {
			this.scheduleVelocityUpdate();
			this.health = (int) (this.health - amount);
			if (this.health <= 0) {
				this.discard();
			}

			return true;
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putShort("Health", (short) this.health);
		view.putShort("Age", (short) this.orbAge);
		view.putShort("Value", (short) this.getValue());
		view.putInt("Count", this.pickingCount);
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.health = view.getShort("Health", (short) 5);
		this.orbAge = view.getShort("Age", (short) 0);
		this.setValue(view.getShort("Value", (short) 0));
		this.pickingCount = view.<Integer>read("Count", Codecs.POSITIVE_INT).orElse(1);
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity serverPlayerEntity) {
			if (player.experiencePickUpDelay == 0) {
				player.experiencePickUpDelay = 2;
				player.sendPickup(this, 1);
				int i = this.repairPlayerGears(serverPlayerEntity, this.getValue());
				if (i > 0) {
					player.addExperience(i);
				}

				this.pickingCount--;
				if (this.pickingCount == 0) {
					this.discard();
				}
			}
		}
	}

	private int repairPlayerGears(ServerPlayerEntity player, int amount) {
		Optional<EnchantmentEffectContext> optional = EnchantmentHelper.chooseEquipmentWith(
				EnchantmentEffectComponentTypes.REPAIR_WITH_XP, player, ItemStack::isDamaged
		);
		if (optional.isPresent()) {
			ItemStack itemStack = optional.get().stack();
			int i = EnchantmentHelper.getRepairWithExperience(player.getEntityWorld(), itemStack, amount);
			int j = Math.min(i, itemStack.getDamage());
			itemStack.setDamage(itemStack.getDamage() - j);
			if (j > 0) {
				int k = amount - j * amount / i;
				if (k > 0) {
					return this.repairPlayerGears(player, k);
				}
			}

			return 0;
		}
		else {
			return amount;
		}
	}

	public int getValue() {
		return this.dataTracker.get(VALUE);
	}

	private void setValue(int value) {
		this.dataTracker.set(VALUE, value);
	}

	public int getOrbSize() {
		int i = this.getValue();
		if (i >= 2477) {
			return 10;
		}
		else if (i >= 1237) {
			return 9;
		}
		else if (i >= 617) {
			return 8;
		}
		else if (i >= 307) {
			return 7;
		}
		else if (i >= 149) {
			return 6;
		}
		else if (i >= 73) {
			return 5;
		}
		else if (i >= 37) {
			return 4;
		}
		else if (i >= 17) {
			return 3;
		}
		else if (i >= 7) {
			return 2;
		}
		else {
			return i >= 3 ? 1 : 0;
		}
	}

	public static int roundToOrbSize(int value) {
		if (value >= 2477) {
			return 2477;
		}
		else if (value >= 1237) {
			return 1237;
		}
		else if (value >= 617) {
			return 617;
		}
		else if (value >= 307) {
			return 307;
		}
		else if (value >= 149) {
			return 149;
		}
		else if (value >= 73) {
			return 73;
		}
		else if (value >= 37) {
			return 37;
		}
		else if (value >= 17) {
			return 17;
		}
		else if (value >= 7) {
			return 7;
		}
		else {
			return value >= 3 ? 3 : 1;
		}
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.AMBIENT;
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return this.interpolator;
	}
}
