package hospitalcardgui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * File test kết nối database Supabase
 * Sử dụng Connection Pooler (không dùng Direct Connection)
 */
public class DatabaseTest {
    
    // Connection string với Pooler - Transaction mode
    // Host: aws-1-ap-southeast-1.pooler.supabase.com
    // Port: 6543 (Pooler port)
    // User: postgres.hfjwsrzusreadekfxpoh (có project ID)
    private static final String CONNECTION_STRING = 
        "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?user=postgres.hfjwsrzusreadekfxpoh&password=1234567&sslmode=require";
    
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("TEST KẾT NỐI DATABASE SUPABASE - CONNECTION POOLER");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Connection Type: Pooler (Transaction Mode)");
        System.out.println("Host: aws-1-ap-southeast-1.pooler.supabase.com");
        System.out.println("Port: 6543");
        System.out.println("User: postgres.hfjwsrzusreadekfxpoh");
        System.out.println("═══════════════════════════════════════════════════════\n");
        
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Đã load PostgreSQL Driver");
            
            // Kết nối đến database
            System.out.println("🔄 Đang kết nối đến database...");
            Connection conn = DriverManager.getConnection(CONNECTION_STRING);
            
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ KẾT NỐI THÀNH CÔNG!\n");
                
                // Lấy thông tin database
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println("THÔNG TIN DATABASE:");
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println("Database Product: " + metaData.getDatabaseProductName());
                System.out.println("Database Version: " + metaData.getDatabaseProductVersion());
                System.out.println("Driver Name: " + metaData.getDriverName());
                System.out.println("Driver Version: " + metaData.getDriverVersion());
                System.out.println("URL: " + metaData.getURL());
                System.out.println("Username: " + metaData.getUserName());
                System.out.println("═══════════════════════════════════════════════════════");
                
                // Test query đơn giản
                System.out.println("\n🔄 Đang test query...");
                try (var stmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT version()")) {
                    if (rs.next()) {
                        System.out.println("✅ Query thành công!");
                        System.out.println("PostgreSQL Version: " + rs.getString(1));
                    }
                }
                
                conn.close();
                System.out.println("\n✅ Đã đóng kết nối");
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println("TEST HOÀN TẤT - KẾT NỐI THÀNH CÔNG!");
                System.out.println("═══════════════════════════════════════════════════════");
                
            } else {
                System.err.println("❌ Kết nối thất bại: Connection is null or closed");
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ LỖI: Không tìm thấy PostgreSQL Driver!");
            System.err.println("Hãy đảm bảo file postgresql-*.jar đã được thêm vào classpath");
            e.printStackTrace();
            
        } catch (SQLException e) {
            System.err.println("❌ LỖI SQL:");
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Message: " + e.getMessage());
            
            // Kiểm tra nếu là UnknownHostException
            Throwable cause = e.getCause();
            if (cause instanceof java.net.UnknownHostException) {
                System.err.println("\n═══════════════════════════════════════════════════════");
                System.err.println("LỖI: UnknownHostException - Không thể resolve hostname!");
                System.err.println("═══════════════════════════════════════════════════════");
                System.err.println("Nguyên nhân có thể:");
                System.err.println("1. Hostname không đúng hoặc đã thay đổi");
                System.err.println("2. Project Supabase bị pause hoặc xóa");
                System.err.println("3. Vấn đề DNS hoặc mạng");
                System.err.println("4. Firewall chặn kết nối");
                System.err.println("\nHãy kiểm tra:");
                System.err.println("- Connection string trong Supabase Dashboard");
                System.err.println("- Trạng thái project trong Supabase");
                System.err.println("- Kết nối internet");
                System.err.println("═══════════════════════════════════════════════════════");
            }
            
            e.printStackTrace();
            
        } catch (Exception e) {
            System.err.println("❌ LỖI KHÔNG XÁC ĐỊNH:");
            e.printStackTrace();
        }
    }
}

