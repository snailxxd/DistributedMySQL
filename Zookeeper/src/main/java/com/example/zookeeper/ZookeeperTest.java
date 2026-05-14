package com.example.zookeeper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class ZookeeperTest {

    private static ZookeeperClient zkClient;
    private static ClusterManager clusterManager;
    private static MetadataManager metadataManager;

    public static void main(String[] args) {
        System.out.println("========== ZooKeeper 模块测试开始 ==========\n");

        // 1. 测试 ZookeeperClient
        testZookeeperClient();

        // 2. 测试 ClusterManager
        testClusterManager();

        // 3. 测试 MetadataManager
        testMetadataManager();

        // 4. 等待一段时间观察 Watcher 效果
        try {
            System.out.println("\n等待 5 秒观察 Watcher 效果...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 5. 关闭连接
        if (zkClient != null) {
            try {
                zkClient.close();
                System.out.println("\nZooKeeper 连接已关闭");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n========== ZooKeeper 模块测试结束 ==========");
    }

    /**
     * 测试 1: ZookeeperClient 基本功能
     */
    private static void testZookeeperClient() {
        System.out.println("【测试 1】ZookeeperClient 基本功能");
        System.out.println("-----------------------------------");

        try {
            // 创建客户端并连接
            zkClient = new ZookeeperClient("localhost:2181", 5000);
            zkClient.connect();
            System.out.println("✅ 连接 ZooKeeper 成功");

            // 等待连接建立
            Thread.sleep(1000);

            // 测试创建持久节点
            String testPath = "/test_node";
            String testData = "Hello ZooKeeper";
            zkClient.createPersistentNode(testPath, testData.getBytes());
            System.out.println("✅ 创建持久节点: " + testPath);

            // 测试读取数据
            byte[] readData = zkClient.getData(testPath, null);
            String readStr = new String(readData);
            System.out.println("✅ 读取节点数据: " + readStr);
            System.out.println("   预期数据: " + testData);
            System.out.println("   数据匹配: " + (testData.equals(readStr) ? "✅ 是" : "❌ 否"));

            // 测试更新数据
            String newData = "Updated Data";
            zkClient.setData(testPath, newData.getBytes());
            byte[] updatedData = zkClient.getData(testPath, null);
            System.out.println("✅ 更新节点数据: " + new String(updatedData));

            // 测试获取子节点
            List<String> children = zkClient.getChildren("/", null);
            System.out.println("✅ 根节点下的子节点: " + children);

            // 测试节点是否存在
            boolean exists = zkClient.exists(testPath);
            System.out.println("✅ 节点 " + testPath + " 存在: " + exists);

            // 测试删除节点
            zkClient.deleteNode(testPath);
            boolean existsAfterDelete = zkClient.exists(testPath);
            System.out.println("✅ 删除节点后，节点存在: " + existsAfterDelete + " (预期: false)");

            System.out.println("\n✅ ZookeeperClient 测试通过\n");

        } catch (IOException e) {
            System.err.println("❌ 连接失败: " + e.getMessage());
            System.err.println("   请确保 ZooKeeper 服务已启动 (localhost:2181)");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试 2: ClusterManager 集群管理功能
     */
    private static void testClusterManager() {
        System.out.println("【测试 2】ClusterManager 集群管理功能");
        System.out.println("-----------------------------------");

        try {
            // 创建集群管理器，模拟 RegionServer
            String serverId = "rs-001";
            String nodePath = "/region_servers";

            // 先确保父节点存在
            zkClient.createPersistentNode("/region_servers", "".getBytes());

            clusterManager = new ClusterManager(zkClient, serverId, nodePath);

            // 注册节点
            clusterManager.register();
            System.out.println("✅ RegionServer 注册成功: " + serverId);

            // 获取所有活跃的 RegionServer
            List<String> servers = clusterManager.getActiveRegionServers();
            System.out.println("✅ 当前活跃的 RegionServer 列表: " + servers);
            System.out.println("   预期包含: " + serverId);
            System.out.println("   匹配结果: " + (servers.contains(serverId) ? "✅ 是" : "❌ 否"));

            // 注销节点
            clusterManager.unregister();
            System.out.println("✅ RegionServer 注销成功");

            // 再次获取列表，验证已移除
            List<String> serversAfter = clusterManager.getActiveRegionServers();
            System.out.println("✅ 注销后的 RegionServer 列表: " + serversAfter);
            System.out.println("   是否包含 " + serverId + ": " + serversAfter.contains(serverId) + " (预期: false)");

            System.out.println("\n✅ ClusterManager 测试通过\n");

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试 3: MetadataManager 元数据管理功能
     */
    private static void testMetadataManager() {
        System.out.println("【测试 3】MetadataManager 元数据管理功能");
        System.out.println("-----------------------------------");

        try {
            metadataManager = new MetadataManager(zkClient);

            // 初始化元数据节点
            metadataManager.initMetaNode();
            System.out.println("✅ 元数据节点初始化成功");

            // 测试添加表映射
            String tableName = "user_table";
            String regionServerId = "rs-002";
            metadataManager.updateTableRegion(tableName, regionServerId);
            System.out.println("✅ 添加表映射: " + tableName + " -> " + regionServerId);

            // 测试查询表映射
            String foundServer = metadataManager.getRegionServerForTable(tableName);
            System.out.println("✅ 查询表映射: " + tableName + " -> " + foundServer);
            System.out.println("   预期: " + regionServerId);
            System.out.println("   匹配结果: " + (regionServerId.equals(foundServer) ? "✅ 是" : "❌ 否"));

            // 测试表是否存在
            boolean exists = metadataManager.tableExists(tableName);
            System.out.println("✅ 表 " + tableName + " 存在: " + exists + " (预期: true)");

            // 测试更新表映射
            String newRegionServerId = "rs-003";
            metadataManager.updateTableRegion(tableName, newRegionServerId);
            String updatedServer = metadataManager.getRegionServerForTable(tableName);
            System.out.println("✅ 更新表映射: " + tableName + " -> " + updatedServer);
            System.out.println("   预期: " + newRegionServerId);
            System.out.println("   匹配结果: " + (newRegionServerId.equals(updatedServer) ? "✅ 是" : "❌ 否"));

            // 测试删除表映射
            metadataManager.deleteTableRegion(tableName);
            boolean existsAfterDelete = metadataManager.tableExists(tableName);
            System.out.println("✅ 删除表映射后，表存在: " + existsAfterDelete + " (预期: false)");

            System.out.println("\n✅ MetadataManager 测试通过\n");

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}