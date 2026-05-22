package net.minecraft;

import com.mojang.logging.LogUtils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.command.EntitySelectorOptions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Language;
import net.minecraft.util.annotation.SuppressLinter;
import net.minecraft.util.logging.DebugLoggerPrintStream;
import net.minecraft.util.logging.LoggerPrintStream;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleVisitor;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Точка инициализации игрового движка Minecraft.
 * <p>
 * Отвечает за однократную загрузку всех реестров, регистрацию поведений блоков,
 * диспенсеров, котлов и перенаправление стандартных потоков вывода в логгер.
 * Вызов {@link #initialize()} обязателен перед любым обращением к игровым данным.
 */
@SuppressLinter(reason = "System.out setup")
public class Bootstrap {

	public static final PrintStream SYSOUT = System.out;
	public static final AtomicLong LOAD_TIME = new AtomicLong(-1L);

	private static volatile boolean initialized;
	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Выполняет однократную инициализацию всех игровых подсистем.
	 * <p>
	 * Метод идемпотентен: повторные вызовы игнорируются. Порядок инициализации:
	 * регистрация горючих блоков → компостируемых предметов → типов сущностей →
	 * опций селектора → поведений диспенсеров и котлов → реестров → групп предметов →
	 * перенаправление потоков вывода.
	 *
	 * @throws IllegalStateException если реестры пусты или тип сущности PLAYER не зарегистрирован
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		Instant start = Instant.now();

		if (Registries.REGISTRIES.getIds().isEmpty()) {
			throw new IllegalStateException("Unable to load registries");
		}

		FireBlock.registerDefaultFlammables();
		ComposterBlock.registerDefaultCompostableItems();

		if (EntityType.getId(EntityType.PLAYER) == null) {
			throw new IllegalStateException("Failed loading EntityTypes");
		}

		EntitySelectorOptions.register();
		DispenserBehavior.registerDefaults();
		CauldronBehavior.registerBehavior();
		Registries.bootstrap();
		ItemGroups.collect();
		setOutputStreams();

		LOAD_TIME.set(Duration.between(start, Instant.now()).toMillis());
	}

	/**
	 * Собирает ключи переводов из реестра, для которых отсутствует локализация.
	 *
	 * @param registry       итерируемый реестр объектов
	 * @param keyExtractor   функция извлечения ключа перевода из объекта реестра
	 * @param translationKeys множество, в которое добавляются отсутствующие ключи
	 */
	private static <T> void collectMissingTranslations(
		Iterable<T> registry,
		Function<T, String> keyExtractor,
		Set<String> translationKeys
	) {
		Language language = Language.getInstance();
		registry.forEach(object -> {
			String key = keyExtractor.apply((T) object);
			if (!language.hasTranslation(key)) {
				translationKeys.add(key);
			}
		});
	}

	/**
	 * Собирает ключи переводов игровых правил, для которых отсутствует локализация.
	 *
	 * @param translations множество, в которое добавляются отсутствующие ключи
	 */
	private static void collectMissingGameRuleTranslations(Set<String> translations) {
		Language language = Language.getInstance();
		GameRules gameRules = new GameRules(FeatureFlags.FEATURE_MANAGER.getFeatureSet());
		gameRules.accept(new GameRuleVisitor() {
			@Override
			public <T> void visit(GameRule<T> rule) {
				if (!language.hasTranslation(rule.getTranslationKey())) {
					translations.add(rule.toShortString());
				}
			}
		});
	}

	/**
	 * Возвращает отсортированное множество всех ключей переводов, для которых
	 * отсутствует локализация в текущем языковом файле.
	 *
	 * @return отсортированное множество отсутствующих ключей переводов
	 */
	public static Set<String> getMissingTranslations() {
		Set<String> missing = new TreeSet<>();
		collectMissingTranslations(Registries.ATTRIBUTE, EntityAttribute::getTranslationKey, missing);
		collectMissingTranslations(Registries.ENTITY_TYPE, EntityType::getTranslationKey, missing);
		collectMissingTranslations(Registries.STATUS_EFFECT, StatusEffect::getTranslationKey, missing);
		collectMissingTranslations(Registries.ITEM, Item::getTranslationKey, missing);
		collectMissingTranslations(Registries.BLOCK, AbstractBlock::getTranslationKey, missing);
		collectMissingTranslations(
			Registries.CUSTOM_STAT,
			stat -> "stat." + stat.toString().replace(':', '.'),
			missing
		);
		collectMissingGameRuleTranslations(missing);
		return missing;
	}

	/**
	 * Проверяет, что Bootstrap был инициализирован, иначе бросает исключение.
	 *
	 * @param callerGetter поставщик строки с именем вызывающего кода (для диагностики)
	 * @throws IllegalArgumentException если Bootstrap не был инициализирован
	 */
	public static void ensureBootstrapped(Supplier<String> callerGetter) {
		if (!initialized) {
			throw createNotBootstrappedException(callerGetter);
		}
	}

	private static RuntimeException createNotBootstrappedException(Supplier<String> callerGetter) {
		try {
			String caller = callerGetter.get();
			return new IllegalArgumentException("Not bootstrapped (called from " + caller + ")");
		} catch (Exception cause) {
			RuntimeException exception = new IllegalArgumentException(
				"Not bootstrapped (failed to resolve location)"
			);
			exception.addSuppressed(cause);
			return exception;
		}
	}

	/**
	 * Логирует все отсутствующие переводы и проверяет атрибуты сущностей.
	 * В режиме разработки также проверяет команды на наличие пропущенных переводов.
	 */
	public static void logMissing() {
		ensureBootstrapped(() -> "validate");

		if (SharedConstants.isDevelopment) {
			getMissingTranslations().forEach(key -> LOGGER.error("Missing translations: {}", key));
			CommandManager.checkMissing();
		}

		DefaultAttributeRegistry.checkMissing();
	}

	private static void setOutputStreams() {
		if (LOGGER.isDebugEnabled()) {
			System.setErr(new DebugLoggerPrintStream("STDERR", System.err));
			System.setOut(new DebugLoggerPrintStream("STDOUT", SYSOUT));
		} else {
			System.setErr(new LoggerPrintStream("STDERR", System.err));
			System.setOut(new LoggerPrintStream("STDOUT", SYSOUT));
		}
	}

	public static void println(String str) {
		SYSOUT.println(str);
	}
}
