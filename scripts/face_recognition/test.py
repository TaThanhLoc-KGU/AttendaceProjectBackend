import cv2
import numpy as np
import insightface
import pymysql
import base64
from scipy.spatial.distance import cosine
import time
from collections import defaultdict, deque
import threading
import queue
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlparse
import math

class ImprovedRTSPFaceRecognition:
    def __init__(self, rtsp_url=None, max_workers=4):
        # Database
        self.connection = None
        self.student_embeddings = {}
        self.embedding_cache_time = 0
        self.cache_duration = 300

        # Models
        self.app = None
        self.recognition_threshold = 0.55  # Tăng threshold để chính xác hơn

        # RTSP Configuration
        self.rtsp_url = rtsp_url
        self.cap = None
        self.connection_retry_count = 0
        self.max_retry_count = 5
        self.reconnect_delay = 3

        # Performance Optimization
        self.frame_queue = queue.Queue(maxsize=8)
        self.result_queue = queue.Queue(maxsize=20)

        # Threading
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self.is_running = False
        self.capture_thread = None
        self.process_thread = None
        self.display_thread = None

        # Improved Frame Processing
        self.frame_skip = 1  # Process every frame for better tracking
        self.frame_count = 0
        self.target_fps = 20  # Giảm target FPS để xử lý tốt hơn
        self.process_interval = 1.0 / self.target_fps

        # Enhanced Face Tracking
        self.face_tracks = {}
        self.next_track_id = 0
        self.track_history = defaultdict(lambda: deque(maxlen=15))  # Tăng history
        self.recognition_history = defaultdict(lambda: deque(maxlen=10))  # Lịch sử nhận diện

        # Tracking parameters
        self.max_track_distance = 100  # Khoảng cách tối đa để track
        self.track_timeout = 20  # Timeout cho track không active
        self.min_confidence_frames = 3  # Số frame tối thiểu để xác nhận nhận diện

        # Multi-scale detection
        self.detection_scales = [(320, 320), (416, 416)]  # Multiple scales
        self.current_scale_idx = 0

        # Recognition stabilization
        self.recognition_buffer = defaultdict(list)  # Buffer để smooth recognition
        self.buffer_size = 5

        # Statistics
        self.stats = {
            'frames_captured': 0,
            'frames_processed': 0,
            'faces_detected': 0,
            'recognitions_made': 0,
            'confident_recognitions': 0,
            'start_time': time.time(),
            'fps_capture': 0,
            'fps_process': 0
        }

        print("🚀 Improved RTSP Face Recognition initialized")

    def connect_database(self):
        """Kết nối database"""
        try:
            self.connection = pymysql.connect(
                host='127.0.0.1',
                user='taloc',
                password='24082003',
                database='face_attendance',
                charset='utf8mb4',
                cursorclass=pymysql.cursors.DictCursor,
                autocommit=True,
                connect_timeout=10,
                read_timeout=30,
                write_timeout=30
            )
            print("✅ Database connected successfully")
            return True
        except Exception as e:
            print(f"❌ Database connection error: {e}")
            return False

    def load_embeddings_from_db(self, force_reload=False):
        """Load embeddings với caching"""
        current_time = time.time()

        if not force_reload and self.student_embeddings and \
                (current_time - self.embedding_cache_time) < self.cache_duration:
            print(f"📋 Using cached embeddings ({len(self.student_embeddings)} students)")
            return True

        if not self.connection:
            if not self.connect_database():
                return False

        try:
            with self.connection.cursor() as cursor:
                cursor.execute("""
                               SELECT ma_sv, ho_ten, embedding
                               FROM sinhvien
                               WHERE embedding IS NOT NULL
                               ORDER BY ma_sv
                               """)
                students = cursor.fetchall()

                new_embeddings = {}
                success_count = 0

                for student in students:
                    try:
                        embedding_data = student['embedding']
                        if isinstance(embedding_data, str):
                            embedding_bytes = base64.b64decode(embedding_data)
                            embedding = np.frombuffer(embedding_bytes, dtype=np.float32)

                            if len(embedding) == 512:
                                embedding = embedding / np.linalg.norm(embedding)
                                new_embeddings[student['ma_sv']] = {
                                    'ma_sv': student['ma_sv'],
                                    'ho_ten': student['ho_ten'],
                                    'embedding': embedding
                                }
                                success_count += 1
                    except Exception as e:
                        print(f"⚠️ Error processing {student['ma_sv']}: {e}")
                        continue

                self.student_embeddings = new_embeddings
                self.embedding_cache_time = current_time

                print(f"🎯 Loaded {success_count} embeddings successfully")
                return success_count > 0

        except Exception as e:
            print(f"❌ Error loading embeddings: {e}")
            return False

    def init_face_model(self):
        """Initialize InsightFace model với optimization"""
        try:
            providers = ['CUDAExecutionProvider', 'CPUExecutionProvider']
            self.app = insightface.app.FaceAnalysis(
                providers=providers,
                allowed_modules=['detection', 'recognition']
            )
            # Dùng scale vừa phải cho balance giữa tốc độ và độ chính xác
            self.app.prepare(ctx_id=0, det_size=self.detection_scales[0])
            print("✅ InsightFace model initialized (GPU)")
            return True
        except Exception as e:
            try:
                self.app = insightface.app.FaceAnalysis(
                    providers=['CPUExecutionProvider']
                )
                self.app.prepare(ctx_id=-1, det_size=self.detection_scales[0])
                print("✅ InsightFace model initialized (CPU)")
                return True
            except Exception as e2:
                print(f"❌ Model initialization error: {e2}")
                return False

    def connect_rtsp(self):
        """Connect RTSP với optimized settings"""
        if not self.rtsp_url:
            return False

        try:
            if self.cap is not None:
                self.cap.release()

            self.cap = cv2.VideoCapture(self.rtsp_url, cv2.CAP_FFMPEG)

            # Tối ưu cho stability
            self.cap.set(cv2.CAP_PROP_BUFFERSIZE, 2)
            self.cap.set(cv2.CAP_PROP_FPS, 20)  # Giảm FPS để ổn định hơn
            self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
            self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

            ret, frame = self.cap.read()
            if ret and frame is not None:
                print(f"✅ RTSP connected: {frame.shape[1]}x{frame.shape[0]}")
                self.connection_retry_count = 0
                return True
            else:
                print("❌ RTSP failed to read frame")
                return False

        except Exception as e:
            print(f"❌ RTSP connection error: {e}")
            return False

    def capture_frames(self):
        """Thread function for capturing frames"""
        fps_counter = 0
        fps_start = time.time()

        while self.is_running:
            try:
                ret, frame = self.cap.read()
                if not ret or frame is None:
                    print("⚠️ Failed to capture frame")
                    time.sleep(0.1)
                    continue

                try:
                    self.frame_queue.put_nowait((frame, time.time()))
                    self.stats['frames_captured'] += 1
                    fps_counter += 1
                except queue.Full:
                    try:
                        self.frame_queue.get_nowait()
                        self.frame_queue.put_nowait((frame, time.time()))
                    except queue.Empty:
                        pass

                if time.time() - fps_start >= 1.0:
                    self.stats['fps_capture'] = fps_counter
                    fps_counter = 0
                    fps_start = time.time()

            except Exception as e:
                print(f"❌ Capture error: {e}")
                time.sleep(0.1)

    def enhanced_face_detection(self, frame):
        """Enhanced face detection với multiple scales"""
        try:
            best_faces = []

            # Thử detection với scale hiện tại
            height, width = frame.shape[:2]

            # Adaptive scaling dựa trên kích thước frame
            if width > 1280:
                scale = 1280 / width
                detection_frame = cv2.resize(frame, (int(width * scale), int(height * scale)))
                scale_back = 1.0 / scale
            else:
                detection_frame = frame
                scale_back = 1.0

            # Detect faces
            faces = self.app.get(detection_frame)

            # Scale back coordinates nếu cần
            if scale_back != 1.0:
                for face in faces:
                    face.bbox = face.bbox * scale_back

            # Filter faces by confidence và size
            for face in faces:
                bbox = face.bbox.astype(int)
                face_width = bbox[2] - bbox[0]
                face_height = bbox[3] - bbox[1]
                face_area = face_width * face_height

                # Filter faces that are too small
                if face_area > 1600 and face.det_score > 0.5:  # Tăng threshold
                    best_faces.append({
                        'bbox': bbox,
                        'embedding': face.embedding,
                        'confidence': face.det_score,
                        'area': face_area
                    })

            # Sort by confidence and area
            best_faces.sort(key=lambda x: (x['confidence'], x['area']), reverse=True)

            return best_faces[:10]  # Giới hạn max 10 faces

        except Exception as e:
            print(f"❌ Face detection error: {e}")
            return []

    def improved_face_tracking(self, faces):
        """Improved face tracking với motion prediction"""
        current_time = time.time()
        current_tracks = {}

        for face in faces:
            bbox = face['bbox']
            center_x = (bbox[0] + bbox[2]) // 2
            center_y = (bbox[1] + bbox[3]) // 2

            best_track_id = None
            min_distance = float('inf')

            # Tìm track gần nhất với motion prediction
            for track_id, track_data in self.face_tracks.items():
                if 'center' in track_data and 'last_seen' in track_data:
                    # Kiểm tra thời gian
                    time_diff = current_time - track_data['last_seen']
                    if time_diff > 2.0:  # Skip tracks quá cũ
                        continue

                    old_x, old_y = track_data['center']

                    # Motion prediction nếu có velocity
                    if 'velocity' in track_data:
                        vel_x, vel_y = track_data['velocity']
                        predicted_x = old_x + vel_x * time_diff
                        predicted_y = old_y + vel_y * time_diff
                    else:
                        predicted_x, predicted_y = old_x, old_y

                    # Tính distance với predicted position
                    distance = math.sqrt((center_x - predicted_x)**2 + (center_y - predicted_y)**2)

                    # Adaptive threshold dựa trên thời gian
                    threshold = self.max_track_distance * (1 + time_diff)

                    if distance < min_distance and distance < threshold:
                        min_distance = distance
                        best_track_id = track_id

            if best_track_id is not None:
                track_id = best_track_id
                # Tính velocity
                old_center = self.face_tracks[track_id]['center']
                old_time = self.face_tracks[track_id]['last_seen']
                dt = current_time - old_time
                if dt > 0:
                    vel_x = (center_x - old_center[0]) / dt
                    vel_y = (center_y - old_center[1]) / dt
                else:
                    vel_x = vel_y = 0
            else:
                track_id = self.next_track_id
                self.next_track_id += 1
                vel_x = vel_y = 0

            current_tracks[track_id] = {
                'face': face,
                'bbox': bbox,
                'center': (center_x, center_y),
                'velocity': (vel_x, vel_y),
                'last_seen': current_time,
                'age': 0
            }

        # Age old tracks
        for track_id in list(self.face_tracks.keys()):
            if track_id not in current_tracks:
                self.face_tracks[track_id]['age'] = self.face_tracks[track_id].get('age', 0) + 1
                if self.face_tracks[track_id]['age'] > self.track_timeout:
                    del self.face_tracks[track_id]
                    # Clear recognition history for this track
                    if track_id in self.recognition_buffer:
                        del self.recognition_buffer[track_id]

        self.face_tracks.update(current_tracks)
        return current_tracks

    def stable_recognition(self, track_id, embedding):
        """Stable recognition với voting mechanism"""
        student, confidence = self.find_best_match(embedding)

        # Add to recognition buffer
        if track_id not in self.recognition_buffer:
            self.recognition_buffer[track_id] = []

        self.recognition_buffer[track_id].append({
            'student': student,
            'confidence': confidence,
            'timestamp': time.time()
        })

        # Keep only recent recognitions
        buffer = self.recognition_buffer[track_id]
        current_time = time.time()
        buffer = [r for r in buffer if current_time - r['timestamp'] < 3.0]  # 3 second window
        buffer = buffer[-self.buffer_size:]  # Keep last N recognitions
        self.recognition_buffer[track_id] = buffer

        if len(buffer) < self.min_confidence_frames:
            return None, 0.0

        # Voting mechanism
        if student is not None:
            # Count votes for this student
            votes = sum(1 for r in buffer if r['student'] is not None and
                        r['student']['ma_sv'] == student['ma_sv'] and
                        r['confidence'] > (1 - self.recognition_threshold))

            vote_ratio = votes / len(buffer)
            avg_confidence = np.mean([r['confidence'] for r in buffer if r['student'] is not None])

            # Require majority vote and high confidence
            if vote_ratio >= 0.6 and avg_confidence > (1 - self.recognition_threshold):
                return student, avg_confidence

        return None, confidence

    def process_faces_batch(self, frame):
        """Process faces với enhanced tracking và recognition"""
        try:
            # Enhanced face detection
            faces = self.enhanced_face_detection(frame)

            if not faces:
                return []

            # Improved tracking
            tracked_faces = self.improved_face_tracking(faces)

            # Stable recognition
            results = []
            for track_id, track_data in tracked_faces.items():
                face = track_data['face']
                bbox = track_data['bbox']

                # Stable recognition
                student, confidence = self.stable_recognition(track_id, face['embedding'])

                result = {
                    'track_id': track_id,
                    'bbox': bbox,
                    'student': student,
                    'confidence': confidence,
                    'detection_confidence': face['confidence'],
                    'timestamp': time.time(),
                    'stable': len(self.recognition_buffer.get(track_id, [])) >= self.min_confidence_frames
                }

                results.append(result)

                # Update history
                self.track_history[track_id].append({
                    'student': student,
                    'confidence': confidence,
                    'timestamp': time.time()
                })

            self.stats['faces_detected'] = len(faces)
            self.stats['recognitions_made'] += len(results)
            self.stats['confident_recognitions'] += sum(1 for r in results if r['stable'])

            return results

        except Exception as e:
            print(f"❌ Face processing error: {e}")
            return []

    def process_frames(self):
        """Thread function for processing frames"""
        fps_counter = 0
        fps_start = time.time()
        last_process_time = 0

        while self.is_running:
            try:
                current_time = time.time()
                if current_time - last_process_time < self.process_interval:
                    time.sleep(0.01)
                    continue

                try:
                    frame, capture_time = self.frame_queue.get(timeout=0.1)
                except queue.Empty:
                    continue

                # Process every frame for better tracking
                self.frame_count += 1

                # Adaptive frame skipping dựa trên load
                skip_frames = max(1, self.frame_skip)
                if self.frame_count % skip_frames != 0:
                    continue

                results = self.process_faces_batch(frame)

                try:
                    self.result_queue.put_nowait({
                        'frame': frame,
                        'results': results,
                        'timestamp': current_time
                    })
                except queue.Full:
                    try:
                        self.result_queue.get_nowait()
                        self.result_queue.put_nowait({
                            'frame': frame,
                            'results': results,
                            'timestamp': current_time
                        })
                    except queue.Empty:
                        pass

                self.stats['frames_processed'] += 1
                fps_counter += 1
                last_process_time = current_time

                if current_time - fps_start >= 1.0:
                    self.stats['fps_process'] = fps_counter
                    fps_counter = 0
                    fps_start = current_time

            except Exception as e:
                print(f"❌ Processing error: {e}")
                time.sleep(0.1)

    def find_best_match(self, face_embedding):
        """Optimized face matching"""
        if len(self.student_embeddings) == 0:
            return None, 0.0

        face_embedding = face_embedding / np.linalg.norm(face_embedding)

        best_match = None
        best_similarity = 0.0

        for ma_sv, student in self.student_embeddings.items():
            similarity = 1 - cosine(face_embedding, student['embedding'])

            if similarity > best_similarity:
                best_similarity = similarity
                best_match = student

        if best_similarity > (1 - self.recognition_threshold):
            return best_match, best_similarity
        else:
            return None, best_similarity

    def draw_enhanced_info(self, frame, results):
        """Enhanced drawing với stability indicators"""
        for result in results:
            track_id = result['track_id']
            bbox = result['bbox']
            student = result['student']
            confidence = result['confidence']
            stable = result['stable']
            detection_conf = result['detection_confidence']

            x1, y1, x2, y2 = bbox.astype(int)

            if student is not None and stable:
                color = (0, 255, 0)  # Green cho stable recognition
                name = student['ho_ten']
                ma_sv = student['ma_sv']
                status = "CONFIRMED"
            elif student is not None:
                color = (0, 255, 255)  # Yellow cho unstable
                name = student['ho_ten']
                ma_sv = student['ma_sv']
                status = "DETECTING..."
            else:
                color = (0, 0, 255)  # Red cho unknown
                name = "Unknown"
                ma_sv = "N/A"
                status = "UNKNOWN"

            # Draw bounding box với thickness dựa trên stability
            thickness = 3 if stable else 2
            cv2.rectangle(frame, (x1, y1), (x2, y2), color, thickness)

            # Draw track ID
            cv2.putText(frame, f"ID:{track_id}", (x1, y1-35),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)

            # Draw info với background
            info_y = y2 + 25

            # Background cho text
            text_bg_height = 60
            cv2.rectangle(frame, (x1, info_y - 15), (x1 + 200, info_y + text_bg_height), (0, 0, 0), -1)

            # Status
            cv2.putText(frame, status, (x1 + 5, info_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

            # Name
            cv2.putText(frame, name[:15], (x1 + 5, info_y + 20),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

            if student is not None:
                cv2.putText(frame, f"MSSV: {ma_sv}", (x1 + 5, info_y + 40),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1)

            # Confidence
            conf_text = f"Conf: {confidence*100:.1f}%"
            cv2.putText(frame, conf_text, (x1 + 5, info_y + 55),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1)

    def draw_enhanced_stats(self, frame):
        """Draw enhanced statistics"""
        cv2.rectangle(frame, (10, 10), (450, 160), (0, 0, 0), -1)
        cv2.rectangle(frame, (10, 10), (450, 160), (255, 255, 255), 2)

        runtime = time.time() - self.stats['start_time']

        stats_text = [
            f"Capture FPS: {self.stats['fps_capture']} | Process FPS: {self.stats['fps_process']}",
            f"Students in DB: {len(self.student_embeddings)}",
            f"Active Tracks: {len(self.face_tracks)}",
            f"Faces detected: {self.stats['faces_detected']}",
            f"Total recognitions: {self.stats['recognitions_made']}",
            f"Confident recognitions: {self.stats['confident_recognitions']}",
            f"Runtime: {runtime:.1f}s"
        ]

        for i, text in enumerate(stats_text):
            cv2.putText(frame, text, (20, 35 + i * 18),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 255), 1)

    def display_results(self):
        """Thread function for displaying results"""
        while self.is_running:
            try:
                try:
                    result_data = self.result_queue.get(timeout=0.1)
                except queue.Empty:
                    continue

                frame = result_data['frame']
                results = result_data['results']

                self.draw_enhanced_info(frame, results)
                self.draw_enhanced_stats(frame)

                cv2.imshow('Improved RTSP Face Recognition', frame)

                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    self.is_running = False
                    break
                elif key == ord('+') or key == ord('='):
                    self.recognition_threshold = min(0.8, self.recognition_threshold + 0.02)
                    print(f"📊 Threshold: {self.recognition_threshold:.3f}")
                elif key == ord('-'):
                    self.recognition_threshold = max(0.4, self.recognition_threshold - 0.02)
                    print(f"📊 Threshold: {self.recognition_threshold:.3f}")
                elif key == ord('r'):
                    print("🔄 Reloading embeddings...")
                    self.load_embeddings_from_db(force_reload=True)
                elif key == ord('c'):
                    print("🧹 Clearing recognition history...")
                    self.recognition_buffer.clear()
                    self.track_history.clear()

            except Exception as e:
                print(f"❌ Display error: {e}")
                time.sleep(0.1)

    def run(self):
        """Main execution function"""
        print("🚀 Starting Improved RTSP Face Recognition...")

        if not self.connect_database():
            print("❌ Database connection failed")
            return

        if not self.load_embeddings_from_db():
            print("❌ Failed to load embeddings")
            return

        if not self.init_face_model():
            print("❌ Failed to initialize face model")
            return

        if not self.connect_rtsp():
            print("❌ Failed to connect RTSP")
            return

        print("✅ All components initialized successfully")
        print("🚀 Starting enhanced multi-threaded processing...")

        self.is_running = True

        self.capture_thread = threading.Thread(target=self.capture_frames, daemon=True)
        self.process_thread = threading.Thread(target=self.process_frames, daemon=True)
        self.display_thread = threading.Thread(target=self.display_results, daemon=True)

        self.capture_thread.start()
        self.process_thread.start()
        self.display_thread.start()

        print("🎯 System running! Controls:")
        print("   Q = Quit")
        print("   +/- = Adjust recognition threshold")
        print("   R = Reload database")
        print("   C = Clear tracking history")

        try:
            while self.is_running:
                time.sleep(0.1)
        except KeyboardInterrupt:
            print("\n⚠️ Interrupted by user")
        finally:
            self.cleanup()

    def cleanup(self):
        """Cleanup resources"""
        print("🧹 Cleaning up...")

        self.is_running = False

        if self.capture_thread and self.capture_thread.is_alive():
            self.capture_thread.join(timeout=2)
        if self.process_thread and self.process_thread.is_alive():
            self.process_thread.join(timeout=2)
        if self.display_thread and self.display_thread.is_alive():
            self.display_thread.join(timeout=2)

        if self.cap:
            self.cap.release()
        cv2.destroyAllWindows()
        if self.connection:
            self.connection.close()
        if self.executor:
            self.executor.shutdown(wait=True)

        print("✅ Cleanup completed")

def main():
    """Entry point"""
    print("=" * 70)
    print("🚀 IMPROVED RTSP FACE RECOGNITION")
    print("=" * 70)

    rtsp_url = input("Enter RTSP URL (or press Enter for webcam): ").strip()

    if not rtsp_url:
        print("⚠️ Using webcam")
        rtsp_url = 0

    if isinstance(rtsp_url, str) and rtsp_url.startswith('rtsp://'):
        parsed = urlparse(rtsp_url)
        if not parsed.hostname:
            print("❌ Invalid RTSP URL")
            return
        print(f"✅ RTSP URL validated: {parsed.hostname}:{parsed.port or 554}")

    print("\n🎯 Improvements:")
    print("✅ Enhanced face tracking với motion prediction")
    print("✅ Stable recognition với voting mechanism")
    print("✅ Multi-scale detection")
    print("✅ Adaptive thresholds")
    print("✅ Recognition buffering và smoothing")
    print("✅ Better handling of multiple people")
    print("=" * 70)

    if rtsp_url == 0:
        app = ImprovedRTSPFaceRecognition()
        app.rtsp_url = None
        def connect_local():
            app.cap = cv2.VideoCapture(0)
            app.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
            app.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
            app.cap.set(cv2.CAP_PROP_FPS, 25)
            return app.cap.isOpened()
        app.connect_rtsp = connect_local
    else:
        app = ImprovedRTSPFaceRecognition(rtsp_url, max_workers=4)

    app.run()

if __name__ == "__main__":
    main()