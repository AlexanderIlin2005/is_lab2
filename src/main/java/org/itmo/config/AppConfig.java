package org.itmo.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validation; // Добавлено для Validator
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import javax.sql.DataSource; // Используй javax.sql.DataSource
import org.springframework.beans.factory.annotation.Value; // Добавлено для чтения свойств
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource; // Добавлено для чтения database.properties
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer; // Добавлено для работы @Value
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource; // Добавлено для создания DataSource
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
// Указываем Spring, где находится файл с настройками БД
@PropertySource("classpath:database.properties")
@ComponentScan("org.itmo")
@EnableJpaRepositories(basePackages = "org.itmo.repository")
@EnableTransactionManagement
public class AppConfig {

    // --- 1. Поля для считывания настроек БД (из database.properties) ---
    @Value("${db.driver}")
    private String driverClass;
    @Value("${db.url}")
    private String url;
    @Value("${db.username}")
    private String username;
    @Value("${db.password}")
    private String password;

    // --- 2. Бин-конфигуратор для поддержки @Value (ОБЯЗАТЕЛЕН) ---
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    // --- 3. Бин DataSource (ИСПРАВЛЕНИЕ: Гарантирует, что Spring найдет подключение к БД) ---
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    // --- 4. Бин Validator (Добавлен для XML-импорта) ---
    @Bean
    public Validator validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }

    // --- 5. Конфигурация JPA (ОСТАЛАСЬ ПРЕЖНЕЙ) ---
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        // ВАЖНО: DataSource будет автоматически связан Spring'ом с этим EMF
        // благодаря тому, что мы объявили его бином выше!
        em.setPersistenceUnitName("my-persistence-unit");
        em.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        return em;
    }

    // --- 6. Менеджер Транзакций (ОСТАЛАСЬ ПРЕЖНЕЙ) ---
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }
}