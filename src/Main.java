
import ui.SplashScreen;

public class Main {
    public static void main(String[] args) {
        // 设置系统外观
        try {
            javax.swing.UIManager.setLookAndFeel(
                javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("无法设置系统外观: " + e.getMessage());
        }

        // 在事件调度线程中启动程序
        javax.swing.SwingUtilities.invokeLater(() -> {
            SplashScreen splashScreen = new SplashScreen();
            splashScreen.showSplash();
        });
}
}