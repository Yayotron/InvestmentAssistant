package io.yayotron.investmentassistant.feeder.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        // Programmatically define caches with different specifications
        CaffeineCache stockDataCache = new CaffeineCache("stockData",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(200) // Example size
                        .build());

        CaffeineCache companyOverviewCache = new CaffeineCache("companyOverview",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(500) // Example size
                        .build());

        CaffeineCache earningsDataCache = new CaffeineCache("earningsData",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(500) // Example size
                        .build());

        CaffeineCache sectorPerformanceCache = new CaffeineCache("sectorPerformance",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS) // 1-hour TTL
                        .maximumSize(100) // Example size, sector data might not be too large
                        .build());

        CaffeineCache fmpSectorPECache = new CaffeineCache("fmpSectorPE",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(200) // Assuming a moderate number of sectors per exchange
                        .build());

        CaffeineCache fmpIndustryPECache = new CaffeineCache("fmpIndustryPE",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(500) // Assuming a moderate number of industries per exchange
                        .build());
        
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
                stockDataCache,
                companyOverviewCache,
                earningsDataCache,
                sectorPerformanceCache,
                fmpSectorPECache,
                fmpIndustryPECache
        ));
        return manager;
    }
}
