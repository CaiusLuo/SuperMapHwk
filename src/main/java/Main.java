import com.supermap.data.Workspace;
import ui.HwkMainFrame;

import javax.swing.*;

/**
 * @author Caius
 * @description
 * @since Created in 2026-06-26
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HwkMainFrame frame = new HwkMainFrame();
            frame.setVisible(true);
        });
    }
}
