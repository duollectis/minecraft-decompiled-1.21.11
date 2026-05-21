package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.TestInstanceBlockActionC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.test.TestInstance;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code TestInstanceBlockScreen}.
 */
public class TestInstanceBlockScreen extends Screen {

	private static final Text TEST_ID_TEXT = Text.translatable("test_instance_block.test_id");
	private static final Text SIZE_TEXT = Text.translatable("test_instance_block.size");
	private static final Text ENTITIES_TEXT = Text.translatable("test_instance_block.entities");
	private static final Text ROTATION_TEXT = Text.translatable("test_instance_block.rotation");
	private static final int PADDING = 8;
	private static final int CONTENT_WIDTH = 316;
	private final TestInstanceBlockEntity testInstanceBlockEntity;
	private @Nullable TextFieldWidget testIdTextField;
	private @Nullable TextFieldWidget sizeXField;
	private @Nullable TextFieldWidget sizeYField;
	private @Nullable TextFieldWidget sizeZField;
	private @Nullable ScrollableTextWidget statusWidget;
	private @Nullable ButtonWidget saveButton;
	private @Nullable ButtonWidget exportButton;
	private @Nullable CyclingButtonWidget<Boolean> entitiesButton;
	private @Nullable CyclingButtonWidget<BlockRotation> rotationButton;

	public TestInstanceBlockScreen(TestInstanceBlockEntity testInstanceBlockEntity) {
		super(testInstanceBlockEntity.getCachedState().getBlock().getName());
		this.testInstanceBlockEntity = testInstanceBlockEntity;
	}

	@Override
	protected void init() {
		int i = this.width / 2 - 158;
		boolean bl = SharedConstants.isDevelopment;
		int j = bl ? 3 : 2;
		int k = getRoundedWidth(j);
		this.testIdTextField =
				new TextFieldWidget(
						this.textRenderer,
						i,
						40,
						316,
						20,
						Text.translatable("test_instance_block.test_id")
				);
		this.testIdTextField.setMaxLength(128);
		Optional<RegistryKey<TestInstance>> optional = this.testInstanceBlockEntity.getTestKey();
		if (optional.isPresent()) {
			this.testIdTextField.setText(optional.get().getValue().toString());
		}

		this.testIdTextField.setChangedListener(value -> this.refresh(false));
		this.addDrawableChild(this.testIdTextField);
		this.statusWidget = new ScrollableTextWidget(i, 70, 316, 8 * 9, Text.literal(""), this.textRenderer);
		this.addDrawableChild(this.statusWidget);
		Vec3i vec3i = this.testInstanceBlockEntity.getSize();
		int l = 0;
		this.sizeXField =
				new TextFieldWidget(
						this.textRenderer,
						this.getX(l++, 5),
						160,
						getRoundedWidth(5),
						20,
						Text.translatable("structure_block.size.x")
				);
		this.sizeXField.setMaxLength(15);
		this.addDrawableChild(this.sizeXField);
		this.sizeYField =
				new TextFieldWidget(
						this.textRenderer,
						this.getX(l++, 5),
						160,
						getRoundedWidth(5),
						20,
						Text.translatable("structure_block.size.y")
				);
		this.sizeYField.setMaxLength(15);
		this.addDrawableChild(this.sizeYField);
		this.sizeZField =
				new TextFieldWidget(
						this.textRenderer,
						this.getX(l++, 5),
						160,
						getRoundedWidth(5),
						20,
						Text.translatable("structure_block.size.z")
				);
		this.sizeZField.setMaxLength(15);
		this.addDrawableChild(this.sizeZField);
		this.setSize(vec3i);
		this.rotationButton = this.addDrawableChild(
				CyclingButtonWidget
						.builder(TestInstanceBlockScreen::rotationAsText, this.testInstanceBlockEntity.getRotation())
						.values(BlockRotation.values())
						.omitKeyText()
						.build(
								this.getX(l++, 5),
								160,
								getRoundedWidth(5),
								20,
								ROTATION_TEXT,
								(button, rotation) -> this.refresh()
						)
		);
		this.entitiesButton = this.addDrawableChild(
				CyclingButtonWidget.onOffBuilder(!this.testInstanceBlockEntity.shouldIgnoreEntities())
				                   .omitKeyText()
				                   .build(this.getX(l++, 5), 160, getRoundedWidth(5), 20, ENTITIES_TEXT)
		);
		l = 0;
		this.addDrawableChild(ButtonWidget.builder(
				Text.translatable("test_instance.action.reset"), button -> {
					this.executeAction(TestInstanceBlockActionC2SPacket.Action.RESET);
					this.client.setScreen(null);
				}
		).dimensions(this.getX(l++, j), 185, k, 20).build());
		this.saveButton = this.addDrawableChild(ButtonWidget.builder(
				Text.translatable("test_instance.action.save"), button -> {
					this.executeAction(TestInstanceBlockActionC2SPacket.Action.SAVE);
					this.client.setScreen(null);
				}
		).dimensions(this.getX(l++, j), 185, k, 20).build());
		if (bl) {
			this.exportButton = this.addDrawableChild(ButtonWidget.builder(
					Text.literal("Export Structure"), button -> {
						this.executeAction(TestInstanceBlockActionC2SPacket.Action.EXPORT);
						this.client.setScreen(null);
					}
			).dimensions(this.getX(l++, j), 185, k, 20).build());
		}

		this.addDrawableChild(ButtonWidget.builder(
				Text.translatable("test_instance.action.run"), button -> {
					this.executeAction(TestInstanceBlockActionC2SPacket.Action.RUN);
					this.client.setScreen(null);
				}
		).dimensions(this.getX(0, 3), 210, getRoundedWidth(3), 20).build());
		this.addDrawableChild(ButtonWidget
				.builder(ScreenTexts.DONE, button -> this.onDone())
				.dimensions(this.getX(1, 3), 210, getRoundedWidth(3), 20)
				.build());
		this.addDrawableChild(
				ButtonWidget
						.builder(ScreenTexts.CANCEL, button -> this.onCancel())
						.dimensions(this.getX(2, 3), 210, getRoundedWidth(3), 20)
						.build()
		);
		this.refresh(true);
	}

	private void refresh() {
		boolean
				bl =
				this.rotationButton.getValue() == BlockRotation.NONE
						&& Identifier.tryParse(this.testIdTextField.getText()) != null;
		this.saveButton.active = bl;
		if (this.exportButton != null) {
			this.exportButton.active = bl;
		}
	}

	private static Text rotationAsText(BlockRotation rotation) {
		return Text.literal(switch (rotation) {
			case NONE -> "0";
			case CLOCKWISE_90 -> "90";
			case CLOCKWISE_180 -> "180";
			case COUNTERCLOCKWISE_90 -> "270";
		});
	}

	private void setSize(Vec3i vec) {
		this.sizeXField.setText(Integer.toString(vec.getX()));
		this.sizeYField.setText(Integer.toString(vec.getY()));
		this.sizeZField.setText(Integer.toString(vec.getZ()));
	}

	private int getX(int index, int total) {
		int i = this.width / 2 - 158;
		float f = getWidth(total);
		return (int) (i + index * (8.0F + f));
	}

	private static int getRoundedWidth(int total) {
		return (int) getWidth(total);
	}

	private static float getWidth(int total) {
		return (float) (316 - (total - 1) * 8) / total;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int i = this.width / 2 - 158;
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, -1);
		context.drawTextWithShadow(this.textRenderer, TEST_ID_TEXT, i, 30, -6250336);
		context.drawTextWithShadow(this.textRenderer, SIZE_TEXT, i, 150, -6250336);
		context.drawTextWithShadow(this.textRenderer, ROTATION_TEXT, this.rotationButton.getX(), 150, -6250336);
		context.drawTextWithShadow(this.textRenderer, ENTITIES_TEXT, this.entitiesButton.getX(), 150, -6250336);
	}

	private void refresh(boolean initial) {
		boolean
				bl =
				this.executeAction(initial ? TestInstanceBlockActionC2SPacket.Action.INIT
				                           : TestInstanceBlockActionC2SPacket.Action.QUERY);
		if (!bl) {
			this.statusWidget.setMessage(Text
					.translatable("test_instance.description.invalid_id")
					.formatted(Formatting.RED));
		}

		this.refresh();
	}

	private void onDone() {
		this.executeAction(TestInstanceBlockActionC2SPacket.Action.SET);
		this.close();
	}

	private boolean executeAction(TestInstanceBlockActionC2SPacket.Action action) {
		Optional<Identifier> optional = Optional.ofNullable(Identifier.tryParse(this.testIdTextField.getText()));
		Optional<RegistryKey<TestInstance>>
				optional2 =
				optional.map(testId -> RegistryKey.of(RegistryKeys.TEST_INSTANCE, testId));
		Vec3i
				vec3i =
				new Vec3i(
						parse(this.sizeXField.getText()),
						parse(this.sizeYField.getText()),
						parse(this.sizeZField.getText())
				);
		boolean bl = !this.entitiesButton.getValue();
		this.client
				.getNetworkHandler()
				.sendPacket(new TestInstanceBlockActionC2SPacket(
						this.testInstanceBlockEntity.getPos(),
						action,
						optional2,
						vec3i,
						this.rotationButton.getValue(),
						bl
				));
		return optional.isPresent();
	}

	/**
	 * Обрабатывает status.
	 *
	 * @param status status
	 * @param size size
	 */
	public void handleStatus(Text status, Optional<Vec3i> size) {
		MutableText mutableText = Text.empty();
		this.testInstanceBlockEntity
				.getErrorMessage()
				.ifPresent(
						message -> mutableText
								.append(Text.translatable(
										"test_instance.description.failed",
										Text.empty().formatted(Formatting.RED).append(message)
								))
								.append("\n\n")
				);
		mutableText.append(status);
		this.statusWidget.setMessage(mutableText);
		size.ifPresent(this::setSize);
	}

	private void onCancel() {
		this.close();
	}

	private static int parse(String value) {
		try {
			return MathHelper.clamp(Integer.parseInt(value), 1, 48);
		}
		catch (NumberFormatException var2) {
			return 1;
		}
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}
}
