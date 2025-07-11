package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Service.RTSPStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private static final Logger log = LoggerFactory.getLogger(StreamController.class);

    @Autowired
    private RTSPStreamService streamService;

    @PostMapping("/test-rtsp")
    public ResponseEntity<Map<String, Object>> testRTSP(@RequestBody Map<String, String> request) {
        String rtspUrl = request.get("rtspUrl");
        Map<String, Object> result = new HashMap<>();

        if (rtspUrl == null || rtspUrl.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "RTSP URL is required");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            log.info("Testing RTSP URL: {}", rtspUrl);

            // Test 1: Basic URL validation
            if (!isValidRTSPUrl(rtspUrl)) {
                result.put("success", false);
                result.put("message", "Invalid RTSP URL format");
                result.put("suggestion", "URL should start with rtsp:// and include host");
                return ResponseEntity.ok(result);
            }

            // Test 2: Network connectivity
            Map<String, Object> networkTest = testNetworkConnectivity(rtspUrl);
            result.putAll(networkTest);

            if (!(Boolean) networkTest.get("networkReachable")) {
                result.put("success", false);
                result.put("message", "Cannot reach RTSP server");
                return ResponseEntity.ok(result);
            }

            // Test 3: RTSP stream test với multiple methods
            Map<String, Object> streamTest = testRTSPStream(rtspUrl);
            result.putAll(streamTest);

        } catch (Exception e) {
            log.error("RTSP test failed for URL: {}", rtspUrl, e);
            result.put("success", false);
            result.put("message", "Test failed: " + e.getMessage());
            result.put("suggestion", "Check if the camera is online and RTSP credentials are correct");
        }

        return ResponseEntity.ok(result);
    }

    private boolean isValidRTSPUrl(String rtspUrl) {
        try {
            if (!rtspUrl.toLowerCase().startsWith("rtsp://")) {
                return false;
            }
            URI uri = new URI(rtspUrl);
            return uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> testNetworkConnectivity(String rtspUrl) {
        Map<String, Object> result = new HashMap<>();

        try {
            URI uri = new URI(rtspUrl);
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 554;

            log.info("Testing network connectivity to {}:{}", host, port);

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 8000);
                result.put("networkReachable", true);
                result.put("networkMessage", "Successfully connected to " + host + ":" + port);
                log.info("Network connectivity test passed for {}:{}", host, port);
            }

        } catch (Exception e) {
            result.put("networkReachable", false);
            result.put("networkError", e.getMessage());
            result.put("networkMessage", "Cannot connect to RTSP server");
            log.warn("Network connectivity test failed: {}", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> testRTSPStream(String rtspUrl) {
        Map<String, Object> result = new HashMap<>();

        // Test với các transport methods khác nhau
        String[] transports = {"tcp", "udp", null}; // null = auto

        for (String transport : transports) {
            try {
                log.info("Testing RTSP stream with transport: {}", transport != null ? transport : "auto");

                if (testFFprobe(rtspUrl, transport)) {
                    result.put("success", true);
                    result.put("message", "RTSP stream is accessible");
                    result.put("transport", transport != null ? transport : "auto");
                    result.put("suggestion", "Stream should work fine");
                    return result;
                }
            } catch (Exception e) {
                log.warn("RTSP test failed with transport {}: {}", transport, e.getMessage());
            }
        }

        // Nếu tất cả đều fail
        result.put("success", false);
        result.put("message", "RTSP stream is not accessible");
        result.put("suggestion", "Check camera credentials, network settings, or try different RTSP URL format");

        return result;
    }

    private boolean testFFprobe(String rtspUrl, String transport) {
        try {
            List<String> command = new ArrayList<>();
            command.addAll(Arrays.asList("ffprobe", "-v", "quiet"));

            if (transport != null) {
                command.addAll(Arrays.asList("-rtsp_transport", transport));
            }

            command.addAll(Arrays.asList(
                    "-i", rtspUrl,
                    "-show_entries", "stream=codec_type,codec_name",
                    "-of", "csv=p=0"
            ));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.warn("FFprobe test failed: {}", e.getMessage());
            return false;
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "API is working");
        result.put("timestamp", new Date());

        // Test FFmpeg availability
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                result.put("ffmpeg", "Available");

                // Get FFmpeg version
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String firstLine = reader.readLine();
                    if (firstLine != null) {
                        result.put("ffmpegVersion", firstLine);
                    }
                }
            } else {
                result.put("ffmpeg", "Not available or timeout");
            }
        } catch (Exception e) {
            result.put("ffmpeg", "Error: " + e.getMessage());
        }

        // Test service availability
        if (streamService != null) {
            result.put("streamService", "Available");
            result.putAll(streamService.getStreamStats());
        } else {
            result.put("streamService", "Not available");
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStream(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String rtspUrl = request.get("rtspUrl");
            if (rtspUrl == null || rtspUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "RTSP URL is required");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Starting stream for URL: {}", rtspUrl);

            // Validate URL format
            if (!isValidRTSPUrl(rtspUrl)) {
                response.put("status", "error");
                response.put("message", "Invalid RTSP URL format");
                return ResponseEntity.badRequest().body(response);
            }

            String streamId = "camera_" + System.currentTimeMillis();

            try {
                String hlsUrl = streamService.startHLSStream(rtspUrl, streamId);

                response.put("streamId", streamId);
                response.put("hlsUrl", hlsUrl);
                response.put("status", "started");
                response.put("message", "Stream is starting, please wait 10-15 seconds for HLS segments");

                log.info("Stream started successfully: {} -> {}", streamId, hlsUrl);

            } catch (RuntimeException e) {
                log.error("Failed to start stream for URL: {}", rtspUrl, e);

                response.put("status", "error");
                response.put("message", "Cannot start stream: " + e.getMessage());

                // Provide helpful suggestions based on error
                if (e.getMessage().contains("Cannot connect")) {
                    response.put("suggestion", "Check if camera is online and network is accessible");
                } else if (e.getMessage().contains("FFmpeg")) {
                    response.put("suggestion", "FFmpeg may not be installed or accessible");
                } else {
                    response.put("suggestion", "Try testing the RTSP URL first");
                }

                return ResponseEntity.ok(response); // Return 200 but with error status
            }

        } catch (Exception e) {
            log.error("Unexpected error starting stream", e);
            response.put("status", "error");
            response.put("message", "Unexpected error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop/{streamId}")
    public ResponseEntity<Map<String, String>> stopStream(@PathVariable String streamId) {
        try {
            log.info("Stopping stream: {}", streamId);
            streamService.stopStream(streamId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "stopped");
            response.put("streamId", streamId);
            response.put("message", "Stream stopped successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error stopping stream: {}", streamId, e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("streamId", streamId);
            response.put("message", "Error stopping stream: " + e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/check/{streamId}")
    public ResponseEntity<Map<String, Object>> checkStream(@PathVariable String streamId) {
        Map<String, Object> result = new HashMap<>();

        try {
            result.put("streamId", streamId);

            // Check both possible locations
            Path targetPath = Paths.get("target/classes/static/streams", streamId, "playlist.m3u8");
            Path srcPath = Paths.get("src/main/resources/static/streams", streamId, "playlist.m3u8");

            Path playlistPath = null;

            if (Files.exists(targetPath)) {
                playlistPath = targetPath;
                result.put("location", "target");
            } else if (Files.exists(srcPath)) {
                playlistPath = srcPath;
                result.put("location", "src");
            }

            if (playlistPath != null) {
                result.put("playlistExists", true);
                result.put("playlistPath", playlistPath.toAbsolutePath().toString());
                result.put("fileSize", Files.size(playlistPath));
                result.put("lastModified", Files.getLastModifiedTime(playlistPath).toString());

                // List all files in stream directory
                Path streamDir = playlistPath.getParent();
                if (Files.exists(streamDir)) {
                    List<String> files = Files.list(streamDir)
                            .map(path -> path.getFileName().toString())
                            .sorted()
                            .collect(Collectors.toList());
                    result.put("files", files);
                    result.put("fileCount", files.size());
                }

                // Check playlist content
                try {
                    List<String> lines = Files.readAllLines(playlistPath);
                    result.put("playlistLines", lines.size());
                    result.put("hasSegments", lines.stream().anyMatch(line -> line.endsWith(".ts")));
                } catch (Exception e) {
                    result.put("playlistReadError", e.getMessage());
                }

            } else {
                result.put("playlistExists", false);
                result.put("message", "Playlist not found in either target or src directories");
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("Error checking stream {}: {}", streamId, e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = streamService.getStreamStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting stream stats", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}