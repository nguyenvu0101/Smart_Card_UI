package hospitalcardgui;

import javax.smartcardio.*;
import java.util.List;

public class CardManager {

    // 1. Singleton Instance
    private static CardManager instance;
    private CardChannel channel;
    private Card card;

    // Constructor private để chặn new CardManager() bên ngoài
    CardManager() {
    }

    // 2. Hàm getInstance() để truy cập toàn cục
    public static CardManager getInstance() {
        if (instance == null) {
            instance = new CardManager();
        }
        return instance;
    }

    // 3. Hàm kết nối thẻ
    // 3. Hàm kết nối thẻ
    public boolean connect() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty()) {
                System.out.println("Không tìm thấy đầu đọc thẻ!");
                return false;
            }

            CardTerminal terminal = terminals.get(0);
            // Kết nối giao thức T=0 hoặc T=1
            card = terminal.connect("*");
            channel = card.getBasicChannel();
            
            // --- FIX LỖI USER APP: SELECT APPLET THỦ CÔNG ---
            // AID chuẩn (lấy từ ảnh bạn gửi lúc nãy): 00 11 22 33 44 55 67 11
            byte[] aid = {
                (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, 
                (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11
            };
            
            ResponseAPDU resp = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
            
            if (resp.getSW() != 0x9000) {
                // Thử AID phụ nếu AID chính không được (dự phòng)
                byte[] aidBackup = {
                    (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, 
                    (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x00
                };
                resp = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aidBackup));
                
                if (resp.getSW() != 0x9000) {
                    System.err.println("CẢNH BÁO: Không Select được Applet! SW=" + Integer.toHexString(resp.getSW()));
                    // Tùy chọn: return false nếu muốn ép buộc phải Select được
                }
            }
            // ------------------------------------------------
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. Hàm ngắt kết nối
    public void disconnect() {
        try {
            if (card != null) {
                card.disconnect(false);
            }
        } catch (CardException e) {
            e.printStackTrace();
        }
    }

    // 5. Kiểm tra đã kết nối chưa
    public boolean isConnected() {
        return channel != null;
    }
    
    public CardChannel getChannel() {
        return channel;
    }

    // 6. Hàm gửi lệnh APDU cấp thấp (dùng cho CardIssuePanel)
    public ResponseAPDU send(CommandAPDU cmd) throws CardException {
        if (channel == null) {
            throw new CardException("Chưa kết nối thẻ!");
        }
        return channel.transmit(cmd);
    }
}
