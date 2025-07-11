/**
 * Enhanced Sidebar Component with Dynamic Menu Loading
 * Supports role-based navigation and responsive design
 */
class SidebarManager {
    constructor() {
        this.sidebar = null
        this.overlay = null
        this.toggleBtn = null
        this.closeBtn = null
        this.currentUser = null
        this.menuItems = []
        this.touchStartX = 0
        this.touchStartY = 0
        this.isResizing = false

        this.init()
    }

    async init() {
        this.createElements()
        this.setupEventListeners()
        await this.loadUserInfo()
        this.checkActiveState()
        this.handleResize()
    }

    createElements() {
        this.sidebar = document.getElementById("sidebar")
        this.overlay = document.getElementById("sidebarOverlay") || this.createOverlay()
        this.toggleBtn = document.getElementById("sidebarToggle")
        this.closeBtn = document.getElementById("sidebarClose")

        if (!this.overlay && this.sidebar) {
            this.overlay = this.createOverlay()
        }
    }

    createOverlay() {
        const overlay = document.createElement("div")
        overlay.id = "sidebarOverlay"
        overlay.className = "sidebar-overlay"
        document.body.appendChild(overlay)
        return overlay
    }

    setupEventListeners() {
        // Toggle button
        if (this.toggleBtn) {
            this.toggleBtn.addEventListener("click", () => this.toggleSidebar())
        }

        // Close button
        if (this.closeBtn) {
            this.closeBtn.addEventListener("click", () => this.closeSidebar())
        }

        // Overlay click
        if (this.overlay) {
            this.overlay.addEventListener("click", () => this.closeSidebar())
        }

        // Keyboard events
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && this.isOpen()) {
                this.closeSidebar()
            }
        })

        // Window resize
        window.addEventListener("resize", () => this.handleResize())

        // Touch events for mobile swipe
        this.setupTouchEvents()
    }

    setupTouchEvents() {
        document.addEventListener("touchstart", (e) => {
            this.touchStartX = e.touches[0].clientX
            this.touchStartY = e.touches[0].clientY
        })

        document.addEventListener("touchend", (e) => {
            if (!e.changedTouches || !e.changedTouches[0]) return

            const touchEndX = e.changedTouches[0].clientX
            const touchEndY = e.changedTouches[0].clientY
            const deltaX = touchEndX - this.touchStartX
            const deltaY = Math.abs(touchEndY - this.touchStartY)

            // Only handle horizontal swipes
            if (deltaY > 100) return

            // Swipe right to open (from left edge)
            if (deltaX > 100 && this.touchStartX < 50 && !this.isOpen()) {
                this.openSidebar()
            }
            // Swipe left to close
            else if (deltaX < -100 && this.isOpen()) {
                this.closeSidebar()
            }
        })
    }

    async loadUserInfo() {
        try {
            // Get current user info from server
            const response = await fetch("/api/auth/me")
            if (response.ok) {
                this.currentUser = await response.json()
                this.updateUserDisplay()
                this.loadMenuItems()
            } else {
                // Fallback for demo/development
                this.currentUser = {
                    name: "Lecturer User",
                    role: "LECTURER",
                    email: "lecturer@example.com",
                }
                this.updateUserDisplay()
                this.loadMenuItems()
            }
        } catch (error) {
            console.warn("Could not load user info, using fallback")
            this.currentUser = {
                name: "Lecturer User",
                role: "LECTURER",
                email: "lecturer@example.com",
            }
            this.updateUserDisplay()
            this.loadMenuItems()
        }
    }

    updateUserDisplay() {
        const userName = document.getElementById("userName")
        const userRole = document.getElementById("userRole")
        const userInitial = document.getElementById("userInitial")

        if (userName) userName.textContent = this.currentUser.name || "User"
        if (userRole) userRole.textContent = this.getRoleDisplayName(this.currentUser.role)
        if (userInitial) {
            userInitial.textContent = (this.currentUser.name || "U").charAt(0).toUpperCase()
        }
    }

    getRoleDisplayName(role) {
        const roleMap = {
            ADMIN: "Quản trị viên",
            LECTURER: "Giảng viên",
            STUDENT: "Sinh viên",
        }
        return roleMap[role] || role
    }

    loadMenuItems() {
        const role = this.currentUser?.role || "LECTURER"
        this.menuItems = this.getMenuItemsByRole(role)
        this.renderMenu()
    }

    getMenuItemsByRole(role) {
        const menuConfigs = {
            ADMIN: [
                {
                    title: "Tổng quan",
                    icon: "fas fa-tachometer-alt",
                    url: "/admin/dashboard",
                    active: window.location.pathname === "/admin/dashboard",
                },
                {
                    title: "Quản lý người dùng",
                    icon: "fas fa-users",
                    children: [
                        {
                            title: "Tài khoản",
                            icon: "fas fa-user-shield",
                            url: "/admin/taikhoan",
                            active: window.location.pathname === "/admin/taikhoan",
                        },
                        {
                            title: "Sinh viên",
                            icon: "fas fa-user-graduate",
                            url: "/admin/sinhvien",
                            active: window.location.pathname === "/admin/sinhvien",
                        },
                        {
                            title: "Giảng viên",
                            icon: "fas fa-chalkboard-teacher",
                            url: "/admin/giangvien",
                            active: window.location.pathname === "/admin/giangvien",
                        },
                    ],
                },
                {
                    title: "Quản lý học tập",
                    icon: "fas fa-graduation-cap",
                    children: [
                        {
                            title: "Môn học",
                            icon: "fas fa-book",
                            url: "/admin/monhoc",
                            active: window.location.pathname === "/admin/monhoc",
                        },
                        {
                            title: "Lớp học phần",
                            icon: "fas fa-chalkboard",
                            url: "/admin/lophocphan",
                            active: window.location.pathname === "/admin/lophocphan",
                        },
                        {
                            title: "Lịch học",
                            icon: "fas fa-calendar-alt",
                            url: "/admin/lichhoc",
                            active: window.location.pathname === "/admin/lichhoc",
                        },
                    ],
                },
                {
                    title: "Quản lý điểm danh",
                    icon: "fas fa-clipboard-check",
                    children: [
                        {
                            title: "Thiết bị Camera",
                            icon: "fas fa-camera",
                            url: "/admin/camera",
                            active: window.location.pathname === "/admin/camera",
                        },
                        {
                            title: "Báo cáo điểm danh",
                            icon: "fas fa-chart-bar",
                            url: "/admin/baocao-diemdanh",
                            active: window.location.pathname === "/admin/baocao-diemdanh",
                        },
                    ],
                },
                {
                    title: "Hệ thống",
                    icon: "fas fa-cog",
                    children: [
                        {
                            title: "Cấu hình",
                            icon: "fas fa-sliders-h",
                            url: "/admin/config",
                            active: window.location.pathname === "/admin/config",
                        },
                        {
                            title: "Logs",
                            icon: "fas fa-file-alt",
                            url: "/admin/logs",
                            active: window.location.pathname === "/admin/logs",
                        },
                    ],
                },
            ],
            LECTURER: [
                {
                    title: "Tổng quan",
                    icon: "fas fa-tachometer-alt",
                    url: "/lecturer/dashboard",
                    active: window.location.pathname === "/lecturer/dashboard",
                },
                {
                    title: "Lịch giảng dạy",
                    icon: "fas fa-calendar-alt",
                    url: "/lecturer/lichhoc",
                    active: window.location.pathname === "/lecturer/lichhoc",
                },
                {
                    title: "Lớp học phần",
                    icon: "fas fa-chalkboard",
                    url: "/lecturer/lophoc",
                    active: window.location.pathname === "/lecturer/lophoc",
                },
                {
                    title: "Điểm danh",
                    icon: "fas fa-clipboard-check",
                    children: [
                        {
                            title: "Điểm danh hôm nay",
                            icon: "fas fa-calendar-day",
                            url: "/lecturer/diemdanh-homnay",
                            active: window.location.pathname === "/lecturer/diemdanh-homnay",
                        },
                        {
                            title: "Điểm danh thủ công",
                            icon: "fas fa-hand-pointer",
                            url: "/lecturer/diemdanh-thucong",
                            active: window.location.pathname === "/lecturer/diemdanh-thucong",
                        },
                        {
                            title: "Lịch sử điểm danh",
                            icon: "fas fa-history",
                            url: "/lecturer/lichsu-diemdanh",
                            active: window.location.pathname === "/lecturer/lichsu-diemdanh",
                        },
                    ],
                },
                {
                    title: "Báo cáo",
                    icon: "fas fa-chart-bar",
                    children: [
                        {
                            title: "Báo cáo ngày học",
                            icon: "fas fa-calendar-day",
                            url: "/lecturer/baocao-ngay",
                            active: window.location.pathname === "/lecturer/baocao-ngay",
                        },
                        {
                            title: "Báo cáo học kỳ",
                            icon: "fas fa-graduation-cap",
                            url: "/lecturer/baocao-hocky",
                            active: window.location.pathname === "/lecturer/baocao-hocky",
                        },
                        {
                            title: "Thống kê tổng quan",
                            icon: "fas fa-chart-pie",
                            url: "/lecturer/thongke",
                            active: window.location.pathname === "/lecturer/thongke",
                        },
                    ],
                },
                {
                    title: "Thông tin cá nhân",
                    icon: "fas fa-user",
                    url: "/lecturer/thongtin-canhan",
                    active: window.location.pathname === "/lecturer/thongtin-canhan",
                },
            ],
            STUDENT: [
                {
                    title: "Tổng quan",
                    icon: "fas fa-tachometer-alt",
                    url: "/student/dashboard",
                    active: window.location.pathname === "/student/dashboard",
                },
                {
                    title: "Lịch học",
                    icon: "fas fa-calendar-alt",
                    url: "/student/lichhoc",
                    active: window.location.pathname === "/student/lichhoc",
                },
                {
                    title: "Lớp học",
                    icon: "fas fa-chalkboard",
                    url: "/student/lophoc",
                    active: window.location.pathname === "/student/lophoc",
                },
                {
                    title: "Điểm danh",
                    icon: "fas fa-clipboard-check",
                    children: [
                        {
                            title: "Lịch sử điểm danh",
                            icon: "fas fa-history",
                            url: "/student/lichsu-diemdanh",
                            active: window.location.pathname === "/student/lichsu-diemdanh",
                        },
                        {
                            title: "Thống kê điểm danh",
                            icon: "fas fa-chart-bar",
                            url: "/student/thongke-diemdanh",
                            active: window.location.pathname === "/student/thongke-diemdanh",
                        },
                    ],
                },
                {
                    title: "Khuôn mặt",
                    icon: "fas fa-user-circle",
                    children: [
                        {
                            title: "Đăng ký khuôn mặt",
                            icon: "fas fa-camera",
                            url: "/student/dangky-khuonmat",
                            active: window.location.pathname === "/student/dangky-khuonmat",
                        },
                        {
                            title: "Cập nhật khuôn mặt",
                            icon: "fas fa-sync-alt",
                            url: "/student/capnhat-khuonmat",
                            active: window.location.pathname === "/student/capnhat-khuonmat",
                        },
                    ],
                },
                {
                    title: "Thông tin cá nhân",
                    icon: "fas fa-user",
                    url: "/student/thongtin-canhan",
                    active: window.location.pathname === "/student/thongtin-canhan",
                },
            ],
        }

        return menuConfigs[role] || menuConfigs.LECTURER
    }

    renderMenu() {
        const navContainer = document.getElementById("sidebarNav")
        if (!navContainer) return

        // Clear loading state
        navContainer.innerHTML = ""

        this.menuItems.forEach((item) => {
            const menuElement = this.createMenuItem(item)
            navContainer.appendChild(menuElement)
        })
    }

    createMenuItem(item) {
        const li = document.createElement("li")
        li.className = "nav-item"

        if (item.children && item.children.length > 0) {
            // Parent item with children
            li.innerHTML = `
                <div class="nav-link nav-parent" data-bs-toggle="collapse" data-bs-target="#menu-${this.generateId(item.title)}">
                    <div class="nav-icon">
                        <i class="${item.icon}"></i>
                    </div>
                    <span class="nav-text">${item.title}</span>
                    <div class="nav-arrow">
                        <i class="fas fa-chevron-down"></i>
                    </div>
                </div>
                <div class="collapse nav-submenu" id="menu-${this.generateId(item.title)}">
                    <ul class="nav-submenu-list">
                        ${item.children
                .map(
                    (child) => `
                            <li class="nav-subitem">
                                <a href="${child.url}" class="nav-sublink ${child.active ? "active" : ""}">
                                    <div class="nav-subicon">
                                        <i class="${child.icon}"></i>
                                    </div>
                                    <span class="nav-subtext">${child.title}</span>
                                </a>
                            </li>
                        `
                )
                .join("")}
                    </ul>
                </div>
            `
        } else {
            // Single item
            li.innerHTML = `
                <a href="${item.url}" class="nav-link ${item.active ? "active" : ""}">
                    <div class="nav-icon">
                        <i class="${item.icon}"></i>
                    </div>
                    <span class="nav-text">${item.title}</span>
                </a>
            `
        }

        return li
    }

    generateId(text) {
        return text.toLowerCase().replace(/\s+/g, "-").replace(/[^\w-]/g, "")
    }

    toggleSidebar() {
        if (this.isOpen()) {
            this.closeSidebar()
        } else {
            this.openSidebar()
        }
    }

    openSidebar() {
        if (!this.sidebar) return

        this.sidebar.classList.add("show")
        if (this.overlay) this.overlay.classList.add("show")
        document.body.classList.add("sidebar-open")

        // Focus trap for accessibility
        this.sidebar.focus()
    }

    closeSidebar() {
        if (!this.sidebar) return

        this.sidebar.classList.remove("show")
        if (this.overlay) this.overlay.classList.remove("show")
        document.body.classList.remove("sidebar-open")
    }

    isOpen() {
        return this.sidebar?.classList.contains("show") || false
    }

    checkActiveState() {
        // Update active states based on current URL
        const currentPath = window.location.pathname

        // Remove all active states
        document.querySelectorAll('.nav-link.active, .nav-sublink.active').forEach(el => {
            el.classList.remove('active')
        })

        // Set active state for current page
        const activeLink = document.querySelector(`a[href="${currentPath}"]`)
        if (activeLink) {
            activeLink.classList.add('active')

            // If it's a submenu item, expand the parent
            const submenu = activeLink.closest('.nav-submenu')
            if (submenu) {
                submenu.classList.add('show')
                const parentToggle = document.querySelector(`[data-bs-target="#${submenu.id}"]`)
                if (parentToggle) {
                    parentToggle.setAttribute('aria-expanded', 'true')
                }
            }
        }
    }

    handleResize() {
        if (this.isResizing) return

        this.isResizing = true

        // Auto-close on mobile when resizing to larger screen
        if (window.innerWidth >= 768 && this.isOpen()) {
            this.closeSidebar()
        }

        setTimeout(() => {
            this.isResizing = false
        }, 100)
    }
}

// Initialize sidebar when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
    window.sidebarManager = new SidebarManager()
})

// Global functions for backward compatibility
function toggleSidebar() {
    if (window.sidebarManager) {
        window.sidebarManager.toggleSidebar()
    }
}

function closeSidebar() {
    if (window.sidebarManager) {
        window.sidebarManager.closeSidebar()
    }
}

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SidebarManager
}