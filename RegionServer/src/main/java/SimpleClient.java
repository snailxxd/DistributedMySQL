import java.io.*;
import java.net.Socket;

/**
 * RegionServer 测试客户端
 * 连接到 localhost:22222 并发送 SQL 语句
 */
public class SimpleClient {
    
    public static void main(String[] args) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("【RegionServer 测试客户端】");
        System.out.println("=".repeat(80) + "\n");
        
        try (Socket socket = new Socket("localhost", 22222)) {
            
            BufferedReader serverInput = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter serverOutput = new PrintWriter(
                socket.getOutputStream(), true
            );
            BufferedReader consoleInput = new BufferedReader(
                new InputStreamReader(System.in)
            );
            
            // 读取服务器欢迎信息
            String line;
            while ((line = serverInput.readLine()) != null) {
                System.out.println(line);
                if (line.startsWith("【说明】")) break;
            }
            
            // 交互式输入
            System.out.println("\n【准备】输入 SQL 语句（以;; 结尾）或输入 quit 退出\n");
            
            while (true) {
                System.out.print("SQL> ");
                String input = consoleInput.readLine();
                
                if (input == null || input.equalsIgnoreCase("quit")) {
                    break;
                }
                
                if (input.trim().isEmpty()) {
                    continue;
                }
                
                // 发送 SQL
                serverOutput.println(input);
                
                // 读取响应
                System.out.println();
                while ((line = serverInput.readLine()) != null) {
                    System.out.println(line);
                    if (line.startsWith("━") && line.length() > 40) break;
                }
                System.out.println();
            }
            
            System.out.println("【退出】连接已关闭");
            
        } catch (IOException e) {
            System.err.println("❌ 【错误】无法连接到 RegionServer");
            System.err.println("   请确保 RegionServer 正在监听 localhost:22222");
            System.err.println("   错误信息: " + e.getMessage());
        }
    }
}
