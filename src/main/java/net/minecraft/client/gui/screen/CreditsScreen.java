package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.block.entity.AbstractEndPortalBlockEntityRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.MusicType;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Экран финальных титров и поэмы конца игры.
 * Поддерживает ускорение прокрутки пробелом и Ctrl, а также обратную прокрутку стрелкой вверх.
 */
@Environment(EnvType.CLIENT)
public class CreditsScreen extends Screen {

	private static final Identifier VIGNETTE_TEXTURE = Identifier.ofVanilla("textures/misc/credits_vignette.png");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text SEPARATOR_LINE = Text.literal("============").formatted(Formatting.WHITE);
	private static final String CENTERED_LINE_PREFIX = "           ";
	private static final String OBFUSCATION_PLACEHOLDER =
			"" + Formatting.WHITE + Formatting.OBFUSCATED + Formatting.GREEN + Formatting.AQUA;
	private static final float SPACE_BAR_SPEED_MULTIPLIER = 5.0F;
	private static final float CTRL_KEY_SPEED_MULTIPLIER = 15.0F;
	private static final int KEY_LEFT_CTRL = 341;
	private static final int KEY_RIGHT_CTRL = 345;
	private static final int KEY_SPACE = 32;
	private static final int CREDITS_LINE_HEIGHT = 12;
	private static final int CREDITS_WRAP_WIDTH = 256;
	private static final int LOGO_OFFSET_Y = 100;
	private static final int CREDITS_BOTTOM_PADDING = 24;
	private static final Identifier END_POEM_TEXT_LOCATION = Identifier.ofVanilla("texts/end.txt");
	private static final Identifier CREDITS_TEXT_LOCATION = Identifier.ofVanilla("texts/credits.json");
	private static final Identifier POST_CREDITS_TEXT_LOCATION = Identifier.ofVanilla("texts/postcredits.txt");
	private final boolean endCredits;
	private final Runnable finishAction;
	private float time;
	private List<OrderedText> credits;
	private List<Text> narratedCredits;
	private IntSet centeredLines;
	private int creditsHeight;
	private boolean spaceKeyPressed;
	private final IntSet pressedCtrlKeys = new IntOpenHashSet();
	private float speed;
	private final float baseSpeed;
	private int speedMultiplier;
	private final LogoDrawer logoDrawer = new LogoDrawer(false);

	public CreditsScreen(boolean endCredits, Runnable finishAction) {
		super(NarratorManager.EMPTY);
		this.endCredits = endCredits;
		this.finishAction = finishAction;
		baseSpeed = endCredits ? 0.5F : 0.75F;
		speedMultiplier = 1;
		speed = baseSpeed;
	}

	private float getSpeed() {
		return spaceKeyPressed
				? baseSpeed * (5.0F + pressedCtrlKeys.size() * CTRL_KEY_SPEED_MULTIPLIER) * speedMultiplier
				: baseSpeed * speedMultiplier;
	}

	@Override
	public void tick() {
		client.getMusicTracker().tick();
		client.getSoundManager().tick(false);
		float totalHeight = creditsHeight + height + height + CREDITS_BOTTOM_PADDING;

		if (time > totalHeight) {
			closeScreen();
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isUp()) {
			speedMultiplier = -1;
		} else if (input.key() == KEY_LEFT_CTRL || input.key() == KEY_RIGHT_CTRL) {
			pressedCtrlKeys.add(input.key());
		} else if (input.key() == KEY_SPACE) {
			spaceKeyPressed = true;
		}

		speed = getSpeed();
		return super.keyPressed(input);
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		if (input.isUp()) {
			speedMultiplier = 1;
		}

		if (input.key() == KEY_SPACE) {
			spaceKeyPressed = false;
		} else if (input.key() == KEY_LEFT_CTRL || input.key() == KEY_RIGHT_CTRL) {
			pressedCtrlKeys.remove(input.key());
		}

		speed = getSpeed();
		return super.keyReleased(input);
	}

	@Override
	public void close() {
		closeScreen();
	}

	private void closeScreen() {
		finishAction.run();
	}

	@Override
	protected void init() {
		if (credits != null) {
			return;
		}

		credits = Lists.newArrayList();
		narratedCredits = Lists.newArrayList();
		centeredLines = new IntOpenHashSet();

		if (endCredits) {
			load(END_POEM_TEXT_LOCATION, this::readPoem);
		}

		load(CREDITS_TEXT_LOCATION, this::readCredits);

		if (endCredits) {
			load(POST_CREDITS_TEXT_LOCATION, this::readPoem);
		}

		creditsHeight = credits.size() * CREDITS_LINE_HEIGHT;
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(narratedCredits.toArray(Text[]::new));
	}

	private void load(Identifier fileLocation, CreditsReader reader) {
		try (Reader fileReader = client.getResourceManager().openAsReader(fileLocation)) {
			reader.read(fileReader);
		} catch (Exception exception) {
			LOGGER.error("Couldn't load credits from file {}", fileLocation, exception);
		}
	}

	private void readPoem(Reader reader) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(reader);
		Random random = Random.create(8124371L);
		String line;

		while ((line = bufferedReader.readLine()) != null) {
			line = line.replaceAll("PLAYERNAME", client.getSession().getUsername());
			int placeholderIndex;

			while ((placeholderIndex = line.indexOf(OBFUSCATION_PLACEHOLDER)) != -1) {
				String before = line.substring(0, placeholderIndex);
				String after = line.substring(placeholderIndex + OBFUSCATION_PLACEHOLDER.length());
				line = before + Formatting.WHITE + Formatting.OBFUSCATED
						+ "XXXXXXXX".substring(0, random.nextInt(4) + 3) + after;
			}

			addText(line);
			addEmptyLine();
		}

		for (int padding = 0; padding < 8; padding++) {
			addEmptyLine();
		}
	}

	private void readCredits(Reader reader) {
		for (JsonElement sectionElement : JsonHelper.deserializeArray(reader)) {
			JsonObject sectionObj = sectionElement.getAsJsonObject();
			String sectionName = sectionObj.get("section").getAsString();
			addText(SEPARATOR_LINE, true, false);
			addText(Text.literal(sectionName).formatted(Formatting.YELLOW), true, true);
			addText(SEPARATOR_LINE, true, false);
			addEmptyLine();
			addEmptyLine();

			for (JsonElement disciplineElement : sectionObj.getAsJsonArray("disciplines")) {
				JsonObject disciplineObj = disciplineElement.getAsJsonObject();
				String disciplineName = disciplineObj.get("discipline").getAsString();

				if (StringUtils.isNotEmpty(disciplineName)) {
					addText(Text.literal(disciplineName).formatted(Formatting.YELLOW), true, true);
					addEmptyLine();
					addEmptyLine();
				}

				for (JsonElement titleElement : disciplineObj.getAsJsonArray("titles")) {
					JsonObject titleObj = titleElement.getAsJsonObject();
					String titleName = titleObj.get("title").getAsString();
					JsonArray namesArray = titleObj.getAsJsonArray("names");
					addText(Text.literal(titleName).formatted(Formatting.GRAY), false, true);

					for (JsonElement nameElement : namesArray) {
						addText(
								Text.literal(CENTERED_LINE_PREFIX).append(nameElement.getAsString()).formatted(Formatting.WHITE),
								false,
								true
						);
					}

					addEmptyLine();
					addEmptyLine();
				}
			}
		}
	}

	private void addEmptyLine() {
		credits.add(OrderedText.EMPTY);
		narratedCredits.add(ScreenTexts.EMPTY);
	}

	private void addText(String text) {
		Text textObj = Text.literal(text);
		credits.addAll(client.textRenderer.wrapLines(textObj, CREDITS_WRAP_WIDTH));
		narratedCredits.add(textObj);
	}

	private void addText(Text text, boolean centered, boolean narrate) {
		if (centered) {
			centeredLines.add(credits.size());
		}

		credits.add(text.asOrderedText());

		if (narrate) {
			narratedCredits.add(text);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		renderVignette(context);
		time = Math.max(0.0F, time + deltaTicks * speed);
		int centerX = width / 2 - 128;
		int logoY = height + 50;
		float scrollOffset = -time;
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(0.0F, scrollOffset);
		context.createNewRootLayer();
		logoDrawer.draw(context, width, 1.0F, logoY);
		int lineY = logoY + LOGO_OFFSET_Y;

		for (int lineIndex = 0; lineIndex < credits.size(); lineIndex++) {
			if (lineIndex == credits.size() - 1) {
				float lastLineOffset = lineY + scrollOffset - (height / 2 - 6);

				if (lastLineOffset < 0.0F) {
					context.getMatrices().translate(0.0F, -lastLineOffset);
				}
			}

			if (lineY + scrollOffset + CREDITS_LINE_HEIGHT + 8.0F > 0.0F && lineY + scrollOffset < height) {
				OrderedText line = credits.get(lineIndex);

				if (centeredLines.contains(lineIndex)) {
					context.drawCenteredTextWithShadow(textRenderer, line, centerX + 128, lineY, -1);
				} else {
					context.drawTextWithShadow(textRenderer, line, centerX, lineY, -1);
				}
			}

			lineY += CREDITS_LINE_HEIGHT;
		}

		context.getMatrices().popMatrix();
	}

	private void renderVignette(DrawContext context) {
		context.drawTexture(RenderPipelines.VIGNETTE, VIGNETTE_TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (endCredits) {
			TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			AbstractTexture skyTexture = textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE);
			AbstractTexture portalTexture = textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE);
			TextureSetup textureSetup = TextureSetup.of(
					skyTexture.getGlTextureView(),
					skyTexture.getSampler(),
					portalTexture.getGlTextureView(),
					portalTexture.getSampler()
			);
			context.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, width, height);
		} else {
			super.renderBackground(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	protected void renderDarkening(DrawContext context, int x, int y, int width, int height) {
		float scrollProgress = time * 0.5F;
		Screen.renderBackgroundTexture(context, Screen.MENU_BACKGROUND_TEXTURE, 0, 0, 0.0F, scrollProgress, width, height);
	}

	@Override
	public boolean shouldPause() {
		return !endCredits;
	}

	@Override
	public boolean keepOpenThroughPortal() {
		return true;
	}

	@Override
	public void removed() {
		client.getMusicTracker().stop(MusicType.CREDITS);
	}

	@Override
	public MusicSound getMusic() {
		return MusicType.CREDITS;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface CreditsReader {

		void read(Reader reader) throws IOException;
	}
}
