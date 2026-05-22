package net.minecraft.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Предмет «Глаз Края». Имеет два режима использования:
 * <ul>
 *   <li>При клике на рамку портала Края — вставляет глаз и, если рамка завершена, открывает портал.</li>
 *   <li>При использовании в воздухе — запускает снаряд, летящий в сторону крепости.</li>
 * </ul>
 */
public class EnderEyeItem extends Item {

	/** Радиус поиска крепости в чанках. */
	private static final int STRONGHOLD_SEARCH_RADIUS = 100;

	/** Диапазон случайного питча звука броска. */
	private static final float THROW_PITCH_MIN = 0.33F;
	private static final float THROW_PITCH_MAX = 0.5F;

	/** Флаг обновления блока при установке глаза в рамку. */
	private static final int FRAME_UPDATE_FLAGS = 2;

	/** Смещение рамки портала для поиска угла паттерна (3x3 рамка). */
	private static final int PORTAL_FRAME_OFFSET = -3;

	/** Размер внутренней части портала Края. */
	private static final int PORTAL_INNER_SIZE = 3;

	public EnderEyeItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Вставляет глаз Края в рамку портала. Если все 12 рамок заполнены,
	 * заменяет внутренние блоки 3x3 на блоки портала Края и воспроизводит глобальный звук.
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (!blockState.isOf(Blocks.END_PORTAL_FRAME) || blockState.get(EndPortalFrameBlock.EYE)) {
			return ActionResult.PASS;
		}

		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}

		BlockState filledFrame = blockState.with(EndPortalFrameBlock.EYE, true);
		Block.pushEntitiesUpBeforeBlockChange(blockState, filledFrame, world, pos);
		world.setBlockState(pos, filledFrame, FRAME_UPDATE_FLAGS);
		world.updateComparators(pos, Blocks.END_PORTAL_FRAME);
		context.getStack().decrement(1);
		world.syncWorldEvent(1503, pos, 0);

		BlockPattern.Result patternResult = EndPortalFrameBlock.getCompletedFramePattern().searchAround(world, pos);

		if (patternResult != null) {
			BlockPos portalOrigin = patternResult.getFrontTopLeft().add(PORTAL_FRAME_OFFSET, 0, PORTAL_FRAME_OFFSET);

			for (int dx = 0; dx < PORTAL_INNER_SIZE; dx++) {
				for (int dz = 0; dz < PORTAL_INNER_SIZE; dz++) {
					BlockPos portalBlock = portalOrigin.add(dx, 0, dz);
					world.breakBlock(portalBlock, true, null);
					world.setBlockState(portalBlock, Blocks.END_PORTAL.getDefaultState(), FRAME_UPDATE_FLAGS);
				}
			}

			world.syncGlobalEvent(1038, portalOrigin.add(1, 0, 1), 0);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 0;
	}

	/**
	 * Запускает снаряд «Глаз Края», летящий в сторону ближайшей крепости.
	 * Если крепость не найдена — потребляет предмет без эффекта.
	 */
	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		BlockHitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.NONE);

		if (hitResult.getType() == HitResult.Type.BLOCK
				&& world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.END_PORTAL_FRAME)
		) {
			return ActionResult.PASS;
		}

		user.setCurrentHand(hand);

		if (world instanceof ServerWorld serverWorld) {
			BlockPos strongholdPos = serverWorld.locateStructure(
					StructureTags.EYE_OF_ENDER_LOCATED,
					user.getBlockPos(),
					STRONGHOLD_SEARCH_RADIUS,
					false
			);

			if (strongholdPos == null) {
				return ActionResult.CONSUME;
			}

			EyeOfEnderEntity eyeEntity = new EyeOfEnderEntity(world, user.getX(), user.getBodyY(0.5), user.getZ());
			eyeEntity.setItem(stack);
			eyeEntity.initTargetPos(Vec3d.of(strongholdPos));
			world.emitGameEvent(GameEvent.PROJECTILE_SHOOT, eyeEntity.getEntityPos(), GameEvent.Emitter.of(user));
			world.spawnEntity(eyeEntity);

			if (user instanceof ServerPlayerEntity serverPlayer) {
				Criteria.USED_ENDER_EYE.trigger(serverPlayer, strongholdPos);
			}

			float throwPitch = MathHelper.lerp(world.random.nextFloat(), THROW_PITCH_MIN, THROW_PITCH_MAX);
			world.playSound(
					null,
					user.getX(),
					user.getY(),
					user.getZ(),
					SoundEvents.ENTITY_ENDER_EYE_LAUNCH,
					SoundCategory.NEUTRAL,
					1.0F,
					throwPitch
			);

			stack.decrementUnlessCreative(1, user);
			user.incrementStat(Stats.USED.getOrCreateStat(this));
		}

		return ActionResult.SUCCESS_SERVER;
	}
}
