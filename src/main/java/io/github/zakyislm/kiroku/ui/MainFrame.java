package io.github.zakyislm.kiroku.ui;

import io.github.zakyislm.kiroku.config.AppConfig;
import io.github.zakyislm.kiroku.config.ConfigManager;
import io.github.zakyislm.kiroku.engine.ScreenshotEngine;
import io.github.zakyislm.kiroku.monitor.WindowMonitor;
import io.github.zakyislm.kiroku.server.LocalServer;
import io.github.zakyislm.kiroku.utils.BrowserDetector;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class MainFrame extends JFrame implements LocalServer.ServerListener, WindowMonitor.MonitorListener {
    // Style Colors
    private static final Color BG_COLOR = new Color(0x12, 0x14, 0x14);
    private static final Color CARD_BG_COLOR = new Color(0x1b, 0x1c, 0x1c);
    private static final Color BORDER_COLOR = new Color(0x2a, 0x2b, 0x2b);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(0x8e, 0x91, 0x92);
    private static final Color ACCENT_GREEN = new Color(0x22, 0xc5, 0x5e);
    private static final Color ERROR_COLOR = new Color(0xff, 0xb4, 0xab);

    // Style Fonts
    private final Font fontHeadline = new Font("Segoe UI", Font.BOLD, 16);
    private final Font fontBody = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font fontMuted = new Font("Segoe UI", Font.PLAIN, 11);
    private final Font fontLabel = new Font("Consolas", Font.PLAIN, 12);

    // Core Services
    private LocalServer server;
    private WindowMonitor monitor;
    private TrayIcon trayIcon;

    // UI Controls
    private CustomToggleSwitch headerToggle;
    private JLabel statusLabel;
    private JPanel statusDot;
    private JTextField delayField;
    private JTextField countField;
    private JComboBox<String> modeCombo;
    private JPanel whitelistRowsContainer;
    private JLabel folderPathLabel;

    private JCheckBox autoStartCheckBox;
    private JCheckBox bgCheckBox;
    private JCheckBox continuousCheckBox;
    private JTextField intervalField;
    private JLabel intervalLabel;

    public MainFrame() {
        setUndecorated(true);
        applyDarkTheme();

        setTitle("kiroku");
        setSize(400, 680);
        setMinimumSize(new Dimension(380, 500));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set Window Icon to desktop-app-logo-started.png
        java.net.URL windowIconUrl = MainFrame.class.getResource("/desktop-app-logo-started.png");
        if (windowIconUrl != null) {
            setIconImage(new ImageIcon(windowIconUrl).getImage());
        }

        // Setup custom resize and drag listeners
        FrameResizeListener resizeListener = new FrameResizeListener(this);
        addMouseListener(resizeListener);
        addMouseMotionListener(resizeListener);
        
        // Window Listener for Minimize to Tray or Exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (ConfigManager.getConfig().runInBackground) {
                    minimizeToTray();
                } else {
                    shutdownAndExit();
                }
            }
        });

        // Initialize Core Components
        ConfigManager.loadConfig();
        server = new LocalServer(this);
        monitor = new WindowMonitor(this);

        // Build UI
        initUI();

        // Start Services if enabled
        server.start();
        if (ConfigManager.getConfig().isEnabled) {
            monitor.start();
        }

        // Setup System Tray
        setupSystemTray();
        
        addNotify();
        enableDarkTitleBar();

        log("kiroku initialized successfully");
    }

    private void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback
        }

        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("ScrollPane.background", BG_COLOR);
        UIManager.put("Viewport.background", BG_COLOR);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.background", CARD_BG_COLOR);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretColor", TEXT_PRIMARY);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(BORDER_COLOR, 1));
        UIManager.put("ComboBox.background", CARD_BG_COLOR);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.border", BorderFactory.createLineBorder(BORDER_COLOR, 1));
        UIManager.put("CheckBox.background", CARD_BG_COLOR);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("OptionPane.background", BG_COLOR);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background", CARD_BG_COLOR);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        // Add 1px border and margins for dragging/resizing
        getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        ((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        AppConfig config = ConfigManager.getConfig();

        // --- TOP HEADER PANEL ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        // Logo + App Name (Replaced with simple text label)
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoPanel.setBackground(BG_COLOR);

        JLabel textLabel = new JLabel("monitoring status");
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        textLabel.setForeground(TEXT_MUTED);
        logoPanel.add(textLabel);
        
        headerPanel.add(logoPanel, BorderLayout.WEST);

        // Header Toggle Switch
        headerToggle = new CustomToggleSwitch(config.isEnabled);
        headerToggle.addActionListener(e -> {
            boolean enabled = headerToggle.isSelected();
            config.isEnabled = enabled;
            ConfigManager.saveConfig();
            
            statusLabel.setText(enabled ? "active" : "inactive");
            statusDot.repaint();
            
            if (enabled) {
                monitor.start();
                log("monitoring: active");
            } else {
                monitor.stop();
                log("monitoring: inactive");
            }
        });
        headerPanel.add(headerToggle, BorderLayout.EAST);

        // --- CENTER CONTAINER (SCROLLABLE PANEL) ---
        JPanel wrapper = new JPanel(new MaxWidthCenterLayout(352));
        wrapper.setBackground(BG_COLOR);
        wrapper.add(createStatusView(headerPanel));

        JScrollPane mainScroll = createScrollPane(wrapper);
        
        add(createCustomTitleBar(this, "kiroku", true), BorderLayout.NORTH);
        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createStatusView(JPanel headerPanel) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(BG_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        AppConfig config = ConfigManager.getConfig();

        // Add Header Panel inside the scroll view first
        addToContainer(container, headerPanel);
        addToContainer(container, Box.createVerticalStrut(12));

        // 1. System Status Card
        JPanel statusCard = createCardPanel();
        
        JLabel title = new JLabel("system status");
        title.setFont(fontHeadline);
        title.setForeground(TEXT_PRIMARY);
        title.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(statusCard, title);
        addToCard(statusCard, Box.createVerticalStrut(4));
        
        JLabel desc = new JLabel("<html><body style='width: 280px;'>monitor and manage your automatic logging configuration in real-time.</body></html>");
        desc.setFont(fontBody);
        desc.setForeground(TEXT_MUTED);
        desc.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(statusCard, desc);
        addToCard(statusCard, Box.createVerticalStrut(16));
        
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setBackground(CARD_BG_COLOR);
        
        JPanel statusInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusInfo.setBackground(CARD_BG_COLOR);
        
        statusLabel = new JLabel(config.isEnabled ? "active" : "inactive");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(TEXT_PRIMARY);
        statusInfo.add(statusLabel);
        
        statusDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(headerToggle.isSelected() ? ACCENT_GREEN : TEXT_MUTED);
                g2.fillOval(0, 2, 10, 10);
                g2.dispose();
            }
        };
        statusDot.setPreferredSize(new Dimension(12, 14));
        statusDot.setBackground(CARD_BG_COLOR);
        statusInfo.add(statusDot);
        statusRow.add(statusInfo, BorderLayout.WEST);
        
        JButton takeNowBtn = new JButton("take now");
        styleButton(takeNowBtn);
        takeNowBtn.setPreferredSize(new Dimension(80, 24));
        takeNowBtn.addActionListener(e -> {
            log("take now requested...");
            ScreenshotEngine.CaptureResult res = ScreenshotEngine.captureScreen(config.rootDir);
            if (res.success) {
                log("saved: " + res.file.getName() + " (#" + res.id + ")");
                showCaptureNotificationDialog(res.file.getName());
            } else {
                log("error: " + res.errorMessage);
                showTextDialog("capture failed: " + res.errorMessage, "error", true);
            }
        });
        statusRow.add(takeNowBtn, BorderLayout.EAST);
        
        addToCard(statusCard, statusRow);
        addToContainer(container, statusCard);
        addToContainer(container, Box.createVerticalStrut(16));

        // 2. Timer Settings Card
        JPanel timerCard = createCardPanel();
        
        JPanel timerHeader = new JPanel(new BorderLayout());
        timerHeader.setBackground(CARD_BG_COLOR);
        JLabel timerTitle = new JLabel("timer settings");
        timerTitle.setFont(fontHeadline);
        timerTitle.setForeground(TEXT_PRIMARY);
        timerTitle.setHorizontalAlignment(SwingConstants.LEFT);
        timerHeader.add(timerTitle, BorderLayout.WEST);
        
        JButton applyBtn = new JButton("apply");
        styleButton(applyBtn);
        applyBtn.setPreferredSize(new Dimension(60, 24));
        applyBtn.addActionListener(e -> {
            try {
                int delay = Integer.parseInt(delayField.getText());
                int count = Integer.parseInt(countField.getText());
                if (delay < 0 || count < 1) {
                    showTextDialog("delay must be >= 0 and count must be >= 1.", "validation error", true);
                    return;
                }
                config.timerMinutes = delay;
                config.screenshotCount = count;
                config.screenshotMode = modeCombo.getSelectedItem().equals("simultaneous") ? "serentak" : "bertahap";
                ConfigManager.saveConfig();
                log("timer settings applied: delay=" + delay + ", count=" + count + ", mode=" + config.screenshotMode);
                showTextDialog("timer settings applied successfully.", "success", false);
            } catch (NumberFormatException ex) {
                showTextDialog("please enter valid numbers for delay and count.", "validation error", true);
            }
        });
        timerHeader.add(applyBtn, BorderLayout.EAST);
        addToCard(timerCard, timerHeader);
        addToCard(timerCard, Box.createVerticalStrut(12));
        
        JLabel delayLabel = new JLabel("delay (minutes)");
        delayLabel.setFont(fontMuted);
        delayLabel.setForeground(TEXT_MUTED);
        delayLabel.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(timerCard, delayLabel);
        addToCard(timerCard, Box.createVerticalStrut(4));
        
        JPanel delayInputPanel = new JPanel(new BorderLayout());
        delayInputPanel.setBackground(CARD_BG_COLOR);
        
        delayField = new JTextField(String.format("%02d", config.timerMinutes));
        delayField.setBackground(CARD_BG_COLOR);
        delayField.setForeground(TEXT_PRIMARY);
        delayField.setCaretColor(Color.WHITE);
        delayField.setFont(fontLabel);
        delayField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        delayInputPanel.add(delayField, BorderLayout.CENTER);
        
        JPanel delayBtnGroup = new JPanel(new java.awt.GridLayout(2, 1));
        delayBtnGroup.setBackground(CARD_BG_COLOR);
        
        JButton delayPlus = new JButton("+");
        styleSpinnerButton(delayPlus);
        delayPlus.addActionListener(e -> {
            try {
                int val = Integer.parseInt(delayField.getText());
                delayField.setText(String.format("%02d", Math.min(120, val + 1)));
            } catch (NumberFormatException ex) {
                delayField.setText("05");
            }
        });
        
        JButton delayMinus = new JButton("-");
        styleSpinnerButton(delayMinus);
        delayMinus.addActionListener(e -> {
            try {
                int val = Integer.parseInt(delayField.getText());
                delayField.setText(String.format("%02d", Math.max(0, val - 1)));
            } catch (NumberFormatException ex) {
                delayField.setText("05");
            }
        });
        
        delayBtnGroup.add(delayPlus);
        delayBtnGroup.add(delayMinus);
        delayInputPanel.add(delayBtnGroup, BorderLayout.EAST);
        addToCard(timerCard, delayInputPanel);
        addToCard(timerCard, Box.createVerticalStrut(12));
        
        JLabel countLabel = new JLabel("count");
        countLabel.setFont(fontMuted);
        countLabel.setForeground(TEXT_MUTED);
        countLabel.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(timerCard, countLabel);
        addToCard(timerCard, Box.createVerticalStrut(4));
        
        countField = new JTextField(String.valueOf(config.screenshotCount));
        countField.setBackground(CARD_BG_COLOR);
        countField.setForeground(TEXT_PRIMARY);
        countField.setCaretColor(Color.WHITE);
        countField.setFont(fontLabel);
        countField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        addToCard(timerCard, countField);
        addToCard(timerCard, Box.createVerticalStrut(12));
        
        JLabel modeLabel = new JLabel("mode");
        modeLabel.setFont(fontMuted);
        modeLabel.setForeground(TEXT_MUTED);
        modeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(timerCard, modeLabel);
        addToCard(timerCard, Box.createVerticalStrut(4));
        
        modeCombo = new JComboBox<>(new String[]{"simultaneous", "gradual"});
        modeCombo.setSelectedItem("bertahap".equalsIgnoreCase(config.screenshotMode) ? "gradual" : "simultaneous");
        styleComboBox(modeCombo);
        addToCard(timerCard, modeCombo);
        
        addToContainer(container, timerCard);
        addToContainer(container, Box.createVerticalStrut(16));

        // 3. Monitored Browsers Card
        JPanel browsersCard = createCardPanel();
        JLabel browsersTitle = new JLabel("monitored browsers");
        browsersTitle.setFont(fontHeadline);
        browsersTitle.setForeground(TEXT_PRIMARY);
        browsersTitle.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(browsersCard, browsersTitle);
        addToCard(browsersCard, Box.createVerticalStrut(12));
        
        java.util.List<BrowserDetector.DetectedBrowser> detectedBrowsers = BrowserDetector.detectBrowsers();
        for (BrowserDetector.DetectedBrowser browser : detectedBrowsers) {
            boolean isChecked = config.monitoredBrowsers.contains(browser.name);
            JCheckBox cb = new JCheckBox(browser.name.toLowerCase(), isChecked);
            cb.setFont(fontBody);
            cb.setIcon(new CustomCheckboxIcon(false));
            cb.setSelectedIcon(new CustomCheckboxIcon(true));
            cb.setHorizontalAlignment(SwingConstants.LEFT);
            cb.setMargin(new Insets(2, 0, 2, 0));
            cb.addActionListener(e -> {
                if (cb.isSelected()) {
                    if (!config.monitoredBrowsers.contains(browser.name)) {
                        config.monitoredBrowsers.add(browser.name);
                    }
                } else {
                    config.monitoredBrowsers.remove(browser.name);
                }
                ConfigManager.saveConfig();
            });
            addToCard(browsersCard, cb);
            addToCard(browsersCard, Box.createVerticalStrut(8));
        }
        addToContainer(container, browsersCard);
        addToContainer(container, Box.createVerticalStrut(16));

        // 4. URL Whitelist Card
        JPanel whitelistCard = createCardPanel();
        
        JPanel whitelistHeader = new JPanel(new BorderLayout());
        whitelistHeader.setBackground(CARD_BG_COLOR);
        JLabel whitelistTitle = new JLabel("url whitelist");
        whitelistTitle.setFont(fontHeadline);
        whitelistTitle.setForeground(TEXT_PRIMARY);
        whitelistTitle.setHorizontalAlignment(SwingConstants.LEFT);
        whitelistHeader.add(whitelistTitle, BorderLayout.WEST);
        
        JButton addBtn = new JButton("add");
        styleButton(addBtn);
        addBtn.setPreferredSize(new Dimension(50, 24));
        addBtn.addActionListener(e -> addWhitelistDialog());
        whitelistHeader.add(addBtn, BorderLayout.EAST);
        addToCard(whitelistCard, whitelistHeader);
        addToCard(whitelistCard, Box.createVerticalStrut(12));
        
        whitelistRowsContainer = new JPanel();
        whitelistRowsContainer.setLayout(new BoxLayout(whitelistRowsContainer, BoxLayout.Y_AXIS));
        whitelistRowsContainer.setBackground(CARD_BG_COLOR);
        addToCard(whitelistCard, whitelistRowsContainer);
        
        addToContainer(container, whitelistCard);
        addToContainer(container, Box.createVerticalStrut(16));

        // 5. Destination Folder Card
        JPanel folderCard = createCardPanel();
        JLabel folderTitle = new JLabel("destination folder");
        folderTitle.setFont(fontHeadline);
        folderTitle.setForeground(TEXT_PRIMARY);
        folderTitle.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(folderCard, folderTitle);
        addToCard(folderCard, Box.createVerticalStrut(12));
        
        JPanel folderRow = new JPanel(new BorderLayout());
        folderRow.setBackground(CARD_BG_COLOR);
        folderRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        
        folderPathLabel = new JLabel(config.rootDir);
        folderPathLabel.setFont(fontLabel);
        folderPathLabel.setForeground(TEXT_PRIMARY);
        folderPathLabel.setHorizontalAlignment(SwingConstants.LEFT);
        folderPathLabel.setPreferredSize(new Dimension(170, 20));
        folderPathLabel.setMinimumSize(new Dimension(170, 20));
        folderPathLabel.setMaximumSize(new Dimension(170, 20));
        folderRow.add(folderPathLabel, BorderLayout.CENTER);
        
        JButton browseButton = new JButton("browse");
        styleButton(browseButton);
        browseButton.setPreferredSize(new Dimension(80, 24));
        browseButton.addActionListener(e -> showOpenChooser());
        folderRow.add(browseButton, BorderLayout.EAST);
        addToCard(folderCard, folderRow);
        addToCard(folderCard, Box.createVerticalStrut(8));
        
        JLabel folderDesc = new JLabel("<html><body style='width: 280px;'>storage location for log files and automatic recordings.</body></html>");
        folderDesc.setFont(fontMuted);
        folderDesc.setForeground(TEXT_MUTED);
        folderDesc.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(folderCard, folderDesc);
        
        addToContainer(container, folderCard);
        addToContainer(container, Box.createVerticalStrut(16));

        // 6. Startup & Background Card (Settings)
        JPanel startupCard = createCardPanel();
        JLabel startupTitle = new JLabel("startup & background");
        startupTitle.setFont(fontHeadline);
        startupTitle.setForeground(TEXT_PRIMARY);
        startupTitle.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(startupCard, startupTitle);
        addToCard(startupCard, Box.createVerticalStrut(12));
        
        autoStartCheckBox = new JCheckBox("auto-start with windows", config.autoStart);
        autoStartCheckBox.setFont(fontBody);
        autoStartCheckBox.setIcon(new CustomCheckboxIcon(false));
        autoStartCheckBox.setSelectedIcon(new CustomCheckboxIcon(true));
        autoStartCheckBox.setHorizontalAlignment(SwingConstants.LEFT);
        autoStartCheckBox.setMargin(new Insets(2, 0, 2, 0));
        autoStartCheckBox.addActionListener(e -> {
            boolean sel = autoStartCheckBox.isSelected();
            config.autoStart = sel;
            ConfigManager.saveConfig();
            updateStartupShortcut(sel);
        });
        addToCard(startupCard, autoStartCheckBox);
        addToCard(startupCard, Box.createVerticalStrut(8));
        
        bgCheckBox = new JCheckBox("enable background run (tray)", config.runInBackground);
        bgCheckBox.setFont(fontBody);
        bgCheckBox.setIcon(new CustomCheckboxIcon(false));
        bgCheckBox.setSelectedIcon(new CustomCheckboxIcon(true));
        bgCheckBox.setHorizontalAlignment(SwingConstants.LEFT);
        bgCheckBox.setMargin(new Insets(2, 0, 2, 0));
        bgCheckBox.addActionListener(e -> {
            config.runInBackground = bgCheckBox.isSelected();
            ConfigManager.saveConfig();
        });
        addToCard(startupCard, bgCheckBox);
        addToContainer(container, startupCard);
        addToContainer(container, Box.createVerticalStrut(16));

        // 7. Continuous Capture Card (Continuous Mode)
        JPanel contCard = createCardPanel();
        JLabel contTitle = new JLabel("continuous mode");
        contTitle.setFont(fontHeadline);
        contTitle.setForeground(TEXT_PRIMARY);
        contTitle.setHorizontalAlignment(SwingConstants.LEFT);
        addToCard(contCard, contTitle);
        addToCard(contCard, Box.createVerticalStrut(12));
        
        continuousCheckBox = new JCheckBox("ambil beruntun (continuous mode)", config.continuousCapture);
        continuousCheckBox.setFont(fontBody);
        continuousCheckBox.setIcon(new CustomCheckboxIcon(false));
        continuousCheckBox.setSelectedIcon(new CustomCheckboxIcon(true));
        continuousCheckBox.setHorizontalAlignment(SwingConstants.LEFT);
        continuousCheckBox.setMargin(new Insets(2, 0, 2, 0));
        
        intervalLabel = new JLabel("interval (minutes)");
        intervalLabel.setFont(fontMuted);
        intervalLabel.setForeground(TEXT_MUTED);
        intervalLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        intervalField = new JTextField(String.valueOf(config.intervalMinutes));
        intervalField.setFont(fontLabel);
        intervalField.setBackground(CARD_BG_COLOR);
        intervalField.setForeground(TEXT_PRIMARY);
        intervalField.setCaretColor(Color.WHITE);
        intervalField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        
        continuousCheckBox.addActionListener(e -> {
            boolean selected = continuousCheckBox.isSelected();
            config.continuousCapture = selected;
            ConfigManager.saveConfig();
            
            intervalLabel.setEnabled(selected);
            intervalField.setEnabled(selected);
        });
        addToCard(contCard, continuousCheckBox);
        addToCard(contCard, Box.createVerticalStrut(12));
        
        addToCard(contCard, intervalLabel);
        addToCard(contCard, Box.createVerticalStrut(4));
        addToCard(contCard, intervalField);
        
        intervalField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void save() {
                try {
                    int val = Integer.parseInt(intervalField.getText());
                    if (val >= 1) {
                        config.intervalMinutes = val;
                        ConfigManager.saveConfig();
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { save(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { save(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { save(); }
        });
        
        boolean isCont = config.continuousCapture;
        intervalLabel.setEnabled(isCont);
        intervalField.setEnabled(isCont);
        
        addToContainer(container, contCard);
        
        rebuildWhitelistRows();

        return container;
    }

    private void rebuildWhitelistRows() {
        whitelistRowsContainer.removeAll();

        AppConfig config = ConfigManager.getConfig();
        if (config.whitelist.isEmpty()) {
            JLabel emptyLabel = new JLabel("no entries in whitelist.");
            emptyLabel.setFont(fontBody);
            emptyLabel.setForeground(TEXT_MUTED);
            addToCard(whitelistRowsContainer, emptyLabel);
        } else {
            // Table Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(CARD_BG_COLOR);
            header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
            
            JLabel aliasCol = new JLabel("alias");
            aliasCol.setFont(fontLabel);
            aliasCol.setForeground(TEXT_MUTED);
            header.add(aliasCol, BorderLayout.WEST);
            
            JLabel actionCol = new JLabel("action");
            actionCol.setFont(fontLabel);
            actionCol.setForeground(TEXT_MUTED);
            header.add(actionCol, BorderLayout.EAST);
            
            addToCard(whitelistRowsContainer, header);
            addToCard(whitelistRowsContainer, Box.createVerticalStrut(4));

            for (int i = 0; i < config.whitelist.size(); i++) {
                final int index = i;
                AppConfig.WhitelistEntry entry = config.whitelist.get(i);
                
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(CARD_BG_COLOR);
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
                
                JLabel nameLbl = new JLabel(entry.name.toLowerCase());
                nameLbl.setFont(fontLabel);
                nameLbl.setForeground(TEXT_PRIMARY);
                row.add(nameLbl, BorderLayout.WEST);
                
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
                actions.setBackground(CARD_BG_COLOR);
                
                JButton editBtn = createTextButton("edit", TEXT_PRIMARY);
                editBtn.addActionListener(e -> editWhitelistDialog(index));
                actions.add(editBtn);
                
                JButton deleteBtn = createTextButton("delete", ERROR_COLOR);
                deleteBtn.addActionListener(e -> {
                    showDeleteConfirmDialog(entry.name, entry.id, "URL_RECORD", () -> {
                        config.whitelist.remove(index);
                        ConfigManager.saveConfig();
                        log("deleted whitelist: " + entry.name.toLowerCase());
                        rebuildWhitelistRows();
                    });
                });
                actions.add(deleteBtn);
                
                row.add(actions, BorderLayout.EAST);
                addToCard(whitelistRowsContainer, row);
            }
        }
        whitelistRowsContainer.revalidate();
        whitelistRowsContainer.repaint();
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD_BG_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void addToCard(JPanel card, Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        card.add(comp);
    }

    private void addToContainer(JPanel container, Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        container.add(comp);
    }

    private void enableDarkTitleBar() {
        try {
            com.sun.jna.platform.win32.WinDef.HWND hwnd = new com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Native.getWindowPointer(this));
            int[] dark = {1};
            Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, 20, dark, 4);
            Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, 19, dark, 4);
            int[] black = {0};
            Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, 35, black, 4);
        } catch (Throwable e) {
            // Ignore on non-Windows or if JNA is not found
        }
    }

    private interface Dwm extends com.sun.jna.Library {
        Dwm INSTANCE = com.sun.jna.Native.load("dwmapi", Dwm.class, com.sun.jna.win32.W32APIOptions.DEFAULT_OPTIONS);
        int DwmSetWindowAttribute(com.sun.jna.platform.win32.WinDef.HWND hwnd, int dwAttribute, int[] pvAttribute, int cbAttribute);
    }

    private JScrollPane createScrollPane(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(BG_COLOR);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, Integer.MAX_VALUE));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(BG_COLOR);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                g.setColor(BORDER_COLOR);
                g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }
        });
        return scroll;
    }

    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(CARD_BG_COLOR);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createLineBorder(TEXT_PRIMARY, 1));
        btn.setFont(fontBody);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(TEXT_PRIMARY);
                btn.setForeground(BG_COLOR);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(CARD_BG_COLOR);
                btn.setForeground(TEXT_PRIMARY);
            }
        });
    }

    private void styleSpinnerButton(JButton btn) {
        btn.setPreferredSize(new Dimension(24, 16));
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(CARD_BG_COLOR);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0, BORDER_COLOR));
        btn.setFont(fontLabel);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(TEXT_PRIMARY);
                btn.setForeground(BG_COLOR);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(CARD_BG_COLOR);
                btn.setForeground(TEXT_PRIMARY);
            }
        });
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFocusable(false);
        combo.setBackground(CARD_BG_COLOR);
        combo.setForeground(TEXT_PRIMARY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        combo.setFont(fontBody);
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBackground(isSelected ? BG_COLOR : CARD_BG_COLOR);
                label.setForeground(TEXT_PRIMARY);
                label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return label;
            }
        });

        Object child = combo.getAccessibleContext().getAccessibleChild(0);
        if (child instanceof javax.swing.plaf.basic.BasicComboPopup) {
            javax.swing.plaf.basic.BasicComboPopup popup = (javax.swing.plaf.basic.BasicComboPopup) child;
            popup.getList().setBackground(CARD_BG_COLOR);
            popup.getList().setForeground(TEXT_PRIMARY);
        }
    }

    private JButton createTextButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setForeground(color);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.setFont(fontLabel);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setText("<html><u>" + text + "</u></html>");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setText(text);
            }
        });
        return btn;
    }

    private String getAutomaticPattern(String host) {
        String lower = host.toLowerCase().trim();
        if (lower.contains("meet.google.com")) {
            return "^\\/[a-z]{3}-[a-z]{4}-[a-z]{3}";
        } else if (lower.contains("zoom.us")) {
            return "^\\/j\\/\\d{9,11}";
        } else if (lower.contains("quizizz.com")) {
            return "^\\/join";
        } else if (lower.contains("teams.microsoft.com")) {
            return "^\\/meet\\/";
        } else {
            return "^\\/.+";
        }
    }

    private void addWhitelistDialog() {
        showWhitelistDialog(null, false, -1);
    }

    private void editWhitelistDialog(int selectedIdx) {
        if (selectedIdx < 0) return;
        AppConfig config = ConfigManager.getConfig();
        AppConfig.WhitelistEntry entry = config.whitelist.get(selectedIdx);
        showWhitelistDialog(entry, true, selectedIdx);
    }

    private void updateStartupShortcut(boolean enable) {
        try {
            File startupDir = new File(System.getenv("APPDATA"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup");
            File batFile = new File(startupDir, "Kiroku.bat");

            if (enable) {
                File buildBat = new File(System.getProperty("user.dir"), "build.bat");
                if (!buildBat.exists()) {
                    buildBat = new File("build.bat");
                }
                
                String content = "@echo off\n" +
                                 "cd /d \"" + buildBat.getParentFile().getAbsolutePath() + "\"\n" +
                                 "start \"\" build.bat\n";
                
                try (FileWriter writer = new FileWriter(batFile)) {
                    writer.write(content);
                }
                log("auto-start enabled (created startup shortcut)");
            } else {
                if (batFile.exists()) {
                    batFile.delete();
                }
                log("auto-start disabled (removed startup shortcut)");
            }
        } catch (Exception e) {
            log("failed to update auto-start: " + e.getMessage());
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            log("system tray not supported on this platform");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            java.net.URL trayIconUrl = MainFrame.class.getResource("/icon-app-desktop.png");
            Image trayImage;
            if (trayIconUrl != null) {
                trayImage = new ImageIcon(trayIconUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            } else {
                BufferedImage fallbackImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = fallbackImg.createGraphics();
                g.setColor(new Color(108, 92, 231));
                g.fillRoundRect(1, 1, 14, 14, 4, 4);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString("Ki", 2, 12);
                g.dispose();
                trayImage = fallbackImg;
            }

            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("open kiroku");
            openItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });
            popup.add(openItem);

            MenuItem exitItem = new MenuItem("exit");
            exitItem.addActionListener(e -> shutdownAndExit());
            popup.add(exitItem);

            trayIcon = new TrayIcon(trayImage, "kiroku", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });

            tray.add(trayIcon);
        } catch (Exception e) {
            log("error setting up system tray: " + e.getMessage());
        }
    }

    private void shutdownAndExit() {
        if (server != null) {
            server.stop();
        }
        if (monitor != null) {
            monitor.stop();
        }
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ex) {
                // Ignore
            }
        }
        System.exit(0);
    }

    private void minimizeToTray() {
        setVisible(false);
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logLine = "[" + timestamp + "] " + message;

        try {
            File logDir = new File(System.getProperty("user.home"), ".kiroku");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            File logFile = new File(logDir, "kiroku.log");
            try (FileWriter fw = new FileWriter(logFile, true)) {
                fw.write(logLine + "\r\n");
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void onCaptureRequested(String source, String matchName, String url) {
        log(String.format("capture requested from '%s' for '%s' (%s)", source, matchName.toLowerCase(), url));
    }

    @Override
    public void onLogMessage(String message) {
        log(message.toLowerCase());
    }

    @Override
    public void onWindowMatchFound(String matchName, String windowTitle) {
        log(String.format("match: window '%s' detected (matches whitelist: '%s')", windowTitle.toLowerCase(), matchName.toLowerCase()));
    }

    @Override
    public void onCaptureCompleted(int id, String timestamp, String filePath) {
        if (trayIcon != null) {
            trayIcon.displayMessage("kiroku — captured", "screenshot saved: " + new File(filePath).getName(), TrayIcon.MessageType.INFO);
        }
        SwingUtilities.invokeLater(() -> showCaptureNotificationDialog(new File(filePath).getName()));
    }

    // --- CUSTOM SWING COMPONENTS / OVERRIDES ---

    public static class CustomCheckboxIcon implements Icon {
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

    public static class CustomToggleSwitch extends JToggleButton {
        private float location = 0f;
        private javax.swing.Timer timer;

        public CustomToggleSwitch(boolean selected) {
            super();
            setSelected(selected);
            location = selected ? 1f : 0f;
            setPreferredSize(new Dimension(48, 24));
            setMinimumSize(new Dimension(48, 24));
            setMaximumSize(new Dimension(48, 24));
            setBorder(null);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addActionListener(e -> {
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }
                final boolean target = isSelected();
                timer = new javax.swing.Timer(15, evt -> {
                    if (target) {
                        location += 0.2f;
                        if (location >= 1f) {
                            location = 1f;
                            timer.stop();
                        }
                    } else {
                        location -= 0.2f;
                        if (location <= 0f) {
                            location = 0f;
                            timer.stop();
                        }
                    }
                    repaint();
                });
                timer.start();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();

            g2.setColor(BG_COLOR);
            g2.fillRect(0, 0, w, h);

            g2.setColor(isSelected() ? Color.WHITE : TEXT_MUTED);
            g2.drawRect(0, 0, w - 1, h - 1);

            int knobSize = h - 6;
            int maxTravel = w - knobSize - 6;
            int knobX = 3 + (int)(location * maxTravel);
            int knobY = 3;

            g2.setColor(Color.WHITE);
            g2.fillRect(knobX, knobY, knobSize, knobSize);

            g2.dispose();
        }
    }

    public static class MaxWidthCenterLayout implements LayoutManager {
        private final int maxWidth;

        public MaxWidthCenterLayout(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {}

        @Override
        public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            if (parent.getComponentCount() == 0) return new Dimension(0, 0);
            Component child = parent.getComponent(0);
            Dimension pref = child.getPreferredSize();
            return new Dimension(Math.min(pref.width, maxWidth), pref.height);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            if (parent.getComponentCount() == 0) return new Dimension(0, 0);
            Component child = parent.getComponent(0);
            Dimension min = child.getMinimumSize();
            return new Dimension(Math.min(min.width, maxWidth), min.height);
        }

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() == 0) return;
            Component child = parent.getComponent(0);
            int parentW = parent.getWidth();
            int parentH = parent.getHeight();

            int childW = Math.min(parentW, maxWidth);
            int childH = parentH;

            int x = (parentW - childW) / 2;
            int y = 0;

            child.setBounds(x, y, childW, childH);
        }
    }

    // --- CUSTOM TITLE BAR & POPUP DIALOG METHODS ---

    private JPanel createCustomTitleBar(Window window, String title, boolean showMinimize) {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(Color.BLACK);
        titleBar.setPreferredSize(new Dimension(window.getWidth(), 32));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Dragging support
        final Point[] dragOffset = {null};
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getPoint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && window instanceof Frame && showMinimize) {
                    Frame frame = (Frame) window;
                    int state = frame.getExtendedState();
                    if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                        frame.setExtendedState(Frame.NORMAL);
                    } else {
                        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    }
                    frame.revalidate();
                    frame.repaint();
                }
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset[0] != null) {
                    if (window instanceof Frame && (((Frame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                        return;
                    }
                    Point curr = e.getLocationOnScreen();
                    window.setLocation(curr.x - dragOffset[0].x, curr.y - dragOffset[0].y);
                }
            }
        });

        // Left section: Logo + Title text
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(Color.BLACK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 8, 0, 0);

        java.net.URL logoUrl = MainFrame.class.getResource("/desktop-app-logo-started.png");
        if (logoUrl != null) {
            ImageIcon icon = new ImageIcon(new ImageIcon(logoUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            JLabel logoLabel = new JLabel(icon);
            leftPanel.add(logoLabel, gbc);
            gbc.gridx++;
        }

        JLabel titleLabel = new JLabel(title.toLowerCase());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(TEXT_PRIMARY);
        gbc.insets = new Insets(0, 8, 0, 8);
        leftPanel.add(titleLabel, gbc);

        titleBar.add(leftPanel, BorderLayout.WEST);

        // Right section: Minimize, Maximize & Close buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(Color.BLACK);

        if (showMinimize) {
            TitleBarButton minBtn = new TitleBarButton(TitleBarButton.Type.MINIMIZE, window);
            minBtn.addActionListener(e -> {
                if (window instanceof Frame) {
                    ((Frame) window).setExtendedState(Frame.ICONIFIED);
                }
            });
            rightPanel.add(minBtn);

            TitleBarButton maxBtn = new TitleBarButton(TitleBarButton.Type.MAXIMIZE, window);
            maxBtn.addActionListener(e -> {
                if (window instanceof Frame) {
                    Frame frame = (Frame) window;
                    int state = frame.getExtendedState();
                    if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                        frame.setExtendedState(Frame.NORMAL);
                    } else {
                        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    }
                    frame.revalidate();
                    frame.repaint();
                }
            });
            rightPanel.add(maxBtn);
        }

        TitleBarButton closeBtn = new TitleBarButton(TitleBarButton.Type.CLOSE, window);
        closeBtn.addActionListener(e -> {
            if (window instanceof MainFrame) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            } else {
                window.setVisible(false);
                window.dispose();
            }
        });
        rightPanel.add(closeBtn);

        titleBar.add(rightPanel, BorderLayout.EAST);
        return titleBar;
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
            setPreferredSize(new Dimension(46, 31));
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


    private void showCaptureNotificationDialog(String fileName) {
        JDialog dialog = new JDialog(this, "kiroku", false);
        dialog.setUndecorated(true);
        dialog.setSize(360, 140);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BG_COLOR);
        
        JPanel titleBar = createCustomTitleBar(dialog, "kiroku", false);
        dialog.add(titleBar, BorderLayout.NORTH);
        
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setBackground(BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        
        // Painted Phone Outline Icon
        JPanel phoneIconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                // phone outline
                g2.drawRoundRect(10, 4, 20, 38, 5, 5);
                // screen border
                g2.drawRect(12, 8, 16, 30);
                // camera notch
                g2.fillRect(17, 6, 6, 1);
                g2.dispose();
            }
        };
        phoneIconPanel.setPreferredSize(new Dimension(40, 44));
        phoneIconPanel.setBackground(BG_COLOR);
        content.add(phoneIconPanel, BorderLayout.WEST);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(BG_COLOR);
        
        JLabel messageLabel = new JLabel("screenshot captured successfully");
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(messageLabel);
        textPanel.add(Box.createVerticalStrut(4));
        
        JLabel fileLabel = new JLabel(fileName);
        fileLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        fileLabel.setForeground(TEXT_MUTED);
        fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(fileLabel);
        
        content.add(textPanel, BorderLayout.CENTER);
        dialog.add(content, BorderLayout.CENTER);
        
        // Footer (OK Button and bottom 2px white line)
        JPanel bottomArea = new JPanel();
        bottomArea.setLayout(new BoxLayout(bottomArea, BoxLayout.Y_AXIS));
        bottomArea.setBackground(BG_COLOR);
        
        JPanel footerRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));
        footerRow.setBackground(BG_COLOR);
        
        JButton okBtn = new JButton("OK");
        styleDialogButton(okBtn, true);
        okBtn.setPreferredSize(new Dimension(80, 24));
        okBtn.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        footerRow.add(okBtn);
        bottomArea.add(footerRow);
        
        // Bottom 2px white line
        JPanel accentLine = new JPanel();
        accentLine.setBackground(Color.WHITE);
        accentLine.setPreferredSize(new Dimension(360, 2));
        accentLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        bottomArea.add(accentLine);
        
        dialog.add(bottomArea, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        
        // Position at bottom right corner of the screen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle screenRect = ge.getMaximumWindowBounds();
        int x = screenRect.x + screenRect.width - dialog.getWidth() - 20;
        int y = screenRect.y + screenRect.height - dialog.getHeight() - 20;
        dialog.setLocation(x, y);
        
        // Auto close after 3 seconds
        javax.swing.Timer timer = new javax.swing.Timer(3000, evt -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        timer.setRepeats(false);
        timer.start();
        
        dialog.setVisible(true);
    }

    private void showDeleteConfirmDialog(String name, String id, String type, Runnable onConfirm) {
        JDialog dialog = new JDialog(this, "confirm delete", true);
        dialog.setUndecorated(true);
        dialog.setSize(400, 220);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BG_COLOR);
        
        JPanel titleBar = createCustomTitleBar(dialog, "confirm delete", false);
        dialog.add(titleBar, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout(16, 0));
        content.setBackground(BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        
        // Warning Icon Panel (triangle)
        JPanel warningIconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                int[] xPoints = {24, 4, 44};
                int[] yPoints = {6, 40, 40};
                g2.drawPolygon(xPoints, yPoints, 3);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                g2.drawString("!", 22, 33);
                g2.dispose();
            }
        };
        warningIconPanel.setPreferredSize(new Dimension(48, 48));
        warningIconPanel.setBackground(BG_COLOR);
        content.add(warningIconPanel, BorderLayout.WEST);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(BG_COLOR);
        
        JLabel messageLabel = new JLabel("delete " + name.toLowerCase() + "?");
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(messageLabel);
        textPanel.add(Box.createVerticalStrut(6));
        
        JLabel subtextLabel = new JLabel("<html><body style='width: 250px;'>This action is permanent and cannot be reversed. All associated logs will be purged.</body></html>");
        subtextLabel.setFont(fontBody);
        subtextLabel.setForeground(TEXT_MUTED);
        subtextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(subtextLabel);
        textPanel.add(Box.createVerticalStrut(12));
        
        // Detail Metadata Panel
        JPanel detailBox = new JPanel(new BorderLayout());
        detailBox.setBackground(new Color(0x0d, 0x0e, 0x0f));
        detailBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3D, 0x3D, 0x3D), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        detailBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel idLabel = new JLabel("ID: " + id.toUpperCase());
        idLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        idLabel.setForeground(TEXT_MUTED);
        detailBox.add(idLabel, BorderLayout.WEST);
        
        JLabel typeLabel = new JLabel("TYPE: " + type.toUpperCase());
        typeLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        typeLabel.setForeground(TEXT_MUTED);
        detailBox.add(typeLabel, BorderLayout.EAST);
        
        textPanel.add(detailBox);
        content.add(textPanel, BorderLayout.CENTER);
        dialog.add(content, BorderLayout.CENTER);
        
        // Action Buttons Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0x1b, 0x1c, 0x1c));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        
        JButton noBtn = new JButton("No");
        styleDialogButton(noBtn, false);
        noBtn.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        footer.add(noBtn);
        
        JButton yesBtn = new JButton("Yes");
        styleDialogButton(yesBtn, true);
        yesBtn.addActionListener(e -> {
            onConfirm.run();
            dialog.setVisible(false);
            dialog.dispose();
        });
        footer.add(yesBtn);
        
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showWhitelistDialog(AppConfig.WhitelistEntry entry, boolean isEdit, int selectedIdx) {
        JDialog dialog = new JDialog(this, isEdit ? "edit whitelist entry" : "add whitelist entry", true);
        dialog.setUndecorated(true);
        dialog.setSize(470, 310);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BG_COLOR);
        
        JPanel titleBar = createCustomTitleBar(dialog, isEdit ? "edit whitelist entry" : "add whitelist entry", false);
        dialog.add(titleBar, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        
        JLabel nameLabel = new JLabel("NAME (E.G. GOOGLE MEET)");
        nameLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        nameLabel.setForeground(TEXT_MUTED);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(4));
        
        JTextField nameField = new JTextField(isEdit ? entry.name : "");
        styleDialogTextField(nameField);
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(nameField);
        content.add(Box.createVerticalStrut(12));
        
        JLabel urlLabel = new JLabel("BASE URL (E.G. MEET.GOOGLE.COM)");
        urlLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        urlLabel.setForeground(TEXT_MUTED);
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(urlLabel);
        content.add(Box.createVerticalStrut(4));
        
        JTextField urlField = new JTextField(isEdit ? entry.baseUrl : "");
        styleDialogTextField(urlField);
        urlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(urlField);
        content.add(Box.createVerticalStrut(12));
        
        JCheckBox requirePatternCheckBox = new JCheckBox("require room/meeting pattern (auto-regex)", isEdit ? entry.hasParameter : true);
        requirePatternCheckBox.setFont(fontBody);
        requirePatternCheckBox.setBackground(BG_COLOR);
        requirePatternCheckBox.setForeground(TEXT_PRIMARY);
        requirePatternCheckBox.setFocusPainted(false);
        requirePatternCheckBox.setIcon(new CustomCheckboxIcon(false));
        requirePatternCheckBox.setSelectedIcon(new CustomCheckboxIcon(true));
        requirePatternCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(requirePatternCheckBox);
        content.add(Box.createVerticalStrut(4));
        
        JLabel patternDesc = new JLabel("Strict validation for dynamic meeting identifiers.");
        patternDesc.setFont(fontMuted);
        patternDesc.setForeground(TEXT_MUTED);
        patternDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        patternDesc.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        content.add(patternDesc);
        
        dialog.add(content, BorderLayout.CENTER);
        
        // Footer Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0x1b, 0x1c, 0x1c));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        
        JButton cancelBtn = new JButton("Cancel");
        styleDialogButton(cancelBtn, false);
        cancelBtn.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        footer.add(cancelBtn);
        
        JButton okBtn = new JButton("OK");
        styleDialogButton(okBtn, true);
        okBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String host = urlField.getText().trim();
            boolean requirePattern = requirePatternCheckBox.isSelected();
            
            if (name.isEmpty() || host.isEmpty()) {
                showTextDialog("name and base url cannot be empty.", "error", true);
                return;
            }
            
            AppConfig config = ConfigManager.getConfig();
            if (isEdit) {
                entry.name = name;
                entry.baseUrl = host;
                entry.hasParameter = requirePattern;
                entry.parameterPattern = requirePattern ? getAutomaticPattern(host) : "";
            } else {
                AppConfig.WhitelistEntry newEntry = new AppConfig.WhitelistEntry();
                newEntry.id = Long.toString(System.currentTimeMillis()) + "_" + (int)(Math.random() * 1000);
                newEntry.name = name;
                newEntry.baseUrl = host;
                newEntry.hasParameter = requirePattern;
                newEntry.parameterPattern = requirePattern ? getAutomaticPattern(host) : "";
                newEntry.enabled = true;
                config.whitelist.add(newEntry);
            }
            ConfigManager.saveConfig();
            log((isEdit ? "edited" : "added") + " whitelist: " + name.toLowerCase());
            rebuildWhitelistRows();
            
            dialog.setVisible(false);
            dialog.dispose();
        });
        footer.add(okBtn);
        
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showOpenChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        AppConfig config = ConfigManager.getConfig();
        chooser.setCurrentDirectory(new File(config.rootDir));
        
        JDialog dialog = new JDialog(this, "open", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BG_COLOR);
        
        JPanel titleBar = createCustomTitleBar(dialog, "open", false);
        dialog.add(titleBar, BorderLayout.NORTH);
        dialog.add(chooser, BorderLayout.CENTER);
        
        chooser.addActionListener(evt -> {
            if (evt.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                folderPathLabel.setText(path);
                config.rootDir = path;
                ConfigManager.saveConfig();
                log("save directory updated to: " + path);
                dialog.setVisible(false);
                dialog.dispose();
            } else if (evt.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showTextDialog(String message, String title, boolean isError) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        dialog.setSize(340, 160);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BG_COLOR);
        
        JPanel titleBar = createCustomTitleBar(dialog, title, false);
        dialog.add(titleBar, BorderLayout.NORTH);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        
        JLabel msgLabel = new JLabel("<html><body style='width: 280px;'>" + message + "</body></html>");
        msgLabel.setFont(fontBody);
        msgLabel.setForeground(TEXT_PRIMARY);
        msgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(msgLabel);
        
        dialog.add(content, BorderLayout.CENTER);
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(0x1b, 0x1c, 0x1c));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        
        JButton okBtn = new JButton("OK");
        styleDialogButton(okBtn, true);
        okBtn.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
        footer.add(okBtn);
        
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void styleDialogTextField(JTextField tf) {
        tf.setBackground(new Color(0x0d, 0x0e, 0x0f));
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setFont(fontLabel);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3D, 0x3D, 0x3D), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        tf.setMaximumSize(new Dimension(420, 32));
        tf.setPreferredSize(new Dimension(420, 32));
    }

    private void styleDialogButton(JButton btn, boolean primary) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(90, 30));
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
            btn.setBorder(BorderFactory.createLineBorder(new Color(0x3D, 0x3D, 0x3D), 1));
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

    public static class FrameResizeListener extends MouseAdapter {
        private final JFrame frame;
        private final int borderSize = 6;
        private int dragType = 0;
        private Point startPos;
        private Rectangle startBounds;

        public FrameResizeListener(JFrame frame) {
            this.frame = frame;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                frame.setCursor(Cursor.getDefaultCursor());
                return;
            }
            Point p = e.getPoint();
            int w = frame.getWidth();
            int h = frame.getHeight();
            int cursorType = Cursor.DEFAULT_CURSOR;

            if (p.x < borderSize) {
                if (p.y < borderSize) cursorType = Cursor.NW_RESIZE_CURSOR;
                else if (p.y > h - borderSize) cursorType = Cursor.SW_RESIZE_CURSOR;
                else cursorType = Cursor.W_RESIZE_CURSOR;
            } else if (p.x > w - borderSize) {
                if (p.y < borderSize) cursorType = Cursor.NE_RESIZE_CURSOR;
                else if (p.y > h - borderSize) cursorType = Cursor.SE_RESIZE_CURSOR;
                else cursorType = Cursor.E_RESIZE_CURSOR;
            } else if (p.y < borderSize) {
                cursorType = Cursor.N_RESIZE_CURSOR;
            } else if (p.y > h - borderSize) {
                cursorType = Cursor.S_RESIZE_CURSOR;
            }

            frame.setCursor(Cursor.getPredefinedCursor(cursorType));
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                dragType = 0;
                return;
            }
            startPos = e.getLocationOnScreen();
            startBounds = frame.getBounds();
            
            Point p = e.getPoint();
            int w = frame.getWidth();
            int h = frame.getHeight();
            dragType = 0;

            if (p.x < borderSize) {
                if (p.y < borderSize) dragType = Cursor.NW_RESIZE_CURSOR;
                else if (p.y > h - borderSize) dragType = Cursor.SW_RESIZE_CURSOR;
                else dragType = Cursor.W_RESIZE_CURSOR;
            } else if (p.x > w - borderSize) {
                if (p.y < borderSize) dragType = Cursor.NE_RESIZE_CURSOR;
                else if (p.y > h - borderSize) dragType = Cursor.SE_RESIZE_CURSOR;
                else dragType = Cursor.E_RESIZE_CURSOR;
            } else if (p.y < borderSize) {
                dragType = Cursor.N_RESIZE_CURSOR;
            } else if (p.y > h - borderSize) {
                dragType = Cursor.S_RESIZE_CURSOR;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                return;
            }
            if (dragType == 0) return;

            Point currPos = e.getLocationOnScreen();
            int dx = currPos.x - startPos.x;
            int dy = currPos.y - startPos.y;

            int newX = startBounds.x;
            int newY = startBounds.y;
            int newW = startBounds.width;
            int newH = startBounds.height;

            Dimension minSize = frame.getMinimumSize();
            if (minSize == null) minSize = new Dimension(100, 100);

            if (dragType == Cursor.E_RESIZE_CURSOR || dragType == Cursor.NE_RESIZE_CURSOR || dragType == Cursor.SE_RESIZE_CURSOR) {
                newW = Math.max(minSize.width, startBounds.width + dx);
            }
            if (dragType == Cursor.S_RESIZE_CURSOR || dragType == Cursor.SW_RESIZE_CURSOR || dragType == Cursor.SE_RESIZE_CURSOR) {
                newH = Math.max(minSize.height, startBounds.height + dy);
            }
            if (dragType == Cursor.W_RESIZE_CURSOR || dragType == Cursor.NW_RESIZE_CURSOR || dragType == Cursor.SW_RESIZE_CURSOR) {
                int potentialW = startBounds.width - dx;
                if (potentialW >= minSize.width) {
                    newX = startBounds.x + dx;
                    newW = potentialW;
                }
            }
            if (dragType == Cursor.N_RESIZE_CURSOR || dragType == Cursor.NW_RESIZE_CURSOR || dragType == Cursor.NE_RESIZE_CURSOR) {
                int potentialH = startBounds.height - dy;
                if (potentialH >= minSize.height) {
                    newY = startBounds.y + dy;
                    newH = potentialH;
                }
            }

            frame.setBounds(newX, newY, newW, newH);
            frame.validate();
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            frame.setCursor(Cursor.getDefaultCursor());
        }
    }
}
