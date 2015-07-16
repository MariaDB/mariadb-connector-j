package org.mariadb.jdbc.failover;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.JDBCUrl;
import org.mariadb.jdbc.MySQLConnection;
import org.mariadb.jdbc.internal.common.UrlHAMode;
import org.mariadb.jdbc.internal.mysql.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.logging.*;

/**
 *  Base util class.
 *  For testing
 *  example mvn test -DdbUrl=jdbc:mysql://localhost:3306,localhost:3307/test?user=root -DlogLevel=FINEST
 *
 *  specific parameters :
 *  defaultMultiHostUrl :
 *
 *
 */
@Ignore
public class BaseMultiHostTest {
    protected static Logger log = Logger.getLogger("org.mariadb.jdbc");

    protected static String initialGaleraUrl;
    protected static String initialAuroraUrl;
    protected static String initialReplicationUrl;
    protected static String initialUrl;


    protected static String proxyGaleraUrl;
    protected static String proxyAuroraUrl;
    protected static String proxyReplicationUrl;
    protected static String proxyUrl;

    protected static String username;
    private static String hostname;
    public enum TestType {
        AURORA, REPLICATION, GALERA, NONE
    }
    public TestType currentType;

    //hosts
    private static HashMap<TestType,TcpProxy[]> proxySet = new HashMap<>();

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.fine("Starting test: " + description.getMethodName());
        }

        protected void finished(Description description) {
            log.fine("finished test: " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void beforeClass()  throws SQLException, IOException {

        initialUrl = System.getProperty("dbUrl");
        initialGaleraUrl = System.getProperty("defaultGaleraUrl");
        initialReplicationUrl = System.getProperty("defaultReplicationUrl");
        initialAuroraUrl = System.getProperty("defaultAuroraUrl");

        if (initialUrl != null)  proxyUrl=createProxies(initialUrl, TestType.NONE);
        if (initialReplicationUrl != null) proxyReplicationUrl=createProxies(initialReplicationUrl, TestType.REPLICATION);
        if (initialGaleraUrl != null) proxyGaleraUrl=createProxies(initialGaleraUrl, TestType.GALERA);
        if (initialAuroraUrl != null) proxyAuroraUrl=createProxies(initialAuroraUrl, TestType.AURORA);
    }

    public static boolean requireMinimumVersion(Connection connection, int major, int minor) throws SQLException {
        DatabaseMetaData md = connection.getMetaData();
        int dbMajor = md.getDatabaseMajorVersion();
        int dbMinor = md.getDatabaseMinorVersion();
        return (dbMajor > major ||
                (dbMajor == major && dbMinor >= minor));
    }

    private static String createProxies(String tmpUrl, TestType proxyType) {
        JDBCUrl tmpJdbcUrl = JDBCUrl.parse(tmpUrl);
        TcpProxy[] tcpProxies = new TcpProxy[tmpJdbcUrl.getHostAddresses().size()];
        username = tmpJdbcUrl.getUsername();
        hostname = tmpJdbcUrl.getHostAddresses().get(0).host;
        String sockethosts = "";
        HostAddress hostAddress;
        for (int i=0;i<tmpJdbcUrl.getHostAddresses().size();i++) {
            try {
                hostAddress = tmpJdbcUrl.getHostAddresses().get(i);
                tcpProxies[i] = new TcpProxy(hostAddress.host, hostAddress.port);
                log.fine("creating socket " + proxyType+" : "+ hostAddress.host + ":" + hostAddress.port + " -> localhost:" + tcpProxies[i].getLocalPort());
                sockethosts+=",address=(host=localhost)(port="+tcpProxies[i].getLocalPort()+")"+((hostAddress.type != null)?"(type="+hostAddress.type+")":"");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        proxySet.put(proxyType, tcpProxies);
        if (tmpJdbcUrl.getHaMode().equals(UrlHAMode.NONE)) {
            return "jdbc:mysql://"+sockethosts.substring(1)+"/"+tmpUrl.split("/")[3];
        } else {
            return "jdbc:mysql:"+tmpJdbcUrl.getHaMode().toString().toLowerCase()+"://"+sockethosts.substring(1)+"/"+tmpUrl.split("/")[3];
        }

    }


    protected Connection getNewConnection() throws SQLException {
        return getNewConnection(null, false);
    }

    protected Connection getNewConnection(boolean proxy) throws SQLException {
        return getNewConnection(null, proxy);
    }

    protected Connection getNewConnection(String additionnalConnectionData, boolean proxy) throws SQLException {
        return getNewConnection(additionnalConnectionData, proxy, false);
    }

    protected Connection getNewConnection(String additionnalConnectionData, boolean proxy, boolean forceNewProxy) throws SQLException {
        if (proxy) {
            String tmpProxyUrl = proxyUrl;
            if (forceNewProxy) {
                tmpProxyUrl = createProxies(initialUrl, currentType);
            }
            if (additionnalConnectionData == null) {
                return DriverManager.getConnection(tmpProxyUrl);
            } else {
                return DriverManager.getConnection(tmpProxyUrl + additionnalConnectionData);
            }
        } else {
            if (additionnalConnectionData == null) {
                return DriverManager.getConnection(initialUrl);
            } else {
                return DriverManager.getConnection(initialUrl + additionnalConnectionData);
            }
        }
    }

    @AfterClass
    public static void afterClass()  throws SQLException {
        if (proxySet !=null) {
            for (TcpProxy[] tcpProxies : proxySet.values()) {
                for (TcpProxy tcpProxy : tcpProxies) {
                    try {
                        tcpProxy.stop();
                    } catch (Exception e) {}
                }
            }
        }
    }

    public void stopProxy(int hostNumber, long millissecond) {
        log.fine("stopping host "+hostNumber);
        proxySet.get(currentType)[hostNumber - 1].restart(millissecond);
    }

    public void stopProxy(int hostNumber) {
        log.fine("stopping host "+hostNumber);
        proxySet.get(currentType)[hostNumber - 1].stop();
    }

    public void restartProxy(int hostNumber) {
        log.fine("restart host "+hostNumber);
        if (hostNumber != -1) proxySet.get(currentType)[hostNumber - 1].restart();
    }
    public void assureProxy() {
        for (TcpProxy[] tcpProxies : proxySet.values()) {
            for (TcpProxy tcpProxy : tcpProxies) {
                    tcpProxy.assureProxyOk();
            }
        }
    }

    public void assureBlackList(Connection connection) {
        try {
            Protocol protocol = getProtocolFromConnection(connection);
            protocol.getProxy().getListener().getBlacklist().clear();
        } catch (Throwable e) { }
    }


    //does the user have super privileges or not?
    public boolean hasSuperPrivilege(Connection connection, String testName) throws SQLException{
        boolean superPrivilege = false;
        Statement st = connection.createStatement();

        // first test for specific user and host combination
        ResultSet rs = st.executeQuery("SELECT Super_Priv FROM mysql.user WHERE user = '" + username + "' AND host = '" + hostname + "'");
        if (rs.next()) {
            superPrivilege = (rs.getString(1).equals("Y") ? true : false);
        } else
        {
            // then check for user on whatever (%) host
            rs = st.executeQuery("SELECT Super_Priv FROM mysql.user WHERE user = '" + username + "' AND host = '%'");
            if (rs.next())
                superPrivilege = (rs.getString(1).equals("Y") ? true : false);
        }

        rs.close();

        if (superPrivilege)
            log.fine("test '" + testName + "' skipped because user '" + username + "' has SUPER privileges");

        return superPrivilege;
    }

    protected Protocol getProtocolFromConnection(Connection conn) throws Throwable {

        Method getProtocol = MySQLConnection.class.getDeclaredMethod("getProtocol", new Class[0]);
        getProtocol.setAccessible(true);
        return (Protocol) getProtocol.invoke(conn);
    }

    public int getServerId(Connection connection) throws Throwable {
        Protocol protocol = getProtocolFromConnection(connection);
        HostAddress hostAddress = protocol.getHostAddress();
        List<HostAddress> hostAddressList = protocol.getJdbcUrl().getHostAddresses();
        return hostAddressList.indexOf(hostAddress) + 1;
    }

    public boolean inTransaction(Connection connection) throws Throwable {
        Protocol protocol = getProtocolFromConnection(connection);
        return protocol.inTransaction();
    }
    boolean isMariadbServer(Connection connection) throws SQLException {
        DatabaseMetaData md = connection.getMetaData();
        return md.getDatabaseProductVersion().indexOf("MariaDB") != -1;
    }
}