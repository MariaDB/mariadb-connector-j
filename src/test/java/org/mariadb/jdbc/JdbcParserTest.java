/*
 *
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2020 MariaDB Corporation Ab.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to Monty Program Ab info@montyprogram.com.
 *
 * This particular MariaDB Client for Java file is work
 * derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
 * the following copyright and notice provisions:
 *
 * Copyright (c) 2009-2011, Marcus Eriksson
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of the driver nor the names of its contributors may not be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.mariadb.jdbc;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Test;
import org.mariadb.jdbc.internal.util.constant.HaMode;
import org.mariadb.jdbc.internal.util.constant.ParameterConstant;
import org.mariadb.jdbc.internal.util.constant.RedirectionOption;
import org.mariadb.jdbc.util.Options;

@SuppressWarnings("ConstantConditions")
public class JdbcParserTest {

  @Test
  public void testMariaAlias() throws Throwable {
    UrlParser jdbc = UrlParser.parse("jdbc:mariadb://localhost/test");
    UrlParser jdbc2 = UrlParser.parse("jdbc:mysql://localhost/test");
    assertEquals(jdbc.getDatabase(), jdbc2.getDatabase());
    assertEquals(jdbc.getOptions(), jdbc2.getOptions());
    assertEquals(jdbc.getHostAddresses(), jdbc2.getHostAddresses());
    assertEquals(jdbc.getHaMode(), jdbc2.getHaMode());
  }

  @Test
  public void testAuroraUseBatchMultiSend() throws Throwable {
    assertTrue(
        UrlParser.parse("jdbc:mariadb://localhost/test")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);
    assertTrue(
        UrlParser.parse("jdbc:mariadb://localhost/test?useBatchMultiSend=true")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);
    assertFalse(
        UrlParser.parse("jdbc:mariadb://localhost/test?useBatchMultiSend=false")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);

    assertFalse(
        UrlParser.parse("jdbc:mariadb:aurora://localhost/test")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);
    assertTrue(
        UrlParser.parse("jdbc:mariadb:aurora://localhost/test?useBatchMultiSend=true")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);

    String hostAurora =
        "jdbc:mariadb://localhost,instance-1-cluster.cluster-cvz6gk5op1wk.us-east-1.rds.amazonaws.com:3306/test";
    assertFalse(UrlParser.parse(hostAurora).auroraPipelineQuirks().getOptions().useBatchMultiSend);
    assertTrue(
        UrlParser.parse(hostAurora + "?useBatchMultiSend=true")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);

    String hostAuroraUpper =
        "jdbc:mariadb://localhost,instance-1-cluster.cluster-cvz6gk5op1wk.us-east-1.rds.AMAZONAWS.com:3306/test";
    assertFalse(
        UrlParser.parse(hostAuroraUpper).auroraPipelineQuirks().getOptions().useBatchMultiSend);
    assertTrue(
        UrlParser.parse(hostAuroraUpper + "?useBatchMultiSend=true")
            .auroraPipelineQuirks()
            .getOptions()
            .useBatchMultiSend);

    MariaDbDataSource datasource = new MariaDbDataSource();
    datasource.initialize();
    assertNull(datasource.getUrlParser().getOptions().useBatchMultiSend);
    datasource.setUrl(hostAurora);
    assertFalse(datasource.getUrlParser().auroraPipelineQuirks().getOptions().useBatchMultiSend);
    datasource.setUrl(hostAurora + "?useBatchMultiSend=true");
    assertTrue(datasource.getUrlParser().auroraPipelineQuirks().getOptions().useBatchMultiSend);
  }

  @Test
  public void testWrongFormat() {
    try {
      UrlParser.parse("jdbc:mariadb:/localhost/test");
      fail("Must have throw exception");
    } catch (SQLException ie) {
      assertTrue(ie.getMessage().contains("url parsing error : '//' is not present in the url"));
    }
  }

  @Test
  public void testNoAdditionalPart() throws SQLException {
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost/").getDatabase());
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost/").getUsername());
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost").getDatabase());
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost").getUsername());
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost?").getDatabase());
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost?").getUsername());
  }

  @Test
  public void testDatabaseOnly() throws SQLException {
    assertEquals("DB", UrlParser.parse("jdbc:mariadb://localhost/DB").getDatabase());
    assertEquals(null, UrlParser.parse("jdbc:mariadb://localhost/DB").getUsername());
  }

  @Test
  public void testUrl() throws SQLException {
    UrlParser parser =
        new UrlParser(
            "DB", Arrays.asList(new HostAddress("local", 3306)), new Options(), HaMode.REPLICATION);
    assertEquals(
        "jdbc:mariadb:replication://address=(host=local)(port=3306)(type=master)/DB",
        parser.getInitialUrl());
    assertEquals(
        "jdbc:mariadb:replication://address=(host=local)(port=3306)(type=master)/DB",
        parser.toString());
    assertEquals(parser, parser);
    assertNotEquals(this, parser);
    assertEquals(
        UrlParser.parse(
            "jdbc:mariadb:replication://address=(host=local)(port=3306)(type=master)/DB"),
        parser);

    parser =
        new UrlParser(
            "DB",
            Arrays.asList(
                new HostAddress("host1", 3306),
                new HostAddress("host2", 3307, ParameterConstant.TYPE_REPLICA)),
            new Options(),
            HaMode.REPLICATION);
    assertEquals(
        "jdbc:mariadb:replication://address=(host=host1)(port=3306)(type=master),address=(host=host2)(port=3307)(type=replica)/DB",
        parser.getInitialUrl());
    parser.setDatabase("DB2");
    assertEquals("DB2", parser.getDatabase());
  }

  @Test
  public void testAuroraUsePipelineAuth() throws Throwable {
    assertTrue(
        UrlParser.parse("jdbc:mariadb://localhost/test")
            .auroraPipelineQuirks()
            .getOptions()
            .usePipelineAuth);
    assertTrue(
        UrlParser.parse("jdbc:mariadb://localhost/test?usePipelineAuth=true")
            .auroraPipelineQuirks()
            .getOptions()
            .usePipelineAuth);
    assertFalse(
        UrlParser.parse("jdbc:mariadb://localhost/test?usePipelineAuth=false")
            .auroraPipelineQuirks()
            .getOptions()
            .usePipelineAuth);

    assertFalse(
        UrlParser.parse("jdbc:mariadb:aurora://localhost/test")
            .auroraPipelineQuirks()
            .getOptions()
            .usePipelineAuth);
    assertTrue(
        UrlParser.parse("jdbc:mariadb:aurora://localhost/test?usePipelineAuth=true")
            .auroraPipelineQuirks()
            .getOptions()
            .usePipelineAuth);

    String hostAurora =
        "jdbc:mariadb://localhost,instance-1-cluster.cluster-cvz6gk5op1wk.us-east-1.rds.amazonaws.com:3306/test";
    assertFalse(UrlParser.parse(hostAurora).auroraPipelineQuirks().getOptions().usePipelineAuth);
    assertTrue(UrlParser.parse(hostAurora + "?usePipelineAuth=true").getOptions().usePipelineAuth);

    String hostAuroraUpper =
        "jdbc:mariadb://localhost,instance-1-cluster.cluster-cvz6gk5op1wk.us-east-1.RDS.amazonaws.com:3306/test";
    assertFalse(
        UrlParser.parse(hostAuroraUpper).auroraPipelineQuirks().getOptions().usePipelineAuth);
    assertTrue(
        UrlParser.parse(hostAuroraUpper + "?usePipelineAuth=true").getOptions().usePipelineAuth);

    MariaDbDataSource datasource = new MariaDbDataSource();
    datasource.initialize();
    assertNull(datasource.getUrlParser().getOptions().usePipelineAuth);
    datasource.setUrl(hostAurora);
    assertFalse(datasource.getUrlParser().auroraPipelineQuirks().getOptions().usePipelineAuth);
    datasource.setUrl(hostAurora + "?usePipelineAuth=true");
    assertTrue(datasource.getUrlParser().auroraPipelineQuirks().getOptions().usePipelineAuth);
  }

  @Test
  public void testAcceptsUrl() {
    Driver driver = new Driver();
    assertTrue(driver.acceptsURL("jdbc:mariadb://localhost/test"));
    assertTrue(driver.acceptsURL("jdbc:mysql://localhost/test"));
    assertFalse(driver.acceptsURL("jdbc:mysql://localhost/test?disableMariaDbDriver"));
  }

  @Test
  public void testSslAlias() throws Throwable {
    UrlParser jdbc = UrlParser.parse("jdbc:mariadb://localhost/test?useSSL=true");
    assertTrue(jdbc.getOptions().useSsl);

    jdbc = UrlParser.parse("jdbc:mariadb://localhost/test?useSsl=true");
    assertTrue(jdbc.getOptions().useSsl);

    jdbc = UrlParser.parse("jdbc:mariadb://localhost/test");
    assertNull(jdbc.getOptions().useSsl);
  }

  @Test
  public void testNamePipeUrl() throws Throwable {
    UrlParser jdbc = UrlParser.parse("jdbc:mariadb:///test?useSSL=true");
    assertTrue(jdbc.getOptions().useSsl);
  }

  @Test
  public void testBooleanDefault() throws Throwable {
    assertNull(UrlParser.parse("jdbc:mariadb:///test").getOptions().useSsl);
    assertTrue(UrlParser.parse("jdbc:mariadb:///test?useSSL=true").getOptions().useSsl);
    assertTrue(UrlParser.parse("jdbc:mariadb:///test?useSSL").getOptions().useSsl);
  }

  @Test
  public void testOptionTakeDefault() throws Throwable {
    UrlParser jdbc = UrlParser.parse("jdbc:mariadb://localhost/test");
    assertEquals(30_000, jdbc.getOptions().connectTimeout);
    assertTrue(jdbc.getOptions().validConnectionTimeout == 0);
    assertFalse(jdbc.getOptions().autoReconnect);
    assertNull(jdbc.getOptions().user);
    assertFalse(jdbc.getOptions().createDatabaseIfNotExist);
    assertNull(jdbc.getOptions().socketTimeout);
  }

  @Test
  public void testOptionParse() throws Throwable {
    UrlParser jdbc =
        UrlParser.parse(
            "jdbc:mariadb://localhost/test?user=root&password=toto&createDB=true"
                + "&autoReconnect=true&validConnectionTimeout=2&connectTimeout=5&socketTimeout=20");
    assertTrue(jdbc.getOptions().connectTimeout == 5);
    assertTrue(jdbc.getOptions().socketTimeout == 20);
    assertTrue(jdbc.getOptions().validConnectionTimeout == 2);
    assertTrue(jdbc.getOptions().autoReconnect);
    assertTrue(jdbc.getOptions().createDatabaseIfNotExist);

    assertTrue("root".equals(jdbc.getOptions().user));
    assertTrue("root".equals(jdbc.getUsername()));

    assertTrue("toto".equals(jdbc.getOptions().password));
    assertTrue("toto".equals(jdbc.getPassword()));
  }

  @Test
  public void testOptionParseSlash() throws Throwable {
    UrlParser jdbc =
        UrlParser.parse(
            "jdbc:mariadb://127.0.0.1:3306/colleo?user=root&password=toto"
                + "&localSocket=/var/run/mysqld/mysqld.sock");
    assertTrue("/var/run/mysqld/mysqld.sock".equals(jdbc.getOptions().localSocket));

    assertTrue("root".equals(jdbc.getOptions().user));
    assertTrue("root".equals(jdbc.getUsername()));

    assertTrue("toto".equals(jdbc.getOptions().password));
    assertTrue("toto".equals(jdbc.getPassword()));
  }

  @Test
  public void testOptionParseIntegerMinimum() throws Throwable {
    UrlParser jdbc =
        UrlParser.parse(
            "jdbc:mariadb://localhost/test?user=root&autoReconnect=true"
                + "&validConnectionTimeout=0&connectTimeout=5");
    assertTrue(jdbc.getOptions().connectTimeout == 5);
    assertTrue(jdbc.getOptions().validConnectionTimeout == 0);
    assertTrue(jdbc.getOptions().autoReconnect);
    assertTrue("root".equals(jdbc.getOptions().user));
  }

  @Test
  public void testWithoutDb() throws Throwable {
    UrlParser jdbc = UrlParser.parse("jdbc:mariadb://localhost/?user=root&autoReconnect=true");
    assertTrue(jdbc.getOptions().autoReconnect);
    assertNull(jdbc.getDatabase());

    UrlParser jdbc2 = UrlParser.parse("jdbc:mariadb://localhost?user=root&autoReconnect=true");
    assertTrue(jdbc2.getOptions().autoReconnect);
    assertNull(jdbc2.getDatabase());
  }

  @Test(expected = SQLException.class)
  public void testOptionParseIntegerNotPossible() throws Throwable {
    UrlParser.parse(
        "jdbc:mariadb://localhost/test?user=root&autoReconnect=true&validConnectionTimeout=-2"
            + "&connectTimeout=5");
    fail();
  }

  @Test()
  public void testJdbcParserSimpleIpv4basic() throws SQLException {
    String url = "jdbc:mariadb://master:3306,replica1:3307,replica2:3308/database";
    UrlParser.parse(url);
  }

  @Test
  public void testJdbcParserSimpleIpv4basicError() throws SQLException {
    UrlParser urlParser = UrlParser.parse(null);
    assertTrue(urlParser == null);
  }

  @Test
  public void testJdbcParserSimpleIpv4basicwithoutDatabase() throws SQLException {
    String url = "jdbc:mariadb://master:3306,replica1:3307,replica2:3308/";
    UrlParser urlParser = UrlParser.parse(url);
    assertNull(urlParser.getDatabase());
    assertNull(urlParser.getUsername());
    assertNull(urlParser.getPassword());
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(new HostAddress("master", 3306).equals(urlParser.getHostAddresses().get(0)));
    assertTrue(new HostAddress("replica1", 3307).equals(urlParser.getHostAddresses().get(1)));
    assertTrue(new HostAddress("replica2", 3308).equals(urlParser.getHostAddresses().get(2)));
  }

  @Test
  public void testJdbcParserWithoutDatabaseWithProperties() throws SQLException {
    String url = "jdbc:mariadb://master:3306,replica1:3307,replica2:3308?autoReconnect=true";
    UrlParser urlParser = UrlParser.parse(url);
    assertNull(urlParser.getDatabase());
    assertNull(urlParser.getUsername());
    assertNull(urlParser.getPassword());
    assertTrue(urlParser.getOptions().autoReconnect);
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(new HostAddress("master", 3306).equals(urlParser.getHostAddresses().get(0)));
    assertTrue(new HostAddress("replica1", 3307).equals(urlParser.getHostAddresses().get(1)));
    assertTrue(new HostAddress("replica2", 3308).equals(urlParser.getHostAddresses().get(2)));
  }

  @Test
  public void testJdbcParserSimpleIpv4Properties() throws SQLException {
    String url =
        "jdbc:mariadb://master:3306,replica1:3307,replica2:3308/database?autoReconnect=true";
    Properties prop = new Properties();
    prop.setProperty("user", "greg");
    prop.setProperty("password", "pass");

    UrlParser urlParser = UrlParser.parse(url, prop);
    assertTrue("database".equals(urlParser.getDatabase()));
    assertTrue("greg".equals(urlParser.getUsername()));
    assertTrue("pass".equals(urlParser.getPassword()));
    assertTrue(urlParser.getOptions().autoReconnect);
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(new HostAddress("master", 3306).equals(urlParser.getHostAddresses().get(0)));
    assertTrue(new HostAddress("replica1", 3307).equals(urlParser.getHostAddresses().get(1)));
    assertTrue(new HostAddress("replica2", 3308).equals(urlParser.getHostAddresses().get(2)));
  }

  @Test
  public void testJdbcParserBooleanOption() {
    String url = "jdbc:mariadb://master:3306,replica1:3307,replica2:3308?autoReconnect=truee";
    Properties prop = new Properties();
    prop.setProperty("user", "greg");
    prop.setProperty("password", "pass");
    try {
      UrlParser.parse(url, prop);
    } catch (SQLException sqle) {
      assertTrue(
          sqle.getMessage()
              .contains(
                  "Optional parameter autoReconnect must be boolean (true/false or 0/1) was \"truee\""));
    }
  }

  @Test
  public void testJdbcParserSimpleIpv4() throws SQLException {
    String url =
        "jdbc:mariadb://master:3306,replica1:3307,replica2:3308/database?user=greg&password=pass";
    UrlParser urlParser = UrlParser.parse(url);
    assertTrue("database".equals(urlParser.getDatabase()));
    assertTrue("greg".equals(urlParser.getUsername()));
    assertTrue("pass".equals(urlParser.getPassword()));
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(new HostAddress("master", 3306).equals(urlParser.getHostAddresses().get(0)));
    assertTrue(new HostAddress("replica1", 3307).equals(urlParser.getHostAddresses().get(1)));
    assertTrue(new HostAddress("replica2", 3308).equals(urlParser.getHostAddresses().get(2)));
  }

  @Test
  public void testJdbcParserSimpleIpv6() throws SQLException {
    String url =
        "jdbc:mariadb://[2001:0660:7401:0200:0000:0000:0edf:bdd7]:3306,[2001:660:7401:200::edf:bdd7]:3307"
            + "/database?user=greg&password=pass";
    UrlParser urlParser = UrlParser.parse(url);
    assertTrue("database".equals(urlParser.getDatabase()));
    assertTrue("greg".equals(urlParser.getUsername()));
    assertTrue("pass".equals(urlParser.getPassword()));
    assertTrue(urlParser.getHostAddresses().size() == 2);
    assertTrue(
        new HostAddress("2001:0660:7401:0200:0000:0000:0edf:bdd7", 3306)
            .equals(urlParser.getHostAddresses().get(0)));
    assertTrue(
        new HostAddress("2001:660:7401:200::edf:bdd7", 3307)
            .equals(urlParser.getHostAddresses().get(1)));
  }

  @Test
  public void testJdbcParserParameter() throws SQLException {
    String url =
        "jdbc:mariadb://address=(type=master)(port=3306)(host=master1),address=(port=3307)(type=master)"
            + "(host=master2),address=(type=replica)(host=replica1)(port=3308)/database?user=greg&password=pass";
    UrlParser urlParser = UrlParser.parse(url);
    assertTrue("database".equals(urlParser.getDatabase()));
    assertTrue("greg".equals(urlParser.getUsername()));
    assertTrue("pass".equals(urlParser.getPassword()));
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(
        new HostAddress("master1", 3306, "master").equals(urlParser.getHostAddresses().get(0)));
    assertTrue(
        new HostAddress("master2", 3307, "master").equals(urlParser.getHostAddresses().get(1)));
    assertTrue(
        new HostAddress("replica1", 3308, "replica").equals(urlParser.getHostAddresses().get(2)));
    url =
        "jdbc:mariadb://address=(type=master)(port=3306)(host=master1),address=(port=3307)(type=master)"
            + "(host=master2),address=(type=slave)(host=replica1)(port=3308)/database?user=greg&password=pass";
    assertTrue(
        new HostAddress("replica1", 3308, "replica").equals(urlParser.getHostAddresses().get(2)));
  }

  @Test(expected = SQLException.class)
  public void testJdbcParserParameterErrorEqual() throws SQLException {
    String url =
        "jdbc:mariadb://address=(type=)(port=3306)(host=master1),address=(port=3307)(type=master)"
            + "(host=master2),address=(type=replica)(host=replica1)(port=3308)/database?user=greg&password=pass";
    UrlParser.parse(url);
    fail("Must have throw an SQLException");
  }

  @Test
  public void testJdbcParserHaModeNone() throws SQLException {
    String url = "jdbc:mariadb://localhost/database";
    UrlParser jdbc = UrlParser.parse(url);
    assertTrue(jdbc.getHaMode().equals(HaMode.NONE));
  }

  @Test
  public void testJdbcParserHaModeLoadReplication() throws SQLException {
    String url = "jdbc:mariadb:replication://localhost/database";
    UrlParser jdbc = UrlParser.parse(url);
    assertTrue(jdbc.getHaMode().equals(HaMode.REPLICATION));
  }

  @Test
  public void testJdbcParserReplicationParameter() throws SQLException {
    String url =
        "jdbc:mariadb:replication://address=(type=master)(port=3306)(host=master1),address=(port=3307)"
            + "(type=master)(host=master2),address=(type=replica)(host=replica1)(port=3308)/database"
            + "?user=greg&password=pass";
    UrlParser urlParser = UrlParser.parse(url);
    assertTrue("database".equals(urlParser.getDatabase()));
    assertTrue("greg".equals(urlParser.getUsername()));
    assertTrue("pass".equals(urlParser.getPassword()));
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(
        new HostAddress("master1", 3306, "master").equals(urlParser.getHostAddresses().get(0)));
    assertTrue(
        new HostAddress("master2", 3307, "master").equals(urlParser.getHostAddresses().get(1)));
    assertTrue(
        new HostAddress("replica1", 3308, "replica").equals(urlParser.getHostAddresses().get(2)));
  }

  @Test
  public void testJdbcParserReplicationParameterWithoutType() throws SQLException {
    String url = "jdbc:mariadb:replication://master1,replica1,replica2/database";
    UrlParser urlParser = UrlParser.parse(url);
    assertTrue("database".equals(urlParser.getDatabase()));
    assertTrue(urlParser.getHostAddresses().size() == 3);
    assertTrue(
        new HostAddress("master1", 3306, "master").equals(urlParser.getHostAddresses().get(0)));
    assertTrue(
        new HostAddress("replica1", 3306, "replica").equals(urlParser.getHostAddresses().get(1)));
    assertTrue(
        new HostAddress("replica2", 3306, "replica").equals(urlParser.getHostAddresses().get(2)));
  }

  @Test
  public void testJdbcParserHaModeLoadAurora() throws SQLException {
    String url =
        "jdbc:mariadb:aurora://cluster-identifier.cluster-customerID.region.rds.amazonaws.com/database";
    UrlParser jdbc = UrlParser.parse(url);
    assertTrue(jdbc.getHaMode().equals(HaMode.AURORA));
  }

  /**
   * Conj-167 : Driver is throwing IllegalArgumentException instead of returning null.
   *
   * @throws SQLException if any exception occur
   */
  @Test
  public void checkOtherDriverCompatibility() throws SQLException {
    UrlParser jdbc = UrlParser.parse("jdbc:h2:mem:RZM;DB_CLOSE_DELAY=-1");
    assertTrue(jdbc == null);
  }

  /**
   * CONJ-423] driver doesn't accept connection string with "disableMariaDbDriver".
   *
   * @throws SQLException if any exception occur
   */
  @Test
  public void checkDisable() throws SQLException {
    UrlParser jdbc = UrlParser.parse("jdbc:mysql://localhost/test?disableMariaDbDriver");
    assertTrue(jdbc == null);
  }

  @Test
  public void checkHaMode() throws SQLException {
    checkHaMode("jdbc:mysql://localhost/test", HaMode.NONE);
    checkHaMode("jdbc:mariadb://localhost/test", HaMode.NONE);
    checkHaMode("jdbc:mariadb:replication://localhost/test", HaMode.REPLICATION);
    checkHaMode("jdbc:mariadb:replication//localhost/test", HaMode.REPLICATION);
    checkHaMode("jdbc:mariadb:aurora://localhost/test", HaMode.AURORA);
    checkHaMode("jdbc:mariadb:failover://localhost:3306/test", HaMode.LOADBALANCE);
    checkHaMode("jdbc:mariadb:loadbalance://localhost:3306/test", HaMode.LOADBALANCE);

    try {
      checkHaMode("jdbc:mariadb:replicati//localhost/test", HaMode.REPLICATION);
      fail();
    } catch (SQLException sqle) {
      assertTrue(
          sqle.getMessage().contains("wrong failover parameter format in connection String"));
    }
  }

  private void checkHaMode(String url, HaMode expectedHaMode) throws SQLException {
    UrlParser jdbc = UrlParser.parse(url);
    assertEquals(expectedHaMode, jdbc.getHaMode());
  }

  /**
   * CONJ-452 : correcting line break in connection url.
   *
   * @throws SQLException if any exception occur
   */
  @Test
  public void checkInfileCertificate() throws SQLException {
    String url =
        "jdbc:mariadb://1.2.3.4/testj?user=diego"
            + "&profileSql=true&serverSslCert="
            + "-----BEGIN CERTIFICATE-----\n"
            + "MIIDITCCAgmgAwIBAgIBADANBgkqhkiG9w0BAQUFADBIMSMwIQYDVQQDExpHb29n\n"
            + "bGUgQ2xvdWQgU1FMIFNlcnZlciBDQTEUMBIGA1UEChMLR29vZ2xlLCBJbmMxCzAJ\n"
            + "BgNVBAYTAlVTMB4XDTE3MDQyNzEyMjcyNFoXDTE5MDQyNzEyMjgyNFowSDEjMCEG\n"
            + "A1UEAxMaR29vZ2xlIENsb3VkIFNRTCBTZXJ2ZXIgQ0ExFDASBgNVBAoTC0dvb2ds\n"
            + "ZSwgSW5jMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC\n"
            + "ggEBANA6vS37//gvYOsEXKA9Cnlt/C1Ef/a5zJNahpVxx8HbJn6DF+pQbhHm3o4P\n"
            + "TeZp1HoRg5TRiXOEkNTBmgSQbR2+otM2q2gmkn4XAh0+yXkNW3hr2IydJyg9C26v\n"
            + "/OzFvuLcw9iDBvrn433pDa6vjYDU+wiQaVtr1ItzsoE/kgW2IkgFVQB+CrkpAmwm\n"
            + "omwEze3QFUUznP0PHy3P7g7UVD9u5x3APY6kVt2dq8mnOiLZkyfHHR2j6+j0E73I\n"
            + "k3HQv7D0yRIv3kuNpFgJbITVgDIq9ukWU2XinDHUjguCDH+yQAoQH7hOQlWUHIz8\n"
            + "/TtfZjrlUQf2uLzOWCn5KxfEqTkCAwEAAaMWMBQwEgYDVR0TAQH/BAgwBgEB/wIB\n"
            + "ADANBgkqhkiG9w0BAQUFAAOCAQEArYkBkUvMvteT4fN6lxUkzmp8R7clLPkA2HnJ\n"
            + "7IUK3c5GJ0/blffxj/9Oe2g+etga15SIO73GNAnTxhxIJllKRmmh6SR+dwNMkAvE\n"
            + "xp87/Y6cSeJp5d4HhZUvxpFjaUDsWIC8tpbriUJoqGIirprLVcsPgDjKyuvVOlbK\n"
            + "aQf3fOoBPLspGWHgic8Iw1O4kRcStUGCSCwOtYcgMJEhVqTgX0sTX5BgatZhr8FY\n"
            + "Mnoceo2vzzxgHJU9qZuPkpYDs+ipQjzhoIJaY4HU2Uz4jMptqxSdzsPpC6PAKwuN\n"
            + "+LBCR0B194YbRn6726vWwUUE05yskVN6gllGSCgZ/G8y98DhjQ==\n"
            + "-----END CERTIFICATE-----&useSSL=true&password=testj&password=pwd2";
    UrlParser jdbc = UrlParser.parse(url);
    assertEquals("diego", jdbc.getOptions().user);
    assertEquals(true, jdbc.getOptions().profileSql);
    assertEquals(
        "-----BEGIN CERTIFICATE-----\n"
            + "MIIDITCCAgmgAwIBAgIBADANBgkqhkiG9w0BAQUFADBIMSMwIQYDVQQDExpHb29n\n"
            + "bGUgQ2xvdWQgU1FMIFNlcnZlciBDQTEUMBIGA1UEChMLR29vZ2xlLCBJbmMxCzAJ\n"
            + "BgNVBAYTAlVTMB4XDTE3MDQyNzEyMjcyNFoXDTE5MDQyNzEyMjgyNFowSDEjMCEG\n"
            + "A1UEAxMaR29vZ2xlIENsb3VkIFNRTCBTZXJ2ZXIgQ0ExFDASBgNVBAoTC0dvb2ds\n"
            + "ZSwgSW5jMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC\n"
            + "ggEBANA6vS37//gvYOsEXKA9Cnlt/C1Ef/a5zJNahpVxx8HbJn6DF+pQbhHm3o4P\n"
            + "TeZp1HoRg5TRiXOEkNTBmgSQbR2+otM2q2gmkn4XAh0+yXkNW3hr2IydJyg9C26v\n"
            + "/OzFvuLcw9iDBvrn433pDa6vjYDU+wiQaVtr1ItzsoE/kgW2IkgFVQB+CrkpAmwm\n"
            + "omwEze3QFUUznP0PHy3P7g7UVD9u5x3APY6kVt2dq8mnOiLZkyfHHR2j6+j0E73I\n"
            + "k3HQv7D0yRIv3kuNpFgJbITVgDIq9ukWU2XinDHUjguCDH+yQAoQH7hOQlWUHIz8\n"
            + "/TtfZjrlUQf2uLzOWCn5KxfEqTkCAwEAAaMWMBQwEgYDVR0TAQH/BAgwBgEB/wIB\n"
            + "ADANBgkqhkiG9w0BAQUFAAOCAQEArYkBkUvMvteT4fN6lxUkzmp8R7clLPkA2HnJ\n"
            + "7IUK3c5GJ0/blffxj/9Oe2g+etga15SIO73GNAnTxhxIJllKRmmh6SR+dwNMkAvE\n"
            + "xp87/Y6cSeJp5d4HhZUvxpFjaUDsWIC8tpbriUJoqGIirprLVcsPgDjKyuvVOlbK\n"
            + "aQf3fOoBPLspGWHgic8Iw1O4kRcStUGCSCwOtYcgMJEhVqTgX0sTX5BgatZhr8FY\n"
            + "Mnoceo2vzzxgHJU9qZuPkpYDs+ipQjzhoIJaY4HU2Uz4jMptqxSdzsPpC6PAKwuN\n"
            + "+LBCR0B194YbRn6726vWwUUE05yskVN6gllGSCgZ/G8y98DhjQ==\n"
            + "-----END CERTIFICATE-----",
        jdbc.getOptions().serverSslCert);
    assertEquals(true, jdbc.getOptions().useSsl);
    assertEquals("testj", jdbc.getOptions().password);
  }

  /**
   * CONJ-464 : Using of "slowQueryThresholdNanos" option results in class cast exception.
   *
   * @throws SQLException if any exception occur
   */
  @Test
  public void checkBigSlowQueryThresholdNanosValue() throws SQLException {
    String url = "jdbc:mariadb://1.2.3.4/testj?user=john&slowQueryThresholdNanos=" + Long.MAX_VALUE;
    UrlParser jdbc = UrlParser.parse(url);
    assertEquals(Long.MAX_VALUE, jdbc.getOptions().slowQueryThresholdNanos.longValue());
  }

  /**
   * MDEV-15935 : Connection Redirection Mechanism in MariaDB Client/Server Protocol
   *
   * @throws SQLException if any exception occur
   */
  @Test
  public void checkEnableRedirectValue() throws SQLException {
      UrlParser jdbc = UrlParser.parse("jdbc:mysql://hostName:3306?enableRedirect=on");
      assertEquals(jdbc.getOptions().enableRedirect, RedirectionOption.ON.toString().toLowerCase());

      UrlParser jdbc2 = UrlParser.parse("jdbc:mysql://hostName:3306?enableRedirect=off");
      assertEquals(jdbc2.getOptions().enableRedirect, RedirectionOption.OFF.toString().toLowerCase());

      UrlParser jdbc3 = UrlParser.parse("jdbc:mysql://hostName:3306?enableRedirect=preferred");
      assertEquals(jdbc3.getOptions().enableRedirect, RedirectionOption.PREFERRED.toString().toLowerCase());

      UrlParser jdbc4 = UrlParser.parse("jdbc:mysql://hostName:3306");
      assertEquals(jdbc4.getOptions().enableRedirect, RedirectionOption.OFF.toString().toLowerCase());

  }
}
