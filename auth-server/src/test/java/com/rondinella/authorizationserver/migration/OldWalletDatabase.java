package com.rondinella.authorizationserver.migration;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** The H2 file database of the old deployment, with the schema of the migrations and three users in it. */
final class OldWalletDatabase {

  static final String PASSWORD = "ada-password";
  static final String WALLET = "[[{\"address\":\"0xabc\",\"crypto\":{\"cipher\":\"aes-128-ctr\"}}],[\"Main\"],0]";

  final String url;

  private OldWalletDatabase(String url) {
    this.url = url;
  }

  static OldWalletDatabase create(Path dir) throws SQLException {
    String url = "jdbc:h2:file:" + dir.resolve("wallet").toAbsolutePath() + ";IFEXISTS=FALSE";
    migrate(new DriverManagerDataSource(url, "sa", ""), "h2");
    try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
      String hash = new BCryptPasswordEncoder().encode(PASSWORD);
      s.execute("insert into users (email, password, first_name, last_name, wallet) values"
          + " ('ada@example.com', '" + hash + "', 'Ada', 'Lovelace', '" + WALLET + "')");
      // from GitHub, no password yet
      s.execute("insert into users (email, password, first_name) values ('octo@example.com', null, 'octo')");
      s.execute("insert into users (email, password, is_enabled, is_account_locked) values"
          + " ('old@example.com', '" + hash + "', false, true)");
      s.execute("insert into authorities (user_email, authority) values"
          + " ('ada@example.com', 'USER'), ('ada@example.com', 'ADMIN'), ('octo@example.com', 'USER'), ('old@example.com', 'USER')");
    }
    // the application reads it the way production will: only if the file is there
    return new OldWalletDatabase(url.replace(";IFEXISTS=FALSE", ";IFEXISTS=TRUE"));
  }

  static void migrate(DataSource dataSource, String vendor) {
    Flyway.configure().dataSource(dataSource)
        .locations("classpath:db/migration/common", "classpath:db/migration/" + vendor)
        .load().migrate();
  }

  /** Every row of both tables, in a stable order, as text: to compare source and destination. */
  static List<String> rows(DataSource dataSource) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    List<String> rows = new ArrayList<>(jdbc.query(
        "select email, password, first_name, last_name, wallet, is_account_expired, is_account_locked,"
            + " is_credentials_expired, is_enabled from users order by email",
        (rs, i) -> String.join("|", rs.getString(1), String.valueOf(rs.getString(2)), String.valueOf(rs.getString(3)),
            String.valueOf(rs.getString(4)), rs.getString(5), String.valueOf(rs.getBoolean(6)),
            String.valueOf(rs.getBoolean(7)), String.valueOf(rs.getBoolean(8)), String.valueOf(rs.getBoolean(9)))));
    rows.addAll(jdbc.query("select user_email, authority from authorities order by user_email, authority",
        (rs, i) -> rs.getString(1) + "|" + rs.getString(2)));
    return rows;
  }
}
