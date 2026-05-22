package net.minecraft.client.gui.screen.world;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.updater.WorldUpdater;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.ToIntFunction;

/**
 * Экран оптимизации (обновления) мира до текущей версии.
 * Отображает прогресс обновления чанков по измерениям с цветовой индикацией.
 */
@Environment(EnvType.CLIENT)
public class OptimizeWorldScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int TITLE_Y = 20;
	private static final int INFO_Y = 40;
	private static final int INFO_LINE_SPACING = 12;
	private static final int COLOR_OVERWORLD = -13408734;
	private static final int COLOR_NETHER = -10075085;
	private static final int COLOR_END = -8943531;
	private static final int COLOR_DEFAULT = -2236963;
	private static final ToIntFunction<RegistryKey<World>> DIMENSION_COLORS = Util.make(
			new Reference2IntOpenHashMap(), map -> {
				map.put(World.OVERWORLD, COLOR_OVERWORLD);
				map.put(World.NETHER, COLOR_NETHER);
				map.put(World.END, COLOR_END);
				map.defaultReturnValue(COLOR_DEFAULT);
			}
	);

	private final BooleanConsumer callback;
	private final WorldUpdater updater;

	public static @Nullable OptimizeWorldScreen create(
			MinecraftClient client,
			BooleanConsumer callback,
			DataFixer dataFixer,
			LevelStorage.Session storageSession,
			boolean eraseCache
	) {
		try {
			IntegratedServerLoader serverLoader = client.createIntegratedServerLoader();
			ResourcePackManager resourcePackManager = VanillaDataPackProvider.createManager(storageSession);

			try (SaveLoader saveLoader = serverLoader.load(
					storageSession.readLevelProperties(),
					false,
					resourcePackManager
			)) {
				SaveProperties saveProperties = saveLoader.saveProperties();
				DynamicRegistryManager.Immutable registries = saveLoader
						.combinedDynamicRegistries()
						.getCombinedRegistryManager();
				storageSession.backupLevelDataFile(registries, saveProperties);

				return new OptimizeWorldScreen(
						callback, dataFixer, storageSession, saveProperties, eraseCache, registries
				);
			}
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to load datapacks, can't optimize world", exception);
			return null;
		}
	}

	private OptimizeWorldScreen(
			BooleanConsumer callback,
			DataFixer dataFixer,
			LevelStorage.Session storageSession,
			SaveProperties saveProperties,
			boolean eraseCache,
			DynamicRegistryManager registries
	) {
		super(Text.translatable("optimizeWorld.title", saveProperties.getLevelInfo().getLevelName()));
		this.callback = callback;
		this.updater = new WorldUpdater(storageSession, dataFixer, saveProperties, registries, eraseCache, false);
	}

	@Override
	protected void init() {
		super.init();
		addDrawableChild(ButtonWidget.builder(
				ScreenTexts.CANCEL, button -> {
					updater.cancel();
					callback.accept(false);
				}
		).dimensions(width / 2 - 100, height / 4 + 150, 200, 20).build());
	}

	@Override
	public void tick() {
		if (updater.isDone()) {
			callback.accept(true);
		}
	}

	@Override
	public void close() {
		callback.accept(false);
	}

	@Override
	public void removed() {
		updater.cancel();
		updater.close();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);

		int barLeft = width / 2 - 150;
		int barRight = width / 2 + 150;
		int barTop = height / 4 + 100;
		int barBottom = barTop + 10;

		context.drawCenteredTextWithShadow(
				textRenderer,
				updater.getStatus(),
				width / 2,
				barTop - 9 - 2,
				-6250336
		);

		if (updater.getTotalChunkCount() > 0) {
			context.fill(barLeft - 1, barTop - 1, barRight + 1, barBottom + 1, -16777216);

			context.drawTextWithShadow(
					textRenderer,
					Text.translatable("optimizeWorld.info.converted", updater.getUpgradedChunkCount()),
					barLeft,
					INFO_Y,
					-6250336
			);
			context.drawTextWithShadow(
					textRenderer,
					Text.translatable("optimizeWorld.info.skipped", updater.getSkippedChunkCount()),
					barLeft,
					INFO_Y + INFO_LINE_SPACING,
					-6250336
			);
			context.drawTextWithShadow(
					textRenderer,
					Text.translatable("optimizeWorld.info.total", updater.getTotalChunkCount()),
					barLeft,
					INFO_Y + INFO_LINE_SPACING * 2,
					-6250336
			);

			int barOffset = 0;
			for (RegistryKey<World> dimension : updater.getWorlds()) {
				int segmentWidth = MathHelper.floor(updater.getProgress(dimension) * (barRight - barLeft));
				context.fill(barLeft + barOffset, barTop, barLeft + barOffset + segmentWidth, barBottom, DIMENSION_COLORS.applyAsInt(dimension));
				barOffset += segmentWidth;
			}

			int processedChunks = updater.getUpgradedChunkCount() + updater.getSkippedChunkCount();
			Text counterText = Text.translatable("optimizeWorld.progress.counter", processedChunks, updater.getTotalChunkCount());
			Text percentText = Text.translatable(
					"optimizeWorld.progress.percentage",
					MathHelper.floor(updater.getProgress() * 100.0F)
			);

			context.drawCenteredTextWithShadow(textRenderer, counterText, width / 2, barTop + 2 * 9 + 2, -6250336);
			context.drawCenteredTextWithShadow(
					textRenderer,
					percentText,
					width / 2,
					barTop + (barBottom - barTop) / 2 - 9 / 2,
					-6250336
			);
		}
	}
}
