package com.learnova.elearning.integration.storage;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * §7.5 design_us15_us17.md — VideoDurationProbe không được tin duration client
 * khai, phải tự đọc box {@code moov/mvhd}. Kiểm tra bằng file MP4 tối giản dựng
 * tay (chỉ {@code ftyp} + {@code moov/mvhd}), không cần asset video thật.
 */
class LocalVideoDurationProbeTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storage;
    private LocalVideoDurationProbe probe;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setRootDir(tempDir.toString());
        LocalStorageSigner signer = new LocalStorageSigner(properties);
        storage = new LocalStorageService(properties, signer);
        probe = new LocalVideoDurationProbe(storage);
    }

    @Test
    @DisplayName("Đọc đúng thời lượng từ box mvhd, không phụ thuộc client")
    void probeSeconds_readsDurationFromMvhd() throws IOException {
        byte[] mp4 = minimalMp4(1000, 125_000); // timescale=1000, duration=125000 -> 125s
        storage.store("videos/lesson-1.mp4", new ByteArrayInputStream(mp4), "video/mp4");

        int seconds = probe.probeSeconds("videos/lesson-1.mp4");

        assertThat(seconds).isEqualTo(125);
    }

    @Test
    @DisplayName("Làm tròn đúng khi timescale không chia hết duration")
    void probeSeconds_roundsFractionalSeconds() throws IOException {
        byte[] mp4 = minimalMp4(3, 100); // 100/3 = 33.33s -> làm tròn 33
        storage.store("videos/lesson-2.mp4", new ByteArrayInputStream(mp4), "video/mp4");

        int seconds = probe.probeSeconds("videos/lesson-2.mp4");

        assertThat(seconds).isEqualTo(33);
    }

    @Test
    @DisplayName("Object không tồn tại trên storage -> UPLOAD_OBJECT_NOT_FOUND")
    void probeSeconds_missingObject_throwsNotFound() {
        assertThatThrownBy(() -> probe.probeSeconds("videos/missing.mp4"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UPLOAD_OBJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("File không phải MP4 hợp lệ (đổi đuôi giả) -> UPLOAD_METADATA_MISMATCH")
    void probeSeconds_notAnMp4_throwsMetadataMismatch() {
        storage.store("videos/fake.mp4",
                new ByteArrayInputStream("this is not an mp4 file".getBytes(StandardCharsets.UTF_8)),
                "video/mp4");

        assertThatThrownBy(() -> probe.probeSeconds("videos/fake.mp4"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UPLOAD_METADATA_MISMATCH);
    }

    /** Dựng MP4 tối giản: box ftyp + box moov chứa đúng box mvhd (version 0). */
    private static byte[] minimalMp4(long timescale, long duration) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);

        writeFtypBox(data);
        writeMoovBox(data, timescale, duration);

        return out.toByteArray();
    }

    private static void writeFtypBox(DataOutputStream data) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(body);
        b.writeBytes("isom");   // major_brand
        b.writeInt(0);          // minor_version
        b.writeBytes("isom");   // compatible_brands[0]

        writeBox(data, "ftyp", body.toByteArray());
    }

    private static void writeMoovBox(DataOutputStream data, long timescale, long duration) throws IOException {
        ByteArrayOutputStream mvhdBody = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(mvhdBody);

        b.writeInt(0);                  // version(1) + flags(3), version 0
        b.writeInt(0);                  // creation_time
        b.writeInt(0);                  // modification_time
        b.writeInt((int) timescale);    // timescale
        b.writeInt((int) duration);     // duration
        b.writeInt(0x00010000);         // rate = 1.0
        b.writeShort(0x0100);           // volume = 1.0
        b.writeShort(0);                // reserved
        b.writeInt(0);                  // reserved[0]
        b.writeInt(0);                  // reserved[1]
        // unity transform matrix
        int[] matrix = {0x00010000, 0, 0, 0, 0x00010000, 0, 0, 0, 0x40000000};
        for (int v : matrix) {
            b.writeInt(v);
        }
        for (int i = 0; i < 6; i++) {
            b.writeInt(0);              // pre_defined
        }
        b.writeInt(2);                  // next_track_ID

        ByteArrayOutputStream mvhdBox = new ByteArrayOutputStream();
        writeBox(new DataOutputStream(mvhdBox), "mvhd", mvhdBody.toByteArray());

        writeBox(data, "moov", mvhdBox.toByteArray());
    }

    private static void writeBox(DataOutputStream data, String type, byte[] body) throws IOException {
        data.writeInt(body.length + 8);
        data.writeBytes(type);
        data.write(body);
    }
}
