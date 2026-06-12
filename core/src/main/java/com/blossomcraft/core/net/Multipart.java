package com.blossomcraft.core.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Minimal {@code multipart/form-data} body builder for file uploads
 * (music tracks, video chunks, avatars) — matching the {@code FormData}
 * the website posts to {@code music.php}, {@code videos.php}, etc.
 */
public final class Multipart {

    /** A single file part (field name, filename, content type, bytes). */
    public static final class FilePart {
        final String field;
        final String filename;
        final String contentType;
        final byte[] data;

        public FilePart(String field, String filename, String contentType, byte[] data) {
            this.field = field;
            this.filename = filename;
            this.contentType = contentType == null ? "application/octet-stream" : contentType;
            this.data = data;
        }
    }

    private final String boundary = "----BlossomCraft" + Long.toHexString(System.nanoTime());
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public String boundary() {
        return boundary;
    }

    public Multipart addField(String name, String value) {
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        write(value == null ? "" : value);
        write("\r\n");
        return this;
    }

    public Multipart addFile(FilePart part) {
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + part.field
                + "\"; filename=\"" + part.filename + "\"\r\n");
        write("Content-Type: " + part.contentType + "\r\n\r\n");
        writeBytes(part.data);
        write("\r\n");
        return this;
    }

    public byte[] build() {
        write("--" + boundary + "--\r\n");
        return buffer.toByteArray();
    }

    private void write(String text) {
        writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeBytes(byte[] bytes) {
        try {
            buffer.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
