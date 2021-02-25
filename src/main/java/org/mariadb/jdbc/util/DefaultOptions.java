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
 */

package org.mariadb.jdbc.util;

import java.lang.reflect.Field;
import java.util.Properties;
import org.mariadb.jdbc.credential.CredentialPlugin;
import org.mariadb.jdbc.internal.util.OptionUtils;
import org.mariadb.jdbc.internal.util.constant.HaMode;
import org.mariadb.jdbc.internal.util.constant.RedirectionOption;

public enum DefaultOptions {
  USER("user", "1.0.0", "Database user name", false),
  PASSWORD("password", "1.0.0", "Password of database user", false),

  /**
   * The connect timeout value, in milliseconds, or zero for no timeout. Default: 30000 (30 seconds)
   * (was 0 before 2.1.2)
   */
  CONNECT_TIMEOUT(
      "connectTimeout",
      30_000,
      0,
      "1.1.8",
      "The connect timeout value, in milliseconds, or zero for no timeout.",
      false),
  PIPE("pipe", "1.1.3", "On Windows, specify named pipe name to connect.", false),
  LOCAL_SOCKET(
      "localSocket",
      "1.1.4",
      "Permits connecting to the database via Unix domain socket, if the server allows it."
          + " \nThe value is the path of Unix domain socket (i.e \"socket\" database parameter : "
          + "select @@socket).",
      false),
  SHARED_MEMORY(
      "sharedMemory",
      "1.1.4",
      "Permits connecting to the database via shared memory, if the server allows "
          + "it. \nThe value is the base name of the shared memory.",
      false),
  TCP_NO_DELAY(
      "tcpNoDelay",
      Boolean.TRUE,
      "1.0.0",
      "Sets corresponding option on the connection socket.",
      false),
  TCP_ABORTIVE_CLOSE(
      "tcpAbortiveClose",
      Boolean.FALSE,
      "1.1.1",
      "Sets corresponding option on the connection socket.",
      false),
  LOCAL_SOCKET_ADDRESS(
      "localSocketAddress",
      "1.1.8",
      "Hostname or IP address to bind the connection socket to a local (UNIX domain) socket.",
      false),
  SOCKET_TIMEOUT(
      "socketTimeout",
      new Integer[] {10000, null, null, null, null, null},
      0,
      "1.1.8",
      "Defined the "
          + "network socket timeout (SO_TIMEOUT) in milliseconds. Value of 0 disables this timeout. \n"
          + "If the goal is to set a timeout for all queries, since MariaDB 10.1.1, the server has permitted a "
          + "solution to limit the query time by setting a system variable, max_statement_time. The advantage is that"
          + " the connection then is still usable.\n"
          + "Default: 0 (standard configuration) or 10000ms (using \"aurora\" failover configuration).",
      false),
  INTERACTIVE_CLIENT(
      "interactiveClient",
      Boolean.FALSE,
      "1.1.8",
      "Session timeout is defined by the wait_timeout "
          + "server variable. Setting interactiveClient to true will tell the server to use the interactive_timeout "
          + "server variable.",
      false),
  DUMP_QUERY_ON_EXCEPTION(
      "dumpQueriesOnException",
      Boolean.FALSE,
      "1.1.0",
      "If set to 'true', an exception is thrown "
          + "during query execution containing a query string.",
      false),
  USE_OLD_ALIAS_METADATA_BEHAVIOR(
      "useOldAliasMetadataBehavior",
      Boolean.FALSE,
      "1.1.9",
      "Metadata ResultSetMetaData.getTableName() returns the physical table name. \"useOldAliasMetadataBehavior\""
          + " permits activating the legacy code that sends the table alias if set.",
      false),
  ALLOW_LOCAL_INFILE(
      "allowLocalInfile", Boolean.TRUE, "1.2.1", "Permit loading data from file", false),
  SESSION_VARIABLES(
      "sessionVariables",
      "1.1.0",
      "<var>=<value> pairs separated by comma, mysql session variables, "
          + "set upon establishing successful connection.",
      false),
  CREATE_DATABASE_IF_NOT_EXISTS(
      "createDatabaseIfNotExist",
      Boolean.FALSE,
      "1.1.8",
      "the specified database in the url will be created if non-existent.",
      false),
  SERVER_TIMEZONE(
      "serverTimezone",
      "1.1.8",
      "Defines the server time zone.\n"
          + "to use only if the jre server has a different time implementation of the server.\n"
          + "(best to have the same server time zone when possible).",
      false),
  NULL_CATALOG_MEANS_CURRENT(
      "nullCatalogMeansCurrent",
      Boolean.TRUE,
      "1.1.8",
      "DatabaseMetaData use current catalog if null.",
      false),
  TINY_INT_IS_BIT(
      "tinyInt1isBit",
      Boolean.TRUE,
      "1.0.0",
      "Datatype mapping flag, handle Tiny as BIT(boolean).",
      false),
  YEAR_IS_DATE_TYPE(
      "yearIsDateType", Boolean.TRUE, "1.0.0", "Year is date type, rather than numerical.", false),
  USE_SSL(
      "useSsl",
      Boolean.FALSE,
      "1.1.0",
      "Force SSL on connection. (legacy alias \"useSSL\")",
      false),
  USER_COMPRESSION(
      "useCompression",
      Boolean.FALSE,
      "1.0.0",
      "Compresses the exchange with the database through gzip."
          + " This permits better performance when the database is not in the same location.",
      false),
  ALLOW_MULTI_QUERIES(
      "allowMultiQueries",
      Boolean.FALSE,
      "1.0.0",
      "permit multi-queries like insert into ab (i) "
          + "values (1); insert into ab (i) values (2).",
      false),
  REWRITE_BATCHED_STATEMENTS(
      "rewriteBatchedStatements",
      Boolean.FALSE,
      "1.1.8",
      "For insert queries, rewrite "
          + "batchedStatement to execute in a single executeQuery.\n"
          + "example:\n"
          + "   insert into ab (i) values (?) with first batch values = 1, second = 2 will be rewritten\n"
          + "   insert into ab (i) values (1), (2). \n"
          + "\n"
          + "If query cannot be rewriten in \"multi-values\", rewrite will use multi-queries : INSERT INTO "
          + "TABLE(col1) VALUES (?) ON DUPLICATE KEY UPDATE col2=? with values [1,2] and [2,3]\" will be rewritten\n"
          + "INSERT INTO TABLE(col1) VALUES (1) ON DUPLICATE KEY UPDATE col2=2;INSERT INTO TABLE(col1) VALUES (3) ON "
          + "DUPLICATE KEY UPDATE col2=4\n"
          + "\n"
          + "when active, the useServerPrepStmts option is set to false",
      false),
  TCP_KEEP_ALIVE(
      "tcpKeepAlive",
      Boolean.TRUE,
      "1.0.0",
      "Sets corresponding option on the connection socket.",
      false),
  TCP_RCV_BUF(
      "tcpRcvBuf",
      (Integer) null,
      0,
      "1.0.0",
      "set buffer size for TCP buffer (SO_RCVBUF).",
      false),
  TCP_SND_BUF(
      "tcpSndBuf",
      (Integer) null,
      0,
      "1.0.0",
      "set buffer size for TCP buffer (SO_SNDBUF).",
      false),
  SOCKET_FACTORY(
      "socketFactory",
      "1.0.0",
      "to use a custom socket factory, set it to the full name of the class that"
          + " implements javax.net.SocketFactory.",
      false),
  PIN_GLOBAL_TX_TO_PHYSICAL_CONNECTION(
      "pinGlobalTxToPhysicalConnection", Boolean.FALSE, "1.1.8", "", false),
  TRUST_SERVER_CERTIFICATE(
      "trustServerCertificate",
      Boolean.FALSE,
      "1.1.1",
      "When using SSL, do not check server's certificate.",
      false),
  SERVER_SSL_CERT(
      "serverSslCert",
      "1.1.3",
      "Permits providing server's certificate in DER form, or server's CA"
          + " certificate. The server will be added to trustStor. This permits a self-signed certificate to be trusted.\n"
          + "Can be used in one of 3 forms : \n"
          + "* serverSslCert=/path/to/cert.pem (full path to certificate)\n"
          + "* serverSslCert=classpath:relative/cert.pem (relative to current classpath)\n"
          + "* or as verbatim DER-encoded certificate string \"------BEGIN CERTIFICATE-----\" .",
      false),
  USE_FRACTIONAL_SECONDS(
      "useFractionalSeconds",
      Boolean.TRUE,
      "1.0.0",
      "Correctly handle subsecond precision in"
          + " timestamps (feature available with MariaDB 5.3 and later).\n"
          + "May confuse 3rd party components (Hibernate).",
      false),
  AUTO_RECONNECT(
      "autoReconnect",
      Boolean.FALSE,
      "1.2.0",
      "Driver must recreateConnection after a failover.",
      false),
  FAIL_ON_READ_ONLY(
      "failOnReadOnly",
      Boolean.FALSE,
      "1.2.0",
      "After a master failover and no other master found,"
          + " back on a read-only host ( throw exception if not).",
      false),
  RETRY_ALL_DOWN(
      "retriesAllDown",
      120,
      0,
      "1.2.0",
      "When using loadbalancing, the number of times the driver should"
          + " cycle through available hosts, attempting to connect.\n"
          + "     * Between cycles, the driver will pause for 250ms if no servers are available.",
      false),
  FAILOVER_LOOP_RETRIES(
      "failoverLoopRetries",
      120,
      0,
      "1.2.0",
      "When using failover, the number of times the driver"
          + " should cycle silently through available hosts, attempting to connect.\n"
          + "     * Between cycles, the driver will pause for 250ms if no servers are available.\n"
          + "     * if set to 0, there will be no silent reconnection",
      false),
  VALID_CONNECTION_TIMEOUT(
      "validConnectionTimeout",
      0,
      0,
      "1.2.0",
      "When in multiple hosts, after this time in"
          + " second without used, verification that the connections haven't been lost.\n"
          + "     * When 0, no verification will be done. Defaults to 0 (120 before 1.5.8 version)",
      false),
  LOAD_BALANCE_BLACKLIST_TIMEOUT(
      "loadBalanceBlacklistTimeout",
      50,
      0,
      "1.2.0",
      "time in second a server is blacklisted after a connection failure.",
      false),
  CACHE_PREP_STMTS(
      "cachePrepStmts",
      Boolean.TRUE,
      "1.3.0",
      "enable/disable prepare Statement cache, default true.",
      false),
  PREP_STMT_CACHE_SIZE(
      "prepStmtCacheSize",
      250,
      0,
      "1.3.0",
      "This sets the number of prepared statements that the "
          + "driver will cache per connection if \"cachePrepStmts\" is enabled.",
      false),
  PREP_STMT_CACHE_SQL_LIMIT(
      "prepStmtCacheSqlLimit",
      2048,
      0,
      "1.3.0",
      "This is the maximum length of a prepared SQL"
          + " statement that the driver will cache  if \"cachePrepStmts\" is enabled.",
      false),
  ASSURE_READONLY(
      "assureReadOnly",
      Boolean.FALSE,
      "1.3.0",
      "Ensure that when Connection.setReadOnly(true) is called, host is in read-only mode by "
          + "setting the session transaction to read-only.",
      false),
  USE_LEGACY_DATETIME_CODE(
      "useLegacyDatetimeCode",
      Boolean.TRUE,
      "1.3.0",
      "if true (default) store date/timestamps "
          + "according to client time zone.\n"
          + "if false, store all date/timestamps in DB according to server time zone, and time information (that is a"
          + " time difference), doesn't take\n"
          + "timezone in account.",
      false),
  MAXIMIZE_MYSQL_COMPATIBILITY(
      "maximizeMysqlCompatibility",
      Boolean.FALSE,
      "1.3.0",
      "maximize MySQL compatibility.\n"
          + "when using jdbc setDate(), will store date in client timezone, not in server timezone when "
          + "useLegacyDatetimeCode = false.\n"
          + "default to false.",
      false),
  USE_SERVER_PREP_STMTS(
      "useServerPrepStmts",
      Boolean.FALSE,
      "1.3.0",
      "useServerPrepStmts must prepared statements be"
          + " prepared on server side, or just faked on client side.\n"
          + "     * if rewriteBatchedStatements is set to true, this options will be set to false.",
      false),
  TRUSTSTORE(
      "trustStore",
      "1.3.0",
      "File path of the trustStore file (similar to java System property "
          + "\"javax.net.ssl.trustStore\"). (legacy alias trustCertificateKeyStoreUrl)\n"
          + "Use the specified file for trusted root certificates.\n"
          + "When set, overrides serverSslCert.",
      false),
  TRUST_CERTIFICATE_KEYSTORE_PASSWORD(
      "trustStorePassword",
      "1.3.0",
      "Password for the trusted root certificate file"
          + " (similar to java System property \"javax.net.ssl.trustStorePassword\").\n"
          + "(legacy alias trustCertificateKeyStorePassword).",
      false),
  KEYSTORE(
      "keyStore",
      "1.3.0",
      "File path of the keyStore file that contain client private key store and associate "
          + "certificates (similar to java System property \"javax.net.ssl.keyStore\", but ensure that only the "
          + "private key's entries are used).(legacy alias clientCertificateKeyStoreUrl).",
      false),
  KEYSTORE_PASSWORD(
      "keyStorePassword",
      "1.3.0",
      "Password for the client certificate keyStore (similar to java "
          + "System property \"javax.net.ssl.keyStorePassword\").(legacy alias clientCertificateKeyStorePassword)",
      false),
  PRIVATE_KEYS_PASSWORD(
      "keyPassword",
      "1.5.3",
      "Password for the private key in client certificate keyStore. (only "
          + "needed if private key password differ from keyStore password).",
      false),
  ENABLED_SSL_PROTOCOL_SUITES(
      "enabledSslProtocolSuites",
      "1.5.0",
      "Force TLS/SSL protocol to a specific set of TLS "
          + "versions (comma separated list). \n"
          + "Example : \"TLSv1, TLSv1.1, TLSv1.2\"\n"
          + "(Alias \"enabledSSLProtocolSuites\" works too)",
      false),
  ENABLED_SSL_CIPHER_SUITES(
      "enabledSslCipherSuites",
      "1.5.0",
      "Force TLS/SSL cipher (comma separated list).\n"
          + "Example : \"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_DSS_WITH_AES_256_GCM_SHA384\"",
      false),
  CONTINUE_BATCH_ON_ERROR(
      "continueBatchOnError",
      Boolean.TRUE,
      "1.4.0",
      "When executing batch queries, must batch continue on error.",
      false),
  JDBC_COMPLIANT_TRUNCATION(
      "jdbcCompliantTruncation",
      Boolean.TRUE,
      "1.4.0",
      "Truncation error (\"Data truncated for"
          + " column '%' at row %\", \"Out of range value for column '%' at row %\") will be thrown as error, and not as warning.",
      false),
  CACHE_CALLABLE_STMTS(
      "cacheCallableStmts",
      Boolean.FALSE,
      "1.4.0",
      "enable/disable callable Statement cache, default false.",
      false),
  CALLABLE_STMT_CACHE_SIZE(
      "callableStmtCacheSize",
      150,
      0,
      "1.4.0",
      "This sets the number of callable statements "
          + "that the driver will cache per VM if \"cacheCallableStmts\" is enabled.",
      false),
  CONNECTION_ATTRIBUTES(
      "connectionAttributes",
      "1.4.0",
      "When performance_schema is active, permit to send server "
          + "some client information in a key;value pair format "
          + "(example: connectionAttributes=key1:value1,key2,value2).\n"
          + "Those informations can be retrieved on server within tables performance_schema.session_connect_attrs "
          + "and performance_schema.session_account_connect_attrs.\n"
          + "This can permit from server an identification of client/application",
      false),
  USE_BATCH_MULTI_SEND(
      "useBatchMultiSend",
      (Boolean) null,
      "1.5.0",
      "*Not compatible with aurora*\n"
          + "Driver will can send queries by batch. \n"
          + "If set to false, queries are sent one by one, waiting for the result before sending the next one. \n"
          + "If set to true, queries will be sent by batch corresponding to the useBatchMultiSendNumber option value"
          + " (default 100) or according to the max_allowed_packet server variable if the packet size does not permit"
          + " sending as many queries. Results will be read later, avoiding a lot of network latency when the client"
          + " and server aren't on the same host. \n"
          + "\n"
          + "This option is mainly effective when the client is distant from the server.",
      false),
  USE_BATCH_MULTI_SEND_NUMBER(
      "useBatchMultiSendNumber",
      100,
      1,
      "1.5.0",
      "When option useBatchMultiSend is active,"
          + " indicate the maximum query send in a row before reading results.",
      false),
  LOGGING(
      "log",
      Boolean.FALSE,
      "1.5.0",
      "Enable log information. \n"
          + "require Slf4j version > 1.4 dependency.\n"
          + "Log level correspond to Slf4j logging implementation",
      false),
  PROFILE_SQL("profileSql", Boolean.FALSE, "1.5.0", "log query execution time.", false),
  MAX_QUERY_LOG_SIZE("maxQuerySizeToLog", 1024, 0, "1.5.0", "Max query log size.", false),
  SLOW_QUERY_TIME(
      "slowQueryThresholdNanos",
      null,
      0L,
      "1.5.0",
      "Will log query with execution time superior to this value (if defined )",
      false),
  PASSWORD_CHARACTER_ENCODING(
      "passwordCharacterEncoding",
      "1.5.9",
      "Indicate password encoding charset. If not set, driver use platform's default charset.",
      false),
  PIPELINE_AUTH(
      "usePipelineAuth",
      (Boolean) null,
      "1.6.0",
      "*Not compatible with aurora*\n"
          + "During connection, different queries are executed. When option is active those queries are send using"
          + " pipeline (all queries are send, then only all results are reads), permitting faster connection "
          + "creation",
      false),
  ENABLE_PACKET_DEBUG(
      "enablePacketDebug",
      Boolean.FALSE,
      "1.6.0",
      "Driver will save the last 16 MariaDB packet "
          + "exchanges (limited to first 1000 bytes). Hexadecimal value of those packets will be added to stacktrace"
          + " when an IOException occur.\n"
          + "This option has no impact on performance but driver will then take 16kb more memory.",
      false),
  SSL_HOSTNAME_VERIFICATION(
      "disableSslHostnameVerification",
      Boolean.FALSE,
      "2.1.0",
      "When using ssl, the driver "
          + "checks the hostname against the server's identity as presented in the server's certificate (checking "
          + "alternative names or the certificate CN) to prevent man-in-the-middle attacks. This option permits "
          + "deactivating this validation. Hostname verification is disabled when the trustServerCertificate "
          + "option is set",
      false),
  ENABLE_REDIRECTION(
      "enableRedirect",
      RedirectionOption.OFF.toString().toLowerCase(),
      "2.1.0",
      "This option determines how redirection will be used: "
          + "\n off: Do not use redirection."
          + "\n on: Must use redirection, fail the connection if server does not support redirection or redirection fails."
          + "\n preferred: Use redirection if supported by the server and redirection is successful; otherwise, do not use redirection.",
      false),
  USE_BULK_PROTOCOL(
      "useBulkStmts",
      Boolean.FALSE,
      "2.1.0",
      "Use dedicated COM_STMT_BULK_EXECUTE protocol for batch "
          + "insert when possible. (batch without Statement.RETURN_GENERATED_KEYS and streams) to have faster batch. "
          + "(significant only if server MariaDB >= 10.2.7)",
      false),
  AUTOCOMMIT(
      "autocommit",
      Boolean.TRUE,
      "2.2.0",
      "Set default autocommit value on connection initialization",
      false),
  POOL(
      "pool",
      Boolean.FALSE,
      "2.2.0",
      "Use pool. This option is useful only if not using a DataSource object, but "
          + "only a connection object.",
      false),
  POOL_NAME(
      "poolName",
      "2.2.0",
      "Pool name that permits identifying threads. default: auto-generated as "
          + "MariaDb-pool-<pool-index>",
      false),
  MAX_POOL_SIZE(
      "maxPoolSize",
      8,
      1,
      "2.2.0",
      "The maximum number of physical connections that the pool should contain.",
      false),
  MIN_POOL_SIZE(
      "minPoolSize",
      (Integer) null,
      0,
      "2.2.0",
      "When connections are removed due to not being used for "
          + "longer than than \"maxIdleTime\", connections are closed and removed from the pool. \"minPoolSize\" "
          + "indicates the number of physical connections the pool should keep available at all times. Should be less"
          + " or equal to maxPoolSize.",
      false),
  MAX_IDLE_TIME(
      "maxIdleTime",
      600,
      Options.MIN_VALUE__MAX_IDLE_TIME,
      "2.2.0",
      "The maximum amount of time in seconds"
          + " that a connection can stay in the pool when not used. This value must always be below @wait_timeout"
          + " value - 45s \n"
          + "Default: 600 in seconds (=10 minutes), minimum value is 60 seconds",
      false),
  POOL_VALID_MIN_DELAY(
      "poolValidMinDelay",
      1000,
      0,
      "2.2.0",
      "When asking a connection to pool, the pool will "
          + "validate the connection state. \"poolValidMinDelay\" permits disabling this validation if the connection"
          + " has been borrowed recently avoiding useless verifications in case of frequent reuse of connections. "
          + "0 means validation is done each time the connection is asked.",
      false),
  STATIC_GLOBAL(
      "staticGlobal",
      Boolean.FALSE,
      "2.2.0",
      "Indicates the values of the global variables "
          + "max_allowed_packet, wait_timeout, autocommit, auto_increment_increment, time_zone, system_time_zone and"
          + " tx_isolation) won't be changed, permitting the pool to create new connections faster.",
      false),
  REGISTER_POOL_JMX(
      "registerJmxPool", Boolean.TRUE, "2.2.0", "Register JMX monitoring pools.", false),
  USE_RESET_CONNECTION(
      "useResetConnection",
      Boolean.FALSE,
      "2.2.0",
      "When a connection is closed() "
          + "(given back to pool), the pool resets the connection state. Setting this option, the prepare command "
          + "will be deleted, session variables changed will be reset, and user variables will be destroyed when the"
          + " server permits it (>= MariaDB 10.2.4, >= MySQL 5.7.3), permitting saving memory on the server if the "
          + "application make extensive use of variables. Must not be used with the useServerPrepStmts option",
      false),
  ALLOW_MASTER_DOWN(
      "allowMasterDownConnection",
      Boolean.FALSE,
      "2.2.0",
      "When using master/replica configuration, "
          + "permit to create connection when master is down. If no master is up, default connection is then a replica "
          + "and Connection.isReadOnly() will then return true.",
      false),
  GALERA_ALLOWED_STATE(
      "galeraAllowedState",
      "2.2.5",
      "Usually, Connection.isValid just send an empty packet to "
          + "server, and server send a small response to ensure connectivity. When this option is set, connector will"
          + " ensure Galera server state \"wsrep_local_state\" correspond to allowed values (separated by comma). "
          + "Example \"4,5\", recommended is \"4\". see galera state to know more.",
      false),
  USE_AFFECTED_ROWS(
      "useAffectedRows",
      Boolean.FALSE,
      "2.3.0",
      "If false (default), use \"found rows\" for the row "
          + "count of statements. This corresponds to the JDBC standard.\n"
          + "If true, use \"affected rows\" for the row count.\n"
          + "This changes the behavior of, for example, UPDATE... ON DUPLICATE KEY statements.",
      false),
  INCLUDE_STATUS(
      "includeInnodbStatusInDeadlockExceptions",
      Boolean.FALSE,
      "2.3.0",
      "add \"SHOW ENGINE INNODB STATUS\" result to exception trace when having a deadlock exception",
      false),
  INCLUDE_THREAD_DUMP(
      "includeThreadDumpInDeadlockExceptions",
      Boolean.FALSE,
      "2.3.0",
      "add thread dump to exception trace when having a deadlock exception",
      false),
  READ_AHEAD(
      "useReadAheadInput",
      Boolean.TRUE,
      "2.4.0",
      "use a buffered inputSteam that read socket available data",
      false),
  KEY_STORE_TYPE(
      "keyStoreType",
      (String) null,
      "2.4.0",
      "indicate key store type (JKS/PKCS12). default is null, then using java default type",
      false),
  TRUST_STORE_TYPE(
      "trustStoreType",
      (String) null,
      "2.4.0",
      "indicate trust store type (JKS/PKCS12). default is null, then using java default type",
      false),
  SERVICE_PRINCIPAL_NAME(
      "servicePrincipalName",
      (String) null,
      "2.4.0",
      "when using GSSAPI authentication, SPN (Service Principal Name) use the server SPN information. When set, "
          + "connector will use this value, ignoring server information",
      false),
  DEFAULT_FETCH_SIZE(
      "defaultFetchSize",
      0,
      0,
      "2.4.2",
      "The driver will call setFetchSize(n) with this value on all newly-created Statements",
      false),
  USE_MYSQL_AS_DATABASE(
      "useMysqlMetadata",
      Boolean.FALSE,
      "2.4.1",
      "force DatabaseMetadata.getDatabaseProductName() "
          + "to return \"MySQL\" as database, not real database type",
      false),
  BLANK_TABLE_NAME_META(
      "blankTableNameMeta",
      Boolean.FALSE,
      "2.4.3",
      "Resultset metadata getTableName always return blank. "
          + "This option is mainly for ORACLE db compatibility",
      false),
  CREDENTIAL_TYPE(
      "credentialType",
      (String) null,
      "2.5.0",
      "Indicate the credential plugin type to use. Plugin must be present in classpath",
      false),
  SERVER_KEY_FILE(
      "serverRsaPublicKeyFile",
      (String) null,
      "2.5.0",
      "Indicate path to MySQL server public key file",
      false),
  ALLOW_SERVER_KEY_RETRIEVAL(
      "allowPublicKeyRetrieval",
      Boolean.FALSE,
      "2.5.0",
      "Permit to get MySQL server key retrieval",
      false),
  TLS_SOCKET_TYPE(
      "tlsSocketType", (String) null, "2.5.0", "Indicate TLS socket type implementation", false),
  TRACK_SCHEMA(
      "trackSchema",
      Boolean.TRUE,
      "2.6.0",
      "manage session_track_schema setting when server has CLIENT_SESSION_TRACK capability",
      false),
  ENSURE_SOCKET_STATE(
      "ensureSocketState",
      Boolean.FALSE,
      "2.7.0",
      "ensure socket state before issuing a new command",
      false);
  private final String optionName;
  private final String description;
  private final boolean required;
  private final Object objType;
  private final Object defaultValue;
  private final Object minValue;
  private final Object maxValue;

  DefaultOptions(
      final String optionName,
      final String implementationVersion,
      String description,
      boolean required) {
    this.optionName = optionName;
    this.description = description;
    this.required = required;
    objType = String.class;
    defaultValue = null;
    minValue = null;
    maxValue = null;
  }

  DefaultOptions(
      final String optionName,
      final String defaultValue,
      final String implementationVersion,
      String description,
      boolean required) {
    this.optionName = optionName;
    this.description = description;
    this.required = required;
    objType = String.class;
    this.defaultValue = defaultValue;
    minValue = null;
    maxValue = null;
  }

  DefaultOptions(
      final String optionName,
      final Boolean defaultValue,
      final String implementationVersion,
      String description,
      boolean required) {
    this.optionName = optionName;
    this.objType = Boolean.class;
    this.defaultValue = defaultValue;
    this.description = description;
    this.required = required;
    minValue = null;
    maxValue = null;
  }

  DefaultOptions(
      final String optionName,
      final Integer defaultValue,
      final Integer minValue,
      final String implementationVersion,
      String description,
      boolean required) {
    this.optionName = optionName;
    this.objType = Integer.class;
    this.defaultValue = defaultValue;
    this.minValue = minValue;
    this.maxValue = Integer.MAX_VALUE;
    this.description = description;
    this.required = required;
  }

  DefaultOptions(
      final String optionName,
      final Long defaultValue,
      final Long minValue,
      final String implementationVersion,
      String description,
      boolean required) {
    this.optionName = optionName;
    this.objType = Long.class;
    this.defaultValue = defaultValue;
    this.minValue = minValue;
    this.maxValue = Long.MAX_VALUE;
    this.description = description;
    this.required = required;
  }

  DefaultOptions(
      final String optionName,
      final Integer[] defaultValue,
      final Integer minValue,
      final String implementationVersion,
      String description,
      boolean required) {
    this.optionName = optionName;
    this.objType = Integer.class;
    this.defaultValue = defaultValue;
    this.minValue = minValue;
    this.maxValue = Integer.MAX_VALUE;
    this.description = description;
    this.required = required;
  }

  public static Options defaultValues(final HaMode haMode) {
    return parse(haMode, "", new Properties());
  }

  /**
   * Generate an Options object with default value corresponding to High Availability mode.
   *
   * @param haMode current high Availability mode
   * @param pool is for pool
   * @return Options object initialized
   */
  public static Options defaultValues(HaMode haMode, boolean pool) {
    Properties properties = new Properties();
    properties.setProperty("pool", String.valueOf(pool));
    Options options = parse(haMode, "", properties);
    postOptionProcess(options, null);
    return options;
  }

  /**
   * Parse additional properties.
   *
   * @param haMode current haMode.
   * @param urlParameters options defined in url
   * @param options initial options
   */
  public static void parse(final HaMode haMode, final String urlParameters, final Options options) {
    Properties prop = new Properties();
    parse(haMode, urlParameters, prop, options);
    postOptionProcess(options, null);
  }

  private static Options parse(
      final HaMode haMode, final String urlParameters, final Properties properties) {
    Options options = parse(haMode, urlParameters, properties, null);
    postOptionProcess(options, null);
    return options;
  }

  /**
   * Parse additional properties .
   *
   * @param haMode current haMode.
   * @param urlParameters options defined in url
   * @param properties options defined by properties
   * @param options initial options
   * @return options
   */
  public static Options parse(
      final HaMode haMode,
      final String urlParameters,
      final Properties properties,
      final Options options) {
    if (urlParameters != null && !urlParameters.isEmpty()) {
      String[] parameters = urlParameters.split("&");
      for (String parameter : parameters) {
        int pos = parameter.indexOf('=');
        if (pos == -1) {
          if (!properties.containsKey(parameter)) {
            properties.setProperty(parameter, "");
          }
        } else {
          if (!properties.containsKey(parameter.substring(0, pos))) {
            properties.setProperty(parameter.substring(0, pos), parameter.substring(pos + 1));
          }
        }
      }
    }
    return parse(haMode, properties, options);
  }

  private static Options parse(
      final HaMode haMode, final Properties properties, final Options paramOptions) {
    final Options options = paramOptions != null ? paramOptions : new Options();

    try {
      // Option object is already initialized to default values.
      // loop on properties,
      // - check DefaultOption to check that property value correspond to type (and range)
      // - set values
      for (final String key : properties.stringPropertyNames()) {
        final String propertyValue = properties.getProperty(key);
        final DefaultOptions o = OptionUtils.OPTIONS_MAP.get(key);
        if (o != null && propertyValue != null) {
          final Field field = Options.class.getField(o.optionName);
          if (o.objType.equals(String.class)) {
            field.set(options, propertyValue);
          } else if (o.objType.equals(Boolean.class)) {
            switch (propertyValue.toLowerCase()) {
              case "":
              case "1":
              case "true":
                field.set(options, Boolean.TRUE);
                break;

              case "0":
              case "false":
                field.set(options, Boolean.FALSE);
                break;

              default:
                throw new IllegalArgumentException(
                    "Optional parameter "
                        + o.optionName
                        + " must be boolean (true/false or 0/1) was \""
                        + propertyValue
                        + "\"");
            }
          } else if (o.objType.equals(Integer.class)) {
            try {
              final Integer value = Integer.parseInt(propertyValue);
              assert o.minValue != null;
              assert o.maxValue != null;
              if (value.compareTo((Integer) o.minValue) < 0
                  || value.compareTo((Integer) o.maxValue) > 0) {
                throw new IllegalArgumentException(
                    "Optional parameter "
                        + o.optionName
                        + " must be greater or equal to "
                        + o.minValue
                        + (((Integer) o.maxValue != Integer.MAX_VALUE)
                            ? " and smaller than " + o.maxValue
                            : " ")
                        + ", was \""
                        + propertyValue
                        + "\"");
              }
              field.set(options, value);
            } catch (NumberFormatException n) {
              throw new IllegalArgumentException(
                  "Optional parameter "
                      + o.optionName
                      + " must be Integer, was \""
                      + propertyValue
                      + "\"");
            }
          } else if (o.objType.equals(Long.class)) {
            try {
              final Long value = Long.parseLong(propertyValue);
              assert o.minValue != null;
              assert o.maxValue != null;
              if (value.compareTo((Long) o.minValue) < 0
                  || value.compareTo((Long) o.maxValue) > 0) {
                throw new IllegalArgumentException(
                    "Optional parameter "
                        + o.optionName
                        + " must be greater or equal to "
                        + o.minValue
                        + (((Long) o.maxValue != Long.MAX_VALUE)
                            ? " and smaller than " + o.maxValue
                            : " ")
                        + ", was \""
                        + propertyValue
                        + "\"");
              }
              field.set(options, value);
            } catch (NumberFormatException n) {
              throw new IllegalArgumentException(
                  "Optional parameter "
                      + o.optionName
                      + " must be Long, was \""
                      + propertyValue
                      + "\"");
            }
          }
        } else {
          // keep unknown option:
          // those might be used in authentication or identity plugin
          options.nonMappedOptions.setProperty(key, properties.getProperty(key));
        }
      }

      // special case : field with multiple default according to HA_MODE
      if (options.socketTimeout == null) {
        options.socketTimeout = ((Integer[]) SOCKET_TIMEOUT.defaultValue)[haMode.ordinal()];
      }

    } catch (NoSuchFieldException | IllegalAccessException n) {
      n.printStackTrace();
    } catch (SecurityException s) {
      // only for jws, so never thrown
      throw new IllegalArgumentException("Security too restrictive : " + s.getMessage());
    }

    return options;
  }

  /**
   * Option initialisation end : set option value to a coherent state.
   *
   * @param options options
   * @param credentialPlugin credential plugin
   */
  public static void postOptionProcess(final Options options, CredentialPlugin credentialPlugin) {

    // disable use server prepare id using client rewrite
    if (options.rewriteBatchedStatements) {
      options.useServerPrepStmts = false;
    }

    // pipe cannot use read and write socket simultaneously
    if (options.pipe != null) {
      options.useBatchMultiSend = false;
      options.usePipelineAuth = false;
    }

    // if min pool size default to maximum pool size if not set
    if (options.pool) {
      options.minPoolSize =
          options.minPoolSize == null
              ? options.maxPoolSize
              : Math.min(options.minPoolSize, options.maxPoolSize);
    }

    // if fetchSize is set to less than 0, default it to 0
    if (options.defaultFetchSize < 0) {
      options.defaultFetchSize = 0;
    }

    if (credentialPlugin != null && credentialPlugin.mustUseSsl()) {
      options.useSsl = Boolean.TRUE;
    }
  }

  /**
   * Generate parameter String equivalent to options.
   *
   * @param options options
   * @param haMode high availability Mode
   * @param sb String builder
   */
  public static void propertyString(
      final Options options, final HaMode haMode, final StringBuilder sb) {
    try {
      boolean first = true;
      for (DefaultOptions o : DefaultOptions.values()) {
        final Object value = Options.class.getField(o.optionName).get(options);

        if (value != null && !value.equals(o.defaultValue)) {
          if (first) {
            first = false;
            sb.append('?');
          } else {
            sb.append('&');
          }
          sb.append(o.optionName).append('=');
          if (o.objType.equals(String.class)) {
            sb.append((String) value);
          } else if (o.objType.equals(Boolean.class)) {
            sb.append(((Boolean) value).toString());
          } else if (o.objType.equals(Integer.class) || o.objType.equals(Long.class)) {
            sb.append(value);
          }
        }
      }
    } catch (NoSuchFieldException | IllegalAccessException n) {
      n.printStackTrace();
    }
  }

  public String getOptionName() {
    return optionName;
  }

  public String getDescription() {
    return description;
  }

  public boolean isRequired() {
    return required;
  }
}