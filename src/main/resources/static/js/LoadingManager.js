/**
 * LoadingManager.js
 * Module quản lý tất cả loading states trong Face Attendance System
 */

class LoadingManager {
    constructor() {
        this.loadingStates = new Map();
        this.init();
    }

    init() {
        // Create global loading container if not exists
        this.createGlobalContainer();

        // Setup global styles
        this.injectStyles();

        console.log('🔄 LoadingManager initialized');
    }

    /**
     * Tạo container global cho loading
     */
    createGlobalContainer() {
        if (!document.getElementById('globalLoadingContainer')) {
            const container = document.createElement('div');
            container.id = 'globalLoadingContainer';
            container.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                pointer-events: none;
                z-index: 10000;
            `;
            document.body.appendChild(container);
        }
    }

    /**
     * Inject CSS styles cho loading
     */
    injectStyles() {
        if (document.getElementById('loadingManagerStyles')) return;

        const styles = document.createElement('style');
        styles.id = 'loadingManagerStyles';
        styles.textContent = `
            /* Loading Manager Styles */
            .lm-spinner {
                display: inline-block;
                width: 20px;
                height: 20px;
                border: 3px solid rgba(0,0,0,.1);
                border-radius: 50%;
                border-top-color: #007bff;
                animation: lm-spin 1s linear infinite;
            }

            .lm-spinner.large { width: 40px; height: 40px; border-width: 4px; }
            .lm-spinner.white { border-color: rgba(255,255,255,.3); border-top-color: #fff; }

            @keyframes lm-spin {
                to { transform: rotate(360deg); }
            }

            .lm-skeleton {
                background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
                background-size: 200% 100%;
                animation: lm-skeleton 1.5s infinite;
                border-radius: 4px;
            }

            @keyframes lm-skeleton {
                0% { background-position: 200% 0; }
                100% { background-position: -200% 0; }
            }

            .lm-overlay {
                position: absolute;
                top: 0; left: 0; right: 0; bottom: 0;
                background: rgba(255,255,255,0.9);
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                border-radius: inherit;
                z-index: 1000;
                transition: opacity 0.3s ease;
            }

            .lm-overlay.dark {
                background: rgba(0,0,0,0.7);
                color: white;
            }

            .lm-fade-in {
                animation: lm-fadeIn 0.5s ease-in;
            }

            @keyframes lm-fadeIn {
                from { opacity: 0; transform: translateY(10px); }
                to { opacity: 1; transform: translateY(0); }
            }

            .lm-error {
                background: #fee;
                border: 2px dashed #fcc;
                color: #c66;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                border-radius: 8px;
                cursor: pointer;
                transition: all 0.3s ease;
                padding: 1rem;
                text-align: center;
            }

            .lm-error:hover {
                background: #fdd;
                border-color: #fbb;
            }

            .lm-dots::after {
                content: '';
                animation: lm-dots 1.5s steps(4, end) infinite;
            }

            @keyframes lm-dots {
                0%, 20% { content: ''; }
                40% { content: '.'; }
                60% { content: '..'; }
                80%, 100% { content: '...'; }
            }
        `;
        document.head.appendChild(styles);
    }

    /**
     * Hiển thị page loading
     */
    showPageLoading(message = 'Đang tải') {
        const pageLoading = document.getElementById('pageLoading') || this.createPageLoading();
        const messageEl = pageLoading.querySelector('.loading-message');

        if (messageEl) {
            messageEl.innerHTML = `${message}<span class="lm-dots"></span>`;
        }

        pageLoading.classList.remove('hidden');
        this.loadingStates.set('page', true);

        return pageLoading;
    }

    /**
     * Ẩn page loading
     */
    hidePageLoading(delay = 500) {
        const pageLoading = document.getElementById('pageLoading');
        if (pageLoading) {
            setTimeout(() => {
                pageLoading.classList.add('hidden');
                this.loadingStates.set('page', false);
            }, delay);
        }
    }

    /**
     * Tạo page loading element
     */
    createPageLoading() {
        const pageLoading = document.createElement('div');
        pageLoading.id = 'pageLoading';
        pageLoading.style.cssText = `
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(255,255,255,0.95);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            transition: opacity 0.5s ease;
        `;
        pageLoading.innerHTML = `
            <div class="lm-spinner large"></div>
            <div class="loading-message" style="margin-top: 1rem; font-size: 1rem; color: #666;">
                Đang tải<span class="lm-dots"></span>
            </div>
        `;

        pageLoading.classList.add('hidden');
        document.body.appendChild(pageLoading);

        // Add hidden class styles
        const hiddenStyle = document.createElement('style');
        hiddenStyle.textContent = `
            #pageLoading.hidden {
                opacity: 0;
                pointer-events: none;
            }
        `;
        document.head.appendChild(hiddenStyle);

        return pageLoading;
    }

    /**
     * Tạo loading overlay cho element
     */
    showOverlay(element, options = {}) {
        if (!element) return null;

        const {
            dark = false,
            message = 'Đang tải',
            size = 'normal',
            id = null
        } = options;

        // Remove existing overlay
        this.hideOverlay(element);

        const overlay = document.createElement('div');
        overlay.className = `lm-overlay ${dark ? 'dark' : ''}`;
        if (id) overlay.id = id;

        overlay.innerHTML = `
            <div class="lm-spinner ${size === 'large' ? 'large' : ''} ${dark ? 'white' : ''}"></div>
            <div style="margin-top: 0.5rem; font-size: 0.9rem; text-align: center;">
                ${message}<span class="lm-dots"></span>
            </div>
        `;

        // Make parent relative if needed
        const position = window.getComputedStyle(element).position;
        if (position === 'static') {
            element.style.position = 'relative';
        }

        element.appendChild(overlay);
        this.loadingStates.set(element, overlay);

        return overlay;
    }

    /**
     * Ẩn loading overlay
     */
    hideOverlay(element, delay = 0) {
        if (!element) return;

        const overlay = element.querySelector('.lm-overlay');
        if (overlay) {
            setTimeout(() => {
                overlay.style.opacity = '0';
                setTimeout(() => {
                    if (overlay.parentNode) {
                        overlay.remove();
                    }
                    this.loadingStates.delete(element);
                }, 300);
            }, delay);
        }
    }

    /**
     * Tạo skeleton loading cho grid
     */
    showGridSkeleton(container, count = 5, aspectRatio = '1 / 1') {
        if (!container) return;

        container.innerHTML = '';

        for (let i = 0; i < count; i++) {
            const skeleton = document.createElement('div');
            skeleton.className = 'lm-skeleton';
            skeleton.style.cssText = `
                aspect-ratio: ${aspectRatio};
                border-radius: 8px;
                position: relative;
            `;

            // Add loading icon
            skeleton.innerHTML = `
                <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);">
                    <div class="lm-spinner"></div>
                </div>
            `;

            container.appendChild(skeleton);
        }

        this.loadingStates.set(container, 'skeleton');
    }

    /**
     * Hiển thị error state
     */
    showError(container, message = 'Có lỗi xảy ra', retryCallback = null) {
        if (!container) return;

        const errorDiv = document.createElement('div');
        errorDiv.className = 'lm-error';
        errorDiv.innerHTML = `
            <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 0.5rem;"></i>
            <div style="font-weight: 600; margin-bottom: 0.25rem;">${message}</div>
            ${retryCallback ? '<small>Nhấn để thử lại</small>' : ''}
        `;

        if (retryCallback) {
            errorDiv.onclick = retryCallback;
        }

        container.innerHTML = '';
        container.appendChild(errorDiv);
    }

    /**
     * Quản lý loading cho image
     */
    handleImageLoading(img, onLoad = null, onError = null) {
        if (!img) return;

        // Add loading class
        img.classList.add('image-loading');

        // Create overlay for parent
        const parent = img.closest('.face-image-card, .avatar-container') || img.parentElement;
        if (parent) {
            this.showOverlay(parent, { dark: true, message: 'Đang tải ảnh' });
        }

        img.onload = () => {
            img.classList.remove('image-loading');
            img.classList.add('lm-fade-in');

            if (parent) {
                this.hideOverlay(parent);
            }

            if (onLoad) onLoad(img);
        };

        img.onerror = () => {
            img.classList.remove('image-loading');

            if (parent) {
                this.hideOverlay(parent);
                this.showError(parent, 'Lỗi tải ảnh', () => {
                    // Retry loading
                    this.handleImageLoading(img, onLoad, onError);
                });
            }

            if (onError) onError(img);
        };
    }

    /**
     * Progress bar manager
     */
    createProgressBar(container, id = null) {
        const progressId = id || `progress_${Date.now()}`;

        const progressWrapper = document.createElement('div');
        progressWrapper.id = progressId;
        progressWrapper.style.cssText = `
            width: 100%;
            margin: 1rem 0;
            display: none;
        `;

        progressWrapper.innerHTML = `
            <div class="progress" style="height: 8px; border-radius: 4px; background: #e9ecef;">
                <div class="progress-bar" style="
                    height: 100%; 
                    background: #007bff; 
                    border-radius: 4px;
                    transition: width 0.3s ease;
                    width: 0%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 0.75rem;
                    font-weight: bold;
                    color: white;
                "></div>
            </div>
            <div class="progress-text" style="
                text-align: center;
                margin-top: 0.5rem;
                font-size: 0.9rem;
                color: #666;
            "></div>
        `;

        container.appendChild(progressWrapper);
        return progressWrapper;
    }

    updateProgress(progressElement, percent, text = null) {
        if (!progressElement) return;

        const bar = progressElement.querySelector('.progress-bar');
        const textEl = progressElement.querySelector('.progress-text');

        progressElement.style.display = 'block';

        if (bar) {
            bar.style.width = percent + '%';
            bar.textContent = percent + '%';
        }

        if (textEl && text) {
            textEl.textContent = text;
        }
    }

    hideProgress(progressElement, delay = 0) {
        if (!progressElement) return;

        setTimeout(() => {
            progressElement.style.display = 'none';
        }, delay);
    }

    /**
     * Utility methods
     */
    isLoading(key) {
        return this.loadingStates.get(key) || false;
    }

    clearAllLoading() {
        this.loadingStates.forEach((value, key) => {
            if (typeof key === 'string') {
                // Handle named loadings
                if (key === 'page') {
                    this.hidePageLoading(0);
                }
            } else {
                // Handle element loadings
                this.hideOverlay(key, 0);
            }
        });
        this.loadingStates.clear();
    }

    /**
     * Debug method
     */
    getLoadingStates() {
        return Array.from(this.loadingStates.entries());
    }
}

// Singleton instance
const LoadingManager_Instance = new LoadingManager();

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = LoadingManager_Instance;
} else {
    window.LoadingManager = LoadingManager_Instance;
}

/**
 * Usage Examples:
 *
 * // Show page loading
 * LoadingManager.showPageLoading('Đang khởi tạo ứng dụng');
 *
 * // Show overlay on element
 * const overlay = LoadingManager.showOverlay(document.getElementById('myElement'), {
 *     dark: true,
 *     message: 'Đang xử lý',
 *     size: 'large'
 * });
 *
 * // Show grid skeleton
 * LoadingManager.showGridSkeleton(gridContainer, 5);
 *
 * // Handle image loading
 * LoadingManager.handleImageLoading(imgElement,
 *     (img) => console.log('Image loaded'),
 *     (img) => console.log('Image error')
 * );
 *
 * // Create progress bar
 * const progress = LoadingManager.createProgressBar(container);
 * LoadingManager.updateProgress(progress, 50, 'Đang tải... 50%');
 *
 * // Show error
 * LoadingManager.showError(container, 'Không thể tải dữ liệu', () => {
 *     // Retry logic
 * });
 *
 * // Hide loading
 * LoadingManager.hideOverlay(element);
 * LoadingManager.hidePageLoading();
 */