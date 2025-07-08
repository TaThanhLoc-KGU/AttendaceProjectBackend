/**
 * Sidebar Manager - Dynamic sidebar with role-based menu
 * Compatible with Spring Boot Face Attendance System
 */

class SidebarManager {
    constructor() {
        this.sidebar = null
        this.overlay = null
        this.toggle = null
        this.currentUser = null
        this.menuItems = []
        this.isCollapsed = false
        this.isMobile = window.innerWidth <= 768

        this.init()
    }

    init() {
        this.sidebar = document.getElementById("sidebar")
        this.overlay = document.getElementById("sidebarOverlay")
        this.toggle = document.getElementById("sidebarToggle")

        if (!this.sidebar) {
            console.warn("Sidebar element not found")
            return
        }

        this.setupEventListeners()
        this.loadUserInfo()
        this.loadTheme()
        this.handleResize()
    }

    setupEventListeners() {
        // Toggle button
        if (this.toggle) {
            this.toggle.addEventListener("click", () => this.toggleSidebar())
        }

        // Close button
        const closeBtn = document.getElementById("sidebarClose")
        if (closeBtn) {
            closeBtn.addEventListener("click", () => this.closeSidebar())
        }

        // Overlay click
        if (this.overlay) {
            this.overlay.addEventListener("click", () => this.closeSidebar())
        }

        // Window resize
        window.addEventListener("resize", () => this.handleResize())

        // Keyboard shortcuts
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && this.isOpen()) {
                this.closeSidebar()
            }
        })

        // Touch gestures for mobile
        this.setupTouchGestures()
    }

    setupTouchGestures() {
        let startX = 0
        let currentX = 0
        let isDragging = false

        document.addEventListener("touchstart", (e) => {
            startX = e.touches[0].clientX
            isDragging = true
        })

        document.addEventListener("touchmove", (e) => {
            if (!isDragging) return
            currentX = e.touches[0].clientX
        })

        document.addEventListener("touchend", () => {
            if (!isDragging) return
            isDragging = false

            const deltaX = currentX - startX

            // Swipe right to open (from left edge)
            if (startX < 50 && deltaX > 100) {
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
                    name: "Admin User",
                    role: "ADMIN",
                    email: "admin@example.com",
                }
                this.updateUserDisplay()
                this.loadMenuItems()
            }
        } catch (error) {
            console.warn("Could not load user info, using fallback")
            this.currentUser = {
                name: "Admin User",
                role: "ADMIN",
                email: "admin@example.com",
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
        const role = this.currentUser?.role || "ADMIN"
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
                    title: "Quản lý hệ thống",
                    icon: "fas fa-cogs",
                    children: [
                        {
                            title: "Quản lý khoa",
                            icon: "fas fa-university",
                            url: "/admin/khoa",
                            active: window.location.pathname === "/admin/khoa",
                        },
                        {
                            title: "Quản lý ngành",
                            icon: "fas fa-sitemap",
                            url: "/admin/nganh",
                            active: window.location.pathname === "/admin/nganh",
                        },
                        {
                            title: "Quản lý môn học",
                            icon: "fas fa-book",
                            url: "/admin/monhoc",
                            active: window.location.pathname === "/admin/monhoc",
                        },
                        {
                            title: "Quản lý lớp",
                            icon: "fas fa-users",
                            url: "/admin/lop",
                            active: window.location.pathname === "/admin/lop",
                        },
                    ],
                },
                {
                    title: "Quản lý người dùng",
                    icon: "fas fa-user-friends",
                    children: [
                        {
                            title: "Giảng viên",
                            icon: "fas fa-chalkboard-teacher",
                            url: "/admin/giangvien",
                            active: window.location.pathname === "/admin/giangvien",
                        },
                        {
                            title: "Sinh viên",
                            icon: "fas fa-user-graduate",
                            url: "/admin/sinhvien",
                            active: window.location.pathname === "/admin/sinhvien",
                        },
                        {
                            title: "Tài khoản",
                            icon: "fas fa-user-cog",
                            url: "/admin/taikhoan",
                            active: window.location.pathname === "/admin/taikhoan",
                        },
                    ],
                },
                {
                    title: "Quản lý học tập",
                    icon: "fas fa-graduation-cap",
                    children: [
                        {
                            title: "Cấu hình Học kỳ - Năm học",
                            icon: "fas fa-calendar-alt",
                            url: "/admin/cauhinh-hocky",
                            active: window.location.pathname === "/admin/namhoc",
                        },
                        {
                            title: "Lớp học phần",
                            icon: "fas fa-clipboard-list",
                            url: "/admin/lophocphan",
                            active: window.location.pathname === "/admin/lophocphan",
                        },
                        {
                            title: "Lịch học",
                            icon: "fas fa-clock",
                            url: "/admin/lichhoc",
                            active: window.location.pathname === "/admin/lichhoc",
                        },
                    ],
                },
                {
                    title: "Điểm danh",
                    icon: "fas fa-user-check",
                    children: [
                        {
                            title: "Quản lý điểm danh",
                            icon: "fas fa-check-circle",
                            url: "/admin/diemdanh",
                            active: window.location.pathname === "/admin/diemdanh",
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
                    icon: "fas fa-server",
                    children: [
                        {
                            title: "Camera",
                            icon: "fas fa-video",
                            url: "/admin/camera",
                            active: window.location.pathname === "/admin/camera",
                        },
                        {
                            title: "Phòng học",
                            icon: "fas fa-door-open",
                            url: "/admin/phonghoc",
                            active: window.location.pathname === "/admin/phonghoc",
                        },
                        {
                            title: "Nhật ký hệ thống",
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
                    title: "Lớp học của tôi",
                    icon: "fas fa-chalkboard",
                    url: "/lecturer/lophoc",
                    active: window.location.pathname === "/lecturer/lophoc",
                },
                {
                    title: "Điểm danh",
                    icon: "fas fa-user-check",
                    children: [
                        {
                            title: "Điểm danh hôm nay",
                            icon: "fas fa-calendar-day",
                            url: "/lecturer/diemdanh-homnay",
                            active: window.location.pathname === "/lecturer/diemdanh-homnay",
                        },
                        {
                            title: "Lịch sử điểm danh",
                            icon: "fas fa-history",
                            url: "/lecturer/lichsu-diemdanh",
                            active: window.location.pathname === "/lecturer/lichsu-diemdanh",
                        },
                        {
                            title: "Báo cáo điểm danh",
                            icon: "fas fa-chart-line",
                            url: "/lecturer/baocao-diemdanh",
                            active: window.location.pathname === "/lecturer/baocao-diemdanh",
                        },
                    ],
                },
                {
                    title: "Lịch giảng dạy",
                    icon: "fas fa-calendar-alt",
                    url: "/lecturer/lich-giangday",
                    active: window.location.pathname === "/lecturer/lich-giangday",
                },
                {
                    title: "Sinh viên",
                    icon: "fas fa-user-graduate",
                    children: [
                        {
                            title: "Danh sách sinh viên",
                            icon: "fas fa-list",
                            url: "/lecturer/danhsach-sinhvien",
                            active: window.location.pathname === "/lecturer/danhsach-sinhvien",
                        },
                        {
                            title: "Quản lý khuôn mặt",
                            icon: "fas fa-user-circle",
                            url: "/lecturer/quanly-khuonmat",
                            active: window.location.pathname === "/lecturer/quanly-khuonmat",
                        },
                    ],
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
                    title: "Điểm danh của tôi",
                    icon: "fas fa-user-check",
                    children: [
                        {
                            title: "Lịch sử điểm danh",
                            icon: "fas fa-history",
                            url: "/student/lichsu-diemdanh",
                            active: window.location.pathname === "/student/lichsu-diemdanh",
                        },
                        {
                            title: "Thống kê điểm danh",
                            icon: "fas fa-chart-pie",
                            url: "/student/thongke-diemdanh",
                            active: window.location.pathname === "/student/thongke-diemdanh",
                        },
                    ],
                },
                {
                    title: "Lịch học",
                    icon: "fas fa-calendar-alt",
                    url: "/student/lich-hoc",
                    active: window.location.pathname === "/student/lich-hoc",
                },
                {
                    title: "Lớp học phần",
                    icon: "fas fa-book-open",
                    url: "/student/lophocphan",
                    active: window.location.pathname === "/student/lophocphan",
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

        return menuConfigs[role] || menuConfigs.ADMIN
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
                        `,
                )
                .join("")}
                    </ul>
                </div>
            `

            // Auto-expand if any child is active
            if (item.children.some((child) => child.active)) {
                setTimeout(() => {
                    const collapse = li.querySelector(".collapse")
                    if (collapse) {
                        collapse.classList.add("show")
                    }
                }, 100)
            }
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
        return text
            .toLowerCase()
            .replace(/[^a-z0-9]/g, "-")
            .replace(/-+/g, "-")
            .replace(/^-|-$/g, "")
    }

    toggleSidebar() {
        if (this.isMobile) {
            this.isOpen() ? this.closeSidebar() : this.openSidebar()
        } else {
            this.isCollapsed ? this.expandSidebar() : this.collapseSidebar()
        }
    }

    openSidebar() {
        if (!this.sidebar) return

        this.sidebar.classList.add("show")
        if (this.overlay) this.overlay.classList.add("show")
        document.body.classList.add("sidebar-open")
    }

    closeSidebar() {
        if (!this.sidebar) return

        this.sidebar.classList.remove("show")
        if (this.overlay) this.overlay.classList.remove("show")
        document.body.classList.remove("sidebar-open")
    }

    collapseSidebar() {
        if (!this.sidebar) return

        this.sidebar.classList.add("collapsed")
        this.isCollapsed = true
        localStorage.setItem("sidebar-collapsed", "true")
    }

    expandSidebar() {
        if (!this.sidebar) return

        this.sidebar.classList.remove("collapsed")
        this.isCollapsed = false
        localStorage.setItem("sidebar-collapsed", "false")
    }

    isOpen() {
        return this.sidebar && this.sidebar.classList.contains("show")
    }

    handleResize() {
        const wasMobile = this.isMobile
        this.isMobile = window.innerWidth <= 768

        if (wasMobile !== this.isMobile) {
            // Reset sidebar state on breakpoint change
            this.closeSidebar()
            this.sidebar.classList.remove("collapsed")

            if (!this.isMobile) {
                // Restore collapsed state on desktop
                const wasCollapsed = localStorage.getItem("sidebar-collapsed") === "true"
                if (wasCollapsed) {
                    this.collapseSidebar()
                }
            }
        }
    }

    loadTheme() {
        const savedTheme = localStorage.getItem("theme") || "light"
        this.applyTheme(savedTheme)
    }

    toggleTheme() {
        const currentTheme = document.documentElement.getAttribute("data-theme") || "light"
        const newTheme = currentTheme === "light" ? "dark" : "light"
        this.applyTheme(newTheme)
        localStorage.setItem("theme", newTheme)
    }

    applyTheme(theme) {
        document.documentElement.setAttribute("data-theme", theme)
        const themeIcon = document.getElementById("themeIcon")
        if (themeIcon) {
            themeIcon.className = theme === "light" ? "fas fa-moon" : "fas fa-sun"
        }
    }

    showSettings() {
        // Implement settings modal or redirect
        alert("Chức năng cài đặt đang được phát triển")
    }

    logout() {
        if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
            // Clear local storage
            localStorage.clear()
            sessionStorage.clear()

            // Redirect to login
            window.location.href = "/auth/login"
        }
    }

    // Static methods for global access
    static getInstance() {
        if (!window.sidebarManager) {
            window.sidebarManager = new SidebarManager()
        }
        return window.sidebarManager
    }

    static toggleTheme() {
        SidebarManager.getInstance().toggleTheme()
    }

    static showSettings() {
        SidebarManager.getInstance().showSettings()
    }

    static logout() {
        SidebarManager.getInstance().logout()
    }
}

// Initialize sidebar when DOM is ready
document.addEventListener("DOMContentLoaded", () => {
    // Initialize sidebar manager
    window.sidebarManager = new SidebarManager()

    // Global functions for backward compatibility
    window.toggleSidebar = () => window.sidebarManager.toggleSidebar()
    window.initializeSidebar = () => window.sidebarManager.init()
})

// Export for module systems
if (typeof module !== "undefined" && module.exports) {
    module.exports = SidebarManager
}
