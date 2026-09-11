package com.learnova.elearning.integration.storage;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/** Reads only box headers and mvhd; never materializes mdat or an entire moov. */
final class Mp4DurationReader {
    private Mp4DurationReader() {}

    static int readDurationSeconds(SeekableByteChannel channel) throws IOException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(60);
        return scan(channel, channel.size(), false, deadline);
    }

    private static int scan(SeekableByteChannel channel, long end, boolean inMovie, long deadline) throws IOException {
        int boxes = 0;
        while (channel.position() < end) {
            if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline)
                throw new java.io.InterruptedIOException("Video metadata deadline exceeded");
            if (++boxes > 100_000) throw new IOException("Too many MP4 boxes");
            long start = channel.position();
            if (end - start < 8) throw new EOFException("Truncated MP4 header");
            ByteBuffer header = read(channel, 8);
            long size = Integer.toUnsignedLong(header.getInt());
            int type = header.getInt();
            int headerSize = 8;
            if (size == 1) { size = read(channel, 8).getLong(); headerSize = 16; }
            else if (size == 0) size = end - start;
            if (size < headerSize || size > end - start) throw new IOException("Invalid MP4 box size");
            long boxEnd = start + size;
            if (type == 0x6d6f6f76 && !inMovie) return scan(channel, boxEnd, true, deadline); // moov
            if (type == 0x6d766864 && inMovie) { // mvhd full box
                if (boxEnd - channel.position() < 4) throw new EOFException("Truncated mvhd");
                int version = Byte.toUnsignedInt(read(channel, 4).get());
                int length = version == 0 ? 16 : version == 1 ? 28 : -1;
                if (length < 0 || boxEnd - channel.position() < length) throw new IOException("Invalid mvhd");
                ByteBuffer body = read(channel, length);
                body.position(version == 0 ? 8 : 16);
                long scale = Integer.toUnsignedLong(body.getInt());
                long duration = version == 0 ? Integer.toUnsignedLong(body.getInt()) : body.getLong();
                double seconds = scale == 0 ? 0 : (double) duration / scale;
                if (duration <= 0 || (version == 0 && duration == 0xffffffffL) || !Double.isFinite(seconds)
                        || seconds <= 0 || seconds > Integer.MAX_VALUE) throw new IOException("Invalid video duration");
                return Math.max(1, (int) Math.round(seconds));
            }
            channel.position(boxEnd);
        }
        throw new IOException("MP4 movie duration not found");
    }

    private static ByteBuffer read(SeekableByteChannel channel, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException();
            if (channel.read(buffer) <= 0) throw new EOFException("Truncated MP4");
        }
        return buffer.flip();
    }
}
