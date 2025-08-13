package io.yayotron.investmentassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FeederApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeederApplication.class, args);
	}

}
