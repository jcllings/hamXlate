package com.hamxlate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TranslationControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void listsFormats() throws Exception {
        mvc.perform(get("/api/formats"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"frequency-first\",\"name-first\"]"));
    }

    @Test
    void translatesQuotedFieldsAndUtf8Bom() throws Exception {
        mvc.perform(multipart("/api/translations")
                .file(csv("\uFEFFName,Frequency\r\n\"Hill, north\",146.520\r\n\"Line\nTwo\",145.000\r\n"))
                .param("inputFormat", "name-first").param("outputFormat", "frequency-first"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string("Frequency,Name\r\n146.520,\"Hill, north\"\r\n145.000,\"Line\nTwo\"\r\n"));
    }

    @Test
    void translatesInReverseAndAllowsHeaderOnlyFiles() throws Exception {
        mvc.perform(multipart("/api/translations")
                .file(csv("Frequency,Name\r\n"))
                .param("inputFormat", "frequency-first").param("outputFormat", "name-first"))
                .andExpect(status().isOk())
                .andExpect(content().string("Name,Frequency\r\n"));
    }

    @Test
    void rejectsUnknownFormat() throws Exception {
        mvc.perform(multipart("/api/translations").file(csv("Name,Frequency\n"))
                .param("inputFormat", "unknown").param("outputFormat", "frequency-first"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsWrongHeadersAndInconsistentRows() throws Exception {
        for (String input : new String[] {
                "Frequency,Name\n146.52,Local\n",
                "Name,Frequency,Other\nLocal,146.52,x\n",
                "Name,Frequency\nLocal\n",
                "Name,Frequency\nLocal,146.52,extra\n",
                "Name,Name\nLocal,146.52\n",
                "Name,Frequency\n\"unclosed,146.52"
        }) {
            mvc.perform(multipart("/api/translations").file(csv(input))
                    .param("inputFormat", "name-first").param("outputFormat", "frequency-first"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsEmptyAndInvalidUtf8() throws Exception {
        for (byte[] input : new byte[][] {new byte[0], {(byte) 0xff}}) {
            mvc.perform(multipart("/api/translations")
                    .file(new MockMultipartFile("file", "input.csv", "text/csv", input))
                    .param("inputFormat", "name-first").param("outputFormat", "frequency-first"))
                    .andExpect(status().isBadRequest());
        }
    }

    private MockMultipartFile csv(String contents) {
        return new MockMultipartFile("file", "input.csv", "text/csv", contents.getBytes(StandardCharsets.UTF_8));
    }
}
