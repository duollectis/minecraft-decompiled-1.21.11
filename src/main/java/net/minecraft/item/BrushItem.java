package net.minecraft.item;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Предмет кисти для раскопок. Используется на раскапываемых блоках
 * ({@link BrushableBlock}) для постепенного извлечения предметов.
 */
public class BrushItem extends Item {

	public static final int ANIMATION_DURATION = 10;
	private static final int MAX_BRUSH_TIME = 200;
	/** Тик внутри анимационного цикла, на котором происходит реальное взаимодействие. */
	private static final int BRUSH_TICK_OFFSET = 5;
	/** Скорость разлёта частиц пыли. */
	private static final double DUST_PARTICLE_SPEED = 3.0;
	/** Минимальное количество частиц пыли за одно взаимодействие. */
	private static final int DUST_PARTICLE_MIN = 7;
	/** Максимальное количество частиц пыли за одно взаимодействие. */
	private static final int DUST_PARTICLE_MAX = 12;

	public BrushItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();

		if (player != null && getHitResult(player).getType() == HitResult.Type.BLOCK) {
			player.setCurrentHand(context.getHand());
		}

		return ActionResult.CONSUME;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BRUSH;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return MAX_BRUSH_TIME;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (remainingUseTicks < 0 || !(user instanceof PlayerEntity player)) {
			user.stopUsingItem();
			return;
		}

		HitResult hitResult = getHitResult(player);

		if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
			user.stopUsingItem();
			return;
		}

		int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks + 1;
		boolean isBrushTick = usedTicks % ANIMATION_DURATION == BRUSH_TICK_OFFSET;

		if (!isBrushTick) {
			return;
		}

		BlockPos blockPos = blockHitResult.getBlockPos();
		BlockState blockState = world.getBlockState(blockPos);
		Arm arm = user.getActiveHand() == Hand.MAIN_HAND
			? player.getMainArm()
			: player.getMainArm().getOpposite();

		if (blockState.hasBlockBreakParticles() && blockState.getRenderType() != BlockRenderType.INVISIBLE) {
			addDustParticles(world, blockHitResult, blockState, user.getRotationVec(0.0F), arm);
		}

		SoundEvent brushSound = blockState.getBlock() instanceof BrushableBlock brushable
			? brushable.getBrushingSound()
			: SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC;

		world.playSound(player, blockPos, brushSound, SoundCategory.BLOCKS);

		if (world instanceof ServerWorld serverWorld
			&& world.getBlockEntity(blockPos) instanceof BrushableBlockEntity brushableEntity
		) {
			boolean didBrush = brushableEntity.brush(
				world.getTime(), serverWorld, player, blockHitResult.getSide(), stack
			);

			if (didBrush) {
				EquipmentSlot slot = stack.equals(player.getEquippedStack(EquipmentSlot.OFFHAND))
					? EquipmentSlot.OFFHAND
					: EquipmentSlot.MAINHAND;
				stack.damage(1, player, slot);
			}
		}
	}

	private HitResult getHitResult(PlayerEntity user) {
		return ProjectileUtil.getCollision(user, EntityPredicates.CAN_HIT, user.getBlockInteractionRange());
	}

	private void addDustParticles(
		World world,
		BlockHitResult hitResult,
		BlockState state,
		Vec3d userRotation,
		Arm arm
	) {
		int armSign = arm == Arm.RIGHT ? 1 : -1;
		int particleCount = world.getRandom().nextBetweenExclusive(DUST_PARTICLE_MIN, DUST_PARTICLE_MAX);
		BlockStateParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
		Direction side = hitResult.getSide();
		DustParticlesOffset offset = DustParticlesOffset.fromSide(userRotation, side);
		Vec3d hitPos = hitResult.getPos();

		for (int index = 0; index < particleCount; index++) {
			world.addParticleClient(
				particleEffect,
				hitPos.x - (side == Direction.WEST ? 1.0E-6F : 0.0F),
				hitPos.y,
				hitPos.z - (side == Direction.NORTH ? 1.0E-6F : 0.0F),
				offset.xd() * armSign * DUST_PARTICLE_SPEED * world.getRandom().nextDouble(),
				0.0,
				offset.zd() * armSign * DUST_PARTICLE_SPEED * world.getRandom().nextDouble()
			);
		}
	}

	/**
	 * Смещение частиц пыли в зависимости от стороны блока и направления взгляда игрока.
	 */
	record DustParticlesOffset(double xd, double yd, double zd) {

		private static final double BRUSH_REACH = 1.0;
		private static final double BRUSH_OFFSET = 0.1;

		public static DustParticlesOffset fromSide(Vec3d userRotation, Direction side) {
			return switch (side) {
				case DOWN, UP -> new DustParticlesOffset(userRotation.getZ(), 0.0, -userRotation.getX());
				case NORTH -> new DustParticlesOffset(BRUSH_REACH, 0.0, -BRUSH_OFFSET);
				case SOUTH -> new DustParticlesOffset(-BRUSH_REACH, 0.0, BRUSH_OFFSET);
				case WEST -> new DustParticlesOffset(-BRUSH_OFFSET, 0.0, -BRUSH_REACH);
				case EAST -> new DustParticlesOffset(BRUSH_OFFSET, 0.0, BRUSH_REACH);
			};
		}
	}
}
