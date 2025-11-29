package hospitalcardgui;

import javax.smartcardio.*;
import java.util.List;

public class CardManager {

    private static CardManager instance;
    private CardChannel channel;
    private Card card;

    CardManager() { }

    public static CardManager getInstance() {
        if (instance == null) instance = new CardManager();
        return instance;
    }

    public boolean connect() {
        try {
            // 1. Tìm Terminal
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty()) {
                System.out.println("Không tìm thấy đầu đọc thẻ!");
                return false;
            }

            // 2. Kết nối vật lý
            CardTerminal terminal = terminals.get(0);
            card = terminal.connect("*"); // T=0 hoặc T=1
            channel = card.getBasicChannel();
            
            // --- QUAN TRỌNG: KHÔNG SELECT APPLET Ở ĐÂY ---
            // Lý do: Để các Panel (Write/Pin) tự Select. 
            // Nếu Select ở đây mà lỗi, các Panel sau không biết, cứ gửi lệnh -> Lỗi 6F00.
            // Hãy để logic nghiệp vụ (Select AID nào) cho tầng trên quyết định.
            
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        try {
            if (card != null) card.disconnect(false);
        } catch (CardException e) { e.printStackTrace(); }
    }

    public boolean isConnected() { return channel != null; }
    
    public CardChannel getChannel() { return channel; }

    public ResponseAPDU send(CommandAPDU cmd) throws CardException {
        if (channel == null) throw new CardException("Chưa kết nối thẻ!");
        return channel.transmit(cmd);
    }
}
