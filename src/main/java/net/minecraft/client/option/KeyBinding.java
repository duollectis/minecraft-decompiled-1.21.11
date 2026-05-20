package net.minecraft.client.option;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class KeyBinding implements Comparable<KeyBinding> {
   private static final Map<String, KeyBinding> KEYS_BY_ID = Maps.newHashMap();
   private static final Map<InputUtil.Key, List<KeyBinding>> KEY_TO_BINDINGS = Maps.newHashMap();
   private final String id;
   private final InputUtil.Key defaultKey;
   private final KeyBinding.Category category;
   public InputUtil.Key boundKey;
   private boolean pressed;
   private int timesPressed;
   private final int field_63464;

   public static void onKeyPressed(InputUtil.Key key) {
      forAllKeyBinds(key, keyx -> keyx.timesPressed++);
   }

   public static void setKeyPressed(InputUtil.Key key, boolean pressed) {
      forAllKeyBinds(key, keyx -> keyx.setPressed(pressed));
   }

   private static void forAllKeyBinds(InputUtil.Key key, Consumer<KeyBinding> keyConsumer) {
      List<KeyBinding> list = KEY_TO_BINDINGS.get(key);
      if (list != null && !list.isEmpty()) {
         for (KeyBinding keyBinding : list) {
            keyConsumer.accept(keyBinding);
         }
      }
   }

   public static void updatePressedStates() {
      Window window = MinecraftClient.getInstance().getWindow();

      for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
         if (keyBinding.shouldSetOnGameFocus()) {
            keyBinding.setPressed(InputUtil.isKeyPressed(window, keyBinding.boundKey.getCode()));
         }
      }
   }

   public static void unpressAll() {
      for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
         keyBinding.reset();
      }
   }

   public static void restoreToggleStates() {
      for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
         if (keyBinding instanceof StickyKeyBinding stickyKeyBinding && stickyKeyBinding.shouldRestoreOnScreenClose()) {
            stickyKeyBinding.setPressed(true);
         }
      }
   }

   public static void untoggleStickyKeys() {
      for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
         if (keyBinding instanceof StickyKeyBinding stickyKeyBinding) {
            stickyKeyBinding.untoggle();
         }
      }
   }

   public static void updateKeysByCode() {
      KEY_TO_BINDINGS.clear();

      for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
         keyBinding.registerBinding(keyBinding.boundKey);
      }
   }

   public KeyBinding(String id, int code, KeyBinding.Category category) {
      this(id, InputUtil.Type.KEYSYM, code, category);
   }

   public KeyBinding(String string, InputUtil.Type type, int i, KeyBinding.Category category) {
      this(string, type, i, category, 0);
   }

   public KeyBinding(String id, InputUtil.Type type, int code, KeyBinding.Category category, int i) {
      this.id = id;
      this.boundKey = type.createFromCode(code);
      this.defaultKey = this.boundKey;
      this.category = category;
      this.field_63464 = i;
      KEYS_BY_ID.put(id, this);
      this.registerBinding(this.boundKey);
   }

   public boolean isPressed() {
      return this.pressed;
   }

   public KeyBinding.Category getCategory() {
      return this.category;
   }

   public boolean wasPressed() {
      if (this.timesPressed == 0) {
         return false;
      } else {
         this.timesPressed--;
         return true;
      }
   }

   protected void reset() {
      this.timesPressed = 0;
      this.setPressed(false);
   }

   protected boolean shouldSetOnGameFocus() {
      return this.boundKey.getCategory() == InputUtil.Type.KEYSYM && this.boundKey.getCode() != InputUtil.UNKNOWN_KEY.getCode();
   }

   public String getId() {
      return this.id;
   }

   public InputUtil.Key getDefaultKey() {
      return this.defaultKey;
   }

   public void setBoundKey(InputUtil.Key boundKey) {
      this.boundKey = boundKey;
   }

   public int compareTo(KeyBinding keyBinding) {
      if (this.category == keyBinding.category) {
         return this.field_63464 == keyBinding.field_63464
            ? I18n.translate(this.id).compareTo(I18n.translate(keyBinding.id))
            : Integer.compare(this.field_63464, keyBinding.field_63464);
      } else {
         return Integer.compare(KeyBinding.Category.CATEGORIES.indexOf(this.category), KeyBinding.Category.CATEGORIES.indexOf(keyBinding.category));
      }
   }

   public static Supplier<Text> getLocalizedName(String id) {
      KeyBinding keyBinding = KEYS_BY_ID.get(id);
      return keyBinding == null ? () -> Text.translatable(id) : keyBinding::getBoundKeyLocalizedText;
   }

   public boolean equals(KeyBinding other) {
      return this.boundKey.equals(other.boundKey);
   }

   public boolean isUnbound() {
      return this.boundKey.equals(InputUtil.UNKNOWN_KEY);
   }

   public boolean matchesKey(KeyInput key) {
      return key.key() == InputUtil.UNKNOWN_KEY.getCode()
         ? this.boundKey.getCategory() == InputUtil.Type.SCANCODE && this.boundKey.getCode() == key.scancode()
         : this.boundKey.getCategory() == InputUtil.Type.KEYSYM && this.boundKey.getCode() == key.key();
   }

   public boolean matchesMouse(Click click) {
      return this.boundKey.getCategory() == InputUtil.Type.MOUSE && this.boundKey.getCode() == click.button();
   }

   public Text getBoundKeyLocalizedText() {
      return this.boundKey.getLocalizedText();
   }

   public boolean isDefault() {
      return this.boundKey.equals(this.defaultKey);
   }

   public String getBoundKeyTranslationKey() {
      return this.boundKey.getTranslationKey();
   }

   public void setPressed(boolean pressed) {
      this.pressed = pressed;
   }

   private void registerBinding(InputUtil.Key key) {
      KEY_TO_BINDINGS.computeIfAbsent(key, keyx -> new ArrayList<>()).add(this);
   }

   public static @Nullable KeyBinding byId(String id) {
      return KEYS_BY_ID.get(id);
   }

   @Environment(EnvType.CLIENT)
   public record Category(Identifier id) {
      static final List<KeyBinding.Category> CATEGORIES = new ArrayList<>();
      public static final KeyBinding.Category MOVEMENT = create("movement");
      public static final KeyBinding.Category MISC = create("misc");
      public static final KeyBinding.Category MULTIPLAYER = create("multiplayer");
      public static final KeyBinding.Category GAMEPLAY = create("gameplay");
      public static final KeyBinding.Category INVENTORY = create("inventory");
      public static final KeyBinding.Category CREATIVE = create("creative");
      public static final KeyBinding.Category SPECTATOR = create("spectator");
      public static final KeyBinding.Category DEBUG = create("debug");

      private static KeyBinding.Category create(String name) {
         return create(Identifier.ofVanilla(name));
      }

      public static KeyBinding.Category create(Identifier id) {
         KeyBinding.Category category = new KeyBinding.Category(id);
         if (CATEGORIES.contains(category)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Category '%s' is already registered.", id));
         } else {
            CATEGORIES.add(category);
            return category;
         }
      }

      public Text getLabel() {
         return Text.translatable(this.id.toTranslationKey("key.category"));
      }
   }
}
