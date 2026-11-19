package com.hamxlate;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

public interface CsvFormat {
    String id();

    List<Channel> read(Reader reader) throws IOException;

    void write(List<Channel> channels, Writer writer) throws IOException;
}
