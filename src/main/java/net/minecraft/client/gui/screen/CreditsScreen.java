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

@Environment(EnvType.CLIENT)
/**
 * {@code CreditsScreen}.
 */
public class CreditsScreen extends Screen {

	private static final Identifier VIGNETTE_TEXTURE = Identifier.ofVanilla("textures/misc/credits_vignette.png");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text SEPARATOR_LINE = Text.literal("============").formatted(Formatting.WHITE);
	private static final String CENTERED_LINE_PREFIX = "           ";
	private static final String
			OBFUSCATION_PLACEHOLDER =
			"" + Formatting.WHITE + Formatting.OBFUSCATED + Formatting.GREEN + Formatting.AQUA;
	private static final float SPACE_BAR_SPEED_MULTIPLIER = 5.0F;
	private static final float CTRL_KEY_SPEED_MULTIPLIER = 15.0F;
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
		if (!endCredits) {
			this.baseSpeed = 0.75F;
		}
		else {
			this.baseSpeed = 0.5F;
		}

		this.speedMultiplier = 1;
		this.speed = this.baseSpeed;
	}

	private float getSpeed() {
		return this.spaceKeyPressed
		       ? this.baseSpeed * (5.0F + this.pressedCtrlKeys.size() * 15.0F) * this.speedMultiplier
		       : this.baseSpeed * this.speedMultiplier;
	}

	@Override
	public void tick() {
		this.client.getMusicTracker().tick();
		this.client.getSoundManager().tick(false);
		float f = this.creditsHeight + this.height + this.height + 24;
		if (this.time > f) {
			this.closeScreen();
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isUp()) {
			this.speedMultiplier = -1;
		}
		else if (input.key() == 341 || input.key() == 345) {
			this.pressedCtrlKeys.add(input.key());
		}
		else if (input.key() == 32) {
			this.spaceKeyPressed = true;
		}

		this.speed = this.getSpeed();
		return super.keyPressed(input);
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		if (input.isUp()) {
			this.speedMultiplier = 1;
		}

		if (input.key() == 32) {
			this.spaceKeyPressed = false;
		}
		else if (input.key() == 341 || input.key() == 345) {
			this.pressedCtrlKeys.remove(input.key());
		}

		this.speed = this.getSpeed();
		return super.keyReleased(input);
	}

	@Override
	public void close() {
		this.closeScreen();
	}

	private void closeScreen() {
		this.finishAction.run();
	}

	@Override
	protected void init() {
		if (this.credits == null) {
			this.credits = Lists.newArrayList();
			this.narratedCredits = Lists.newArrayList();
			this.centeredLines = new IntOpenHashSet();
			if (this.endCredits) {
				this.load(END_POEM_TEXT_LOCATION, this::readPoem);
			}

			this.load(CREDITS_TEXT_LOCATION, this::readCredits);
			if (this.endCredits) {
				this.load(POST_CREDITS_TEXT_LOCATION, this::readPoem);
			}

			this.creditsHeight = this.credits.size() * 12;
		}
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(this.narratedCredits.toArray(Text[]::new));
	}

	private void load(Identifier fileLocation, CreditsScreen.CreditsReader reader) {
		try (Reader reader2 = this.client.getResourceManager().openAsReader(fileLocation)) {
			reader.read(reader2);
		}
		catch (Exception var8) {
			LOGGER.error("Couldn't load credits from file {}", fileLocation, var8);
		}
	}

	private void readPoem(Reader reader) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(reader);
		Random random = Random.create(8124371L);

		String string;
		while ((string = bufferedReader.readLine()) != null) {
			string = string.replaceAll("PLAYERNAME", this.client.getSession().getUsername());

			int i;
			while ((i = string.indexOf(OBFUSCATION_PLACEHOLDER)) != -1) {
				String string2 = string.substring(0, i);
				String string3 = string.substring(i + OBFUSCATION_PLACEHOLDER.length());
				string =
						string2 + Formatting.WHITE + Formatting.OBFUSCATED + "XXXXXXXX".substring(
								0,
								random.nextInt(4) + 3
						) + string3;
			}

			this.addText(string);
			this.addEmptyLine();
		}

		for (int i = 0; i < 8; i++) {
			this.addEmptyLine();
		}
	}

	private void readCredits(Reader reader) {
		for (JsonElement jsonElement : JsonHelper.deserializeArray(reader)) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			String string = jsonObject.get("section").getAsString();
			this.addText(SEPARATOR_LINE, true, false);
			this.addText(Text.literal(string).formatted(Formatting.YELLOW), true, true);
			this.addText(SEPARATOR_LINE, true, false);
			this.addEmptyLine();
			this.addEmptyLine();

			for (JsonElement jsonElement2 : jsonObject.getAsJsonArray("disciplines")) {
				JsonObject jsonObject2 = jsonElement2.getAsJsonObject();
				String string2 = jsonObject2.get("discipline").getAsString();
				if (StringUtils.isNotEmpty(string2)) {
					this.addText(Text.literal(string2).formatted(Formatting.YELLOW), true, true);
					this.addEmptyLine();
					this.addEmptyLine();
				}

				for (JsonElement jsonElement3 : jsonObject2.getAsJsonArray("titles")) {
					JsonObject jsonObject3 = jsonElement3.getAsJsonObject();
					String string3 = jsonObject3.get("title").getAsString();
					JsonArray jsonArray4 = jsonObject3.getAsJsonArray("names");
					this.addText(Text.literal(string3).formatted(Formatting.GRAY), false, true);

					for (JsonElement jsonElement4 : jsonArray4) {
						String string4 = jsonElement4.getAsString();
						this.addText(
								Text.literal("           ").append(string4).formatted(Formatting.WHITE),
								false,
								true
						);
					}

					this.addEmptyLine();
					this.addEmptyLine();
				}
			}
		}
	}

	private void addEmptyLine() {
		this.credits.add(OrderedText.EMPTY);
		this.narratedCredits.add(ScreenTexts.EMPTY);
	}

	private void addText(String text) {
		Text text2 = Text.literal(text);
		this.credits.addAll(this.client.textRenderer.wrapLines(text2, 256));
		this.narratedCredits.add(text2);
	}

	private void addText(Text text, boolean centered, boolean narrate) {
		if (centered) {
			this.centeredLines.add(this.credits.size());
		}

		this.credits.add(text.asOrderedText());
		if (narrate) {
			this.narratedCredits.add(text);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		this.renderVignette(context);
		this.time = Math.max(0.0F, this.time + deltaTicks * this.speed);
		int i = this.width / 2 - 128;
		int j = this.height + 50;
		float f = -this.time;
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(0.0F, f);
		context.createNewRootLayer();
		this.logoDrawer.draw(context, this.width, 1.0F, j);
		int k = j + 100;

		for (int l = 0; l < this.credits.size(); l++) {
			if (l == this.credits.size() - 1) {
				float g = k + f - (this.height / 2 - 6);
				if (g < 0.0F) {
					context.getMatrices().translate(0.0F, -g);
				}
			}

			if (k + f + 12.0F + 8.0F > 0.0F && k + f < this.height) {
				OrderedText orderedText = this.credits.get(l);
				if (this.centeredLines.contains(l)) {
					context.drawCenteredTextWithShadow(this.textRenderer, orderedText, i + 128, k, -1);
				}
				else {
					context.drawTextWithShadow(this.textRenderer, orderedText, i, k, -1);
				}
			}

			k += 12;
		}

		context.getMatrices().popMatrix();
	}

	private void renderVignette(DrawContext context) {
		context.drawTexture(
				RenderPipelines.VIGNETTE,
				VIGNETTE_TEXTURE,
				0,
				0,
				0.0F,
				0.0F,
				this.width,
				this.height,
				this.width,
				this.height
		);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.endCredits) {
			TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			AbstractTexture
					abstractTexture =
					textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE);
			AbstractTexture
					abstractTexture2 =
					textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE);
			TextureSetup textureSetup = TextureSetup.of(
					abstractTexture.getGlTextureView(),
					abstractTexture.getSampler(),
					abstractTexture2.getGlTextureView(),
					abstractTexture2.getSampler()
			);
			context.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, this.width, this.height);
		}
		else {
			super.renderBackground(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	protected void renderDarkening(DrawContext context, int x, int y, int width, int height) {
		float f = this.time * 0.5F;
		Screen.renderBackgroundTexture(context, Screen.MENU_BACKGROUND_TEXTURE, 0, 0, 0.0F, f, width, height);
	}

	@Override
	public boolean shouldPause() {
		return !this.endCredits;
	}

	@Override
	public boolean keepOpenThroughPortal() {
		return true;
	}

	@Override
	public void removed() {
		this.client.getMusicTracker().stop(MusicType.CREDITS);
	}

	@Override
	public MusicSound getMusic() {
		return MusicType.CREDITS;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code CreditsReader}.
	 */
	interface CreditsReader {

		void read(Reader reader) throws IOException;
	}
}
