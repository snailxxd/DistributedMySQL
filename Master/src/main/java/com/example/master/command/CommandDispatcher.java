package com.example.master.command;

import com.alibaba.fastjson.JSONObject;
import com.example.zookeeper.ZookeeperClient;
import org.apache.zookeeper.KeeperException;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通过 ZooKeeper 持久节点向 RegionServer 下发指令。
 *
 * <p>路径约定：
 * <ul>
 *   <li>指令： /commands/&lt;regionId&gt;/cmd-&lt;timestamp&gt;-&lt;seq&gt;
 *       数据为 JSON： {"id": "...", "type": "MIGRATE|CLEAR", "tables": [...], "ts": ...}</li>
 *   <li>回执： /acks/&lt;regionId&gt;/&lt;cmdId&gt;（由 RegionServer 写入；本类只负责约定路径）</li>
 * </ul>
 *
 * <p>不开 socket 长连接的折中：RegionServer 监听自己的 /commands/&lt;regionId&gt;
 * 目录，发现新子节点即按 type 执行。
 */
public final class CommandDispatcher {

    public static final String TYPE_MIGRATE = "MIGRATE";
    public static final String TYPE_CLEAR = "CLEAR";

    private final ZookeeperClient zkClient;
    private final String commandsRoot;
    private final String acksRoot;
    private final AtomicLong seq = new AtomicLong();

    public CommandDispatcher(ZookeeperClient zkClient, String commandsRoot, String acksRoot) {
        this.zkClient = zkClient;
        this.commandsRoot = commandsRoot;
        this.acksRoot = acksRoot;
    }

    /** 启动时调用，预创建根目录。 */
    public void init() throws KeeperException, InterruptedException {
        zkClient.createPersistentNode(commandsRoot, "".getBytes());
        zkClient.createPersistentNode(acksRoot, "".getBytes());
    }

    /**
     * 让 targetRegion 接管 fromRegion 上的若干表（从 FTP/约定位置加载）。
     */
    public String sendMigrate(String targetRegion, String fromRegion, List<String> tables)
            throws KeeperException, InterruptedException {
        JSONObject body = new JSONObject();
        body.put("type", TYPE_MIGRATE);
        body.put("from", fromRegion);
        body.put("tables", tables);
        return dispatch(targetRegion, body);
    }

    /**
     * 让 targetRegion 清空自身存储（用于"曾下线又上线"的节点回收）。
     */
    public String sendClear(String targetRegion) throws KeeperException, InterruptedException {
        JSONObject body = new JSONObject();
        body.put("type", TYPE_CLEAR);
        return dispatch(targetRegion, body);
    }

    /** 通用下发：创建一个 /commands/&lt;region&gt;/cmd-&lt;ts&gt;-&lt;n&gt; 持久节点。 */
    private String dispatch(String regionId, JSONObject body)
            throws KeeperException, InterruptedException {
        ensureRegionDir(regionId);
        long now = System.currentTimeMillis();
        long n = seq.incrementAndGet();
        String cmdId = "cmd-" + now + "-" + n;
        body.put("id", cmdId);
        body.put("ts", now);
        String path = commandsRoot + "/" + regionId + "/" + cmdId;
        zkClient.createPersistentNode(path, body.toJSONString().getBytes());
        System.out.println("Command dispatched: " + path + " -> " + body.toJSONString());
        return cmdId;
    }

    private void ensureRegionDir(String regionId) throws KeeperException, InterruptedException {
        zkClient.createPersistentNode(commandsRoot + "/" + regionId, "".getBytes());
        zkClient.createPersistentNode(acksRoot + "/" + regionId, "".getBytes());
    }
}
