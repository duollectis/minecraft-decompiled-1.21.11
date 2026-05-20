package net.minecraft.client.gui.screen.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogButtonData;
import net.minecraft.dialog.action.SimpleDialogAction;
import net.minecraft.dialog.type.ServerLinksDialog;
import net.minecraft.server.ServerLinks;
import net.minecraft.text.ClickEvent;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ServerLinksDialogScreen extends ColumnsDialogScreen<ServerLinksDialog> {
   public ServerLinksDialogScreen(@Nullable Screen parent, ServerLinksDialog dialog, DialogNetworkAccess networkAccess) {
      super(parent, dialog, networkAccess);
   }

   protected Stream<DialogActionButtonData> streamActionButtonData(ServerLinksDialog serverLinksDialog, DialogNetworkAccess dialogNetworkAccess) {
      return dialogNetworkAccess.getServerLinks().entries().stream().map(entry -> createButton(serverLinksDialog, entry));
   }

   private static DialogActionButtonData createButton(ServerLinksDialog dialog, ServerLinks.Entry entry) {
      return new DialogActionButtonData(
         new DialogButtonData(entry.getText(), dialog.buttonWidth()), Optional.of(new SimpleDialogAction(new ClickEvent.OpenUrl(entry.link())))
      );
   }
}
