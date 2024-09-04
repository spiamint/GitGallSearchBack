package kr.granblue.gbfsearchback.config;

import kr.granblue.gbfsearchback.domain.DcBoardEmbedding;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "kr.granblue.gbfsearchback.repository.postgre",
        entityManagerFactoryRef = "postgreEntityManager",
        transactionManagerRef = "postgreTransactionManager"
)
public class PostgreDataSourceConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.postgre")
    public DataSourceProperties postgreDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource postgreDataSource() {
        return postgreDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "postgreEntityManager")
    public LocalContainerEntityManagerFactoryBean postgreEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder.dataSource(postgreDataSource()).packages(DcBoardEmbedding.class)
                .build();
    }

    @Bean(name = "postgreTransactionManager")
    public PlatformTransactionManager postgreTrnasactionMAnager(
            @Qualifier("postgreEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        entityManagerFactoryBean.setJpaProperties(properties);
        return new JpaTransactionManager(entityManagerFactoryBean.getObject());
    }


}
