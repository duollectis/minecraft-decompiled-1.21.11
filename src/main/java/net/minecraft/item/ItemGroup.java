package net.minecraft.item;

import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Группа предметов в меню творческого режима.
 * <p>Каждая группа имеет иконку, отображаемое имя, тип и набор предметов.
 * Создаётся через {@link Builder}.</p>
 */
public class ItemGroup {

	static final Identifier ITEMS = getTabTextureId("items");

	private final Text displayName;
	Identifier texture = ITEMS;
	boolean scrollbar = true;
	boolean renderName = true;
	boolean special = false;

	private final ItemGroup.Row row;
	private final int column;
	private final ItemGroup.Type type;
	private @Nullable ItemStack icon;
	private Collection<ItemStack> displayStacks = ItemStackSet.create();
	private Set<ItemStack> searchTabStacks = ItemStackSet.create();
	private final Supplier<ItemStack> iconSupplier;
	private final ItemGroup.EntryCollector entryCollector;

	ItemGroup(
			ItemGroup.Row row,
			int column,
			ItemGroup.Type type,
			Text displayName,
			Supplier<ItemStack> iconSupplier,
			ItemGroup.EntryCollector entryCollector
	) {
		this.row = row;
		this.column = column;
		this.displayName = displayName;
		this.iconSupplier = iconSupplier;
		this.entryCollector = entryCollector;
		this.type = type;
	}

	public static Identifier getTabTextureId(String name) {
		return Identifier.ofVanilla("textures/gui/container/creative_inventory/tab_" + name + ".png");
	}

	public static ItemGroup.Builder create(ItemGroup.Row location, int column) {
		return new ItemGroup.Builder(location, column);
	}

	public Text getDisplayName() {
		return displayName;
	}

	public ItemStack getIcon() {
		if (icon == null) {
			icon = iconSupplier.get();
		}

		return icon;
	}

	public Identifier getTexture() {
		return texture;
	}

	public boolean shouldRenderName() {
		return renderName;
	}

	public boolean hasScrollbar() {
		return scrollbar;
	}

	public int getColumn() {
		return column;
	}

	public ItemGroup.Row getRow() {
		return row;
	}

	public boolean hasStacks() {
		return !displayStacks.isEmpty();
	}

	/**
	 * Определяет, должна ли группа отображаться в меню творческого режима.
	 * <p>Группы типа {@link Type#CATEGORY} отображаются только если содержат предметы.</p>
	 *
	 * @return {@code true} если группу следует показать
	 */
	public boolean shouldDisplay() {
		return type != ItemGroup.Type.CATEGORY || hasStacks();
	}

	public boolean isSpecial() {
		return special;
	}

	public ItemGroup.Type getType() {
		return type;
	}

	/**
	 * Обновляет список отображаемых предметов группы с учётом активных фич и прав игрока.
	 *
	 * @param displayContext контекст отображения с активными фичами и реестрами
	 */
	public void updateEntries(ItemGroup.DisplayContext displayContext) {
		ItemGroup.EntriesImpl entries = new ItemGroup.EntriesImpl(this, displayContext.enabledFeatures());
		Registries.ITEM_GROUP
				.getKey(this)
				.orElseThrow(() -> new IllegalStateException("Unregistered creative tab: " + this));
		entryCollector.accept(displayContext, entries);
		displayStacks = entries.parentTabStacks;
		searchTabStacks = entries.searchTabStacks;
	}

	public Collection<ItemStack> getDisplayStacks() {
		return displayStacks;
	}

	public Collection<ItemStack> getSearchTabStacks() {
		return searchTabStacks;
	}

	public boolean contains(ItemStack stack) {
		return searchTabStacks.contains(stack);
	}

	/**
	 * Билдер для создания групп предметов творческого режима.
	 */
	public static class Builder {

		private static final ItemGroup.EntryCollector EMPTY_ENTRIES = (displayContext, entries) -> {};

		private final ItemGroup.Row row;
		private final int column;
		private Text displayName = Text.empty();
		private Supplier<ItemStack> iconSupplier = () -> ItemStack.EMPTY;
		private ItemGroup.EntryCollector entryCollector = EMPTY_ENTRIES;
		private boolean scrollbar = true;
		private boolean renderName = true;
		private boolean special = false;
		private ItemGroup.Type type = ItemGroup.Type.CATEGORY;
		private Identifier texture = ItemGroup.ITEMS;

		public Builder(ItemGroup.Row row, int column) {
			this.row = row;
			this.column = column;
		}

		public ItemGroup.Builder displayName(Text displayName) {
			this.displayName = displayName;
			return this;
		}

		public ItemGroup.Builder icon(Supplier<ItemStack> iconSupplier) {
			this.iconSupplier = iconSupplier;
			return this;
		}

		public ItemGroup.Builder entries(ItemGroup.EntryCollector entryCollector) {
			this.entryCollector = entryCollector;
			return this;
		}

		public ItemGroup.Builder special() {
			special = true;
			return this;
		}

		public ItemGroup.Builder noRenderedName() {
			renderName = false;
			return this;
		}

		public ItemGroup.Builder noScrollbar() {
			scrollbar = false;
			return this;
		}

		protected ItemGroup.Builder type(ItemGroup.Type type) {
			this.type = type;
			return this;
		}

		public ItemGroup.Builder texture(Identifier texture) {
			this.texture = texture;
			return this;
		}

		/**
		 * Создаёт группу предметов с заданными параметрами.
		 * <p>Специальные группы ({@link Type#HOTBAR}, {@link Type#INVENTORY}) не могут
		 * иметь собственных предметов.</p>
		 *
		 * @return новая группа предметов
		 * @throws IllegalStateException если специальная группа имеет коллектор предметов
		 */
		public ItemGroup build() {
			if ((type == ItemGroup.Type.HOTBAR || type == ItemGroup.Type.INVENTORY)
					&& entryCollector != EMPTY_ENTRIES) {
				throw new IllegalStateException("Special tabs can't have display items");
			}

			ItemGroup group = new ItemGroup(row, column, type, displayName, iconSupplier, entryCollector);
			group.special = special;
			group.renderName = renderName;
			group.scrollbar = scrollbar;
			group.texture = texture;
			return group;
		}
	}

	/**
	 * Контекст отображения группы предметов: активные фичи, права и реестры.
	 */
	public record DisplayContext(
			FeatureSet enabledFeatures,
			boolean hasPermissions,
			RegistryWrapper.WrapperLookup lookup
	) {

		public boolean doesNotMatch(
				FeatureSet enabledFeatures,
				boolean hasPermissions,
				RegistryWrapper.WrapperLookup registries
		) {
			return !this.enabledFeatures.equals(enabledFeatures)
					|| this.hasPermissions != hasPermissions
					|| this.lookup != registries;
		}
	}

	/**
	 * Интерфейс для добавления предметов в группу творческого режима.
	 */
	public interface Entries {

		void add(ItemStack stack, ItemGroup.StackVisibility visibility);

		default void add(ItemStack stack) {
			add(stack, ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}

		default void add(ItemConvertible item, ItemGroup.StackVisibility visibility) {
			add(new ItemStack(item), visibility);
		}

		default void add(ItemConvertible item) {
			add(new ItemStack(item), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}

		default void addAll(Collection<ItemStack> stacks, ItemGroup.StackVisibility visibility) {
			stacks.forEach(stack -> add(stack, visibility));
		}

		default void addAll(Collection<ItemStack> stacks) {
			addAll(stacks, ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	static class EntriesImpl implements ItemGroup.Entries {

		public final Collection<ItemStack> parentTabStacks = ItemStackSet.create();
		public final Set<ItemStack> searchTabStacks = ItemStackSet.create();

		private final ItemGroup group;
		private final FeatureSet enabledFeatures;

		public EntriesImpl(ItemGroup group, FeatureSet enabledFeatures) {
			this.group = group;
			this.enabledFeatures = enabledFeatures;
		}

		@Override
		public void add(ItemStack stack, ItemGroup.StackVisibility visibility) {
			if (stack.getCount() != 1) {
				throw new IllegalArgumentException("Stack size must be exactly 1");
			}

			boolean isDuplicate = parentTabStacks.contains(stack)
					&& visibility != ItemGroup.StackVisibility.SEARCH_TAB_ONLY;
			if (isDuplicate) {
				throw new IllegalStateException(
						"Accidentally adding the same item stack twice "
								+ stack.toHoverableText().getString()
								+ " to a Creative Mode Tab: "
								+ group.getDisplayName().getString()
				);
			}

			if (!stack.getItem().isEnabled(enabledFeatures)) {
				return;
			}

			switch (visibility) {
				case PARENT_AND_SEARCH_TABS -> {
					parentTabStacks.add(stack);
					searchTabStacks.add(stack);
				}
				case PARENT_TAB_ONLY -> parentTabStacks.add(stack);
				case SEARCH_TAB_ONLY -> searchTabStacks.add(stack);
			}
		}
	}

	@FunctionalInterface
	public interface EntryCollector {

		void accept(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries);
	}

	public enum Row {
		TOP,
		BOTTOM
	}

	public enum StackVisibility {
		PARENT_AND_SEARCH_TABS,
		PARENT_TAB_ONLY,
		SEARCH_TAB_ONLY
	}

	public enum Type {
		CATEGORY,
		INVENTORY,
		HOTBAR,
		SEARCH
	}
}
