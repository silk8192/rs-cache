# rs-cache
An custom implementation of the RuneScape cache file system.

At the present moment, this only is able to edit cache's of revision 317 to 400+ but with one exception: Instead of reading a tribyte/int16_t, it reads normal sized ints. (4 bytes instead of 3)

Interfacing the cache is fairly straightforward and can be done as so:
```
Cache cache = new Cache(Paths.get("cache path));
ByteBuffer data = cache.getIndex(indexId).read(fileId);
cache.close();
```

It also supports writing to the cache with overwrite support:
```
Cache cache = new Cache(Paths.get("cache path));
cache.getIndex(indexId).write(fileId, bufferData); 
cache.close();
```
