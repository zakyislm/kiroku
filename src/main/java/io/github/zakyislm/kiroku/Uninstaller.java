package io.github.zakyislm.kiroku;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Uninstaller extends JFrame {
    private static final Color BG_COLOR = new Color(0x12, 0x14, 0x14);
    private static final Color CARD_BG_COLOR = new Color(0x1b, 0x1c, 0x1c);
    private static final Color BORDER_COLOR = new Color(0x2a, 0x2b, 0x2b);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(0x8e, 0x91, 0x92);
    private static final Color ACCENT_RED = new Color(0xff, 0x55, 0x55);

    private final Font fontTitle = new Font("Segoe UI", Font.BOLD, 16);
    private final Font fontBody = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font fontMuted = new Font("Segoe UI", Font.PLAIN, 11);
    private final Font fontButton = new Font("Segoe UI", Font.BOLD, 12);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JCheckBox cleanConfigCheckbox;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private Point dragOffset;

    public Uninstaller() {
        setUndecorated(true);
        setSize(400, 260);
        getContentPane().setBackground(BG_COLOR);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main Layout: Title Bar + Content Card
        setLayout(new BorderLayout());

        // 1. Custom Title Bar
        JPanel titleBar = createCustomTitleBar();
        add(titleBar, BorderLayout.NORTH);

        // 2. Card Content Panel
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BG_COLOR);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        mainContentPanel.add(createInitialPanel(), "INITIAL");
        mainContentPanel.add(createUninstallingPanel(), "UNINSTALLING");
        mainContentPanel.add(createCompletedPanel(), "COMPLETED");

        add(mainContentPanel, BorderLayout.CENTER);

        // Apply dark border to the whole window
        getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
    }

    private JPanel createCustomTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(Color.BLACK);
        titleBar.setPreferredSize(new Dimension(getWidth(), 32));

        // Drag listeners
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                setLocation(curr.x - dragOffset.x, curr.y - dragOffset.y);
            }
        });

        // Left Section: Logo + Title
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(Color.BLACK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 8, 0, 0);

        java.net.URL logoUrl = Uninstaller.class.getResource("/desktop-app-logo-started.png");
        if (logoUrl != null) {
            ImageIcon icon = new ImageIcon(new ImageIcon(logoUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            JLabel logoLabel = new JLabel(icon);
            leftPanel.add(logoLabel, gbc);
            gbc.gridx++;
        }

        JLabel titleLabel = new JLabel("kiroku uninstaller");
        titleLabel.setFont(fontMuted);
        titleLabel.setForeground(TEXT_PRIMARY);
        gbc.insets = new Insets(0, 8, 0, 8);
        leftPanel.add(titleLabel, gbc);

        titleBar.add(leftPanel, BorderLayout.WEST);

        // Right Section: Close Button
        TitleBarButton closeBtn = new TitleBarButton(TitleBarButton.Type.CLOSE, this);
        closeBtn.addActionListener(e -> System.exit(0));

        titleBar.add(closeBtn, BorderLayout.EAST);
        return titleBar;
    }

    private JPanel createInitialPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        JLabel title = new JLabel("uninstall kiroku?");
        title.setFont(fontTitle);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        JLabel desc = new JLabel("<html><body style='width: 320px;'>This will remove the application, startup shortcuts, and associated configuration profiles.</body></html>");
        desc.setFont(fontBody);
        desc.setForeground(TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(16));

        // Clean configs checkbox
        cleanConfigCheckbox = new JCheckBox("remove all logs and saved data files", true);
        cleanConfigCheckbox.setFont(fontBody);
        cleanConfigCheckbox.setBackground(BG_COLOR);
        cleanConfigCheckbox.setForeground(TEXT_PRIMARY);
        cleanConfigCheckbox.setFocusPainted(false);
        cleanConfigCheckbox.setIcon(new CustomCheckboxIcon(false));
        cleanConfigCheckbox.setSelectedIcon(new CustomCheckboxIcon(true));
        cleanConfigCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cleanConfigCheckbox);
        panel.add(Box.createVerticalStrut(24));

        // Action Buttons Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnRow.setBackground(BG_COLOR);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton noBtn = new JButton("No");
        styleButton(noBtn, false);
        noBtn.addActionListener(e -> System.exit(0));
        btnRow.add(noBtn);

        JButton yesBtn = new JButton("Yes");
        styleButton(yesBtn, true);
        yesBtn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, "UNINSTALLING");
            startUninstallation();
        });
        btnRow.add(yesBtn);

        panel.add(btnRow);
        return panel;
    }

    private JPanel createUninstallingPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        JLabel title = new JLabel("uninstalling kiroku...");
        title.setFont(fontTitle);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        progressBar = new JProgressBar(0, 100);
        progressBar.setBackground(BORDER_COLOR);
        progressBar.setForeground(TEXT_PRIMARY);
        progressBar.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        progressBar.setPreferredSize(new Dimension(350, 8));
        progressBar.setMaximumSize(new Dimension(350, 8));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(12));

        statusLabel = new JLabel("stopping background services...");
        statusLabel.setFont(fontBody);
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);

        return panel;
    }

    private JPanel createCompletedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        JLabel title = new JLabel("kiroku uninstalled successfully");
        title.setFont(fontTitle);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        JLabel desc = new JLabel("<html><body style='width: 320px;'>The application has been successfully removed from your computer.</body></html>");
        desc.setFont(fontBody);
        desc.setForeground(TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(32));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setBackground(BG_COLOR);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton okBtn = new JButton("OK");
        styleButton(okBtn, true);
        okBtn.addActionListener(e -> {
            triggerDirectoryCleanup();
            System.exit(0);
        });
        btnRow.add(okBtn);

        panel.add(btnRow);
        return panel;
    }

    private void styleButton(JButton btn, boolean primary) {
        btn.setFont(fontButton);
        btn.setPreferredSize(new Dimension(100, 30));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (primary) {
            btn.setBackground(TEXT_PRIMARY);
            btn.setForeground(BG_COLOR);
            btn.setBorder(BorderFactory.createLineBorder(TEXT_PRIMARY, 1));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(TEXT_MUTED);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(TEXT_PRIMARY);
                }
            });
        } else {
            btn.setBackground(BG_COLOR);
            btn.setForeground(TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(TEXT_PRIMARY, 1));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(CARD_BG_COLOR);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(BG_COLOR);
                }
            });
        }
    }

    private void startUninstallation() {
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Task 1: Kill Kiroku process
                publish(10);
                setProgressText("stopping Kiroku background process...");
                killProcess("Kiroku.exe");
                Thread.sleep(800);

                // Task 2: Remove startup script
                publish(40);
                setProgressText("removing Windows startup shortcuts...");
                removeStartupShortcut();
                Thread.sleep(600);

                // Task 3: Remove custom log files if chosen
                publish(70);
                if (cleanConfigCheckbox.isSelected()) {
                    setProgressText("removing log files and configurations...");
                    removeAppData();
                }
                Thread.sleep(600);

                // Task 4: Done
                publish(100);
                setProgressText("finalizing uninstallation...");
                Thread.sleep(400);

                return null;
            }

            private void setProgressText(String text) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(text));
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                progressBar.setValue(progress);
            }

            @Override
            protected void done() {
                cardLayout.show(mainContentPanel, "COMPLETED");
            }
        };
        worker.execute();
    }

    private void killProcess(String imageName) {
        try {
            Runtime.getRuntime().exec("taskkill /f /im " + imageName);
        } catch (IOException e) {
            // Ignore
        }
    }

    private void removeStartupShortcut() {
        try {
            File startupDir = new File(System.getenv("APPDATA"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup");
            File batFile = new File(startupDir, "Kiroku.bat");
            if (batFile.exists()) {
                batFile.delete();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void removeAppData() {
        try {
            File logDir = new File(System.getProperty("user.home"), ".kiroku");
            deleteDirectoryRecursive(logDir);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void deleteDirectoryRecursive(File path) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectoryRecursive(f);
                }
            }
        }
        path.delete();
    }

    private void triggerDirectoryCleanup() {
        // Since we are running from the installation folder, we cannot delete our own EXE directly.
        // We write a batch file to %TEMP% that sleeps for 1 second, deletes the installation directory, and deletes itself.
        try {
            File installDir = new File(System.getProperty("user.dir"));
            File tempBat = new File(System.getenv("TEMP"), "kiroku-cleanup.bat");

            String batContent = "@echo off\r\n" +
                                "timeout /t 1 /nobreak > nul\r\n" +
                                "rmdir /s /q \"" + installDir.getAbsolutePath() + "\"\r\n" +
                                "del \"%~f0\"\r\n";

            try (FileWriter fw = new FileWriter(tempBat)) {
                fw.write(batContent);
            }

            // Launch the batch script in the background
            Runtime.getRuntime().exec("cmd /c start /b \"\" \"" + tempBat.getAbsolutePath() + "\"");

        } catch (Exception e) {
            // Ignore
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Uninstaller ui = new Uninstaller();
            ui.setVisible(true);
        });
    }

    private static class TitleBarButton extends JButton {
        public enum Type { MINIMIZE, MAXIMIZE, CLOSE }
        private final Type type;
        private final Window window;

        public TitleBarButton(Type type, Window window) {
            this.type = type;
            this.window = window;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.BLACK);
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(46, 32));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (type == Type.CLOSE) {
                        setBackground(new Color(0xe8, 0x11, 0x23)); // Windows Red
                        setForeground(Color.WHITE);
                    } else {
                        setBackground(new Color(0x22, 0x22, 0x22)); // Dark Grey
                        setForeground(Color.WHITE);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(Color.BLACK);
                    setForeground(Color.WHITE);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getForeground());

            int w = getWidth();
            int h = getHeight();

            if (type == Type.MINIMIZE) {
                int lineW = 10;
                int x = (w - lineW) / 2;
                int y = h / 2;
                g2.drawLine(x, y, x + lineW, y);
            } else if (type == Type.MAXIMIZE) {
                boolean isMaximized = false;
                if (window instanceof Frame) {
                    isMaximized = (((Frame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
                }
                if (isMaximized) {
                    int size = 8;
                    int x1 = (w - size) / 2 + 2;
                    int y1 = (h - size) / 2 - 2;
                    g2.drawRect(x1, y1, size - 1, size - 1);

                    int x2 = (w - size) / 2 - 2;
                    int y2 = (h - size) / 2 + 2;
                    g2.setColor(getBackground());
                    g2.fillRect(x2, y2, size, size);
                    g2.setColor(getForeground());
                    g2.drawRect(x2, y2, size - 1, size - 1);
                } else {
                    int size = 10;
                    int x = (w - size) / 2;
                    int y = (h - size) / 2;
                    g2.drawRect(x, y, size - 1, size - 1);
                }
            } else if (type == Type.CLOSE) {
                int size = 10;
                int x = (w - size) / 2;
                int y = (h - size) / 2;
                g2.drawLine(x, y, x + size - 1, y + size - 1);
                g2.drawLine(x + size - 1, y, x, y + size - 1);
            }

            g2.dispose();
        }
    }

    // Custom check box icon implementation matching Kiroku UI styles
    private static class CustomCheckboxIcon implements Icon {
        private final boolean checked;
        public CustomCheckboxIcon(boolean checked) {
            this.checked = checked;
        }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(checked ? Color.WHITE : BORDER_COLOR);
            g2.drawRect(x, y, 16, 16);
            if (checked) {
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 3, y + 3, 11, 11);
            }
            g2.dispose();
        }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
    }
}
