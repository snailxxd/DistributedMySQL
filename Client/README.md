# Client 模块

客户端负责将 SQL 语句路由到 Master 或 RegionServer。

## 客户端工作流程

1. 若是 `CREATE TABLE` / `DROP TABLE`，直接发给 Master。
2. 其他 SQL 语句直接发给 RegionServer。
3. 发送到 RegionServer 前，会从 SQL 中提取 table 用于定位 RegionServer。
4. 若 RegionServer 返回 MOVED，则刷新缓存并重试一次。

## SQL 支持策略

- 仅识别 `CREATE TABLE` / `DROP TABLE` 两类 DDL 以决定路由。
- 其余 SQL 不做格式限制，原样发送给 RegionServer。
- 为保证可路由，请确保 SQL 中包含 table 名（FROM / INTO / UPDATE / DELETE FROM）。

## 依赖其他模块提供的接口

### Master 模块接口
- 处理 `CREATE TABLE` / `DROP TABLE` 等 DDL。
- 处理 Region 定位请求：
```
LOCATE_TABLE <table>
```

### RegionServer 模块接口
- 接收所有非 CREATE/DROP 的 SQL 原文。
- 返回原始响应字符串（如 `OK` / `OK <value>` / `ERR <message>` / `MOVED host:port`）。

## 运行方式

客户端为交互式输入：
```
SQL 语句
exit
```


## 协议

Master 请求：
```
LOCATE_TABLE <table>
```
Master 响应：
```
REGION host:port
ERR <message>
```
