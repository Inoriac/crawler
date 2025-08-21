package com.pixiv.crawler.gui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * GUI日志处理器，将日志输出重定向到GUI界面
 */
public class GUILogHandler extends Handler {
    
    private TextArea logArea;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    public GUILogHandler(TextArea logArea) {
        this.logArea = logArea;
        this.outputStream = new ByteArrayOutputStream();
        this.originalOut = System.out;
        this.originalErr = System.err;
        
        // 设置日志级别
        setLevel(Level.ALL);
        
        // 重定向System.out和System.err
        redirectSystemStreams();
    }
    
    private void redirectSystemStreams(){
        // 创建自定义的PrintStream，将输出重定向到GUI
        PrintStream guiOut = new PrintStream(outputStream, true, StandardCharsets.UTF_8) {
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                flushToGUI();
            }
            
            @Override
            public void write(int b) {
                super.write(b);
                flushToGUI();
            }
            
            @Override
            public void write(byte[] b) throws IOException {
                super.write(b);
                flushToGUI();
            }
        };
        
        // 重定向标准输出和错误输出
        System.setOut(guiOut);
        System.setErr(guiOut);
    }
    
    private void flushToGUI() {
        try {
            String output = outputStream.toString(StandardCharsets.UTF_8.name());
            if (!output.isEmpty()) {
                Platform.runLater(() -> {
                    logArea.appendText(output);
                    logArea.setScrollTop(Double.MAX_VALUE);
                });
                outputStream.reset();
            }
        } catch (Exception e) {
            // 如果出现编码问题，使用默认编码
            try {
                String output = outputStream.toString();
                if (!output.isEmpty()) {
                    Platform.runLater(() -> {
                        logArea.appendText(output);
                        logArea.setScrollTop(Double.MAX_VALUE);
                    });
                    outputStream.reset();
                }
            } catch (Exception ex) {
                // 忽略错误，避免无限循环
            }
        }
    }
    
    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            String message = getFormatter().format(record);
            Platform.runLater(() -> {
                logArea.appendText(message);
                logArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }
    
    @Override
    public void flush() {
        flushToGUI();
    }
    
    @Override
    public void close() throws SecurityException {
        // 恢复原始输出流
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * 手动添加日志消息
     */
    public void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText("[" + java.time.LocalTime.now().toString() + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
