package com.rs.cache;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Random;
import java.util.zip.CRC32;

public class CacheTest {

    private static ByteBuffer sampleWriteData = ByteBuffer.allocate(8192);

    private static long crcWriteHash;

    static {
        CRC32 crc = new CRC32();
        Random rand = new Random();
        for(int i = 0; i < 8192; i++) {
            sampleWriteData.put((byte) rand.nextInt(255));
        }
        sampleWriteData.flip();
        crc.update(sampleWriteData);
        crcWriteHash = crc.getValue();
    }

    @Test
    public void testReadAndWrite() throws IOException {
        Cache cache = Cache.constructCache(Paths.get(System.getProperty("user.home"), "testcache"), 1);
        cache.getIndex(0).write(1, sampleWriteData);
        CRC32 crc = new CRC32();
        ByteBuffer readData = cache.getIndex(0).read(1);
        crc.update(readData);
        cache.close();
        assertEquals(crcWriteHash, crc.getValue());
    }
}
