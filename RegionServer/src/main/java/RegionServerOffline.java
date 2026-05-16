import RegionManagers.SocketManager.ClientSocketManager;
import miniSQL.API;

import java.io.IOException;

/**
 * RegionServer 离线版本 - 用于网络接口测试
 * 不依赖 MasterServer 和 ZooKeeper，直接启动客户端监听
 * 
 * 使用方式：
 * 1. 运行此程序，监听 22222 端口
 * 2. 用客户端连接 localhost:22222
 * 3. 发送 SQL 语句即可
 */
public class RegionServerOffline {

    private static final int CLIENT_PORT = 22222;

    public static void main(String[] args) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("【RegionServer 离线版本 - 用于测试网络接口】");
        System.out.println("=".repeat(80));
        
        try {
            // 步骤1: 初始化 API
            System.out.println("\n【步骤1】初始化 API (BufferManager, CatalogManager, IndexManager)...");
            API.initial();
            System.out.println("【步骤1】✅ API 初始化完成\n");
            
            // 步骤2: 创建客户端监听器（不连接 MasterServer）
            System.out.println("【步骤2】启动客户端监听器...");
            OfflineClientSocketManager clientGateway = new OfflineClientSocketManager(CLIENT_PORT);
            System.out.println("【步骤2】✅ 客户端监听器启动完成\n");
            
            // 步骤3: 在后台线程启动监听
            System.out.println("【步骤3】在后台线程启动监听...");
            Thread clientThread = new Thread(clientGateway);
            clientThread.setDaemon(false);
            clientThread.start();
            System.out.println("【步骤3】✅ 后台线程启动完成\n");
            
            // 输出连接信息
            System.out.println("━".repeat(80));
            System.out.println("【服务状态】正在监听客户端连接...");
            System.out.println("【端口】22222");
            System.out.println("【地址】localhost:22222");
            System.out.println("【用途】测试 SQL 通过网络通信");
            System.out.println("━".repeat(80) + "\n");
            
            System.out.println("【客户端连接方式】");
            System.out.println("  Windows: telnet localhost 22222");
            System.out.println("  Linux:   nc localhost 22222");
            System.out.println("  Java客户端: 见下面的 SimpleClient.java\n");
            
            System.out.println("【发送 SQL 示例】");
            System.out.println("  create table test(id int, name char(10), primary key(id));;");
            System.out.println("  insert into test values(1, 'hello');;");
            System.out.println("  select * from test;;");
            System.out.println("  delete from test where id=1;;");
            System.out.println("\n【按 Ctrl+C 退出】\n");
            
            // 保持程序运行
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("❌ 【错误】" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 离线版本的客户端管理器 - 简化版）
     */
    static class OfflineClientSocketManager implements Runnable {
        private final java.net.ServerSocket listenSocket;
        
        OfflineClientSocketManager(int port) throws IOException {
            this.listenSocket = new java.net.ServerSocket(port);
        }
        
        @Override
        public void run() {
            try {
                int clientCount = 0;
                while (true) {
                    java.net.Socket incoming = listenSocket.accept();
                    clientCount++;
                    System.out.println("\n【新连接】客户端 #" + clientCount + " 已连接");
                    System.out.println("【地址】" + incoming.getInetAddress() + ":" + incoming.getPort());
                    
                    // 为每个客户端创建处理线程
                    OfflineClientHandler handler = new OfflineClientHandler(incoming, clientCount);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                System.err.println("❌ 监听异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 单个客户端处理器
     */
    static class OfflineClientHandler implements Runnable {
        private final java.net.Socket client;
        private final int clientId;
        
        OfflineClientHandler(java.net.Socket client, int clientId) {
            this.client = client;
            this.clientId = clientId;
        }
        
        @Override
        public void run() {
            try (
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(client.getInputStream())
                );
                java.io.PrintWriter writer = new java.io.PrintWriter(
                    client.getOutputStream(), true
                )
            ) {
                writer.println("═".repeat(80));
                writer.println("【欢迎】RegionServer 离线测试版本");
                writer.println("【客户端ID】" + clientId);
                writer.println("【说明】输入 SQL 语句（必须以;; 结尾），或输入 quit 退出");
                writer.println("═".repeat(80));
                
                String sql;
                while ((sql = reader.readLine()) != null) {
                    if (sql.trim().equalsIgnoreCase("quit")) {
                        writer.println("【状态】连接已关闭");
                        break;
                    }
                    
                    if (sql.trim().isEmpty()) {
                        continue;
                    }
                    
                    System.out.println("\n【客户端 #" + clientId + " 请求】" + sql);
                    
                    try {
                        // 执行 SQL
                        String result = miniSQL.Interpreter.interpret(sql + ";;");
                        
                        // 返回结果
                        writer.println("━".repeat(80));
                        writer.println("【结果】");
                        writer.println(result);
                        writer.println("━".repeat(80));
                        
                        System.out.println("【结果返回】已发送给客户端 #" + clientId);
                        
                    } catch (Exception e) {
                        writer.println("❌ 【错误】" + e.getMessage());
                        System.out.println("【执行异常】" + e.getMessage());
                    }
                }
                
                System.out.println("【客户端 #" + clientId + " 断开连接】");
                
            } catch (IOException e) {
                System.err.println("【客户端 #" + clientId + " 异常】" + e.getMessage());
            }
        }
    }
}
