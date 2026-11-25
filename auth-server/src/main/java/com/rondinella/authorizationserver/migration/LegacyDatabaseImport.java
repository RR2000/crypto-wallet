package com.rondinella.authorizationserver.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-off copy of the users (and their authorities) from the old H2 file database into the current one, used to
 * move the demo from H2 to PostgreSQL without losing anyone.
 *
 * <p>It copies only when both destination tables are empty, in a single transaction, and checks the counts before
 * committing: run it again and it does nothing. The source is only read. The log carries counts, never user data,
 * and a failure is reported with its SQL state only (driver messages can quote the values of a row).
 */
public class LegacyDatabaseImport {

  private static final Logger log = LoggerFactory.getLogger(LegacyDatabaseImport.class);

  private static final String USERS = "email, password, first_name, last_name, wallet,"
      + " is_account_expired, is_account_locked, is_credentials_expired, is_enabled";
  private static final String AUTHORITIES = "user_email, authority";

  /** What happened: whether rows were copied, and the counts of the source. */
  public record Result(boolean copied, long users, long authorities) {}

  private final DataSource destination;
  private final String sourceUrl;
  private final String sourceUser;
  private final String sourcePassword;

  public LegacyDatabaseImport(DataSource destination, String sourceUrl, String sourceUser, String sourcePassword) {
    this.destination = destination;
    this.sourceUrl = sourceUrl;
    this.sourceUser = sourceUser;
    this.sourcePassword = sourcePassword;
  }

  public Result run() {
    try (Connection source = DriverManager.getConnection(sourceUrl, sourceUser, sourcePassword);
         Connection target = destination.getConnection()) {
      source.setReadOnly(true);
      long users = count(source, "users");
      long authorities = count(source, "authorities");
      log.info("wallet import: source users={} authorities={}", users, authorities);

      boolean autoCommit = target.getAutoCommit();
      target.setAutoCommit(false);
      try {
        long existingUsers = count(target, "users");
        long existingAuthorities = count(target, "authorities");
        if (existingUsers > 0 || existingAuthorities > 0) {
          target.rollback();
          log.info("wallet import: destination not empty (users={} authorities={}), nothing copied",
              existingUsers, existingAuthorities);
          return new Result(false, users, authorities);
        }
        copyUsers(source, target);
        copyAuthorities(source, target);
        long copiedUsers = count(target, "users");
        long copiedAuthorities = count(target, "authorities");
        if (copiedUsers != users || copiedAuthorities != authorities) {
          throw new IllegalStateException("wallet import: counts differ after the copy (users " + copiedUsers + "/"
              + users + ", authorities " + copiedAuthorities + "/" + authorities + "), rolled back");
        }
        target.commit();
        log.info("wallet import: copied users={} authorities={}", copiedUsers, copiedAuthorities);
        return new Result(true, users, authorities);
      } catch (SQLException | RuntimeException e) {
        target.rollback();
        throw e;
      } finally {
        target.setAutoCommit(autoCommit);
      }
    } catch (SQLException e) {
      // no cause: the driver's message may contain the values of a row (e.g. a duplicate key)
      throw new IllegalStateException("wallet import failed (SQL state " + e.getSQLState() + ", error code "
          + e.getErrorCode() + "), nothing copied");
    }
  }

  private static long count(Connection c, String table) throws SQLException {
    try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from " + table)) {
      rs.next();
      return rs.getLong(1);
    }
  }

  private static void copyUsers(Connection source, Connection target) throws SQLException {
    try (Statement read = source.createStatement();
         ResultSet rs = read.executeQuery("select " + USERS + " from users order by email");
         PreparedStatement write = target.prepareStatement(
             "insert into users (" + USERS + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      while (rs.next()) {
        for (int i = 1; i <= 5; i++) {
          write.setString(i, rs.getString(i));   // the wallet is a CLOB in H2 and text in PostgreSQL
        }
        for (int i = 6; i <= 9; i++) {
          write.setBoolean(i, rs.getBoolean(i));
        }
        write.addBatch();
      }
      write.executeBatch();
    }
  }

  private static void copyAuthorities(Connection source, Connection target) throws SQLException {
    try (Statement read = source.createStatement();
         ResultSet rs = read.executeQuery("select " + AUTHORITIES + " from authorities order by user_email, authority");
         PreparedStatement write = target.prepareStatement("insert into authorities (" + AUTHORITIES + ") values (?, ?)")) {
      while (rs.next()) {
        write.setString(1, rs.getString(1));
        write.setString(2, rs.getString(2));
        write.addBatch();
      }
      write.executeBatch();
    }
  }
}
