package com.yupi.yuaicodemother.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

/**
 * 记忆任务专用线程池配置
 * - 核心线程 2，最大 5，队列 200
 * - 线程名前缀 memory-task-，CallerRunsPolicy 防止任务丢失
 */
@EnableAsync
@Configuration
public class MemoryTaskExecutorConfig {

    @Bean(name = "memoryTaskExecutor")
    public Executor memoryTaskExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                5,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("memory-task-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.allowCoreThreadTimeOut(false);
        return executor;
    }
}
