package com.hamxlate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranslationController {
    private final Map<String, CsvFormat> formats;

    public TranslationController(List<CsvFormat> formats) {
        this.formats = formats.stream().collect(Collectors.toUnmodifiableMap(CsvFormat::id, Function.identity()));
    }

    @GetMapping("/api/formats")
    public List<String> formats() {
        return formats.keySet().stream().sorted().toList();
    }

    @PostMapping(value = "/api/translations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> translate(@RequestParam("file") MultipartFile file,
            @RequestParam("inputFormat") String inputFormat, @RequestParam("outputFormat") String outputFormat)
            throws IOException {
        CsvFormat input = findFormat(inputFormat);
        CsvFormat output = findFormat(outputFormat);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        List<Channel> channels;
        try (PushbackReader reader = new PushbackReader(new InputStreamReader(file.getInputStream(),
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)))) {
            int first = reader.read();
            if (first != -1 && first != '\uFEFF') {
                reader.unread(first);
            }
            channels = input.read(reader);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid UTF-8 or malformed CSV file", ex);
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        output.write(channels, new OutputStreamWriter(bytes, StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("translated.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes.toByteArray());
    }

    private CsvFormat findFormat(String id) {
        CsvFormat format = formats.get(id);
        if (format == null) {
            throw new IllegalArgumentException("Unknown CSV format: " + id);
        }
        return format;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalidInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ex.getMessage());
    }
}

@Configuration
class CsvFormats {
    @Bean
    CsvFormat nameFirst() {
        return new ColumnCsvFormat("name-first", List.of("Name", "Frequency"));
    }

    @Bean
    CsvFormat frequencyFirst() {
        return new ColumnCsvFormat("frequency-first", List.of("Frequency", "Name"));
    }
}
