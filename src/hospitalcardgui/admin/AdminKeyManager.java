package hospitalcardgui.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class AdminKeyManager {

    private static byte[] adminMasterKey = null;
    
    // Salt cố định hệ thống (Không được đổi sau khi đã triển khai)
    private static final String SYSTEM_SALT = "HOSPITAL_SECURE_SYSTEM_2025_SALT";

    // 1. Sinh Key từ mật khẩu khi đăng nhập
    public static void generateKeyFromPassword(String password) {
        try {
            String mix = password + SYSTEM_SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            adminMasterKey = digest.digest(mix.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            adminMasterKey = null;
        }
    }

    // 2. Lấy Key để dùng
    public static byte[] getKey() {
        if (adminMasterKey == null) {
            throw new RuntimeException("Admin Key chưa được khởi tạo! Hãy đăng nhập lại.");
        }
        return adminMasterKey;
    }

    // 3. Kiểm tra Key có tồn tại không
    public static boolean isKeyReady() {
        return adminMasterKey != null;
    }

    // 4. Xóa Key khi đăng xuất
    public static void clearKey() {
        if (adminMasterKey != null) {
            Arrays.fill(adminMasterKey, (byte) 0);
            adminMasterKey = null;
        }
    }
}
