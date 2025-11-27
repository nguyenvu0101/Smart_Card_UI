package hospitalcardgui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
        "jdbc:postgresql://db.hfjwsrzusreadekfxpoh.supabase.co:5432/postgres?sslmode=require";

    private static final String USER = "postgres";

    // Database Password nằm trong Supabase: Settings → Database → Password
    private static final String PASSWORD = "1234567";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {

        // Load JDBC driver
        Class.forName("org.postgresql.Driver");
        System.out.println("✓ Driver PostgreSQL đã tải.");

        // Kết nối Supabase
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("✓ Kết nối Supabase thành công.");

        return conn;
    }
}
