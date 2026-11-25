package com.rondinella.authorizationserver.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LegacyDatabaseImportTest {

  static EmbeddedPostgres postgres;

  @TempDir Path dir;

  @BeforeAll
  static void startPostgres() throws IOException {
    postgres = EmbeddedPostgres.start();
  }

  @AfterAll
  static void stopPostgres() throws IOException {
    postgres.close();
  }

  /** A new, empty PostgreSQL database with the schema of the migrations. */
  private static DataSource emptyPostgres() {
    String name = "wallet_" + UUID.randomUUID().toString().replace("-", "");
    new JdbcTemplate(postgres.getPostgresDatabase()).execute("create database " + name);
    DataSource dataSource = postgres.getDatabase("postgres", name);
    OldWalletDatabase.migrate(dataSource, "postgresql");
    return dataSource;
  }

  private static DataSource emptyH2() {
    DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    OldWalletDatabase.migrate(dataSource, "h2");
    return dataSource;
  }

  private static void assertCopiedOnce(OldWalletDatabase old, DataSource destination) {
    var source = new DriverManagerDataSource(old.url, "sa", "");
    var expected = OldWalletDatabase.rows(source);

    var first = new LegacyDatabaseImport(destination, old.url, "sa", "").run();
    assertThat(first).isEqualTo(new LegacyDatabaseImport.Result(true, 3, 4));
    assertThat(OldWalletDatabase.rows(destination)).hasSize(7).containsExactlyElementsOf(expected);

    // a restart with the variable still set: nothing duplicated, nothing changed
    var second = new LegacyDatabaseImport(destination, old.url, "sa", "").run();
    assertThat(second).isEqualTo(new LegacyDatabaseImport.Result(false, 3, 4));
    assertThat(OldWalletDatabase.rows(destination)).containsExactlyElementsOf(expected);
    // the source is only read
    assertThat(OldWalletDatabase.rows(source)).containsExactlyElementsOf(expected);
  }

  @Test
  void copiesUsersAndAuthoritiesOnceIntoAnEmptyPostgres() throws Exception {
    assertCopiedOnce(OldWalletDatabase.create(dir), emptyPostgres());
  }

  @Test
  void copiesUsersAndAuthoritiesOnceIntoAnEmptyH2() throws Exception {
    assertCopiedOnce(OldWalletDatabase.create(dir), emptyH2());
  }

  @Test
  void leavesADestinationWithUsersAlone() throws Exception {
    var old = OldWalletDatabase.create(dir);
    var destination = emptyPostgres();
    var jdbc = new JdbcTemplate(destination);
    jdbc.update("insert into users (email, password) values ('new@example.com', 'x')");

    var result = new LegacyDatabaseImport(destination, old.url, "sa", "").run();

    assertThat(result.copied()).isFalse();
    assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from authorities", Long.class)).isZero();
  }

  @Test
  void aMissingSourceStopsWithoutCopyingOrCreatingIt() {
    var destination = emptyPostgres();
    String missing = "jdbc:h2:file:" + dir.resolve("nothing-here").toAbsolutePath() + ";IFEXISTS=TRUE";

    assertThatThrownBy(() -> new LegacyDatabaseImport(destination, missing, "sa", "").run())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("nothing copied")
        .hasNoCause();
    assertThat(new JdbcTemplate(destination).queryForObject("select count(*) from users", Long.class)).isZero();
    assertThat(dir.resolve("nothing-here.mv.db")).doesNotExist();
  }

  @Test
  void aFailureRollsBackAndNeverQuotesTheRows() throws Exception {
    var old = OldWalletDatabase.create(dir);
    var destination = emptyPostgres();
    // a destination that refuses one of the source rows: the users already inserted must go away too
    new JdbcTemplate(destination).execute("alter table authorities add constraint no_admin check (authority <> 'ADMIN')");

    assertThatThrownBy(() -> new LegacyDatabaseImport(destination, old.url, "sa", "").run())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SQL state 23514")
        .hasNoCause()
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain("ada@example.com", "ADMIN"));
    assertThat(new JdbcTemplate(destination).queryForObject("select count(*) from users", Long.class)).isZero();
  }
}
