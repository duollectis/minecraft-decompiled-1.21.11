package net.minecraft.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code TutorialManager}.
 */
public class TutorialManager {

	private final MinecraftClient client;
	private @Nullable TutorialStepHandler currentHandler;

	public TutorialManager(MinecraftClient client, GameOptions options) {
		this.client = client;
	}

	/**
	 * Обрабатывает событие movement.
	 *
	 * @param input input
	 */
	public void onMovement(Input input) {
		if (this.currentHandler != null) {
			this.currentHandler.onMovement(input);
		}
	}

	/**
	 * Обрабатывает событие update mouse.
	 *
	 * @param deltaX delta x
	 * @param deltaY delta y
	 */
	public void onUpdateMouse(double deltaX, double deltaY) {
		if (this.currentHandler != null) {
			this.currentHandler.onMouseUpdate(deltaX, deltaY);
		}
	}

	/**
	 * Tick.
	 *
	 * @param world world
	 * @param hitResult hit result
	 */
	public void tick(@Nullable ClientWorld world, @Nullable HitResult hitResult) {
		if (this.currentHandler != null && hitResult != null && world != null) {
			this.currentHandler.onTarget(world, hitResult);
		}
	}

	/**
	 * Обрабатывает событие block breaking.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 * @param progress progress
	 */
	public void onBlockBreaking(ClientWorld world, BlockPos pos, BlockState state, float progress) {
		if (this.currentHandler != null) {
			this.currentHandler.onBlockBreaking(world, pos, state, progress);
		}
	}

	/**
	 * Обрабатывает событие inventory opened.
	 */
	public void onInventoryOpened() {
		if (this.currentHandler != null) {
			this.currentHandler.onInventoryOpened();
		}
	}

	/**
	 * Обрабатывает событие slot update.
	 *
	 * @param stack stack
	 */
	public void onSlotUpdate(ItemStack stack) {
		if (this.currentHandler != null) {
			this.currentHandler.onSlotUpdate(stack);
		}
	}

	/**
	 * Destroy handler.
	 */
	public void destroyHandler() {
		if (this.currentHandler != null) {
			this.currentHandler.destroy();
			this.currentHandler = null;
		}
	}

	/**
	 * Создаёт handler.
	 */
	public void createHandler() {
		if (this.currentHandler != null) {
			this.destroyHandler();
		}

		this.currentHandler = this.client.options.tutorialStep.createHandler(this);
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (this.currentHandler != null) {
			if (this.client.world != null) {
				this.currentHandler.tick();
			}
			else {
				this.destroyHandler();
			}
		}
		else if (this.client.world != null) {
			this.createHandler();
		}
	}

	public void setStep(TutorialStep step) {
		this.client.options.tutorialStep = step;
		this.client.options.write();
		if (this.currentHandler != null) {
			this.currentHandler.destroy();
			this.currentHandler = step.createHandler(this);
		}
	}

	public MinecraftClient getClient() {
		return this.client;
	}

	public boolean isInSurvival() {
		return this.client.interactionManager == null ? false : this.client.interactionManager.getCurrentGameMode()
		                                                        == GameMode.SURVIVAL;
	}

	/**
	 * Key to text.
	 *
	 * @param name name
	 *
	 * @return Text — результат операции
	 */
	public static Text keyToText(String name) {
		return Text.keybind("key." + name).formatted(Formatting.BOLD);
	}

	/**
	 * Обрабатывает событие pickup slot click.
	 *
	 * @param cursorStack cursor stack
	 * @param slotStack slot stack
	 * @param clickType click type
	 */
	public void onPickupSlotClick(ItemStack cursorStack, ItemStack slotStack, ClickType clickType) {
	}
}
