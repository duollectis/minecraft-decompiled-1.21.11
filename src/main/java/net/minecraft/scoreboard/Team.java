package net.minecraft.scoreboard;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Конкретная реализация команды скорборда.
 * <p>
 * Хранит список участников, настройки отображения (префикс, суффикс, цвет),
 * правила видимости и столкновений. При изменении любого параметра
 * уведомляет {@link Scoreboard} для синхронизации с клиентами.
 */
public class Team extends AbstractTeam {

	private static final int FRIENDLY_FIRE_FLAG = 1;
	private static final int SHOW_INVISIBLES_FLAG = 2;

	private final Scoreboard scoreboard;
	private final String name;
	private final Set<String> playerList = Sets.newHashSet();
	private Text displayName;
	private Text prefix = ScreenTexts.EMPTY;
	private Text suffix = ScreenTexts.EMPTY;
	private boolean friendlyFire = true;
	private boolean showFriendlyInvisibles = true;
	private VisibilityRule nameTagVisibilityRule = VisibilityRule.ALWAYS;
	private VisibilityRule deathMessageVisibilityRule = VisibilityRule.ALWAYS;
	private Formatting color = Formatting.RESET;
	private CollisionRule collisionRule = CollisionRule.ALWAYS;
	private final Style nameStyle;

	public Team(Scoreboard scoreboard, String name) {
		this.scoreboard = scoreboard;
		this.name = name;
		this.displayName = Text.literal(name);
		this.nameStyle = Style.EMPTY
				.withInsertion(name)
				.withHoverEvent(new HoverEvent.ShowText(Text.literal(name)));
	}

	public Packed pack() {
		return new Packed(
				name,
				Optional.of(displayName),
				color != Formatting.RESET ? Optional.of(color) : Optional.empty(),
				friendlyFire,
				showFriendlyInvisibles,
				prefix,
				suffix,
				nameTagVisibilityRule,
				deathMessageVisibilityRule,
				collisionRule,
				List.copyOf(playerList)
		);
	}

	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	@Override
	public String getName() {
		return name;
	}

	public Text getDisplayName() {
		return displayName;
	}

	/**
	 * Возвращает отображаемое имя команды в квадратных скобках с цветом команды.
	 * Используется в командах и сообщениях чата для идентификации команды.
	 */
	public MutableText getFormattedName() {
		MutableText formatted = Texts.bracketed(displayName.copy().fillStyle(nameStyle));
		Formatting teamColor = getColor();
		if (teamColor != Formatting.RESET) {
			formatted.formatted(teamColor);
		}

		return formatted;
	}

	public void setDisplayName(Text displayName) {
		if (displayName == null) {
			throw new IllegalArgumentException("Name cannot be null");
		}

		this.displayName = displayName;
		scoreboard.updateScoreboardTeam(this);
	}

	public void setPrefix(@Nullable Text prefix) {
		this.prefix = prefix == null ? ScreenTexts.EMPTY : prefix;
		scoreboard.updateScoreboardTeam(this);
	}

	public Text getPrefix() {
		return prefix;
	}

	public void setSuffix(@Nullable Text suffix) {
		this.suffix = suffix == null ? ScreenTexts.EMPTY : suffix;
		scoreboard.updateScoreboardTeam(this);
	}

	public Text getSuffix() {
		return suffix;
	}

	@Override
	public Collection<String> getPlayerList() {
		return playerList;
	}

	/**
	 * Оборачивает имя участника в префикс и суффикс команды с цветом.
	 * Используется при отображении имён игроков в чате и над головой.
	 */
	@Override
	public MutableText decorateName(Text name) {
		MutableText decorated = Text.empty().append(prefix).append(name).append(suffix);
		Formatting teamColor = getColor();
		if (teamColor != Formatting.RESET) {
			decorated.formatted(teamColor);
		}

		return decorated;
	}

	/**
	 * Статический вариант декорирования имени: если команда равна {@code null},
	 * возвращает копию исходного имени без изменений.
	 */
	public static MutableText decorateName(@Nullable AbstractTeam team, Text name) {
		return team == null ? name.copy() : team.decorateName(name);
	}

	@Override
	public boolean isFriendlyFireAllowed() {
		return friendlyFire;
	}

	public void setFriendlyFireAllowed(boolean friendlyFire) {
		this.friendlyFire = friendlyFire;
		scoreboard.updateScoreboardTeam(this);
	}

	@Override
	public boolean shouldShowFriendlyInvisibles() {
		return showFriendlyInvisibles;
	}

	public void setShowFriendlyInvisibles(boolean showFriendlyInvisibles) {
		this.showFriendlyInvisibles = showFriendlyInvisibles;
		scoreboard.updateScoreboardTeam(this);
	}

	@Override
	public VisibilityRule getNameTagVisibilityRule() {
		return nameTagVisibilityRule;
	}

	@Override
	public VisibilityRule getDeathMessageVisibilityRule() {
		return deathMessageVisibilityRule;
	}

	public void setNameTagVisibilityRule(VisibilityRule nameTagVisibilityRule) {
		this.nameTagVisibilityRule = nameTagVisibilityRule;
		scoreboard.updateScoreboardTeam(this);
	}

	public void setDeathMessageVisibilityRule(VisibilityRule deathMessageVisibilityRule) {
		this.deathMessageVisibilityRule = deathMessageVisibilityRule;
		scoreboard.updateScoreboardTeam(this);
	}

	@Override
	public CollisionRule getCollisionRule() {
		return collisionRule;
	}

	public void setCollisionRule(CollisionRule collisionRule) {
		this.collisionRule = collisionRule;
		scoreboard.updateScoreboardTeam(this);
	}

	/**
	 * Возвращает битовую маску флагов команды для сетевой передачи.
	 * Бит 0 — дружественный огонь, бит 1 — видимость невидимых союзников.
	 */
	public int getFriendlyFlagsBitwise() {
		int flags = 0;
		if (isFriendlyFireAllowed()) {
			flags |= FRIENDLY_FIRE_FLAG;
		}

		if (shouldShowFriendlyInvisibles()) {
			flags |= SHOW_INVISIBLES_FLAG;
		}

		return flags;
	}

	/**
	 * Устанавливает флаги команды из битовой маски, полученной по сети.
	 */
	public void setFriendlyFlagsBitwise(int flags) {
		setFriendlyFireAllowed((flags & FRIENDLY_FIRE_FLAG) > 0);
		setShowFriendlyInvisibles((flags & SHOW_INVISIBLES_FLAG) > 0);
	}

	public void setColor(Formatting color) {
		this.color = color;
		scoreboard.updateScoreboardTeam(this);
	}

	@Override
	public Formatting getColor() {
		return color;
	}

	/**
	 * Упакованное представление команды для сериализации в NBT/JSON и сетевой передачи.
	 */
	public record Packed(
			String name,
			Optional<Text> displayName,
			Optional<Formatting> color,
			boolean allowFriendlyFire,
			boolean seeFriendlyInvisibles,
			Text memberNamePrefix,
			Text memberNameSuffix,
			VisibilityRule nameTagVisibility,
			VisibilityRule deathMessageVisibility,
			CollisionRule collisionRule,
			List<String> players
	) {

		public static final Codec<Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.STRING.fieldOf("Name").forGetter(Packed::name),
						TextCodecs.CODEC.optionalFieldOf("DisplayName").forGetter(Packed::displayName),
						Formatting.COLOR_CODEC.optionalFieldOf("TeamColor").forGetter(Packed::color),
						Codec.BOOL.optionalFieldOf("AllowFriendlyFire", true).forGetter(Packed::allowFriendlyFire),
						Codec.BOOL.optionalFieldOf("SeeFriendlyInvisibles", true).forGetter(Packed::seeFriendlyInvisibles),
						TextCodecs.CODEC
								.optionalFieldOf("MemberNamePrefix", ScreenTexts.EMPTY)
								.forGetter(Packed::memberNamePrefix),
						TextCodecs.CODEC
								.optionalFieldOf("MemberNameSuffix", ScreenTexts.EMPTY)
								.forGetter(Packed::memberNameSuffix),
						VisibilityRule.CODEC
								.optionalFieldOf("NameTagVisibility", VisibilityRule.ALWAYS)
								.forGetter(Packed::nameTagVisibility),
						VisibilityRule.CODEC
								.optionalFieldOf("DeathMessageVisibility", VisibilityRule.ALWAYS)
								.forGetter(Packed::deathMessageVisibility),
						CollisionRule.CODEC
								.optionalFieldOf("CollisionRule", CollisionRule.ALWAYS)
								.forGetter(Packed::collisionRule),
						Codec.STRING.listOf().optionalFieldOf("Players", List.of()).forGetter(Packed::players)
				).apply(instance, Packed::new)
		);
	}
}
