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
	 * Компонент содержимого написанной книги. Хранит заголовок, автора, поколение копии и страницы.
	 * Поколение ограничено диапазоном [0, {@link #MAX_GENERATION}]; книги поколения {@link #UNCOPIABLE_GENERATION}
	 * и выше не могут быть скопированы.
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
	public static final Codec<Text> PAGE_CODEC = TextCodecs.withJsonLengthLimit(MAX_SERIALIZED_PAGE_LENGTH);
	public static final Codec<List<RawFilteredPair<Text>>> PAGES_CODEC = createPagesCodec(PAGE_CODEC);
	public static final Codec<WrittenBookContentComponent> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
									RawFilteredPair
											.createCodec(Codec.string(0, MAX_TITLE_LENGTH))
											.fieldOf("title")
											.forGetter(WrittenBookContentComponent::title),
									Codec.STRING.fieldOf("author").forGetter(WrittenBookContentComponent::author),
									Codecs
											.rangedInt(0, MAX_GENERATION)
											.optionalFieldOf("generation", 0)
											.forGetter(WrittenBookContentComponent::generation),
									PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(WrittenBookContentComponent::pages),
									Codec.BOOL.optionalFieldOf("resolved", false).forGetter(WrittenBookContentComponent::resolved)
							)
							.apply(instance, WrittenBookContentComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, WrittenBookContentComponent> PACKET_CODEC = PacketCodec.tuple(
			RawFilteredPair.createPacketCodec(PacketCodecs.string(MAX_TITLE_LENGTH)),
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
		if (generation < 0 || generation > MAX_GENERATION) {
			throw new IllegalArgumentException(
				"Generation was " + generation + ", but must be between 0 and " + MAX_GENERATION
			);
		}

		this.title = title;
		this.author = author;
		this.generation = generation;
		this.pages = pages;
		this.resolved = resolved;
	}

	private static Codec<RawFilteredPair<Text>> createPageCodec(Codec<Text> textCodec) {
		return RawFilteredPair.createCodec(textCodec);
	}

	public static Codec<List<RawFilteredPair<Text>>> createPagesCodec(Codec<Text> textCodec) {
		return createPageCodec(textCodec).listOf();
	}

	public @Nullable WrittenBookContentComponent copy() {
		return generation >= UNCOPIABLE_GENERATION
				? null
				: new WrittenBookContentComponent(title, author, generation + 1, pages, resolved);
	}

	/**
		 * Пытается разрешить (resolve) текст страниц книги в стеке предмета через команду.
		 * Если книга уже разрешена — ничего не делает. Если разрешение не удалось — помечает как resolved без изменений.
		 *
		 * @param stack         стек предмета с компонентом книги
		 * @param commandSource источник команды для разрешения текста
		 * @param player        игрок-контекст (может быть null)
		 * @return {@code true} если страницы были успешно разрешены и обновлены
		 */
	public static boolean resolveInStack(
			ItemStack stack,
			ServerCommandSource commandSource,
			@Nullable PlayerEntity player
	) {
		WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
		if (content == null || content.resolved()) {
			return false;
		}

		WrittenBookContentComponent resolved = content.resolve(commandSource, player);
		if (resolved != null) {
			stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, resolved);
			return true;
		}

		stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content.asResolved());
		return false;
	}

	/**
		 * Разрешает все страницы книги, подставляя значения команд и переменных через {@code source}.
		 * Возвращает {@code null} если книга уже разрешена или хотя бы одна страница не поддаётся разрешению.
		 *
		 * @param source источник команды
		 * @param player игрок-контекст (может быть null)
		 * @return новый компонент с разрешёнными страницами, или {@code null}
		 */
	public @Nullable WrittenBookContentComponent resolve(ServerCommandSource source, @Nullable PlayerEntity player) {
		if (resolved) {
			return null;
		}

		Builder<RawFilteredPair<Text>> builder = ImmutableList.builderWithExpectedSize(pages.size());

		for (RawFilteredPair<Text> page : pages) {
			Optional<RawFilteredPair<Text>> resolvedPage = resolvePage(source, player, page);
			if (resolvedPage.isEmpty()) {
				return null;
			}

			builder.add(resolvedPage.get());
		}

		return new WrittenBookContentComponent(title, author, generation, builder.build(), true);
	}

	public WrittenBookContentComponent asResolved() {
		return new WrittenBookContentComponent(title, author, generation, pages, true);
	}

	private static Optional<RawFilteredPair<Text>> resolvePage(
			ServerCommandSource source,
			@Nullable PlayerEntity player,
			RawFilteredPair<Text> page
	) {
		return page.resolve(text -> {
			try {
				Text parsed = Texts.parse(source, text, player, 0);
				return exceedsSerializedLengthLimit(parsed, source.getRegistryManager())
						? Optional.empty()
						: Optional.of(parsed);
			}
			catch (Exception e) {
				return Optional.of(text);
			}
		});
	}

	private static boolean exceedsSerializedLengthLimit(Text text, RegistryWrapper.WrapperLookup registries) {
		DataResult<JsonElement> dataResult = TextCodecs.CODEC.encodeStart(registries.getOps(JsonOps.INSTANCE), text);
		return dataResult.isSuccess() && JsonHelper.isTooLarge(dataResult.getOrThrow(), MAX_SERIALIZED_PAGE_LENGTH);
	}

	public List<Text> getPages(boolean shouldFilter) {
		return Lists.transform(pages, page -> page.get(shouldFilter));
	}

	public WrittenBookContentComponent withPages(List<RawFilteredPair<Text>> newPages) {
		return new WrittenBookContentComponent(title, author, generation, newPages, false);
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (!StringHelper.isBlank(author)) {
			textConsumer.accept(Text.translatable("book.byAuthor", author).formatted(Formatting.GRAY));
		}

		textConsumer.accept(Text.translatable("book.generation." + generation).formatted(Formatting.GRAY));
	}
}
