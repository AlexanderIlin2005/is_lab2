package org.itmo.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// --- ADD THESE IMPORTS ---
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
// --- END IMPORTS ---

@Configuration
@ComponentScan("org.itmo") // Сканируем весь пакет org.itmo
@EnableJpaRepositories(basePackages = "org.itmo.repository")
@EnableTransactionManagement
public class AppConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setPersistenceUnitName("my-persistence-unit");
        em.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    // --- 👇 ADD THIS BEAN DEFINITION 👇 ---
    @Bean
    public Validator validator() {
        // Create the default ValidatorFactory
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            // Get the Validator instance from the factory
            return factory.getValidator();
        }
    }
    // --- 👆 END OF BEAN DEFINITION 👆 ---
}