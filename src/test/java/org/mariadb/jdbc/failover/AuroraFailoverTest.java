package org.mariadb.jdbc.failover;

import org.junit.*;
import org.mariadb.jdbc.internal.util.constant.HaMode;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class AuroraFailoverTest extends BaseReplication {

    /**
     * Initialisation.
     *
     * @throws SQLException exception
     */
    @BeforeClass()
    public static void beforeClass2() throws SQLException {
        proxyUrl = proxyAuroraUrl;
        Assume.assumeTrue(initialAuroraUrl != null);
    }

    /**
     * Initialisation.
     *
     * @throws SQLException exception
     */
    @Before
    public void init() throws SQLException {
        defaultUrl = initialAuroraUrl;
        currentType = HaMode.AURORA;
    }


    @Test
    public void testErrorWriteOnReplica() throws SQLException {
        Connection connection = null;
        try {
            connection = getNewConnection(false);
            connection.setReadOnly(true);
            Statement stmt = connection.createStatement();
            Assert.assertTrue(connection.isReadOnly());
            try {
                stmt.execute("drop table if exists multinode4");
                log.error("ERROR - > must not be able to write on slave. check if you database is start with --read-only");
                Assert.fail();
            } catch (SQLException e) {
                //normal exception
            }
        } finally {
            connection.close();
        }
    }

    @Test
    public void testReplication() throws SQLException, InterruptedException {
        Connection connection = null;
        try {
            connection = getNewConnection(false);
            Statement stmt = connection.createStatement();
            stmt.execute("drop table  if exists auroraReadSlave");
            stmt.execute("create table auroraReadSlave (id int not null primary key auto_increment, test VARCHAR(10))");

            //wait to be sure slave have replicate data
            Thread.sleep(200);

            connection.setReadOnly(true);
            ResultSet rs = stmt.executeQuery("Select count(*) from auroraReadSlave");
            Assert.assertTrue(rs.next());
            connection.setReadOnly(false);
            stmt.execute("drop table  if exists auroraReadSlave");
        } finally {
            connection.close();
        }
    }

    @Test
    public void failoverSlaveToMasterFail() throws Throwable {
        Connection connection = null;
        try {
            connection = getNewConnection("&retriesAllDown=1&secondsBeforeRetryMaster=1", true);
            int masterServerId = getServerId(connection);
            connection.setReadOnly(true);
            int slaveServerId = getServerId(connection);
            Assert.assertTrue(slaveServerId != masterServerId);

            connection.setCatalog("mysql"); //to be sure there will be a query, and so an error when switching connection
            stopProxy(masterServerId);
            try {
                //must not throw error until there is a query
                connection.setReadOnly(false);
                Assert.fail();
            } catch (SQLException e) {
                //normal exception
            }
        } finally {
            connection.close();
        }
    }

    @Test
    public void pingReconnectAfterRestart() throws Throwable {
        Connection connection = null;
        try {
            connection = getNewConnection("&retriesAllDown=1&secondsBeforeRetryMaster=1&queriesBeforeRetryMaster=50000", true);
            Statement st = connection.createStatement();
            int masterServerId = getServerId(connection);
            stopProxy(masterServerId);

            long stoppedTime = System.currentTimeMillis();
            try {
                st.execute("SELECT 1");
            } catch (SQLException e) {
                //normal exception
            }
            restartProxy(masterServerId);
            long restartTime = System.currentTimeMillis();

            boolean loop = true;
            while (loop) {
                if (!connection.isClosed()) {
                    log.trace("reconnection with failover loop after : "
                            + (System.currentTimeMillis() - stoppedTime) + "ms");
                    loop = false;
                }
                if (System.currentTimeMillis() - restartTime > 15 * 1000) {
                    Assert.fail();
                }
                Thread.sleep(250);
            }
        } finally {
            connection.close();
        }
    }


    @Test
    public void failoverDuringMasterSetReadOnly() throws Throwable {
        Connection connection = null;
        try {
            int masterServerId = -1;
            connection = getNewConnection("&retriesAllDown=1", true);
            masterServerId = getServerId(connection);

            stopProxy(masterServerId);

            connection.setReadOnly(true);

            int slaveServerId = getServerId(connection);

            Assert.assertFalse(slaveServerId == masterServerId);
            Assert.assertTrue(connection.isReadOnly());
        } finally {
            connection.close();
        }
    }


    @Test
    public void reconnectMasterAfterFailover() throws Throwable {
        Connection connection = null;
        try {
            connection = getNewConnection("&retriesAllDown=1", true);
            //if super user can write on slave
            Assume.assumeTrue(!hasSuperPrivilege(connection, "reconnectMasterAfterFailover"));
            Statement st = connection.createStatement();
            st.execute("drop table  if exists multinode2");
            st.execute("create table multinode2 (id int not null primary key , amount int not null) ENGINE = InnoDB");
            st.execute("insert into multinode2 (id, amount) VALUE (1 , 100)");

            int masterServerId = getServerId(connection);
            long stopTime = System.currentTimeMillis();
            stopProxy(masterServerId, 10000);
            try {
                st.execute("insert into multinode2 (id, amount) VALUE (2 , 100)");
                Assert.assertTrue(System.currentTimeMillis() - stopTime > 10);
                Assert.assertTrue(System.currentTimeMillis() - stopTime < 20);
            } catch (SQLException e) {
                //eat exception
            }
        } finally {
            connection.close();
        }
    }


    @Test
    public void failoverMasterWithAutoConnectAndTransaction() throws Throwable {
        Connection connection = null;
        try {
            connection = getNewConnection("&autoReconnect=true&retriesAllDown=1", true);
            Statement st = connection.createStatement();

            final int masterServerId = getServerId(connection);
            st.execute("drop table  if exists multinodeTransaction");
            st.execute("create table multinodeTransaction (id int not null primary key , amount int not null) "
                    + "ENGINE = InnoDB");
            connection.setAutoCommit(false);
            st.execute("insert into multinodeTransaction (id, amount) VALUE (1 , 100)");
            stopProxy(masterServerId);
            Assert.assertTrue(inTransaction(connection));
            try {
                // will to execute the query. if there is a connection error, try a ping, if ok, good, query relaunched.
                // If not good, transaction is considered be lost
                st.execute("insert into multinodeTransaction (id, amount) VALUE (2 , 10)");
                Assert.fail();
            } catch (SQLException e) {
                log.trace("normal error : " + e.getMessage());
            }
            restartProxy(masterServerId);
            try {
                st = connection.createStatement();
                st.execute("insert into multinodeTransaction (id, amount) VALUE (2 , 10)");
            } catch (SQLException e) {
                e.printStackTrace();
                Assert.fail();
            }
        } finally {
            connection.close();
        }
    }

    @Test
    public void testFailMaster() throws Throwable {
        Connection connection = null;
        try {
            connection = getNewConnection("&retriesAllDown=1&autoReconnect=true", true);
            Statement stmt = connection.createStatement();
            int masterServerId = getServerId(connection);
            stopProxy(masterServerId);
            long stopTime = System.currentTimeMillis();
            try {
                stmt.execute("SELECT 1");
                Assert.fail();
            } catch (SQLException e) {
                //normal error
            }
            Assert.assertTrue(!connection.isReadOnly());
            Assert.assertTrue(System.currentTimeMillis() - stopTime < 20 * 1000);
        } finally {
            connection.close();
        }
    }

    /**
     * Conj-79.
     *
     * @throws SQLException exception
     */
    @Test
    public void socketTimeoutTest() throws SQLException {
        Connection connection = null;
        try {
            // set a short connection timeout
            connection = getNewConnection("&socketTimeout=4000", false);

            PreparedStatement ps = connection.prepareStatement("SELECT 1");
            ResultSet rs = ps.executeQuery();
            rs.next();

            // wait for the connection to time out
            ps = connection.prepareStatement("SELECT sleep(5)");

            // a timeout should occur here
            try {
                rs = ps.executeQuery();
                Assert.fail();
            } catch (SQLException e) {
                // check that it's a timeout that occurs
                Assert.assertTrue(e.getMessage().contains("timed out"));
            }
            try {
                ps = connection.prepareStatement("SELECT 2");
                ps.execute();
            } catch (Exception e) {
                Assert.fail();
            }

            try {
                rs = ps.executeQuery();
            } catch (SQLException e) {
                Assert.fail();
            }

            // the connection should not be closed
            assertTrue(!connection.isClosed());
        } finally {
            connection.close();
        }
    }

    /**
     * Conj-166
     * Connection error code must be thrown.
     *
     * @throws SQLException exception
     */
    @Test
    public void testAccessDeniedErrorCode() throws SQLException {
        try {
            DriverManager.getConnection(defaultUrl + "&retriesAllDown=1", "foouser", "foopwd");
            Assert.fail();
        } catch (SQLException e) {
            Assert.assertTrue("28000".equals(e.getSQLState()));
            Assert.assertTrue(1045 == e.getErrorCode());
        }
    }

}
