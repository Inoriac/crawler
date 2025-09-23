package com.pixiv.crawler.gui;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.main.PixivCrawler;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.service.impl.ArtistServiceImpl;
import com.pixiv.crawler.service.impl.Downloader;
import com.pixiv.crawler.service.impl.PopularImageServiceImpl;
import com.pixiv.crawler.util.JsonUtil;
import com.pixiv.crawler.util.SettingsManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.text.Font;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class PixivCrawlerGUI extends Application {
    
    // 日志区域
    private TextArea logArea;
    
    // 相关推荐功能控件
    private TextField startPidField;
    private TextField maxDepthField;
    private TextField imagesPerRoundField;
    private TextField recommendSavePathField;
    private Button recommendStartButton;
    private Button recommendStopButton;
    
    // 日榜功能控件
    private TextField rankingSavePathField;
    private Button rankingStartButton;
    private Button rankingStopButton;
    
    // 热门作品功能控件
    private TextField popularTagField;
    private TextField popularSavePathField;
    private Button popularStartButton;
    private Button popularStopButton;
    
    // 画师作品功能控件
    private TextField artistIdField;
    private TextField artistSavePathField;
    private Button artistStartButton;
    private Button artistStopButton;
    
    // 通用控件
    // 移除statusLabel和progressBar，因为右侧面板只保留日志区域
    
    // 执行器和状态
    private ExecutorService executorService;
    private volatile boolean isRunning = false;
    private PixivCrawler crawler;
    private Downloader downloader;
    
    // 面板切换相关
    private VBox contentArea;
    private VBox functionPanel;
    private VBox settingsPanel;
    private Label currentModeLabel;
    
    // 日志处理器
    private GUILogHandler logHandler;
    
    // 背景相关
    private String currentBackgroundPath = null;
    private Background currentBackground = null;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pixiv 爬虫工具 - 多功能版本");
        
        // 创建主布局 - 垂直布局
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        
        // 加载保存的背景设置
        loadSavedBackground(mainLayout);
        
        // 顶部：功能栏选择区
        HBox topBar = createTopBar();
        
        // 中间：主要内容区域
        this.contentArea = new VBox(20);
        
        // 创建功能面板
        this.functionPanel = createFunctionPanel();
        
        // 创建设置面板
        this.settingsPanel = createSettingsPanel();
        
        // 默认显示功能面板
        this.contentArea.getChildren().add(this.functionPanel);
        
        // 组装主布局
        mainLayout.getChildren().addAll(topBar, contentArea);
        
        // 设置场景
        Scene scene = new Scene(mainLayout, 1300, 900);
        
        // 加载CSS样式文件
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        
        // 初始化
        initializeValues();
        setupEventHandlers();
        
        // 初始化日志处理器
        initializeLogHandler();
        
        // 显示窗口
        primaryStage.show();
        
        // 注册关闭事件
        primaryStage.setOnCloseRequest(event -> {
            if (isRunning) {
                event.consume();
                showAlert("请先停止所有爬虫任务");
            } else {
                shutdown();
            }
        });
    }
    
    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");
        
        // 功能按钮
        Button functionButton = new Button("功能区");
        functionButton.setPrefWidth(100);
        functionButton.getStyleClass().add("function-button");
        
        // 设置按钮
        Button settingsButton = new Button("设置");
        settingsButton.setPrefWidth(100);
        settingsButton.getStyleClass().add("function-button");
        
        // 当前状态标签
        Label currentModeLabel = new Label("当前模式：功能区");
        currentModeLabel.getStyleClass().addAll("mode-label", "black-text");
        currentModeLabel.setPadding(new Insets(0, 0, 0, 20));
        
        // 背景设置按钮
        Button backgroundButton = new Button("背景设置");
        backgroundButton.setPrefWidth(100);
        backgroundButton.getStyleClass().add("background-button");
        backgroundButton.setOnAction(e -> showBackgroundSettings());
        
        topBar.getChildren().addAll(functionButton, settingsButton, backgroundButton, currentModeLabel);
        
        // 按钮事件
        functionButton.setOnAction(e -> showFunctionPanel());
        settingsButton.setOnAction(e -> showSettingsPanel());
        
        return topBar;
    }
    
    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(15);
        leftPanel.setPrefWidth(500);
        leftPanel.getStyleClass().add("left-panel");
        
        // 标题
        Label titleLabel = new Label("Pixiv 多功能爬虫");
        titleLabel.getStyleClass().addAll("title-label", "black-text");
        titleLabel.setAlignment(Pos.CENTER);
        
        // 创建各个功能区域
        TitledPane recommendPane = createRecommendPane();
        TitledPane rankingPane = createRankingPane();
        TitledPane popularPane = createPopularPane();
        TitledPane artistPane = createArtistPane();
        
        leftPanel.getChildren().addAll(
            titleLabel,
            recommendPane,
            rankingPane,
            popularPane,
            artistPane
        );
        
        return leftPanel;
    }
    
    private VBox createFunctionPanel() {
        HBox functionLayout = new HBox(20);
        
        // 左侧：功能配置区域
        VBox leftPanel = createLeftPanel();
        
        // 右侧：日志和状态区域
        VBox rightPanel = createRightPanel();
        
        // 设置左右面板的比例，让右侧日志区域占据更多空间
        HBox.setHgrow(leftPanel, Priority.NEVER);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        functionLayout.getChildren().addAll(leftPanel, rightPanel);
        
        VBox functionPanel = new VBox(10);
        functionPanel.getChildren().add(functionLayout);
        
        return functionPanel;
    }
    
    private VBox createSettingsPanel() {
        VBox settingsPanel = new VBox(15);
        settingsPanel.setPadding(new Insets(20));
        settingsPanel.getStyleClass().add("settings-panel");
        
        // 标题
        Label titleLabel = new Label("系统设置");
        titleLabel.getStyleClass().addAll("settings-title-label", "black-text");
        titleLabel.setAlignment(Pos.CENTER);
        
        // 网络设置
        TitledPane networkPane = createNetworkSettingsPane();
        
        // 下载设置
        TitledPane downloadPane = createDownloadSettingsPane();
        
        // 算法设置
        TitledPane algorithmPane = createAlgorithmSettingsPane();
        
        // 路径设置
        TitledPane pathPane = createPathSettingsPane();
        
        // 保存按钮
        Button saveButton = new Button("保存设置");
        saveButton.setPrefWidth(120);
        saveButton.setPrefHeight(40);
        saveButton.getStyleClass().add("settings-button");
        saveButton.setOnAction(e -> saveSettings());
        
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().add(saveButton);
        
        settingsPanel.getChildren().addAll(
            titleLabel,
            networkPane,
            downloadPane,
            algorithmPane,
            pathPane,
            buttonBox
        );
        
        return settingsPanel;
    }
    
    private TitledPane createNetworkSettingsPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 端口设置
        HBox portBox = new HBox(10);
        TextField portField = new TextField(String.valueOf(GlobalConfig.PORT));
        portField.getStyleClass().add("text-field");
        portBox.getChildren().addAll(
            new Label("代理端口:"),
            portField
        );
        
        // 连接超时
        HBox connectTimeoutBox = new HBox(10);
        TextField connectTimeoutField = new TextField(String.valueOf(GlobalConfig.CONNECT_TIMEOUT));
        connectTimeoutField.getStyleClass().add("text-field");
        connectTimeoutBox.getChildren().addAll(
            new Label("连接超时(秒):"),
            connectTimeoutField
        );
        
        // 读取超时
        HBox readTimeoutBox = new HBox(10);
        TextField readTimeoutField = new TextField(String.valueOf(GlobalConfig.READ_TIMEOUT));
        readTimeoutField.getStyleClass().add("text-field");
        readTimeoutBox.getChildren().addAll(
            new Label("读取超时(秒):"),
            readTimeoutField
        );
        
        // 重试次数
        HBox retryBox = new HBox(10);
        TextField retryField = new TextField(String.valueOf(GlobalConfig.RETRY_COUNT));
        retryField.getStyleClass().add("text-field");
        retryBox.getChildren().addAll(
            new Label("重试次数:"),
            retryField
        );
        
        content.getChildren().addAll(portBox, connectTimeoutBox, readTimeoutBox, retryBox);
        
        TitledPane pane = new TitledPane("网络设置", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private TitledPane createDownloadSettingsPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 线程数
        HBox threadBox = new HBox(10);
        TextField threadField = new TextField(String.valueOf(GlobalConfig.THREAD_COUNT));
        threadField.getStyleClass().add("text-field");
        threadBox.getChildren().addAll(
            new Label("下载线程数:"),
            threadField
        );
        
        // R-18开关
        CheckBox r18CheckBox = new CheckBox("启用R-18下载");
        r18CheckBox.setSelected(GlobalConfig.R18_DOWNLOAD_ENABLED);
        r18CheckBox.getStyleClass().add("settings-checkbox");
        
        // 漫画排除开关
        CheckBox mangaCheckBox = new CheckBox("排除漫画作品");
        mangaCheckBox.setSelected(GlobalConfig.MANGA_EXCLUDE_ENABLED);
        mangaCheckBox.getStyleClass().add("settings-checkbox");
        
        // 自动清理开关
        CheckBox cleanCheckBox = new CheckBox("自动清理.part文件");
        cleanCheckBox.setSelected(GlobalConfig.AUTO_CLEAN_PART_FILES);
        cleanCheckBox.getStyleClass().add("settings-checkbox");
        
        // 显示进度开关
        CheckBox progressCheckBox = new CheckBox("显示下载进度");
        progressCheckBox.setSelected(GlobalConfig.SHOW_DOWNLOAD_PROGRESS);
        progressCheckBox.getStyleClass().add("settings-checkbox");
        
        content.getChildren().addAll(threadBox, r18CheckBox, mangaCheckBox, cleanCheckBox, progressCheckBox);
        
        TitledPane pane = new TitledPane("下载设置", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private TitledPane createAlgorithmSettingsPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 最大深度
        HBox depthBox = new HBox(10);
        TextField depthField = new TextField(String.valueOf(GlobalConfig.MAX_DEPTH));
        depthField.getStyleClass().add("text-field");
        depthBox.getChildren().addAll(
            new Label("最大深度:"),
            depthField
        );
        
        // 每轮图片数
        HBox roundBox = new HBox(10);
        TextField roundField = new TextField(String.valueOf(GlobalConfig.RECOMMEND_START_IMAGES_PER_ROUND));
        roundField.getStyleClass().add("text-field");
        roundBox.getChildren().addAll(
            new Label("每轮图片数:"),
            roundField
        );
        
        // 推荐图片数
        HBox recommendBox = new HBox(10);
        TextField recommendField = new TextField(String.valueOf(GlobalConfig.PER_RECOMMEND_MAX_IMAGE));
        recommendField.getStyleClass().add("text-field");
        recommendBox.getChildren().addAll(
            new Label("推荐图片数:"),
            recommendField
        );
        
        // 画师图片数
        HBox artistBox = new HBox(10);
        TextField artistField = new TextField(String.valueOf(GlobalConfig.ARTIST_MAX_IMAGE));
        artistField.getStyleClass().add("text-field");
        artistBox.getChildren().addAll(
            new Label("画师图片数:"),
            artistField
        );
        
        content.getChildren().addAll(depthBox, roundBox, recommendBox, artistBox);
        
        TitledPane pane = new TitledPane("算法设置", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private TitledPane createPathSettingsPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 基础路径
        HBox basePathBox = new HBox(10);
        TextField basePathField = new TextField(GlobalConfig.BASE_SAVE_PATH);
        basePathField.setPrefWidth(300);
        basePathField.getStyleClass().add("path-field");
        basePathBox.getChildren().addAll(
            new Label("基础保存路径:"),
            basePathField
        );
        
        Button selectPathButton = new Button("选择路径");
        selectPathButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择基础保存路径");
            File selectedDirectory = directoryChooser.showDialog(null);
            if (selectedDirectory != null) {
                basePathField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        basePathBox.getChildren().add(selectPathButton);
        
        content.getChildren().add(basePathBox);
        
        TitledPane pane = new TitledPane("路径设置", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private void showFunctionPanel() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(functionPanel);
        if (currentModeLabel != null) {
            currentModeLabel.setText("当前模式：功能区");
        }
    }
    
    private void showSettingsPanel() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(settingsPanel);
        if (currentModeLabel != null) {
            currentModeLabel.setText("当前模式：设置");
        }
    }
    
    private void initializeLogHandler() {
        // 创建日志处理器
        logHandler = new GUILogHandler(logArea);
        
        // 设置日志格式
        logHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return "[" + java.time.LocalTime.now().toString() + "] " + record.getMessage() + "\n";
            }
        });
        
        // 添加测试日志
        logHandler.log("GUI日志系统初始化完成");
        logHandler.log("编码设置：UTF-8");
    }
    
    private void setDefaultBackground(VBox mainLayout) {
        // 设置默认的渐变背景
        BackgroundFill backgroundFill = new BackgroundFill(
            new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(240, 248, 255)),  // 浅蓝色
                new Stop(1, Color.rgb(255, 255, 255))   // 白色
            ),
            CornerRadii.EMPTY, Insets.EMPTY
        );
        currentBackground = new Background(backgroundFill);
        mainLayout.setBackground(currentBackground);
    }
    
    private void loadSavedBackground(VBox mainLayout) {
        String backgroundType = SettingsManager.getBackgroundType();
        
        if ("image".equals(backgroundType)) {
            String imagePath = SettingsManager.getBackgroundPath();
            if (!imagePath.isEmpty() && new File(imagePath).exists()) {
                try {
                    Image backgroundImage = new Image(new File(imagePath).toURI().toString());
                    BackgroundImage bgImage = new BackgroundImage(
                        backgroundImage,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
                    );
                    
                    BackgroundFill transparentFill = new BackgroundFill(
                        Color.rgb(255, 255, 255, 0.3),
                        CornerRadii.EMPTY, Insets.EMPTY
                    );
                    
                    currentBackground = new Background(new BackgroundFill[]{transparentFill}, new BackgroundImage[]{bgImage});
                    currentBackgroundPath = imagePath;
                    mainLayout.setBackground(currentBackground);
                    return;
                } catch (Exception e) {
                    System.err.println("加载保存的背景图片失败: " + e.getMessage());
                }
            }
        } else {
            // 加载保存的主题
            String theme = SettingsManager.getBackgroundTheme();
            if (!"default".equals(theme)) {
                applyPresetBackground(theme);
                return;
            }
        }
        
        // 如果加载失败或没有保存的设置，使用默认背景
        setDefaultBackground(mainLayout);
    }
    
    private void showBackgroundSettings() {
        Stage backgroundStage = new Stage();
        backgroundStage.setTitle("背景设置");
        backgroundStage.initModality(Modality.APPLICATION_MODAL);
        
        VBox backgroundLayout = new VBox(20);
        backgroundLayout.setPadding(new Insets(20));
        backgroundLayout.setAlignment(Pos.CENTER);
        backgroundLayout.getStyleClass().add("background-settings-window");
        
        // 标题
        Label titleLabel = new Label("自定义背景设置");
        titleLabel.getStyleClass().addAll("settings-title-label", "black-text");
        titleLabel.setAlignment(Pos.CENTER);
        
        // 预设背景选项
        VBox presetBox = new VBox(10);
        presetBox.setAlignment(Pos.CENTER);
        presetBox.getStyleClass().add("background-button-group");
        Label presetLabel = new Label("预设背景:");
        presetLabel.getStyleClass().addAll("settings-group-label", "black-text");
        presetLabel.setAlignment(Pos.CENTER);
        
        Button defaultBtn = new Button("默认渐变");
        Button blueBtn = new Button("蓝色主题");
        Button greenBtn = new Button("绿色主题");
        Button purpleBtn = new Button("紫色主题");
        Button darkBtn = new Button("深色主题");
        
        // 设置按钮样式和居中
        defaultBtn.setPrefWidth(150);
        blueBtn.setPrefWidth(150);
        greenBtn.setPrefWidth(150);
        purpleBtn.setPrefWidth(150);
        darkBtn.setPrefWidth(150);
        
        defaultBtn.setOnAction(e -> applyPresetBackground("default"));
        blueBtn.setOnAction(e -> applyPresetBackground("blue"));
        greenBtn.setOnAction(e -> applyPresetBackground("green"));
        purpleBtn.setOnAction(e -> applyPresetBackground("purple"));
        darkBtn.setOnAction(e -> applyPresetBackground("dark"));
        
        presetBox.getChildren().addAll(presetLabel, defaultBtn, blueBtn, greenBtn, purpleBtn, darkBtn);
        
        // 自定义图片背景
        VBox customBox = new VBox(10);
        customBox.setAlignment(Pos.CENTER);
        customBox.getStyleClass().add("background-button-group");
        Label customLabel = new Label("自定义图片背景:");
        customLabel.getStyleClass().addAll("settings-group-label", "black-text");
        customLabel.setAlignment(Pos.CENTER);
        
        Button selectImageBtn = new Button("选择背景图片");
        Button clearImageBtn = new Button("清除背景图片");
        
        // 设置按钮样式和居中
        selectImageBtn.setPrefWidth(150);
        clearImageBtn.setPrefWidth(150);
        
        selectImageBtn.setOnAction(e -> selectBackgroundImage());
        clearImageBtn.setOnAction(e -> clearBackgroundImage());
        
        customBox.getChildren().addAll(customLabel, selectImageBtn, clearImageBtn);
        
        // 关闭按钮
        Button closeBtn = new Button("关闭");
        closeBtn.setPrefWidth(150);
        closeBtn.setOnAction(e -> backgroundStage.close());
        
        backgroundLayout.getChildren().addAll(titleLabel, presetBox, customBox, closeBtn);
        
        Scene scene = new Scene(backgroundLayout, 400, 500);
        backgroundStage.setScene(scene);
        backgroundStage.show();
    }
    
    private void applyPresetBackground(String theme) {
        BackgroundFill backgroundFill = null;
        
        switch (theme) {
            case "default":
                backgroundFill = new BackgroundFill(
                    new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(240, 248, 255)),
                        new Stop(1, Color.rgb(255, 255, 255))
                    ),
                    CornerRadii.EMPTY, Insets.EMPTY
                );
                break;
            case "blue":
                backgroundFill = new BackgroundFill(
                    new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(173, 216, 230)),
                        new Stop(1, Color.rgb(135, 206, 235))
                    ),
                    CornerRadii.EMPTY, Insets.EMPTY
                );
                break;
            case "green":
                backgroundFill = new BackgroundFill(
                    new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(144, 238, 144)),
                        new Stop(1, Color.rgb(34, 139, 34))
                    ),
                    CornerRadii.EMPTY, Insets.EMPTY
                );
                break;
            case "purple":
                backgroundFill = new BackgroundFill(
                    new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(221, 160, 221)),
                        new Stop(1, Color.rgb(138, 43, 226))
                    ),
                    CornerRadii.EMPTY, Insets.EMPTY
                );
                break;
            case "dark":
                backgroundFill = new BackgroundFill(
                    new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(64, 64, 64)),
                        new Stop(1, Color.rgb(32, 32, 32))
                    ),
                    CornerRadii.EMPTY, Insets.EMPTY
                );
                break;
        }
        
        if (backgroundFill != null) {
            currentBackground = new Background(backgroundFill);
            currentBackgroundPath = null;
            
            // 保存设置
            SettingsManager.setBackgroundType("gradient");
            SettingsManager.setBackgroundTheme(theme);
            SettingsManager.setBackgroundPath("");
            
            applyBackgroundToMainLayout();
        }
    }
    
    private void selectBackgroundImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择背景图片");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                currentBackgroundPath = selectedFile.getAbsolutePath();
                Image backgroundImage = new Image(selectedFile.toURI().toString());
                
                BackgroundImage bgImage = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
                );
                
                // 创建半透明的背景填充，让背景图片更加柔和
                BackgroundFill transparentFill = new BackgroundFill(
                    Color.rgb(255, 255, 255, 0.3), // 半透明白色
                    CornerRadii.EMPTY, Insets.EMPTY
                );
                
                // 组合背景图片和半透明填充
                currentBackground = new Background(new BackgroundFill[]{transparentFill}, new BackgroundImage[]{bgImage});
                
                // 保存设置
                SettingsManager.setBackgroundType("image");
                SettingsManager.setBackgroundPath(currentBackgroundPath);
                SettingsManager.setBackgroundTheme("");
                
                applyBackgroundToMainLayout();
                showAlert("背景图片设置成功并已保存！");
            } catch (Exception e) {
                showAlert("背景图片设置失败: " + e.getMessage());
            }
        }
    }
    
    private void clearBackgroundImage() {
        setDefaultBackground((VBox) contentArea.getParent());
        currentBackgroundPath = null;
        
        // 保存设置
        SettingsManager.setBackgroundType("gradient");
        SettingsManager.setBackgroundTheme("default");
        SettingsManager.setBackgroundPath("");
        
        showAlert("背景图片已清除并恢复默认设置！");
    }
    
    private void applyBackgroundToMainLayout() {
        // 获取主布局并应用背景
        if (contentArea != null && contentArea.getParent() != null) {
            VBox mainLayout = (VBox) contentArea.getParent();
            mainLayout.setBackground(currentBackground);
        }
    }
    
    private void saveSettings() {
        // 这里可以添加保存设置的逻辑
        showAlert("设置已保存！");
    }
    
    private TitledPane createRecommendPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 起始图片ID
        HBox pidBox = new HBox(10);
        startPidField = new TextField();
        startPidField.setPromptText("输入起始图片ID");
        startPidField.getStyleClass().add("text-field");
        pidBox.getChildren().addAll(
            new Label("起始图片ID:"),
            startPidField
        );
        
        // 最大深度
        HBox depthBox = new HBox(10);
        maxDepthField = new TextField();
        maxDepthField.setPromptText("推荐算法最大深度");
        maxDepthField.getStyleClass().add("text-field");
        depthBox.getChildren().addAll(
            new Label("最大深度:"),
            maxDepthField
        );
        
        // 每轮图片数
        HBox roundBox = new HBox(10);
        imagesPerRoundField = new TextField();
        imagesPerRoundField.setPromptText("每次获取的图片数量");
        imagesPerRoundField.getStyleClass().add("text-field");
        roundBox.getChildren().addAll(
            new Label("每轮图片数:"),
            imagesPerRoundField
        );
        
        // 保存路径
        HBox pathBox = new HBox(10);
        recommendSavePathField = new TextField();
        recommendSavePathField.setPromptText("选择图片保存路径");
        recommendSavePathField.setEditable(false);
        recommendSavePathField.getStyleClass().add("path-field");
        pathBox.getChildren().addAll(
            new Label("保存路径:"),
            recommendSavePathField
        );
        
        Button selectPathButton = new Button("选择路径");
        selectPathButton.setOnAction(e -> selectSavePath(recommendSavePathField));
        pathBox.getChildren().add(selectPathButton);
        
        // 控制按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        recommendStartButton = new Button("开始相关推荐");
        recommendStopButton = new Button("停止");
        recommendStopButton.setDisable(true);
        buttonBox.getChildren().addAll(recommendStartButton, recommendStopButton);
        
        content.getChildren().addAll(pidBox, depthBox, roundBox, pathBox, buttonBox);
        
        TitledPane pane = new TitledPane("相关推荐爬取", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private TitledPane createRankingPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 保存路径
        HBox pathBox = new HBox(10);
        rankingSavePathField = new TextField();
        rankingSavePathField.setPromptText("选择日榜图片保存路径");
        rankingSavePathField.setEditable(false);
        rankingSavePathField.getStyleClass().add("path-field");
        pathBox.getChildren().addAll(
            new Label("保存路径:"),
            rankingSavePathField
        );
        
        Button selectPathButton = new Button("选择路径");
        selectPathButton.setOnAction(e -> selectSavePath(rankingSavePathField));
        pathBox.getChildren().add(selectPathButton);
        
        // 控制按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        rankingStartButton = new Button("开始日榜爬取");
        rankingStopButton = new Button("停止");
        rankingStopButton.setDisable(true);
        buttonBox.getChildren().addAll(rankingStartButton, rankingStopButton);
        
        content.getChildren().addAll(pathBox, buttonBox);
        
        TitledPane pane = new TitledPane("日榜爬取", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private TitledPane createPopularPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 标签
        HBox tagBox = new HBox(10);
        popularTagField = new TextField();
        popularTagField.setPromptText("输入要搜索的标签");
        popularTagField.getStyleClass().add("text-field");
        tagBox.getChildren().addAll(
            new Label("标签:"),
            popularTagField
        );
        
        // 保存路径
        HBox pathBox = new HBox(10);
        popularSavePathField = new TextField();
        popularSavePathField.setPromptText("选择热门图片保存路径");
        popularSavePathField.setEditable(false);
        popularSavePathField.getStyleClass().add("path-field");
        pathBox.getChildren().addAll(
            new Label("保存路径:"),
            popularSavePathField
        );
        
        Button selectPathButton = new Button("选择路径");
        selectPathButton.setOnAction(e -> selectSavePath(popularSavePathField));
        pathBox.getChildren().add(selectPathButton);
        
        // 控制按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        popularStartButton = new Button("开始热门爬取");
        popularStopButton = new Button("停止");
        popularStopButton.setDisable(true);
        buttonBox.getChildren().addAll(popularStartButton, popularStopButton);
        
        content.getChildren().addAll(tagBox, pathBox, buttonBox);
        
        TitledPane pane = new TitledPane("热门作品爬取", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }
    
    private TitledPane createArtistPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 画师ID
        HBox idBox = new HBox(10);
        artistIdField = new TextField();
        artistIdField.setPromptText("输入画师ID");
        artistIdField.getStyleClass().add("text-field");
        idBox.getChildren().addAll(
            new Label("画师ID:"),
            artistIdField
        );
        
        // 保存路径
        HBox pathBox = new HBox(10);
        artistSavePathField = new TextField();
        artistSavePathField.setPromptText("选择画师作品保存路径");
        artistSavePathField.setEditable(false);
        artistSavePathField.getStyleClass().add("path-field");
        pathBox.getChildren().addAll(
            new Label("保存路径:"),
            artistSavePathField
        );
        
        Button selectPathButton = new Button("选择路径");
        selectPathButton.setOnAction(e -> selectSavePath(artistSavePathField));
        pathBox.getChildren().add(selectPathButton);
        
        // 控制按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        artistStartButton = new Button("开始画师爬取");
        artistStopButton = new Button("停止");
        artistStopButton.setDisable(true);
        buttonBox.getChildren().addAll(artistStartButton, artistStopButton);
        
        content.getChildren().addAll(idBox, pathBox, buttonBox);
        
        TitledPane pane = new TitledPane("画师作品爬取", content);
        // 设置TitledPane的样式，使用CSS类
        pane.getStyleClass().add("custom-titled-pane");
        
        return pane;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(15);
        rightPanel.setPrefWidth(650);
        rightPanel.getStyleClass().add("transparent-panel");

        // 日志区域
        Label logLabel = new Label("运行日志:");
        logLabel.getStyleClass().add("log-label");

        logArea = new TextArea();
        logArea.setPrefRowCount(35);
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFont(Font.font("Consolas", 11));
        logArea.setPrefHeight(600);
        logArea.getStyleClass().add("log-area");

        // 滚动面板
        ScrollPane scrollPane = new ScrollPane(logArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("log-scroll");

        // 让日志区域占据所有空间
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rightPanel.getChildren().addAll(
            logLabel,
            scrollPane
        );

        // 设置标签不占用额外空间
        VBox.setVgrow(logLabel, Priority.NEVER);

        return rightPanel;
    }
    
    private void initializeValues() {
        // 相关推荐默认值
        startPidField.setText(GlobalConfig.ARTWORK_START_PID);
        maxDepthField.setText(String.valueOf(GlobalConfig.MAX_DEPTH));
        imagesPerRoundField.setText(String.valueOf(GlobalConfig.RECOMMEND_START_IMAGES_PER_ROUND));
        recommendSavePathField.setText(GlobalConfig.RECOMMENDATIONS_BASE_PATH);
        
        // 日榜默认值
        rankingSavePathField.setText(GlobalConfig.RANKING_BASE_PATH);
        
        // 热门作品默认值
        popularTagField.setText("スズラン(アークナイツ)");
        popularSavePathField.setText(GlobalConfig.POPULAR_BASE_PATH);
        
        // 画师作品默认值
        artistIdField.setText(GlobalConfig.ARTIST_START_ID);
        artistSavePathField.setText(GlobalConfig.ARTIST_BASE_PATH);
    }
    
    private void setupEventHandlers() {
        // 相关推荐按钮事件
        recommendStartButton.setOnAction(e -> startRecommendCrawling());
        recommendStopButton.setOnAction(e -> stopCrawling());
        
        // 日榜按钮事件
        rankingStartButton.setOnAction(e -> startRankingCrawling());
        rankingStopButton.setOnAction(e -> stopCrawling());
        
        // 热门作品按钮事件
        popularStartButton.setOnAction(e -> startPopularCrawling());
        popularStopButton.setOnAction(e -> stopCrawling());
        
        // 画师作品按钮事件
        artistStartButton.setOnAction(e -> startArtistCrawling());
        artistStopButton.setOnAction(e -> stopCrawling());
    }
    
    private void selectSavePath(TextField pathField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择保存路径");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            pathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    private void startRecommendCrawling() {
        if (isRunning) {
            showAlert("爬虫正在运行中");
            return;
        }
        
        if (!validateRecommendInput()) {
            return;
        }
        
        setRunningState(true, "recommend");
        logArea.clear();
        
        executorService = Executors.newSingleThreadExecutor();
        
        executorService.submit(() -> {
            try {
                crawler = new PixivCrawler();
                
                Platform.runLater(() -> {
                    logMessage("开始执行相关推荐算法...");
                });
                
                crawler.downloadRecommendImages(
                    JsonUtil.getImageInfoById(startPidField.getText()),
                    recommendSavePathField.getText()
                );
                
                Platform.runLater(() -> {
                    logMessage("相关推荐爬取完成！");
                    setRunningState(false, "recommend");
                });
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    logMessage("相关推荐爬取错误: " + ex.getMessage());
                    setRunningState(false, "recommend");
                    showAlert("相关推荐爬取过程中出现错误: " + ex.getMessage());
                });
            }
        });
    }
    
    private void startRankingCrawling() {
        if (isRunning) {
            showAlert("爬虫正在运行中");
            return;
        }
        
        if (!validateRankingInput()) {
            return;
        }
        
        setRunningState(true, "ranking");
        logArea.clear();
        
        executorService = Executors.newSingleThreadExecutor();
        
        executorService.submit(() -> {
            try {
                crawler = new PixivCrawler();
                
                Platform.runLater(() -> {
                    logMessage("开始执行日榜爬取...");
                });
                
                crawler.fetchRankingImages();
                
                Platform.runLater(() -> {
                    logMessage("日榜爬取完成！");
                    setRunningState(false, "ranking");
                });
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    logMessage("日榜爬取错误: " + ex.getMessage());
                    setRunningState(false, "ranking");
                    showAlert("日榜爬取过程中出现错误: " + ex.getMessage());
                });
            }
        });
    }
    
    private void startPopularCrawling() {
        if (isRunning) {
            showAlert("爬虫正在运行中");
            return;
        }
        
        if (!validatePopularInput()) {
            return;
        }
        
        setRunningState(true, "popular");
        logArea.clear();
        
        executorService = Executors.newSingleThreadExecutor();
        
        executorService.submit(() -> {
            try {
                crawler = new PixivCrawler();
                downloader = new Downloader();
                PopularImageServiceImpl popularImageServiceImpl = new PopularImageServiceImpl();
                
                Platform.runLater(() -> {
                    logMessage("开始执行热门作品爬取...");
                });
                
                String tag = popularTagField.getText();
                List<PixivImage> popularImages = popularImageServiceImpl.getPopularImagesByTag(tag);
                
                String savePath = popularSavePathField.getText() + "/" + tag;
                downloader.startDownload(popularImages, "热门作品", savePath);
                
                // 为每个热门作品爬取相关推荐
                for (int i = 0; i < popularImages.size(); i++) {
                    crawler.downloadRecommendImages(popularImages.get(i), savePath + "/recommend");
                }
                
                Platform.runLater(() -> {
                    logMessage("热门作品爬取完成！");
                    setRunningState(false, "popular");
                });
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    logMessage("热门作品爬取错误: " + ex.getMessage());
                    setRunningState(false, "popular");
                    showAlert("热门作品爬取过程中出现错误: " + ex.getMessage());
                });
            }
        });
    }
    
    private void startArtistCrawling() {
        if (isRunning) {
            showAlert("爬虫正在运行中");
            return;
        }
        
        if (!validateArtistInput()) {
            return;
        }
        
        setRunningState(true, "artist");
        logArea.clear();
        
        executorService = Executors.newSingleThreadExecutor();
        
        executorService.submit(() -> {
            try {
                crawler = new PixivCrawler();
                downloader = new Downloader();
                ArtistServiceImpl artistServiceImpl = new ArtistServiceImpl();
                
                Platform.runLater(() -> {
                    logMessage("开始执行画师作品爬取...");
                });
                
                String artistId = artistIdField.getText();
                List<PixivImage> pixivImages = artistServiceImpl.searchArtworksByArtistId(artistId, GlobalConfig.ARTIST_MAX_IMAGE);
                String artistName = artistServiceImpl.getArtistName(artistId);
                
                String savePath = artistSavePathField.getText() + "/" + artistName;
                downloader.startDownload(pixivImages, "画师作品", savePath);
                
                Platform.runLater(() -> {
                    logMessage("画师作品爬取完成！");
                    setRunningState(false, "artist");
                });
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    logMessage("画师作品爬取错误: " + ex.getMessage());
                    setRunningState(false, "artist");
                    showAlert("画师作品爬取过程中出现错误: " + ex.getMessage());
                });
            }
        });
    }
    
    private void stopCrawling() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        if (downloader != null) {
            downloader.stopDownload();
        }
        
        logMessage("正在停止爬虫...");
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        setRunningState(false, "all");
    }
    
    private boolean validateRecommendInput() {
        try {
            Integer.parseInt(startPidField.getText());
            Integer.parseInt(maxDepthField.getText());
            Integer.parseInt(imagesPerRoundField.getText());
            
            if (recommendSavePathField.getText().trim().isEmpty()) {
                showAlert("请选择相关推荐保存路径");
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            showAlert("请输入有效的数字");
            return false;
        }
    }
    
    private boolean validateRankingInput() {
        if (rankingSavePathField.getText().trim().isEmpty()) {
            showAlert("请选择日榜保存路径");
            return false;
        }
        return true;
    }
    
    private boolean validatePopularInput() {
        if (popularTagField.getText().trim().isEmpty()) {
            showAlert("请输入标签");
            return false;
        }
        if (popularSavePathField.getText().trim().isEmpty()) {
            showAlert("请选择热门作品保存路径");
            return false;
        }
        return true;
    }
    
    private boolean validateArtistInput() {
        if (artistIdField.getText().trim().isEmpty()) {
            showAlert("请输入画师ID");
            return false;
        }
        if (artistSavePathField.getText().trim().isEmpty()) {
            showAlert("请选择画师作品保存路径");
            return false;
        }
        return true;
    }
    
    private void setRunningState(boolean running, String type) {
        isRunning = running;
        
        if (running) {
            // 禁用所有开始按钮
            recommendStartButton.setDisable(true);
            rankingStartButton.setDisable(true);
            popularStartButton.setDisable(true);
            artistStartButton.setDisable(true);
            
            // 启用对应的停止按钮
            switch (type) {
                case "recommend":
                    recommendStopButton.setDisable(false);
                    break;
                case "ranking":
                    rankingStopButton.setDisable(false);
                    break;
                case "popular":
                    popularStopButton.setDisable(false);
                    break;
                case "artist":
                    artistStopButton.setDisable(false);
                    break;
            }
        } else {
            // 启用所有开始按钮
            recommendStartButton.setDisable(false);
            rankingStartButton.setDisable(false);
            popularStartButton.setDisable(false);
            artistStartButton.setDisable(false);
            
            // 禁用所有停止按钮
            recommendStopButton.setDisable(true);
            rankingStopButton.setDisable(true);
            popularStopButton.setDisable(true);
            artistStopButton.setDisable(true);
        }
    }
    
    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText("[" + java.time.LocalTime.now().toString() + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (downloader != null) {
            downloader.stopDownload();
        }
        if (logHandler != null) {
            logHandler.close();
        }
        Platform.exit();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
