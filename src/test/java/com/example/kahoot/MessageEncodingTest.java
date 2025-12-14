package com.example.kahoot;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class MessageEncodingTest {

    @Test
    public void encodeDecode_roundtrip() throws Exception {
        String original = "1|Alice & Bob|100;2|Bob|80;";

        String encoded = URLEncoder.encode(original, StandardCharsets.UTF_8.name());
        assertNotNull(encoded);
        assertNotEquals(original, encoded);

        String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
        assertEquals(original, decoded);
    }

    @Test
    public void questionOptionEncoding() throws Exception {
        String question = "What is 1|2?";
        String option = "3|4";

        String qEnc = URLEncoder.encode(question, StandardCharsets.UTF_8.name());
        String oEnc = URLEncoder.encode(option, StandardCharsets.UTF_8.name());

        // Simulate server message: QUESTION|1|<qEnc>|10|5|1|2|101|<oEnc>|102|<oEnc2>|
        String message = "QUESTION|1|" + qEnc + "|10|5|1|2|101|" + oEnc + "|";

        String[] parts = message.split("\\|");
        assertEquals("QUESTION", parts[0]);
        String parsedQ = URLDecoder.decode(parts[2], StandardCharsets.UTF_8.name());
        assertEquals(question, parsedQ);

        String parsedOpt = URLDecoder.decode(parts[8], StandardCharsets.UTF_8.name());
        assertEquals(option, parsedOpt);
    }
}
