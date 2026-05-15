package com.example.master;

import com.example.master.balancer.LoadBalancer;
import com.example.master.cluster.RegionRegistry;
import com.example.master.cluster.RegionWatcher;
import com.example.master.command.CommandDispatcher;
import com.example.master.config.MasterConfig;
import com.example.master.meta.TableMetaStore;
import com.example.master.net.ClientSocketServer;
import com.example.master.service.MasterRequestHandler;
import com.example.master.strategy.FailoverManager;
import com.example.zookeeper.MetadataManager;
import com.example.zookeeper.ZookeeperClient;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Master 进程入口。
 *
 * <p>启动流程：
 * <ol>
 *   <li>解析参数 → MasterConfig</li>
 *   <li>连接 ZooKeeper</li>
 *   <li>初始化元数据存储，从 /meta/table_region_map 拉取全量到内存</li>
 *   <li>初始化指令通道目录 /commands /acks</li>
 *   <li>启动 RegionWatcher，对照 /region_servers 子节点变化，
 *       通过 FailoverManager 处理 DISCOVER / RECOVER / INVALID</li>
 *   <li>启动客户端 Socket 服务</li>
 * </ol>
 *
 * <p>命令行参数（均可省略）：
 * <pre>
 *   --client-port=12345
 *   --zk=localhost:2181
 *   --zk-timeout=5000
 *   --region-port=5000
 *   --regions-znode=/region_servers
 *   --meta-znode=/meta/table_region_map
 *   --commands-znode=/commands
 *   --acks-znode=/acks
 * </pre>
 */
public final class Master {

    public static void main(String[] args) throws Exception {
        MasterConfig config = MasterConfig.fromArgs(args);
        System.out.println("Master starting with config: clientPort=" + config.getClientPort()
                + ", zk=" + config.getZkConnect()
                + ", regionPort=" + config.getRegionDefaultPort());

        ZookeeperClient zkClient = new ZookeeperClient(config.getZkConnect(), config.getZkSessionTimeout());
        zkClient.connect();
        awaitZkConnected(zkClient, config.getZkSessionTimeout());

        MetadataManager metadataManager = new MetadataManager(zkClient);
        TableMetaStore metaStore = new TableMetaStore(metadataManager, zkClient, config.getMetaZNode());
        metaStore.loadFromZk();

        RegionRegistry registry = new RegionRegistry();
        LoadBalancer balancer = new LoadBalancer(registry, metaStore);

        CommandDispatcher dispatcher = new CommandDispatcher(
                zkClient, config.getCommandsZNode(), config.getAcksZNode());
        dispatcher.init();

        FailoverManager failover = new FailoverManager(registry, metaStore, balancer, dispatcher, config);
        RegionWatcher watcher = new RegionWatcher(zkClient, config.getRegionsZNode(), failover);
        watcher.start();

        MasterRequestHandler handler = new MasterRequestHandler(metaStore, registry, balancer);
        ClientSocketServer server = new ClientSocketServer(config.getClientPort(), handler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Master shutting down...");
            server.shutdown();
            watcher.stop();
            try {
                zkClient.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "master-shutdown"));

        server.serve();
    }

    /** 轮询等待 ZK 连接进入 CONNECTED 状态。 */
    private static void awaitZkConnected(ZookeeperClient client, int timeoutMs)
            throws InterruptedException, IOException {
        ZooKeeper zk = client.getZooKeeper();
        long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 1000);
        while (System.currentTimeMillis() < deadline) {
            ZooKeeper.States state = zk.getState();
            if (state == ZooKeeper.States.CONNECTED) {
                return;
            }
            if (state == ZooKeeper.States.CLOSED || state == ZooKeeper.States.AUTH_FAILED) {
                throw new IOException("ZooKeeper connect failed, state=" + state);
            }
            Thread.sleep(100);
        }
        throw new IOException("ZooKeeper connect timeout after " + timeoutMs + " ms");
    }
}
