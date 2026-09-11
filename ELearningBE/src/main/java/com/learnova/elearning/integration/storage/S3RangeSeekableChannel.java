package com.learnova.elearning.integration.storage;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * {@link SeekableByteChannel} đọc một object S3/R2 bằng các {@code GetObjectRequest}
 * mang header {@code Range} — không tải hết file. Chỉ đọc, không ghi. §7.5.
 */
final class S3RangeSeekableChannel implements SeekableByteChannel {

    private final S3Client s3Client;
    private final String bucket;
    private final String key;
    private final long size;
    private long position = 0;
    private byte[] buffer = new byte[0];
    private long bufferStart = -1;
    private boolean open = true;

    S3RangeSeekableChannel(S3Client s3Client, String bucket, String key) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.size = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
                .contentLength();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();
        if (position >= size) {
            return -1;
        }
        if (!dst.hasRemaining()) return 0;
        if (position < bufferStart || position >= bufferStart + buffer.length) {
            long end = Math.min(position + 65536, size) - 1;
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key)
                    .range("bytes=" + position + "-" + end).build();
            try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(request)) {
                if (in.response().contentRange() == null) {
                    in.abort();
                    throw new IOException("Storage ignored Range request");
                }
                buffer = in.readNBytes((int) (end - position + 1));
                if (buffer.length == 0) throw new IOException("Empty storage range");
                bufferStart = position;
            } catch (UncheckedIOException e) { throw e.getCause(); }
        }
        int offset = (int) (position - bufferStart);
        int count = Math.min(dst.remaining(), buffer.length - offset);
        dst.put(buffer, offset, count);
        position += count;
        return count;
    }

    @Override
    public int write(ByteBuffer src) {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0) {
            throw new IllegalArgumentException("Negative position: " + newPosition);
        }
        this.position = newPosition;
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
    }

    private void ensureOpen() throws ClosedChannelException {
        if (!open) {
            throw new ClosedChannelException();
        }
    }
}
