package miniSQL.test;

import miniSQL.API;
import miniSQL.Interpreter;

/**
 * 纯SQL调试测试器 - 专注于SQL解析的准确性
 * 不启动完整的分布式系统
 */
public class PureSqlTester {
    
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("【纯SQL调试测试器】- 测试4条常见SQL语句");
        System.out.println("=".repeat(80) + "\n");
        
        try {
            // 初始化
            System.out.println("【步骤0】初始化API...");
            API.initial();
            System.out.println("【步骤0】✅ 初始化完成\n");
            
            // 测试4条常见SQL
            testSingleSql("create table users (id int, name char(20), age int, primary key(id));;");
            testSingleSql("insert into users values (1, 'Alice', 25);;");
            testSingleSql("select * from users;;");
            testSingleSql("delete from users where id = 1;;");
            
            // 保存
            System.out.println("\n【步骤最后】保存数据到磁盘...");
            API.store();
            System.out.println("【步骤最后】✅ 数据保存完成\n");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 强制退出，防止后台线程继续运行
        System.exit(0);
    }
    
    private static void testSingleSql(String sql) throws Exception {
        System.out.println("━".repeat(80));
        System.out.println("【SQL文本】" + sql);
        System.out.println("━".repeat(80));
        
        String result = Interpreter.interpret(sql);
        
        System.out.println("\n【执行结果】" + (result.isEmpty() ? "(空结果)" : result));
        System.out.println("━".repeat(80) + "\n");
    }
}
