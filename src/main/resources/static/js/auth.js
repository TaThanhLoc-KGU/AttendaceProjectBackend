/**
 * ========================================
 * FACE ATTENDANCE - AUTH MANAGER (FINAL)
 * Đã được viết lại để đảm bảo đăng nhập hoạt động mượt mà
 * ========================================
 */

let AuthManager = {
    // Cấu hình
    config: {
        apiBaseUrl: '/api',
        redirectDelay: 1000, // Delay chuyển hướng sau khi đăng nhập thành công (ms)
        alertTimeout: 5000, // Thời gian hiển thị thông báo (ms)
        maxLoginAttempts: 3, // Số lần thử đăng nhập tối đa
        lockoutTime: 15 * 60 * 1000 // 15 phút khóa tài khoản
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
        console.log('📍 Current URL:', window.location.href);

        if (!window.localStorage || !window.fetch || !window.Promise) {
            alert('Trình duyệt không được hỗ trợ. Vui lòng sử dụng trình duyệt hiện đại hơn.');
            return;
        }

        this.bindEvents();
        this.setupPasswordToggle();
        this.handleServerMessages();
        this.checkLockout();

        if (this.isOnLoginPage() && !this.hasUrlParameters()) {
            this.checkExistingLogin();
        } else {
            this.loadRememberedCredentials();
        }

        console.log('✅ AuthManager initialized');
    },

    // Kiểm tra có đang ở trang login không
    isOnLoginPage() {
        const path = window.location.pathname;
        return ['/', '/index', '/index.html', '/login'].includes(path);
    },

    // Kiểm tra có tham số URL (error, message, skipRedirect)
    hasUrlParameters() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.has('error') || urlParams.has('message') || urlParams.has('skipRedirect');
    },

    // Gắn sự kiện
    bindEvents() {
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        const forgotForm = document.getElementById('forgotPasswordForm');
        if (forgotForm) {
            forgotForm.addEventListener('submit', (e) => this.handleForgotPassword(e));
        }

        // Xử lý phím Enter
        document.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !this.state.isLoading && !this.state.isLocked) {
                this.handleEnterKey(e);
            }
        });

        // Xác thực real-time
        const inputs = document.querySelectorAll('input[required]');
        inputs.forEach(input => {
            input.addEventListener('blur', () => this.validateField(input));
            input.addEventListener('input', () => this.clearFieldError(input));
        });
    },

    // Kiểm tra đăng nhập hiện tại
    async checkExistingLogin() {
        console.log('🔍 Checking existing login...');

        const token = localStorage.getItem('accessToken');
        const userStr = localStorage.getItem('user');

        if (!token || !userStr) {
            console.log('🔓 No auth data found');
            this.loadRememberedCredentials();
            return;
        }

        try {
            const user = JSON.parse(userStr);
            console.log('👤 Found user:', user.username, 'Role:', user.vaiTro);

            // Kiểm tra token hợp lệ qua API
            const isValid = await this.validateToken(token);
            if (isValid) {
                console.log('✅ Valid token, redirecting...');
                this.redirectByRole(user.vaiTro);
            } else {
                console.log('❌ Invalid or expired token');
                this.clearAuthData();
                this.loadRememberedCredentials();
            }
        } catch (error) {
            console.error('❌ Error in checkExistingLogin:', error);
            this.clearAuthData();
            this.loadRememberedCredentials();
        }
    },

    // Load thông tin đăng nhập đã lưu
    loadRememberedCredentials() {
        const rememberMe = localStorage.getItem('rememberMe');
        const savedUsername = localStorage.getItem('savedUsername');

        if (rememberMe === 'true' && savedUsername) {
            const usernameInput = document.getElementById('username');
            const rememberCheckbox = document.getElementById('rememberMe');
            if (usernameInput) usernameInput.value = savedUsername;
            if (rememberCheckbox) rememberCheckbox.checked = true;
            console.log('💾 Loaded remembered username:', savedUsername);
        }
    },

    // Xử lý đăng nhập
    async handleLogin(event) {
        event.preventDefault();
        console.log('🔐 Login attempt started...');

        if (this.state.isLoading || this.state.isLocked) {
            console.log('🛑 Login blocked - loading or locked');
            return;
        }

        const form = event.target;
        const formData = new FormData(form);
        const credentials = {
            username: formData.get('username')?.trim(),
            password: formData.get('password'),
            rememberMe: formData.get('remember-me') === 'on'
        };

        if (!this.validateLoginForm(credentials)) {
            console.log('❌ Form validation failed');
            return;
        }

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

    // Xác thực form đăng nhập
    validateLoginForm(credentials) {
        let isValid = true;
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');

        if (!credentials.username) {
            this.showFieldError(usernameInput, 'Vui lòng nhập tên đăng nhập');
            isValid = false;
        } else if (credentials.username.length < 3) {
            this.showFieldError(usernameInput, 'Tên đăng nhập phải có ít nhất 3 ký tự');
            isValid = false;
        }

        if (!credentials.password) {
            this.showFieldError(passwordInput, 'Vui lòng nhập mật khẩu');
            isValid = false;
        } else if (credentials.password.length < 3) {
            this.showFieldError(passwordInput, 'Mật khẩu phải có ít nhất 3 ký tự');
            isValid = false;
        }

        return isValid;
    },

    // Gửi yêu cầu đăng nhập
    async makeLoginRequest(credentials) {
        const response = await fetch(`${this.config.apiBaseUrl}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                username: credentials.username,
                password: credentials.password
            })
        });

        let data;
        try {
            data = await response.json();
        } catch (e) {
            data = { message: 'Phản hồi server không hợp lệ' };
        }

        return { response, data };
    },

    // Xử lý phản hồi đăng nhập
    async handleLoginResponse({ response, data }, credentials) {
        if (response.ok) {
            console.log('✅ Đăng nhập thành công cho user:', data.username);
            this.state.loginAttempts = 0;
            localStorage.removeItem('loginAttempts');
            localStorage.removeItem('lockoutTime');

            if (credentials.rememberMe) {
                localStorage.setItem('rememberMe', 'true');
                localStorage.setItem('savedUsername', data.username);
            } else {
                localStorage.removeItem('rememberMe');
                localStorage.removeItem('savedUsername');
            }

            this.showAlert('Đăng nhập thành công! Đang chuyển hướng...', 'success');
            setTimeout(() => this.redirectByRole(data.vaiTro), this.config.redirectDelay);
        } else {
            console.log('❌ Đăng nhập thất bại:', data.message);
            this.state.loginAttempts++;
            localStorage.setItem('loginAttempts', this.state.loginAttempts);

            const attemptsLeft = this.config.maxLoginAttempts - this.state.loginAttempts;
            const errorMessage = data.message || 'Tên đăng nhập hoặc mật khẩu không chính xác';

            if (attemptsLeft > 0) {
                this.showAlert(`${errorMessage}. Còn ${attemptsLeft} lần thử.`, 'danger');
            } else {
                this.lockAccount();
            }
        }
    },

    // Xử lý lỗi đăng nhập
    handleLoginError(error) {
        this.state.loginAttempts++;
        localStorage.setItem('loginAttempts', this.state.loginAttempts);

        const attemptsLeft = this.config.maxLoginAttempts - this.state.loginAttempts;
        const errorMessage = error.message.includes('fetch') ?
            'Không thể kết nối đến server. Vui lòng kiểm tra mạng.' :
            'Đã xảy ra lỗi. Vui lòng thử lại sau.';

        if (attemptsLeft > 0) {
            this.showAlert(`${errorMessage} Còn ${attemptsLeft} lần thử.`, 'danger');
        } else {
            this.lockAccount();
        }
    },

    // Kiểm tra token hợp lệ qua API
    async validateToken(token) {
        try {
            const response = await fetch(`${this.config.apiBaseUrl}/auth/validate-token`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            });
            const data = await response.json();
            return response.ok && data.success;
        } catch (error) {
            console.error('❌ Token validation failed:', error);
            return false;
        }
    },

    // Chuyển hướng theo vai trò
    redirectByRole(role) {
        console.log('🔄 Redirecting for role:', role);

        const routes = {
            'ADMIN': '/admin/dashboard',
            'GIANGVIEN': '/lecturer/dashboard',
            'SINHVIEN': '/student/dashboard'
        };

        const targetRoute = routes[role];
        if (!targetRoute) {
            console.error('❌ Unknown role:', role);
            this.showAlert('Vai trò người dùng không hợp lệ', 'danger');
            this.clearAuthData();
            return;
        }

        // Tránh chuyển hướng lặp
        if (window.location.pathname !== targetRoute) {
            console.log(`🔄 Redirecting to ${targetRoute}`);
            window.location.href = targetRoute;
        } else {
            console.log('📍 Already on target page');
        }
    },

    // Lưu dữ liệu xác thực
    saveAuthData(data, rememberMe) {
        try {
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken || '');
            localStorage.setItem('user', JSON.stringify(data.user));
            localStorage.setItem('loginTime', new Date().toISOString());

            if (rememberMe) {
                localStorage.setItem('rememberMe', 'true');
                localStorage.setItem('savedUsername', data.user.username);
            } else {
                localStorage.removeItem('rememberMe');
                localStorage.removeItem('savedUsername');
            }

            console.log('✅ Auth data saved');
        } catch (error) {
            console.error('❌ Error saving auth data:', error);
        }
    },

    // Xóa dữ liệu xác thực
    clearAuthData() {
        ['accessToken', 'refreshToken', 'user', 'loginTime'].forEach(key => localStorage.removeItem(key));
        console.log('🗑️ Auth data cleared');
    },

    // Khóa tài khoản
    lockAccount() {
        this.state.isLocked = true;
        const lockoutEnd = Date.now() + this.config.lockoutTime;
        localStorage.setItem('lockoutTime', lockoutEnd);

        this.showAlert(`Tài khoản bị khóa do quá số lần thử. Vui lòng đợi ${this.config.lockoutTime / 60000} phút.`, 'warning');
        this.setFormDisabled(true);
        this.startLockoutCountdown(lockoutEnd);
    },

    // Kiểm tra trạng thái khóa
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
                this.state.isLocked = false;
                localStorage.removeItem('lockoutTime');
                localStorage.removeItem('loginAttempts');
                this.state.loginAttempts = 0;
                this.setFormDisabled(false);
                this.showAlert('Tài khoản đã được mở khóa.', 'info');
            }
        }
    },

    // Đếm ngược thời gian khóa
    startLockoutCountdown(lockoutEnd) {
        const countdownInterval = setInterval(() => {
            const timeLeft = lockoutEnd - Date.now();

            if (timeLeft <= 0) {
                clearInterval(countdownInterval);
                this.state.isLocked = false;
                this.setFormDisabled(false);
                localStorage.removeItem('lockoutTime');
                localStorage.removeItem('loginAttempts');
                this.state.loginAttempts = 0;
                this.hideAlert();
                this.showAlert('Tài khoản đã được mở khóa.', 'info');
            } else {
                const minutes = Math.ceil(timeLeft / 60000);
                this.showAlert(`Tài khoản bị khóa. Còn lại: ${minutes} phút.`, 'warning');
            }
        }, 1000);
    },

    // Xử lý quên mật khẩu
    async handleForgotPassword(event) {
        event.preventDefault();
        if (this.state.isLoading) return;

        const form = event.target;
        const username = form.querySelector('#resetUsername')?.value.trim();
        const usernameInput = document.getElementById('resetUsername');

        if (!username) {
            this.showFieldError(usernameInput, 'Vui lòng nhập tên đăng nhập');
            return;
        }

        try {
            this.setLoadingState(true, 'resetPasswordBtn');
            const response = await fetch(`${this.config.apiBaseUrl}/auth/forgot-password?username=${encodeURIComponent(username)}`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            });

            const modal = bootstrap.Modal.getInstance(document.getElementById('forgotPasswordModal'));
            modal.hide();

            if (response.ok) {
                this.showAlert('Yêu cầu đặt lại mật khẩu đã được gửi. Kiểm tra email của bạn.', 'success');
                this.resetForgotForm();
            } else {
                const data = await response.json();
                this.showAlert(data.message || 'Lỗi khi gửi yêu cầu đặt lại mật khẩu.', 'danger');
            }
        } catch (error) {
            console.error('❌ Forgot password error:', error);
            this.showAlert('Không thể kết nối đến server.', 'danger');
        } finally {
            this.setLoadingState(false, 'resetPasswordBtn');
        }
    },

    // Cài đặt toggle mật khẩu
    setupPasswordToggle() {
        const toggleBtn = document.getElementById('togglePassword');
        if (!toggleBtn) return;

        toggleBtn.addEventListener('click', () => {
            const passwordInput = document.getElementById('password');
            const icon = toggleBtn.querySelector('i');
            const type = passwordInput.type === 'password' ? 'text' : 'password';
            passwordInput.type = type;
            icon.classList.toggle('fa-eye', type === 'password');
            icon.classList.toggle('fa-eye-slash', type === 'text');
        });
    },

    // Xử lý thông báo từ server
    handleServerMessages() {
        const urlParams = new URLSearchParams(window.location.search);
        const error = urlParams.get('error');
        const message = urlParams.get('message');

        if (error) {
            console.log('📨 Server error:', error);
            this.showAlert(this.getErrorMessage(error), 'danger');
        }
        if (message) {
            console.log('📨 Server message:', message);
            this.showAlert(this.getMessage(message), 'success');
        }
    },

    // Xử lý phím Enter
    handleEnterKey(event) {
        const activeModal = document.querySelector('.modal.show');
        if (activeModal) {
            const submitBtn = activeModal.querySelector('.modal-footer .btn-primary');
            if (submitBtn && !submitBtn.disabled) {
                event.preventDefault();
                submitBtn.click();
            }
        } else {
            const loginForm = document.getElementById('loginForm');
            if (loginForm && !this.state.isLoading && !this.state.isLocked) {
                event.preventDefault();
                loginForm.dispatchEvent(new Event('submit'));
            }
        }
    },

    // Xác thực trường
    validateField(input) {
        const value = input.value.trim();
        if (!value && input.hasAttribute('required')) {
            this.showFieldError(input, 'Trường này là bắt buộc');
            return false;
        }
        if (input.name === 'username' && value.length < 3) {
            this.showFieldError(input, 'Tên đăng nhập phải có ít nhất 3 ký tự');
            return false;
        }
        return true;
    },

    // Hiển thị lỗi trường
    showFieldError(input, message) {
        input.classList.add('is-invalid');
        const feedback = input.nextElementSibling;
        if (feedback?.classList.contains('invalid-feedback')) {
            feedback.textContent = message;
        }
    },

    // Xóa lỗi trường
    clearFieldError(input) {
        input.classList.remove('is-invalid');
        const feedback = input.nextElementSibling;
        if (feedback?.classList.contains('invalid-feedback')) {
            feedback.textContent = '';
        }
    },

    // Đặt trạng thái loading
    setLoadingState(isLoading, buttonId = 'loginBtn') {
        this.state.isLoading = isLoading;
        const button = document.getElementById(buttonId);
        if (button) {
            button.disabled = isLoading;
            button.classList.toggle('loading', isLoading);
        }
    },

    // Vô hiệu hóa form
    setFormDisabled(disabled) {
        const form = document.getElementById('loginForm');
        if (form) {
            form.querySelectorAll('input, button').forEach(input => {
                input.disabled = disabled;
            });
        }
    },

    // Hiển thị thông báo
    showAlert(message, type = 'info') {
        const alertDiv = document.getElementById('alertMessage');
        const alertText = document.getElementById('alertText');
        if (!alertDiv || !alertText) return;

        alertDiv.className = `alert alert-${type}`;
        alertText.textContent = message;
        alertDiv.classList.remove('d-none');

        if (this.alertTimeout) clearTimeout(this.alertTimeout);
        this.alertTimeout = setTimeout(() => this.hideAlert(), this.config.alertTimeout);
    },

    // Ẩn thông báo
    hideAlert() {
        const alertDiv = document.getElementById('alertMessage');
        if (alertDiv) alertDiv.classList.add('d-none');
        if (this.alertTimeout) {
            clearTimeout(this.alertTimeout);
            this.alertTimeout = null;
        }
    },

    // Reset form quên mật khẩu
    resetForgotForm() {
        const form = document.getElementById('forgotPasswordForm');
        if (form) {
            form.reset();
            form.querySelectorAll('input').forEach(input => this.clearFieldError(input));
        }
    },

    // Ánh xạ thông báo lỗi
    getErrorMessage(errorCode) {
        const messages = {
            'login_failed': 'Tên đăng nhập hoặc mật khẩu không đúng',
            'not_authenticated': 'Vui lòng đăng nhập để tiếp tục',
            'invalid_role': 'Vai trò không hợp lệ',
            'access_denied': 'Không có quyền truy cập',
            'session_expired': 'Phiên đăng nhập đã hết hạn'
        };
        return messages[errorCode] || 'Đã xảy ra lỗi. Vui lòng thử lại.';
    },

    // Ánh xạ thông báo thành công
    getMessage(messageCode) {
        const messages = {
            'logout_success': 'Đăng xuất thành công',
            'password_reset': 'Mật khẩu đã được đặt lại',
            'account_created': 'Tài khoản đã được tạo'
        };
        return messages[messageCode] || messageCode;
    },
};



// Khởi tạo khi DOM sẵn sàng
document.addEventListener('DOMContentLoaded', () => {
    console.log('🌐 DOM loaded, initializing AuthManager...');
    AuthManager.init();
});

// Xuất cho toàn cục
window.AuthManager = AuthManager;