package RegionManagers;

import RegionManagers.SocketManager.ClientSocketManager;
import RegionManagers.SocketManager.MasterSocketManager;
import miniSQL.API;

import java.io.IOException;

/** Region 从节点总控：ZK 注册、Master 连接、Client 监听。 */
public class RegionManager {

    private static final int CLIENT_PORT = 22222;

    private final DataBaseManager catalog;
    private final ClientSocketManager clientGateway;
    private final MasterSocketManager masterLink;
    private final zkServiceManager zkRegistrar;

    public RegionManager() throws IOException, InterruptedException {
        this.catalog = new DataBaseManager();
        this.zkRegistrar = new zkServiceManager();
        this.masterLink = openMasterLink();
        this.clientGateway = new ClientSocketManager(CLIENT_PORT, this.masterLink);
        launchClientAcceptLoop();
    }

    public void run() throws Exception {
        API.initial();
        launchDaemon(this.zkRegistrar);
        launchDaemon(this.masterLink);
        System.out.println("从节点开始运行！");
    }

    private MasterSocketManager openMasterLink() throws IOException {
        MasterSocketManager link = new MasterSocketManager();
        link.sendTableInfoToMaster(this.catalog.getMetaInfo());
        return link;
    }

    private void launchClientAcceptLoop() {
        launchDaemon(this.clientGateway);
    }

    private static void launchDaemon(Runnable task) {
        new Thread(task).start();
    }
}
