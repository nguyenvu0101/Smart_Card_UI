package hospitalcardgui;

import javax.smartcardio.*;
import java.util.List;

public class CardManager {

    private CardTerminal terminal;
    private Card card;
    private CardChannel channel;
    private boolean connected = false;

    // AID trùng với JCIDE converter (00 11 22 33 44 55 67 11)
    private static final byte[] APPLET_AID = {
            (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33,
            (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11
    };

    public boolean connect() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> list = factory.terminals().list();
            if (list == null || list.isEmpty()) {
                System.out.println("Không tìm thấy đầu đọc.");
                return false;
            }
            terminal = list.get(0);
            if (!terminal.isCardPresent()) {
                System.out.println("Chưa có thẻ trong reader.");
                return false;
            }
            card = terminal.connect("T=0");
            channel = card.getBasicChannel();

            // SELECT applet
            CommandAPDU select = new CommandAPDU(
                    0x00, 0xA4, 0x04, 0x00, APPLET_AID);
            ResponseAPDU resp = channel.transmit(select);
            if (resp.getSW() == 0x9000) {
                connected = true;
                return true;
            } else {
                System.out.println("Select applet lỗi, SW=" +
                        Integer.toHexString(resp.getSW()));
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        try {
            if (card != null) card.disconnect(false);
        } catch (Exception ignored) {}
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public CardChannel getChannel() {
        return channel;
    }
}
