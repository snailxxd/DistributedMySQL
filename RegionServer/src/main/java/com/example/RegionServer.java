package com.example;

import com.example.regionserver.config.RegionServerConfig;
import com.example.regionserver.net.RegionSocketServer;
import com.example.regionserver.region.RegionManager;
import com.example.regionserver.service.RegionCommandListener;
import com.example.regionserver.service.RSRequestHandler;
import com.example.regionserver.sql.SqlRoutingParser;
import com.example.regionserver.storage.MySQLStorageEngine;
import com.example.zookeeper.ClusterManager;
import com.example.zookeeper.MetadataManager;
import com.example.zookeeper.ZookeeperClient;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class RegionServer {
    public static void main(String[] args) throws Exception {
        RegionServerConfig config = RegionServerConfig.fromArgs(args);
        config.validate();

        System.out.println("RegionServer starting with config: regionId=" + config.getRegionId()
                + ", table=" + config.getLogicalTable()
                + ", port=" + config.getPort()
                + ", zk=" + config.getZkConnect());

        ZookeeperClient zkClient = new ZookeeperClient(config.getZkConnect(), config.getZkSessionTimeout());
        zkClient.connect();
        awaitZkConnected(zkClient, config.getZkSessionTimeout());

        ClusterManager clusterManager = new ClusterManager(zkClient, config.getRegionId(), config.getRegionsZNode());
        clusterManager.register();

        MetadataManager metadataManager = new MetadataManager(zkClient, config.getMetaZNode());
        RegionManager regionManager = new RegionManager(config, zkClient, metadataManager);

        MySQLStorageEngine storageEngine = new MySQLStorageEngine(
                config.getJdbcUrl(),
                config.getJdbcUser(),
                config.getJdbcPassword());

        SqlRoutingParser routingParser = new SqlRoutingParser();
        RSRequestHandler handler = new RSRequestHandler(regionManager, storageEngine, routingParser);
        RegionSocketServer server = new RegionSocketServer(config.getPort(), handler);

        RegionCommandListener commandListener = new RegionCommandListener(
                zkClient,
                config.getCommandsZNode(),
                config.getAcksZNode(),
                config.getRegionId(),
                regionManager,
                storageEngine);
        commandListener.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("RegionServer shutting down...");
            server.shutdown();
            commandListener.stop();
            try {
                clusterManager.unregister();
                zkClient.close();
            } catch (Exception e) {
                System.err.println("Shutdown cleanup failed: " + e.getMessage());
            }
        }, "regionserver-shutdown"));

        server.serve();
    }

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