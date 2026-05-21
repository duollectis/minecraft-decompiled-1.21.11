package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code EnchantmentScreen}.
 */
public class EnchantmentScreen extends HandledScreen<EnchantmentScreenHandler> {

	private static final Identifier[] LEVEL_TEXTURES = new Identifier[]{
			Identifier.ofVanilla("container/enchanting_table/level_1"),
			Identifier.ofVanilla("container/enchanting_table/level_2"),
			Identifier.ofVanilla("container/enchanting_table/level_3")
	};
	private static final Identifier[] LEVEL_DISABLED_TEXTURES = new Identifier[]{
			Identifier.ofVanilla("container/enchanting_table/level_1_disabled"),
			Identifier.ofVanilla("container/enchanting_table/level_2_disabled"),
			Identifier.ofVanilla("container/enchanting_table/level_3_disabled")
	};
	private static final Identifier
			ENCHANTMENT_SLOT_DISABLED_TEXTURE =
			Identifier.ofVanilla("container/enchanting_table/enchantment_slot_disabled");
	private static final Identifier
			ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE =
			Identifier.ofVanilla("container/enchanting_table/enchantment_slot_highlighted");
	private static final Identifier
			ENCHANTMENT_SLOT_TEXTURE =
			Identifier.ofVanilla("container/enchanting_table/enchantment_slot");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/enchanting_table.png");
	private static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/entity/enchanting_table_book.png");
	private final Random random = Random.create();
	private BookModel BOOK_MODEL;
	public float nextPageAngle;
	public float pageAngle;
	public float approximatePageAngle;
	public float pageRotationSpeed;
	public float nextPageTurningSpeed;
	public float pageTurningSpeed;
	private ItemStack stack = ItemStack.EMPTY;

	public EnchantmentScreen(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		this.BOOK_MODEL = new BookModel(this.client.getLoadedEntityModels().getModelPart(EntityModelLayers.BOOK));
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		this.client.player.experienceBarDisplayStartTime = this.client.player.age;
		this.doTick();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		int i = (this.width - this.backgroundWidth) / 2;
		int j = (this.height - this.backgroundHeight) / 2;

		for (int k = 0; k < 3; k++) {
			double d = click.x() - (i + 60);
			double e = click.y() - (j + 14 + 19 * k);
			if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0 && this.handler.onButtonClick(this.client.player, k)) {
				this.client.interactionManager.clickButton(this.handler.syncId, k);
				return true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int i = (this.width - this.backgroundWidth) / 2;
		int j = (this.height - this.backgroundHeight) / 2;
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE,
				i,
				j,
				0.0F,
				0.0F,
				this.backgroundWidth,
				this.backgroundHeight,
				256,
				256
		);
		this.drawBook(context, i, j);
		EnchantingPhrases.getInstance().setSeed(this.handler.getSeed());
		int k = this.handler.getLapisCount();

		for (int l = 0; l < 3; l++) {
			int m = i + 60;
			int n = m + 20;
			int o = this.handler.enchantmentPower[l];
			if (o == 0) {
				context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						ENCHANTMENT_SLOT_DISABLED_TEXTURE,
						m,
						j + 14 + 19 * l,
						108,
						19
				);
			}
			else {
				String string = o + "";
				int p = 86 - this.textRenderer.getWidth(string);
				StringVisitable stringVisitable = EnchantingPhrases.getInstance().generatePhrase(this.textRenderer, p);
				int q = -9937334;
				if ((k < l + 1 || this.client.player.experienceLevel < o) && !this.client.player.isInCreativeMode()) {
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							ENCHANTMENT_SLOT_DISABLED_TEXTURE,
							m,
							j + 14 + 19 * l,
							108,
							19
					);
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							LEVEL_DISABLED_TEXTURES[l],
							m + 1,
							j + 15 + 19 * l,
							16,
							16
					);
					context.drawWrappedText(
							this.textRenderer,
							stringVisitable,
							n,
							j + 16 + 19 * l,
							p,
							ColorHelper.fullAlpha((q & 16711422) >> 1),
							false
					);
					q = -12550384;
				}
				else {
					int r = mouseX - (i + 60);
					int s = mouseY - (j + 14 + 19 * l);
					if (r >= 0 && s >= 0 && r < 108 && s < 19) {
						context.drawGuiTexture(
								RenderPipelines.GUI_TEXTURED,
								ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE,
								m,
								j + 14 + 19 * l,
								108,
								19
						);
						context.setCursor(StandardCursors.POINTING_HAND);
						q = -128;
					}
					else {
						context.drawGuiTexture(
								RenderPipelines.GUI_TEXTURED,
								ENCHANTMENT_SLOT_TEXTURE,
								m,
								j + 14 + 19 * l,
								108,
								19
						);
					}

					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							LEVEL_TEXTURES[l],
							m + 1,
							j + 15 + 19 * l,
							16,
							16
					);
					context.drawWrappedText(this.textRenderer, stringVisitable, n, j + 16 + 19 * l, p, q, false);
					q = -8323296;
				}

				context.drawTextWithShadow(
						this.textRenderer,
						string,
						n + 86 - this.textRenderer.getWidth(string),
						j + 16 + 19 * l + 7,
						q
				);
			}
		}
	}

	private void drawBook(DrawContext context, int x, int y) {
		float f = this.client.getRenderTickCounter().getTickProgress(false);
		float g = MathHelper.lerp(f, this.pageTurningSpeed, this.nextPageTurningSpeed);
		float h = MathHelper.lerp(f, this.pageAngle, this.nextPageAngle);
		int i = x + 14;
		int j = y + 14;
		int k = i + 38;
		int l = j + 31;
		context.addBookModel(this.BOOK_MODEL, BOOK_TEXTURE, 40.0F, g, h, i, j, k, l);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		float f = this.client.getRenderTickCounter().getTickProgress(false);
		super.render(context, mouseX, mouseY, f);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
		boolean bl = this.client.player.isInCreativeMode();
		int i = this.handler.getLapisCount();

		for (int j = 0; j < 3; j++) {
			int k = this.handler.enchantmentPower[j];
			Optional<RegistryEntry.Reference<Enchantment>> optional = this.client
					.world
					.getRegistryManager()
					.getOrThrow(RegistryKeys.ENCHANTMENT)
					.getEntry(this.handler.enchantmentId[j]);
			if (!optional.isEmpty()) {
				int l = this.handler.enchantmentLevel[j];
				int m = j + 1;
				if (this.isPointWithinBounds(60, 14 + 19 * j, 108, 17, mouseX, mouseY) && k > 0 && l >= 0) {
					List<Text> list = Lists.newArrayList();
					list.add(Text
							.translatable("container.enchant.clue", Enchantment.getName(optional.get(), l))
							.formatted(Formatting.WHITE));
					if (!bl) {
						list.add(ScreenTexts.EMPTY);
						if (this.client.player.experienceLevel < k) {
							list.add(Text
									.translatable(
											"container.enchant.level.requirement",
											this.handler.enchantmentPower[j]
									)
									.formatted(Formatting.RED));
						}
						else {
							MutableText mutableText;
							if (m == 1) {
								mutableText = Text.translatable("container.enchant.lapis.one");
							}
							else {
								mutableText = Text.translatable("container.enchant.lapis.many", m);
							}

							list.add(mutableText.formatted(i >= m ? Formatting.GRAY : Formatting.RED));
							MutableText mutableText2;
							if (m == 1) {
								mutableText2 = Text.translatable("container.enchant.level.one");
							}
							else {
								mutableText2 = Text.translatable("container.enchant.level.many", m);
							}

							list.add(mutableText2.formatted(Formatting.GRAY));
						}
					}

					context.drawTooltip(this.textRenderer, list, mouseX, mouseY);
					break;
				}
			}
		}
	}

	public void doTick() {
		ItemStack itemStack = this.handler.getSlot(0).getStack();
		if (!ItemStack.areEqual(itemStack, this.stack)) {
			this.stack = itemStack;

			do {
				this.approximatePageAngle =
						this.approximatePageAngle + (this.random.nextInt(4) - this.random.nextInt(4));
			}
			while (this.nextPageAngle <= this.approximatePageAngle + 1.0F
					&& this.nextPageAngle >= this.approximatePageAngle - 1.0F);
		}

		this.pageAngle = this.nextPageAngle;
		this.pageTurningSpeed = this.nextPageTurningSpeed;
		boolean bl = false;

		for (int i = 0; i < 3; i++) {
			if (this.handler.enchantmentPower[i] != 0) {
				bl = true;
				break;
			}
		}

		if (bl) {
			this.nextPageTurningSpeed += 0.2F;
		}
		else {
			this.nextPageTurningSpeed -= 0.2F;
		}

		this.nextPageTurningSpeed = MathHelper.clamp(this.nextPageTurningSpeed, 0.0F, 1.0F);
		float f = (this.approximatePageAngle - this.nextPageAngle) * 0.4F;
		float g = 0.2F;
		f = MathHelper.clamp(f, -0.2F, 0.2F);
		this.pageRotationSpeed = this.pageRotationSpeed + (f - this.pageRotationSpeed) * 0.9F;
		this.nextPageAngle = this.nextPageAngle + this.pageRotationSpeed;
	}
}
