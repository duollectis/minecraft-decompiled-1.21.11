package net.minecraft.client.render.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.BrainDebugData;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code BrainDebugRenderer}.
 */
public class BrainDebugRenderer implements DebugRenderer.Renderer {

	private static final boolean SHOW_NAME = true;
	private static final boolean SHOW_PROFESSION = false;
	private static final boolean SHOW_BEHAVIORS = false;
	private static final boolean SHOW_ACTIVITIES = false;
	private static final boolean SHOW_INVENTORY = false;
	private static final boolean SHOW_GOSSIPS = false;
	private static final boolean SHOW_PATH = false;
	private static final boolean SHOW_HEALTH = true;
	private static final boolean SHOW_WANTEDGOLEM = false;
	private static final boolean SHOW_ANGER_LEVEL = true;
	private static final boolean SHOW_BREEDING_COOLDOWN = true;
	private static final boolean SHOW_LOOK_TARGET = true;
	private static final boolean SHOW_WALK_TARGET = true;
	private static final boolean SHOW_POI_JOB = true;
	private static final boolean SHOW_POI_HOME = true;
	private static final boolean SHOW_POI_MEETING = true;
	private static final boolean SHOW_MOBS_NEARBY = true;
	private static final boolean SHOW_MEMORY_MAP = true;
	private static final boolean SHOW_POTENTIAL_JOB_SITES = true;
	private static final int POI_RANGE = 30;
	private static final int TARGET_ENTITY_RANGE = 8;
	private static final float DEFAULT_DRAWN_STRING_SIZE = 0.32F;
	private static final int AQUA = -16711681;
	private static final int GRAY = -3355444;
	private static final int PINK = -98404;
	private static final int ORANGE = -23296;
	private final MinecraftClient client;
	private @Nullable UUID targetedEntity;

	public BrainDebugRenderer(MinecraftClient client) {
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
		this.draw(store);
		if (!this.client.player.isSpectator()) {
			this.updateTargetedEntity();
		}
	}

	private void draw(DebugDataStore debugDataStore) {
		debugDataStore.forEachEntityData(
				DebugSubscriptionTypes.BRAINS, (entity, brainDebugData) -> {
					if (this.client.player.isInRange(entity, 30.0)) {
						this.drawBrain(entity, brainDebugData);
					}
				}
		);
	}

	private void drawBrain(Entity entity, BrainDebugData brainDebugData) {
		boolean bl = this.isTargeted(entity);
		int i = 0;
		GizmoDrawing.entityLabel(entity, i, brainDebugData.name(), -1, 0.48F);
		i++;
		if (bl) {
			GizmoDrawing.entityLabel(
					entity,
					i,
					brainDebugData.profession() + " " + brainDebugData.xp() + " xp",
					-1,
					DEFAULT_DRAWN_STRING_SIZE
			);
			i++;
		}

		if (bl) {
			int j = brainDebugData.health() < brainDebugData.maxHealth() ? -23296 : -1;
			GizmoDrawing.entityLabel(
					entity,
					i,
					"health: " + String.format(Locale.ROOT, "%.1f", brainDebugData.health()) + " / " + String.format(
							Locale.ROOT,
							"%.1f",
							brainDebugData.maxHealth()
					),
					j,
					DEFAULT_DRAWN_STRING_SIZE
			);
			i++;
		}

		if (bl && !brainDebugData.inventory().equals("")) {
			GizmoDrawing.entityLabel(entity, i, brainDebugData.inventory(), PINK, DEFAULT_DRAWN_STRING_SIZE);
			i++;
		}

		if (bl) {
			for (String string : brainDebugData.behaviors()) {
				GizmoDrawing.entityLabel(entity, i, string, AQUA, DEFAULT_DRAWN_STRING_SIZE);
				i++;
			}
		}

		if (bl) {
			for (String string : brainDebugData.activities()) {
				GizmoDrawing.entityLabel(entity, i, string, -16711936, DEFAULT_DRAWN_STRING_SIZE);
				i++;
			}
		}

		if (brainDebugData.wantsGolem()) {
			GizmoDrawing.entityLabel(entity, i, "Wants Golem", ORANGE, DEFAULT_DRAWN_STRING_SIZE);
			i++;
		}

		if (bl && brainDebugData.angerLevel() != -1) {
			GizmoDrawing.entityLabel(entity, i, "Anger Level: " + brainDebugData.angerLevel(), PINK, DEFAULT_DRAWN_STRING_SIZE);
			i++;
		}

		if (bl) {
			for (String string : brainDebugData.gossips()) {
				if (string.startsWith(brainDebugData.name())) {
					GizmoDrawing.entityLabel(entity, i, string, -1, DEFAULT_DRAWN_STRING_SIZE);
				}
				else {
					GizmoDrawing.entityLabel(entity, i, string, ORANGE, DEFAULT_DRAWN_STRING_SIZE);
				}

				i++;
			}
		}

		if (bl) {
			for (String string : Lists.reverse(brainDebugData.memories())) {
				GizmoDrawing.entityLabel(entity, i, string, GRAY, DEFAULT_DRAWN_STRING_SIZE);
				i++;
			}
		}
	}

	private boolean isTargeted(Entity entity) {
		return Objects.equals(this.targetedEntity, entity.getUuid());
	}

	public Map<BlockPos, List<String>> getGhostPointsOfInterest(DebugDataStore store) {
		Map<BlockPos, List<String>> map = Maps.newHashMap();
		store.forEachEntityData(
				DebugSubscriptionTypes.BRAINS, (entity, data) -> {
					for (BlockPos blockPos : Iterables.concat(data.pois(), data.potentialPois())) {
						map.computeIfAbsent(blockPos, pos -> Lists.newArrayList()).add(data.name());
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
