# Changelog
* [1.4.6](#1.4.6) Released on 13 june 2016 
* [1.4.5](#1.4.5) Released on 18 mai 2016 
* [1.4.4](#1.4.4) Released on 04 mai 2016 
* [1.4.3](#1.4.3) Released on 22 april 2016 
* [1.4.2](#1.4.2) Released on 08 april 2016
* [1.4.1](#1.4.1) Released on 07 april 2016
* [1.4.0](#1.4.0) Released on 31 march 2016

---

## 1.4.6
* [CONJ-293] Permit named pipe connection without host
* [CONJ-309] Possible NPE on aurora when failover occur during connection initialisation
* [CONJ-312] NPE while loading a null from TIMESTAMP field using binary protocol
* [misc] batch with one parameter correction (using rewriteBatchedStatements option)

## 1.4.5
* [CONJ-297] Useless memory consumption when using Statement.setQueryTimeout
* [CONJ-294] PrepareStatement on master reconnection after a failover
* [CONJ-288] using SHOW VARIABLES to replace SELECT on connection to permit connection on a galera non primary node
* [CONJ-290] Timestamps format error when using prepareStatement with options useFractionalSeconds and useServerPrepStmts

## 1.4.4
* [CONJ-289] PrepareStatement on master reconnection after a failover
* [CONJ-288] using SHOW VARIABLES to replace SELECT on connection to permit connection on a galera non primary node

## 1.4.3

* [CONJ-284] Cannot read autoincremented IDs bigger than Short.MAX_VALUE
* [CONJ-283] Parsing correction on MariaDbClientPreparedStatement - syntax error on insert values
* [CONJ-282] Handling YEARs with binary prepareStatement
* [CONJ-281] Connector/J is incompatible with Google App Engine correction
* [CONJ-278] Improve prepared statement on failover

## 1.4.2

* [CONJ-275] Streaming result without result throw "Current position is before the first row"


## 1.4.1


* [CONJ-274] correction to permit connection to MySQL 5.1 server
* [CONJ-273] correction when using prepareStatement without parameters and option rewriteBatchedStatements to true
* [CONJ-270] permit 65535 parameters to server preparedStatement
* [CONJ-268] update license header
* [misc] when option rewriteBatchedStatements is set to true, correction of packet separation when query size > max_allow_packet
* [misc] performance improvement for select result.

## 1.4.0

### Complete implementation of fetch size.
CONJ-26
JDBC allows to specify the number of rows fetched for a query, and this number is referred to as the fetch size
Before version 1.4.0, query were loading all results or row by row using Statement.setFetchSize(Integer.MIN_VALUE).
Now it's possible to set fetch size according to your need. 
Loading all results for large result sets is using a lot of memory. This functionnality permit to save memory without having performance decrease.

### Memory footprint improvement
CONJ-125
Buffers have been optimized to reduced memory footprint

### CallableStatement  performance improvement.
CONJ-209
Calling function / procedure performance is now optimized according to query. Depending on queries, difference can be up to 300%.

### Authentication evolution
CONJ-251 Permit now new authentication possibility : [PAM authentication](https://mariadb.com/kb/en/mariadb/pam-authentication-plugin/), and GSSAPI/SSPI authentication.

GSSAPI/SSPI authentication authentication plugin for MariaDB permit a passwordless login.

On Unix systems, GSSAPI is usually synonymous with Kerberos authentication. Windows has slightly different but very similar API called SSPI, that along with Kerberos, also supports NTLM authentication.
See more detail in [GSSAPI/SSPI configuration](https://github.com/MariaDB/mariadb-connector-j/blob/master/documentation/plugin/GSSAPI.md)

### Connection attributes
CONJ-217
Driver information informations are now send to [connection attributes tables](https://mariadb.com/kb/en/mariadb/performance-schema-session_connect_attrs-table/) (performance_schema must be activated).
A new option "connectionAttributes" permit to add client specifics data.

For example when connecting with the following connection string {{{"jdbc:mysql://localhost:3306/testj?user=root&connectionAttributes=myOption:1,mySecondOption:'jj'"}}}, 
if performance_schema is activated, information about this connection will be available during the time this connection is active :
``` java
select * from performance_schema.session_connect_attrs where processList_id = 5
+----------------+-----------------+---------------------+------------------+
| PROCESSLIST_ID | ATTR_NAME       | ATTR_VALUE          | ORDINAL_POSITION |
+----------------+-----------------+---------------------+------------------+
|5               |_client_name     |MariaDB connector/J  |0                 |
|5               |_client_version  |1.4.0-SNAPSHOT       |1                 |
|5               |_os              |Windows 8.1          |2                 | 
|5               |_pid             |14124@portable-diego |3                 |
|5               |_thread          |5                    |4                 |
|5               |_java_vendor     |Oracle Corporation	 |5                 |
|5               |_java_version    |1.7.0_79	         |6                 |
|5               |myOption         |1	                 |7                 |
|5               |mySecondOption   |'jj'                 |8                 |
+----------------+-----------------+---------------------+------------------+
```


## Minor evolution
* CONJ-210 : adding a "jdbcCompliantTruncation" option to force truncation warning as SQLException.
* CONJ-211 : when in master/slave configuration, option "assureReadOnly" will ensure that slaves are in read-only mode ( forcing transaction by a query "SET SESSION TRANSACTION READ ONLY"). 
* CONJ-213 : new option "continueBatchOnError". Permit to continue batch when an exception occur : When executing a batch and an error occur, must the batch stop immediatly (default) or finish remaining batch before throwing exception.

## Bugfix
* CONJ-236 : Using a parametrized query with a smallint -1 does return the unsigned value
* CONJ-250 : Tomcat doesn't stop when using Aurora failover configuration
* CONJ-260 : Add jdbc nString, nCharacterStream, nClob implementation
* CONJ-269 : handle server configuration autocommit=0
* CONJ-271 : ResultSet.first() may throw SQLDataException: Current position is before the first row
