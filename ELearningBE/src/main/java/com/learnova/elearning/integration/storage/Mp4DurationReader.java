package com.learnova.elearning.integration.storage;

import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.mp4parser.boxes.iso14496.part12.MovieHeaderBox;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * Đọc box {@code moov/mvhd} của MP4 để lấy thời lượng thật — dùng chung cho
 * {@link LocalVideoDurationProbe} và {@link S3VideoDurationProbe} (§7.5).
 * <p>
 * {@link IsoFile} chỉ đọc box header tuần tự và nhảy qua ({@code position()})
 * những box không cần (vd. {@code mdat} khổng lồ đứng trước {@code moov} khi
 * video không "faststart") — không cần tải hết file.
 */
final class Mp4DurationReader {

    private Mp4DurationReader() {
    }

    static int readDurationSeconds(SeekableByteChannel channel) throws IOException {
        try (IsoFile isoFile = new IsoFile(channel)) {
            MovieBox moov = isoFile.getBoxes(MovieBox.class).stream()
                    .findFirst()
                    .orElseThrow(() -> new IOException("MP4 'moov' box not found"));
            MovieHeaderBox mvhd = moov.getBoxes(MovieHeaderBox.class).stream()
                    .findFirst()
                    .orElseThrow(() -> new IOException("MP4 'mvhd' box not found"));

            long timescale = mvhd.getTimescale();
            if (timescale <= 0) {
                throw new IOException("MP4 'mvhd' box has invalid timescale");
            }
            double seconds = (double) mvhd.getDuration() / timescale;
            return (int) Math.round(seconds);
        }
    }
}
