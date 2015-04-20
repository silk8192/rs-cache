package com.rs.cache;

import java.nio.ByteBuffer;

/**
 * Holds operations to the single 520 byte blocks found in the cache.
 *
 * @author 8345T
 */
public class Sector {

    public static final int HEADER_SIZE = 10, BLOCK_SIZE = 510, TOTAL_SIZE = HEADER_SIZE + BLOCK_SIZE;

    private int fileId, chunk, nextSector, currentIndex;

    private ByteBuffer blockData = ByteBuffer.allocate(BLOCK_SIZE);

    public Sector(ByteBuffer buffer) {
        this.fileId = buffer.getInt();
        this.chunk = buffer.getShort() & 0xffff;
        this.nextSector = ((buffer.get() & 0xff) << 16) | ((buffer.get() & 0xff) << 8) | (buffer.get() & 0xff);
        this.currentIndex = buffer.get() & 0xff;

        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        this.blockData = ByteBuffer.wrap(b);
    }

    public Sector(int fileId, int chunk, int nextSector, int currentIndex, ByteBuffer data) {
        this.fileId = fileId;
        this.chunk = chunk;
        this.nextSector = nextSector;
        this.currentIndex = currentIndex;
        this.blockData = data;
    }

    public ByteBuffer getBlockData() {
        return blockData;
    }

    public int getFileId() {

        return fileId;
    }

    public int getChunk() {
        return chunk;
    }

    public int getNextSector() {
        return nextSector;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(TOTAL_SIZE);
        buffer.putInt(fileId);
        buffer.putShort((short) chunk);
        buffer.put((byte) (nextSector >> 16));
        buffer.put((byte) (nextSector >> 8));
        buffer.put((byte) nextSector);

        buffer.put((byte) currentIndex);
        buffer.put(blockData.array());

        buffer.flip();
        return buffer;
    }
}
