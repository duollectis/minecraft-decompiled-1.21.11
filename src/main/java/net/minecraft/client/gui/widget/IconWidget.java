package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code IconWidget}.
 */
public abstract class IconWidget extends ClickableWidget {

	IconWidget(int x, int y, int width, int height) {
		super(x, y, width, height, ScreenTexts.EMPTY);
	}

	/**
	 * Create.
	 *
	 * @param width width
	 * @param height height
	 * @param texture texture
	 * @param textureWidth texture width
	 * @param textureHeight texture height
	 *
	 * @return IconWidget — результат операции
	 */
	public static IconWidget create(int width, int height, Identifier texture, int textureWidth, int textureHeight) {
		return new IconWidget.Texture(0, 0, width, height, texture, textureWidth, textureHeight);
	}

	/**
	 * Create.
	 *
	 * @param width width
	 * @param height height
	 * @param texture texture
	 *
	 * @return IconWidget — результат операции
	 */
	public static IconWidget create(int width, int height, Identifier texture) {
		return new IconWidget.Simple(0, 0, width, height, texture);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public boolean isInteractable() {
		return false;
	}

	public abstract void setTexture(Identifier texture);

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		return null;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Simple}.
	 */
	static class Simple extends IconWidget {

		private Identifier texture;

		public Simple(int x, int y, int width, int height, Identifier texture) {
			super(x, y, width, height);
			this.texture = texture;
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					this.texture,
					this.getX(),
					this.getY(),
					this.getWidth(),
					this.getHeight()
			);
		}

		@Override
		public void setTexture(Identifier texture) {
			this.texture = texture;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Texture}.
	 */
	static class Texture extends IconWidget {

		private Identifier texture;
		private final int textureWidth;
		private final int textureHeight;

		public Texture(int x, int y, int width, int height, Identifier texture, int textureWidth, int textureHeight) {
			super(x, y, width, height);
			this.texture = texture;
			this.textureWidth = textureWidth;
			this.textureHeight = textureHeight;
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					this.texture,
					this.getX(),
					this.getY(),
					0.0F,
					0.0F,
					this.getWidth(),
					this.getHeight(),
					this.textureWidth,
					this.textureHeight
			);
		}

		@Override
		public void setTexture(Identifier texture) {
			this.texture = texture;
		}
	}
}
