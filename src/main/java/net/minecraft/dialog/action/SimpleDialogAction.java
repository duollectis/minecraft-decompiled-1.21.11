package net.minecraft.dialog.action;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Util;

public record SimpleDialogAction(ClickEvent value) implements DialogAction {
   public static final Map<ClickEvent.Action, MapCodec<SimpleDialogAction>> CODECS = Util.make(() -> {
      Map<ClickEvent.Action, MapCodec<SimpleDialogAction>> map = new EnumMap<>(ClickEvent.Action.class);

      for (ClickEvent.Action action : ClickEvent.Action.class.getEnumConstants()) {
         if (action.isUserDefinable()) {
            @SuppressWarnings("unchecked")
            MapCodec<ClickEvent> mapCodec = (MapCodec<ClickEvent>) (MapCodec<?>) action.getCodec();
            map.put(action, mapCodec.xmap(SimpleDialogAction::new, SimpleDialogAction::value));
         }
      }

      return Collections.unmodifiableMap(map);
   });

   @Override
   public MapCodec<SimpleDialogAction> getCodec() {
      return CODECS.get(this.value.getAction());
   }

   @Override
   public Optional<ClickEvent> createClickEvent(Map<String, DialogAction.ValueGetter> valueGetters) {
      return Optional.of(this.value);
   }
}
