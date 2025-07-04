/**
 * ========================================
 * COMMON UTILITIES - FACE ATTENDANCE
 * Các hàm tiện ích chung cho toàn bộ ứng dụng
 * ========================================
 */

let Common = {
    // Cấu hình chung
    config: {
        apiBaseUrl: '/api',
        dateFormat: 'vi-VN',
        currency: 'VND',
        timeout: 30000,
        debounceDelay: 300
    },

    // Utility functions
    utils: {
        /**
         * Format date theo định dạng Việt Nam
         */
        formatDate(date, options = {}) {
            if (!date) return '';
            const defaultOptions = {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            };
            return new Date(date).toLocaleDateString(Common.config.dateFormat, { ...defaultOptions, ...options });
        },

        /**
         * Format datetime
         */
        formatDateTime(date) {
            if (!date) return '';
            return new Date(date).toLocaleString(Common.config.dateFormat);
        },

        /**
         * Format currency
         */
        formatCurrency(amount) {
            if (amount == null) return '';
            return new Intl.NumberFormat(Common.config.dateFormat, {
                style: 'currency',
                currency: Common.config.currency
            }).format(amount);
        },

        /**
         * Debounce function
         */
        debounce(func, wait = Common.config.debounceDelay) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        },

        /**
         * Throttle function
         */
        throttle(func, limit) {
            let inThrottle;
            return function() {
                const args = arguments;
                const context = this;
                if (!inThrottle) {
                    func.apply(context, args);
                    inThrottle = true;
                    setTimeout(() => inThrottle = false, limit);
                }
            };
        },

        /**
         * Generate UUID
         */
        generateUUID() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                const r = Math.random() * 16 | 0;
                const v = c == 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        },

        /**
         * Validate email
         */
        isValidEmail(email) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            return emailRegex.test(email);
        },

        /**
         * Validate phone number (VN format)
         */
        isValidPhone(phone) {
            const phoneRegex = /^(84|0[3|5|7|8|9])[0-9]{8}$/;
            return phoneRegex.test(phone.replace(/\s/g, ''));
        },

        /**
         * Sanitize HTML
         */
        sanitizeHtml(str) {
            const temp = document.createElement('div');
            temp.textContent = str;
            return temp.innerHTML;
        },

        /**
         * Convert string to slug
         */
        slugify(str) {
            return str
                .toLowerCase()
                .trim()
                .replace(/[^\w\s-]/g, '')
                .replace(/[\s_-]+/g, '-')
                .replace(/^-+|-+$/g, '');
        },

        /**
         * Deep clone object
         */
        deepClone(obj) {
            return JSON.parse(JSON.stringify(obj));
        },

        /**
         * Check if object is empty
         */
        isEmpty(obj) {
            return Object.keys(obj).length === 0;
        },

        /**
         * Capitalize first letter
         */
        capitalize(str) {
            if (!str) return '';
            return str.charAt(0).toUpperCase() + str.slice(1);
        },

        /**
         * Truncate text
         */
        truncate(str, length = 100) {
            if (!str) return '';
            return str.length > length ? str.substring(0, length) + '...' : str;
        }
    },

    // API utilities
    api: {
        /**
         * Get authorization header
         */
        getAuthHeaders() {
            const token = localStorage.getItem('accessToken');
            return {
                'Content-Type': 'application/json',
                'Authorization': token ? `Bearer ${token}` : ''
            };
        },

        /**
         * Make API call with error handling
         */
        async call(endpoint, options = {}) {
            const defaultOptions = {
                method: 'GET',
                headers: Common.api.getAuthHeaders(),
                timeout: Common.config.timeout
            };

            const config = { ...defaultOptions, ...options };

            // Merge headers properly
            if (options.headers) {
                config.headers = { ...defaultOptions.headers, ...options.headers };
            }

            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), config.timeout);

                const response = await fetch(`${Common.config.apiBaseUrl}${endpoint}`, {
                    ...config,
                    signal: controller.signal
                });

                clearTimeout(timeoutId);

                if (!response.ok) {
                    if (response.status === 401) {
                        Common.auth.handleUnauthorized();
                        throw new Error('Phiên đăng nhập đã hết hạn');
                    }

                    let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
                    try {
                        const errorData = await response.json();
                        errorMessage = errorData.message || errorMessage;
                    } catch (e) {
                        // Ignore JSON parse error
                    }
                    throw new Error(errorMessage);
                }

                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return await response.json();
                }
                return await response.text();

            } catch (error) {
                if (error.name === 'AbortError') {
                    throw new Error('Request timeout');
                }
                throw error;
            }
        },

        /**
         * GET request
         */
        async get(endpoint, params = {}) {
            const queryString = new URLSearchParams(params).toString();
            const url = queryString ? `${endpoint}?${queryString}` : endpoint;
            return await Common.api.call(url);
        },

        /**
         * POST request
         */
        async post(endpoint, data = {}) {
            return await Common.api.call(endpoint, {
                method: 'POST',
                body: JSON.stringify(data)
            });
        },

        /**
         * PUT request
         */
        async put(endpoint, data = {}) {
            return await Common.api.call(endpoint, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
        },

        /**
         * DELETE request
         */
        async delete(endpoint) {
            return await Common.api.call(endpoint, {
                method: 'DELETE'
            });
        },

        /**
         * Upload file
         */
        async upload(endpoint, file, additionalData = {}) {
            const formData = new FormData();
            formData.append('file', file);

            Object.keys(additionalData).forEach(key => {
                formData.append(key, additionalData[key]);
            });

            const token = localStorage.getItem('accessToken');
            return await fetch(`${Common.config.apiBaseUrl}${endpoint}`, {
                method: 'POST',
                headers: {
                    'Authorization': token ? `Bearer ${token}` : ''
                },
                body: formData
            });
        }
    },

    // Authentication utilities
    auth: {
        /**
         * Check if user is authenticated
         */
        isAuthenticated() {
            const token = localStorage.getItem('accessToken');
            const user = localStorage.getItem('user');
            return !!(token && user);
        },

        /**
         * Get current user
         */
        getCurrentUser() {
            try {
                const user = localStorage.getItem('user');
                return user ? JSON.parse(user) : null;
            } catch (error) {
                console.error('Error parsing user data:', error);
                return null;
            }
        },

        /**
         * Get user role
         */
        getUserRole() {
            const user = Common.auth.getCurrentUser();
            return user ? user.role : null;
        },

        /**
         * Check if user has specific role
         */
        hasRole(role) {
            const userRole = Common.auth.getUserRole();
            return userRole === role;
        },

        /**
         * Handle unauthorized access
         */
        handleUnauthorized() {
            console.warn('🔒 Unauthorized access detected');
            Common.storage.clear();
            Common.ui.showAlert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        },

        /**
         * Logout user
         */
        logout() {
            Common.storage.clear();
            window.location.href = '/?message=logout_success';
        }
    },

    // Storage utilities
    storage: {
        /**
         * Set item to localStorage with error handling
         */
        set(key, value) {
            try {
                localStorage.setItem(key, typeof value === 'object' ? JSON.stringify(value) : value);
                return true;
            } catch (error) {
                console.error('Error setting localStorage:', error);
                return false;
            }
        },

        /**
         * Get item from localStorage with error handling
         */
        get(key, defaultValue = null) {
            try {
                const value = localStorage.getItem(key);
                if (value === null) return defaultValue;

                // Try to parse as JSON, fallback to string
                try {
                    return JSON.parse(value);
                } catch {
                    return value;
                }
            } catch (error) {
                console.error('Error getting localStorage:', error);
                return defaultValue;
            }
        },

        /**
         * Remove item from localStorage
         */
        remove(key) {
            try {
                localStorage.removeItem(key);
                return true;
            } catch (error) {
                console.error('Error removing localStorage:', error);
                return false;
            }
        },

        /**
         * Clear all localStorage
         */
        clear() {
            try {
                localStorage.clear();
                return true;
            } catch (error) {
                console.error('Error clearing localStorage:', error);
                return false;
            }
        }
    },

    // UI utilities
    ui: {
        /**
         * Show alert notification
         */
        showAlert(message, type = 'info', duration = 5000) {
            // Create alert container if not exists
            let container = document.getElementById('alertContainer');
            if (!container) {
                container = document.createElement('div');
                container.id = 'alertContainer';
                container.className = 'position-fixed top-0 end-0 p-3';
                container.style.zIndex = '9999';
                document.body.appendChild(container);
            }

            const alertId = 'alert-' + Date.now();
            const alertHtml = `
                <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                    <i class="fas fa-${Common.ui.getAlertIcon(type)} me-2"></i>
                    ${Common.utils.sanitizeHtml(message)}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `;

            container.insertAdjacentHTML('beforeend', alertHtml);

            // Auto remove
            if (duration > 0) {
                setTimeout(() => {
                    const alert = document.getElementById(alertId);
                    if (alert) {
                        alert.remove();
                    }
                }, duration);
            }
        },

        /**
         * Get alert icon based on type
         */
        getAlertIcon(type) {
            const icons = {
                success: 'check-circle',
                danger: 'exclamation-triangle',
                warning: 'exclamation-circle',
                info: 'info-circle',
                primary: 'info-circle',
                secondary: 'info-circle'
            };
            return icons[type] || 'info-circle';
        },

        /**
         * Show loading spinner
         */
        showLoading(show = true, target = null) {
            if (target) {
                if (show) {
                    target.innerHTML = `
                        <div class="d-flex justify-content-center align-items-center p-4">
                            <div class="loading-spinner"></div>
                            <span class="ms-2">Đang tải...</span>
                        </div>
                    `;
                }
            } else {
                let overlay = document.getElementById('globalLoadingOverlay');
                if (show) {
                    if (!overlay) {
                        overlay = document.createElement('div');
                        overlay.id = 'globalLoadingOverlay';
                        overlay.className = 'position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center';
                        overlay.style.cssText = 'background: rgba(0,0,0,0.5); z-index: 10000;';
                        overlay.innerHTML = `
                            <div class="bg-white p-4 rounded shadow text-center">
                                <div class="loading-spinner mb-3"></div>
                                <p class="mb-0">Đang xử lý...</p>
                            </div>
                        `;
                        document.body.appendChild(overlay);
                    }
                    overlay.style.display = 'flex';
                } else if (overlay) {
                    overlay.style.display = 'none';
                }
            }
        },

        /**
         * Show confirmation dialog
         */
        confirm(message, title = 'Xác nhận') {
            return new Promise((resolve) => {
                // Create modal HTML
                const modalId = 'confirmModal-' + Date.now();
                const modalHtml = `
                    <div class="modal fade" id="${modalId}" tabindex="-1">
                        <div class="modal-dialog">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">${Common.utils.sanitizeHtml(title)}</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body">
                                    <p>${Common.utils.sanitizeHtml(message)}</p>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                                    <button type="button" class="btn btn-primary" id="confirmBtn">Xác nhận</button>
                                </div>
                            </div>
                        </div>
                    </div>
                `;

                document.body.insertAdjacentHTML('beforeend', modalHtml);
                const modal = new bootstrap.Modal(document.getElementById(modalId));

                // Handle buttons
                document.getElementById('confirmBtn').addEventListener('click', () => {
                    modal.hide();
                    resolve(true);
                });

                modal._element.addEventListener('hidden.bs.modal', () => {
                    modal._element.remove();
                    resolve(false);
                });

                modal.show();
            });
        },

        /**
         * Animate number counter
         */
        animateNumber(element, targetValue, duration = 1000) {
            if (!element) return;

            const startValue = parseInt(element.textContent) || 0;
            const increment = (targetValue - startValue) / (duration / 16);
            let current = startValue;

            const timer = setInterval(() => {
                current += increment;
                if ((increment > 0 && current >= targetValue) ||
                    (increment < 0 && current <= targetValue)) {
                    current = targetValue;
                    clearInterval(timer);
                }
                element.textContent = Math.round(current);
            }, 16);
        },

        /**
         * Smooth scroll to element
         */
        scrollTo(element, offset = 0) {
            if (typeof element === 'string') {
                element = document.querySelector(element);
            }
            if (element) {
                const top = element.offsetTop - offset;
                window.scrollTo({
                    top: top,
                    behavior: 'smooth'
                });
            }
        },

        /**
         * Toggle element visibility
         */
        toggle(element, show = null) {
            if (typeof element === 'string') {
                element = document.querySelector(element);
            }
            if (element) {
                if (show === null) {
                    element.style.display = element.style.display === 'none' ? '' : 'none';
                } else {
                    element.style.display = show ? '' : 'none';
                }
            }
        }
    },

    // Validation utilities
    validation: {
        /**
         * Validate form
         */
        validateForm(form) {
            if (typeof form === 'string') {
                form = document.querySelector(form);
            }
            if (!form) return false;

            let isValid = true;
            const requiredFields = form.querySelectorAll('[required]');

            requiredFields.forEach(field => {
                if (!Common.validation.validateField(field)) {
                    isValid = false;
                }
            });

            return isValid;
        },

        /**
         * Validate individual field
         */
        validateField(field) {
            const value = field.value.trim();
            const type = field.type;
            let isValid = true;
            let message = '';

            // Required check
            if (field.hasAttribute('required') && !value) {
                isValid = false;
                message = 'Trường này không được để trống';
            }
            // Email validation
            else if (type === 'email' && value && !Common.utils.isValidEmail(value)) {
                isValid = false;
                message = 'Email không hợp lệ';
            }
            // Phone validation
            else if (field.name === 'phone' && value && !Common.utils.isValidPhone(value)) {
                isValid = false;
                message = 'Số điện thoại không hợp lệ';
            }
            // Min length
            else if (field.hasAttribute('minlength')) {
                const minLength = parseInt(field.getAttribute('minlength'));
                if (value.length < minLength) {
                    isValid = false;
                    message = `Tối thiểu ${minLength} ký tự`;
                }
            }

            // Update UI
            if (isValid) {
                field.classList.remove('is-invalid');
                field.classList.add('is-valid');
            } else {
                field.classList.add('is-invalid');
                field.classList.remove('is-valid');

                const feedback = field.nextElementSibling;
                if (feedback && feedback.classList.contains('invalid-feedback')) {
                    feedback.textContent = message;
                }
            }

            return isValid;
        },

        /**
         * Clear validation
         */
        clearValidation(form) {
            if (typeof form === 'string') {
                form = document.querySelector(form);
            }
            if (!form) return;

            form.querySelectorAll('.is-invalid, .is-valid').forEach(field => {
                field.classList.remove('is-invalid', 'is-valid');
            });

            form.querySelectorAll('.invalid-feedback').forEach(feedback => {
                feedback.textContent = '';
            });
        }
    }
};

// Global functions for backward compatibility
function logout() {
    Common.auth.logout();
}

function showAlert(message, type, duration) {
    Common.ui.showAlert(message, type, duration);
}

function formatDate(date) {
    return Common.utils.formatDate(date);
}

// Export Common for use in other scripts
window.Common = Common;

// Initialize common functionality when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('🔧 Common utilities loaded');

    // Setup global error handler
    window.addEventListener('unhandledrejection', (event) => {
        console.error('Unhandled promise rejection:', event.reason);
        Common.ui.showAlert('Đã xảy ra lỗi không mong muốn', 'danger');
    });

    // Setup global click handler for external links
    document.addEventListener('click', (e) => {
        if (e.target.matches('a[href^="http"]')) {
            e.target.setAttribute('target', '_blank');
            e.target.setAttribute('rel', 'noopener noreferrer');
        }
    });
});