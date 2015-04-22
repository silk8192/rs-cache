package com.rs.cache;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Holds the index file and all operations related to it.
 *
 * @author 8345T <explosionsbehindme@gmail.com>
 */
public class Index implements Closeable {

    public static final int BLOCK_SIZE = 8;

    /**
     * The id of this index, or type.
     */
    private int id;

    /**
     * The data streams for the index file and the main cache file.
     */
    private FileChannel indexChannel, cacheChannel;

    public Index(int id, Path cachePath, FileChannel cacheChannel) {
        this.id = id;
        try {
            this.indexChannel = new RandomAccessFile(new File(cachePath.toString(), "main_file_cache.idx" + id), "rw").getChannel();
            this.cacheChannel = cacheChannel;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * Writes an item to the file store.
     *
     * @param file The file to write.
     * @param data The file's data.
     * @return true if the file was written, false otherwise.
     */
    public boolean write(int file, ByteBuffer data) {
        boolean success = write(file, data, true);
        if (!success) {
            success = write(file, data, false);
        }

        return success;
    }


    public boolean write(int fileId, ByteBuffer data, boolean overwrite) {
        data.position(0);
        int fileSize = data.remaining();
        int currentSector;
        try {
            if (overwrite) {
                ByteBuffer header = indexChannel.map(FileChannel.MapMode.READ_ONLY, fileId * BLOCK_SIZE, BLOCK_SIZE);
                header.getInt();
                currentSector = header.getInt();

                if (currentSector <= 0 || currentSector > cacheChannel.size() / Sector.TOTAL_SIZE) {
                    return false;
                }
            } else {
                currentSector = (int) ((cacheChannel.size() + Sector.TOTAL_SIZE - 1) / Sector.TOTAL_SIZE);
                if (currentSector == 0)
                    currentSector = 1;
            }

            ByteBuffer header = ByteBuffer.allocate(BLOCK_SIZE);
            header.putInt(fileSize);
            header.putInt(currentSector);
            header.flip();
            indexChannel.write(header, fileId * BLOCK_SIZE);

            int remainder = fileSize, chunk = 0;
            do {
                int nextSector = 0;
                if (overwrite) {
                    Sector firstBlock = new Sector(cacheChannel.map(FileChannel.MapMode.READ_ONLY, currentSector * Sector.TOTAL_SIZE, Sector.TOTAL_SIZE));

                    if (fileId != firstBlock.getFileId() || chunk != firstBlock.getChunk() || this.getId() != firstBlock.getChunk()) {
                        return false;
                    }
                    if (nextSector < 0 || nextSector > cacheChannel.size() / Sector.TOTAL_SIZE) {
                        return false;
                    }
                }

                if (nextSector == 0) {
                    nextSector = (int) ((cacheChannel.size() + Sector.TOTAL_SIZE - 1) / Sector.TOTAL_SIZE);
                    if (nextSector == 0 || nextSector == currentSector) {
                        nextSector++;
                    } else if (remainder <= Sector.BLOCK_SIZE) {
                        nextSector = 0;
                    }
                }

                int currentBlockLength = remainder > Sector.BLOCK_SIZE ? Sector.BLOCK_SIZE : remainder;
                byte[] dataToWrite = new byte[currentBlockLength];
                data.get(dataToWrite, 0, currentBlockLength);
                Sector currentSectorData = new Sector(fileId, chunk, nextSector, this.getId(), ByteBuffer.wrap(dataToWrite));
                cacheChannel.write(currentSectorData.toByteBuffer(), currentSector * Sector.TOTAL_SIZE);
                remainder -= currentBlockLength;
                currentSector = nextSector;
                chunk++;
            } while (remainder > 0);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ByteBuffer read(int fileId) {
        if (fileId > getFileCount()) {
            throw new IllegalStateException("File requested doesn't exist!");
        }

        try {
            ByteBuffer header = indexChannel.map(FileChannel.MapMode.READ_ONLY, fileId * BLOCK_SIZE, BLOCK_SIZE);
            int fileSize = header.getInt();
            int sectorAt = header.getInt();

            ByteBuffer fileData = ByteBuffer.allocate(fileSize);

            int remainder = fileSize, chunk = 0;
            do {
                if (sectorAt == 0) {
                    System.err.println("End of sector error!");
                }

                int currentBlockLength = remainder > Sector.BLOCK_SIZE ? Sector.BLOCK_SIZE : remainder;
                ByteBuffer currentBlock = ByteBuffer.allocate(currentBlockLength + Sector.HEADER_SIZE);
                cacheChannel.read(currentBlock, sectorAt * Sector.TOTAL_SIZE);
                currentBlock.flip();
                Sector currentSectorData = new Sector(currentBlock);

                if (fileId != currentSectorData.getFileId() || chunk != currentSectorData.getChunk() || this.getId() != currentSectorData.getCurrentIndex()) {
                    throw new IOException("Currupted data!");
                }

                fileData.put(currentSectorData.getBlockData());
                remainder -= currentBlockLength;
                sectorAt = currentSectorData.getNextSector();
                chunk++;
            } while (remainder > 0);
            fileData.flip();
            return fileData;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to read file!");
        }
    }

    /**
     * Gets the number of files stored in this file store.
     *
     * @return This file store's file count.
     */
    public int getFileCount() {
        try {
            return (int) (indexChannel.size() / BLOCK_SIZE);
        } catch (IOException e) {
            return 0;
        }
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", indexChannel=" + indexChannel +
                ", cacheChannel=" + cacheChannel +
                '}';
    }

    @Override
    public void close() throws IOException {
        indexChannel.close();
    }
}
