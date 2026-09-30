package com.releaseowl.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * The CDS starter brings Spring's JDBC auto-configuration onto the classpath.
 * This service is stateless and has no entities, so the datasource
 * auto-configuration is excluded - otherwise Spring would fail at startup
 * looking for a JDBC driver that this application neither has nor needs.
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
