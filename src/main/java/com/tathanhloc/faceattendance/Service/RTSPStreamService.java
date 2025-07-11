package com.tathanhloc.faceattendance.Service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RTSPStreamService {

    private static final Logger log = LoggerFactory.getLogger(RTSPStreamService.class);
    private final Map<String, Process> activeStreams = new ConcurrentHashMap<>();

    public String startHLSStream(String rtspUrl, String streamId) {
        try {
            // Tạo thư mục trong target/classes/static để Spring Boot có thể serve
            Path outputDir = Paths.get("target/classes/static/streams", streamId);
            Files.createDirectories(outputDir);

            log.info("Creating HLS stream directory: {}", outputDir.toAbsolutePath());

            // Test RTSP connection trước
            if (!testRTSPConnection(rtspUrl)) {
                throw new RuntimeException("Cannot connect to RTSP stream: " + rtspUrl);
            }

            // FFmpeg command với HEVC -> H.264 conversion
            List<String> command = Arrays.asList(
                    "ffmpeg", "-y",
                    "-rtsp_transport", "tcp",
                    "-rtsp_flags", "prefer_tcp",
                    "-timeout", "20000000",
                    "-i", rtspUrl,

                    // Convert HEVC -> H.264 cho web compatibility
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

                    // No audio (camera không có audio)
                    "-an",

                    // HLS options
                    "-f", "hls",
                    "-hls_time", "2",
                    "-hls_list_size", "5",
                    "-hls_flags", "delete_segments+independent_segments",
                    "-hls_segment_type", "mpegts",
                    outputDir.resolve("playlist.m3u8").toString()
            );

            log.info("Starting FFmpeg with command: {}", String.join(" ", command));

            // Start process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Log FFmpeg output
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("FFmpeg: {}", line);
                    }
                } catch (IOException e) {
                    log.error("Error reading FFmpeg output", e);
                }
            }).start();

            activeStreams.put(streamId, process);

            return "/streams/" + streamId + "/playlist.m3u8";

        } catch (Exception e) {
            log.error("Failed to start stream", e);
            throw new RuntimeException("Failed to start stream: " + e.getMessage(), e);
        }
    }

    private boolean testRTSPConnection(String rtspUrl) {
        try {
            List<String> testCommand = Arrays.asList(
                    "ffprobe", "-v", "quiet",
                    "-rtsp_transport", "tcp",
                    "-i", rtspUrl,
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0"
            );

            ProcessBuilder pb = new ProcessBuilder(testCommand);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.warn("RTSP connection test failed", e);
            return false;
        }
    }

    public void stopStream(String streamId) {
        Process process = activeStreams.remove(streamId);
        if (process != null) {
            process.destroy();
            log.info("Stopped stream: {}", streamId);
        }

        // Clean up files từ target directory
        try {
            Path streamDir = Paths.get("target/classes/static/streams", streamId);
            if (Files.exists(streamDir)) {
                Files.walk(streamDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("Failed to clean up stream directory", e);
        }
    }
}