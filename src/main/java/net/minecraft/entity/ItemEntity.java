package net.minecraft.entity;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Сущность выброшенного предмета. Поддерживает слияние стаков ({@code tryMerge}),
 * задержку подбора ({@code pickupDelay}), бесконечное существование ({@code NEVER_DESPAWN_AGE})
 * и исчезновение через 6000 тиков. Владелец ({@code owner}) ограничивает, кто может подобрать предмет.
 */
public class ItemEntity extends Entity implements Ownable {

	private static final TrackedData<ItemStack> STACK =
		DataTracker.registerData(ItemEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	private static final float ITEM_VERTICAL_OFFSET = 0.1F;
	public static final float ITEM_EYE_HEIGHT = 0.2125F;
	private static final int DESPAWN_AGE = 6000;
	private static final int CANNOT_PICK_UP_DELAY = 32767;
	private static final int NEVER_DESPAWN_AGE = -32768;
	private static final int DEFAULT_HEALTH = 5;
	/** Тик, за один до исчезновения: DESPAWN_AGE - 1 */
	private static final int ALMOST_DESPAWN_AGE = DESPAWN_AGE - 1;
	private int itemAge = 0;
	private int pickupDelay = 0;
	private int health = DEFAULT_HEALTH;
	private @Nullable LazyEntityReference<Entity> thrower;
	private @Nullable UUID owner;
	public final float uniqueOffset = this.random.nextFloat() * (float) Math.PI * 2.0F;

	public ItemEntity(EntityType<? extends ItemEntity> entityType, World world) {
		super(entityType, world);
		this.setYaw(this.random.nextFloat() * 360.0F);
	}

	public ItemEntity(World world, double x, double y, double z, ItemStack stack) {
		this(world, x, y, z, stack, world.random.nextDouble() * 0.2 - ITEM_VERTICAL_OFFSET, 0.2, world.random.nextDouble() * 0.2 - ITEM_VERTICAL_OFFSET);
	}

	public ItemEntity(
			World world,
			double x,
			double y,
			double z,
			ItemStack stack,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		this(EntityType.ITEM, world);
		this.setPosition(x, y, z);
		this.setVelocity(velocityX, velocityY, velocityZ);
		this.setStack(stack);
	}

	@Override
	public boolean occludeVibrationSignals() {
		return this.getStack().isIn(ItemTags.DAMPENS_VIBRATIONS);
	}

	@Override
	public @Nullable Entity getOwner() {
		return LazyEntityReference.getEntity(this.thrower, this.getEntityWorld());
	}

	@Override
	public void copyFrom(Entity original) {
		super.copyFrom(original);
		if (original instanceof ItemEntity itemEntity) {
			this.thrower = itemEntity.thrower;
		}
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(STACK, ItemStack.EMPTY);
	}

	@Override
	protected double getGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		if (this.getStack().isEmpty()) {
			this.discard();
			return;
		}

		super.tick();
		if (this.pickupDelay > 0 && this.pickupDelay != CANNOT_PICK_UP_DELAY) {
			this.pickupDelay--;
		}

		this.lastX = this.getX();
		this.lastY = this.getY();
		this.lastZ = this.getZ();
		Vec3d velocityBefore = this.getVelocity();

		if (this.isTouchingWater() && this.getFluidHeight(FluidTags.WATER) > ITEM_VERTICAL_OFFSET) {
			this.applyWaterBuoyancy();
		}
		else if (this.isInLava() && this.getFluidHeight(FluidTags.LAVA) > ITEM_VERTICAL_OFFSET) {
			this.applyLavaBuoyancy();
		}
		else {
			this.applyGravity();
		}

		if (this.getEntityWorld().isClient()) {
			this.noClip = false;
		}
		else {
			this.noClip = !this.getEntityWorld().isSpaceEmpty(this, this.getBoundingBox().contract(1.0E-7));
			if (this.noClip) {
				this.pushOutOfBlocks(
						this.getX(),
						(this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0,
						this.getZ()
				);
			}
		}

		if (!this.isOnGround() || this.getVelocity().horizontalLengthSquared() > 1.0E-5F
				|| (this.age + this.getId()) % 4 == 0) {
			this.move(MovementType.SELF, this.getVelocity());
			this.tickBlockCollision();
			float friction = 0.98F;
			if (this.isOnGround()) {
				friction = this.getEntityWorld()
						.getBlockState(this.getVelocityAffectingPos())
						.getBlock()
						.getSlipperiness() * 0.98F;
			}

			this.setVelocity(this.getVelocity().multiply(friction, 0.98, friction));
			if (this.isOnGround()) {
				Vec3d currentVelocity = this.getVelocity();
				if (currentVelocity.y < 0.0) {
					this.setVelocity(currentVelocity.multiply(1.0, -0.5, 1.0));
				}
			}
		}

		boolean movedBlock = MathHelper.floor(this.lastX) != MathHelper.floor(this.getX())
				|| MathHelper.floor(this.lastY) != MathHelper.floor(this.getY())
				|| MathHelper.floor(this.lastZ) != MathHelper.floor(this.getZ());
		int mergeInterval = movedBlock ? 2 : 40;
		if (this.age % mergeInterval == 0 && !this.getEntityWorld().isClient() && this.canMerge()) {
			this.tryMerge();
		}

		if (this.itemAge != NEVER_DESPAWN_AGE) {
			this.itemAge++;
		}

		this.velocityDirty = this.velocityDirty | this.updateWaterState();
		if (!this.getEntityWorld().isClient()) {
			double velocityChangeSq = this.getVelocity().subtract(velocityBefore).lengthSquared();
			if (velocityChangeSq > 0.01) {
				this.velocityDirty = true;
			}
		}

		if (!this.getEntityWorld().isClient() && this.itemAge >= DESPAWN_AGE) {
			this.discard();
		}
	}

	@Override
	public BlockPos getVelocityAffectingPos() {
		return this.getPosWithYOffset(0.999999F);
	}

	private void applyWaterBuoyancy() {
		this.applyBuoyancy(0.99F);
	}

	private void applyLavaBuoyancy() {
		this.applyBuoyancy(0.95F);
	}

	private void applyBuoyancy(double horizontalMultiplier) {
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(
				vec3d.x * horizontalMultiplier,
				vec3d.y + (vec3d.y < 0.06F ? 5.0E-4F : 0.0F),
				vec3d.z * horizontalMultiplier
		);
	}

	private void tryMerge() {
		if (this.canMerge()) {
			for (ItemEntity itemEntity : this.getEntityWorld()
			                                 .getEntitiesByClass(
					                                 ItemEntity.class,
					                                 this.getBoundingBox().expand(0.5, 0.0, 0.5),
					                                 otherItemEntity -> otherItemEntity != this
							                                 && otherItemEntity.canMerge()
			                                 )) {
				if (itemEntity.canMerge()) {
					this.tryMerge(itemEntity);
					if (this.isRemoved()) {
						break;
					}
				}
			}
		}
	}

	private boolean canMerge() {
		ItemStack itemStack = this.getStack();
		return this.isAlive() && this.pickupDelay != CANNOT_PICK_UP_DELAY && this.itemAge != NEVER_DESPAWN_AGE && this.itemAge < DESPAWN_AGE
				&& itemStack.getCount() < itemStack.getMaxCount();
	}

	private void tryMerge(ItemEntity other) {
		ItemStack itemStack = this.getStack();
		ItemStack itemStack2 = other.getStack();
		if (Objects.equals(this.owner, other.owner) && canMerge(itemStack, itemStack2)) {
			if (itemStack2.getCount() < itemStack.getCount()) {
				merge(this, itemStack, other, itemStack2);
			}
			else {
				merge(other, itemStack2, this, itemStack);
			}
		}
	}

	public static boolean canMerge(ItemStack stack1, ItemStack stack2) {
		return stack2.getCount() + stack1.getCount() <= stack2.getMaxCount()
				&& ItemStack.areItemsAndComponentsEqual(stack1, stack2);
	}

	public static ItemStack merge(ItemStack stack1, ItemStack stack2, int maxCount) {
		int i = Math.min(Math.min(stack1.getMaxCount(), maxCount) - stack1.getCount(), stack2.getCount());
		ItemStack itemStack = stack1.copyWithCount(stack1.getCount() + i);
		stack2.decrement(i);
		return itemStack;
	}

	private static void merge(ItemEntity targetEntity, ItemStack stack1, ItemStack stack2) {
		ItemStack itemStack = merge(stack1, stack2, Item.DEFAULT_MAX_COUNT);
		targetEntity.setStack(itemStack);
	}

	private static void merge(
			ItemEntity targetEntity,
			ItemStack targetStack,
			ItemEntity sourceEntity,
			ItemStack sourceStack
	) {
		merge(targetEntity, targetStack, sourceStack);
		targetEntity.pickupDelay = Math.max(targetEntity.pickupDelay, sourceEntity.pickupDelay);
		targetEntity.itemAge = Math.min(targetEntity.itemAge, sourceEntity.itemAge);
		if (sourceStack.isEmpty()) {
			sourceEntity.discard();
		}
	}

	@Override
	public boolean isFireImmune() {
		return !this.getStack().takesDamageFrom(this.getDamageSources().inFire()) || super.isFireImmune();
	}

	@Override
	protected boolean shouldPlayBurnSoundInLava() {
		return this.health <= 0 || this.age % 10 == 0;
	}

	@Override
	public final boolean clientDamage(DamageSource source) {
		return !this.isAlwaysInvulnerableTo(source) && this.getStack().takesDamageFrom(source);
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isAlwaysInvulnerableTo(source)) {
			return false;
		}
		else if (!world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				&& source.getAttacker() instanceof MobEntity) {
			return false;
		}
		else if (!this.getStack().takesDamageFrom(source)) {
			return false;
		}
		else {
			this.scheduleVelocityUpdate();
			this.health = (int) (this.health - amount);
			this.emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
			if (this.health <= 0) {
				this.getStack().onItemEntityDestroyed(this);
				this.discard();
			}

			return true;
		}
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		return !explosion.preservesDecorativeEntities() || super.isImmuneToExplosion(explosion);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putShort("Health", (short) this.health);
		view.putShort("Age", (short) this.itemAge);
		view.putShort("PickupDelay", (short) this.pickupDelay);
		LazyEntityReference.writeData(this.thrower, view, "Thrower");
		view.putNullable("Owner", Uuids.INT_STREAM_CODEC, this.owner);
		if (!this.getStack().isEmpty()) {
			view.put("Item", ItemStack.CODEC, this.getStack());
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.health = view.getShort("Health", (short) DEFAULT_HEALTH);
		this.itemAge = view.getShort("Age", (short) 0);
		this.pickupDelay = view.getShort("PickupDelay", (short) 0);
		this.owner = view.<UUID>read("Owner", Uuids.INT_STREAM_CODEC).orElse(null);
		this.thrower = LazyEntityReference.fromData(view, "Thrower");
		this.setStack(view.<ItemStack>read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		if (this.getStack().isEmpty()) {
			this.discard();
		}
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (this.getEntityWorld().isClient()) {
			return;
		}

		ItemStack itemStack = this.getStack();
		Item item = itemStack.getItem();
		int count = itemStack.getCount();
		if (this.pickupDelay == 0
				&& (this.owner == null || this.owner.equals(player.getUuid()))
				&& player.getInventory().insertStack(itemStack)) {
			player.sendPickup(this, count);
			if (itemStack.isEmpty()) {
				this.discard();
				itemStack.setCount(count);
			}

			player.increaseStat(Stats.PICKED_UP.getOrCreateStat(item), count);
			player.triggerItemPickedUpByEntityCriteria(this);
		}
	}

	@Override
	public Text getName() {
		Text text = this.getCustomName();
		return text != null ? text : this.getStack().getItemName();
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		Entity entity = super.teleportTo(teleportTarget);
		if (!this.getEntityWorld().isClient() && entity instanceof ItemEntity itemEntity) {
			itemEntity.tryMerge();
		}

		return entity;
	}

	public ItemStack getStack() {
		return this.getDataTracker().get(STACK);
	}

	public void setStack(ItemStack stack) {
		this.getDataTracker().set(STACK, stack);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (STACK.equals(data)) {
			this.getStack().setHolder(this);
		}
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
	}

	public void setThrower(Entity thrower) {
		this.thrower = LazyEntityReference.of(thrower);
	}

	public int getItemAge() {
		return this.itemAge;
	}

	public void setToDefaultPickupDelay() {
		this.pickupDelay = 10;
	}

	public void resetPickupDelay() {
		this.pickupDelay = 0;
	}

	public void setPickupDelayInfinite() {
		this.pickupDelay = CANNOT_PICK_UP_DELAY;
	}

	public void setPickupDelay(int pickupDelay) {
		this.pickupDelay = pickupDelay;
	}

	public boolean cannotPickup() {
		return this.pickupDelay > 0;
	}

	public void setNeverDespawn() {
		this.itemAge = NEVER_DESPAWN_AGE;
	}

	public void setCovetedItem() {
		this.itemAge = -DESPAWN_AGE;
	}

	public void setDespawnImmediately() {
		this.setPickupDelayInfinite();
		this.itemAge = ALMOST_DESPAWN_AGE;
	}

	public static float getRotation(float age, float uniqueOffset) {
		return age / 20.0F + uniqueOffset;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.AMBIENT;
	}

	@Override
	public float getBodyYaw() {
		return 180.0F - getRotation(this.getItemAge() + 0.5F, this.uniqueOffset) / (float) (Math.PI * 2) * 360.0F;
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		return slot == 0 ? StackReference.of(this::getStack, this::setStack) : super.getStackReference(slot);
	}
}
