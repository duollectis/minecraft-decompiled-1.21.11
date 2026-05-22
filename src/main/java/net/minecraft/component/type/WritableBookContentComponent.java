package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.RawFilteredPair;

import java.util.List;
import java.util.stream.Stream;

/**
	 * Компонент содержимого редактируемой книги (книга и перо). Хранит страницы в виде
	 * отфильтрованных пар строк с ограничением по длине и количеству.
	 */
public record WritableBookContentComponent(List<RawFilteredPair<String>> pages) implements BookContent<String, WritableBookContentComponent> {

	public static final WritableBookContentComponent DEFAULT = new WritableBookContentComponent(List.of());
	public static final int MAX_PAGE_LENGTH = 1024;
	public static final int MAX_PAGE_COUNT = 100;
	private static final Codec<RawFilteredPair<String>> PAGE_CODEC = RawFilteredPair.createCodec(Codec.string(0, MAX_PAGE_LENGTH));
	public static final Codec<List<RawFilteredPair<String>>> PAGES_CODEC = PAGE_CODEC.sizeLimitedListOf(MAX_PAGE_COUNT);
	public static final Codec<WritableBookContentComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(PAGES_CODEC
							.optionalFieldOf("pages", List.of())
							.forGetter(WritableBookContentComponent::pages))
					.apply(instance, WritableBookContentComponent::new)
	);
	public static final PacketCodec<ByteBuf, WritableBookContentComponent>
			PACKET_CODEC =
			RawFilteredPair.createPacketCodec(PacketCodecs.string(MAX_PAGE_LENGTH))
							.collect(PacketCodecs.toList(MAX_PAGE_COUNT))
							.xmap(WritableBookContentComponent::new, WritableBookContentComponent::pages);

	public WritableBookContentComponent(List<RawFilteredPair<String>> pages) {
		if (pages.size() > MAX_PAGE_COUNT) {
			throw new IllegalArgumentException(
				"Got " + pages.size() + " pages, but maximum is " + MAX_PAGE_COUNT
			);
		}

		this.pages = pages;
	}

	public Stream<String> stream(boolean shouldFilter) {
		return pages.stream().map(page -> page.get(shouldFilter));
	}

	public WritableBookContentComponent withPages(List<RawFilteredPair<String>> newPages) {
		return new WritableBookContentComponent(newPages);
	}
}
