package net.minecraft.client.gui.hud.bar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickManager;
import net.minecraft.world.waypoint.EntityTickProgress;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.minecraft.world.waypoint.Waypoint;

/**
 * Полоса локатора — отображает путевые точки (waypoints) в виде иконок
 * над стандартной полосой HUD. Иконки позиционируются по горизонтальному
 * углу между игроком и точкой (±60°), стрелки вверх/вниз указывают высоту.
 */
@Environment(EnvType.CLIENT)
public class LocatorBar implements Bar {

	private static final Identifier BACKGROUND = Identifier.ofVanilla("hud/locator_bar_background");
	private static final Identifier ARROW_UP = Identifier.ofVanilla("hud/locator_bar_arrow_up");
	private static final Identifier ARROW_DOWN = Identifier.ofVanilla("hud/locator_bar_arrow_down");

	/** Высота иконки путевой точки в пикселях. */
	private static final int WAYPOINT_ICON_SIZE = 9;

	/** Ширина стрелки направления по вертикали. */
	private static final int ARROW_WIDTH = 7;

	/** Высота стрелки направления по вертикали. */
	private static final int ARROW_HEIGHT = 5;

	/** Смещение иконки вверх относительно центра полосы. */
	private static final int ICON_Y_OFFSET = 2;

	/** Смещение стрелки вниз (ниже полосы). */
	private static final int ARROW_DOWN_Y_OFFSET = 6;

	/** Смещение стрелки вверх (выше полосы). */
	private static final int ARROW_UP_Y_OFFSET = -6;

	/** Горизонтальный смещение стрелки относительно иконки. */
	private static final int ARROW_X_OFFSET = 1;

	/** Максимальный угол обзора (±60°) для отображения путевых точек. */
	private static final double MAX_YAW_DEGREES = 60.0;

	/** Коэффициент перевода угла в пиксельное смещение (173 пикселя / 2 / 60°). */
	private static final double YAW_TO_PIXEL_FACTOR = 173.0 / 2.0 / MAX_YAW_DEGREES;

	/** Яркость цвета иконки, генерируемого из хэша UUID/имени. */
	private static final float GENERATED_COLOR_BRIGHTNESS = 0.9F;

	private final MinecraftClient client;

	public LocatorBar(MinecraftClient client) {
		this.client = client;
	}

	@Override
	public void renderBar(DrawContext context, RenderTickCounter tickCounter) {
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			BACKGROUND,
			getCenterX(client.getWindow()),
			getCenterY(client.getWindow()),
			WIDTH,
			HEIGHT
		);
	}

	@Override
	public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
		Entity cameraEntity = client.getCameraEntity();

		if (cameraEntity == null) {
			return;
		}

		World world = cameraEntity.getEntityWorld();
		TickManager tickManager = world.getTickManager();
		EntityTickProgress tickProgress = entityx -> tickCounter.getTickProgress(!tickManager.shouldSkipTick(entityx));
		int barY = getCenterY(client.getWindow());

		client.player
			.networkHandler
			.getWaypointHandler()
			.forEachWaypoint(cameraEntity, waypoint -> renderWaypoint(context, waypoint, cameraEntity, world, tickProgress, barY));
	}

	/**
	 * Рисует иконку одной путевой точки и стрелку вертикального направления.
	 * Пропускает точку, если она принадлежит самому игроку или выходит за угол ±60°.
	 */
	private void renderWaypoint(
		DrawContext context,
		TrackedWaypoint waypoint,
		Entity cameraEntity,
		World world,
		EntityTickProgress tickProgress,
		int barY
	) {
		boolean isOwnWaypoint = waypoint.getSource()
			.left()
			.map(uuid -> uuid.equals(cameraEntity.getUuid()))
			.orElse(false);

		if (isOwnWaypoint) {
			return;
		}

		double yaw = waypoint.getRelativeYaw(world, client.gameRenderer.getCamera(), tickProgress);

		if (yaw <= -MAX_YAW_DEGREES || yaw > MAX_YAW_DEGREES) {
			return;
		}

		int centerX = MathHelper.ceil((context.getScaledWindowWidth() - WAYPOINT_ICON_SIZE) / 2.0F);
		int pixelOffset = MathHelper.floor(yaw * YAW_TO_PIXEL_FACTOR);
		int iconX = centerX + pixelOffset;

		Waypoint.Config config = waypoint.getConfig();
		WaypointStyleAsset styleAsset = client.getWaypointStyleAssetManager().get(config.style);
		float distance = MathHelper.sqrt((float) waypoint.squaredDistanceTo(cameraEntity));
		Identifier sprite = styleAsset.getSpriteForDistance(distance);
		int color = config.color.orElseGet(() -> generateColorFromSource(waypoint));

		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			sprite,
			iconX,
			barY - ICON_Y_OFFSET,
			WAYPOINT_ICON_SIZE,
			WAYPOINT_ICON_SIZE,
			color
		);

		TrackedWaypoint.Pitch pitch = waypoint.getPitch(world, client.gameRenderer, tickProgress);

		if (pitch == TrackedWaypoint.Pitch.NONE) {
			return;
		}

		boolean isDown = pitch == TrackedWaypoint.Pitch.DOWN;
		Identifier arrowSprite = isDown ? ARROW_DOWN : ARROW_UP;
		int arrowYOffset = isDown ? ARROW_DOWN_Y_OFFSET : ARROW_UP_Y_OFFSET;

		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			arrowSprite,
			iconX + ARROW_X_OFFSET,
			barY + arrowYOffset,
			ARROW_WIDTH,
			ARROW_HEIGHT
		);
	}

	/** Генерирует цвет иконки из хэша UUID или имени источника путевой точки. */
	private int generateColorFromSource(TrackedWaypoint waypoint) {
		return (Integer) waypoint.getSource()
			.map(
				uuid -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, uuid.hashCode()), GENERATED_COLOR_BRIGHTNESS),
				name -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, name.hashCode()), GENERATED_COLOR_BRIGHTNESS)
			);
	}
}
