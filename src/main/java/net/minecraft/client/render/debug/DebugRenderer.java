package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.debug.DebugDataStore;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Координатор всех отладочных рендереров клиента.
 * При изменении версии списка отладочных записей ({@code DebugHudEntries}) автоматически
 * пересоздаёт набор активных рендереров. Каждый рендерер активируется по соответствующему
 * флагу в {@code SharedConstants} или видимости записи в {@code DebugHudEntries}.
 */
@Environment(EnvType.CLIENT)
public class DebugRenderer {

	private final List<DebugRenderer.Renderer> renderers = new ArrayList<>();
	private long currentVersion;

	public DebugRenderer() {
		initRenderers();
	}

	/**
	 * Пересоздаёт список активных рендереров на основе текущих флагов отладки.
	 * Вызывается при инициализации и при изменении версии {@code DebugHudEntries}.
	 */
	public void initRenderers() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		renderers.clear();
		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.CHUNK_BORDERS)) {
			renderers.add(new ChunkBorderDebugRenderer(minecraftClient));
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.CHUNK_SECTION_OCTREE)) {
			renderers.add(new OctreeDebugRenderer(minecraftClient));
		}

		if (SharedConstants.PATHFINDING) {
			renderers.add(new PathfindingDebugRenderer());
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_WATER_LEVELS)) {
			renderers.add(new WaterDebugRenderer(minecraftClient));
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_HEIGHTMAP)) {
			renderers.add(new HeightmapDebugRenderer(minecraftClient));
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_COLLISION_BOXES)) {
			renderers.add(new CollisionDebugRenderer(minecraftClient));
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_ENTITY_SUPPORTING_BLOCKS)) {
			renderers.add(new SupportingBlockDebugRenderer(minecraftClient));
		}

		if (SharedConstants.NEIGHBORSUPDATE) {
			renderers.add(new NeighborUpdateDebugRenderer());
		}

		if (SharedConstants.EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER) {
			renderers.add(new RedstoneUpdateOrderDebugRenderer());
		}

		if (SharedConstants.STRUCTURES) {
			renderers.add(new StructureDebugRenderer());
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_BLOCK_LIGHT_LEVELS)
				|| minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_SKY_LIGHT_LEVELS)
		) {
			renderers.add(new SkyLightDebugRenderer(
					minecraftClient,
					minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_BLOCK_LIGHT_LEVELS),
					minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_SKY_LIGHT_LEVELS)
			));
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_SOLID_FACES)) {
			renderers.add(new BlockOutlineDebugRenderer(minecraftClient));
		}

		if (SharedConstants.VILLAGE_SECTIONS) {
			renderers.add(new VillageSectionsDebugRenderer());
		}

		if (SharedConstants.BRAIN) {
			renderers.add(new BrainDebugRenderer(minecraftClient));
		}

		if (SharedConstants.POI) {
			renderers.add(new PoiDebugRenderer(new BrainDebugRenderer(minecraftClient)));
		}

		if (SharedConstants.BEES) {
			renderers.add(new BeeDebugRenderer(minecraftClient));
		}

		if (SharedConstants.RAIDS) {
			renderers.add(new RaidCenterDebugRenderer(minecraftClient));
		}

		if (SharedConstants.GOAL_SELECTOR) {
			renderers.add(new GoalSelectorDebugRenderer(minecraftClient));
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
			renderers.add(new ChunkLoadingDebugRenderer(minecraftClient));
		}

		if (SharedConstants.GAME_EVENT_LISTENERS) {
			renderers.add(new GameEventDebugRenderer());
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_SKY_LIGHT_SECTIONS)) {
			renderers.add(new LightDebugRenderer(minecraftClient, LightType.SKY));
		}

		if (SharedConstants.BREEZE_MOB) {
			renderers.add(new BreezeDebugRenderer(minecraftClient));
		}

		if (SharedConstants.ENTITY_BLOCK_INTERSECTION) {
			renderers.add(new EntityBlockIntersectionsDebugRenderer());
		}

		if (minecraftClient.debugHudEntryList.isEntryVisible(DebugHudEntries.ENTITY_HITBOXES)) {
			renderers.add(new EntityHitboxDebugRenderer(minecraftClient));
		}

		renderers.add(new ChunkDebugRenderer(minecraftClient));
	}

	public void render(Frustum frustum, double cameraX, double cameraY, double cameraZ, float tickProgress) {
		MinecraftClient client = MinecraftClient.getInstance();
		DebugDataStore debugDataStore = client.getNetworkHandler().getDebugDataStore();

		if (client.debugHudEntryList.getVersion() != currentVersion) {
			currentVersion = client.debugHudEntryList.getVersion();
			initRenderers();
		}

		for (DebugRenderer.Renderer renderer : renderers) {
			renderer.render(cameraX, cameraY, cameraZ, debugDataStore, frustum, tickProgress);
		}
	}

	/**
		* Находит сущность, на которую смотрит указанная сущность, в радиусе {@code maxDistance} блоков.
		* Использует рейкаст по ограничивающим боксам всех сущностей в зоне видимости.
		*
		* @param entity наблюдатель (обычно игрок), может быть {@code null}
		* @param maxDistance максимальная дистанция поиска в блоках
		* @return найденная сущность или {@link Optional#empty()}
		*/
	public static Optional<Entity> getTargetedEntity(@Nullable Entity entity, int maxDistance) {
		if (entity == null) {
			return Optional.empty();
		}

		Vec3d eyePos = entity.getEyePos();
		Vec3d lookVec = entity.getRotationVec(1.0F).multiply(maxDistance);
		Vec3d endPos = eyePos.add(lookVec);
		Box searchBox = entity.getBoundingBox().stretch(lookVec).expand(1.0);
		int maxDistanceSq = maxDistance * maxDistance;
		EntityHitResult hit = ProjectileUtil.raycast(entity, eyePos, endPos, searchBox, EntityPredicates.CAN_HIT, maxDistanceSq);

		if (hit == null) {
			return Optional.empty();
		}

		return eyePos.squaredDistanceTo(hit.getPos()) > maxDistanceSq
				? Optional.empty()
				: Optional.of(hit.getEntity());
	}

	private static Vec3d hueToRgb(float hue) {
		// Алгоритм преобразования оттенка HSV в RGB без промежуточных структур
		float hueScaled = 5.99999F;
		int sector = (int) (MathHelper.clamp(hue, 0.0F, 1.0F) * hueScaled);
		float fraction = hue * hueScaled - sector;

		return switch (sector) {
			case 0 -> new Vec3d(1.0, fraction, 0.0);
			case 1 -> new Vec3d(1.0F - fraction, 1.0, 0.0);
			case 2 -> new Vec3d(0.0, 1.0, fraction);
			case 3 -> new Vec3d(0.0, 1.0 - fraction, 1.0);
			case 4 -> new Vec3d(fraction, 0.0, 1.0);
			case 5 -> new Vec3d(1.0, 0.0, 1.0 - fraction);
			default -> throw new IllegalStateException("Unexpected value: " + sector);
		};
	}

	private static Vec3d shiftHue(float r, float g, float b, float dHue) {
		Vec3d rComponent = hueToRgb(dHue).multiply(r);
		Vec3d gComponent = hueToRgb((dHue + 0.33333334F) % 1.0F).multiply(g);
		Vec3d bComponent = hueToRgb((dHue + 0.6666667F) % 1.0F).multiply(b);
		Vec3d combined = rComponent.add(gComponent).add(bComponent);
		double maxChannel = Math.max(Math.max(1.0, combined.x), Math.max(combined.y, combined.z));
		return new Vec3d(combined.x / maxChannel, combined.y / maxChannel, combined.z / maxChannel);
	}

	/** Интерфейс отладочного рендерера, вызываемого каждый кадр. */
	@Environment(EnvType.CLIENT)
	public interface Renderer {

		void render(
				double cameraX,
				double cameraY,
				double cameraZ,
				DebugDataStore store,
				Frustum frustum,
				float tickProgress
		);
	}
}
