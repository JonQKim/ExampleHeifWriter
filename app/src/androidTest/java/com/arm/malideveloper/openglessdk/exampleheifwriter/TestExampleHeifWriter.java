package com.arm.malideveloper.openglessdk.exampleheifwriter;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class TestExampleHeifWriter {
    private static final String TAG = "TestExampleHeifWriter";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.arm.malideveloper.openglessdk.exampleheifwriter", appContext.getPackageName());
    }

    @Test
    public void encodeBitmapToHeifBitstream_writesOutputFile() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context targetContext = instrumentation.getTargetContext();
        Context testContext = instrumentation.getContext();

        Bitmap bitmap;
        try (InputStream inputStream = testContext.getAssets().open("Cat03.jpg")) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        }
        assertNotNull("Failed to decode Cat03.jpg to Bitmap", bitmap);

        byte[] heifData = ExampleHeifWriter.encodeBitmapToHeifBitstream(targetContext, bitmap);
        assertNotNull("HEIF data must not be null", heifData);
        assertTrue("HEIF data must not be empty", heifData.length > 0);

        ContentResolver resolver = targetContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Cat03_test_output_" + UUID.randomUUID() + ".heif");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/heif");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ExampleHeifWriter");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri itemUri = resolver.insert(collectionUri, values);
        assertNotNull("Failed to create entry in Downloads collection", itemUri);

        try (OutputStream outputStream = resolver.openOutputStream(itemUri)) {
            assertNotNull("Failed to open output stream for Downloads entry", outputStream);
            outputStream.write(heifData);
            outputStream.flush();
        }

        ContentValues pendingUpdate = new ContentValues();
        pendingUpdate.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(itemUri, pendingUpdate, null, null);

        long recordedSize = querySize(resolver, itemUri);
        assertTrue("Stored HEIF entry should report a size", recordedSize > 0);

        Log.i(TAG, "Saved HEIF to Downloads. Uri=" + itemUri + ", size=" + recordedSize + " bytes");
    }

    private long querySize(ContentResolver resolver, Uri uri) {
        long recordedSize = 0L;
        try (android.database.Cursor cursor = resolver.query(uri, new String[]{MediaStore.MediaColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                if (sizeIndex != -1) {
                    recordedSize = cursor.getLong(sizeIndex);
                }
            }
        }
        return recordedSize;
    }
}
