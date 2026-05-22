package net.minecraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Предмет арбалета. Поддерживает зарядку стрелами и фейерверками,
 * а также эффекты зачарований (быстрая зарядка, мультивыстрел).
 */
public class CrossbowItem extends RangedWeaponItem {

	private static final float DEFAULT_PULL_TIME = 1.25F;
	public static final int RANGE = 8;
	public static final float ARROW_SPEED = 1.6F;

	private static final float CHARGE_PROGRESS = 0.2F;
	private static final float LOAD_PROGRESS = 0.5F;
	private static final float DEFAULT_SPEED = 3.15F;
	private static final float FIREWORK_ROCKET_SPEED = 1.6F;
	/** Максимальное время использования — фактически бесконечное удержание. */
	private static final int MAX_USE_TICKS = 72000;
	private static final float FIREWORK_EYE_OFFSET_Y = 0.15F;
	private static final float FIREWORK_BODY_FRACTION = 0.3333333333333333F;
	private static final float FIREWORK_DISTANCE_FACTOR = 0.2F;

	private static final CrossbowItem.LoadingSounds DEFAULT_LOADING_SOUNDS = new CrossbowItem.LoadingSounds(
		Optional.of(SoundEvents.ITEM_CROSSBOW_LOADING_START),
		Optional.of(SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE),
		Optional.of(SoundEvents.ITEM_CROSSBOW_LOADING_END)
	);

	private boolean charged = false;
	private boolean loaded = false;

	public CrossbowItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public Predicate<ItemStack> getHeldProjectiles() {
		return CROSSBOW_HELD_PROJECTILES;
	}

	@Override
	public Predicate<ItemStack> getProjectiles() {
		return BOW_PROJECTILES;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		ChargedProjectilesComponent chargedProjectiles = stack.get(DataComponentTypes.CHARGED_PROJECTILES);

		if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
			shootAll(world, user, hand, stack, getSpeed(chargedProjectiles), 1.0F, null);
			return ActionResult.CONSUME;
		}

		if (!user.getProjectileType(stack).isEmpty()) {
			charged = false;
			loaded = false;
			user.setCurrentHand(hand);
			return ActionResult.CONSUME;
		}

		return ActionResult.FAIL;
	}

	private static float getSpeed(ChargedProjectilesComponent chargedProjectiles) {
		return chargedProjectiles.contains(Items.FIREWORK_ROCKET) ? FIREWORK_ROCKET_SPEED : DEFAULT_SPEED;
	}

	@Override
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;
		return getPullProgress(usedTicks, stack, user) >= 1.0F && isCharged(stack);
	}

	private static boolean loadProjectiles(LivingEntity shooter, ItemStack crossbow) {
		List<ItemStack> projectiles = load(crossbow, shooter.getProjectileType(crossbow), shooter);

		if (projectiles.isEmpty()) {
			return false;
		}

		crossbow.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(projectiles));
		return true;
	}

	public static boolean isCharged(ItemStack stack) {
		ChargedProjectilesComponent chargedProjectiles = stack.getOrDefault(
			DataComponentTypes.CHARGED_PROJECTILES,
			ChargedProjectilesComponent.DEFAULT
		);
		return !chargedProjectiles.isEmpty();
	}

	@Override
	protected void shoot(
		LivingEntity shooter,
		ProjectileEntity projectile,
		int index,
		float speed,
		float divergence,
		float yaw,
		@Nullable LivingEntity target
	) {
		Vector3f velocity;

		if (target != null) {
			double deltaX = target.getX() - shooter.getX();
			double deltaZ = target.getZ() - shooter.getZ();
			double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
			double deltaY = target.getBodyY(FIREWORK_BODY_FRACTION) - projectile.getY() + horizontalDist * FIREWORK_DISTANCE_FACTOR;
			velocity = calcVelocity(shooter, new Vec3d(deltaX, deltaY, deltaZ), yaw);
		} else {
			Vec3d oppositeRot = shooter.getOppositeRotationVector(1.0F);
			Quaternionf rotation = new Quaternionf().setAngleAxis(
				yaw * (float) (Math.PI / 180.0),
				oppositeRot.x,
				oppositeRot.y,
				oppositeRot.z
			);
			Vec3d rotVec = shooter.getRotationVec(1.0F);
			velocity = rotVec.toVector3f().rotate(rotation);
		}

		projectile.setVelocity(velocity.x(), velocity.y(), velocity.z(), speed, divergence);

		float soundPitch = getSoundPitch(shooter.getRandom(), index);
		shooter.getEntityWorld().playSound(
			null,
			shooter.getX(),
			shooter.getY(),
			shooter.getZ(),
			SoundEvents.ITEM_CROSSBOW_SHOOT,
			shooter.getSoundCategory(),
			1.0F,
			soundPitch
		);
	}

	private static Vector3f calcVelocity(LivingEntity shooter, Vec3d direction, float yaw) {
		Vector3f normalized = direction.toVector3f().normalize();
		Vector3f perpendicular = new Vector3f(normalized).cross(new Vector3f(0.0F, 1.0F, 0.0F));

		if (perpendicular.lengthSquared() <= 1.0E-7) {
			Vec3d oppositeRot = shooter.getOppositeRotationVector(1.0F);
			perpendicular = new Vector3f(normalized).cross(oppositeRot.toVector3f());
		}

		Vector3f upAxis = new Vector3f(normalized).rotateAxis(
			(float) (Math.PI / 2),
			perpendicular.x,
			perpendicular.y,
			perpendicular.z
		);
		return new Vector3f(normalized).rotateAxis(
			yaw * (float) (Math.PI / 180.0),
			upAxis.x,
			upAxis.y,
			upAxis.z
		);
	}

	@Override
	protected ProjectileEntity createArrowEntity(
		World world,
		LivingEntity shooter,
		ItemStack weaponStack,
		ItemStack projectileStack,
		boolean critical
	) {
		if (projectileStack.isOf(Items.FIREWORK_ROCKET)) {
			return new FireworkRocketEntity(
				world,
				projectileStack,
				shooter,
				shooter.getX(),
				shooter.getEyeY() - FIREWORK_EYE_OFFSET_Y,
				shooter.getZ(),
				true
			);
		}

		ProjectileEntity projectile = super.createArrowEntity(world, shooter, weaponStack, projectileStack, critical);

		if (projectile instanceof PersistentProjectileEntity persistent) {
			persistent.setSound(SoundEvents.ITEM_CROSSBOW_HIT);
		}

		return projectile;
	}

	@Override
	protected int getWeaponStackDamage(ItemStack projectile) {
		return projectile.isOf(Items.FIREWORK_ROCKET) ? 3 : 1;
	}

	/**
	 * Выпускает все заряженные снаряды из арбалета и сбрасывает компонент заряженных снарядов.
	 * Засчитывает достижение и статистику для игроков-серверов.
	 */
	public void shootAll(
		World world,
		LivingEntity shooter,
		Hand hand,
		ItemStack stack,
		float speed,
		float divergence,
		@Nullable LivingEntity target
	) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		ChargedProjectilesComponent chargedProjectiles = stack.set(
			DataComponentTypes.CHARGED_PROJECTILES,
			ChargedProjectilesComponent.DEFAULT
		);

		if (chargedProjectiles == null || chargedProjectiles.isEmpty()) {
			return;
		}

		shootAll(
			serverWorld,
			shooter,
			hand,
			stack,
			chargedProjectiles.getProjectiles(),
			speed,
			divergence,
			shooter instanceof PlayerEntity,
			target
		);

		if (shooter instanceof ServerPlayerEntity serverPlayer) {
			Criteria.SHOT_CROSSBOW.trigger(serverPlayer, stack);
			serverPlayer.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
		}
	}

	private static float getSoundPitch(Random random, int index) {
		return index == 0 ? 1.0F : getSoundPitch((index & 1) == 1, random);
	}

	private static float getSoundPitch(boolean flag, Random random) {
		float base = flag ? 0.63F : 0.43F;
		return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + base;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world.isClient()) {
			return;
		}

		CrossbowItem.LoadingSounds loadingSounds = getLoadingSounds(stack);
		float progress = (float) (stack.getMaxUseTime(user) - remainingUseTicks) / getPullTime(stack, user);

		if (progress < CHARGE_PROGRESS) {
			charged = false;
			loaded = false;
		}

		if (progress >= CHARGE_PROGRESS && !charged) {
			charged = true;
			loadingSounds.start().ifPresent(sound -> world.playSound(
				null,
				user.getX(),
				user.getY(),
				user.getZ(),
				sound.value(),
				SoundCategory.PLAYERS,
				0.5F,
				1.0F
			));
		}

		if (progress >= LOAD_PROGRESS && !loaded) {
			loaded = true;
			loadingSounds.mid().ifPresent(sound -> world.playSound(
				null,
				user.getX(),
				user.getY(),
				user.getZ(),
				sound.value(),
				SoundCategory.PLAYERS,
				0.5F,
				1.0F
			));
		}

		if (progress >= 1.0F && !isCharged(stack) && loadProjectiles(user, stack)) {
			loadingSounds.end().ifPresent(sound -> world.playSound(
				null,
				user.getX(),
				user.getY(),
				user.getZ(),
				sound.value(),
				user.getSoundCategory(),
				1.0F,
				1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + CHARGE_PROGRESS
			));
		}
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return MAX_USE_TICKS;
	}

	/**
	 * Вычисляет время зарядки арбалета в тиках с учётом зачарования быстрой зарядки.
	 *
	 * @param stack стек арбалета
	 * @param user  сущность, заряжающая арбалет
	 * @return количество тиков для полной зарядки
	 */
	public static int getPullTime(ItemStack stack, LivingEntity user) {
		float chargeTime = EnchantmentHelper.getCrossbowChargeTime(stack, user, DEFAULT_PULL_TIME);
		return MathHelper.floor(chargeTime * 20.0F);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.CROSSBOW;
	}

	CrossbowItem.LoadingSounds getLoadingSounds(ItemStack stack) {
		return EnchantmentHelper
			.getEffect(stack, EnchantmentEffectComponentTypes.CROSSBOW_CHARGING_SOUNDS)
			.orElse(DEFAULT_LOADING_SOUNDS);
	}

	private static float getPullProgress(int useTicks, ItemStack stack, LivingEntity user) {
		float progress = (float) useTicks / getPullTime(stack, user);
		return Math.min(progress, 1.0F);
	}

	@Override
	public boolean isUsedOnRelease(ItemStack stack) {
		return stack.isOf(this);
	}

	@Override
	public int getRange() {
		return RANGE;
	}

	/**
	 * Тип заряда арбалета: пустой, стрела или ракета-фейерверк.
	 */
	public enum ChargeType implements StringIdentifiable {
		NONE("none"),
		ARROW("arrow"),
		ROCKET("rocket");

		public static final Codec<CrossbowItem.ChargeType> CODEC = StringIdentifiable.createCodec(CrossbowItem.ChargeType::values);

		private final String name;

		ChargeType(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Звуки загрузки арбалета: начало, середина и конец зарядки.
	 * Может быть переопределён зачарованием.
	 */
	public record LoadingSounds(
		Optional<RegistryEntry<SoundEvent>> start,
		Optional<RegistryEntry<SoundEvent>> mid,
		Optional<RegistryEntry<SoundEvent>> end
	) {

		public static final Codec<CrossbowItem.LoadingSounds> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				SoundEvent.ENTRY_CODEC.optionalFieldOf("start").forGetter(CrossbowItem.LoadingSounds::start),
				SoundEvent.ENTRY_CODEC.optionalFieldOf("mid").forGetter(CrossbowItem.LoadingSounds::mid),
				SoundEvent.ENTRY_CODEC.optionalFieldOf("end").forGetter(CrossbowItem.LoadingSounds::end)
			).apply(instance, CrossbowItem.LoadingSounds::new)
		);
	}
}
