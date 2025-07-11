/**
 * ==========================================
 * CAMERA MANAGEMENT SYSTEM - JavaScript
 * Face Attendance System - RTSP Support
 * ==========================================
 */

// ===== GLOBAL VARIABLES =====
let cameras = [];
let rooms = [];
let cameraToDelete = null;
let isEditMode = false;

// Configuration
const CONFIG = {
    refreshInterval: 30000, // 30 seconds
    apiEndpoints: {
        cameras: '/api/cameras',
        rooms: '/api/phonghoc/all'
    },
    streamTimeout: 10000 // 10 seconds timeout for stream loading
};

// ===== INITIALIZATION =====
document.addEventListener('DOMContentLoaded', function() {
    console.log('📹 Camera management system initializing...');

    // Initialize all components
    initializeEventListeners();
    initializeModals();

    // Load initial data
    loadRooms();
    loadCameras();

    // Setup periodic refresh
    setupPeriodicRefresh();

    console.log('✅ Camera management system ready');
});

// ===== EVENT LISTENERS =====
function initializeEventListeners() {
    // Header buttons
    const addCameraBtn = document.getElementById('addCameraBtn');
    const testAllBtn = document.getElementById('testAllBtn');
    const exportConfigBtn = document.getElementById('exportConfigBtn');
    const toggleAllOnBtn = document.getElementById('toggleAllOnBtn');
    const toggleAllOffBtn = document.getElementById('toggleAllOffBtn');
    const showRTSPGuideBtn = document.getElementById('showRTSPGuideBtn');

    // Form elements
    const cameraForm = document.getElementById('cameraForm');
    const saveButton = document.getElementById('saveButton');
    const testConnectionBtn = document.getElementById('testConnectionBtn');
    const deleteButton = document.getElementById('deleteButton');
    const sidebarToggle = document.getElementById('sidebarToggle');

    // Bind events
    if (addCameraBtn) addCameraBtn.addEventListener('click', openAddCameraModal);
    if (testAllBtn) testAllBtn.addEventListener('click', testAllConnections);
    if (exportConfigBtn) exportConfigBtn.addEventListener('click', exportCameraConfig);
    if (toggleAllOnBtn) toggleAllOnBtn.addEventListener('click', () => toggleAllCameras(true));
    if (toggleAllOffBtn) toggleAllOffBtn.addEventListener('click', () => toggleAllCameras(false));
    if (showRTSPGuideBtn) showRTSPGuideBtn.addEventListener('click', showRTSPGuide);
    if (saveButton) saveButton.addEventListener('click', saveCamera);
    if (testConnectionBtn) testConnectionBtn.addEventListener('click', testCameraConnection);
    if (deleteButton) deleteButton.addEventListener('click', confirmDelete);
    if (sidebarToggle) sidebarToggle.addEventListener('click', toggleSidebar);

    // Form submission
    if (cameraForm) {
        cameraForm.addEventListener('submit', function(e) {
            e.preventDefault();
            if (validateForm()) {
                saveCamera();
            }
        });
    }

    // Input validation
    const ipAddressInput = document.getElementById('ipAddress');
    if (ipAddressInput) {
        ipAddressInput.addEventListener('blur', validateUrlInput);
    }

    // Zone validation
    const vungInInput = document.getElementById('vungIn');
    const vungOutInput = document.getElementById('vungOut');
    if (vungInInput) vungInInput.addEventListener('blur', () => validateJSONInput('vungIn'));
    if (vungOutInput) vungOutInput.addEventListener('blur', () => validateJSONInput('vungOut'));

    console.log('✅ Event listeners initialized');
}

function initializeModals() {
    const cameraModal = document.getElementById('cameraModal');
    if (cameraModal) {
        cameraModal.addEventListener('hidden.bs.modal', resetForm);
    }
}

function setupPeriodicRefresh() {
    setInterval(() => {
        console.log('🔄 Auto-refreshing camera data...');
        loadCameras();
    }, CONFIG.refreshInterval);
}

function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) {
        sidebar.classList.toggle('collapsed');
    }
}

// ===== DATA LOADING FUNCTIONS =====
async function loadRooms() {
    try {
        showLoading('Loading rooms...');

        const response = await fetch(CONFIG.apiEndpoints.rooms);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        rooms = await response.json();
        updateRoomSelect();

        console.log(`✅ Loaded ${rooms.length} rooms`);
    } catch (error) {
        console.error('❌ Error loading rooms:', error);
        showNotification('Lỗi khi tải danh sách phòng học: ' + error.message, 'error');
        handleRoomLoadError();
    }
}

async function loadCameras() {
    try {
        showLoading('Loading cameras...');

        const response = await fetch(CONFIG.apiEndpoints.cameras);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        cameras = await response.json();

        // Ensure proper data structure
        cameras = cameras.map(camera => ({
            ...camera,
            active: camera.active !== false,
            id: parseInt(camera.id) || camera.id
        }));

        updateStatistics();
        updateCameraGrid();

        console.log(`✅ Loaded ${cameras.length} cameras`);
    } catch (error) {
        console.error('❌ Error loading cameras:', error);
        showNotification('Lỗi khi tải danh sách camera: ' + error.message, 'error');
        showErrorState();
    }
}

// ===== DISPLAY UPDATE FUNCTIONS =====
function updateRoomSelect() {
    const select = document.getElementById('maPhong');
    if (!select) return;

    select.innerHTML = '<option value="">Chọn phòng học</option>';

    if (rooms && rooms.length > 0) {
        rooms.forEach(room => {
            const option = document.createElement('option');
            option.value = room.maPhong;
            option.textContent = `${room.maPhong} - ${room.tenPhong}`;
            select.appendChild(option);
        });
    }
}

function updateStatistics() {
    const total = cameras.length;
    const online = cameras.filter(c => c.active === true).length;
    const offline = total - online;
    const roomsCovered = new Set(
        cameras.filter(c => c.maPhong && c.maPhong.trim())
            .map(c => c.maPhong)
    ).size;

    // Update with animation
    animateCounter('totalCameras', total);
    animateCounter('onlineCameras', online);
    animateCounter('offlineCameras', offline);
    animateCounter('roomsCovered', roomsCovered);

    console.log(`📊 Stats updated: ${total} total, ${online} online, ${offline} offline, ${roomsCovered} rooms`);
}

function animateCounter(elementId, targetValue) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const currentValue = parseInt(element.textContent) || 0;
    if (currentValue === targetValue) return;

    const increment = targetValue > currentValue ? 1 : -1;
    const timer = setInterval(() => {
        const current = parseInt(element.textContent) || 0;
        if (current === targetValue) {
            clearInterval(timer);
        } else {
            element.textContent = current + increment;
        }
    }, 50);
}

function updateCameraGrid() {
    const grid = document.getElementById('cameraGrid');
    if (!grid) return;

    if (!cameras || cameras.length === 0) {
        grid.innerHTML = createEmptyState();
        return;
    }

    // Clear loading state
    const loadingState = document.getElementById('loadingState');
    if (loadingState) {
        loadingState.remove();
    }

    // Create camera cards
    grid.innerHTML = cameras.map((camera, index) =>
        createCameraCard(camera, index)
    ).join('');

    // Initialize streams after DOM update
    setTimeout(() => {
        cameras.forEach(camera => initializeCameraStream(camera));
    }, 100);
}

function createEmptyState() {
    return `
        <div style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
            <i class="fas fa-video-slash text-muted" style="font-size: 4rem; margin-bottom: 1rem;"></i>
            <h5 class="text-muted mb-3">Chưa có camera nào được thêm</h5>
            <p class="text-muted mb-4">Thêm camera đầu tiên để bắt đầu giám sát phòng học</p>
            <button class="btn btn-primary btn-lg" onclick="openAddCameraModal()">
                <i class="fas fa-plus me-2"></i>Thêm camera đầu tiên
            </button>
        </div>
    `;
}

function createCameraCard(camera, index = 0) {
    const roomInfo = rooms.find(r => r.maPhong === camera.maPhong);
    const statusClass = camera.active ? 'online' : 'offline';

    return `
        <div class="camera-card" data-camera-id="${camera.id}" style="animation-delay: ${index * 100}ms;">
            <div class="camera-header">
                <div class="camera-status ${statusClass}" title="${camera.active ? 'Đang hoạt động' : 'Mất kết nối'}"></div>
                <h6 class="mb-1">${escapeHtml(camera.tenCamera)}</h6>
                <small class="opacity-75">
                    ${roomInfo ? `${roomInfo.maPhong} - ${escapeHtml(roomInfo.tenPhong)}` : 'Chưa gán phòng'}
                </small>
            </div>
            
            <div class="camera-preview" id="preview-${camera.id}">
                ${createStreamContainer(camera)}
                <div class="camera-overlay">
                    <div class="camera-controls">
                        <button class="btn btn-sm btn-light" onclick="viewFullscreen(${camera.id})" 
                                title="Xem toàn màn hình">
                            <i class="fas fa-expand"></i>
                        </button>
                        <button class="btn btn-sm btn-info" onclick="testSingleCamera(${camera.id})" 
                                title="Test kết nối">
                            <i class="fas fa-wifi"></i>
                        </button>
                        <button class="btn btn-sm btn-warning" onclick="editCamera(${camera.id})" 
                                title="Chỉnh sửa">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteCamera(${camera.id})" 
                                title="Xóa">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
            
            <div class="camera-info">
                <div class="camera-detail">
                    <i class="fas fa-network-wired"></i>
                    <span title="${camera.ipAddress}">${truncateText(camera.ipAddress, 35)}</span>
                </div>
                <div class="camera-detail">
                    <i class="fas fa-info-circle"></i>
                    <span class="connection-test ${camera.active ? 'success' : 'failed'}">
                        <i class="fas ${camera.active ? 'fa-check' : 'fa-times'}"></i>
                        ${camera.active ? 'Đang hoạt động' : 'Mất kết nối'}
                    </span>
                </div>
                ${(camera.vungIn || camera.vungOut) ? `
                <div class="camera-detail">
                    <i class="fas fa-crosshairs"></i>
                    <span>Đã cấu hình vùng phát hiện</span>
                </div>
                ` : ''}
                ${camera.password ? `
                <div class="camera-detail">
                    <i class="fas fa-lock"></i>
                    <span>Có bảo mật</span>
                </div>
                ` : ''}
            </div>
        </div>
    `;
}

function createStreamContainer(camera) {
    if (!camera.active || !camera.ipAddress) {
        return `
            <div class="no-stream">
                <i class="fas fa-video-slash fs-1 mb-3"></i>
                <p>Camera không hoạt động</p>
                <small class="text-muted">Kiểm tra kết nối và cấu hình</small>
            </div>
        `;
    }

    const streamType = getStreamType(camera.ipAddress);
    const methodBadge = getStreamMethodBadge(streamType);

    return `
        <div class="stream-container">
            ${methodBadge}
            ${createStreamElement(camera, streamType)}
            <div class="stream-loading">
                <i class="fas fa-spinner fa-spin fs-3 mb-2"></i>
                <p>Đang tải stream...</p>
                <small>${getStreamTypeDescription(streamType)}</small>
            </div>
            <div class="stream-error" style="display: none;">
                <i class="fas fa-exclamation-triangle fs-3 mb-2"></i>
                <p>Không thể kết nối stream</p>
                <div class="mt-2">
                    <button class="btn btn-sm btn-primary" onclick="retryStream(${camera.id})">
                        <i class="fas fa-redo me-1"></i>Thử lại
                    </button>
                    ${streamType === 'RTSP' ? `
                    <button class="btn btn-sm btn-success" onclick="openInVLC('${camera.ipAddress}')">
                        <i class="fas fa-external-link-alt me-1"></i>Mở VLC
                    </button>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
}

function createStreamElement(camera, streamType) {
    if (streamType === 'RTSP') {
        // For RTSP, create video element for potential WebRTC/HLS conversion
        return `
            <video class="camera-stream" 
                   id="video-${camera.id}"
                   controls 
                   muted 
                   preload="none"
                   style="display: none;">
                Your browser does not support video playback.
            </video>
            <div class="rtsp-fallback" style="display: none;">
                <i class="fas fa-video fs-3 mb-2"></i>
                <p class="mb-2">RTSP Stream</p>
                <small class="text-break mb-3">${camera.ipAddress}</small>
                <div>
                    <button class="btn btn-sm btn-success" onclick="openInVLC('${camera.ipAddress}')">
                        <i class="fas fa-play me-1"></i>Mở trong VLC
                    </button>
                    <button class="btn btn-sm btn-info" onclick="copyToClipboard('${camera.ipAddress}')">
                        <i class="fas fa-copy me-1"></i>Copy URL
                    </button>
                </div>
            </div>
        `;
    } else if (streamType === 'HTTP_MJPEG') {
        // For HTTP MJPEG streams
        return `
            <img class="camera-stream" 
                 id="img-${camera.id}"
                 alt="Camera Stream"
                 style="display: none;">
        `;
    } else {
        // For other HTTP streams
        return `
            <video class="camera-stream" 
                   id="video-${camera.id}"
                   controls 
                   muted 
                   preload="none"
                   style="display: none;">
                Your browser does not support video playback.
            </video>
        `;
    }
}

function getStreamMethodBadge(streamType) {
    const badges = {
        'RTSP': '<div class="stream-method-badge rtsp">RTSP</div>',
        'HTTP_MJPEG': '<div class="stream-method-badge http">MJPEG</div>',
        'HTTP': '<div class="stream-method-badge http">HTTP</div>',
        'HTTPS': '<div class="stream-method-badge http">HTTPS</div>'
    };
    return badges[streamType] || '';
}

function getStreamTypeDescription(streamType) {
    const descriptions = {
        'RTSP': 'Đang kết nối RTSP stream...',
        'HTTP_MJPEG': 'Đang tải MJPEG stream...',
        'HTTP': 'Đang tải HTTP stream...',
        'HTTPS': 'Đang tải HTTPS stream...'
    };
    return descriptions[streamType] || 'Đang tải stream...';
}

// ===== STREAM HANDLING =====
function initializeCameraStream(camera) {
    if (!camera.active || !camera.ipAddress) return;

    const streamType = getStreamType(camera.ipAddress);
    const previewContainer = document.getElementById(`preview-${camera.id}`);

    if (!previewContainer) return;

    console.log(`🎥 Initializing ${streamType} stream for camera: ${camera.tenCamera}`);

    if (streamType === 'HTTP_MJPEG') {
        initializeHTTPStream(camera, previewContainer);
    } else if (streamType === 'RTSP') {
        initializeRTSPStream(camera, previewContainer);
    } else {
        initializeGenericStream(camera, previewContainer);
    }
}

function initializeHTTPStream(camera, container) {
    const img = container.querySelector(`#img-${camera.id}`);
    const loading = container.querySelector('.stream-loading');
    const error = container.querySelector('.stream-error');

    if (!img) return;

    img.onload = () => {
        console.log(`✅ HTTP stream loaded for camera ${camera.id}`);
        hideElement(loading);
        hideElement(error);
        showElement(img);
    };

    img.onerror = () => {
        console.log(`❌ HTTP stream failed for camera ${camera.id}`);
        hideElement(loading);
        hideElement(img);
        showElement(error);
    };

    // Add timestamp to prevent caching
    const separator = camera.ipAddress.includes('?') ? '&' : '?';
    img.src = `${camera.ipAddress}${separator}_t=${Date.now()}`;

    // Timeout fallback
    setTimeout(() => {
        if (img.style.display === 'none') {
            img.onerror();
        }
    }, CONFIG.streamTimeout);
}

function initializeRTSPStream(camera, container) {
    const video = container.querySelector(`#video-${camera.id}`);
    const loading = container.querySelector('.stream-loading');
    const error = container.querySelector('.stream-error');
    const fallback = container.querySelector('.rtsp-fallback');

    if (!video) return;

    console.log(`🎥 RTSP stream detected for camera: ${camera.tenCamera}`);
    console.log(`📡 RTSP URL: ${camera.ipAddress}`);

    // For RTSP streams, directly show fallback since browsers can't handle RTSP natively
    showRTSPFallback(camera, video, loading, error, fallback);
}

function showRTSPFallback(camera, video, loading, error, fallback) {
    console.log(`📺 Showing RTSP interface for camera ${camera.id}`);

    hideElement(loading);
    hideElement(error);
    hideElement(video);
    showElement(fallback);

    // Update fallback content with clean, user-friendly interface
    if (fallback) {
        fallback.innerHTML = `
            <div class="text-center">
                <i class="fas fa-video fs-1 mb-3 text-primary"></i>
                <h6 class="mb-2">${escapeHtml(camera.tenCamera)}</h6>
                <p class="mb-3 text-light">RTSP Stream Ready</p>
                
                <div class="mb-3">
                    <button class="btn btn-success btn-sm me-2" onclick="openInVLC('${camera.ipAddress}')">
                        <i class="fas fa-play me-1"></i>VLC Player
                    </button>
                    <button class="btn btn-info btn-sm me-2" onclick="copyToClipboard('${camera.ipAddress}')">
                        <i class="fas fa-copy me-1"></i>Copy URL
                    </button>
                    <button class="btn btn-warning btn-sm" onclick="tryAlternativePlayer('${camera.ipAddress}')">
                        <i class="fas fa-external-link-alt me-1"></i>Player khác
                    </button>
                </div>
                
                <div class="mt-2">
                    <small class="text-muted d-block">
                        <i class="fas fa-info-circle me-1"></i>
                        Nhấn VLC Player để xem trực tiếp
                    </small>
                    <details class="mt-2">
                        <summary class="text-muted" style="cursor: pointer; font-size: 0.8rem;">
                            <i class="fas fa-chevron-right me-1"></i>Xem URL
                        </summary>
                        <code class="text-break d-block mt-2" style="font-size: 0.7rem; background: rgba(255,255,255,0.1); padding: 0.5rem; border-radius: 4px;">
                            ${camera.ipAddress}
                        </code>
                    </details>
                </div>
            </div>
        `;
    }
}

window.openInVLC = function(rtspUrl) {
    console.log(`🎥 Opening RTSP in VLC: ${rtspUrl}`);

    // Try to open with VLC protocol
    const vlcUrl = `vlc://${rtspUrl}`;
    const link = document.createElement('a');
    link.href = vlcUrl;
    link.click();

    // Show instructions
    showVLCInstructions(rtspUrl);

    // Also show notification
    showNotification('Đang mở stream trong VLC...', 'info');
};

// ===== STREAM TESTING FOR RTSP =====
function testRTSPStream(url, timeout, resolve) {
    console.log(`🔍 Testing RTSP stream: ${url}`);

    // For RTSP, we'll validate the URL format and show success
    // Real testing would require backend support or special libraries

    setTimeout(() => {
        // Extract info from RTSP URL
        const urlPattern = /^rtsp:\/\/(([^:@]+):([^@]+)@)?([^:\/]+):?(\d+)?(\/.*)?$/i;
        const match = url.match(urlPattern);

        if (match) {
            const [, , username, password, host, port, path] = match;
            const hasAuth = username && password;
            const portInfo = port ? `:${port}` : ':554 (default)';

            let message = `RTSP URL hợp lệ - Host: ${host}${portInfo}`;
            if (hasAuth) {
                message += ` (có authentication)`;
            }

            resolve({
                success: true,
                message: message
            });
        } else {
            resolve({
                success: false,
                message: 'RTSP URL không đúng định dạng'
            });
        }
    }, 1500); // Simulate processing time
}

function initializeGenericStream(camera, container) {
    const video = container.querySelector(`#video-${camera.id}`);
    const loading = container.querySelector('.stream-loading');
    const error = container.querySelector('.stream-error');

    if (!video) return;

    video.onloadeddata = () => {
        console.log(`✅ Generic stream loaded for camera ${camera.id}`);
        hideElement(loading);
        hideElement(error);
        showElement(video);
    };

    video.onerror = () => {
        console.log(`❌ Generic stream failed for camera ${camera.id}`);
        hideElement(loading);
        hideElement(video);
        showElement(error);
    };

    video.src = camera.ipAddress;

    // Timeout fallback
    setTimeout(() => {
        if (video.style.display === 'none') {
            video.onerror();
        }
    }, CONFIG.streamTimeout);
}

async function playVideoWithTimeout(video, timeout = 5000) {
    return new Promise((resolve, reject) => {
        const timeoutId = setTimeout(() => {
            cleanup();
            reject(new Error('Video play timeout'));
        }, timeout);

        const cleanup = () => {
            clearTimeout(timeoutId);
            video.removeEventListener('loadeddata', onSuccess);
            video.removeEventListener('error', onError);
        };

        const onSuccess = () => {
            cleanup();
            resolve(true);
        };

        const onError = () => {
            cleanup();
            reject(new Error('Video play error'));
        };

        video.addEventListener('loadeddata', onSuccess);
        video.addEventListener('error', onError);

        video.play().catch(onError);
    });
}

async function setupWebRTCConnection(video, sdp) {
    if (typeof RTCPeerConnection === 'undefined') {
        throw new Error('WebRTC not supported');
    }

    const peerConnection = new RTCPeerConnection({
        iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
    });

    peerConnection.ontrack = (event) => {
        video.srcObject = event.streams[0];
    };

    await peerConnection.setRemoteDescription(new RTCSessionDescription({
        type: 'offer',
        sdp: sdp
    }));

    const answer = await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(answer);

    // Send answer back to server
    await fetch(`/api/camera/webrtc/${camera.id}/answer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sdp: answer.sdp })
    });

    return await playVideoWithTimeout(video, 5000);
}

// ===== UTILITY FUNCTIONS =====
function getStreamType(url) {
    if (!url) return 'UNKNOWN';

    const lowerUrl = url.toLowerCase();

    if (lowerUrl.startsWith('rtsp://')) {
        return 'RTSP';
    } else if (lowerUrl.includes('mjpg') || lowerUrl.includes('mjpeg') || lowerUrl.includes('.jpg')) {
        return 'HTTP_MJPEG';
    } else if (lowerUrl.startsWith('https://')) {
        return 'HTTPS';
    } else if (lowerUrl.startsWith('http://')) {
        return 'HTTP';
    }

    return 'UNKNOWN';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function truncateText(text, maxLength) {
    if (!text || text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

function hideElement(element) {
    if (element) element.style.display = 'none';
}

function showElement(element) {
    if (element) element.style.display = 'block';
}

// ===== LOADING AND ERROR STATES =====
function showLoading(message) {
    console.log(`⏳ ${message}`);
}

function showErrorState() {
    const grid = document.getElementById('cameraGrid');
    if (!grid) return;

    grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
            <i class="fas fa-exclamation-triangle text-warning" style="font-size: 4rem; margin-bottom: 1rem;"></i>
            <h5 class="text-muted mb-3">Có lỗi xảy ra khi tải dữ liệu camera</h5>
            <p class="text-muted mb-4">Vui lòng kiểm tra kết nối mạng và thử lại</p>
            <button class="btn btn-primary" onclick="loadCameras()">
                <i class="fas fa-refresh me-2"></i>Thử lại
            </button>
        </div>
    `;
}

function handleRoomLoadError() {
    const select = document.getElementById('maPhong');
    if (select) {
        select.innerHTML = '<option value="">Lỗi tải phòng học</option>';
    }
}

// ===== NOTIFICATION SYSTEM =====
function showNotification(message, type = 'info') {
    const iconMap = {
        'success': 'success',
        'error': 'error',
        'warning': 'warning',
        'info': 'info'
    };

    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: type === 'error' ? 'Lỗi!' : 'Thông báo',
            text: message,
            icon: iconMap[type] || 'info',
            confirmButtonText: 'Đóng',
            confirmButtonColor: '#1c6681',
            timer: type === 'success' ? 3000 : undefined,
            timerProgressBar: type === 'success'
        });
    } else {
        console.log(`${type.toUpperCase()}: ${message}`);
        if (type === 'error') {
            alert(`Lỗi: ${message}`);
        }
    }
}

// ===== GLOBAL FUNCTIONS FOR HTML ONCLICK =====
window.openAddCameraModal = function() {
    isEditMode = false;
    document.getElementById('cameraModalLabel').innerHTML = '<i class="fas fa-video me-2"></i>Thêm Camera mới';
    document.getElementById('cameraModal').querySelector('.modal-header').className = 'modal-header bg-primary text-white';
    resetForm();

    const modal = new bootstrap.Modal(document.getElementById('cameraModal'));
    modal.show();

    console.log('📝 Opening add camera modal');
};

window.editCamera = async function(id) {
    try {
        const camera = cameras.find(c => c.id == id);
        if (!camera) {
            throw new Error('Không tìm thấy camera');
        }

        isEditMode = true;
        document.getElementById('cameraModalLabel').innerHTML = '<i class="fas fa-edit me-2"></i>Chỉnh sửa Camera';
        document.getElementById('cameraModal').querySelector('.modal-header').className = 'modal-header bg-warning text-white';

        populateForm(camera);

        const modal = new bootstrap.Modal(document.getElementById('cameraModal'));
        modal.show();

        console.log(`✏️ Editing camera: ${camera.tenCamera}`);
    } catch (error) {
        console.error('❌ Error loading camera for edit:', error);
        showNotification('Không thể tải thông tin camera: ' + error.message, 'error');
    }
};

window.deleteCamera = function(id) {
    const camera = cameras.find(c => c.id == id);
    if (!camera) {
        console.log('❌ Camera not found for deletion:', id);
        return;
    }

    cameraToDelete = id;
    document.getElementById('deleteMessage').textContent =
        `Bạn có chắc chắn muốn xóa camera "${camera.tenCamera}" không?`;

    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();

    console.log(`🗑️ Deleting camera: ${camera.tenCamera}`);
};

window.viewFullscreen = function(cameraId) {
    const camera = cameras.find(c => c.id == cameraId);
    if (!camera) {
        console.log('❌ Camera not found for fullscreen:', cameraId);
        return;
    }

    console.log(`🔍 Opening fullscreen for camera: ${camera.tenCamera}`);

    document.getElementById('fullscreenModalLabel').innerHTML =
        `<i class="fas fa-video me-2"></i>${escapeHtml(camera.tenCamera)}`;

    const previewContainer = document.getElementById('fullscreenPreview');
    previewContainer.innerHTML = createStreamContainer(camera);

    // Initialize stream for fullscreen
    setTimeout(() => {
        initializeCameraStream(camera);
    }, 100);

    const modal = new bootstrap.Modal(document.getElementById('fullscreenModal'));
    modal.show();
};

window.testSingleCamera = async function(cameraId) {
    const camera = cameras.find(c => c.id == cameraId);
    if (!camera) return;

    console.log(`🔍 Testing single camera: ${camera.tenCamera}`);

    const cameraCard = document.querySelector(`[data-camera-id="${cameraId}"]`);
    if (cameraCard) {
        const statusIndicator = cameraCard.querySelector('.camera-status');
        statusIndicator.classList.add('testing', 'pulse');

        try {
            const testResult = await testCameraUrl(camera.ipAddress, camera.password);

            setTimeout(() => {
                statusIndicator.classList.remove('testing', 'pulse');
                statusIndicator.className = `camera-status ${testResult.success ? 'online' : 'offline'}`;
            }, 2000);

            showNotification(
                `Camera "${camera.tenCamera}": ${testResult.message}`,
                testResult.success ? 'success' : 'error'
            );

        } catch (error) {
            setTimeout(() => {
                statusIndicator.classList.remove('testing', 'pulse');
                statusIndicator.className = 'camera-status offline';
            }, 2000);

            showNotification(`Lỗi test camera "${camera.tenCamera}": ` + error.message, 'error');
        }
    }
};

window.retryStream = function(cameraId) {
    const camera = cameras.find(c => c.id == cameraId);
    if (!camera) return;

    console.log(`🔄 Retrying stream for camera: ${camera.tenCamera}`);

    // Reset and try again
    setTimeout(() => {
        initializeCameraStream(camera);
    }, 1000);
};

window.tryAlternativePlayer = function(rtspUrl) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Chọn ứng dụng để mở RTSP',
            html: `
                <div class="d-grid gap-2">
                    <button class="btn btn-outline-primary" onclick="Swal.close(); openInVLC('${rtspUrl}')">
                        <i class="fas fa-play me-2"></i>VLC Media Player
                        <small class="d-block text-muted">Khuyên dùng - Hỗ trợ tốt nhất</small>
                    </button>
                    <button class="btn btn-outline-success" onclick="Swal.close(); openInPotPlayer('${rtspUrl}')">
                        <i class="fas fa-video me-2"></i>PotPlayer
                        <small class="d-block text-muted">Cho Windows</small>
                    </button>
                    <button class="btn btn-outline-info" onclick="Swal.close(); openInMPC('${rtspUrl}')">
                        <i class="fas fa-desktop me-2"></i>MPC-HC
                        <small class="d-block text-muted">Media Player Classic</small>
                    </button>
                    <button class="btn btn-outline-warning" onclick="Swal.close(); openInFFPlay('${rtspUrl}')">
                        <i class="fas fa-terminal me-2"></i>FFPlay
                        <small class="d-block text-muted">Command line player</small>
                    </button>
                    <hr>
                    <button class="btn btn-outline-secondary" onclick="Swal.close(); showManualInstructions('${rtspUrl}')">
                        <i class="fas fa-book me-2"></i>Hướng dẫn thủ công
                    </button>
                </div>
            `,
            showConfirmButton: false,
            showCloseButton: true,
            width: 400
        });
    } else {
        // Fallback for no SweetAlert2
        openInVLC(rtspUrl);
    }
};

window.openInPotPlayer = function(rtspUrl) {
    // Try PotPlayer protocol
    const potUrl = `potplayer://${rtspUrl}`;
    const link = document.createElement('a');
    link.href = potUrl;
    link.click();

    showNotification('Đang mở trong PotPlayer...', 'info');
};

window.openInMPC = function(rtspUrl) {
    // Try MPC-HC protocol
    const mpcUrl = `mpc-hc://${rtspUrl}`;
    const link = document.createElement('a');
    link.href = mpcUrl;
    link.click();

    showNotification('Đang mở trong MPC-HC...', 'info');
};

window.openInFFPlay = function(rtspUrl) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'FFPlay Command',
            html: `
                <p>Mở Command Prompt/Terminal và chạy lệnh:</p>
                <div class="alert alert-dark">
                    <code>ffplay "${rtspUrl}"</code>
                    <button class="btn btn-sm btn-outline-light ms-2" onclick="copyToClipboard('ffplay \\"${rtspUrl}\\"')">
                        <i class="fas fa-copy"></i>
                    </button>
                </div>
                <small class="text-muted">FFmpeg cần được cài đặt trước</small>
            `,
            confirmButtonText: 'Đã hiểu'
        });
    } else {
        alert(`Chạy lệnh này trong terminal:\nffplay "${rtspUrl}"`);
    }
};

window.showManualInstructions = function(rtspUrl) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Hướng dẫn mở RTSP Stream',
            html: `
                <div class="text-start">
                    <div class="alert alert-info">
                        <strong>URL Stream:</strong><br>
                        <code class="text-break">${rtspUrl}</code>
                        <button class="btn btn-sm btn-outline-primary ms-2" onclick="copyToClipboard('${rtspUrl}')">
                            <i class="fas fa-copy"></i> Copy
                        </button>
                    </div>
                    
                    <h6><i class="fas fa-desktop me-2"></i>Trên Windows:</h6>
                    <ul>
                        <li><strong>VLC:</strong> Media → Open Network Stream → Paste URL</li>
                        <li><strong>PotPlayer:</strong> Ctrl+U → Paste URL</li>
                        <li><strong>MPC-HC:</strong> File → Open URL → Paste URL</li>
                    </ul>
                    
                    <h6><i class="fas fa-laptop me-2"></i>Trên macOS:</h6>
                    <ul>
                        <li><strong>VLC:</strong> File → Open Network → Paste URL</li>
                        <li><strong>IINA:</strong> File → Open URL → Paste URL</li>
                    </ul>
                    
                    <h6><i class="fas fa-mobile-alt me-2"></i>Trên điện thoại:</h6>
                    <ul>
                        <li><strong>VLC Mobile:</strong> Có sẵn trên iOS/Android</li>
                        <li><strong>MX Player:</strong> Android - hỗ trợ RTSP</li>
                    </ul>
                    
                    <div class="alert alert-warning mt-3">
                        <i class="fas fa-wifi me-2"></i>
                        <strong>Lưu ý:</strong> Đảm bảo thiết bị và camera cùng mạng hoặc có kết nối internet.
                    </div>
                </div>
            `,
            width: 600,
            confirmButtonText: 'Đã hiểu'
        });
    } else {
        alert(`URL RTSP: ${rtspUrl}\n\nMở trong VLC: Media → Open Network Stream → Dán URL`);
    }
};

window.copyToClipboard = function(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
            showNotification('Đã copy vào clipboard', 'success');
        }).catch(err => {
            console.error('Failed to copy:', err);
            fallbackCopyToClipboard(text);
        });
    } else {
        fallbackCopyToClipboard(text);
    }
};

// ===== CRUD OPERATIONS =====
async function saveCamera() {
    if (!validateForm()) {
        console.log('❌ Form validation failed');
        return;
    }

    try {
        const formData = collectFormData();
        const url = isEditMode ? `${CONFIG.apiEndpoints.cameras}/${formData.id}` : CONFIG.apiEndpoints.cameras;
        const method = isEditMode ? 'PUT' : 'POST';

        console.log(`💾 Saving camera (${method}):`, formData);

        showButtonLoading('saveButton', true);

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Có lỗi xảy ra khi lưu camera');
        }

        const result = await response.json();
        console.log('✅ Camera saved successfully:', result);

        // Close modal and refresh data
        bootstrap.Modal.getInstance(document.getElementById('cameraModal')).hide();

        showNotification(
            isEditMode ? 'Cập nhật camera thành công!' : 'Thêm camera thành công!',
            'success'
        );

        await loadCameras();

    } catch (error) {
        console.error('❌ Error saving camera:', error);
        showNotification(error.message || 'Có lỗi xảy ra khi lưu camera', 'error');
    } finally {
        showButtonLoading('saveButton', false);
    }
}

async function confirmDelete() {
    if (!cameraToDelete) return;

    try {
        console.log(`🗑️ Confirming delete camera ID: ${cameraToDelete}`);

        showButtonLoading('deleteButton', true);

        const response = await fetch(`${CONFIG.apiEndpoints.cameras}/${cameraToDelete}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Không thể xóa camera');
        }

        console.log('✅ Camera deleted successfully');

        bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();

        showNotification('Xóa camera thành công!', 'success');
        await loadCameras();

    } catch (error) {
        console.error('❌ Error deleting camera:', error);
        showNotification('Không thể xóa camera: ' + error.message, 'error');
    } finally {
        showButtonLoading('deleteButton', false);
        cameraToDelete = null;
    }
}

// ===== FORM HANDLING =====
function collectFormData() {
    const form = document.getElementById('cameraForm');
    const formData = new FormData(form);

    const data = {
        tenCamera: formData.get('tenCamera').trim(),
        ipAddress: formData.get('ipAddress').trim(),
        maPhong: formData.get('maPhong') || null,
        vungIn: formData.get('vungIn') ? formData.get('vungIn').trim() : null,
        vungOut: formData.get('vungOut') ? formData.get('vungOut').trim() : null,
        active: document.getElementById('isActive').checked
    };

    if (isEditMode) {
        data.id = parseInt(document.getElementById('cameraId').value);
    }

    // Clean up empty strings
    Object.keys(data).forEach(key => {
        if (data[key] === '') {
            data[key] = null;
        }
    });

    return data;
}

function populateForm(camera) {
    document.getElementById('cameraId').value = camera.id || '';
    document.getElementById('tenCamera').value = camera.tenCamera || '';
    document.getElementById('ipAddress').value = camera.ipAddress || '';
    document.getElementById('maPhong').value = camera.maPhong || '';
    document.getElementById('vungIn').value = camera.vungIn || '';
    document.getElementById('vungOut').value = camera.vungOut || '';
    document.getElementById('isActive').checked = camera.active !== false;

    console.log('📝 Form populated with camera data');
}

function resetForm() {
    const form = document.getElementById('cameraForm');
    if (!form) return;

    form.reset();
    form.classList.remove('was-validated');

    // Clear validation states
    form.querySelectorAll('.is-invalid').forEach(el => {
        el.classList.remove('is-invalid');
    });

    // Hide connection status
    const connectionStatus = document.getElementById('connectionStatus');
    if (connectionStatus) {
        connectionStatus.style.display = 'none';
    }

    // Reset hidden fields
    document.getElementById('cameraId').value = '';

    // Set defaults
    document.getElementById('isActive').checked = true;

    console.log('📝 Form reset to defaults');
}

function validateForm() {
    const form = document.getElementById('cameraForm');
    if (!form) return false;

    let isValid = form.checkValidity();
    form.classList.add('was-validated');

    // Custom validations
    const tenCamera = document.getElementById('tenCamera').value.trim();
    const ipAddress = document.getElementById('ipAddress').value.trim();
    const vungIn = document.getElementById('vungIn').value.trim();
    const vungOut = document.getElementById('vungOut').value.trim();

    // Camera name validation
    if (!tenCamera) {
        setFieldError('tenCamera', 'Vui lòng nhập tên camera');
        isValid = false;
    } else if (tenCamera.length < 3) {
        setFieldError('tenCamera', 'Tên camera phải có ít nhất 3 ký tự');
        isValid = false;
    }

    // IP Address/URL validation
    if (!ipAddress) {
        setFieldError('ipAddress', 'Vui lòng nhập địa chỉ IP hoặc URL');
        isValid = false;
    } else if (!validateUrlFormat(ipAddress)) {
        setFieldError('ipAddress', 'URL không hợp lệ. Phải bắt đầu với rtsp://, http:// hoặc https://');
        isValid = false;
    }

    // JSON validation for zones
    if (vungIn && !isValidJSON(vungIn)) {
        setFieldError('vungIn', 'Vùng vào phải có định dạng JSON hợp lệ');
        isValid = false;
    }

    if (vungOut && !isValidJSON(vungOut)) {
        setFieldError('vungOut', 'Vùng ra phải có định dạng JSON hợp lệ');
        isValid = false;
    }

    // Check for duplicate camera names (excluding current camera in edit mode)
    const existingCamera = cameras.find(c =>
        c.tenCamera.toLowerCase() === tenCamera.toLowerCase() &&
        (!isEditMode || c.id != parseInt(document.getElementById('cameraId').value))
    );

    if (existingCamera) {
        setFieldError('tenCamera', 'Tên camera đã tồn tại');
        isValid = false;
    }

    console.log(`📝 Form validation result: ${isValid ? 'VALID' : 'INVALID'}`);
    return isValid;
}

function validateUrlInput() {
    const ipAddress = document.getElementById('ipAddress').value.trim();
    if (ipAddress && !validateUrlFormat(ipAddress)) {
        setFieldError('ipAddress', 'URL không hợp lệ');
    } else {
        clearFieldError('ipAddress');
    }
}

function validateJSONInput(fieldId) {
    const field = document.getElementById(fieldId);
    if (!field) return;

    const value = field.value.trim();
    if (value && !isValidJSON(value)) {
        setFieldError(fieldId, 'JSON không hợp lệ');
    } else {
        clearFieldError(fieldId);
    }
}

function setFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    if (!field) return;

    field.classList.add('is-invalid');

    let feedback = field.parentNode.querySelector('.invalid-feedback');
    if (feedback) {
        feedback.textContent = message;
    }
}

function clearFieldError(fieldId) {
    const field = document.getElementById(fieldId);
    if (!field) return;

    field.classList.remove('is-invalid');
}

function validateUrlFormat(url) {
    if (!url) return false;
    const urlPattern = /^(rtsp|http|https):\/\/.+/i;
    return urlPattern.test(url);
}

function isValidJSON(str) {
    try {
        JSON.parse(str);
        return true;
    } catch (e) {
        return false;
    }
}

// ===== CONNECTION TESTING =====
async function testCameraConnection() {
    const ipAddress = document.getElementById('ipAddress').value.trim();

    if (!ipAddress) {
        showNotification('Vui lòng nhập URL stream trước khi test', 'warning');
        return;
    }

    console.log(`🔍 Testing camera connection: ${ipAddress}`);

    const statusDiv = document.getElementById('connectionStatus');
    statusDiv.style.display = 'inline-flex';
    statusDiv.className = 'connection-test testing';
    statusDiv.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang kiểm tra URL...';

    try {
        const testResult = await testCameraUrl(ipAddress);

        if (testResult.success) {
            statusDiv.className = 'connection-test success';
            statusDiv.innerHTML = `<i class="fas fa-check"></i> ${testResult.message}`;
            console.log('✅ Camera connection test successful');
        } else {
            statusDiv.className = 'connection-test failed';
            statusDiv.innerHTML = `<i class="fas fa-times"></i> ${testResult.message}`;
            console.log('❌ Camera connection test failed:', testResult.message);
        }

    } catch (error) {
        statusDiv.className = 'connection-test failed';
        statusDiv.innerHTML = '<i class="fas fa-times"></i> Lỗi kiểm tra URL';
        console.error('❌ Camera connection test error:', error);
    }
}

async function testCameraUrl(url) {
    return new Promise((resolve) => {
        const timeout = 5000; // 5 seconds timeout

        // Basic URL validation
        if (!validateUrlFormat(url)) {
            resolve({
                success: false,
                message: 'URL không hợp lệ'
            });
            return;
        }

        const streamType = getStreamType(url);

        if (streamType === 'RTSP') {
            // For RTSP, validate format and extract info
            testRTSPStream(url, timeout, resolve);
        } else if (streamType === 'HTTP' || streamType === 'HTTPS') {
            // Test HTTP streams by trying to load
            testHTTPStream(url, timeout, resolve);
        } else {
            resolve({
                success: false,
                message: 'Loại stream không được hỗ trợ'
            });
        }
    });
}

function testHTTPStream(url, timeout, resolve) {
    let resolved = false;

    const resolveOnce = (result) => {
        if (!resolved) {
            resolved = true;
            resolve(result);
        }
    };

    // Try as image first (for MJPEG streams)
    const img = new Image();
    img.onload = () => {
        resolveOnce({
            success: true,
            message: 'Kết nối thành công - HTTP/MJPEG stream hoạt động'
        });
    };

    img.onerror = () => {
        // If image fails, try as video
        testVideoStream(url, resolveOnce);
    };

    // Set timeout
    setTimeout(() => {
        resolveOnce({
            success: false,
            message: 'Timeout - Không thể kết nối trong thời gian cho phép'
        });
    }, timeout);

    // Add timestamp to prevent caching
    const separator = url.includes('?') ? '&' : '?';
    img.src = `${url}${separator}_test=${Date.now()}`;
}

function testVideoStream(url, resolve) {
    const video = document.createElement('video');
    video.muted = true;
    video.preload = 'metadata';

    video.onloadedmetadata = () => {
        resolve({
            success: true,
            message: 'Kết nối thành công - Video stream hoạt động'
        });
    };

    video.onerror = () => {
        resolve({
            success: false,
            message: 'URL không thể truy cập hoặc không phải stream hợp lệ'
        });
    };

    video.src = url;
}

function testRTSPStream(url, timeout, resolve) {
    // For RTSP, we can only validate URL format
    // Real testing would require backend support

    setTimeout(() => {
        // Check if URL has authentication
        const hasAuth = url.includes('@');
        const message = hasAuth ?
            'URL RTSP hợp lệ với authentication - Sẽ thử kết nối khi lưu' :
            'URL RTSP hợp lệ - Sẽ thử kết nối khi lưu';

        resolve({
            success: true,
            message: message
        });
    }, 1500); // Simulate some processing time
}

async function testAllConnections() {
    if (cameras.length === 0) {
        showNotification('Không có camera nào để test', 'info');
        return;
    }

    console.log(`🔍 Testing all ${cameras.length} cameras`);

    showNotification('Đang kiểm tra kết nối tất cả camera...', 'info');

    // Test all cameras in parallel with limit
    const promises = cameras.map(camera => testCameraUrl(camera.ipAddress, camera.password));

    try {
        const results = await Promise.allSettled(promises);
        const successCount = results.filter(r => r.status === 'fulfilled' && r.value.success).length;

        showNotification(
            `Hoàn thành: ${successCount}/${cameras.length} camera hoạt động tốt`,
            successCount === cameras.length ? 'success' : 'warning'
        );

        // Refresh camera data after testing
        await loadCameras();

    } catch (error) {
        console.error('❌ Error testing all cameras:', error);
        showNotification('Lỗi khi test camera', 'error');
    }
}

// ===== ADVANCED FEATURES =====
function exportCameraConfig() {
    if (cameras.length === 0) {
        showNotification('Không có camera nào để xuất', 'warning');
        return;
    }

    console.log('📤 Exporting camera configuration');

    const config = {
        exportDate: new Date().toISOString(),
        version: '1.0',
        cameras: cameras.map(camera => ({
            tenCamera: camera.tenCamera,
            ipAddress: camera.ipAddress,
            maPhong: camera.maPhong,
            vungIn: camera.vungIn,
            vungOut: camera.vungOut,
            active: camera.active,
            hasPassword: !!camera.password
        }))
    };

    const dataStr = JSON.stringify(config, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });

    const link = document.createElement('a');
    link.href = URL.createObjectURL(dataBlob);
    link.download = `camera-config-${new Date().toISOString().split('T')[0]}.json`;
    link.style.display = 'none';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    showNotification('Xuất cấu hình camera thành công!', 'success');
}

function toggleAllCameras(active) {
    if (cameras.length === 0) {
        showNotification('Không có camera nào', 'info');
        return;
    }

    const action = active ? 'bật' : 'tắt';
    console.log(`🔄 Toggle all cameras: ${action}`);

    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: `${active ? 'Bật' : 'Tắt'} tất cả camera?`,
            text: `Bạn có chắc chắn muốn ${action} tất cả ${cameras.length} camera không?`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: `${active ? 'Bật' : 'Tắt'} tất cả`,
            cancelButtonText: 'Hủy',
            confirmButtonColor: active ? '#28a745' : '#dc3545'
        }).then(async (result) => {
            if (result.isConfirmed) {
                await performBatchToggle(active);
            }
        });
    } else {
        if (confirm(`Bạn có chắc chắn muốn ${action} tất cả ${cameras.length} camera không?`)) {
            performBatchToggle(active);
        }
    }
}

async function performBatchToggle(active) {
    try {
        const promises = cameras.map(camera =>
            fetch(`${CONFIG.apiEndpoints.cameras}/${camera.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...camera, active })
            })
        );

        const results = await Promise.allSettled(promises);
        const successCount = results.filter(r => r.status === 'fulfilled' && r.value.ok).length;

        if (successCount === cameras.length) {
            showNotification(`Đã ${active ? 'bật' : 'tắt'} tất cả camera thành công!`, 'success');
        } else {
            showNotification(`Chỉ ${active ? 'bật' : 'tắt'} được ${successCount}/${cameras.length} camera`, 'warning');
        }

        await loadCameras();

    } catch (error) {
        console.error('❌ Error in batch toggle:', error);
        showNotification(`Lỗi khi ${active ? 'bật' : 'tắt'} camera`, 'error');
    }
}

function showRTSPGuide() {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Hướng dẫn sử dụng RTSP Camera',
            html: `
                <div class="text-start">
                    <h6><i class="fas fa-info-circle me-2"></i>Định dạng URL RTSP</h6>
                    <div class="alert alert-info">
                        <code>rtsp://[username:password@]host:port/path</code>
                    </div>
                    
                    <h6 class="mt-3"><i class="fas fa-examples me-2"></i>Ví dụ thực tế</h6>
                    <ul>
                        <li><code>rtsp://admin:BJFEIG@192.168.101.217:554/ch1/main</code></li>
                        <li><code>rtsp://admin:password@192.168.1.100:554/stream</code></li>
                        <li><code>rtsp://192.168.1.100:554/live</code> (không auth)</li>
                    </ul>
                    
                    <h6 class="mt-3"><i class="fas fa-cog me-2"></i>Cách hoạt động</h6>
                    <p>1. <strong>Tự động:</strong> Hệ thống sẽ thử hiển thị stream trực tiếp trên web</p>
                    <p>2. <strong>VLC Fallback:</strong> Nếu không hiển thị được, có nút mở trong VLC</p>
                    <p>3. <strong>Copy URL:</strong> Copy URL để sử dụng trong ứng dụng khác</p>
                    
                    <h6 class="mt-3"><i class="fas fa-exclamation-triangle me-2"></i>Lưu ý quan trọng</h6>
                    <ul>
                        <li>RTSP stream có thể không hiển thị trực tiếp trên browser</li>
                        <li>VLC Media Player là lựa chọn tốt nhất để xem RTSP</li>
                        <li>Đảm bảo camera và máy tính cùng mạng</li>
                        <li>Port mặc định RTSP là 554</li>
                        <li>Kiểm tra firewall và router settings</li>
                    </ul>
                    
                    <div class="alert alert-warning mt-3">
                        <strong>Bảo mật:</strong> Đổi password mặc định của camera. 
                        Tránh để camera trực tiếp trên internet.
                    </div>
                </div>
            `,
            width: 700,
            confirmButtonText: 'Đã hiểu',
            showCloseButton: true
        });
    } else {
        alert('Hướng dẫn RTSP:\n\nĐịnh dạng: rtsp://[username:password@]host:port/path\nVí dụ: rtsp://admin:BJFEIG@192.168.101.217:554/ch1/main');
    }
}

function showVLCInstructions(rtspUrl) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Mở stream trong VLC',
            html: `
                <div class="text-start">
                    <p><strong>Cách 1: Tự động (nếu VLC đã cài đặt)</strong></p>
                    <p>Stream sẽ tự động mở trong VLC nếu có cài đặt.</p>
                    
                    <p class="mt-3"><strong>Cách 2: Thủ công</strong></p>
                    <ol>
                        <li>Mở VLC Media Player</li>
                        <li>Chọn Media > Open Network Stream (Ctrl+N)</li>
                        <li>Dán URL sau vào ô Network URL:</li>
                    </ol>
                    <div class="alert alert-info">
                        <code class="text-break">${rtspUrl}</code>
                        <button class="btn btn-sm btn-outline-primary ms-2" onclick="copyToClipboard('${rtspUrl}')">
                            <i class="fas fa-copy"></i> Copy
                        </button>
                    </div>
                    <p>4. Nhấn Play để xem stream</p>
                    
                    <div class="alert alert-success">
                        <strong>Tip:</strong> VLC có thể mở hầu hết các định dạng stream, 
                        bao gồm RTSP, HTTP, và MJPEG.
                    </div>
                </div>
            `,
            width: 600,
            confirmButtonText: 'Đã hiểu'
        });
    } else {
        alert(`Mở VLC và dán URL này vào Network Stream:\n\n${rtspUrl}`);
    }
}

function fallbackCopyToClipboard(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        document.execCommand('copy');
        showNotification('Đã copy vào clipboard', 'success');
    } catch (err) {
        console.error('Fallback copy failed:', err);
        showNotification('Không thể copy tự động. Vui lòng copy thủ công.', 'warning');
    }

    document.body.removeChild(textArea);
}

function showButtonLoading(buttonId, loading) {
    const button = document.getElementById(buttonId);
    if (!button) return;

    if (loading) {
        button.disabled = true;
        button.dataset.originalText = button.innerHTML;
        button.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xử lý...';
    } else {
        button.disabled = false;
        button.innerHTML = button.dataset.originalText || button.innerHTML;
    }
}

// ===== INITIALIZATION COMPLETE =====
console.log('📹 Camera management JavaScript loaded successfully!');
console.log('🎯 RTSP URL example supported: rtsp://admin:BJFEIG@192.168.101.217:554/ch1/main');

// ===== STREAMING FUNCTIONS =====
let activeStreams = new Map(); // Track active HLS instances
let streamingCameras = new Set(); // Track which cameras are streaming

// Initialize HLS streaming for a camera
function initializeCameraStream(camera) {
    if (!camera.active || !camera.ipAddress) return;

    const videoContainer = document.querySelector(`[data-camera-id="${camera.id}"] .camera-preview`);
    if (!videoContainer) return;

    console.log(`🎥 Initializing stream for camera: ${camera.tenCamera}`);

    // Create video element if not exists
    let videoElement = videoContainer.querySelector('video');
    if (!videoElement) {
        videoElement = document.createElement('video');
        videoElement.className = 'camera-stream';
        videoElement.controls = true;
        videoElement.muted = true;
        videoElement.autoplay = true;
        videoElement.style.width = '100%';
        videoElement.style.height = '100%';
        videoElement.style.objectFit = 'cover';

        // Clear existing content
        videoContainer.innerHTML = '';
        videoContainer.appendChild(videoElement);
    }

    // Start streaming
    startCameraStream(camera.id, camera.ipAddress, videoElement);
}

// Start RTSP stream using our streaming service
async function startCameraStream(cameraId, rtspUrl, videoElement) {
    try {
        showStreamStatus(cameraId, 'connecting', 'Đang kết nối...');

        // Call our streaming API
        const response = await fetch('/api/stream/start', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({rtspUrl: rtspUrl})
        });

        const data = await response.json();

        if (data.status === 'error') {
            throw new Error(data.message);
        }

        // Wait for HLS segments
        setTimeout(() => {
            playHLSStream(cameraId, data.hlsUrl, videoElement);
        }, 3000);

        // Store stream info
        activeStreams.set(cameraId, {
            streamId: data.streamId,
            hlsUrl: data.hlsUrl,
            hls: null
        });

        streamingCameras.add(cameraId);
        console.log(`✅ Stream started for camera ${cameraId}`);

    } catch (error) {
        console.error(`❌ Stream failed for camera ${cameraId}:`, error);
        showStreamStatus(cameraId, 'error', 'Lỗi kết nối');
        showNotification(`Lỗi stream camera: ${error.message}`, 'error');
    }
}

// Play HLS stream
function playHLSStream(cameraId, hlsUrl, videoElement) {
    if (Hls.isSupported()) {
        const hls = new Hls({
            debug: false,
            enableWorker: false,
            lowLatencyMode: true
        });

        hls.loadSource(hlsUrl);
        hls.attachMedia(videoElement);

        hls.on(Hls.Events.MANIFEST_PARSED, () => {
            console.log(`📺 HLS stream ready for camera ${cameraId}`);
            showStreamStatus(cameraId, 'streaming', 'Đang phát');
            videoElement.play().catch(e => console.log('Autoplay prevented:', e));
        });

        hls.on(Hls.Events.ERROR, (event, data) => {
            console.error(`❌ HLS error for camera ${cameraId}:`, data);
            if (data.fatal) {
                showStreamStatus(cameraId, 'error', 'Lỗi phát');
            }
        });

        // Store HLS instance
        const streamInfo = activeStreams.get(cameraId);
        if (streamInfo) {
            streamInfo.hls = hls;
        }

    } else if (videoElement.canPlayType('application/vnd.apple.mpegurl')) {
        // Native HLS support (Safari)
        videoElement.src = hlsUrl;
        videoElement.addEventListener('loadedmetadata', () => {
            console.log(`📺 Native HLS loaded for camera ${cameraId}`);
            showStreamStatus(cameraId, 'streaming', 'Đang phát');
        });
    } else {
        showStreamStatus(cameraId, 'error', 'HLS không hỗ trợ');
        showNotification('Trình duyệt không hỗ trợ HLS streaming', 'error');
    }
}

// Stop camera stream
function stopCameraStream(cameraId) {
    const streamInfo = activeStreams.get(cameraId);
    if (streamInfo) {
        // Stop HLS
        if (streamInfo.hls) {
            streamInfo.hls.destroy();
        }

        // Stop backend stream
        fetch(`/api/stream/stop/${streamInfo.streamId}`, {method: 'POST'})
            .catch(e => console.error('Error stopping stream:', e));

        activeStreams.delete(cameraId);
        streamingCameras.delete(cameraId);

        // Reset video container
        const videoContainer = document.querySelector(`[data-camera-id="${cameraId}"] .camera-preview`);
        if (videoContainer) {
            videoContainer.innerHTML = createStreamContainer({id: cameraId, active: false});
        }

        showStreamStatus(cameraId, 'offline', 'Đã dừng');
        console.log(`⏹️ Stream stopped for camera ${cameraId}`);
    }
}

// Show stream status
function showStreamStatus(cameraId, status, message) {
    const statusElement = document.querySelector(`[data-camera-id="${cameraId}"] .connection-test`);
    if (statusElement) {
        statusElement.className = `connection-test ${status}`;
        statusElement.innerHTML = `<i class="fas ${getStatusIcon(status)}"></i> ${message}`;
    }
}

function getStatusIcon(status) {
    const icons = {
        connecting: 'fa-spinner fa-spin',
        streaming: 'fa-play-circle',
        error: 'fa-exclamation-triangle',
        offline: 'fa-stop-circle'
    };
    return icons[status] || 'fa-question-circle';
}

// Test single camera connection
window.testSingleCamera = async function(cameraId) {
    const camera = cameras.find(c => c.id == cameraId);
    if (!camera) return;

    console.log(`🔍 Testing camera: ${camera.tenCamera}`);
    showStreamStatus(cameraId, 'connecting', 'Đang test...');

    try {
        const response = await fetch('/api/stream/test-rtsp', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({rtspUrl: camera.ipAddress})
        });

        const data = await response.json();

        if (data.success) {
            showStreamStatus(cameraId, 'streaming', 'Kết nối OK');
            showNotification(`✅ Camera "${camera.tenCamera}": Kết nối thành công`, 'success');
        } else {
            showStreamStatus(cameraId, 'error', 'Kết nối lỗi');
            showNotification(`❌ Camera "${camera.tenCamera}": ${data.message}`, 'error');
        }

    } catch (error) {
        showStreamStatus(cameraId, 'error', 'Test lỗi');
        showNotification(`❌ Test camera "${camera.tenCamera}": ${error.message}`, 'error');
    }
};

// Start streaming for a camera
window.startStreamingCamera = function(cameraId) {
    const camera = cameras.find(c => c.id == cameraId);
    if (!camera) return;

    if (streamingCameras.has(cameraId)) {
        showNotification('Camera đã đang streaming', 'warning');
        return;
    }

    initializeCameraStream(camera);
};

// Stop streaming for a camera
window.stopStreamingCamera = function(cameraId) {
    if (!streamingCameras.has(cameraId)) {
        showNotification('Camera không đang streaming', 'warning');
        return;
    }

    stopCameraStream(cameraId);
    showNotification('Đã dừng stream camera', 'success');
};

// Modify existing createCameraCard to include streaming controls
const originalCreateCameraCard = createCameraCard;
createCameraCard = function(camera, index = 0) {
    const cardHTML = originalCreateCameraCard(camera, index);

    // Add streaming buttons to camera controls
    const streamingControls = `
        <button class="btn btn-sm btn-success" onclick="startStreamingCamera(${camera.id})" 
                title="Bắt đầu streaming">
            <i class="fas fa-play"></i>
        </button>
        <button class="btn btn-sm btn-danger" onclick="stopStreamingCamera(${camera.id})" 
                title="Dừng streaming">
            <i class="fas fa-stop"></i>
        </button>
    `;

    // Insert streaming controls before existing controls
    return cardHTML.replace(
        '<button class="btn btn-sm btn-light" onclick="viewFullscreen(',
        streamingControls + '<button class="btn btn-sm btn-light" onclick="viewFullscreen('
    );
};

// Auto-start streams for active cameras when page loads
function autoStartStreams() {
    cameras.forEach(camera => {
        if (camera.active && camera.ipAddress) {
            // Auto-start streaming after a delay
            setTimeout(() => {
                initializeCameraStream(camera);
            }, 1000 + (Math.random() * 2000)); // Stagger starts
        }
    });
}

// Test all cameras
window.testAllConnections = async function() {
    if (cameras.length === 0) {
        showNotification('Không có camera nào để test', 'info');
        return;
    }

    console.log(`🔍 Testing all ${cameras.length} cameras`);
    showNotification('Đang kiểm tra kết nối tất cả camera...', 'info');

    let successCount = 0;
    const testPromises = cameras.map(async (camera, index) => {
        await new Promise(resolve => setTimeout(resolve, index * 500)); // Stagger tests

        try {
            const response = await fetch('/api/stream/test-rtsp', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({rtspUrl: camera.ipAddress})
            });

            const data = await response.json();

            if (data.success) {
                successCount++;
                showStreamStatus(camera.id, 'streaming', 'Test OK');
            } else {
                showStreamStatus(camera.id, 'error', 'Test failed');
            }
        } catch (error) {
            showStreamStatus(camera.id, 'error', 'Test error');
        }
    });

    await Promise.all(testPromises);

    showNotification(
        `Hoàn thành: ${successCount}/${cameras.length} camera hoạt động tốt`,
        successCount === cameras.length ? 'success' : 'warning'
    );

    updateStatistics();
};

// Modify existing loadCameras to include auto-start
const originalLoadCameras = loadCameras;
loadCameras = async function() {
    await originalLoadCameras();

    // Auto-start streams after loading cameras
    setTimeout(() => {
        autoStartStreams();
    }, 2000);
};

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    activeStreams.forEach((streamInfo, cameraId) => {
        if (streamInfo.hls) {
            streamInfo.hls.destroy();
        }
    });
});

console.log('🎥 Camera streaming system loaded successfully!');