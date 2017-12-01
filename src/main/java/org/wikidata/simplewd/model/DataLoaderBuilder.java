package org.wikidata.simplewd.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.dataloader.BatchLoader;
import org.dataloader.CacheMap;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;

import java.util.concurrent.TimeUnit;

public class DataLoaderBuilder {
    public static <K, V> DataLoader<K, V> newDataLoader(BatchLoader<K, V> batchLoadFunction, int cacheSize, int expirationDuration) {
        Cache<K, V> cache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(expirationDuration, TimeUnit.DAYS)
                .build();
        DataLoaderOptions options = DataLoaderOptions.newOptions().setCacheMap(new GuavaCacheMap<>(cache));
        return DataLoader.newDataLoader(batchLoadFunction, options);
    }

    private static class GuavaCacheMap<K, V> implements CacheMap<K, V> {
        private Cache<K, V> cache;

        GuavaCacheMap(Cache<K, V> cache) {
            this.cache = cache;
        }

        @Override
        public boolean containsKey(K k) {
            return cache.getIfPresent(k) != null;
        }

        @Override
        public V get(K k) {
            return cache.getIfPresent(k);
        }

        @Override
        public CacheMap<K, V> set(K k, V v) {
            cache.put(k, v);
            return this;
        }

        @Override
        public CacheMap<K, V> delete(K k) {
            cache.invalidate(k);
            return this;
        }

        @Override
        public CacheMap<K, V> clear() {
            cache.invalidateAll();
            return this;
        }
    }
}
