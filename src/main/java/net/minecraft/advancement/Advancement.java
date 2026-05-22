package net.minecraft.advancement;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Неизменяемая запись, описывающая достижение: его родителя, отображение,
 * награды, критерии выполнения и требования к ним.
 */
public record Advancement(
	Optional<Identifier> parent,
	Optional<AdvancementDisplay> display,
	AdvancementRewards rewards,
	Map<String, AdvancementCriterion<?>> criteria,
	AdvancementRequirements requirements,
	boolean sendsTelemetryEvent,
	Optional<Text> name
) {

	private static final Codec<Map<String, AdvancementCriterion<?>>> CRITERIA_CODEC =
		Codec.unboundedMap(Codec.STRING, AdvancementCriterion.CODEC)
		     .validate(criteria -> criteria.isEmpty()
		                           ? DataResult.error(() -> "Advancement criteria cannot be empty")
		                           : DataResult.success(criteria));

	public static final Codec<Advancement> CODEC = RecordCodecBuilder.<Advancement>create(
		instance -> instance.group(
			Identifier.CODEC.optionalFieldOf("parent").forGetter(Advancement::parent),
			AdvancementDisplay.CODEC.optionalFieldOf("display").forGetter(Advancement::display),
			AdvancementRewards.CODEC
				.optionalFieldOf("rewards", AdvancementRewards.NONE)
				.forGetter(Advancement::rewards),
			CRITERIA_CODEC.fieldOf("criteria").forGetter(Advancement::criteria),
			AdvancementRequirements.CODEC
				.optionalFieldOf("requirements")
				.forGetter(advancement -> Optional.of(advancement.requirements())),
			Codec.BOOL
				.optionalFieldOf("sends_telemetry_event", false)
				.forGetter(Advancement::sendsTelemetryEvent)
		).apply(
			instance,
			(parent, display, rewards, criteria, requirements, sendsTelemetryEvent) -> {
				AdvancementRequirements resolved = requirements.orElseGet(
					() -> AdvancementRequirements.allOf(criteria.keySet())
				);
				return new Advancement(parent, display, rewards, criteria, resolved, sendsTelemetryEvent);
			}
		)
	).validate(Advancement::validate);

	public static final PacketCodec<RegistryByteBuf, Advancement> PACKET_CODEC =
		PacketCodec.of(Advancement::write, Advancement::read);

	public Advancement(
		Optional<Identifier> parent,
		Optional<AdvancementDisplay> display,
		AdvancementRewards rewards,
		Map<String, AdvancementCriterion<?>> criteria,
		AdvancementRequirements requirements,
		boolean sendsTelemetryEvent
	) {
		this(
			parent,
			display,
			rewards,
			Map.copyOf(criteria),
			requirements,
			sendsTelemetryEvent,
			display.map(Advancement::createNameFromDisplay)
		);
	}

	private static DataResult<Advancement> validate(Advancement advancement) {
		return advancement.requirements().validate(advancement.criteria().keySet()).map(validated -> advancement);
	}

	private static Text createNameFromDisplay(AdvancementDisplay display) {
		Text title = display.getTitle();
		Formatting formatting = display.getFrame().getTitleFormat();
		Text tooltip = Texts.setStyleIfAbsent(title.copy(), Style.EMPTY.withColor(formatting))
		                    .append("\n")
		                    .append(display.getDescription());
		Text hoverable = title.copy().styled(style -> style.withHoverEvent(new HoverEvent.ShowText(tooltip)));
		return Texts.bracketed(hoverable).formatted(formatting);
	}

	/**
	 * Возвращает отображаемое имя достижения: либо из его дисплея, либо строковое представление идентификатора.
	 */
	public static Text getNameFromIdentity(AdvancementEntry identifiedAdvancement) {
		return identifiedAdvancement
			.value()
			.name()
			.orElseGet(() -> Text.literal(identifiedAdvancement.id().toString()));
	}

	private void write(RegistryByteBuf buf) {
		buf.writeOptional(parent, PacketByteBuf::writeIdentifier);
		AdvancementDisplay.PACKET_CODEC.collect(PacketCodecs::optional).encode(buf, display);
		requirements.writeRequirements(buf);
		buf.writeBoolean(sendsTelemetryEvent);
	}

	private static Advancement read(RegistryByteBuf buf) {
		return new Advancement(
			buf.readOptional(PacketByteBuf::readIdentifier),
			(Optional<AdvancementDisplay>) AdvancementDisplay.PACKET_CODEC
				.collect(PacketCodecs::optional)
				.decode(buf),
			AdvancementRewards.NONE,
			Map.of(),
			new AdvancementRequirements(buf),
			buf.readBoolean()
		);
	}

	public boolean isRoot() {
		return parent.isEmpty();
	}

	/**
	 * Валидирует все условия критериев достижения через {@link LootContextPredicateValidator}.
	 */
	public void validate(ErrorReporter errorReporter, RegistryEntryLookup.RegistryLookup lookup) {
		criteria.forEach((name, criterion) -> {
			LootContextPredicateValidator validator = new LootContextPredicateValidator(
				errorReporter.makeChild(new ErrorReporter.CriterionContext(name)), lookup
			);
			criterion.conditions().validate(validator);
		});
	}

	/**
	 * Строитель для создания достижений с fluent API.
	 */
	public static class Builder {

		private Optional<Identifier> parentObj = Optional.empty();
		private Optional<AdvancementDisplay> display = Optional.empty();
		private AdvancementRewards rewards = AdvancementRewards.NONE;
		private final ImmutableMap.Builder<String, AdvancementCriterion<?>> criteria = ImmutableMap.builder();
		private Optional<AdvancementRequirements> requirements = Optional.empty();
		private AdvancementRequirements.CriterionMerger merger = AdvancementRequirements.CriterionMerger.AND;
		private boolean sendsTelemetryEvent;

		public static Builder create() {
			return new Builder().sendsTelemetryEvent();
		}

		public static Builder createUntelemetered() {
			return new Builder();
		}

		public Builder parent(AdvancementEntry parent) {
			parentObj = Optional.of(parent.id());
			return this;
		}

		@Deprecated(forRemoval = true)
		public Builder parent(Identifier parentId) {
			parentObj = Optional.of(parentId);
			return this;
		}

		public Builder display(
			ItemStack icon,
			Text title,
			Text description,
			@Nullable Identifier background,
			AdvancementFrame frame,
			boolean showToast,
			boolean announceToChat,
			boolean hidden
		) {
			return display(new AdvancementDisplay(
				icon,
				title,
				description,
				Optional.ofNullable(background).map(AssetInfo.TextureAssetInfo::new),
				frame,
				showToast,
				announceToChat,
				hidden
			));
		}

		public Builder display(
			ItemConvertible icon,
			Text title,
			Text description,
			@Nullable Identifier background,
			AdvancementFrame frame,
			boolean showToast,
			boolean announceToChat,
			boolean hidden
		) {
			return display(new AdvancementDisplay(
				new ItemStack(icon.asItem()),
				title,
				description,
				Optional.ofNullable(background).map(AssetInfo.TextureAssetInfo::new),
				frame,
				showToast,
				announceToChat,
				hidden
			));
		}

		public Builder display(AdvancementDisplay display) {
			this.display = Optional.of(display);
			return this;
		}

		public Builder rewards(AdvancementRewards.Builder builder) {
			return rewards(builder.build());
		}

		public Builder rewards(AdvancementRewards rewards) {
			this.rewards = rewards;
			return this;
		}

		public Builder criterion(String name, AdvancementCriterion<?> criterion) {
			criteria.put(name, criterion);
			return this;
		}

		public Builder criteriaMerger(AdvancementRequirements.CriterionMerger merger) {
			this.merger = merger;
			return this;
		}

		public Builder requirements(AdvancementRequirements requirements) {
			this.requirements = Optional.of(requirements);
			return this;
		}

		public Builder sendsTelemetryEvent() {
			sendsTelemetryEvent = true;
			return this;
		}

		/**
		 * Собирает и возвращает {@link AdvancementEntry} с заданным идентификатором.
		 */
		public AdvancementEntry build(Identifier id) {
			Map<String, AdvancementCriterion<?>> builtCriteria = criteria.buildOrThrow();
			AdvancementRequirements builtRequirements = requirements.orElseGet(
				() -> merger.create(builtCriteria.keySet())
			);
			return new AdvancementEntry(
				id,
				new Advancement(parentObj, display, rewards, builtCriteria, builtRequirements, sendsTelemetryEvent)
			);
		}

		public AdvancementEntry build(Consumer<AdvancementEntry> exporter, String id) {
			AdvancementEntry entry = build(Identifier.of(id));
			exporter.accept(entry);
			return entry;
		}
	}
}
