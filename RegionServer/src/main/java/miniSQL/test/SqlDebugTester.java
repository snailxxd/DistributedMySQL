package miniSQL.test;

import miniSQL.API;
import miniSQL.Interpreter;

/**
 * SQL 调试测试工具 - 用于快速验证SQL语句的准确性
 * 无需启动完整的分布式系统，可以直接测试SQL解析和执行
 */
public class SqlDebugTester {
    
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("【SQL调试测试工具】 - 快速验证SQL语句正确性");
        System.out.println("=".repeat(80) + "\n");
        
        try {
            // 初始化API
            System.out.println("【初始化】正在加载BufferManager、CatalogManager、IndexManager...");
            API.initial();
            System.out.println("【初始化】✅ API初始化完成\n");
            
            // 测试SQL序列
            testSqlStatements();
            
            // 保存数据
            System.out.println("\n【保存】正在保存数据到磁盘...");
            API.store();
            System.out.println("【保存】✅ 数据保存完成\n");
            
        } catch (Exception e) {
            System.out.println("❌ 【错误】" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testSqlStatements() throws Exception {
        // 测试用例集合
        String[][] testCases = {
            {
                "SELECT 测试",
                "select * from student;;"
            },
            {
                "INSERT 测试",
                "insert into student values (1, 'Alice', 20);;"
            },
            {
                "DELETE 测试",
                "delete from student where id = 1;;"
            },
            {
                "CREATE TABLE 测试",
                "create table users (id int, name char(20), age int, primary key (id));;"
            },
            {
                "DROP TABLE 测试",
                "drop table users;;"
            },
            {
                "CREATE INDEX 测试",
                "create index idx_name on student (name);;"
            },
            {
                "DROP INDEX 测试",
                "drop index idx_name;;"
            }
        };
        
        int successCount = 0;
        int failCount = 0;
        
        for (String[] testCase : testCases) {
            String testName = testCase[0];
            String sql = testCase[1];
            
            try {
                System.out.println("━".repeat(80));
                System.out.println("【测试】" + testName);
                System.out.println("【SQL】" + sql);
                System.out.println("━".repeat(80));
                
                String result = Interpreter.interpret(sql);
                
                System.out.println("【✅ 成功】结果: " + (result.isEmpty() ? "(空结果)" : result));
                System.out.println();
                successCount++;
                
            } catch (Exception e) {
                System.out.println("【❌ 失败】" + e.getClass().getSimpleName());
                System.out.println("【错误信息】" + e.getMessage());
                System.out.println();
                failCount++;
            }
        }
        
        // 输出测试统计
        System.out.println("\n" + "=".repeat(80));
        System.out.println("【测试统计】");
        System.out.println("  ✅ 成功: " + successCount + " 个");
        System.out.println("  ❌ 失败: " + failCount + " 个");
        System.out.println("  总计: " + testCases.length + " 个");
        System.out.println("=".repeat(80) + "\n");
    }
}
