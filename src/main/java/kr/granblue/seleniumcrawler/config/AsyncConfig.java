package kr.granblue.seleniumcrawler.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    
    @Bean @Qualifier("asyncExecutor") // ThreadPoolTaskExecutor 기본
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 수
        executor.setPrestartAllCoreThreads(true); // 모든 코어 스레드를 미리 시작
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    @Bean @Qualifier("embedExecutor") // 임베딩용 (권장쓰레드 pageSize + a)
    public Executor getEmbedExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // 기본 스레드 수
        executor.setPrestartAllCoreThreads(true); // 모든 코어 스레드를 미리 시작
        executor.setThreadNamePrefix("Embed-");
        executor.initialize();
        return executor;
    }
}
