package net.minecraft.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/**
 * Предмет «Стеклянная бутылка». Позволяет набирать воду из источников
 * и собирать дыхание дракона из облаков эффектов {@link AreaEffectCloudEntity}.
 */
public class GlassBottleItem extends Item {

	/** Радиус поиска облаков дыхания дракона вокруг игрока. */
	private static final double DRAGON_BREATH_SEARCH_RADIUS = 2.0;

	/** Уменьшение радиуса облака дыхания дракона при сборе. */
	private static final float DRAGON_BREATH_RADIUS_REDUCTION = 0.5F;

	public GlassBottleItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Наполняет бутылку. Приоритет: дыхание дракона > вода из источника.
	 * При сборе дыхания дракона уменьшает радиус облака.
	 */
	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		List<AreaEffectCloudEntity> dragonBreathClouds = world.getEntitiesByClass(
				AreaEffectCloudEntity.class,
				user.getBoundingBox().expand(DRAGON_BREATH_SEARCH_RADIUS),
				entity -> entity.isAlive() && entity.getOwner() instanceof EnderDragonEntity
		);

		if (!dragonBreathClouds.isEmpty()) {
			AreaEffectCloudEntity cloud = dragonBreathClouds.get(0);
			cloud.setRadius(cloud.getRadius() - DRAGON_BREATH_RADIUS_REDUCTION);

			world.playSound(
					null,
					user.getX(),
					user.getY(),
					user.getZ(),
					SoundEvents.ITEM_BOTTLE_FILL_DRAGONBREATH,
					SoundCategory.NEUTRAL,
					1.0F,
					1.0F
			);
			world.emitGameEvent(user, GameEvent.FLUID_PICKUP, user.getEntityPos());

			if (user instanceof ServerPlayerEntity serverPlayer) {
				Criteria.PLAYER_INTERACTED_WITH_ENTITY.trigger(serverPlayer, stack, cloud);
			}

			return ActionResult.SUCCESS.withNewHandStack(fill(stack, user, new ItemStack(Items.DRAGON_BREATH)));
		}

		BlockHitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.SOURCE_ONLY);

		if (hitResult.getType() == HitResult.Type.MISS) {
			return ActionResult.PASS;
		}

		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return ActionResult.PASS;
		}

		BlockPos hitPos = hitResult.getBlockPos();

		if (!world.canEntityModifyAt(user, hitPos)) {
			return ActionResult.PASS;
		}

		if (!world.getFluidState(hitPos).isIn(FluidTags.WATER)) {
			return ActionResult.PASS;
		}

		world.playSound(
				user,
				user.getX(),
				user.getY(),
				user.getZ(),
				SoundEvents.ITEM_BOTTLE_FILL,
				SoundCategory.NEUTRAL,
				1.0F,
				1.0F
		);
		world.emitGameEvent(user, GameEvent.FLUID_PICKUP, hitPos);

		return ActionResult.SUCCESS.withNewHandStack(
				fill(stack, user, PotionContentsComponent.createStack(Items.POTION, Potions.WATER))
		);
	}

	/**
	 * Заменяет бутылку в руке игрока на заполненный стек.
	 * Если стек больше 1 — добавляет в инвентарь или выбрасывает.
	 *
	 * @param stack       исходный стек бутылки
	 * @param player      игрок
	 * @param outputStack результирующий стек (зелье или дыхание дракона)
	 * @return новый стек для руки
	 */
	protected ItemStack fill(ItemStack stack, PlayerEntity player, ItemStack outputStack) {
		player.incrementStat(Stats.USED.getOrCreateStat(this));
		return ItemUsage.exchangeStack(stack, player, outputStack);
	}
}
