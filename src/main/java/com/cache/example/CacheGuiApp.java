package com.cache.example;

import com.cache.bloom.GuavaBloomFilterService;
import com.cache.circuitbreaker.DefaultCircuitBreaker;
import com.cache.config.*;
import com.cache.core.CacheEntry;
import com.cache.core.CacheLevel;
import com.cache.facade.CacheFacade;
import com.cache.facade.MultiLevelCacheFacade;
import com.cache.hotkey.SlidingWindowHotKeyDetector;
import com.cache.local.CaffeineLocalCache;
import com.cache.metrics.CacheMetrics;
import com.cache.metrics.DefaultCacheMetrics;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 多级缓存系统 GUI 演示程序
 */
public class CacheGuiApp extends JFrame {
    
    private CacheFacade<String, String> cacheFacade;
    private CacheMetrics metrics;
    private SlidingWindowHotKeyDetector hotKeyDetector;
    private GuavaBloomFilterService bloomFilter;
    private Map<String, String> database;
    
    // UI组件
    private JTextField keyInput;
    private JTextField valueInput;
    private JTextArea logArea;
    private JLabel l1HitLabel;
    private JLabel l1MissLabel;
    private JLabel l1RateLabel;
    private JLabel l2HitLabel;
    private JLabel l2MissLabel;
    private JLabel l2RateLabel;
    private JTextArea hotKeysArea;
    private JTextArea prometheusArea;
    
    public CacheGuiApp() {
        initializeCache();
        initializeUI();
    }
    
    private void initializeCache() {
        // 创建配置
        CacheConfig config = CacheConfig.builder()
            .localCache(LocalCacheConfig.builder()
                .maxSize(1000)
                .defaultTtl(Duration.ofSeconds(60))
                .recordStats(true)
                .build())
            .bloomFilter(BloomFilterConfig.builder()
                .expectedInsertions(10000)
                .falsePositiveRate(0.01)
                .build())
            .nullCache(NullCacheConfig.builder()
                .enabled(true)
                .ttl(Duration.ofMinutes(1))
                .build())
            .hotKeyDetection(HotKeyDetectionConfig.builder()
                .enabled(true)
                .threshold(3)
                .timeWindow(Duration.ofSeconds(30))
                .build())
            .circuitBreaker(CircuitBreakerConfig.builder()
                .enabled(true)
                .failureThreshold(3)
                .resetTimeout(Duration.ofSeconds(10))
                .build())
            .build();
        
        // 创建组件
        var localCache = new CaffeineLocalCache<String, CacheEntry<String>>(config.localCache());
        var mockDistributedCache = new MockDistributedCache<String, CacheEntry<String>>();
        bloomFilter = new GuavaBloomFilterService(config.bloomFilter());
        hotKeyDetector = new SlidingWindowHotKeyDetector(config.hotKeyDetection());
        var circuitBreaker = new DefaultCircuitBreaker(config.circuitBreaker());
        metrics = new DefaultCacheMetrics();
        
        // 模拟数据库
        database = new HashMap<>();
        database.put("user:1", "张三");
        database.put("user:2", "李四");
        database.put("user:3", "王五");
        database.put("product:100", "iPhone 15");
        database.put("product:200", "MacBook Pro");
        database.put("order:1001", "订单-手机购买");
        database.put("order:1002", "订单-电脑购买");
        
        // 预热布隆过滤器
        database.keySet().forEach(bloomFilter::add);
        
        // 创建缓存门面
        cacheFacade = new MultiLevelCacheFacade<>(
            localCache,
            mockDistributedCache,
            bloomFilter,
            hotKeyDetector,
            circuitBreaker,
            metrics,
            config,
            key -> database.get(key)
        );
    }
    
    private void initializeUI() {
        setTitle("多级缓存系统演示 - Multi-Level Cache System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 顶部操作面板
        mainPanel.add(createOperationPanel(), BorderLayout.NORTH);
        
        // 中间区域
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setLeftComponent(createLogPanel());
        centerSplit.setRightComponent(createStatsPanel());
        centerSplit.setDividerLocation(500);
        mainPanel.add(centerSplit, BorderLayout.CENTER);
        
        // 底部Prometheus面板
        mainPanel.add(createPrometheusPanel(), BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // 初始化显示
        updateStats();
    }
    
    private JPanel createOperationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("缓存操作"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Key输入
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        keyInput = new JTextField(20);
        keyInput.setText("user:1");
        panel.add(keyInput, gbc);
        
        // Value输入
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Value:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        valueInput = new JTextField(20);
        panel.add(valueInput, gbc);
        
        // 按钮
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JButton getBtn = new JButton("查询 (GET)");
        getBtn.setBackground(new Color(76, 175, 80));
        getBtn.addActionListener(e -> doGet());
        panel.add(getBtn, gbc);
        
        gbc.gridx = 1;
        JButton putBtn = new JButton("写入 (PUT)");
        putBtn.setBackground(new Color(33, 150, 243));
        putBtn.addActionListener(e -> doPut());
        panel.add(putBtn, gbc);
        
        gbc.gridx = 2;
        JButton deleteBtn = new JButton("删除 (DELETE)");
        deleteBtn.setBackground(new Color(244, 67, 54));
        deleteBtn.addActionListener(e -> doDelete());
        panel.add(deleteBtn, gbc);
        
        gbc.gridx = 3;
        JButton invalidateBtn = new JButton("失效 (双删)");
        invalidateBtn.setBackground(new Color(255, 152, 0));
        invalidateBtn.addActionListener(e -> doInvalidate());
        panel.add(invalidateBtn, gbc);
        
        // 预设数据提示
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        JLabel hintLabel = new JLabel("预设数据: user:1, user:2, user:3, product:100, product:200, order:1001, order:1002");
        hintLabel.setForeground(Color.GRAY);
        panel.add(hintLabel, gbc);
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("操作日志"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton clearBtn = new JButton("清空日志");
        clearBtn.addActionListener(e -> logArea.setText(""));
        panel.add(clearBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 统计信息面板
        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        statsPanel.setBorder(new TitledBorder("缓存统计"));
        
        // L1统计
        JPanel l1Panel = new JPanel(new GridLayout(3, 2, 5, 5));
        l1Panel.setBorder(new TitledBorder("L1 本地缓存 (Caffeine)"));
        l1Panel.add(new JLabel("命中次数:"));
        l1HitLabel = new JLabel("0");
        l1HitLabel.setForeground(new Color(76, 175, 80));
        l1Panel.add(l1HitLabel);
        l1Panel.add(new JLabel("未命中次数:"));
        l1MissLabel = new JLabel("0");
        l1MissLabel.setForeground(new Color(244, 67, 54));
        l1Panel.add(l1MissLabel);
        l1Panel.add(new JLabel("命中率:"));
        l1RateLabel = new JLabel("0.00%");
        l1RateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        l1Panel.add(l1RateLabel);
        statsPanel.add(l1Panel);
        
        // L2统计
        JPanel l2Panel = new JPanel(new GridLayout(3, 2, 5, 5));
        l2Panel.setBorder(new TitledBorder("L2 分布式缓存 (Redis模拟)"));
        l2Panel.add(new JLabel("命中次数:"));
        l2HitLabel = new JLabel("0");
        l2HitLabel.setForeground(new Color(76, 175, 80));
        l2Panel.add(l2HitLabel);
        l2Panel.add(new JLabel("未命中次数:"));
        l2MissLabel = new JLabel("0");
        l2MissLabel.setForeground(new Color(244, 67, 54));
        l2Panel.add(l2MissLabel);
        l2Panel.add(new JLabel("命中率:"));
        l2RateLabel = new JLabel("0.00%");
        l2RateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        l2Panel.add(l2RateLabel);
        statsPanel.add(l2Panel);
        
        panel.add(statsPanel, BorderLayout.NORTH);
        
        // 热点Key面板
        JPanel hotKeyPanel = new JPanel(new BorderLayout());
        hotKeyPanel.setBorder(new TitledBorder("热点Key检测"));
        hotKeysArea = new JTextArea(5, 20);
        hotKeysArea.setEditable(false);
        hotKeysArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hotKeyPanel.add(new JScrollPane(hotKeysArea), BorderLayout.CENTER);
        panel.add(hotKeyPanel, BorderLayout.CENTER);
        
        // 刷新按钮
        JButton refreshBtn = new JButton("刷新统计");
        refreshBtn.addActionListener(e -> updateStats());
        panel.add(refreshBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPrometheusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Prometheus 监控指标"));
        
        prometheusArea = new JTextArea(6, 50);
        prometheusArea.setEditable(false);
        prometheusArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(prometheusArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton exportBtn = new JButton("导出Prometheus指标");
        exportBtn.addActionListener(e -> {
            prometheusArea.setText(metrics.exportPrometheus());
        });
        panel.add(exportBtn, BorderLayout.EAST);
        
        return panel;
    }
    
    private void doGet() {
        String key = keyInput.getText().trim();
        if (key.isEmpty()) {
            showMessage("请输入Key", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        long start = System.currentTimeMillis();
        Optional<String> result = cacheFacade.get(key);
        long elapsed = System.currentTimeMillis() - start;
        
        if (result.isPresent()) {
            log("✓ GET [%s] = %s (耗时: %dms)", key, result.get(), elapsed);
            valueInput.setText(result.get());
        } else {
            log("✗ GET [%s] = null (耗时: %dms) - 可能被布隆过滤器拦截或数据不存在", key, elapsed);
            valueInput.setText("");
        }
        
        updateStats();
    }
    
    private void doPut() {
        String key = keyInput.getText().trim();
        String value = valueInput.getText().trim();
        
        if (key.isEmpty() || value.isEmpty()) {
            showMessage("请输入Key和Value", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        long start = System.currentTimeMillis();
        cacheFacade.put(key, value, Duration.ofMinutes(5));
        long elapsed = System.currentTimeMillis() - start;
        
        // 同时更新模拟数据库
        database.put(key, value);
        bloomFilter.add(key);
        
        log("✓ PUT [%s] = %s (耗时: %dms)", key, value, elapsed);
        updateStats();
    }
    
    private void doDelete() {
        String key = keyInput.getText().trim();
        if (key.isEmpty()) {
            showMessage("请输入Key", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        long start = System.currentTimeMillis();
        cacheFacade.delete(key);
        long elapsed = System.currentTimeMillis() - start;
        
        log("✓ DELETE [%s] (耗时: %dms)", key, elapsed);
        updateStats();
    }
    
    private void doInvalidate() {
        String key = keyInput.getText().trim();
        if (key.isEmpty()) {
            showMessage("请输入Key", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        long start = System.currentTimeMillis();
        cacheFacade.invalidate(key);
        long elapsed = System.currentTimeMillis() - start;
        
        log("✓ INVALIDATE [%s] - 延迟双删已触发 (耗时: %dms)", key, elapsed);
        updateStats();
    }
    
    private void updateStats() {
        var snapshot = metrics.snapshot();
        
        // L1统计
        long l1Hit = snapshot.hitCounts().getOrDefault(CacheLevel.L1_LOCAL, 0L);
        long l1Miss = snapshot.missCounts().getOrDefault(CacheLevel.L1_LOCAL, 0L);
        double l1Rate = snapshot.hitRates().getOrDefault(CacheLevel.L1_LOCAL, 0.0);
        
        l1HitLabel.setText(String.valueOf(l1Hit));
        l1MissLabel.setText(String.valueOf(l1Miss));
        l1RateLabel.setText(String.format("%.2f%%", l1Rate * 100));
        
        // L2统计
        long l2Hit = snapshot.hitCounts().getOrDefault(CacheLevel.L2_DISTRIBUTED, 0L);
        long l2Miss = snapshot.missCounts().getOrDefault(CacheLevel.L2_DISTRIBUTED, 0L);
        double l2Rate = snapshot.hitRates().getOrDefault(CacheLevel.L2_DISTRIBUTED, 0.0);
        
        l2HitLabel.setText(String.valueOf(l2Hit));
        l2MissLabel.setText(String.valueOf(l2Miss));
        l2RateLabel.setText(String.format("%.2f%%", l2Rate * 100));
        
        // 热点Key
        var hotKeys = hotKeyDetector.getHotKeys();
        if (hotKeys.isEmpty()) {
            hotKeysArea.setText("暂无热点Key\n(同一Key在30秒内访问3次以上将被标记为热点)");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("当前热点Key列表:\n");
            for (String key : hotKeys) {
                sb.append("  🔥 ").append(key).append("\n");
            }
            hotKeysArea.setText(sb.toString());
        }
    }
    
    private void log(String format, Object... args) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String message = String.format("[%s] %s\n", timestamp, String.format(format, args));
        logArea.append(message);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }
    
    public static void main(String[] args) {
        // 设置外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 启动GUI
        SwingUtilities.invokeLater(() -> {
            CacheGuiApp app = new CacheGuiApp();
            app.setVisible(true);
        });
    }
}
