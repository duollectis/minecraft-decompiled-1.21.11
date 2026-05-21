package net.minecraft.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code TutorialStep}.
 */
public enum TutorialStep {
	MOVEMENT("movement", MovementTutorialStepHandler::new),
	FIND_TREE("find_tree", FindTreeTutorialStepHandler::new),
	PUNCH_TREE("punch_tree", PunchTreeTutorialStepHandler::new),
	OPEN_INVENTORY("open_inventory", OpenInventoryTutorialStepHandler::new),
	CRAFT_PLANKS("craft_planks", CraftPlanksTutorialStepHandler::new),
	NONE("none", NoneTutorialStepHandler::new);

	private final String name;
	private final Function<TutorialManager, ? extends TutorialStepHandler> handlerFactory;

	private <T extends TutorialStepHandler> TutorialStep(
			final String name,
			final Function<TutorialManager, T> factory
	) {
		this.name = name;
		this.handlerFactory = factory;
	}

	/**
	 * Создаёт handler.
	 *
	 * @param manager manager
	 *
	 * @return TutorialStepHandler — результат операции
	 */
	public TutorialStepHandler createHandler(TutorialManager manager) {
		return this.handlerFactory.apply(manager);
	}

	public String getName() {
		return this.name;
	}

	/**
	 * By name.
	 *
	 * @param name name
	 *
	 * @return TutorialStep — результат операции
	 */
	public static TutorialStep byName(String name) {
		for (TutorialStep tutorialStep : values()) {
			if (tutorialStep.name.equals(name)) {
				return tutorialStep;
			}
		}

		return NONE;
	}
}
