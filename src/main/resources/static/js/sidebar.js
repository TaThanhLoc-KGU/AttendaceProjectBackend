/**
 * Sidebar Manager - Quản lý sidebar động với phân quyền
 * Tích hợp với hệ thống Face Attendance hiện tại
 * Version: 2.0.0
 */

class SidebarManager {
    constructor() {
        this.isCollapsed = false
        this.isMobile = window.innerWidth <= 1200
        this.currentUser = null
        this.currentRole = null
        this.menuItems = []
        this.activeMenuItem = null

        // DOM elements
        this.sidebar = null
        this.sidebarToggle = null
        this.sidebarOverlay = null
        this.sidebarNav = null
        this.mainContent = null

        // Event listeners
        this.resizeHandler = this.handleResize.bind(this)
        this.clickHandler = this.handleClick.bind(this)

        // Initialize
        this.init()
    }

    /**
     * Initialize sidebar
     */
    async init() {
        try {
            console.log("🎯 Initializing Sidebar Manager...")

            // Wait for DOM to be ready
            if (document.readyState === "loading") {
                document.addEventListener("DOMContentLoaded", () => this.setup())
            } else {
                this.setup()
            }
        } catch (error) {
            console.error("❌ Failed to initialize sidebar:", error)
        }
    }

    /**
     * Setup sidebar after DOM is ready
     */
    async setup() {
        try {
            // Get DOM elements
            this.getDOMElements()

            // Load user data
            await this.loadUserData()

            // Setup menu configuration
            this.setupMenuConfig()

            // Render sidebar
            this.render()

            // Setup event listeners
            this.setupEventListeners()

            // Set initial state
            this.setInitialState()

            console.log("✅ Sidebar Manager initialized successfully")
        } catch (error) {
            console.error("❌ Failed to setup sidebar:", error)
            this.renderError()
        }
    }

    /**
     * Get DOM elements
     */
    getDOMElements() {
        this.sidebar = document.getElementById("sidebar")
        this.sidebarToggle = document.getElementById("sidebarToggle")
        this.sidebarOverlay = document.getElementById("sidebarOverlay")
        this.sidebarNav = document.getElementById("sidebarNav")
        this.mainContent = document.querySelector(".main-content")

        if (!this.sidebar) {
            throw new Error("Sidebar element not found")
        }
    }

    /**
     * Load user data from various sources
     */
    async loadUserData() {
        try {
            // Try to get user from window.currentUserData (Thymeleaf)
            if (window.currentUserData) {
                this.currentUser = window.currentUserData
                console.log("👤 User data loaded from Thymeleaf:", this.currentUser)
            }

            // Try to get user from Common.auth if available
            if (window.Common && window.Common.auth) {
                const commonUser = window.Common.auth.getCurrentUser()
                if (commonUser) {
                    this.currentUser = commonUser
                    console.log("👤 User data loaded from Common.auth:", this.currentUser)
                }
            }

            // Fallback to localStorage
            if (!this.currentUser) {
                const userData = localStorage.getItem("user")
                if (userData) {
                    try {
                        this.currentUser = JSON.parse(userData)
                        console.log("👤 User data loaded from localStorage:", this.currentUser)
                    } catch (e) {
                        console.warn("⚠️ Invalid user data in localStorage")
                    }
                }
            }

            // Determine role
            this.currentRole = this.determineUserRole()

            console.log("🎭 User role determined:", {
                user: this.currentUser?.username || "Unknown",
                role: this.currentRole,
            })
        } catch (error) {
            console.error("❌ Failed to load user data:", error)
            this.currentRole = "GUEST"
        }
    }

    /**
     * Determine user role from various sources
     */
    determineUserRole() {
        if (this.currentUser) {
            // Check vaiTro property (từ backend)
            if (this.currentUser.vaiTro) {
                return this.currentUser.vaiTro.toString().toUpperCase()
            }

            // Check role property
            if (this.currentUser.role) {
                return this.currentUser.role.toString().toUpperCase()
            }

            // Check authorities array
            if (this.currentUser.authorities && Array.isArray(this.currentUser.authorities)) {
                const authority = this.currentUser.authorities[0]
                if (authority) {
                    return authority.replace("ROLE_", "").toUpperCase()
                }
            }

            // Check authority string
            if (this.currentUser.authority) {
                return this.currentUser.authority.replace("ROLE_", "").toUpperCase()
            }
        }

        // Try to determine from URL path
        const path = window.location.pathname
        if (path.includes("/admin/")) return "ADMIN"
        if (path.includes("/lecturer/") || path.includes("/giangvien/")) return "GIANGVIEN"
        if (path.includes("/student/") || path.includes("/sinhvien/")) return "SINHVIEN"

        // Default fallback
        return "GUEST"
    }

    /**
     * Setup menu configuration based on role
     */
    setupMenuConfig() {
        const allMenuItems = [
            // Dashboard - Available for all authenticated users
            {
                id: "dashboard",
                label: "Dashboard",
                icon: "fas fa-tachometer-alt",
                url: this.getDashboardUrl(),
                roles: ["ADMIN", "GIANGVIEN", "SINHVIEN"],
                order: 1,
            },

            // ADMIN MENU ITEMS
            {
                id: "admin-section",
                type: "section",
                label: "Quản trị hệ thống",
                roles: ["ADMIN"],
                order: 10,
            },
            {
                id: "khoa",
                label: "Quản lý Khoa",
                icon: "fas fa-university",
                url: "/admin/khoa",
                roles: ["ADMIN"],
                order: 11,
            },
            {
                id: "nganh",
                label: "Quản lý Ngành",
                icon: "fas fa-sitemap",
                url: "/admin/nganh",
                roles: ["ADMIN"],
                order: 12,
            },
            {
                id: "monhoc",
                label: "Quản lý Môn học",
                icon: "fas fa-book",
                url: "/admin/monhoc",
                roles: ["ADMIN"],
                order: 13,
            },
            {
                id: "giangvien",
                label: "Quản lý Giảng viên",
                icon: "fas fa-chalkboard-teacher",
                url: "/admin/giangvien",
                roles: ["ADMIN"],
                order: 14,
            },
            {
                id: "sinhvien",
                label: "Quản lý Sinh viên",
                icon: "fas fa-user-graduate",
                url: "/admin/sinhvien",
                roles: ["ADMIN"],
                order: 15,
            },
            {
                id: "lop",
                label: "Quản lý Lớp",
                icon: "fas fa-users",
                url: "/admin/lop",
                roles: ["ADMIN"],
                order: 16,
            },
            {
                id: "camera",
                label: "Quản lý Camera",
                icon: "fas fa-video",
                url: "/admin/camera",
                roles: ["ADMIN"],
                order: 17,
            },
            {
                id: "diemdanh-admin",
                label: "Báo cáo Điểm danh",
                icon: "fas fa-clipboard-check",
                url: "/admin/diemdanh",
                roles: ["ADMIN"],
                order: 18,
            },
            {
                id: "system-settings",
                label: "Cài đặt Hệ thống",
                icon: "fas fa-cogs",
                url: "/admin/system",
                roles: ["ADMIN"],
                order: 19,
            },

            // LECTURER MENU ITEMS
            {
                id: "lecturer-section",
                type: "section",
                label: "Giảng dạy",
                roles: ["GIANGVIEN"],
                order: 20,
            },
            {
                id: "my-courses",
                label: "Môn học của tôi",
                icon: "fas fa-book-open",
                url: "/lecturer/courses",
                roles: ["GIANGVIEN"],
                order: 21,
            },
            {
                id: "my-classes",
                label: "Lớp học phần",
                icon: "fas fa-users",
                url: "/lecturer/classes",
                roles: ["GIANGVIEN"],
                order: 22,
            },
            {
                id: "attendance-management",
                label: "Quản lý Điểm danh",
                icon: "fas fa-clipboard-check",
                url: "/lecturer/attendance",
                roles: ["GIANGVIEN"],
                order: 23,
            },
            {
                id: "schedule",
                label: "Lịch giảng dạy",
                icon: "fas fa-calendar-alt",
                url: "/lecturer/schedule",
                roles: ["GIANGVIEN"],
                order: 24,
            },
            {
                id: "student-list",
                label: "Danh sách Sinh viên",
                icon: "fas fa-user-graduate",
                url: "/lecturer/students",
                roles: ["GIANGVIEN"],
                order: 25,
            },
            {
                id: "reports",
                label: "Báo cáo",
                icon: "fas fa-chart-bar",
                url: "/lecturer/reports",
                roles: ["GIANGVIEN"],
                order: 26,
            },

            // STUDENT MENU ITEMS
            {
                id: "student-section",
                type: "section",
                label: "Học tập",
                roles: ["SINHVIEN"],
                order: 30,
            },
            {
                id: "my-attendance",
                label: "Điểm danh của tôi",
                icon: "fas fa-clipboard-check",
                url: "/student/attendance",
                roles: ["SINHVIEN"],
                order: 31,
            },
            {
                id: "my-schedule",
                label: "Lịch học",
                icon: "fas fa-calendar-alt",
                url: "/student/schedule",
                roles: ["SINHVIEN"],
                order: 32,
            },
            {
                id: "my-courses-student",
                label: "Môn học đăng ký",
                icon: "fas fa-book",
                url: "/student/courses",
                roles: ["SINHVIEN"],
                order: 33,
            },
            {
                id: "face-registration",
                label: "Đăng ký Khuôn mặt",
                icon: "fas fa-user-check",
                url: "/student/face-register",
                roles: ["SINHVIEN"],
                order: 34,
                badge: "Mới",
            },
            {
                id: "profile",
                label: "Hồ sơ cá nhân",
                icon: "fas fa-user",
                url: "/student/profile",
                roles: ["SINHVIEN"],
                order: 35,
            },

            // SHARED MENU ITEMS
            {
                id: "shared-section",
                type: "section",
                label: "Tiện ích",
                roles: ["ADMIN", "GIANGVIEN", "SINHVIEN"],
                order: 90,
            },
            {
                id: "notifications",
                label: "Thông báo",
                icon: "fas fa-bell",
                url: "/notifications",
                roles: ["ADMIN", "GIANGVIEN", "SINHVIEN"],
                order: 91,
                badge: "3",
            },
            {
                id: "help",
                label: "Trợ giúp",
                icon: "fas fa-question-circle",
                url: "/help",
                roles: ["ADMIN", "GIANGVIEN", "SINHVIEN"],
                order: 92,
            },
        ]

        // Filter menu items by role
        this.menuItems = allMenuItems
            .filter((item) => item.roles.includes(this.currentRole))
            .sort((a, b) => a.order - b.order)

        console.log(`📋 Menu items loaded for ${this.currentRole}:`, this.menuItems.length)
    }

    /**
     * Get dashboard URL based on role
     */
    getDashboardUrl() {
        switch (this.currentRole) {
            case "ADMIN":
                return "/admin/dashboard"
            case "GIANGVIEN":
                return "/lecturer/dashboard"
            case "SINHVIEN":
                return "/student/dashboard"
            default:
                return "/"
        }
    }

    /**
     * Render sidebar
     */
    render() {
        this.renderUserInfo()
        this.renderNavigation()
        this.setRoleTheme()
        this.setActiveMenuItem()
    }

    /**
     * Render user information
     */
    renderUserInfo() {
        const userAvatar = document.getElementById("userAvatar")
        const userInitial = document.getElementById("userInitial")
        const userName = document.getElementById("userName")
        const userRole = document.getElementById("userRole")

        if (this.currentUser) {
            // Set user avatar
            if (userInitial) {
                userInitial.textContent = this.currentUser.username?.charAt(0)?.toUpperCase() || "U"
            }

            // Set user name
            if (userName) {
                userName.textContent = this.currentUser.username || "Unknown User"
            }

            // Set user role
            if (userRole) {
                const roleLabels = {
                    ADMIN: "Quản trị viên",
                    GIANGVIEN: "Giảng viên",
                    SINHVIEN: "Sinh viên",
                }
                userRole.textContent = roleLabels[this.currentRole] || this.currentRole
            }
        } else {
            // Default values when no user data
            if (userInitial) userInitial.textContent = "G"
            if (userName) userName.textContent = "Guest User"
            if (userRole) userRole.textContent = "Khách"
        }
    }

    /**
     * Render navigation menu
     */
    renderNavigation() {
        if (!this.sidebarNav) return

        const menuHtml = this.menuItems
            .map((item) => {
                if (item.type === "section") {
                    return `
                    <li class="nav-section">
                        <div class="nav-section-title">${item.label}</div>
                    </li>
                `
                }

                return `
                <li class="nav-item" data-menu-id="${item.id}">
                    <a href="${item.url}" class="nav-link" data-url="${item.url}">
                        <div class="nav-icon">
                            <i class="${item.icon}"></i>
                        </div>
                        <span class="nav-text">${item.label}</span>
                        ${item.badge ? `<span class="nav-badge">${item.badge}</span>` : ""}
                        ${item.submenu ? '<i class="nav-arrow fas fa-chevron-down"></i>' : ""}
                    </a>
                    ${item.submenu ? this.renderSubmenu(item.submenu) : ""}
                </li>
            `
            })
            .join("")

        this.sidebarNav.innerHTML = menuHtml
    }

    /**
     * Render submenu
     */
    renderSubmenu(submenuItems) {
        const submenuHtml = submenuItems
            .map(
                (item) => `
            <li class="nav-item">
                <a href="${item.url}" class="nav-link" data-url="${item.url}">
                    <div class="nav-icon">
                        <i class="${item.icon}"></i>
                    </div>
                    <span class="nav-text">${item.label}</span>
                    ${item.badge ? `<span class="nav-badge">${item.badge}</span>` : ""}
                </a>
            </li>
        `,
            )
            .join("")

        return `<ul class="nav-submenu">${submenuHtml}</ul>`
    }

    /**
     * Set role-based theme
     */
    setRoleTheme() {
        if (this.sidebar) {
            this.sidebar.setAttribute("data-role", this.currentRole)
        }
    }

    /**
     * Set active menu item based on current URL
     */
    setActiveMenuItem() {
        const currentPath = window.location.pathname
        const navLinks = this.sidebar?.querySelectorAll(".nav-link")

        if (!navLinks) return

        // Remove active class from all links
        navLinks.forEach((link) => link.classList.remove("active"))

        // Find and set active link
        let activeLink = null
        let maxMatchLength = 0

        navLinks.forEach((link) => {
            const href = link.getAttribute("data-url") || link.getAttribute("href")
            if (href && href !== "#") {
                if (currentPath === href) {
                    // Exact match
                    activeLink = link
                    maxMatchLength = href.length
                } else if (currentPath.startsWith(href) && href.length > maxMatchLength && href !== "/") {
                    // Partial match - choose the longest matching path
                    activeLink = link
                    maxMatchLength = href.length
                }
            }
        })

        if (activeLink) {
            activeLink.classList.add("active")
            this.activeMenuItem = activeLink.closest(".nav-item")?.getAttribute("data-menu-id")
            console.log(`🎯 Active menu item: ${this.activeMenuItem}`)
        }
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        // Sidebar toggle
        if (this.sidebarToggle) {
            this.sidebarToggle.addEventListener("click", () => this.toggle())
        }

        // Overlay click
        if (this.sidebarOverlay) {
            this.sidebarOverlay.addEventListener("click", () => this.hide())
        }

        // Navigation clicks
        if (this.sidebarNav) {
            this.sidebarNav.addEventListener("click", this.clickHandler)
        }

        // Window resize
        window.addEventListener("resize", this.resizeHandler)

        // Keyboard shortcuts
        document.addEventListener("keydown", (e) => {
            if (e.ctrlKey && e.key === "b") {
                e.preventDefault()
                this.toggle()
            }
            if (e.key === "Escape" && this.isMobile && !this.isCollapsed) {
                this.hide()
            }
        })

        // Close button
        const sidebarClose = document.getElementById("sidebarClose")
        if (sidebarClose) {
            sidebarClose.addEventListener("click", () => this.hide())
        }
    }

    /**
     * Handle navigation clicks
     */
    handleClick(e) {
        const navLink = e.target.closest(".nav-link")
        if (!navLink) return

        const url = navLink.getAttribute("data-url") || navLink.getAttribute("href")

        // Handle submenu toggle
        if (navLink.querySelector(".nav-arrow")) {
            e.preventDefault()
            this.toggleSubmenu(navLink)
            return
        }

        // Handle navigation
        if (url && url !== "#") {
            // Update active state
            this.sidebar.querySelectorAll(".nav-link").forEach((link) => {
                link.classList.remove("active")
            })
            navLink.classList.add("active")

            // Hide sidebar on mobile after navigation
            if (this.isMobile) {
                setTimeout(() => this.hide(), 150)
            }
        }
    }

    /**
     * Toggle submenu
     */
    toggleSubmenu(navLink) {
        const navItem = navLink.closest(".nav-item")
        const submenu = navItem?.querySelector(".nav-submenu")
        const arrow = navLink.querySelector(".nav-arrow")

        if (submenu && arrow) {
            const isExpanded = submenu.classList.contains("expanded")

            // Close all other submenus
            this.sidebar.querySelectorAll(".nav-submenu.expanded").forEach((menu) => {
                if (menu !== submenu) {
                    menu.classList.remove("expanded")
                    menu.closest(".nav-item").querySelector(".nav-link").classList.remove("expanded")
                }
            })

            // Toggle current submenu
            if (isExpanded) {
                submenu.classList.remove("expanded")
                navLink.classList.remove("expanded")
            } else {
                submenu.classList.add("expanded")
                navLink.classList.add("expanded")
            }
        }
    }

    /**
     * Handle window resize
     */
    handleResize() {
        const wasMobile = this.isMobile
        this.isMobile = window.innerWidth <= 1200

        if (wasMobile !== this.isMobile) {
            if (this.isMobile) {
                this.hide()
            } else {
                this.show()
            }
            this.updateMainContentClass()
        }
    }

    /**
     * Set initial state
     */
    setInitialState() {
        // Load saved state
        const savedState = localStorage.getItem("sidebar-collapsed")
        if (savedState !== null && !this.isMobile) {
            this.isCollapsed = JSON.parse(savedState)
        }

        // Apply initial state
        if (this.isMobile) {
            this.hide()
        } else {
            if (this.isCollapsed) {
                this.collapse()
            } else {
                this.expand()
            }
        }

        this.updateMainContentClass()
    }

    /**
     * Toggle sidebar
     */
    toggle() {
        if (this.isMobile) {
            if (this.sidebar.classList.contains("show")) {
                this.hide()
            } else {
                this.show()
            }
        } else {
            if (this.isCollapsed) {
                this.expand()
            } else {
                this.collapse()
            }
        }
    }

    /**
     * Show sidebar
     */
    show() {
        if (this.sidebar) {
            this.sidebar.classList.add("show")
            this.sidebar.classList.remove("hidden")
        }

        if (this.sidebarOverlay && this.isMobile) {
            this.sidebarOverlay.classList.add("active")
        }

        if (this.sidebarToggle) {
            this.sidebarToggle.classList.add("active")
        }

        this.updateMainContentClass()
    }

    /**
     * Hide sidebar
     */
    hide() {
        if (this.sidebar) {
            this.sidebar.classList.remove("show")
            this.sidebar.classList.add("hidden")
        }

        if (this.sidebarOverlay) {
            this.sidebarOverlay.classList.remove("active")
        }

        if (this.sidebarToggle) {
            this.sidebarToggle.classList.remove("active")
        }

        this.updateMainContentClass()
    }

    /**
     * Collapse sidebar
     */
    collapse() {
        if (this.isMobile) return

        this.isCollapsed = true

        if (this.sidebar) {
            this.sidebar.classList.add("collapsed")
        }

        // Save state
        localStorage.setItem("sidebar-collapsed", "true")

        this.updateMainContentClass()
    }

    /**
     * Expand sidebar
     */
    expand() {
        if (this.isMobile) return

        this.isCollapsed = false

        if (this.sidebar) {
            this.sidebar.classList.remove("collapsed")
        }

        // Save state
        localStorage.setItem("sidebar-collapsed", "false")

        this.updateMainContentClass()
    }

    /**
     * Update main content class
     */
    updateMainContentClass() {
        if (!this.mainContent) return

        this.mainContent.classList.remove("sidebar-collapsed", "sidebar-hidden")

        if (this.isMobile) {
            this.mainContent.classList.add("sidebar-hidden")
        } else if (this.isCollapsed) {
            this.mainContent.classList.add("sidebar-collapsed")
        }
    }

    /**
     * Render error state
     */
    renderError() {
        if (this.sidebarNav) {
            this.sidebarNav.innerHTML = `
                <li class="nav-item">
                    <div class="nav-link">
                        <div class="nav-icon">
                            <i class="fas fa-exclamation-triangle text-warning"></i>
                        </div>
                        <span class="nav-text">Lỗi tải menu</span>
                    </div>
                </li>
            `
        }
    }

    /**
     * Toggle theme
     */
    static toggleTheme() {
        const currentTheme = document.documentElement.getAttribute("data-theme") || "light"
        const newTheme = currentTheme === "dark" ? "light" : "dark"

        document.documentElement.setAttribute("data-theme", newTheme)
        localStorage.setItem("theme", newTheme)

        // Update theme icon
        const themeIcon = document.getElementById("themeIcon")
        if (themeIcon) {
            themeIcon.className = newTheme === "dark" ? "fas fa-sun" : "fas fa-moon"
        }

        console.log(`🎨 Theme changed to: ${newTheme}`)
    }

    /**
     * Show settings
     */
    static showSettings() {
        console.log("⚙️ Settings clicked")
        alert("Chức năng cài đặt đang được phát triển...")
    }

    /**
     * Logout user
     */
    static logout() {
        if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
            // Clear local storage
            localStorage.clear()
            sessionStorage.clear()

            // Redirect to login
            window.location.href = "/?message=logout_success"
        }
    }

    /**
     * Refresh sidebar (useful when user data changes)
     */
    async refresh() {
        await this.loadUserData()
        this.setupMenuConfig()
        this.render()
    }

    /**
     * Add menu item dynamically
     */
    addMenuItem(item) {
        this.menuItems.push(item)
        this.menuItems.sort((a, b) => a.order - b.order)
        this.renderNavigation()
        this.setActiveMenuItem()
    }

    /**
     * Remove menu item
     */
    removeMenuItem(itemId) {
        this.menuItems = this.menuItems.filter((item) => item.id !== itemId)
        this.renderNavigation()
        this.setActiveMenuItem()
    }

    /**
     * Update menu item
     */
    updateMenuItem(itemId, updates) {
        const itemIndex = this.menuItems.findIndex((item) => item.id === itemId)
        if (itemIndex !== -1) {
            this.menuItems[itemIndex] = { ...this.menuItems[itemIndex], ...updates }
            this.renderNavigation()
            this.setActiveMenuItem()
        }
    }

    /**
     * Get current user
     */
    getCurrentUser() {
        return this.currentUser
    }

    /**
     * Get current role
     */
    getCurrentRole() {
        return this.currentRole
    }

    /**
     * Check if user has role
     */
    hasRole(role) {
        return this.currentRole === role.toUpperCase()
    }

    /**
     * Destroy sidebar manager
     */
    destroy() {
        // Remove event listeners
        window.removeEventListener("resize", this.resizeHandler)

        if (this.sidebarNav) {
            this.sidebarNav.removeEventListener("click", this.clickHandler)
        }

        // Clear references
        this.sidebar = null
        this.sidebarToggle = null
        this.sidebarOverlay = null
        this.sidebarNav = null
        this.mainContent = null
    }
}

// Initialize sidebar manager when DOM is ready
let sidebarManager = null

// Function to initialize sidebar - tương thích với hệ thống hiện tại
function initializeSidebar() {
    if (!sidebarManager) {
        sidebarManager = new SidebarManager()

        // Make it globally accessible
        window.SidebarManager = SidebarManager
        window.sidebarManager = sidebarManager
    }
}

// Auto-initialize when DOM is ready
document.addEventListener("DOMContentLoaded", () => {
    initializeSidebar()

    // Load saved theme - tương thích với hệ thống theme hiện tại
    const savedTheme = localStorage.getItem("theme") || "light"
    document.documentElement.setAttribute("data-theme", savedTheme)

    const themeIcon = document.getElementById("themeIcon")
    if (themeIcon) {
        themeIcon.className = savedTheme === "dark" ? "fas fa-sun" : "fas fa-moon"
    }
})

// Export for module systems
if (typeof module !== "undefined" && module.exports) {
    module.exports = SidebarManager
}

// Global functions for backward compatibility
window.toggleSidebar = () => {
    if (sidebarManager) {
        sidebarManager.toggle()
    }
}

window.logout = () => {
    SidebarManager.logout()
}
