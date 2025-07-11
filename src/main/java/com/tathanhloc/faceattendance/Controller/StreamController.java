package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Service.RTSPStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
    public Map<String, Object> testRTSP(@RequestBody Map<String, String> request) {
        String rtspUrl = request.get("rtspUrl");
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("Testing RTSP URL: {}", rtspUrl);

            // Test với ffprobe trước
            List<String> command = Arrays.asList(
                    "ffprobe", "-v", "quiet",
                    "-rtsp_transport", "tcp",
                    "-i", rtspUrl,
                    "-show_entries", "stream=codec_type,codec_name",
                    "-of", "csv=p=0"
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                result.put("success", false);
                result.put("message", "Connection timeout (15 seconds)");
                result.put("suggestion", "Check if RTSP URL is accessible from this server");
            } else if (process.exitValue() == 0) {
                result.put("success", true);
                result.put("message", "RTSP connection successful");
                result.put("streamInfo", output.toString().trim());
            } else {
                result.put("success", false);
                result.put("message", "RTSP connection failed");
                result.put("error", output.toString().trim());
                result.put("exitCode", process.exitValue());
            }

            // Additional network test
            try {
                URI uri = new URI(rtspUrl);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 554;

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 5000);
                    result.put("networkReachable", true);
                } catch (IOException e) {
                    result.put("networkReachable", false);
                    result.put("networkError", e.getMessage());
                }

            } catch (Exception e) {
                result.put("networkTest", "Failed to parse URL");
            }

        } catch (Exception e) {
            log.error("RTSP test failed", e);
            result.put("success", false);
            result.put("message", "Test failed: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "API is working");
        result.put("timestamp", new Date());

        // Test FFmpeg
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            result.put("ffmpeg", finished && process.exitValue() == 0 ? "Available" : "Not available");
        } catch (Exception e) {
            result.put("ffmpeg", "Error: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/start")
    public Map<String, Object> startStream(@RequestBody Map<String, String> request) {
        try {
            String rtspUrl = request.get("rtspUrl");
            if (rtspUrl == null || rtspUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("RTSP URL is required");
            }

            log.info("Starting stream for URL: {}", rtspUrl);

            String streamId = "camera_" + System.currentTimeMillis();
            String hlsUrl = streamService.startHLSStream(rtspUrl, streamId);

            Map<String, Object> response = new HashMap<>();
            response.put("streamId", streamId);
            response.put("hlsUrl", hlsUrl);
            response.put("status", "started");
            response.put("message", "Stream is starting, please wait 5-10 seconds");

            log.info("Stream started: {} -> {}", streamId, hlsUrl);
            return response;

        } catch (Exception e) {
            log.error("Failed to start stream", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @PostMapping("/stop/{streamId}")
    public Map<String, String> stopStream(@PathVariable String streamId) {
        streamService.stopStream(streamId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "stopped");
        response.put("streamId", streamId);
        return response;
    }

    // Debug endpoint để check file có tồn tại không
    @GetMapping("/check/{streamId}")
    public Map<String, Object> checkStream(@PathVariable String streamId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Path playlistPath = Paths.get("src/main/resources/static/streams", streamId, "playlist.m3u8");
            boolean exists = Files.exists(playlistPath);

            result.put("streamId", streamId);
            result.put("playlistExists", exists);
            result.put("playlistPath", playlistPath.toAbsolutePath().toString());

            if (exists) {
                result.put("fileSize", Files.size(playlistPath));
                result.put("lastModified", Files.getLastModifiedTime(playlistPath).toString());
            }

            // List all files in stream directory
            Path streamDir = Paths.get("src/main/resources/static/streams", streamId);
            if (Files.exists(streamDir)) {
                List<String> files = Files.list(streamDir)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                result.put("files", files);
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}