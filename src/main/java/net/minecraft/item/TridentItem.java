package net.minecraft.item;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Предмет трезубца. Поддерживает бросок как снаряда и атаку вихрем (Riptide)
 * при наличии соответствующего зачарования и контакта с водой/дождём.
 */
public class TridentItem extends Item implements ProjectileItem {

	public static final int MIN_DRAW_DURATION = 10;
	public static final float ATTACK_DAMAGE = 8.0F;
	public static final float THROW_SPEED = 2.5F;

	private static final float ATTACK_SPEED_MODIFIER = -2.9F;
	private static final float RIPTIDE_JUMP_HEIGHT = 1.1999999F;
	private static final int RIPTIDE_SPIN_TICKS = 20;
	private static final float RIPTIDE_SPIN_DAMAGE = 8.0F;
	/** Максимальное время использования — фактически бесконечное удержание. */
	private static final int MAX_USE_TICKS = 72000;

	public TridentItem(Item.Settings settings) {
		super(settings);
	}

	public static AttributeModifiersComponent createAttributeModifiers() {
		return AttributeModifiersComponent.builder()
			.add(
				EntityAttributes.ATTACK_DAMAGE,
				new EntityAttributeModifier(
					BASE_ATTACK_DAMAGE_MODIFIER_ID,
					ATTACK_DAMAGE,
					EntityAttributeModifier.Operation.ADD_VALUE
				),
				AttributeModifierSlot.MAINHAND
			)
			.add(
				EntityAttributes.ATTACK_SPEED,
				new EntityAttributeModifier(
					BASE_ATTACK_SPEED_MODIFIER_ID,
					ATTACK_SPEED_MODIFIER,
					EntityAttributeModifier.Operation.ADD_VALUE
				),
				AttributeModifierSlot.MAINHAND
			)
			.build();
	}

	public static ToolComponent createToolComponent() {
		return new ToolComponent(List.of(), 1.0F, 2, false);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.TRIDENT;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return MAX_USE_TICKS;
	}

	/**
	 * Обрабатывает отпускание трезубца: либо бросает его как снаряд,
	 * либо активирует атаку вихрем (Riptide) при наличии зачарования.
	 */
	@Override
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity player)) {
			return false;
		}

		int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;

		if (usedTicks < MIN_DRAW_DURATION) {
			return false;
		}

		float riptideStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);

		if (riptideStrength > 0.0F && !player.isTouchingWaterOrRain()) {
			return false;
		}

		if (stack.willBreakNextUse()) {
			return false;
		}

		RegistryEntry<SoundEvent> throwSound = EnchantmentHelper
			.getEffect(stack, EnchantmentEffectComponentTypes.TRIDENT_SOUND)
			.orElse(SoundEvents.ITEM_TRIDENT_THROW);

		player.incrementStat(Stats.USED.getOrCreateStat(this));

		if (world instanceof ServerWorld serverWorld && riptideStrength == 0.0F) {
			stack.damage(1, player);
			ItemStack thrownStack = stack.splitUnlessCreative(1, player);
			TridentEntity trident = ProjectileEntity.spawnWithVelocity(
				TridentEntity::new, serverWorld, thrownStack, player, 0.0F, THROW_SPEED, 1.0F
			);

			if (player.isInCreativeMode()) {
				trident.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
			}

			world.playSoundFromEntity(null, trident, throwSound.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
			return true;
		}

		if (riptideStrength > 0.0F) {
			float yaw = player.getYaw();
			float pitch = player.getPitch();
			float velX = -MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
			float velY = -MathHelper.sin(pitch * (float) (Math.PI / 180.0));
			float velZ = MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
			float magnitude = MathHelper.sqrt(velX * velX + velY * velY + velZ * velZ);

			velX *= riptideStrength / magnitude;
			velY *= riptideStrength / magnitude;
			velZ *= riptideStrength / magnitude;

			player.addVelocity(velX, velY, velZ);
			player.useRiptide(RIPTIDE_SPIN_TICKS, RIPTIDE_SPIN_DAMAGE, stack);

			if (player.isOnGround()) {
				player.move(MovementType.SELF, new Vec3d(0.0, RIPTIDE_JUMP_HEIGHT, 0.0));
			}

			world.playSoundFromEntity(null, player, throwSound.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
			return true;
		}

		return false;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (stack.willBreakNextUse()) {
			return ActionResult.FAIL;
		}

		if (EnchantmentHelper.getTridentSpinAttackStrength(stack, user) > 0.0F && !user.isTouchingWaterOrRain()) {
			return ActionResult.FAIL;
		}

		user.setCurrentHand(hand);
		return ActionResult.CONSUME;
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		TridentEntity trident = new TridentEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
		trident.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
		return trident;
	}
}
