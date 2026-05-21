package net.minecraft.component.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.StringHelper;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@code WrittenBookContentComponent}.
 */
public record WrittenBookContentComponent(
		RawFilteredPair<String> title,
		String author,
		int generation,
		List<RawFilteredPair<Text>> pages,
		boolean resolved
)
		implements BookContent<Text, WrittenBookContentComponent>,
		TooltipAppender {

	public static final WrittenBookContentComponent
			DEFAULT =
			new WrittenBookContentComponent(RawFilteredPair.of(""), "", 0, List.of(), true);
	public static final int MAX_SERIALIZED_PAGE_LENGTH = 32767;
	public static final int MAX_PAGES = 16;
	public static final int MAX_TITLE_LENGTH = 32;
	public static final int MAX_GENERATION = 3;
	public static final int UNCOPIABLE_GENERATION = 2;
	public static final Codec<Text> PAGE_CODEC = TextCodecs.withJsonLengthLimit(32767);
	public static final Codec<List<RawFilteredPair<Text>>> PAGES_CODEC = createPagesCodec(PAGE_CODEC);
	public static final Codec<WrittenBookContentComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    RawFilteredPair
							                    .createCodec(Codec.string(0, 32))
							                    .fieldOf("title")
							                    .forGetter(WrittenBookContentComponent::title),
					                    Codec.STRING.fieldOf("author").forGetter(WrittenBookContentComponent::author),
					                    Codecs
							                    .rangedInt(0, 3)
							                    .optionalFieldOf("generation", 0)
							                    .forGetter(WrittenBookContentComponent::generation),
					                    PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(WrittenBookContentComponent::pages),
					                    Codec.BOOL.optionalFieldOf("resolved", false).forGetter(WrittenBookContentComponent::resolved)
			                    )
			                    .apply(instance, WrittenBookContentComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, WrittenBookContentComponent> PACKET_CODEC = PacketCodec.tuple(
			RawFilteredPair.createPacketCodec(PacketCodecs.string(32)),
			WrittenBookContentComponent::title,
			PacketCodecs.STRING,
			WrittenBookContentComponent::author,
			PacketCodecs.VAR_INT,
			WrittenBookContentComponent::generation,
			RawFilteredPair.createPacketCodec(TextCodecs.REGISTRY_PACKET_CODEC).collect(PacketCodecs.toList()),
			WrittenBookContentComponent::pages,
			PacketCodecs.BOOLEAN,
			WrittenBookContentComponent::resolved,
			WrittenBookContentComponent::new
	);

	public WrittenBookContentComponent(
			RawFilteredPair<String> title,
			String author,
			int generation,
			List<RawFilteredPair<Text>> pages,
			boolean resolved
	) {
		if (generation >= 0 && generation <= 3) {
			this.title = title;
			this.author = author;
			this.generation = generation;
			this.pages = pages;
			this.resolved = resolved;
		}
		else {
			throw new IllegalArgumentException("Generation was " + generation + ", but must be between 0 and 3");
		}
	}

	private static Codec<RawFilteredPair<Text>> createPageCodec(Codec<Text> textCodec) {
		return RawFilteredPair.createCodec(textCodec);
	}

	public static Codec<List<RawFilteredPair<Text>>> createPagesCodec(Codec<Text> textCodec) {
		return createPageCodec(textCodec).listOf();
	}

	public @Nullable WrittenBookContentComponent copy() {
		return this.generation >= 2 ? null : new WrittenBookContentComponent(
				this.title,
				this.author,
				this.generation + 1,
				this.pages,
				this.resolved
		);
	}

	public static boolean resolveInStack(
			ItemStack stack,
			ServerCommandSource commandSource,
			@Nullable PlayerEntity player
	) {
		WrittenBookContentComponent writtenBookContentComponent = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
		if (writtenBookContentComponent != null && !writtenBookContentComponent.resolved()) {
			WrittenBookContentComponent
					writtenBookContentComponent2 =
					writtenBookContentComponent.resolve(commandSource, player);
			if (writtenBookContentComponent2 != null) {
				stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, writtenBookContentComponent2);
				return true;
			}

			stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, writtenBookContentComponent.asResolved());
		}

		return false;
	}

	public @Nullable WrittenBookContentComponent resolve(ServerCommandSource source, @Nullable PlayerEntity player) {
		if (this.resolved) {
			return null;
		}
		else {
			Builder<RawFilteredPair<Text>> builder = ImmutableList.builderWithExpectedSize(this.pages.size());

			for (RawFilteredPair<Text> rawFilteredPair : this.pages) {
				Optional<RawFilteredPair<Text>> optional = resolve(source, player, rawFilteredPair);
				if (optional.isEmpty()) {
					return null;
				}

				builder.add(optional.get());
			}

			return new WrittenBookContentComponent(this.title, this.author, this.generation, builder.build(), true);
		}
	}

	public WrittenBookContentComponent asResolved() {
		return new WrittenBookContentComponent(this.title, this.author, this.generation, this.pages, true);
	}

	private static Optional<RawFilteredPair<Text>> resolve(
			ServerCommandSource source,
			@Nullable PlayerEntity player,
			RawFilteredPair<Text> page
	) {
		return page.resolve(text -> {
			try {
				Text text2 = Texts.parse(source, text, player, 0);
				return exceedsSerializedLengthLimit(text2, source.getRegistryManager()) ? Optional.empty()
				                                                                        : Optional.of(text2);
			}
			catch (Exception var4) {
				return Optional.of(text);
			}
		});
	}

	private static boolean exceedsSerializedLengthLimit(Text text, RegistryWrapper.WrapperLookup registries) {
		DataResult<JsonElement> dataResult = TextCodecs.CODEC.encodeStart(registries.getOps(JsonOps.INSTANCE), text);
		return dataResult.isSuccess() && JsonHelper.isTooLarge((JsonElement) dataResult.getOrThrow(), 32767);
	}

	public List<Text> getPages(boolean shouldFilter) {
		return Lists.transform(this.pages, page -> (Text) page.get(shouldFilter));
	}

	public WrittenBookContentComponent withPages(List<RawFilteredPair<Text>> list) {
		return new WrittenBookContentComponent(this.title, this.author, this.generation, list, false);
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (!StringHelper.isBlank(this.author)) {
			textConsumer.accept(Text.translatable("book.byAuthor", this.author).formatted(Formatting.GRAY));
		}

		textConsumer.accept(Text.translatable("book.generation." + this.generation).formatted(Formatting.GRAY));
	}
}
