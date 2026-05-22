package net.minecraft.client.gui.screen.option;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.Nullables;
import net.minecraft.world.Difficulty;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран онлайн-настроек — управляет уведомлениями Realms, видимостью в списке серверов
 * и отображает текущую сложность мира (только для чтения, если мир загружен).
 */
@Environment(EnvType.CLIENT)
public class OnlineOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("options.online.title");

	private @Nullable SimpleOption<Unit> difficulty;

	public OnlineOptionsScreen(Screen parent, GameOptions gameOptions) {
		super(parent, gameOptions, TITLE_TEXT);
	}

	@Override
	protected void init() {
		super.init();
		if (difficulty == null) {
			return;
		}

		ClickableWidget difficultyWidget = body.getWidgetFor(difficulty);
		if (difficultyWidget != null) {
			difficultyWidget.active = false;
		}
	}

	private SimpleOption<?>[] collectOptions(GameOptions gameOptions, MinecraftClient client) {
		List<SimpleOption<?>> options = new ArrayList<>();
		options.add(gameOptions.getRealmsNotifications());
		options.add(gameOptions.getAllowServerListing());
		SimpleOption<Unit> difficultyOption = Nullables.map(
				client.world,
				world -> {
					Difficulty worldDifficulty = world.getDifficulty();
					return new SimpleOption<>(
							"options.difficulty.online",
							SimpleOption.emptyTooltip(),
							(text, unit) -> worldDifficulty.getTranslatableName(),
							new SimpleOption.PotentialValuesBasedCallbacks<>(
									List.of(Unit.INSTANCE),
									Codec.EMPTY.codec()
							),
							Unit.INSTANCE,
							unit -> {}
					);
				}
		);
		if (difficultyOption != null) {
			difficulty = difficultyOption;
			options.add(difficultyOption);
		}

		return options.toArray(new SimpleOption[0]);
	}

	@Override
	protected void addOptions() {
		body.addAll(collectOptions(gameOptions, client));
	}
}
