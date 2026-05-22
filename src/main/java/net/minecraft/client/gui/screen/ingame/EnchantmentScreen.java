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

/**
 * Экран стола зачарований. Отображает три слота зачарований с псевдослучайными фразами
 * на шрифте «alt» и анимированной книгой. Управляет требованиями к уровню и лазуриту.
 */
@Environment(EnvType.CLIENT)
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
	private static final Identifier ENCHANTMENT_SLOT_DISABLED_TEXTURE = Identifier.ofVanilla("container/enchanting_table/enchantment_slot_disabled");
	private static final Identifier ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("container/enchanting_table/enchantment_slot_highlighted");
	private static final Identifier ENCHANTMENT_SLOT_TEXTURE = Identifier.ofVanilla("container/enchanting_table/enchantment_slot");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/enchanting_table.png");
	private static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/entity/enchanting_table_book.png");
	private static final int SLOT_COUNT = 3;
	private static final int SLOT_HEIGHT = 19;
	private static final int SLOT_WIDTH = 108;
	private static final int SLOT_OFFSET_X = 60;
	private static final int SLOT_OFFSET_Y = 14;
	private static final int COLOR_DISABLED = -9937334;
	private static final int COLOR_ACTIVE = -8323296;
	private static final int COLOR_HIGHLIGHTED = -128;
	private static final int COLOR_INSUFFICIENT = -12550384;
	private static final float PAGE_TURN_SPEED = 0.2F;
	private static final float PAGE_ROTATION_DAMPING = 0.9F;
	private static final float MAX_PAGE_DELTA = 0.2F;

	private final Random random = Random.create();
	private BookModel bookModel;
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
		bookModel = new BookModel(client.getLoadedEntityModels().getModelPart(EntityModelLayers.BOOK));
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		client.player.experienceBarDisplayStartTime = client.player.age;
		doTick();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;

		for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
			double relX = click.x() - (bgX + SLOT_OFFSET_X);
			double relY = click.y() - (bgY + SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex);
			if (relX >= 0.0 && relY >= 0.0 && relX < SLOT_WIDTH && relY < SLOT_HEIGHT
				&& handler.onButtonClick(client.player, slotIndex)
			) {
				client.interactionManager.clickButton(handler.syncId, slotIndex);
				return true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
		drawBook(context, bgX, bgY);
		EnchantingPhrases.getInstance().setSeed(handler.getSeed());
		int lapisCount = handler.getLapisCount();

		for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
			int slotX = bgX + SLOT_OFFSET_X;
			int phraseX = slotX + 20;
			int enchantPower = handler.enchantmentPower[slotIndex];

			if (enchantPower == 0) {
				context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					ENCHANTMENT_SLOT_DISABLED_TEXTURE,
					slotX,
					bgY + SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex,
					SLOT_WIDTH,
					SLOT_HEIGHT
				);
				continue;
			}

			String levelText = enchantPower + "";
			int phraseWidth = 86 - textRenderer.getWidth(levelText);
			StringVisitable phrase = EnchantingPhrases.getInstance().generatePhrase(textRenderer, phraseWidth);
			int textColor = COLOR_DISABLED;
			boolean isInsufficient = (lapisCount < slotIndex + 1 || client.player.experienceLevel < enchantPower)
				&& !client.player.isInCreativeMode();

			if (isInsufficient) {
				context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					ENCHANTMENT_SLOT_DISABLED_TEXTURE,
					slotX,
					bgY + SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex,
					SLOT_WIDTH,
					SLOT_HEIGHT
				);
				context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					LEVEL_DISABLED_TEXTURES[slotIndex],
					slotX + 1,
					bgY + 15 + SLOT_HEIGHT * slotIndex,
					16,
					16
				);
				context.drawWrappedText(
					textRenderer,
					phrase,
					phraseX,
					bgY + 16 + SLOT_HEIGHT * slotIndex,
					phraseWidth,
					ColorHelper.fullAlpha((textColor & 16711422) >> 1),
					false
				);
				textColor = COLOR_INSUFFICIENT;
			} else {
				int relMouseX = mouseX - (bgX + SLOT_OFFSET_X);
				int relMouseY = mouseY - (bgY + SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex);
				if (relMouseX >= 0 && relMouseY >= 0 && relMouseX < SLOT_WIDTH && relMouseY < SLOT_HEIGHT) {
					context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE,
						slotX,
						bgY + SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex,
						SLOT_WIDTH,
						SLOT_HEIGHT
					);
					context.setCursor(StandardCursors.POINTING_HAND);
					textColor = COLOR_HIGHLIGHTED;
				} else {
					context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						ENCHANTMENT_SLOT_TEXTURE,
						slotX,
						bgY + SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex,
						SLOT_WIDTH,
						SLOT_HEIGHT
					);
				}

				context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					LEVEL_TEXTURES[slotIndex],
					slotX + 1,
					bgY + 15 + SLOT_HEIGHT * slotIndex,
					16,
					16
				);
				context.drawWrappedText(textRenderer, phrase, phraseX, bgY + 16 + SLOT_HEIGHT * slotIndex, phraseWidth, textColor, false);
				textColor = COLOR_ACTIVE;
			}

			context.drawTextWithShadow(
				textRenderer,
				levelText,
				phraseX + 86 - textRenderer.getWidth(levelText),
				bgY + 16 + SLOT_HEIGHT * slotIndex + 7,
				textColor
			);
		}
	}

	private void drawBook(DrawContext context, int x, int y) {
		float tickProgress = client.getRenderTickCounter().getTickProgress(false);
		float turningSpeed = MathHelper.lerp(tickProgress, pageTurningSpeed, nextPageTurningSpeed);
		float angle = MathHelper.lerp(tickProgress, pageAngle, nextPageAngle);
		int bookX = x + 14;
		int bookY = y + 14;
		context.addBookModel(bookModel, BOOK_TEXTURE, 40.0F, turningSpeed, angle, bookX, bookY, bookX + 38, bookY + 31);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		float tickProgress = client.getRenderTickCounter().getTickProgress(false);
		super.render(context, mouseX, mouseY, tickProgress);
		drawMouseoverTooltip(context, mouseX, mouseY);
		boolean isCreative = client.player.isInCreativeMode();
		int lapisCount = handler.getLapisCount();

		for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
			int enchantPower = handler.enchantmentPower[slotIndex];
			Optional<RegistryEntry.Reference<Enchantment>> enchantEntry = client.world
				.getRegistryManager()
				.getOrThrow(RegistryKeys.ENCHANTMENT)
				.getEntry(handler.enchantmentId[slotIndex]);

			if (enchantEntry.isEmpty()) {
				continue;
			}

			int enchantLevel = handler.enchantmentLevel[slotIndex];
			int requiredLapis = slotIndex + 1;
			if (!isPointWithinBounds(SLOT_OFFSET_X, SLOT_OFFSET_Y + SLOT_HEIGHT * slotIndex, SLOT_WIDTH, 17, mouseX, mouseY)
				|| enchantPower <= 0
				|| enchantLevel < 0
			) {
				continue;
			}

			List<Text> tooltip = Lists.newArrayList();
			tooltip.add(
				Text.translatable("container.enchant.clue", Enchantment.getName(enchantEntry.get(), enchantLevel))
					.formatted(Formatting.WHITE)
			);

			if (!isCreative) {
				tooltip.add(ScreenTexts.EMPTY);
				if (client.player.experienceLevel < enchantPower) {
					tooltip.add(
						Text.translatable("container.enchant.level.requirement", handler.enchantmentPower[slotIndex])
							.formatted(Formatting.RED)
					);
				} else {
					MutableText lapisText = requiredLapis == 1
						? Text.translatable("container.enchant.lapis.one")
						: Text.translatable("container.enchant.lapis.many", requiredLapis);
					tooltip.add(lapisText.formatted(lapisCount >= requiredLapis ? Formatting.GRAY : Formatting.RED));

					MutableText levelText = requiredLapis == 1
						? Text.translatable("container.enchant.level.one")
						: Text.translatable("container.enchant.level.many", requiredLapis);
					tooltip.add(levelText.formatted(Formatting.GRAY));
				}
			}

			context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
			break;
		}
	}

	/**
	 * Обновляет анимацию книги: угол поворота страниц и скорость листания
	 * в зависимости от наличия доступных зачарований.
	 */
	public void doTick() {
		ItemStack currentStack = handler.getSlot(0).getStack();
		if (!ItemStack.areEqual(currentStack, stack)) {
			stack = currentStack;

			do {
				approximatePageAngle += random.nextInt(4) - random.nextInt(4);
			} while (nextPageAngle <= approximatePageAngle + 1.0F && nextPageAngle >= approximatePageAngle - 1.0F);
		}

		pageAngle = nextPageAngle;
		pageTurningSpeed = nextPageTurningSpeed;
		boolean hasEnchantments = false;

		for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
			if (handler.enchantmentPower[slotIndex] != 0) {
				hasEnchantments = true;
				break;
			}
		}

		if (hasEnchantments) {
			nextPageTurningSpeed += PAGE_TURN_SPEED;
		} else {
			nextPageTurningSpeed -= PAGE_TURN_SPEED;
		}

		nextPageTurningSpeed = MathHelper.clamp(nextPageTurningSpeed, 0.0F, 1.0F);
		float pageDelta = MathHelper.clamp((approximatePageAngle - nextPageAngle) * 0.4F, -MAX_PAGE_DELTA, MAX_PAGE_DELTA);
		pageRotationSpeed = pageRotationSpeed + (pageDelta - pageRotationSpeed) * PAGE_ROTATION_DAMPING;
		nextPageAngle += pageRotationSpeed;
	}
}
