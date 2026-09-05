package App;

import view.MainFrame;

import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        UIManager.put("AuditoryCues.playList", null);

        new MainFrame().show();
    }
}
