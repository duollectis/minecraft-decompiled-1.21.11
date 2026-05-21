package net.minecraft.client.render.debug;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.NameGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.BeeDebugData;
import net.minecraft.world.debug.data.BeeHiveDebugData;
import net.minecraft.world.debug.data.GoalSelectorDebugData;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code BeeDebugRenderer}.
 */
public class BeeDebugRenderer implements DebugRenderer.Renderer {

	private static final boolean SHOW_BEE_INFO = true;
	private static final boolean SHOW_HIVE_INFO = true;
	private static final boolean SHOW_FLOWER_INFO = true;
	private static final boolean SHOW_TRAVEL_TICKS = true;
	private static final boolean SHOW_GOALS = true;
	private static final boolean SHOW_HIVE_BEES = true;
	private static final boolean SHOW_BLACKLISTED_HIVES = true;
	private static final boolean SHOW_GHOST_HIVES = true;
	private static final boolean SHOW_HIVE_OCCUPANTS = true;
	private static final boolean SHOW_HONEY_LEVEL = true;
	private static final boolean SHOW_HIVE_TYPE = true;
	private static final boolean SHOW_FLOWERS_MAP = true;
	private static final int HIVE_RANGE = 30;
	private static final int BEE_RANGE = 30;
	private static final int TARGET_ENTITY_RANGE = 8;
	private static final float DEFAULT_DRAWN_STRING_SIZE = 0.32F;
	private static final int ORANGE = -23296;
	private static final int GRAY = -3355444;
	private static final int PINK = -98404;
	private final MinecraftClient client;
	private @Nullable UUID targetedEntity;

	public BeeDebugRenderer(MinecraftClient client) {
		this.client = client;
	}

	@Override
	public void render(
			double cameraX,
			double cameraY,
			double cameraZ,
			DebugDataStore store,
			Frustum frustum,
			float tickProgress
	) {
		this.render(store);
		if (!this.client.player.isSpectator()) {
			this.updateTargetedEntity();
		}
	}

	private void render(DebugDataStore store) {
		BlockPos blockPos = this.getCameraPos().getBlockPos();
		store.forEachEntityData(
				DebugSubscriptionTypes.BEES, (bee, debugData) -> {
					if (this.client.player.isInRange(bee, 30.0)) {
						GoalSelectorDebugData
								goalSelectorDebugData =
								store.getEntityData(DebugSubscriptionTypes.GOAL_SELECTORS, bee);
						this.drawBee(bee, debugData, goalSelectorDebugData);
					}
				}
		);
		this.drawFlowers(store);
		Map<BlockPos, Set<UUID>> map = this.getBlacklistingBees(store);
		store.forEachBlockData(
				DebugSubscriptionTypes.BEE_HIVES, (hivePos, debugData) -> {
					if (blockPos.isWithinDistance(hivePos, 30.0)) {
						drawHive(hivePos);
						Set<UUID> set = map.getOrDefault(hivePos, Set.of());
						this.drawHiveInfo(hivePos, debugData, set, store);
					}
				}
		);
		this.getBeesByHive(store).forEach((hivePos, names) -> {
			if (blockPos.isWithinDistance(hivePos, 30.0)) {
				this.drawHiveBees(hivePos, (List<String>) names);
			}
		});
	}

	private Map<BlockPos, Set<UUID>> getBlacklistingBees(DebugDataStore dataStore) {
		Map<BlockPos, Set<UUID>> map = new HashMap<>();
		dataStore.forEachEntityData(
				DebugSubscriptionTypes.BEES, (entity, data) -> {
					for (BlockPos blockPos : data.blacklistedHives()) {
						map.computeIfAbsent(blockPos, pos -> new HashSet<>()).add(entity.getUuid());
					}
				}
		);
		return map;
	}

	private void drawFlowers(DebugDataStore store) {
		Map<BlockPos, Set<UUID>> map = new HashMap<>();
		store.forEachEntityData(
				DebugSubscriptionTypes.BEES, (entity, data) -> {
					if (data.flowerPos().isPresent()) {
						map.computeIfAbsent(data.flowerPos().get(), flower -> new HashSet<>()).add(entity.getUuid());
					}
				}
		);
		map.forEach((flowerPos, set) -> {
			Set<String> set2 = set.stream().map(NameGenerator::name).collect(Collectors.toSet());
			int i = 1;
			GizmoDrawing.blockLabel(set2.toString(), flowerPos, i++, -256, 0.32F);
			GizmoDrawing.blockLabel("Flower", flowerPos, i++, -1, 0.32F);
			GizmoDrawing.box(flowerPos, 0.05F, DrawStyle.filled(ColorHelper.fromFloats(0.3F, 0.8F, 0.8F, 0.0F)));
		});
	}

	private static String toString(Collection<UUID> bees) {
		if (bees.isEmpty()) {
			return "-";
		}
		else {
			return bees.size() > 3 ? bees.size() + " bees"
			                       : bees.stream().map(NameGenerator::name).collect(Collectors.toSet()).toString();
		}
	}

	private static void drawHive(BlockPos hivePos) {
		float f = 0.05F;
		GizmoDrawing.box(hivePos, 0.05F, DrawStyle.filled(ColorHelper.fromFloats(0.3F, 0.2F, 0.2F, 1.0F)));
	}

	private void drawHiveBees(BlockPos hivePos, List<String> names) {
		float f = 0.05F;
		GizmoDrawing.box(hivePos, 0.05F, DrawStyle.filled(ColorHelper.fromFloats(0.3F, 0.2F, 0.2F, 1.0F)));
		GizmoDrawing.blockLabel(names.toString(), hivePos, 0, -256, 0.32F);
		GizmoDrawing.blockLabel("Ghost Hive", hivePos, 1, -65536, 0.32F);
	}

	private void drawHiveInfo(
			BlockPos blockPos,
			BeeHiveDebugData debugData,
			Collection<UUID> blacklist,
			DebugDataStore store
	) {
		int i = 0;
		if (!blacklist.isEmpty()) {
			drawString("Blacklisted by " + toString(blacklist), blockPos, i++, -65536);
		}

		drawString("Out: " + toString(this.getBeesForHive(blockPos, store)), blockPos, i++, -3355444);
		if (debugData.occupantCount() == 0) {
			drawString("In: -", blockPos, i++, -256);
		}
		else if (debugData.occupantCount() == 1) {
			drawString("In: 1 bee", blockPos, i++, -256);
		}
		else {
			drawString("In: " + debugData.occupantCount() + " bees", blockPos, i++, -256);
		}

		drawString("Honey: " + debugData.honeyLevel(), blockPos, i++, -23296);
		drawString(
				debugData.type().getName().getString() + (debugData.sedated() ? " (sedated)" : ""),
				blockPos,
				i++,
				-1
		);
	}

	private void drawBee(Entity bee, BeeDebugData debugData, @Nullable GoalSelectorDebugData goalSelectorDebugData) {
		boolean bl = this.isTargeted(bee);
		int i = 0;
		GizmoDrawing.entityLabel(bee, i++, debugData.toString(), -1, 0.48F);
		if (debugData.hivePos().isEmpty()) {
			GizmoDrawing.entityLabel(bee, i++, "No hive", -98404, 0.32F);
		}
		else {
			GizmoDrawing.entityLabel(
					bee,
					i++,
					"Hive: " + this.getPositionString(bee, debugData.hivePos().get()),
					-256,
					0.32F
			);
		}

		if (debugData.flowerPos().isEmpty()) {
			GizmoDrawing.entityLabel(bee, i++, "No flower", -98404, 0.32F);
		}
		else {
			GizmoDrawing.entityLabel(
					bee,
					i++,
					"Flower: " + this.getPositionString(bee, debugData.flowerPos().get()),
					-256,
					0.32F
			);
		}

		if (goalSelectorDebugData != null) {
			for (GoalSelectorDebugData.Goal goal : goalSelectorDebugData.goals()) {
				if (goal.isRunning()) {
					GizmoDrawing.entityLabel(bee, i++, goal.name(), -16711936, 0.32F);
				}
			}
		}

		if (debugData.travelTicks() > 0) {
			int j = debugData.travelTicks() < 2400 ? -3355444 : -23296;
			GizmoDrawing.entityLabel(bee, i++, "Travelling: " + debugData.travelTicks() + " ticks", j, 0.32F);
		}
	}

	private static void drawString(String string, BlockPos blockPos, int yOffset, int color) {
		GizmoDrawing.blockLabel(string, blockPos, yOffset, color, 0.32F);
	}

	private Camera getCameraPos() {
		return this.client.gameRenderer.getCamera();
	}

	private String getPositionString(Entity bee, BlockPos pos) {
		double d = pos.getSquaredDistance(bee.getEntityPos());
		double e = Math.round(d * 10.0) / 10.0;
		return pos.toShortString() + " (dist " + e + ")";
	}

	private boolean isTargeted(Entity bee) {
		return Objects.equals(this.targetedEntity, bee.getUuid());
	}

	private Collection<UUID> getBeesForHive(BlockPos pos, DebugDataStore dataStore) {
		Set<UUID> set = new HashSet<>();
		dataStore.forEachEntityData(
				DebugSubscriptionTypes.BEES, (entity, data) -> {
					if (data.hivePosEquals(pos)) {
						set.add(entity.getUuid());
					}
				}
		);
		return set;
	}

	private Map<BlockPos, List<String>> getBeesByHive(DebugDataStore dataStore) {
		Map<BlockPos, List<String>> map = new HashMap<>();
		dataStore.forEachEntityData(
				DebugSubscriptionTypes.BEES, (entity, data) -> {
					if (data.hivePos().isPresent()
							&& dataStore.getBlockData(DebugSubscriptionTypes.BEE_HIVES, data.hivePos().get()) == null) {
						map
								.computeIfAbsent(data.hivePos().get(), hive -> Lists.newArrayList())
								.add(NameGenerator.name(entity));
					}
				}
		);
		return map;
	}

	private void updateTargetedEntity() {
		DebugRenderer
				.getTargetedEntity(this.client.getCameraEntity(), 8)
				.ifPresent(entity -> this.targetedEntity = entity.getUuid());
	}
}
