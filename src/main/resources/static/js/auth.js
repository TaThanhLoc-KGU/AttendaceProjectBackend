/**
 * ========================================
 * FACE ATTENDANCE - AUTH MANAGER
 * Quản lý đăng nhập và xác thực
 * ========================================
 */

const AuthManager = {
    // Cấu hình
    config: {
        apiBaseUrl: '/api',
        redirectDelay: 1500,
        alertTimeout: 5000,
        maxLoginAttempts: 3,
        lockoutTime: 15 * 60 * 1000 // 15 phút
    },

    // Trạng thái
    state: {
        isLoading: false,
        loginAttempts: 0,
        isLocked: false
    },

    // Khởi tạo
    init() {
        console.log('🚀 AuthManager initializing...');
        this.bindEvents();
        this.checkExistingLogin();
        this.setupPasswordToggle();
        this.handleServerMessages();
        this.checkLockout();
        console.log('✅ AuthManager initialized');
    },

    // Bind các sự kiện
    bindEvents() {
        // Form đăng nhập
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        // Form quên mật khẩu
        const forgotForm = document.getElementById('forgotPasswordForm');
        if (forgotForm) {
            forgotForm.addEventListener('submit', (e) => this.handleForgotPassword(e));
        }

        // Enter key handling
        document.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.handleEnterKey(e);
            }
        });

        // Modal events
        const forgotModal = document.getElementById('forgotPasswordModal');
        if (forgotModal) {
            forgotModal.addEventListener('hidden.bs.modal', () => {
                this.resetForgotForm();
            });
        }

        // Real-time validation
        const inputs = document.querySelectorAll('input[required]');
        inputs.forEach(input => {
            input.addEventListener('blur', () => this.validateField(input));
            input.addEventListener('input', () => this.clearFieldError(input));
        });
    },

    // Xử lý đăng nhập
    async handleLogin(event) {
        event.preventDefault();

        if (this.state.isLoading || this.state.isLocked) {
            return;
        }

        const form = event.target;
        const formData = new FormData(form);

        const credentials = {
            username: formData.get('username')?.trim(),
            password: formData.get('password'),
            rememberMe: formData.get('remember-me') === 'on'
        };

        // Validate form
        if (!this.validateLoginForm(credentials)) {
            return;
        }

        // Check login attempts
        if (this.state.loginAttempts >= this.config.maxLoginAttempts) {
            this.lockAccount();
            return;
        }

        try {
            this.setLoadingState(true);
            this.hideAlert();

            const response = await this.makeLoginRequest(credentials);
            await this.handleLoginResponse(response, credentials);

        } catch (error) {
            console.error('❌ Login error:', error);
            this.handleLoginError(error);
        } finally {
            this.setLoadingState(false);
        }
    },

    // Validate form đăng nhập
    validateLoginForm(credentials) {
        let isValid = true;

        // Username validation
        const usernameInput = document.getElementById('username');
        if (!credentials.username) {
            this.showFieldError(usernameInput, 'Vui lòng nhập tên đăng nhập');
            isValid = false;
        } else if (credentials.username.length < 3) {
            this.showFieldError(usernameInput, 'Tên đăng nhập phải có ít nhất 3 ký tự');
            isValid = false;
        }

        // Password validation
        const passwordInput = document.getElementById('password');
        if (!credentials.password) {
            this.showFieldError(passwordInput, 'Vui lòng nhập mật khẩu');
            isValid = false;
        } else if (credentials.password.length < 3) {
            this.showFieldError(passwordInput, 'Mật khẩu phải có ít nhất 3 ký tự');
            isValid = false;
        }

        return isValid;
    },

    // Gửi request đăng nhập
    async makeLoginRequest(credentials) {
        const response = await fetch(`${this.config.apiBaseUrl}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                username: credentials.username,
                password: credentials.password
            })
        });

        let data = {};
        const contentType = response.headers.get('content-type');

        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            data = { message: text };
        }

        return { response, data };
    },

    // Xử lý response đăng nhập
    async handleLoginResponse({ response, data }, credentials) {
        if (response.ok && data.accessToken) {
            // Đăng nhập thành công
            this.state.loginAttempts = 0;
            localStorage.removeItem('loginAttempts');
            localStorage.removeItem('lockoutTime');

            // Lưu thông tin đăng nhập
            this.saveAuthData(data, credentials.rememberMe);

            // Hiển thị thông báo thành công
            this.showAlert('Đăng nhập thành công! Đang chuyển hướng...', 'success');

            // Chuyển hướng
            setTimeout(() => {
                this.redirectByRole(data.user.vaiTro);
            }, this.config.redirectDelay);

        } else {
            // Đăng nhập thất bại
            this.state.loginAttempts++;
            localStorage.setItem('loginAttempts', this.state.loginAttempts);

            const errorMessage = data.message || 'Tên đăng nhập hoặc mật khẩu không chính xác';
            const attemptsLeft = this.config.maxLoginAttempts - this.state.loginAttempts;

            if (attemptsLeft > 0) {
                this.showAlert(`${errorMessage}. Còn ${attemptsLeft} lần thử.`, 'danger');
            } else {
                this.lockAccount();
            }
        }
    },

    // Xử lý lỗi đăng nhập
    handleLoginError(error) {
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            this.showAlert('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.', 'danger');
        } else {
            this.showAlert('Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.', 'danger');
        }
    },

    // Khóa tài khoản tạm thời
    lockAccount() {
        this.state.isLocked = true;
        const lockoutEnd = Date.now() + this.config.lockoutTime;
        localStorage.setItem('lockoutTime', lockoutEnd);

        const minutes = Math.ceil(this.config.lockoutTime / 60000);
        this.showAlert(`Tài khoản tạm thời bị khóa do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau ${minutes} phút.`, 'warning');

        // Disable form
        this.setFormDisabled(true);

        // Start countdown
        this.startLockoutCountdown(lockoutEnd);
    },

    // Kiểm tra lockout
    checkLockout() {
        const lockoutTime = localStorage.getItem('lockoutTime');
        const loginAttempts = localStorage.getItem('loginAttempts');

        if (loginAttempts) {
            this.state.loginAttempts = parseInt(loginAttempts);
        }

        if (lockoutTime) {
            const lockoutEnd = parseInt(lockoutTime);
            const now = Date.now();

            if (now < lockoutEnd) {
                this.state.isLocked = true;
                this.setFormDisabled(true);
                this.startLockoutCountdown(lockoutEnd);
            } else {
                // Lockout hết hạn
                localStorage.removeItem('lockoutTime');
                localStorage.removeItem('loginAttempts');
                this.state.loginAttempts = 0;
                this.state.isLocked = false;
            }
        }
    },

    // Countdown lockout
    startLockoutCountdown(lockoutEnd) {
        const countdownInterval = setInterval(() => {
            const now = Date.now();
            const timeLeft = lockoutEnd - now;

            if (timeLeft <= 0) {
                clearInterval(countdownInterval);
                this.state.isLocked = false;
                this.setFormDisabled(false);
                localStorage.removeItem('lockoutTime');
                localStorage.removeItem('loginAttempts');
                this.state.loginAttempts = 0;
                this.hideAlert();
                this.showAlert('Tài khoản đã được mở khóa. Bạn có thể đăng nhập lại.', 'info');
            } else {
                const minutes = Math.ceil(timeLeft / 60000);
                const seconds = Math.ceil((timeLeft % 60000) / 1000);
                const timeString = minutes > 0 ? `${minutes} phút ${seconds} giây` : `${seconds} giây`;
                this.showAlert(`Tài khoản tạm thời bị khóa. Thời gian còn lại: ${timeString}`, 'warning');
            }
        }, 1000);
    },

    // Xử lý quên mật khẩu
    async handleForgotPassword(event) {
        event.preventDefault();

        if (this.state.isLoading) return;

        const form = event.target;
        const formData = new FormData(form);
        const username = formData.get('resetUsername')?.trim();

        // Validate
        const usernameInput = document.getElementById('resetUsername');
        if (!username) {
            this.showFieldError(usernameInput, 'Vui lòng nhập tên đăng nhập');
            return;
        }

        if (username.length < 3) {
            this.showFieldError(usernameInput, 'Tên đăng nhập phải có ít nhất 3 ký tự');
            return;
        }

        try {
            this.setLoadingState(true, 'resetPasswordBtn');

            const response = await fetch(`${this.config.apiBaseUrl}/auth/forgot-password?username=${encodeURIComponent(username)}`, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (response.ok) {
                // Đóng modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('forgotPasswordModal'));
                modal.hide();

                // Hiển thị thông báo thành công
                this.showAlert('Yêu cầu đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra email của bạn.', 'success');

                // Reset form
                this.resetForgotForm();

            } else {
                const errorText = await response.text();
                this.showAlert(errorText || 'Có lỗi xảy ra khi gửi yêu cầu đặt lại mật khẩu', 'danger');
            }

        } catch (error) {
            console.error('❌ Forgot password error:', error);
            this.showAlert('Không thể kết nối đến server. Vui lòng thử lại sau.', 'danger');
        } finally {
            this.setLoadingState(false, 'resetPasswordBtn');
        }
    },

    // Lưu dữ liệu authentication
    saveAuthData(data, rememberMe) {
        try {
            // Lưu token
            localStorage.setItem('accessToken', data.accessToken);
            if (data.refreshToken) {
                localStorage.setItem('refreshToken', data.refreshToken);
            }

            // Lưu user info
            localStorage.setItem('user', JSON.stringify(data.user));
            localStorage.setItem('loginTime', new Date().toISOString());

            // Remember me
            if (rememberMe) {
                localStorage.setItem('rememberMe', 'true');
                localStorage.setItem('savedUsername', data.user.username);
            } else {
                localStorage.removeItem('rememberMe');
                localStorage.removeItem('savedUsername');
            }

            console.log('✅ Auth data saved successfully');
        } catch (error) {
            console.error('❌ Error saving auth data:', error);
        }
    },

    // Chuyển hướng theo vai trò
    redirectByRole(role) {
        const routes = {
            'ADMIN': '/admin/dashboard',
            'GIANGVIEN': '/lecturer/dashboard',
            'SINHVIEN': '/student/dashboard'
        };

        const targetRoute = routes[role];
        if (targetRoute) {
            console.log(`🔄 Redirecting to ${targetRoute} for role: ${role}`);
            window.location.href = targetRoute;
        } else {
            console.error('❌ Unknown role:', role);
            this.showAlert('Vai trò người dùng không hợp lệ', 'danger');
            this.clearAuthData();
        }
    },

    // Kiểm tra đăng nhập hiện tại
    checkExistingLogin() {
        try {
            const token = localStorage.getItem('accessToken');
            const userStr = localStorage.getItem('user');

            if (token && userStr) {
                const user = JSON.parse(userStr);

                // Kiểm tra token còn hạn không
                if (this.isTokenValid(token)) {
                    console.log('✅ Valid token found, redirecting...');
                    this.redirectByRole(user.vaiTro);
                    return;
                }
            }

            // Load remembered username
            const rememberMe = localStorage.getItem('rememberMe');
            const savedUsername = localStorage.getItem('savedUsername');

            if (rememberMe === 'true' && savedUsername) {
                const usernameInput = document.getElementById('username');
                const rememberCheckbox = document.getElementById('rememberMe');

                if (usernameInput) usernameInput.value = savedUsername;
                if (rememberCheckbox) rememberCheckbox.checked = true;
            }

            // Xóa dữ liệu không hợp lệ
            this.clearAuthData();

        } catch (error) {
            console.error('❌ Error checking existing login:', error);
            this.clearAuthData();
        }
    },

    // Kiểm tra token hợp lệ
    isTokenValid(token) {
        if (!token) return false;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const now = Math.floor(Date.now() / 1000);
            return payload.exp > now;
        } catch (error) {
            console.error('❌ Error validating token:', error);
            return false;
        }
    },

    // Xóa dữ liệu authentication
    clearAuthData() {
        const keysToRemove = ['accessToken', 'refreshToken', 'user', 'loginTime'];
        keysToRemove.forEach(key => localStorage.removeItem(key));
    },

    // Setup toggle password
    setupPasswordToggle() {
        const toggleBtn = document.getElementById('togglePassword');
        if (!toggleBtn) return;

        toggleBtn.addEventListener('click', () => {
            const passwordInput = document.getElementById('password');
            const icon = toggleBtn.querySelector('i');

            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            } else {
                passwordInput.type = 'password';
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            }
        });
    },

    // Xử lý tin nhắn từ server
    handleServerMessages() {
        if (window.serverData) {
            if (window.serverData.error) {
                console.log('📨 Server error:', window.serverData.error);
            }
            if (window.serverData.message) {
                console.log('📨 Server message:', window.serverData.message);
            }
        }
    },

    // Xử lý Enter key
    handleEnterKey(event) {
        const activeModal = document.querySelector('.modal.show');

        if (activeModal) {
            // Trong modal
            const submitBtn = activeModal.querySelector('.modal-footer .btn-primary');
            if (submitBtn && !submitBtn.disabled) {
                event.preventDefault();
                submitBtn.click();
            }
        } else {
            // Trong form chính
            const loginForm = document.getElementById('loginForm');
            if (loginForm && !this.state.isLoading && !this.state.isLocked) {
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                if (submitBtn && !submitBtn.disabled) {
                    event.preventDefault();
                    loginForm.dispatchEvent(new Event('submit'));
                }
            }
        }
    },

    // Validation methods
    validateField(input) {
        const value = input.value.trim();
        const fieldName = input.name;

        this.clearFieldError(input);

        if (input.hasAttribute('required') && !value) {
            this.showFieldError(input, 'Trường này là bắt buộc');
            return false;
        }

        if (fieldName === 'username' && value && value.length < 3) {
            this.showFieldError(input, 'Tên đăng nhập phải có ít nhất 3 ký tự');
            return false;
        }

        if (fieldName === 'password' && value && value.length < 3) {
            this.showFieldError(input, 'Mật khẩu phải có ít nhất 3 ký tự');
            return false;
        }

        return true;
    },

    showFieldError(input, message) {
        input.classList.add('is-invalid');
        const feedback = input.parentNode.querySelector('.invalid-feedback') ||
            input.nextElementSibling;
        if (feedback) {
            feedback.textContent = message;
        }
    },

    clearFieldError(input) {
        input.classList.remove('is-invalid');
        const feedback = input.parentNode.querySelector('.invalid-feedback') ||
            input.nextElementSibling;
        if (feedback) {
            feedback.textContent = '';
        }
    },

    // UI Helper methods
    setLoadingState(isLoading, buttonId = 'loginBtn') {
        this.state.isLoading = isLoading;
        const button = document.getElementById(buttonId);
        if (!button) return;

        if (isLoading) {
            button.classList.add('loading');
            button.disabled = true;
        } else {
            button.classList.remove('loading');
            button.disabled = this.state.isLocked;
        }
    },

    setFormDisabled(disabled) {
        const form = document.getElementById('loginForm');
        if (!form) return;

        const inputs = form.querySelectorAll('input, button');
        inputs.forEach(input => {
            input.disabled = disabled;
        });
    },

    showAlert(message, type = 'info') {
        const alertDiv = document.getElementById('alertMessage');
        const alertText = document.getElementById('alertText');

        if (!alertDiv || !alertText) return;

        // Set classes
        alertDiv.className = `alert alert-${type}`;
        alertText.textContent = message;
        alertDiv.classList.remove('d-none');

        // Auto hide after timeout
        if (this.alertTimeout) {
            clearTimeout(this.alertTimeout);
        }

        this.alertTimeout = setTimeout(() => {
            this.hideAlert();
        }, this.config.alertTimeout);

        console.log(`🔔 Alert [${type}]: ${message}`);
    },

    hideAlert() {
        const alertDiv = document.getElementById('alertMessage');
        if (alertDiv) {
            alertDiv.classList.add('d-none');
        }

        if (this.alertTimeout) {
            clearTimeout(this.alertTimeout);
            this.alertTimeout = null;
        }
    },

    resetForgotForm() {
        const form = document.getElementById('forgotPasswordForm');
        if (form) {
            form.reset();

            // Clear validation states
            const inputs = form.querySelectorAll('input');
            inputs.forEach(input => {
                this.clearFieldError(input);
            });
        }
    }
};

// Utility functions
const AuthUtils = {
    /**
     * Format thời gian còn lại
     */
    formatTimeRemaining(milliseconds) {
        const minutes = Math.floor(milliseconds / 60000);
        const seconds = Math.floor((milliseconds % 60000) / 1000);

        if (minutes > 0) {
            return `${minutes} phút ${seconds} giây`;
        }
        return `${seconds} giây`;
    },

    /**
     * Validate email format
     */
    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },

    /**
     * Generate secure random string
     */
    generateRandomString(length = 32) {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    },

    /**
     * Sanitize input to prevent XSS
     */
    sanitizeInput(input) {
        const div = document.createElement('div');
        div.textContent = input;
        return div.innerHTML;
    },

    /**
     * Check if browser supports required features
     */
    checkBrowserSupport() {
        const requiredFeatures = [
            'localStorage' in window,
            'fetch' in window,
            'Promise' in window
        ];

        return requiredFeatures.every(feature => feature);
    },

    /**
     * Get browser info for logging
     */
    getBrowserInfo() {
        const ua = navigator.userAgent;
        let browser = 'Unknown';

        if (ua.includes('Chrome')) browser = 'Chrome';
        else if (ua.includes('Firefox')) browser = 'Firefox';
        else if (ua.includes('Safari')) browser = 'Safari';
        else if (ua.includes('Edge')) browser = 'Edge';

        return {
            browser,
            userAgent: ua,
            language: navigator.language,
            platform: navigator.platform
        };
    }
};

// Security helpers
const SecurityManager = {
    /**
     * Hash password client-side (if needed)
     */
    async hashPassword(password, salt = '') {
        if (!window.crypto || !window.crypto.subtle) {
            console.warn('⚠️ Web Crypto API not available');
            return password; // Fallback to plain text
        }

        try {
            const encoder = new TextEncoder();
            const data = encoder.encode(password + salt);
            const hash = await window.crypto.subtle.digest('SHA-256', data);
            const hashArray = Array.from(new Uint8Array(hash));
            return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        } catch (error) {
            console.error('❌ Error hashing password:', error);
            return password;
        }
    },

    /**
     * Generate CSRF token
     */
    generateCSRFToken() {
        return AuthUtils.generateRandomString(32);
    },

    /**
     * Validate token format
     */
    isValidTokenFormat(token) {
        if (!token || typeof token !== 'string') return false;

        // JWT format: header.payload.signature
        const parts = token.split('.');
        return parts.length === 3;
    },

    /**
     * Check for suspicious activity
     */
    detectSuspiciousActivity() {
        const suspiciousIndicators = {
            tooManyRequests: this.checkRequestFrequency(),
            unusualUserAgent: this.checkUserAgent(),
            suspiciousLocation: this.checkLocation()
        };

        return Object.values(suspiciousIndicators).some(indicator => indicator);
    },

    checkRequestFrequency() {
        const requests = JSON.parse(localStorage.getItem('requestLog') || '[]');
        const now = Date.now();
        const fiveMinutesAgo = now - (5 * 60 * 1000);

        // Filter requests in last 5 minutes
        const recentRequests = requests.filter(time => time > fiveMinutesAgo);

        // Update log
        recentRequests.push(now);
        localStorage.setItem('requestLog', JSON.stringify(recentRequests.slice(-10)));

        // Check if too many requests (more than 10 in 5 minutes)
        return recentRequests.length > 10;
    },

    checkUserAgent() {
        const ua = navigator.userAgent;
        const suspiciousPatterns = [
            /bot/i,
            /crawler/i,
            /spider/i,
            /scraper/i
        ];

        return suspiciousPatterns.some(pattern => pattern.test(ua));
    },

    checkLocation() {
        // Placeholder for location-based security checks
        // Could integrate with geolocation API
        return false;
    }
};

// Performance monitoring
const PerformanceMonitor = {
    startTime: null,
    metrics: {},

    start(operation) {
        this.startTime = performance.now();
        this.metrics[operation] = { startTime: this.startTime };
    },

    end(operation) {
        if (this.metrics[operation]) {
            const endTime = performance.now();
            this.metrics[operation].duration = endTime - this.metrics[operation].startTime;
            this.metrics[operation].endTime = endTime;

            console.log(`⏱️ ${operation}: ${this.metrics[operation].duration.toFixed(2)}ms`);
        }
    },

    getMetrics() {
        return this.metrics;
    },

    logPageLoad() {
        window.addEventListener('load', () => {
            const navigation = performance.getEntriesByType('navigation')[0];
            if (navigation) {
                console.log('📊 Page Load Metrics:', {
                    domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
                    loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
                    totalTime: navigation.loadEventEnd - navigation.fetchStart
                });
            }
        });
    }
};

// Error handling and logging
const ErrorHandler = {
    errors: [],

    log(error, context = '') {
        const errorInfo = {
            message: error.message || error,
            stack: error.stack,
            context,
            timestamp: new Date().toISOString(),
            url: window.location.href,
            userAgent: navigator.userAgent
        };

        this.errors.push(errorInfo);
        console.error('🚨 Error logged:', errorInfo);

        // Keep only last 50 errors
        if (this.errors.length > 50) {
            this.errors = this.errors.slice(-50);
        }

        // Send to server if critical
        if (this.isCriticalError(error)) {
            this.sendErrorToServer(errorInfo);
        }
    },

    isCriticalError(error) {
        const criticalPatterns = [
            /network/i,
            /authentication/i,
            /security/i,
            /unauthorized/i
        ];

        return criticalPatterns.some(pattern =>
            pattern.test(error.message || error)
        );
    },

    async sendErrorToServer(errorInfo) {
        try {
            await fetch('/api/errors', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(errorInfo)
            });
        } catch (e) {
            console.warn('⚠️ Failed to send error to server:', e);
        }
    },

    getErrors() {
        return this.errors;
    }
};

// Global error handling
window.addEventListener('error', (event) => {
    ErrorHandler.log(event.error, 'Global error handler');
});

window.addEventListener('unhandledrejection', (event) => {
    ErrorHandler.log(event.reason, 'Unhandled promise rejection');
});

// Accessibility enhancements
const AccessibilityManager = {
    init() {
        this.setupKeyboardNavigation();
        this.setupScreenReaderSupport();
        this.setupFocusManagement();
    },

    setupKeyboardNavigation() {
        // Tab navigation through form elements
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Tab') {
                this.handleTabNavigation(event);
            } else if (event.key === 'Escape') {
                this.handleEscapeKey(event);
            }
        });
    },

    setupScreenReaderSupport() {
        // Add aria-labels and descriptions
        const form = document.getElementById('loginForm');
        if (form) {
            form.setAttribute('aria-label', 'Biểu mẫu đăng nhập');
        }

        // Live region for alerts
        const alertDiv = document.getElementById('alertMessage');
        if (alertDiv) {
            alertDiv.setAttribute('aria-live', 'polite');
            alertDiv.setAttribute('aria-atomic', 'true');
        }
    },

    setupFocusManagement() {
        // Focus management for modals
        const modal = document.getElementById('forgotPasswordModal');
        if (modal) {
            modal.addEventListener('shown.bs.modal', () => {
                const firstInput = modal.querySelector('input');
                if (firstInput) {
                    firstInput.focus();
                }
            });
        }
    },

    handleTabNavigation(event) {
        // Ensure tab navigation stays within modal when open
        const activeModal = document.querySelector('.modal.show');
        if (activeModal) {
            const focusableElements = activeModal.querySelectorAll(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
            );

            const firstElement = focusableElements[0];
            const lastElement = focusableElements[focusableElements.length - 1];

            if (event.shiftKey && document.activeElement === firstElement) {
                event.preventDefault();
                lastElement.focus();
            } else if (!event.shiftKey && document.activeElement === lastElement) {
                event.preventDefault();
                firstElement.focus();
            }
        }
    },

    handleEscapeKey(event) {
        // Close modal on Escape
        const activeModal = document.querySelector('.modal.show');
        if (activeModal) {
            const modal = bootstrap.Modal.getInstance(activeModal);
            if (modal) {
                modal.hide();
            }
        }
    }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Check browser support
    if (!AuthUtils.checkBrowserSupport()) {
        alert('Trình duyệt của bạn không được hỗ trợ. Vui lòng cập nhật hoặc sử dụng trình duyệt khác.');
        return;
    }

    // Initialize all managers
    PerformanceMonitor.logPageLoad();
    AccessibilityManager.init();

    // Log browser info
    console.log('🌐 Browser Info:', AuthUtils.getBrowserInfo());

    // Initialize main auth manager
    if (typeof AuthManager !== 'undefined') {
        AuthManager.init();
    }
});

// Export for global access
window.AuthManager = AuthManager;
window.AuthUtils = AuthUtils;
window.SecurityManager = SecurityManager;