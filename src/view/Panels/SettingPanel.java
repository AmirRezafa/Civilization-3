package view.Panels;

import view.MainFrame;

import javax.swing.*;
import java.awt.*;

public class SettingPanel extends JPanel {
    private static final Color BackgroundColor = new Color(25, 25, 25);
    private static final Dimension FieldSize = new Dimension(250, 40);

    private final MainFrame MF;
    private final JSlider slider = new JSlider(0, 100, 50);

    private enum Labels {
        Setting("SETTINGS"),
        SoundRange("Sound Volume: ");

        private final String text;
        Labels(String text){
            this.text = text;
        }

        JLabel getLabel(int size){
            JLabel label = new JLabel(text);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("SansSerif", Font.BOLD, size));
            return label;
        }
    }

    public SettingPanel(MainFrame MF){
        this.MF = MF;
        setBackground(BackgroundColor);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(Labels.Setting.getLabel(30), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;

        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(Labels.SoundRange.getLabel(14), gbc);

        gbc.gridx = 1;
        slider.setBackground(BackgroundColor);
        slider.setForeground(Color.WHITE);
        slider.setPreferredSize(FieldSize);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);


        slider.addChangeListener(e -> {
            controller.AudioManager.getInstance().setVolume(slider.getValue());
        });
        add(slider, gbc);

        JButton backButton = new JButton("Back to Menu");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        backButton.setBackground(new Color(52, 73, 94));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusable(false);
        backButton.setPreferredSize(new Dimension(160, 45));

        backButton.addActionListener(e -> {
            MF.showMenu();
        });

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(backButton, gbc);
    }

    public JSlider getSlider() {
        return slider;
    }
}