# RegionServer

Minimal RegionServer implementation for DistributedMySQL.

## What it does
- Registers itself in ZooKeeper under `/distributed_mysql/rs/<regionId>`
- Accepts one SQL line per TCP connection
- Routes requests based on table + key, replies `MOVED host:port` when needed
- Rewrites logical table to physical table (e.g., `user_region_0`) before executing via JDBC
- Listens to `/commands/<regionId>` for `CLEAR` and `MIGRATE` commands and writes acks to `/acks/<regionId>`

## Quick start
Adjust `--table`, `--jdbc-url`, and `--region-id` for your environment:

```powershell
mvn -q -pl RegionServer -am test
```

```powershell
mvn -q -pl RegionServer -am exec:java -Dexec.mainClass=com.example.RegionServer -Dexec.args="--table=User --region-id=node_127.0.0.1:5000 --port=5000 --jdbc-url=jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC --jdbc-user=root --jdbc-password= --regions-znode=/distributed_mysql/rs"
```

## Key arguments
- `--table=User` logical table name
- `--physical-table=user_region_0` optional override
- `--region-id=node_<ip:port>` znode name (default uses local IP)
- `--port=5000` TCP port for clients
- `--key-start=0 --key-end=1000` optional range
- `--redirect=host:port` optional redirect when key out of range
- `--zk=localhost:2181` ZooKeeper connect string
- `--regions-znode=/distributed_mysql/rs` znode root for RegionServer registration
- `--meta-znode=/meta/table_region_map` table-to-region mapping path
- `--commands-znode=/commands` command root
- `--acks-znode=/acks` ack root
- `--jdbc-url=jdbc:mysql://...` JDBC URL
- `--jdbc-user=...` / `--jdbc-password=...`

## Notes
- Master must use the same `--regions-znode` and `--meta-znode` paths.
- This implementation returns only a single-line status, not full query results.

