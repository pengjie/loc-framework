package com.loc.framework.autoconfigure.jdbc;

import com.google.common.base.Strings;
import com.loc.framework.autoconfigure.ConditionalOnPrefixProperty;
import com.loc.framework.autoconfigure.LocBaseAutoConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.sf.log4jdbc.log.slf4j.Slf4jSpyLogDelegator;
import net.sf.log4jdbc.sql.jdbcapi.DataSourceSpy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created on 2017/12/26.
 */
@Slf4j
@Configuration
@ConditionalOnPrefixProperty(prefix = "loc", value = LocDsProperties.class)
@ConditionalOnClass({
    DataSource.class, HikariDataSource.class, DataSourceSpy.class
})
public class LocDataSourceAutoConfiguration extends LocBaseAutoConfiguration implements
    BeanFactoryPostProcessor, EnvironmentAware, Ordered {

  private ConfigurableEnvironment environment;

  private static final String[] PROPERTIES_TO_COPY = {
      "log4jdbc.debug.stack.prefix",
      "log4jdbc.sqltiming.warn.threshold",
      "log4jdbc.sqltiming.error.threshold",
      "log4jdbc.dump.booleanastruefalse",
      "log4jdbc.dump.fulldebugstacktrace",
      "log4jdbc.dump.sql.maxlinelength",
      "log4jdbc.statement.warn",
      "log4jdbc.dump.sql.select",
      "log4jdbc.dump.sql.insert",
      "log4jdbc.dump.sql.update",
      "log4jdbc.dump.sql.delete",
      "log4jdbc.dump.sql.create",
      "log4jdbc.dump.sql.addsemicolon",
      "log4jdbc.auto.load.popular.drivers",
      "log4jdbc.drivers",
      "log4jdbc.trim.sql",
      "log4jdbc.trim.sql.extrablanklines",
  };

  @Override
  public void postProcessBeanFactory(
      ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    LocDsProperties locDsProperties = resolverSetting(LocDsProperties.class,
        this.environment.getPropertySources());
    initLog4Jdbc();
    locDsProperties.getDataSource().forEach(
        (name, properties) -> createBean(configurableListableBeanFactory, name, properties));
  }


  private void createBean(ConfigurableListableBeanFactory configurableListableBeanFactory,
      String prefixName, JdbcProperties jdbcProperties) {
    String jdbcUrl = jdbcProperties.getUrl();
    checkArgument(!Strings.isNullOrEmpty(jdbcUrl), prefixName + " url is null or empty");
    log.info("prefixName is {}, jdbc properties is {}", prefixName, jdbcProperties);

    HikariDataSource hikariDataSource = createHikariDataSource(jdbcProperties);
    DataSourceSpy dataSource = new DataSourceSpy(hikariDataSource);

    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    AnnotationTransactionAspect.aspectOf().setTransactionManager(transactionManager);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    register(configurableListableBeanFactory, dataSource, prefixName + "DataSource",
        prefixName + "Ds");
    register(configurableListableBeanFactory, jdbcTemplate, prefixName + "JdbcTemplate",
        prefixName + "Jt");
    register(configurableListableBeanFactory, transactionManager, prefixName + "TransactionManager",
        prefixName + "Tx");
  }


  private HikariDataSource createHikariDataSource(JdbcProperties jdbcProperties) {
    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setJdbcUrl(jdbcProperties.getUrl());
    hikariDataSource.setUsername(jdbcProperties.getUsername());
    hikariDataSource.setPassword(jdbcProperties.getPassword());

    JdbcPoolProperties jdbcPoolProperties = jdbcProperties.getPool();
    hikariDataSource.setAutoCommit(jdbcPoolProperties.isAutoCommit());
    hikariDataSource.setConnectionTimeout(jdbcPoolProperties.getConnectionTimeout());
    hikariDataSource.setIdleTimeout(jdbcPoolProperties.getIdleTimeout());
    hikariDataSource.setMaxLifetime(jdbcPoolProperties.getMaxLifetime());
    hikariDataSource.setMaximumPoolSize(jdbcPoolProperties.getMaximumPoolSize());
    hikariDataSource.setMinimumIdle(jdbcPoolProperties.getMinimumIdle());
    hikariDataSource
        .setInitializationFailTimeout(jdbcPoolProperties.getInitializationFailTimeout());
    hikariDataSource.setIsolateInternalQueries(jdbcPoolProperties.isIsolateInternalQueries());
    hikariDataSource.setReadOnly(jdbcPoolProperties.isReadOnly());
    hikariDataSource.setRegisterMbeans(jdbcPoolProperties.isRegisterMbeans());
    Optional.ofNullable(jdbcPoolProperties.getDriverClassName())
        .ifPresent(hikariDataSource::setDriverClassName);
    hikariDataSource.setValidationTimeout(jdbcPoolProperties.getValidationTimeout());
    hikariDataSource.setLeakDetectionThreshold(jdbcPoolProperties.getLeakDetectionThreshold());
    return hikariDataSource;
  }


  private void initLog4Jdbc() {
    for (final String property : PROPERTIES_TO_COPY) {
      if (this.environment.containsProperty(property)) {
        System.setProperty(property, Objects.requireNonNull(this.environment.getProperty(property)));
      }
    }
    System.setProperty("log4jdbc.spylogdelegator.name", this.environment
        .getProperty("log4jdbc.spylogdelegator.name", Slf4jSpyLogDelegator.class.getName()));
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = (ConfigurableEnvironment) environment;
  }

  @Override
  public int getOrder() {
    return Integer.MAX_VALUE - 1;
  }
}
