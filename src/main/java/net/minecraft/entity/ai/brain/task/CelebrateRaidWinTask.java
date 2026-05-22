package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.raid.Raid;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Задача мозга жителя, запускающая празднование победы в рейде:
 * периодически воспроизводит звук и запускает фейерверки случайного цвета.
 */
public class CelebrateRaidWinTask extends MultiTickTask<VillagerEntity> {

	private static final int CELEBRATE_SOUND_CHANCE = 100;
	private static final int FIREWORK_CHANCE = 200;
	private static final int MAX_FIREWORK_FLIGHT = 3;

	private @Nullable Raid raid;

	public CelebrateRaidWinTask(int minRunTime, int maxRunTime) {
		super(ImmutableMap.of(), minRunTime, maxRunTime);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		BlockPos pos = entity.getBlockPos();
		raid = world.getRaidAt(pos);
		return raid != null && raid.hasWon() && SeekSkyTask.isSkyVisible(world, entity, pos);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return raid != null && !raid.hasStopped();
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		raid = null;
		entity.getBrain().refreshActivities(world.getEnvironmentAttributes(), world.getTime(), entity.getEntityPos());
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		Random random = entity.getRandom();
		if (random.nextInt(CELEBRATE_SOUND_CHANCE) == 0) {
			entity.playCelebrateSound();
		}

		if (random.nextInt(FIREWORK_CHANCE) == 0 && SeekSkyTask.isSkyVisible(world, entity, entity.getBlockPos())) {
			DyeColor color = Util.getRandom(DyeColor.values(), random);
			int flight = random.nextInt(MAX_FIREWORK_FLIGHT);
			ItemStack firework = createFirework(color, flight);
			ProjectileEntity.spawn(
					new FireworkRocketEntity(
							entity.getEntityWorld(),
							entity,
							entity.getX(),
							entity.getEyeY(),
							entity.getZ(),
							firework
					),
					world,
					firework
			);
		}
	}

	private ItemStack createFirework(DyeColor color, int flight) {
		ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
		itemStack.set(
				DataComponentTypes.FIREWORKS,
				new FireworksComponent(
						(byte) flight,
						List.of(new FireworkExplosionComponent(
								FireworkExplosionComponent.Type.BURST,
								IntList.of(color.getFireworkColor()),
								IntList.of(),
								false,
								false
						))
				)
		);
		return itemStack;
	}
}
