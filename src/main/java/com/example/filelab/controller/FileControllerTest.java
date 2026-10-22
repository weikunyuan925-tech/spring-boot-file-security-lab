package com.example.filelab.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class FileControllerTest {

    @Test
    void shouldRejectFilenameWithNewline() {
        FileController controller =
                new FileController(null, null, null);

        String result =
                controller.sanitizeOriginalName("bad\nname.pdf");

        Assertions.assertNull(result);
    }

    @Test
    void shouldRejectFilenameWithTab() {
        FileController controller =
                new FileController(null, null, null);

        String result =
                controller.sanitizeOriginalName("bad\tname.pdf");

        Assertions.assertNull(result);
    }
}