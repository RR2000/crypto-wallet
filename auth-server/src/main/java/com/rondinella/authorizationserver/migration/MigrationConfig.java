package com.rondinella.authorizationserver.migration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Flyway migrations, then (only when wallet.import.from is set) the copy of the users from the old H2 database.
 * Both run while the application context starts, so before the server accepts any request: nobody can register
 * in the empty destination in the meantime.
 */
@Configuration
public class MigrationConfig {

  @Bean
  FlywayMigrationStrategy migrateThenImport(@Value("${wallet.import.from:}") String from,
                                            @Value("${wallet.import.username:sa}") String username,
                                            @Value("${wallet.import.password:}") String password) {
    return flyway -> {
      flyway.migrate();
      if (StringUtils.hasText(from)) {
        new LegacyDatabaseImport(flyway.getConfiguration().getDataSource(), from, username, password).run();
      }
    };
  }
}
