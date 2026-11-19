package com.hamxlate;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

final class ColumnCsvFormat implements CsvFormat {
    private final String id;
    private final List<String> columns;

    ColumnCsvFormat(String id, List<String> columns) {
        this.id = id;
        this.columns = List.copyOf(columns);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<Channel> read(Reader reader) throws IOException {
        CSVFormat format = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get();
        try (CSVParser parser = format.parse(reader)) {
            if (!parser.getHeaderNames().equals(columns)) {
                throw new IllegalArgumentException("Expected CSV headers: " + String.join(",", columns));
            }
            List<Channel> channels = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new IllegalArgumentException("CSV row has the wrong number of columns");
                }
                channels.add(new Channel(record.get("Name"), record.get("Frequency")));
            }
            return channels;
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    @Override
    public void write(List<Channel> channels, Writer writer) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180)) {
            printer.printRecord(columns);
            for (Channel channel : channels) {
                for (String column : columns) {
                    printer.print(switch (column) {
                        case "Name" -> channel.name();
                        case "Frequency" -> channel.frequency();
                        default -> throw new IllegalStateException("Unsupported column: " + column);
                    });
                }
                printer.println();
            }
        }
    }
}
