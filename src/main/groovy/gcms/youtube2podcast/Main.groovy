package gcms.youtube2podcast

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.support.SpringBootServletInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
/**
 * Created by gustavo on 19/02/17.
 */

@Configuration
@SpringBootApplication
public class Main extends SpringBootServletInitializer {

    @Bean
    TaskExecutor taskExecutor() {
        def taskExecutor = new ThreadPoolTaskExecutor()
        taskExecutor.setCorePoolSize(1)
        taskExecutor.setMaxPoolSize(2)
        taskExecutor.setQueueCapacity(10)
        taskExecutor
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Main);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main, args);
    }
}
