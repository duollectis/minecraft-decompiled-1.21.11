package net.minecraft.client.gui.screen.dialog;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.dialog.type.ConfirmationDialog;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogListDialog;
import net.minecraft.dialog.type.MultiActionDialog;
import net.minecraft.dialog.type.NoticeDialog;
import net.minecraft.dialog.type.ServerLinksDialog;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DialogScreens {
   private static final Map<MapCodec<? extends Dialog>, DialogScreens.Factory<?>> DIALOG_SCREEN_FACTORIES = new HashMap<>();

   private static <T extends Dialog> void register(MapCodec<T> dialogCodec, DialogScreens.Factory<? super T> factory) {
      DIALOG_SCREEN_FACTORIES.put(dialogCodec, factory);
   }

   public static <T extends Dialog> @Nullable DialogScreen<T> create(T dialog, @Nullable Screen parent, DialogNetworkAccess networkAccess) {
      DialogScreens.Factory<T> factory = (DialogScreens.Factory<T>)DIALOG_SCREEN_FACTORIES.get(dialog.getCodec());
      return factory != null ? factory.create(parent, dialog, networkAccess) : null;
   }

   public static void bootstrap() {
      register(ConfirmationDialog.CODEC, SimpleDialogScreen::new);
      register(NoticeDialog.CODEC, SimpleDialogScreen::new);
      register(DialogListDialog.CODEC, DialogListDialogScreen::new);
      register(MultiActionDialog.CODEC, MultiActionDialogScreen::new);
      register(ServerLinksDialog.CODEC, ServerLinksDialogScreen::new);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Factory<T extends Dialog> {
      DialogScreen<T> create(@Nullable Screen parent, T dialog, DialogNetworkAccess networkAccess);
   }
}
