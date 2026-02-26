package stats.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({
        "stats.server",
        "dto"
})
public class StatsServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatsServerApplication.class, args);
    }
}