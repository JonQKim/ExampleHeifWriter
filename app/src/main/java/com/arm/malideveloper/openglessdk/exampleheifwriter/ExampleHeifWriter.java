package com.arm.malideveloper.openglessdk.exampleheifwriter;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.heifwriter.HeifWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class ExampleHeifWriter {

    private static final int DEFAULT_QUALITY = 90;

    private ExampleHeifWriter() {
        // Utility class
    }

    public static byte[] encodeBitmapToHeifBitstream(Context context, Bitmap bitmap) throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap must not be null");
        }

        File outputFile = File.createTempFile("heifwriter_", ".heif", context.getCacheDir());
        try {
            try (HeifWriter heifWriter = new HeifWriter.Builder(
                    outputFile.getAbsolutePath(),
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    HeifWriter.INPUT_MODE_BITMAP)
                    .setQuality(DEFAULT_QUALITY)
                    .build()) {
                heifWriter.start();
                heifWriter.addBitmap(bitmap);
                try {
                    heifWriter.stop(0);
                } catch (Exception e) {
                    throw new IOException("Failed to stop HeifWriter", e);
                }
            }
            return Files.readAllBytes(outputFile.toPath());
        } finally {
            if (!outputFile.delete() && outputFile.exists()) {
                // No-op: best effort cleanup.
            }
        }
    }
}
