class CameraManager {
    constructor() {
        this.cameras = [];
        this.rooms = [];
        this.filteredCameras = [];
        this.currentView = 'grid';
        this.currentStreamId = null;
        this.hls = null;
        this.currentPage = 1;
        this.itemsPerPage = 12;

        this.init();
    }

    async init() {
        await this.loadRooms();
        await this.loadCameras();
        this.setupEventListeners();
        this.render();
    }

    setupEventListeners() {
        // Search input
        document.getElementById('searchInput').addEventListener('input', () => {
            this.filterCameras();
        });

        // Filter selects
        document.getElementById('roomFilter').addEventListener('change', () => {
            this.filterCameras();
        });

        document.getElementById('statusFilter').addEventListener('change', () => {
            this.filterCameras();
        });

        // Modal events
        document.getElementById('cameraModal').addEventListener('hidden.bs.modal', () => {
            this.resetForm();
        });

        document.getElementById('streamModal').addEventListener('hidden.bs.modal', () => {
            this.stopCurrentStream();
        });

        // Sidebar toggle
        if (typeof Common !== 'undefined' && Common.sidebar) {
            Common.sidebar.init();
        }
    }

    async loadRooms() {
        try {
            const response = await fetch('/api/phonghoc');

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            // Debug: log raw data
            console.log('Raw rooms data from API:', data);

            // Xử lý cấu trúc pagination - data nằm trong field 'content'
            let roomsData = [];
            if (data && data.content && Array.isArray(data.content)) {
                roomsData = data.content;
            } else if (Array.isArray(data)) {
                // Fallback nếu trả về array trực tiếp
                roomsData = data;
            }

            // Lọc phòng học active và có trangThai AVAILABLE
            this.rooms = roomsData.filter(room => {
                // Kiểm tra isActive = true và trangThai AVAILABLE
                return room.isActive === true &&
                    (room.trangThai === 'AVAILABLE' || room.trangThai === 'INACTIVE');
            });

            console.log('Filtered active rooms:', this.rooms);

            const roomSelects = document.querySelectorAll('#roomSelect, #roomFilter');
            roomSelects.forEach(select => {
                const currentValue = select.value;

                // Clear existing options
                select.innerHTML = '';

                // Add default option
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = select.id === 'roomFilter' ?
                    'Tất cả phòng' : 'Chọn phòng học';
                select.appendChild(defaultOption);

                if (this.rooms && this.rooms.length > 0) {
                    this.rooms.forEach(room => {
                        const option = document.createElement('option');
                        option.value = room.maPhong;

                        // Hiển thị tên phòng kèm loại phòng và trạng thái
                        let displayText = room.tenPhong || room.maPhong;
                        if (room.loaiPhongDisplay) {
                            displayText += ` (${room.loaiPhongDisplay})`;
                        }
                        if (room.trangThaiDisplay && room.trangThai !== 'AVAILABLE') {
                            displayText += ` - ${room.trangThaiDisplay}`;
                        }

                        option.textContent = displayText;

                        // Disable nếu đang bảo trì
                        if (room.trangThai === 'MAINTENANCE') {
                            option.disabled = true;
                            option.style.color = '#999';
                        }

                        select.appendChild(option);
                    });
                } else {
                    // Nếu không có phòng nào
                    const noRoomOption = document.createElement('option');
                    noRoomOption.value = '';
                    noRoomOption.textContent = 'Không có phòng học khả dụng';
                    noRoomOption.disabled = true;
                    select.appendChild(noRoomOption);
                }

                // Restore previous value if valid
                if (currentValue && Array.from(select.options).some(opt => opt.value === currentValue)) {
                    select.value = currentValue;
                }
            });

            console.log(`Successfully loaded ${this.rooms.length} available rooms`);

            // Hiển thị thông báo nếu có phòng
            if (this.rooms.length > 0) {
                this.showAlert(`Đã tải ${this.rooms.length} phòng học khả dụng`, 'success');
            }

        } catch (error) {
            console.error('Error loading rooms:', error);
            this.rooms = [];
            this.showAlert('Lỗi khi tải danh sách phòng học: ' + error.message, 'danger');

            // Hiển thị thông báo lỗi trong dropdown
            const roomSelects = document.querySelectorAll('#roomSelect, #roomFilter');
            roomSelects.forEach(select => {
                select.innerHTML = '';

                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = select.id === 'roomFilter' ?
                    'Tất cả phòng' : 'Chọn phòng học';
                select.appendChild(defaultOption);

                const errorOption = document.createElement('option');
                errorOption.value = '';
                errorOption.textContent = 'Lỗi tải danh sách phòng';
                errorOption.disabled = true;
                errorOption.style.color = '#dc3545';
                select.appendChild(errorOption);
            });
        }
    }

    async loadCameras() {
        try {
            const response = await fetch('/api/cameras');
            const data = await response.json();

            this.cameras = Array.isArray(data) ? data : [];
            console.log('Loaded cameras:', this.cameras);

            this.filterCameras();
        } catch (error) {
            console.error('Error loading cameras:', error);
            this.cameras = [];
            this.showAlert('Lỗi khi tải danh sách camera: ' + error.message, 'danger');
        }
    }

    filterCameras() {
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();
        const roomFilter = document.getElementById('roomFilter').value;
        const statusFilter = document.getElementById('statusFilter').value;

        this.filteredCameras = this.cameras.filter(camera => {
            const matchesSearch = !searchTerm ||
                camera.tenCamera.toLowerCase().includes(searchTerm) ||
                (camera.ipAddress && camera.ipAddress.toLowerCase().includes(searchTerm)) ||
                (camera.maPhong && camera.maPhong.toLowerCase().includes(searchTerm));

            const matchesRoom = !roomFilter || camera.maPhong === roomFilter;

            const matchesStatus = !statusFilter || camera.active.toString() === statusFilter;

            return matchesSearch && matchesRoom && matchesStatus;
        });

        this.currentPage = 1;
        this.render();
    }

    toggleView(view) {
        this.currentView = view;

        document.getElementById('gridViewBtn').classList.toggle('active', view === 'grid');
        document.getElementById('tableViewBtn').classList.toggle('active', view === 'table');

        document.getElementById('gridView').classList.toggle('d-none', view !== 'grid');
        document.getElementById('tableView').classList.toggle('d-none', view !== 'table');

        this.render();
    }

    render() {
        if (this.filteredCameras.length === 0) {
            this.showEmptyState();
            return;
        }

        this.hideEmptyState();

        if (this.currentView === 'grid') {
            this.renderGrid();
        } else {
            this.renderTable();
        }

        this.renderPagination();
    }

    renderGrid() {
        const container = document.getElementById('gridView');
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const pageItems = this.filteredCameras.slice(startIndex, endIndex);

        container.innerHTML = pageItems.map(camera => this.createCameraCard(camera)).join('');
    }

    renderTable() {
        const tbody = document.getElementById('cameraTableBody');
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const pageItems = this.filteredCameras.slice(startIndex, endIndex);

        tbody.innerHTML = pageItems.map(camera => this.createCameraRow(camera)).join('');
    }

    createCameraCard(camera) {
        const room = (Array.isArray(this.rooms) && this.rooms.length > 0) ?
            this.rooms.find(r => r.maPhong === camera.maPhong) : null;
        const roomName = room ?
            (room.tenPhong || room.maPhong) + (room.loaiPhongDisplay ? ` (${room.loaiPhongDisplay})` : '') :
            'Chưa gán phòng';

        return `
            <div class="col-md-4 col-lg-3 mb-4">
                <div class="camera-card h-100">
                    <div class="camera-preview">
                        <i class="fas fa-video fa-3x"></i>
                        <div class="position-absolute top-0 end-0 p-2">
                            <span class="status-badge ${camera.active ? 'status-active' : 'status-inactive'}">
                                ${camera.active ? 'Hoạt động' : 'Tạm dừng'}
                            </span>
                        </div>
                    </div>
                    <div class="card-body">
                        <h6 class="card-title">${camera.tenCamera}</h6>
                        <p class="card-text text-muted small mb-2">
                            <i class="fas fa-door-open me-1"></i>${roomName}
                        </p>
                        <p class="card-text text-muted small">
                            <i class="fas fa-network-wired me-1"></i>
                            ${camera.ipAddress ? (camera.ipAddress.length > 30 ? camera.ipAddress.substring(0, 30) + '...' : camera.ipAddress) : 'Chưa cấu hình'}
                        </p>
                    </div>
                    <div class="card-footer bg-transparent">
                        <div class="action-buttons">
                            <button class="btn btn-action btn-stream" onclick="cameraManager.viewCamera(${camera.id})"
                                    title="Xem camera">
                                <i class="fas fa-play"></i>
                            </button>
                            <button class="btn btn-action btn-view" onclick="cameraManager.forceViewCamera(${camera.id})"
                                    title="Force view camera">
                                <i class="fas fa-bolt"></i>
                            </button>
                            <button class="btn btn-action btn-edit" onclick="cameraManager.editCamera(${camera.id})"
                                    title="Sửa">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-action btn-delete" onclick="cameraManager.deleteCamera(${camera.id})"
                                    title="Xóa">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    createCameraRow(camera) {
        const room = (Array.isArray(this.rooms) && this.rooms.length > 0) ?
            this.rooms.find(r => r.maPhong === camera.maPhong) : null;
        const roomName = room ?
            (room.tenPhong || room.maPhong) + (room.loaiPhongDisplay ? ` (${room.loaiPhongDisplay})` : '') :
            'Chưa gán phòng';

        return `
            <tr>
                <td>#${camera.id}</td>
                <td>
                    <strong>${camera.tenCamera}</strong>
                </td>
                <td>${roomName}</td>
                <td>
                    <code class="small">${camera.ipAddress || 'Chưa cấu hình'}</code>
                </td>
                <td>
                    <span class="status-badge ${camera.active ? 'status-active' : 'status-inactive'}">
                        ${camera.active ? 'Hoạt động' : 'Tạm dừng'}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-action btn-stream" onclick="cameraManager.viewCamera(${camera.id})"
                                title="Xem camera">
                            <i class="fas fa-play"></i>
                        </button>
                        <button class="btn btn-action btn-view" onclick="cameraManager.forceViewCamera(${camera.id})"
                                title="Force view camera">
                            <i class="fas fa-bolt"></i>
                        </button>
                        <button class="btn btn-action btn-edit" onclick="cameraManager.editCamera(${camera.id})"
                                title="Sửa">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-action btn-delete" onclick="cameraManager.deleteCamera(${camera.id})"
                                title="Xóa">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    renderPagination() {
        const totalPages = Math.ceil(this.filteredCameras.length / this.itemsPerPage);
        const container = document.getElementById('paginationContainer');
        const pagination = document.getElementById('pagination');

        if (totalPages <= 1) {
            container.classList.add('d-none');
            return;
        }

        container.classList.remove('d-none');

        let paginationHTML = '';

        // Previous button
        paginationHTML += `
            <li class="page-item ${this.currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="cameraManager.changePage(${this.currentPage - 1})">
                    <i class="fas fa-chevron-left"></i>
                </a>
            </li>
        `;

        // Page numbers
        for (let i = 1; i <= totalPages; i++) {
            if (i === 1 || i === totalPages || (i >= this.currentPage - 2 && i <= this.currentPage + 2)) {
                paginationHTML += `
                    <li class="page-item ${i === this.currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" onclick="cameraManager.changePage(${i})">${i}</a>
                    </li>
                `;
            } else if (i === this.currentPage - 3 || i === this.currentPage + 3) {
                paginationHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        // Next button
        paginationHTML += `
            <li class="page-item ${this.currentPage === totalPages ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="cameraManager.changePage(${this.currentPage + 1})">
                    <i class="fas fa-chevron-right"></i>
                </a>
            </li>
        `;

        pagination.innerHTML = paginationHTML;
    }

    changePage(page) {
        const totalPages = Math.ceil(this.filteredCameras.length / this.itemsPerPage);
        if (page < 1 || page > totalPages) return;

        this.currentPage = page;
        this.render();
    }

    showEmptyState() {
        document.getElementById('gridView').innerHTML = '';
        document.getElementById('cameraTableBody').innerHTML = '';
        document.getElementById('emptyState').classList.remove('d-none');
        document.getElementById('paginationContainer').classList.add('d-none');
    }

    hideEmptyState() {
        document.getElementById('emptyState').classList.add('d-none');
    }

    showAddModal() {
        document.getElementById('modalTitle').textContent = 'Thêm Camera';
        document.getElementById('cameraId').value = '';
        this.resetForm();
        new bootstrap.Modal(document.getElementById('cameraModal')).show();
    }

    async editCamera(id) {
        try {
            const response = await fetch(`/api/cameras/${id}`);
            const camera = await response.json();

            document.getElementById('modalTitle').textContent = 'Sửa Camera';
            document.getElementById('cameraId').value = camera.id;
            document.getElementById('cameraName').value = camera.tenCamera;
            document.getElementById('roomSelect').value = camera.maPhong || '';
            document.getElementById('ipAddress').value = camera.ipAddress || '';
            document.getElementById('cameraPassword').value = camera.password || '';
            document.getElementById('cameraStatus').value = camera.active.toString();
            document.getElementById('vungIn').value = camera.vungIn || '';
            document.getElementById('vungOut').value = camera.vungOut || '';

            new bootstrap.Modal(document.getElementById('cameraModal')).show();
        } catch (error) {
            console.error('Error loading camera:', error);
            this.showAlert('Lỗi khi tải thông tin camera', 'danger');
        }
    }

    async deleteCamera(id) {
        const camera = this.cameras.find(c => c.id === id);
        if (!camera) return;

        if (!confirm(`Bạn có chắc chắn muốn xóa camera "${camera.tenCamera}"?`)) {
            return;
        }

        try {
            const response = await fetch(`/api/cameras/${id}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                this.showAlert('Xóa camera thành công', 'success');
                await this.loadCameras();
            } else {
                throw new Error('Failed to delete camera');
            }
        } catch (error) {
            console.error('Error deleting camera:', error);
            this.showAlert('Lỗi khi xóa camera', 'danger');
        }
    }

    async saveCamera() {
        try {
            const formData = this.getFormData();
            if (!this.validateForm(formData)) return;

            const isEdit = !!document.getElementById('cameraId').value;
            const url = isEdit ? `/api/cameras/${formData.id}` : '/api/cameras';
            const method = isEdit ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                this.showAlert(`${isEdit ? 'Cập nhật' : 'Thêm'} camera thành công`, 'success');
                bootstrap.Modal.getInstance(document.getElementById('cameraModal')).hide();
                await this.loadCameras();
            } else {
                const errorData = await response.text();
                throw new Error(errorData || 'Failed to save camera');
            }
        } catch (error) {
            console.error('Error saving camera:', error);
            this.showAlert('Lỗi khi lưu camera: ' + error.message, 'danger');
        }
    }

    getFormData() {
        return {
            id: document.getElementById('cameraId').value || undefined,
            tenCamera: document.getElementById('cameraName').value.trim(),
            maPhong: document.getElementById('roomSelect').value || null,
            ipAddress: document.getElementById('ipAddress').value.trim(),
            password: document.getElementById('cameraPassword').value.trim() || null,
            active: document.getElementById('cameraStatus').value === 'true',
            vungIn: document.getElementById('vungIn').value.trim() || null,
            vungOut: document.getElementById('vungOut').value.trim() || null
        };
    }

    validateForm(data) {
        if (!data.tenCamera) {
            this.showAlert('Vui lòng nhập tên camera', 'warning');
            return false;
        }

        if (!data.ipAddress) {
            this.showAlert('Vui lòng nhập RTSP URL', 'warning');
            return false;
        }

        // Basic RTSP URL validation
        if (!data.ipAddress.startsWith('rtsp://')) {
            this.showAlert('RTSP URL phải bắt đầu bằng rtsp://', 'warning');
            return false;
        }

        return true;
    }

    resetForm() {
        document.getElementById('cameraForm').reset();
        document.getElementById('cameraId').value = '';
        document.getElementById('testResult').innerHTML = '';
        document.getElementById('cameraStatus').value = 'true';
    }

    async testRTSP() {
        const rtspUrl = document.getElementById('ipAddress').value.trim();
        if (!rtspUrl) {
            this.showAlert('Vui lòng nhập RTSP URL trước khi test', 'warning');
            return;
        }

        const resultDiv = document.getElementById('testResult');
        resultDiv.innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-spinner fa-spin me-2"></i>
                Đang test kết nối RTSP...
            </div>
        `;

        try {
            const response = await fetch('/api/stream/test-rtsp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ rtspUrl: rtspUrl })
            });

            const result = await response.json();

            if (result.success) {
                resultDiv.innerHTML = `
                    <div class="alert alert-success">
                        <i class="fas fa-check-circle me-2"></i>
                        <strong>Kết nối thành công!</strong>
                        <div class="mt-2">
                            <small>Network reachable: ${result.networkReachable ? 'Yes' : 'No'}</small>
                            ${result.streamInfo ? `<br><small>Stream info: ${result.streamInfo}</small>` : ''}
                        </div>
                    </div>
                `;
            } else {
                resultDiv.innerHTML = `
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        <strong>Kết nối thất bại!</strong>
                        <div class="mt-2">
                            <small>${result.message}</small>
                            ${result.suggestion ? `<br><small><strong>Gợi ý:</strong> ${result.suggestion}</small>` : ''}
                            ${result.error ? `<br><small><strong>Chi tiết:</strong> ${result.error}</small>` : ''}
                        </div>
                    </div>
                `;
            }
        } catch (error) {
            console.error('RTSP test error:', error);
            resultDiv.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    <strong>Lỗi test:</strong> ${error.message}
                </div>
            `;
        }
    }

    async previewStream() {
        await this.startPreviewStream(false);
    }

    async forcePreviewStream() {
        await this.startPreviewStream(true);
    }

    async startPreviewStream(forceStart) {
        const rtspUrl = document.getElementById('ipAddress').value.trim();
        if (!rtspUrl) {
            this.showAlert('Vui lòng nhập RTSP URL trước khi xem trước', 'warning');
            return;
        }

        // Close current modal and show stream modal
        bootstrap.Modal.getInstance(document.getElementById('cameraModal')).hide();

        document.getElementById('streamModalTitle').textContent = forceStart ? 'Force Preview Camera' : 'Xem trước Camera';
        document.getElementById('cameraInfo').innerHTML = `
            <div><strong>RTSP URL:</strong> <code>${rtspUrl}</code></div>
            <div><strong>Mode:</strong> ${forceStart ? 'Force Start (bypassed tests)' : 'Normal'}</div>
        `;

        const streamModal = new bootstrap.Modal(document.getElementById('streamModal'));
        streamModal.show();

        await this.startStream(rtspUrl, forceStart);
    }

    async viewCamera(id) {
        await this.startViewCamera(id, false);
    }

    async forceViewCamera(id) {
        await this.startViewCamera(id, true);
    }

    async startViewCamera(id, forceStart) {
        const camera = this.cameras.find(c => c.id === id);
        if (!camera || !camera.ipAddress) {
            this.showAlert('Camera chưa được cấu hình RTSP URL', 'warning');
            return;
        }

        const room = (Array.isArray(this.rooms) && this.rooms.length > 0) ?
            this.rooms.find(r => r.maPhong === camera.maPhong) : null;

        document.getElementById('streamModalTitle').textContent = `${forceStart ? 'Force View' : 'Xem'} Camera: ${camera.tenCamera}`;
        document.getElementById('cameraInfo').innerHTML = `
            <div><strong>Tên:</strong> ${camera.tenCamera}</div>
            <div><strong>Phòng:</strong> ${room ? (room.tenPhong || room.maPhong) + (room.loaiPhongDisplay ? ` (${room.loaiPhongDisplay})` : '') : 'Chưa gán phòng'}</div>
            <div><strong>RTSP URL:</strong> <code>${camera.ipAddress}</code></div>
            <div><strong>Mode:</strong> ${forceStart ? 'Force Start (bypassed tests)' : 'Normal'}</div>
            <div><strong>Trạng thái:</strong> 
                <span class="status-badge ${camera.active ? 'status-active' : 'status-inactive'}">
                    ${camera.active ? 'Hoạt động' : 'Tạm dừng'}
                </span>
            </div>
        `;

        const streamModal = new bootstrap.Modal(document.getElementById('streamModal'));
        streamModal.show();

        await this.startStream(camera.ipAddress, forceStart);
    }

    async startStream(rtspUrl, forceStart = false) {
        const loadingOverlay = document.getElementById('streamLoading');
        const video = document.getElementById('streamVideo');
        const statusDiv = document.getElementById('streamStatus');

        loadingOverlay.style.display = 'flex';
        video.style.display = 'none';

        statusDiv.innerHTML = '<div class="text-info">Đang khởi tạo stream...</div>';

        try {
            // Start HLS stream
            const response = await fetch('/api/stream/start', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    rtspUrl: rtspUrl,
                    forceStart: forceStart
                })
            });

            const data = await response.json();

            if (data.status === 'error') {
                throw new Error(data.message);
            }

            this.currentStreamId = data.streamId;
            statusDiv.innerHTML = `
                <div class="text-success">Stream ID: ${data.streamId}</div>
                <div class="text-info">${forceStart ? 'Force mode: ' : ''}Đang chờ HLS segments...</div>
            `;

            // Wait for HLS segments - longer for force start
            const waitTime = forceStart ? 15000 : 8000;
            setTimeout(async () => {
                await this.checkStreamAndPlay(data.hlsUrl);
            }, waitTime);

        } catch (error) {
            console.error('Stream start error:', error);
            loadingOverlay.style.display = 'none';
            statusDiv.innerHTML = `<div class="text-danger">Lỗi: ${error.message}</div>`;
        }
    }

    async checkStreamAndPlay(hlsUrl) {
        const loadingOverlay = document.getElementById('streamLoading');
        const video = document.getElementById('streamVideo');
        const statusDiv = document.getElementById('streamStatus');

        try {
            // Check if stream files exist
            if (this.currentStreamId) {
                const checkResponse = await fetch(`/api/stream/check/${this.currentStreamId}`);
                const checkData = await checkResponse.json();

                if (checkData.playlistExists) {
                    statusDiv.innerHTML = `
                        <div class="text-success">Playlist found: ${checkData.files ? checkData.files.length : 0} files</div>
                        <div class="text-info">Bắt đầu phát stream...</div>
                    `;

                    this.playHLS(hlsUrl);
                } else {
                    statusDiv.innerHTML = `
                        <div class="text-warning">Playlist chưa sẵn sàng</div>
                        <div class="text-info">Thử lại sau 5 giây...</div>
                    `;

                    setTimeout(() => {
                        this.checkStreamAndPlay(hlsUrl);
                    }, 5000);
                }
            }
        } catch (error) {
            console.error('Stream check error:', error);
            loadingOverlay.style.display = 'none';
            statusDiv.innerHTML = `<div class="text-danger">Lỗi kiểm tra stream: ${error.message}</div>`;
        }
    }

    playHLS(hlsUrl) {
        const loadingOverlay = document.getElementById('streamLoading');
        const video = document.getElementById('streamVideo');
        const statusDiv = document.getElementById('streamStatus');

        if (Hls.isSupported()) {
            if (this.hls) {
                this.hls.destroy();
            }

            this.hls = new Hls({
                debug: false,
                enableWorker: false,
                lowLatencyMode: true
            });

            this.hls.loadSource(hlsUrl);
            this.hls.attachMedia(video);

            this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
                loadingOverlay.style.display = 'none';
                video.style.display = 'block';
                statusDiv.innerHTML = '<div class="text-success">Stream đang phát</div>';
                video.play().catch(e => {
                    console.log('Autoplay prevented:', e);
                    statusDiv.innerHTML += '<div class="text-warning">Click để phát video</div>';
                });
            });

            this.hls.on(Hls.Events.ERROR, (event, data) => {
                console.error('HLS Error:', data);

                if (data.fatal) {
                    loadingOverlay.style.display = 'none';
                    statusDiv.innerHTML = `
                        <div class="text-danger">HLS Error: ${data.type} - ${data.details}</div>
                        <div class="text-info">Thử tải lại stream...</div>
                    `;

                    setTimeout(() => {
                        if (this.currentStreamId) {
                            this.checkStreamAndPlay(hlsUrl);
                        }
                    }, 3000);
                }
            });

        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            // Native HLS support (Safari)
            video.src = hlsUrl;
            video.addEventListener('loadedmetadata', () => {
                loadingOverlay.style.display = 'none';
                video.style.display = 'block';
                statusDiv.innerHTML = '<div class="text-success">Stream đang phát (Native HLS)</div>';
                video.play();
            });

            video.addEventListener('error', (e) => {
                loadingOverlay.style.display = 'none';
                statusDiv.innerHTML = '<div class="text-danger">Lỗi phát video</div>';
            });
        } else {
            loadingOverlay.style.display = 'none';
            statusDiv.innerHTML = '<div class="text-danger">Trình duyệt không hỗ trợ HLS</div>';
        }
    }

    stopCurrentStream() {
        if (this.hls) {
            this.hls.destroy();
            this.hls = null;
        }

        if (this.currentStreamId) {
            fetch(`/api/stream/stop/${this.currentStreamId}`, {
                method: 'POST'
            }).catch(error => {
                console.error('Error stopping stream:', error);
            });

            this.currentStreamId = null;
        }

        const video = document.getElementById('streamVideo');
        video.src = '';
        video.style.display = 'none';

        document.getElementById('streamLoading').style.display = 'flex';
        document.getElementById('streamStatus').innerHTML = '';
    }

    showAlert(message, type = 'info') {
        const alertContainer = document.getElementById('alertContainer');
        const alertId = 'alert_' + Date.now();

        const alertHTML = `
            <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                <i class="fas fa-${this.getAlertIcon(type)} me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;

        alertContainer.insertAdjacentHTML('beforeend', alertHTML);

        // Auto dismiss after 5 seconds
        setTimeout(() => {
            const alertElement = document.getElementById(alertId);
            if (alertElement) {
                bootstrap.Alert.getOrCreateInstance(alertElement).close();
            }
        }, 5000);
    }

    getAlertIcon(type) {
        const icons = {
            'success': 'check-circle',
            'danger': 'exclamation-triangle',
            'warning': 'exclamation-circle',
            'info': 'info-circle'
        };
        return icons[type] || 'info-circle';
    }
}

// Global functions
let cameraManager;

function showAddModal() {
    cameraManager.showAddModal();
}

function testRTSP() {
    cameraManager.testRTSP();
}

function previewStream() {
    cameraManager.previewStream();
}

function forcePreviewStream() {
    cameraManager.forcePreviewStream();
}

function saveCamera() {
    cameraManager.saveCamera();
}

function stopCurrentStream() {
    cameraManager.stopCurrentStream();
}

function toggleView(view) {
    cameraManager.toggleView(view);
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    cameraManager = new CameraManager();
});