package hospitalcardgui;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtils {

    // 1. Cấu hình Argon2
    private static final int SALT_LEN = 16;
    private static final int HASH_LEN = 32; // 32 bytes = 256 bits
    private static final int AES_KEY_LEN = 16; // 16 bytes = 128 bits (cho Master Key)

    // 2. Sinh chuỗi Salt ngẫu nhiên
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // 3. Sinh Master Key ngẫu nhiên (AES 128 bit)
    public static byte[] generateMasterKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey key = keyGen.generateKey();
        return key.getEncoded();
    }

    // 4. Hàm băm Argon2: Password + Salt -> Key (byte[])
    public static byte[] deriveKeyArgon2(char[] password, byte[] salt, int outputLen) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(3)       // Số vòng lặp (tăng lên nếu muốn chậm hơn/an toàn hơn)
                .withMemoryAsKB(4096)    // 4MB RAM
                .withParallelism(1)
                .withSalt(salt);

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(builder.build());

        byte[] result = new byte[outputLen];
        gen.generateBytes(password, result);
        return result;
    }

    // 5. Mã hóa AES (ECB mode cho đơn giản, hoặc CBC nếu muốn xịn hơn)
    // Dùng để Wrap Key (Mã hóa MasterKey bằng K_User/K_Admin)
    public static byte[] aesEncrypt(byte[] data, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); 
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    // 6. Giải mã AES
    public static byte[] aesDecrypt(byte[] encryptedData, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }
}
