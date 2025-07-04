/**
 * ========================================
 * ADMIN DASHBOARD MANAGER
 * Quản lý toàn bộ tính năng admin
 * ========================================
 */

let AdminDashboard = {
    // Cấu hình
    config: {
        apiBaseUrl: '/api',
        refreshInterval: 30000, // 30 giây
        chartColors: {
            primary: '#667eea',
            success: '#28a745',
            warning: '#ffc107',
            danger: '#dc3545',
            info: '#17a2b8'
        }
    },

    // State
    state: {
        currentPage: 'dashboard',
        isLoading: false,
        data: {},
        charts: {}
    },

    // Khởi tạo
    init(initialData = {}) {
        console.log('🚀 Admin Dashboard initializing...');
        this.state.data = initialData;
        this.bindEvents();
        this.setupSidebar();
        this.loadDashboardData();
        this.initializeCharts();
        this.startAutoRefresh();
        console.log('✅ Admin Dashboard initialized');
    },

    // Bind events
    bindEvents() {
        // Sidebar toggle
        const sidebarToggle = document.getElementById('sidebarToggle');
        if (sidebarToggle) {
            sidebarToggle.addEventListener('click', () => this.toggleSidebar());
        }

        // Navigation clicks
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => this.handleNavigation(e));
        });

        // Quick actions
        document.querySelectorAll('.quick-action-card').forEach(card => {
            card.addEventListener('click', (e) => this.handleQuickAction(e));
        });

        // Filter changes
        document.addEventListener('change', (e) => {
            if (e.target.matches('.form-select')) {
                this.handleFilterChange(e);
            }
        });

        // Global search
        const searchInput = document.querySelector('.search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => this.handleGlobalSearch(e));
        }
    },

    // Load dashboard data
    async loadDashboardData() {
        try {
            this.setLoadingState(true);

            const [stats, activities, systemStatus] = await Promise.all([
                this.fetchStats(),
                this.fetchRecentActivities(),
                this.fetchSystemStatus()
            ]);

            this.updateStatsCards(stats);
            this.updateActivitiesTable(activities);
            this.updateSystemStatus(systemStatus);

        } catch (error) {
            console.error('❌ Error loading dashboard data:', error);
            this.showAlert('Không thể tải dữ liệu dashboard', 'danger');
        } finally {
            this.setLoadingState(false);
        }
    },

    // Fetch statistics
    async fetchStats() {
        const endpoints = [
            '/api/sinhvien',
            '/api/giangvien',
            '/api/lop/count',
            '/api/diemdanh/today/count',
            '/api/cameras'
        ];

        const results = await Promise.all(
            endpoints.map(endpoint => this.apiCall(endpoint))
        );

        return {
            totalStudents: Array.isArray(results[0]) ? results[0].length : results[0]?.totalElements || 0,
            totalLecturers: Array.isArray(results[1]) ? results[1].length : results[1]?.totalElements || 0,
            totalClasses: results[2] || 0,
            attendanceToday: results[3] || 0,
            activeCameras: Array.isArray(results[4]) ? results[4].filter(c => c.active).length : 0
        };
    },

    // Fetch recent activities
    async fetchRecentActivities() {
        try {
            const logs = await this.apiCall('/api/logs');
            return Array.isArray(logs) ? logs.slice(0, 10) : [];
        } catch (error) {
            console.error('Error fetching activities:', error);
            return [];
        }
    },

    // Fetch system status
    async fetchSystemStatus() {
        return {
            database: { status: 'active', details: 'Kết nối ổn định - 99.9% uptime' },
            apiServer: { status: 'active', details: 'Phản hồi trung bình: 120ms' },
            faceRecognition: { status: 'active', details: 'Độ chính xác: 98.5%' },
            storage: { status: 'warning', details: 'Đã sử dụng 78% dung lượng', progress: 78 },
            backup: { status: 'active', details: 'Lần cuối: 2 giờ trước' }
        };
    },

    // Update stats cards
    updateStatsCards(stats) {
        const updates = [
            { id: 'totalStudents', value: stats.totalStudents, change: '+12%' },
            { id: 'totalLecturers', value: stats.totalLecturers, change: '+5%' },
            { id: 'totalClasses', value: stats.totalClasses, change: '+3%' },
            { id: 'attendanceToday', value: stats.attendanceToday, change: '+8%' },
            { id: 'activeCameras', value: stats.activeCameras, change: '-1 offline' }
        ];

        updates.forEach(({ id, value, change }) => {
            const element = document.getElementById(id);
            if (element) {
                this.animateNumber(element, value);
            }
        });
    },

    // Update activities table
    updateActivitiesTable(activities) {
        const tbody = document.getElementById('activityTableBody');
        if (!tbody) return;

        tbody.innerHTML = activities.map(activity => `
            <tr>
                <td>${this.formatTime(activity.createdAt)}</td>
                <td>${activity.userId || 'System'}</td>
                <td>${activity.action}</td>
                <td>
                    <span class="status-badge ${this.getStatusClass(activity.action)}">
                        ${this.getStatusText(activity.action)}
                    </span>
                </td>
            </tr>
        `).join('');
    },

    // Update system status
    updateSystemStatus(status) {
        Object.entries(status).forEach(([key, value]) => {
            const statusElement = document.querySelector(`[data-status="${key}"]`);
            if (statusElement) {
                statusElement.innerHTML = `
                    <div class="status-info">
                        <span class="status-label">${this.getStatusLabel(key)}</span>
                        <span class="status-badge status-${value.status}">${this.getStatusText(value.status)}</span>
                    </div>
                    <div class="status-details">
                        <small class="text-muted">${value.details}</small>
                        ${value.progress ? `
                            <div class="progress mt-1" style="height: 4px;">
                                <div class="progress-bar bg-${value.status === 'warning' ? 'warning' : 'success'}" 
                                     style="width: ${value.progress}%"></div>
                            </div>
                        ` : ''}
                    </div>
                `;
            }
        });
    },

    // Initialize charts
    initializeCharts() {
        this.initAttendanceChart();
        this.initFacultyChart();
    },

    // Initialize attendance chart
    initAttendanceChart() {
        const canvas = document.getElementById('attendanceChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        this.state.charts.attendance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: this.getLast30Days(),
                datasets: [{
                    label: 'Điểm danh',
                    data: this.generateAttendanceData(),
                    borderColor: this.config.chartColors.primary,
                    backgroundColor: this.config.chartColors.primary + '20',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: '#f1f3f4'
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    },

    // Initialize faculty chart
    initFacultyChart() {
        const canvas = document.getElementById('facultyChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        this.state.charts.faculty = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Công nghệ thông tin', 'Kinh tế', 'Ngoại ngữ', 'Khác'],
                datasets: [{
                    data: [45, 25, 20, 10],
                    backgroundColor: [
                        this.config.chartColors.primary,
                        this.config.chartColors.success,
                        this.config.chartColors.warning,
                        this.config.chartColors.info
                    ],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    },

    // Handle navigation
    handleNavigation(e) {
        const link = e.currentTarget;
        const href = link.getAttribute('href');

        if (href && href !== '#') {
            // Update active state
            document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            // Navigate
            window.location.href = href;
        }
    },

    // Handle quick actions
    handleQuickAction(e) {
        const card = e.currentTarget;
        const action = card.dataset.action;

        switch (action) {
            case 'add-student':
                this.showStudentForm();
                break;
            case 'add-lecturer':
                this.showLecturerForm();
                break;
            case 'add-class':
                this.showClassForm();
                break;
            case 'manage-cameras':
                window.location.href = '/admin/camera';
                break;
            case 'view-reports':
                window.location.href = '/admin/reports';
                break;
            case 'backup-data':
                this.performBackup();
                break;
        }
    },

    // Show student form
    showStudentForm() {
        const modal = new bootstrap.Modal(document.getElementById('studentModal') || this.createStudentModal());
        modal.show();
    },

    // Create student modal
    createStudentModal() {
        const modalHtml = `
            <div class="modal fade" id="studentModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Thêm sinh viên mới</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <form id="studentForm">
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="form-label">Mã sinh viên</label>
                                            <input type="text" class="form-control" name="maSv" required>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="form-label">Họ tên</label>
                                            <input type="text" class="form-control" name="hoTen" required>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="form-label">Email</label>
                                            <input type="email" class="form-control" name="email">
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="form-label">Giới tính</label>
                                            <select class="form-select" name="gioiTinh">
                                                <option value="NAM">Nam</option>
                                                <option value="NU">Nữ</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="form-label">Ngày sinh</label>
                                            <input type="date" class="form-control" name="ngaySinh">
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="form-label">Lớp</label>
                                            <select class="form-select" name="maLop" required>
                                                <option value="">Chọn lớp...</option>
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            </form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                            <button type="button" class="btn btn-primary" onclick="AdminDashboard.saveStudent()">Lưu</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        this.loadClassOptions();
        return document.getElementById('studentModal');
    },

    // Load class options
    async loadClassOptions() {
        try {
            const classes = await this.apiCall('/api/lop');
            const select = document.querySelector('select[name="maLop"]');
            if (select && Array.isArray(classes)) {
                select.innerHTML = '<option value="">Chọn lớp...</option>' +
                    classes.map(cls => `<option value="${cls.maLop}">${cls.tenLop}</option>`).join('');
            }
        } catch (error) {
            console.error('Error loading classes:', error);
        }
    },

    // Save student
    async saveStudent() {
        const form = document.getElementById('studentForm');
        const formData = new FormData(form);
        const data = Object.fromEntries(formData);
        data.isActive = true;

        try {
            await this.apiCall('/api/sinhvien', 'POST', data);
            this.showAlert('Thêm sinh viên thành công!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('studentModal')).hide();
            this.loadDashboardData(); // Refresh data
        } catch (error) {
            console.error('Error saving student:', error);
            this.showAlert('Lỗi khi thêm sinh viên: ' + error.message, 'danger');
        }
    },

    // Perform backup
    async performBackup() {
        if (!confirm('Bạn có chắc chắn muốn thực hiện sao lưu dữ liệu?')) return;

        try {
            this.showAlert('Đang thực hiện sao lưu...', 'info');
            // Simulate backup process
            await new Promise(resolve => setTimeout(resolve, 3000));
            this.showAlert('Sao lưu dữ liệu thành công!', 'success');
        } catch (error) {
            this.showAlert('Lỗi khi sao lưu: ' + error.message, 'danger');
        }
    },

    // Toggle sidebar
    toggleSidebar() {
        const sidebar = document.getElementById('sidebar');
        const mainContent = document.getElementById('mainContent');

        if (sidebar && mainContent) {
            sidebar.classList.toggle('collapsed');
            mainContent.classList.toggle('expanded');
        }
    },

    // Handle filter change
    handleFilterChange(e) {
        const select = e.target;
        const value = select.value;
        const filterId = select.id;

        console.log(`Filter changed: ${filterId} = ${value}`);

        if (filterId === 'attendanceFilter') {
            this.updateAttendanceChart(value);
        }
    },

    // Update attendance chart
    updateAttendanceChart(period) {
        if (!this.state.charts.attendance) return;

        const chart = this.state.charts.attendance;
        chart.data.labels = period === '7' ? this.getLast7Days() :
            period === '30' ? this.getLast30Days() : this.getLast90Days();
        chart.data.datasets[0].data = this.generateAttendanceData(period);
        chart.update();
    },

    // Global search
    handleGlobalSearch(e) {
        const query = e.target.value.toLowerCase();
        console.log('Global search:', query);
        // Implement global search logic
    },

    // Start auto refresh
    startAutoRefresh() {
        setInterval(() => {
            if (this.state.currentPage === 'dashboard') {
                this.loadDashboardData();
            }
        }, this.config.refreshInterval);
    },

    // Utility functions
    async apiCall(endpoint, method = 'GET', data = null) {
        const token = localStorage.getItem('accessToken');
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token ? `Bearer ${token}` : ''
            }
        };

        if (data && method !== 'GET') {
            options.body = JSON.stringify(data);
        }

        const response = await fetch(`${this.config.apiBaseUrl}${endpoint}`, options);

        if (!response.ok) {
            throw new Error(`API call failed: ${response.status} ${response.statusText}`);
        }

        return response.json();
    },

    setLoadingState(isLoading) {
        this.state.isLoading = isLoading;
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.classList.toggle('d-none', !isLoading);
        }
    },

    showAlert(message, type = 'info') {
        // Create alert element if not exists
        let alertContainer = document.getElementById('alertContainer');
        if (!alertContainer) {
            alertContainer = document.createElement('div');
            alertContainer.id = 'alertContainer';
            alertContainer.className = 'position-fixed top-0 end-0 p-3';
            alertContainer.style.zIndex = '9999';
            document.body.appendChild(alertContainer);
        }

        const alertHtml = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                <i class="fas fa-${this.getAlertIcon(type)} me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;

        alertContainer.insertAdjacentHTML('beforeend', alertHtml);

        // Auto remove after 5 seconds
        setTimeout(() => {
            const alerts = alertContainer.querySelectorAll('.alert');
            if (alerts.length > 0) {
                alerts[0].remove();
            }
        }, 5000);
    },

    getAlertIcon(type) {
        const icons = {
            success: 'check-circle',
            danger: 'exclamation-triangle',
            warning: 'exclamation-circle',
            info: 'info-circle'
        };
        return icons[type] || 'info-circle';
    },

    animateNumber(element, targetValue) {
        const currentValue = parseInt(element.textContent) || 0;
        const increment = (targetValue - currentValue) / 20;
        let current = currentValue;

        const timer = setInterval(() => {
            current += increment;
            if ((increment > 0 && current >= targetValue) ||
                (increment < 0 && current <= targetValue)) {
                current = targetValue;
                clearInterval(timer);
            }
            element.textContent = Math.round(current);
        }, 50);
    },

    formatTime(timestamp) {
        return new Date(timestamp).toLocaleTimeString('vi-VN', {
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    getStatusClass(action) {
        if (action.includes('login') || action.includes('success')) return 'status-active';
        if (action.includes('error') || action.includes('failed')) return 'status-danger';
        if (action.includes('warning')) return 'status-warning';
        return 'status-completed';
    },

    getStatusText(status) {
        const statusMap = {
            'active': 'Hoạt động',
            'warning': 'Cảnh báo',
            'danger': 'Lỗi',
            'completed': 'Hoàn thành'
        };
        return statusMap[status] || status;
    },

    getStatusLabel(key) {
        const labels = {
            database: 'Database',
            apiServer: 'API Server',
            faceRecognition: 'Face Recognition',
            storage: 'Storage',
            backup: 'Backup'
        };
        return labels[key] || key;
    },

    getLast7Days() {
        const days = [];
        for (let i = 6; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            days.push(date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' }));
        }
        return days;
    },

    getLast30Days() {
        const days = [];
        for (let i = 29; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            days.push(date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' }));
        }
        return days;
    },

    getLast90Days() {
        const days = [];
        for (let i = 89; i >= 0; i -= 3) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            days.push(date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' }));
        }
        return days;
    },

    generateAttendanceData(period = '30') {
        const length = period === '7' ? 7 : period === '30' ? 30 : 30;
        return Array.from({ length }, () => Math.floor(Math.random() * 100) + 50);
    }
};

// Export for global access
window.AdminDashboard = AdminDashboard;