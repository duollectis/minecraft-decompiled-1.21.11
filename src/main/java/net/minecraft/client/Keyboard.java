package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.DebugOptionsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.GameModeSwitcherScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.*;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.c2s.play.ChangeGameModeC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.GameModeCommand;
import net.minecraft.server.command.VersionCommand;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.util.FeatureDebugLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Обработчик клавиатурных событий клиента.
 * Управляет горячими клавишами отладки, F3-комбинациями, скриншотами и полноэкранным режимом.
 */
@Environment(EnvType.CLIENT)
public class Keyboard {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Время удержания клавиши краша в миллисекундах до срабатывания. */
	public static final int DEBUG_CRASH_TIME = 10000;

	/** Код ошибки GLFW при недоступном формате буфера обмена — не является критической ошибкой. */
	private static final int GLFW_FORMAT_UNAVAILABLE = 65545;

	/** Минимальный интервал между лог-сообщениями при удержании клавиши краша (мс). */
	private static final long CRASH_LOG_INTERVAL_MS = 1000L;

	/** Задержка после последнего обновления привязки клавиш в KeybindsScreen (мс). */
	private static final long KEYBIND_UPDATE_DELAY_MS = 20L;

	private final MinecraftClient client;
	private final Clipboard clipboard = new Clipboard();
	private long debugCrashStartTime = -1L;
	private long debugCrashLastLogTime = -1L;
	private long debugCrashElapsedTime = -1L;
	private boolean switchF3State;

	public Keyboard(MinecraftClient client) {
		this.client = client;
	}

	private boolean processDebugKeys(KeyInput input) {
		switch (input.key()) {
			case InputUtil.GLFW_KEY_E:
				if (client.player == null) {
					return false;
				}

				boolean sectionPathsVisible = client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_SECTION_PATHS);
				debugLog("SectionPath: " + (sectionPathsVisible ? "shown" : "hidden"));
				return true;

			case InputUtil.GLFW_KEY_F:
				boolean fogEnabled = FogRenderer.toggleFog();
				debugLog("Fog: ", fogEnabled);
				return true;

			case InputUtil.GLFW_KEY_L:
				client.chunkCullingEnabled = !client.chunkCullingEnabled;
				debugLog("SmartCull: ", client.chunkCullingEnabled);
				return true;

			case InputUtil.GLFW_KEY_O:
				if (client.player == null) {
					return false;
				}

				boolean octreeVisible = client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_SECTION_OCTREE);
				debugLog("Frustum culling Octree: ", octreeVisible);
				return true;

			case InputUtil.GLFW_KEY_U:
				if (input.hasShift()) {
					client.worldRenderer.killFrustum();
					debugLog("Killed frustum");
				} else {
					client.worldRenderer.captureFrustum();
					debugLog("Captured frustum");
				}

				return true;

			case InputUtil.GLFW_KEY_V:
				if (client.player == null) {
					return false;
				}

				boolean sectionVisibilityEnabled = client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_SECTION_VISIBILITY);
				debugLog("SectionVisibility: ", sectionVisibilityEnabled);
				return true;

			case InputUtil.GLFW_KEY_W:
				client.wireFrame = !client.wireFrame;
				debugLog("WireFrame: ", client.wireFrame);
				return true;

			default:
				return false;
		}
	}

	private void debugLog(String message, boolean value) {
		debugLog(message + (value ? "enabled" : "disabled"));
	}

	private void sendMessage(Text message) {
		client.inGameHud.getChatHud().addMessage(message);
		client.getNarratorManager().narrateSystemMessage(message);
	}

	private static Text getDebugMessage(Formatting formatting, Text message) {
		return Text.empty()
			.append(Text.translatable("debug.prefix").formatted(formatting, Formatting.BOLD))
			.append(ScreenTexts.SPACE)
			.append(message);
	}

	private void debugError(Text message) {
		sendMessage(getDebugMessage(Formatting.RED, message));
	}

	private void debugLog(Text text) {
		sendMessage(getDebugMessage(Formatting.YELLOW, text));
	}

	private void debugLog(String key, Object... args) {
		debugLog(Text.translatable(key, args));
	}

	private void debugLog(String message) {
		debugLog(Text.literal(message));
	}

	/**
	 * Обрабатывает F3-комбинации клавиш.
	 * Возвращает {@code true}, если клавиша была обработана и не должна передаваться дальше.
	 *
	 * @param key событие нажатия клавиши
	 * @return {@code true} если клавиша обработана
	 */
	private boolean processF3(KeyInput key) {
		if (debugCrashStartTime > 0L && debugCrashStartTime < Util.getMeasuringTimeMs() - 100L) {
			return true;
		}

		if (SharedConstants.HOTKEYS && processDebugKeys(key)) {
			return true;
		}

		if (SharedConstants.FEATURE_COUNT) {
			switch (key.key()) {
				case InputUtil.GLFW_KEY_L -> {
					FeatureDebugLogger.dump();
					return true;
				}
				case InputUtil.GLFW_KEY_R -> {
					FeatureDebugLogger.clear();
					return true;
				}
			}
		}

		GameOptions gameOptions = client.options;
		boolean handled = false;

		if (gameOptions.debugReloadChunkKey.matchesKey(key)) {
			client.worldRenderer.reload();
			debugLog("debug.reload_chunks.message");
			handled = true;
		}

		if (gameOptions.debugShowHitboxesKey.matchesKey(key)
			&& client.player != null
			&& !client.player.hasReducedDebugInfo()
		) {
			boolean hitboxesVisible = client.debugHudEntryList.toggleVisibility(DebugHudEntries.ENTITY_HITBOXES);
			debugLog(hitboxesVisible ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
			handled = true;
		}

		if (gameOptions.debugClearChatKey.matchesKey(key)) {
			client.inGameHud.getChatHud().clear(false);
			handled = true;
		}

		if (gameOptions.debugShowChunkBordersKey.matchesKey(key)
			&& client.player != null
			&& !client.player.hasReducedDebugInfo()
		) {
			boolean chunkBordersVisible = client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_BORDERS);
			debugLog(chunkBordersVisible ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
			handled = true;
		}

		if (gameOptions.debugShowAdvancedTooltipsKey.matchesKey(key)) {
			gameOptions.advancedItemTooltips = !gameOptions.advancedItemTooltips;
			debugLog(gameOptions.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
			gameOptions.write();
			handled = true;
		}

		if (gameOptions.debugCopyRecreateCommandKey.matchesKey(key)) {
			if (client.player != null && !client.player.hasReducedDebugInfo()) {
				copyLookAt(
					client.player.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS),
					!key.hasShift()
				);
			}

			handled = true;
		}

		if (gameOptions.debugSpectateKey.matchesKey(key)) {
			if (client.player == null
				|| !GameModeCommand.PERMISSION_CHECK.allows(client.player.getPermissions())
			) {
				debugLog("debug.creative_spectator.error");
			} else if (!client.player.isSpectator()) {
				client.player.networkHandler.sendPacket(new ChangeGameModeC2SPacket(GameMode.SPECTATOR));
			} else {
				GameMode previousMode = MoreObjects.firstNonNull(
					client.interactionManager.getPreviousGameMode(),
					GameMode.CREATIVE
				);
				client.player.networkHandler.sendPacket(new ChangeGameModeC2SPacket(previousMode));
			}

			handled = true;
		}

		if (gameOptions.debugSwitchGameModeKey.matchesKey(key)
			&& client.world != null
			&& client.currentScreen == null
		) {
			if (client.canSwitchGameMode()
				&& GameModeCommand.PERMISSION_CHECK.allows(client.player.getPermissions())
			) {
				client.setScreen(new GameModeSwitcherScreen());
			} else {
				debugLog("debug.gamemodes.error");
			}

			handled = true;
		}

		if (gameOptions.debugOptionsKey.matchesKey(key)) {
			if (client.currentScreen instanceof DebugOptionsScreen) {
				client.currentScreen.close();
			} else if (client.canCurrentScreenInterruptOtherScreen()) {
				if (client.currentScreen != null) {
					client.currentScreen.close();
				}

				client.setScreen(new DebugOptionsScreen());
			}

			handled = true;
		}

		if (gameOptions.debugFocusPauseKey.matchesKey(key)) {
			gameOptions.pauseOnLostFocus = !gameOptions.pauseOnLostFocus;
			gameOptions.write();
			debugLog(gameOptions.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
			handled = true;
		}

		if (gameOptions.debugDumpDynamicTexturesKey.matchesKey(key)) {
			Path runPath = client.runDirectory.toPath().toAbsolutePath();
			Path debugTexturePath = TextureUtil.getDebugTexturePath(runPath);
			client.getTextureManager().dumpDynamicTextures(debugTexturePath);
			Text pathLink = Text.literal(runPath.relativize(debugTexturePath).toString())
				.formatted(Formatting.UNDERLINE)
				.styled(style -> style.withClickEvent(new ClickEvent.OpenFile(debugTexturePath)));
			debugLog(Text.translatable("debug.dump_dynamic_textures", pathLink));
			handled = true;
		}

		if (gameOptions.debugReloadResourcePacksKey.matchesKey(key)) {
			debugLog("debug.reload_resourcepacks.message");
			client.reloadResources();
			handled = true;
		}

		if (gameOptions.debugProfilingKey.matchesKey(key)) {
			if (client.toggleDebugProfiler(this::debugLog)) {
				debugLog(Text.translatable(
					"debug.profiling.start",
					10,
					gameOptions.debugModifierKey.getBoundKeyLocalizedText(),
					gameOptions.debugProfilingKey.getBoundKeyLocalizedText()
				));
			}

			handled = true;
		}

		if (gameOptions.debugCopyLocationKey.matchesKey(key)
			&& client.player != null
			&& !client.player.hasReducedDebugInfo()
		) {
			debugLog("debug.copy_location.message");
			setClipboard(String.format(
				Locale.ROOT,
				"/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
				client.player.getEntityWorld().getRegistryKey().getValue(),
				client.player.getX(),
				client.player.getY(),
				client.player.getZ(),
				client.player.getYaw(),
				client.player.getPitch()
			));
			handled = true;
		}

		if (gameOptions.debugDumpVersionKey.matchesKey(key)) {
			debugLog("debug.version.header");
			VersionCommand.acceptInfo(this::sendMessage);
			handled = true;
		}

		if (gameOptions.debugProfilingChartKey.matchesKey(key)) {
			client.getDebugHud().toggleRenderingChart();
			handled = true;
		}

		if (gameOptions.debugFpsChartsKey.matchesKey(key)) {
			client.getDebugHud().toggleRenderingAndTickCharts();
			handled = true;
		}

		if (gameOptions.debugNetworkChartsKey.matchesKey(key)) {
			client.getDebugHud().togglePacketSizeAndPingCharts();
			handled = true;
		}

		return handled;
	}

	private void copyLookAt(boolean hasQueryPermission, boolean queryServer) {
		HitResult hitResult = client.crosshairTarget;
		if (hitResult == null) {
			return;
		}

		switch (hitResult.getType()) {
			case BLOCK -> {
				BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
				World world = client.player.getEntityWorld();
				BlockState blockState = world.getBlockState(blockPos);

				if (hasQueryPermission) {
					if (queryServer) {
						client.player.networkHandler.getDataQueryHandler().queryBlockNbt(
							blockPos, nbt -> {
								copyBlock(blockState, blockPos, nbt);
								debugLog("debug.inspect.server.block");
							}
						);
					} else {
						BlockEntity blockEntity = world.getBlockEntity(blockPos);
						NbtCompound nbt = blockEntity != null
							? blockEntity.createNbt(world.getRegistryManager())
							: null;
						copyBlock(blockState, blockPos, nbt);
						debugLog("debug.inspect.client.block");
					}
				} else {
					copyBlock(blockState, blockPos, null);
					debugLog("debug.inspect.client.block");
				}
			}
			case ENTITY -> {
				Entity entity = ((EntityHitResult) hitResult).getEntity();
				Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());

				if (hasQueryPermission) {
					if (queryServer) {
						client.player.networkHandler.getDataQueryHandler().queryEntityNbt(
							entity.getId(), nbt -> {
								copyEntity(entityId, entity.getEntityPos(), nbt);
								debugLog("debug.inspect.server.entity");
							}
						);
					} else {
						try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
							entity.getErrorReporterContext(),
							LOGGER
						)) {
							NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
							entity.writeData(nbtWriteView);
							copyEntity(entityId, entity.getEntityPos(), nbtWriteView.getNbt());
						}

						debugLog("debug.inspect.client.entity");
					}
				} else {
					copyEntity(entityId, entity.getEntityPos(), null);
					debugLog("debug.inspect.client.entity");
				}
			}
		}
	}

	private void copyBlock(BlockState state, BlockPos pos, @Nullable NbtCompound nbt) {
		StringBuilder command = new StringBuilder(BlockArgumentParser.stringifyBlockState(state));
		if (nbt != null) {
			command.append(nbt);
		}

		setClipboard(String.format(
			Locale.ROOT,
			"/setblock %d %d %d %s",
			pos.getX(),
			pos.getY(),
			pos.getZ(),
			command
		));
	}

	private void copyEntity(Identifier id, Vec3d pos, @Nullable NbtCompound nbt) {
		String command;
		if (nbt != null) {
			nbt.remove("UUID");
			nbt.remove("Pos");
			String nbtString = NbtHelper.toPrettyPrintedText(nbt).getString();
			command = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", id, pos.x, pos.y, pos.z, nbtString);
		} else {
			command = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", id, pos.x, pos.y, pos.z);
		}

		setClipboard(command);
	}

	/**
	 * Обрабатывает низкоуровневое событие клавиши GLFW.
	 * Управляет инициацией краша, навигацией GUI, F3-комбинациями и привязками клавиш.
	 *
	 * @param window   дескриптор окна GLFW
	 * @param action   действие: 0 = отпустить, 1 = нажать, 2 = повтор
	 * @param input    данные о нажатой клавише
	 */
	private void onKey(long window, @KeyInput.KeyAction int action, KeyInput input) {
		Window clientWindow = client.getWindow();
		if (window != clientWindow.getHandle()) {
			return;
		}

		client.getInactivityFpsLimiter().onInput();
		GameOptions gameOptions = client.options;

		// F3 и debugOverlay могут быть привязаны к одной клавише
		boolean f3SameAsOverlay = gameOptions.debugModifierKey.boundKey.getCode()
			== gameOptions.debugOverlayKey.boundKey.getCode();
		boolean f3Pressed = gameOptions.debugModifierKey.isPressed();
		boolean crashKeyPressed = !gameOptions.debugCrashKey.isUnbound()
			&& InputUtil.isKeyPressed(clientWindow, gameOptions.debugCrashKey.boundKey.getCode());

		if (debugCrashStartTime > 0L) {
			if (!crashKeyPressed || !f3Pressed) {
				debugCrashStartTime = -1L;
			}
		} else if (crashKeyPressed && f3Pressed) {
			switchF3State = f3SameAsOverlay;
			debugCrashStartTime = Util.getMeasuringTimeMs();
			debugCrashLastLogTime = Util.getMeasuringTimeMs();
			debugCrashElapsedTime = 0L;
		}

		Screen screen = client.currentScreen;
		if (screen != null) {
			switch (input.key()) {
				case InputUtil.GLFW_KEY_TAB -> client.setNavigationType(GuiNavigationType.KEYBOARD_TAB);
				case InputUtil.GLFW_KEY_RIGHT, InputUtil.GLFW_KEY_LEFT,
					InputUtil.GLFW_KEY_DOWN, InputUtil.GLFW_KEY_UP ->
					client.setNavigationType(GuiNavigationType.KEYBOARD_ARROW);
			}
		}

		if (action == InputUtil.GLFW_PRESS
			&& !(client.currentScreen instanceof KeybindsScreen keybindsScreen
			&& keybindsScreen.lastKeyCodeUpdateTime > Util.getMeasuringTimeMs() - KEYBIND_UPDATE_DELAY_MS)
		) {
			if (gameOptions.fullscreenKey.matchesKey(input)) {
				clientWindow.toggleFullscreen();
				boolean isFullscreen = clientWindow.isFullscreen();
				gameOptions.getFullscreen().setValue(isFullscreen);
				gameOptions.write();

				if (client.currentScreen instanceof VideoOptionsScreen videoOptionsScreen) {
					videoOptionsScreen.updateFullscreenButtonValue(isFullscreen);
				}

				return;
			}

			if (gameOptions.screenshotKey.matchesKey(input)) {
				if (input.hasCtrlOrCmd() && SharedConstants.PANORAMA_SCREENSHOT) {
					sendMessage(client.takePanorama(client.runDirectory));
				} else {
					ScreenshotRecorder.saveScreenshot(
						client.runDirectory,
						client.getFramebuffer(),
						message -> client.execute(() -> sendMessage(message))
					);
				}

				return;
			}
		}

		if (action != InputUtil.GLFW_RELEASE) {
			boolean notTypingInField = screen == null
				|| !(screen.getFocused() instanceof TextFieldWidget textField)
				|| !textField.isActive();

			if (notTypingInField
				&& input.hasCtrlOrCmd()
				&& input.key() == InputUtil.GLFW_KEY_B
				&& client.getNarratorManager().isActive()
				&& gameOptions.getNarratorHotkey().getValue()
			) {
				boolean wasOff = gameOptions.getNarrator().getValue() == NarratorMode.OFF;
				gameOptions.getNarrator().setValue(
					NarratorMode.byId(gameOptions.getNarrator().getValue().getId() + 1)
				);
				gameOptions.write();

				if (screen != null) {
					screen.refreshNarrator(wasOff);
				}
			}
		}

		if (screen != null) {
			try {
				if (action == InputUtil.GLFW_PRESS || action == InputUtil.GLFW_REPEAT) {
					screen.applyKeyPressNarratorDelay();
					if (screen.keyPressed(input)) {
						if (client.currentScreen == null) {
							InputUtil.Key key = InputUtil.fromKeyCode(input);
							KeyBinding.setKeyPressed(key, false);
						}

						return;
					}
				} else if (action == InputUtil.GLFW_RELEASE) {
					if (screen.keyReleased(input)) {
						if (gameOptions.debugModifierKey.matchesKey(input)) {
							switchF3State = false;
						}

						return;
					}
				}
			} catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "keyPressed event handler");
				screen.addCrashReportSection(crashReport);
				CrashReportSection section = crashReport.addElement("Key");
				section.add("Key", input.key());
				section.add("Scancode", input.scancode());
				section.add("Mods", input.modifiers());
				throw new CrashException(crashReport);
			}
		}

		InputUtil.Key key = InputUtil.fromKeyCode(input);
		boolean noScreen = client.currentScreen == null;
		boolean gameOrMenuScreen = noScreen
			|| client.currentScreen instanceof GameMenuScreen gameMenuScreen && !gameMenuScreen.shouldShowMenu()
			|| client.currentScreen instanceof GameModeSwitcherScreen;

		if (f3SameAsOverlay && gameOptions.debugModifierKey.matchesKey(input) && action == InputUtil.GLFW_RELEASE) {
			if (switchF3State) {
				switchF3State = false;
			} else {
				client.debugHudEntryList.toggleF3Enabled();
			}
		} else if (!f3SameAsOverlay && gameOptions.debugOverlayKey.matchesKey(input) && action == InputUtil.GLFW_PRESS) {
			client.debugHudEntryList.toggleF3Enabled();
		}

		if (action == InputUtil.GLFW_RELEASE) {
			KeyBinding.setKeyPressed(key, false);
		} else {
			boolean f3Handled = false;

			if (gameOrMenuScreen && input.isEscape()) {
				client.openGameMenu(f3Pressed);
				f3Handled = f3Pressed;
			} else if (f3Pressed) {
				f3Handled = processF3(input);
				if (f3Handled && screen instanceof DebugOptionsScreen debugOptionsScreen) {
					DebugOptionsScreen.OptionsListWidget optionsListWidget = debugOptionsScreen.getOptionsListWidget();
					if (optionsListWidget != null) {
						optionsListWidget.children().forEach(DebugOptionsScreen.AbstractEntry::init);
					}
				}
			} else if (gameOrMenuScreen && gameOptions.toggleGuiKey.matchesKey(input)) {
				gameOptions.hudHidden = !gameOptions.hudHidden;
			} else if (gameOrMenuScreen && gameOptions.toggleSpectatorShaderEffectsKey.matchesKey(input)) {
				client.gameRenderer.togglePostProcessorEnabled();
			}

			if (f3SameAsOverlay) {
				switchF3State |= f3Handled;
			}

			if (client.getDebugHud().shouldShowRenderingChart() && !f3Pressed) {
				int digit = input.asNumber();
				if (digit != -1) {
					client.getDebugHud().getPieChart().select(digit);
				}
			}

			if (noScreen || key == gameOptions.debugModifierKey.boundKey) {
				if (f3Handled) {
					KeyBinding.setKeyPressed(key, false);
				} else {
					KeyBinding.setKeyPressed(key, true);
					KeyBinding.onKeyPressed(key);
				}
			}
		}
	}

	private void onChar(long window, CharInput input) {
		if (window != client.getWindow().getHandle()) {
			return;
		}

		Screen screen = client.currentScreen;
		if (screen == null || client.getOverlay() != null) {
			return;
		}

		try {
			screen.charTyped(input);
		} catch (Throwable throwable) {
			CrashReport crashReport = CrashReport.create(throwable, "charTyped event handler");
			screen.addCrashReportSection(crashReport);
			CrashReportSection section = crashReport.addElement("Key");
			section.add("Codepoint", input.codepoint());
			section.add("Mods", input.modifiers());
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Регистрирует GLFW-коллбэки клавиатуры и символьного ввода для указанного окна.
	 *
	 * @param window окно, для которого устанавливаются коллбэки
	 */
	public void setup(Window window) {
		InputUtil.setKeyboardCallbacks(
			window,
			(handle, keyCode, scancode, action, modifiers) -> {
				KeyInput keyInput = new KeyInput(keyCode, scancode, modifiers);
				client.execute(() -> onKey(handle, action, keyInput));
			},
			(handle, codePoint, modifiers) -> {
				CharInput charInput = new CharInput(codePoint, modifiers);
				client.execute(() -> onChar(handle, charInput));
			}
		);
	}

	public String getClipboard() {
		return clipboard.get(
			client.getWindow(),
			(error, description) -> {
				if (error != GLFW_FORMAT_UNAVAILABLE) {
					client.getWindow().logGlError(error, description);
				}
			}
		);
	}

	public void setClipboard(String text) {
		if (text.isEmpty()) {
			return;
		}

		clipboard.set(client.getWindow(), text);
	}

	/**
	 * Проверяет удержание клавиши отладочного краша и выводит предупреждения.
	 * По истечении {@link #DEBUG_CRASH_TIME} вызывает принудительный крэш JVM или игры.
	 */
	public void pollDebugCrash() {
		if (debugCrashStartTime <= 0L) {
			return;
		}

		long now = Util.getMeasuringTimeMs();
		long remaining = DEBUG_CRASH_TIME - (now - debugCrashStartTime);
		long sinceLastLog = now - debugCrashLastLogTime;

		if (remaining < 0L) {
			if (client.isCtrlPressed()) {
				GlfwUtil.makeJvmCrash();
			}

			CrashReport crashReport = new CrashReport(
				"Manually triggered debug crash",
				new Throwable("Manually triggered debug crash")
			);
			CrashReportSection section = crashReport.addElement("Manual crash details");
			WinNativeModuleUtil.addDetailTo(section);
			throw new CrashException(crashReport);
		}

		if (sinceLastLog >= CRASH_LOG_INTERVAL_MS) {
			if (debugCrashElapsedTime == 0L) {
				debugLog(
					"debug.crash.message",
					client.options.debugModifierKey.getBoundKeyLocalizedText().getString(),
					client.options.debugCrashKey.getBoundKeyLocalizedText().getString()
				);
			} else {
				debugError(Text.translatable("debug.crash.warning", MathHelper.ceil((float) remaining / 1000.0F)));
			}

			debugCrashLastLogTime = now;
			debugCrashElapsedTime++;
		}
	}
}
