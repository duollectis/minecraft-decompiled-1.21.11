package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;

public class ReaderBackedParsingState extends ParsingStateImpl<StringReader> {
   private final StringReader reader;

   public ReaderBackedParsingState(ParseErrorList<StringReader> errors, StringReader reader) {
      super(errors);
      this.reader = reader;
   }

   public StringReader getReader() {
      return this.reader;
   }

   @Override
   public int getCursor() {
      return this.reader.getCursor();
   }

   @Override
   public void setCursor(int cursor) {
      this.reader.setCursor(cursor);
   }
}
