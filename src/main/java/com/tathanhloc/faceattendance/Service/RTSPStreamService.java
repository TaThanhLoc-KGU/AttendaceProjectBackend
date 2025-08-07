package com.tathanhloc.faceattendance.Service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RTSPStreamService {

    private static final Logger log = LoggerFactory.getLogger(RTSPStreamService.class);
    private final Map<String, Process> activeStreams = new ConcurrentHashMap<>();

    public String startHLSStream(String rtspUrl, String streamId) {
        try {
            // Tạo thư mục output - thử cả 2 đường dẫn
            Path outputDir = createOutputDirectory(streamId);

            log.info("Creating HLS stream directory: {}", outputDir.toAbsolutePath());

            // Test kết nối với nhiều phương thức
            if (!testRTSPConnectionMultiple(rtspUrl)) {
                throw new RuntimeException("Cannot connect to RTSP stream after multiple attempts: " + rtspUrl);
            }

            // Thử nhiều cấu hình FFmpeg khác nhau
            Process process = startFFmpegProcess(rtspUrl, outputDir);

            if (process == null) {
                throw new RuntimeException("Failed to start FFmpeg process for: " + rtspUrl);
            }

            activeStreams.put(streamId, process);
            return "/streams/" + streamId + "/playlist.m3u8";

        } catch (Exception e) {
            log.error("Failed to start stream for {}: {}", rtspUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to start stream: " + e.getMessage(), e);
        }
    }

    private Path createOutputDirectory(String streamId) throws IOException {
        // Thử tạo trong target/classes/static trước
        Path targetDir = Paths.get("target/classes/static/streams", streamId);
        try {
            Files.createDirectories(targetDir);
            log.info("Created directory in target: {}", targetDir.toAbsolutePath());
            return targetDir;
        } catch (Exception e) {
            log.warn("Failed to create directory in target, trying src: {}", e.getMessage());
        }

        // Fallback về src/main/resources/static
        Path srcDir = Paths.get("src/main/resources/static/streams", streamId);
        Files.createDirectories(srcDir);
        log.info("Created directory in src: {}", srcDir.toAbsolutePath());
        return srcDir;
    }

    private boolean testRTSPConnectionMultiple(String rtspUrl) {
        log.info("Testing RTSP connection to: {}", rtspUrl);

        // THÊM RETRY LOGIC - thử 3 lần
        for (int attempt = 1; attempt <= 3; attempt++) {
            log.info("RTSP connection attempt {}/3", attempt);

            // Test 1: Network connectivity
            if (!testNetworkConnection(rtspUrl)) {
                log.warn("Network connection test failed, attempt {}", attempt);
                if (attempt < 3) {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    continue;
                }
                return false;
            }

            // Test 2: FFprobe với TCP transport
            if (testWithFFprobe(rtspUrl, "tcp")) {
                log.info("RTSP connection test passed with TCP transport, attempt {}", attempt);
                return true;
            }

            // Test 3: FFprobe với UDP transport
            if (testWithFFprobe(rtspUrl, "udp")) {
                log.info("RTSP connection test passed with UDP transport, attempt {}", attempt);
                return true;
            }

            // Test 4: FFprobe mà không chỉ định transport
            if (testWithFFprobe(rtspUrl, null)) {
                log.info("RTSP connection test passed without specific transport, attempt {}", attempt);
                return true;
            }

            if (attempt < 3) {
                log.warn("All tests failed for attempt {}, retrying in 5 seconds...", attempt);
                try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        log.error("All RTSP connection tests failed after 3 attempts for: {}", rtspUrl);
        return false;
    }

    private boolean testNetworkConnection(String rtspUrl) {
        try {
            URI uri = new URI(rtspUrl);
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 554;

            // TĂNG timeout từ 5s lên 15s
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 15000); // Tăng từ 5000 -> 15000
                log.info("Network connection successful to {}:{}", host, port);
                return true;
            }
        } catch (Exception e) {
            log.warn("Network connection failed: {}", e.getMessage());
            return false;
        }
    }


    private boolean testWithFFprobe(String rtspUrl, String transport) {
        try {
            List<String> command;

            if (transport != null) {
                command = Arrays.asList(
                        "ffprobe", "-v", "quiet",
                        "-rtsp_transport", transport,
                        "-timeout", "30000000",  // THÊM timeout parameter
                        "-i", rtspUrl,
                        "-show_entries", "format=duration",
                        "-of", "csv=p=0"
                );
            } else {
                command = Arrays.asList(
                        "ffprobe", "-v", "quiet",
                        "-timeout", "30000000",  // THÊM timeout parameter
                        "-i", rtspUrl,
                        "-show_entries", "format=duration",
                        "-of", "csv=p=0"
                );
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // TĂNG wait time từ 15s lên 30s
            boolean finished = process.waitFor(30, TimeUnit.SECONDS); // Tăng từ 15 -> 30

            if (!finished) {
                process.destroyForcibly();
                log.warn("FFprobe timeout with transport: {}", transport);
                return false;
            }

            boolean success = process.exitValue() == 0;
            log.info("FFprobe test with transport {} result: {}", transport, success);
            return success;

        } catch (Exception e) {
            log.warn("FFprobe test failed with transport {}: {}", transport, e.getMessage());
            return false;
        }
    }

    private Process startFFmpegProcess(String rtspUrl, Path outputDir) {
        // Thử các cấu hình FFmpeg khác nhau
        String[] configs = {"tcp_optimized", "udp_fallback", "basic", "compatible"};

        for (String config : configs) {
            try {
                log.info("Trying FFmpeg configuration: {}", config);
                Process process = createFFmpegProcess(rtspUrl, outputDir, config);

                if (process != null && process.isAlive()) {
                    log.info("FFmpeg started successfully with config: {}", config);

                    // Start output logging thread
                    startOutputLogging(process, config);

                    // Wait a bit to ensure it doesn't fail immediately
                    Thread.sleep(2000);

                    if (process.isAlive()) {
                        return process;
                    } else {
                        log.warn("FFmpeg process died immediately with config: {}", config);
                    }
                }
            } catch (Exception e) {
                log.warn("FFmpeg config {} failed: {}", config, e.getMessage());
            }
        }

        return null;
    }

    private Process createFFmpegProcess(String rtspUrl, Path outputDir, String config) throws IOException {
        List<String> command;

        switch (config) {
            case "tcp_optimized":
                command = Arrays.asList(
                        "ffmpeg", "-y",
                        "-rtsp_transport", "tcp",
                        "-rtsp_flags", "prefer_tcp",
                        "-timeout", "30000000",        // TĂNG từ 20s -> 30s
                        "-reconnect", "1",
                        "-reconnect_streamed", "1",
                        "-reconnect_delay_max", "10",  // TĂNG từ 5s -> 10s
                        "-analyzeduration", "10000000", // THÊM analyze duration (10s)
                        "-probesize", "10000000",       // THÊM probe size (10MB)
                        "-fflags", "+genpts",           // THÊM generate PTS
                        "-avoid_negative_ts", "make_zero", // THÊM để tránh negative timestamp
                        "-i", rtspUrl,

                        // Video encoding - optimized for compatibility
                        "-c:v", "libx264",
                        "-preset", "ultrafast",
                        "-tune", "zerolatency",
                        "-profile:v", "baseline",
                        "-level", "3.1",
                        "-crf", "28",
                        "-maxrate", "2M",
                        "-bufsize", "4M",
                        "-g", "30",
                        "-r", "15",

                        // No audio
                        "-an",

                        // HLS options
                        "-f", "hls",
                        "-hls_time", "2",
                        "-hls_list_size", "5",
                        "-hls_flags", "delete_segments+independent_segments",
                        "-hls_segment_type", "mpegts",
                        "-hls_allow_cache", "0",
                        outputDir.resolve("playlist.m3u8").toString()
                );
                break;

            case "udp_fallback":
                command = Arrays.asList(
                        "ffmpeg", "-y",
                        "-rtsp_transport", "udp",
                        "-timeout", "20000000",         // TĂNG timeout
                        "-analyzeduration", "5000000",  // THÊM analyze duration
                        "-probesize", "5000000",        // THÊM probe size
                        "-i", rtspUrl,

                        "-c:v", "libx264",
                        "-preset", "faster",
                        "-profile:v", "baseline",
                        "-crf", "30",
                        "-maxrate", "1M",
                        "-bufsize", "2M",
                        "-g", "60",
                        "-r", "10",

                        "-an",

                        "-f", "hls",
                        "-hls_time", "4",
                        "-hls_list_size", "3",
                        "-hls_flags", "delete_segments",
                        outputDir.resolve("playlist.m3u8").toString()
                );
                break;

            case "basic":
                command = Arrays.asList(
                        "ffmpeg", "-y",
                        "-timeout", "15000000",         // THÊM timeout
                        "-i", rtspUrl,

                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-crf", "32",
                        "-maxrate", "800k",
                        "-bufsize", "1600k",
                        "-g", "50",

                        "-an",

                        "-f", "hls",
                        "-hls_time", "6",
                        "-hls_list_size", "3",
                        outputDir.resolve("playlist.m3u8").toString()
                );
                break;

            case "compatible":
                command = Arrays.asList(
                        "ffmpeg", "-y",
                        "-re",                          // THÊM real-time
                        "-timeout", "10000000",         // THÊM timeout
                        "-i", rtspUrl,

                        "-vcodec", "copy",
                        "-an",

                        "-f", "hls",
                        "-hls_time", "10",
                        "-hls_list_size", "2",
                        outputDir.resolve("playlist.m3u8").toString()
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown config: " + config);
        }

        log.info("Starting FFmpeg with command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    private void startOutputLogging(Process process, String config) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg [{}]: {}", config, line);

                    // Log important messages at higher level
                    if (line.contains("error") || line.contains("Error") || line.contains("ERROR")) {
                        log.warn("FFmpeg [{}] ERROR: {}", config, line);
                    } else if (line.contains("Opening") || line.contains("Stream") || line.contains("muxer")) {
                        log.info("FFmpeg [{}]: {}", config, line);
                    }
                }
            } catch (IOException e) {
                log.debug("FFmpeg output logging ended for config {}: {}", config, e.getMessage());
            }
        }, "FFmpeg-Logger-" + config).start();
    }

    public void stopStream(String streamId) {
        Process process = activeStreams.remove(streamId);
        if (process != null) {
            log.info("Stopping stream: {}", streamId);

            // Graceful shutdown first
            process.destroy();

            try {
                boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
                if (!terminated) {
                    log.warn("Force killing stream process: {}", streamId);
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while stopping stream: {}", streamId);
                process.destroyForcibly();
            }

            log.info("Stream stopped: {}", streamId);
        }

        // Clean up files
        cleanupStreamFiles(streamId);
    }

    private void cleanupStreamFiles(String streamId) {
        try {
            // Try target directory first
            Path targetDir = Paths.get("target/classes/static/streams", streamId);
            if (Files.exists(targetDir)) {
                deleteDirectory(targetDir);
                log.info("Cleaned up target directory: {}", targetDir);
                return;
            }

            // Try src directory
            Path srcDir = Paths.get("src/main/resources/static/streams", streamId);
            if (Files.exists(srcDir)) {
                deleteDirectory(srcDir);
                log.info("Cleaned up src directory: {}", srcDir);
            }
        } catch (IOException e) {
            log.warn("Failed to clean up stream directory for {}: {}", streamId, e.getMessage());
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    // Utility method to check FFmpeg availability
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.error("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    // Get stream statistics
    public Map<String, Object> getStreamStats() {
        return Map.of(
                "activeStreams", activeStreams.size(),
                "streamIds", activeStreams.keySet(),
                "ffmpegAvailable", isFFmpegAvailable()
        );
    }
    public String startHLSStreamForced(String rtspUrl, String streamId) {
        try {
            // Tạo thư mục output
            Path outputDir = createOutputDirectory(streamId);
            log.info("Creating HLS stream directory (FORCED): {}", outputDir.toAbsolutePath());

            // SKIP connection tests in force mode
            log.warn("FORCE MODE: Skipping RTSP connection tests for: {}", rtspUrl);

            // Thử FFmpeg process trực tiếp
            Process process = startFFmpegProcess(rtspUrl, outputDir);

            if (process == null) {
                throw new RuntimeException("Failed to start FFmpeg process for: " + rtspUrl);
            }

            activeStreams.put(streamId, process);
            return "/streams/" + streamId + "/playlist.m3u8";

        } catch (Exception e) {
            log.error("Failed to start FORCED stream for {}: {}", rtspUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to start forced stream: " + e.getMessage(), e);
        }
    }
    public boolean isStreamActive(String streamId) {
        Process process = activeStreams.get(streamId);
        return process != null && process.isAlive();
    }

    public Map<String, Object> getDetailedStreamInfo(String streamId) {
        Map<String, Object> info = new HashMap<>();

        Process process = activeStreams.get(streamId);
        info.put("processExists", process != null);
        info.put("processAlive", process != null && process.isAlive());

        if (process != null) {
            info.put("processInfo", process.info().toString());
        }

        return info;
    }
    // Enhanced logging for FFmpeg process
    private Process createFFmpegProcessWithEnhancedLogging(String rtspUrl, Path outputDir, String config) throws IOException {
        List<String> command = buildFFmpegCommand(rtspUrl, outputDir, config);

        log.info("=== FFmpeg Debug Info ===");
        log.info("Config: {}", config);
        log.info("RTSP URL: {}", rtspUrl);
        log.info("Output Dir: {}", outputDir.toAbsolutePath());
        log.info("Full Command: {}", String.join(" ", command));
        log.info("Working Directory: {}", System.getProperty("user.dir"));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        // Log environment
        log.info("PATH: {}", pb.environment().get("PATH"));

        try {
            Process process = pb.start();
            log.info("FFmpeg process started with PID: {}", process.pid());

            // Start enhanced logging
            startEnhancedOutputLogging(process, config, outputDir);

            return process;
        } catch (Exception e) {
            log.error("Failed to start FFmpeg process: {}", e.getMessage());
            throw e;
        }
    }

    private void startEnhancedOutputLogging(Process process, String config, Path outputDir) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;

                    // Log more details for debugging
                    if (lineCount < 50 || line.contains("error") || line.contains("Error") ||
                            line.contains("Opening") || line.contains("Stream") ||
                            line.contains("muxer") || line.contains("segment")) {
                        log.info("FFmpeg [{}] L{}: {}", config, lineCount, line);
                    }

                    // Check for specific errors
                    if (line.contains("Connection refused") || line.contains("Connection timed out")) {
                        log.error("FFmpeg [{}] CONNECTION ERROR: {}", config, line);
                    } else if (line.contains("Invalid data") || line.contains("Protocol not found")) {
                        log.error("FFmpeg [{}] PROTOCOL ERROR: {}", config, line);
                    } else if (line.contains("No such file") || line.contains("Permission denied")) {
                        log.error("FFmpeg [{}] FILE ERROR: {}", config, line);
                    }

                    // Log when segments are created
                    if (line.contains(".ts") && line.contains("Opening")) {
                        log.info("FFmpeg [{}] SEGMENT CREATED: {}", config, line);
                    }
                }

                log.info("FFmpeg [{}] process output ended after {} lines", config, lineCount);

            } catch (IOException e) {
                log.debug("FFmpeg output logging ended for config {}: {}", config, e.getMessage());
            }
        }, "FFmpeg-Enhanced-Logger-" + config).start();
    }
    private List<String> buildFFmpegCommand(String rtspUrl, Path outputDir, String config) {
        // Extract command building logic to separate method for better debugging
        switch (config) {
            case "tcp_optimized":
                return Arrays.asList(
                        "ffmpeg", "-y",
                        "-rtsp_transport", "tcp",
                        "-rtsp_flags", "prefer_tcp",
                        "-timeout", "30000000",
                        "-reconnect", "1",
                        "-reconnect_streamed", "1",
                        "-reconnect_delay_max", "10",
                        "-analyzeduration", "10000000",
                        "-probesize", "10000000",
                        "-fflags", "+genpts",
                        "-avoid_negative_ts", "make_zero",
                        "-i", rtspUrl,
                        "-c:v", "libx264",
                        "-preset", "ultrafast",
                        "-tune", "zerolatency",
                        "-profile:v", "baseline",
                        "-level", "3.1",
                        "-crf", "28",
                        "-maxrate", "2M",
                        "-bufsize", "4M",
                        "-g", "30",
                        "-r", "15",
                        "-an",
                        "-f", "hls",
                        "-hls_time", "2",
                        "-hls_list_size", "5",
                        "-hls_flags", "delete_segments+independent_segments",
                        "-hls_segment_type", "mpegts",
                        "-hls_allow_cache", "0",
                        outputDir.resolve("playlist.m3u8").toString()
                );
            // ... other configs
            default:
                throw new IllegalArgumentException("Unknown config: " + config);
        }
    }
}