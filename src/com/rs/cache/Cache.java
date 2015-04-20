package com.rs.cache;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls all access to the cache.
 *
 * @author 8345T
 */
public class Cache implements Closeable {

    /**
     * A hashmap of all indices.
     */
    private HashMap<Integer, Index> indices = new HashMap<>();

    /**
     * The main cache data file.
     */
    private FileChannel cacheChannel;

    /**
     * Finds all cache files within a directory
     *
     * @param cachePath
     */
    public Cache(Path cachePath) {
        try {
            cacheChannel = new RandomAccessFile(new File(cachePath.toString(), "main_file_cache.dat2"), "rw").getChannel();
            Files.list(cachePath).sorted()
                    .filter(f -> f.toString().matches(".*\\.idx.*"))
                    .forEach(i -> {
                        int index = Integer.parseInt(i.toString().substring(i.toString().lastIndexOf("idx") + 3));
                        if (index != 0xff)
                            indices.put(index, new Index(index, cachePath, cacheChannel));
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, Index> getIndices() {
        return indices;
    }

    /**
     * Returns a specified index
     *
     * @param index the index id, or type
     * @return The index required
     */
    public Index getIndex(int index) {
        return this.indices.get(index);
    }

    /**
     * Safely attempts to close opened resources.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        cacheChannel.close();
        for (Map.Entry<Integer, Index> index : indices.entrySet()) {
            index.getValue().close();
        }
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        this.close();
    }
}
