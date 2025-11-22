package com.tickettracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Spring Boot Application Class for Ticket Tracker
 *
 * This application manages workflow tickets, steps, and file attachments
 * with role-based access control and comprehensive audit logging.
 *
 * Extends SpringBootServletInitializer for WAR deployment to Tomcat.
 *
 * @author Ticket Tracker Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class TicketTrackerApplication extends SpringBootServletInitializer {

    /**
     * Main method to run the Spring Boot application
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TicketTrackerApplication.class, args);
    }

    /**
     * Configure the application for WAR deployment
     * Required for deploying to external Tomcat server
     *
     * @param application SpringApplicationBuilder instance
     * @return Configured SpringApplicationBuilder
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TicketTrackerApplication.class);
    }
}
