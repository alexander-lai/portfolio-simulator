package portfoliosim.ui;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;

/** Window wrapper around {@link AppPanel}. */
public class MainFrame extends JFrame {

    public MainFrame(String title, AppPanel panel) {
        super(title);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(panel);
        setMinimumSize(new Dimension(1000, 640));
        pack();
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }
}
