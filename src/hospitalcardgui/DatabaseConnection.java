package hospitalcardgui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

public class DatabaseConnection {

    // Connection Pooler - Transaction Mode
    // Sử dụng pooler thay vì direct connection để tránh lỗi UnknownHostException
    // Host: aws-1-ap-southeast-1.pooler.supabase.com
    // Port: 6543 (Pooler port)
    // User: postgres.hfjwsrzusreadekfxpoh (có project ID)
    private static final String URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?user=postgres.hfjwsrzusreadekfxpoh&password=1234567&sslmode=require";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        // User và password đã được đưa vào URL, không cần truyền riêng
        return DriverManager.getConnection(URL);
    }

    // --- 1. HÀM LOGIN ADMIN (Giữ nguyên) ---
    public static boolean verifyAdmin(String username, String inputPassword) {
        String sql = "SELECT password_hash, salt FROM admins WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, username);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String storedHashB64 = rs.getString("password_hash");
                    String storedSaltB64 = rs.getString("salt");
                    
                    if (storedHashB64 == null || storedSaltB64 == null) return false;

                    byte[] salt = Base64.getDecoder().decode(storedSaltB64);
                    // Gọi hàm băm từ CryptoUtils
                    byte[] computedHash = CryptoUtils.deriveKeyArgon2(inputPassword.toCharArray(), salt, 32);
                    String computedHashB64 = Base64.getEncoder().encodeToString(computedHash);
                    
                    return storedHashB64.equals(computedHashB64);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // --- 2. HÀM MAIN ĐỂ KHỞI TẠO ADMIN (CHẠY CÁI NÀY 1 LẦN) ---
    // Cách dùng: Chuột phải vào file này -> Run File
    public static void main(String[] args) {
        System.out.println(">>> BẮT ĐẦU KHỞI TẠO TÀI KHOẢN ADMIN...");
        
        try (Connection conn = getConnection()) {
            // A. Tạo bảng admins nếu chưa có
            String sqlTable = "CREATE TABLE IF NOT EXISTS admins (" +
                              "username VARCHAR(50) PRIMARY KEY, " +
                              "password_hash TEXT NOT NULL, " +
                              "salt TEXT NOT NULL)";
            conn.createStatement().executeUpdate(sqlTable);
            System.out.println("1. Đã kiểm tra bảng 'admins'.");

            // B. Tạo user 'admin' / pass 'admin123'
            String username = "admin";
            String rawPass = "admin123"; // <--- Mật khẩu mặc định
            
            // Sinh Salt và Hash
            byte[] salt = CryptoUtils.generateSalt();
            byte[] hash = CryptoUtils.deriveKeyArgon2(rawPass.toCharArray(), salt, 32);
            
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // C. Insert hoặc Update
            String sqlInsert = "INSERT INTO admins (username, password_hash, salt) VALUES (?, ?, ?) " +
                               "ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash, salt = EXCLUDED.salt";
            
            PreparedStatement pst = conn.prepareStatement(sqlInsert);
            pst.setString(1, username);
            pst.setString(2, hashB64);
            pst.setString(3, saltB64);
            
            int rows = pst.executeUpdate();
            System.out.println("2. Đã cập nhật Admin thành công!");
            System.out.println("   Tài khoản: " + username);
            System.out.println("   Mật khẩu: " + rawPass);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("LỖI: " + e.getMessage());
        }
    }
}
