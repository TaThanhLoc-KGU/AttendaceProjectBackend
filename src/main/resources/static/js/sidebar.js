/**
 * sidebar.js
 * JavaScript sidebar cho tất cả phân quyền (Admin, Lecturer, Student)
 */

// Sidebar configuration theo role
const SIDEBAR_CONFIG = {
    ADMIN: [
        {
            title: "Tổng quan",
            icon: "fas fa-tachometer-alt",
            url: "/admin/dashboard"
        },
        {
            title: "Quản lý hệ thống",
            icon: "fas fa-cogs",
            children: [
                { title: "Quản lý khoa", icon: "fas fa-university", url: "/admin/khoa" },
                { title: "Quản lý ngành", icon: "fas fa-sitemap", url: "/admin/nganh" },
                { title: "Quản lý môn học", icon: "fas fa-book", url: "/admin/monhoc" },
                { title: "Quản lý lớp", icon: "fas fa-users", url: "/admin/lop" }
            ]
        },
        {
            title: "Quản lý người dùng",
            icon: "fas fa-user-friends",
            children: [
                { title: "Giảng viên", icon: "fas fa-chalkboard-teacher", url: "/admin/giangvien" },
                { title: "Sinh viên", icon: "fas fa-user-graduate", url: "/admin/sinhvien" },
                { title: "Tài khoản", icon: "fas fa-user-cog", url: "/admin/taikhoan" }
            ]
        },
        {
            title: "Quản lý học tập",
            icon: "fas fa-graduation-cap",
            children: [
                { title: "Cấu hình Học kỳ - Năm học", icon: "fas fa-calendar-alt", url: "/admin/cauhinh-hocky" },
                { title: "Lớp học phần", icon: "fas fa-clipboard-list", url: "/admin/lophocphan" },
                { title: "Lịch học", icon: "fas fa-clock", url: "/admin/lichhoc" }
            ]
        },
        {
            title: "Điểm danh",
            icon: "fas fa-user-check",
            children: [
                { title: "Báo cáo điểm danh", icon: "fas fa-chart-bar", url: "/admin/baocao-diemdanh" }
            ]
        },
        {
            title: "Hệ thống",
            icon: "fas fa-server",
            children: [
                { title: "Camera", icon: "fas fa-video", url: "/admin/camera" },
                { title: "Phòng học", icon: "fas fa-door-open", url: "/admin/phonghoc" },
                { title: "Nhật ký hệ thống", icon: "fas fa-file-alt", url: "/admin/logs" }
            ]
        }
    ],

    LECTURER: [
        {
            title: "Tổng quan",
            icon: "fas fa-tachometer-alt",
            url: "/lecturer/dashboard"
        },
        {
            title: "Lớp học của tôi",
            icon: "fas fa-chalkboard",
            url: "/lecturer/lophoc"
        },
        {
            title: "Lịch giảng dạy",
            icon: "fas fa-calendar-alt",
            url: "/lecturer/lich-giangday"
        },
    ],

    STUDENT: [
        {
            title: "Tổng quan",
            icon: "fas fa-tachometer-alt",
            url: "/student/dashboard"
        },
        {
            title: "Điểm danh của tôi",
            icon: "fas fa-user-check",
            children: [
                { title: "Lịch sử điểm danh", icon: "fas fa-history", url: "/student/lichsu-diemdanh" },
                { title: "Thống kê điểm danh", icon: "fas fa-chart-pie", url: "/student/thongke-diemdanh" }
            ]
        },
        {
            title: "Lịch học",
            icon: "fas fa-calendar-alt",
            url: "/student/lich-hoc"
        },
        {
            title: "Lớp học phần",
            icon: "fas fa-book-open",
            url: "/student/lophocphan"
        },
        {
            title: "Khuôn mặt",
            icon: "fas fa-user-circle",
            children: [
                { title: "Đăng ký khuôn mặt", icon: "fas fa-camera", url: "/student/dangky-khuonmat" },
                { title: "Cập nhật khuôn mặt", icon: "fas fa-sync-alt", url: "/student/capnhat-khuonmat" }
            ]
        },
        {
            title: "Thông tin cá nhân",
            icon: "fas fa-user",
            url: "/student/thongtin-canhan"
        }
    ]
};

const ROLE_INFO = {
    ADMIN: { avatar: "fas fa-user-shield", roleDisplay: "Quản trị viên" },
    LECTURER: { avatar: "fas fa-chalkboard-teacher", roleDisplay: "Giảng viên" },
    STUDENT: { avatar: "fas fa-user-graduate", roleDisplay: "Sinh viên" }
};

/**
 * Sidebar Manager - Enhanced with Dynamic Role Detection
 */
class SidebarManager {
    constructor() {
        this.sidebar = null
        this.overlay = null
        this.toggle = null
        this.currentUser = null
        this.currentRole = null
        this.menuItems = []
        this.isCollapsed = false
        this.isMobile = window.innerWidth <= 768

        this.detectRole()
        this.init()
    }

    detectRole() {
        const path = window.location.pathname
        console.log('🔍 Detecting role from path:', path)

        if (path.startsWith('/admin')) {
            this.currentRole = 'ADMIN'
        } else if (path.startsWith('/lecturer')) {
            this.currentRole = 'LECTURER'
        } else if (path.startsWith('/student')) {
            this.currentRole = 'STUDENT'
        } else {
            this.currentRole = 'ADMIN' // Default fallback
        }

        console.log('🎯 Detected role:', this.currentRole)
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
            // Try to get user info from API first
            const response = await fetch("/api/auth/me")
            if (response.ok) {
                this.currentUser = await response.json()
                console.log('✅ Loaded user from API:', this.currentUser)
            } else {
                throw new Error('API not available')
            }
        } catch (error) {
            console.log('ℹ️ API not available, using role-based fallback')
            // Fallback based on detected role
            this.currentUser = {
                name: this.getRoleDisplayName(this.currentRole),
                role: this.currentRole,
                email: `${this.currentRole.toLowerCase()}@example.com`
            }
        }

        this.updateUserDisplay()
        this.loadMenuItems()
    }

    updateUserDisplay() {
        const roleInfo = ROLE_INFO[this.currentRole]
        const userName = document.getElementById("userName")
        const userRole = document.getElementById("userRole")
        const userInitial = document.getElementById("userInitial")
        const userAvatar = document.getElementById("userAvatar")

        if (userName) userName.textContent = this.currentUser?.name || roleInfo.roleDisplay
        if (userRole) userRole.textContent = roleInfo.roleDisplay

        // Update avatar
        if (userAvatar) {
            userAvatar.innerHTML = `<i class="${roleInfo.avatar}"></i>`
        }

        if (userInitial) {
            userInitial.textContent = (this.currentUser?.name || roleInfo.roleDisplay).charAt(0).toUpperCase()
        }

        // Update brand subtitle
        const brandSubtitle = document.querySelector('.brand-subtitle')
        if (brandSubtitle) {
            brandSubtitle.textContent = `${roleInfo.roleDisplay} Portal`
        }

        console.log('👤 Updated user display for:', this.currentRole)
    }

    getRoleDisplayName(role) {
        return ROLE_INFO[role]?.roleDisplay || role
    }

    loadMenuItems() {
        console.log('📋 Loading menu items for role:', this.currentRole)
        this.menuItems = SIDEBAR_CONFIG[this.currentRole] || SIDEBAR_CONFIG.ADMIN
        this.renderMenu()
    }

    renderMenu() {
        const navContainer = document.getElementById("sidebarNav")
        if (!navContainer) {
            console.warn('Nav container not found')
            return
        }

        // Clear loading state
        navContainer.innerHTML = ""

        this.menuItems.forEach((item) => {
            const menuElement = this.createMenuItem(item)
            navContainer.appendChild(menuElement)
        })

        console.log(`✅ Rendered ${this.menuItems.length} menu items`)
    }

    createMenuItem(item) {
        const li = document.createElement("li")
        li.className = "nav-item"

        const currentPath = window.location.pathname
        const isActive = currentPath === item.url

        if (item.children && item.children.length > 0) {
            // Parent item with children
            const hasActiveChild = item.children.some(child => currentPath === child.url)
            const menuId = this.generateId(item.title)

            li.innerHTML = `
                <div class="nav-link nav-parent ${hasActiveChild ? 'active' : ''}" 
                     data-submenu="${menuId}" aria-expanded="${hasActiveChild}">
                    <div class="nav-icon">
                        <i class="${item.icon}"></i>
                    </div>
                    <span class="nav-text">${item.title}</span>
                    <div class="nav-arrow">
                        <i class="fas fa-chevron-down"></i>
                    </div>
                </div>
                <div class="nav-submenu ${hasActiveChild ? 'show' : ''}" 
                     id="${menuId}" style="display: ${hasActiveChild ? 'block' : 'none'};">
                    <ul class="nav-submenu-list">
                        ${item.children
                .map(child => `
                                <li class="nav-subitem">
                                    <a href="${child.url}" class="nav-sublink ${currentPath === child.url ? 'active' : ''}">
                                        <div class="nav-subicon">
                                            <i class="${child.icon}"></i>
                                        </div>
                                        <span class="nav-subtext">${child.title}</span>
                                    </a>
                                </li>
                            `).join("")}
                    </ul>
                </div>
            `

            // Add click event for submenu toggle
            const parentLink = li.querySelector('.nav-parent')
            if (parentLink) {
                parentLink.addEventListener('click', (e) => {
                    e.preventDefault()
                    this.toggleSubmenu(parentLink)
                })
            }
        } else {
            // Single item
            li.innerHTML = `
                <a href="${item.url}" class="nav-link ${isActive ? 'active' : ''}">
                    <div class="nav-icon">
                        <i class="${item.icon}"></i>
                    </div>
                    <span class="nav-text">${item.title}</span>
                </a>
            `
        }

        return li
    }

    toggleSubmenu(toggle) {
        const submenuId = toggle.dataset.submenu
        const submenu = document.getElementById(submenuId)
        const arrow = toggle.querySelector('.nav-arrow i')
        const isExpanded = toggle.getAttribute('aria-expanded') === 'true'

        // Close other submenus
        document.querySelectorAll('.nav-parent').forEach(otherToggle => {
            if (otherToggle !== toggle) {
                const otherSubmenuId = otherToggle.dataset.submenu
                const otherSubmenu = document.getElementById(otherSubmenuId)
                const otherArrow = otherToggle.querySelector('.nav-arrow i')

                otherToggle.setAttribute('aria-expanded', 'false')
                otherToggle.classList.remove('active')
                if (otherSubmenu) {
                    otherSubmenu.classList.remove('show')
                    otherSubmenu.style.display = 'none'
                }
                if (otherArrow) {
                    otherArrow.style.transform = 'rotate(0deg)'
                }
            }
        })

        // Toggle current submenu
        if (submenu) {
            const newExpanded = !isExpanded
            toggle.setAttribute('aria-expanded', newExpanded)

            if (newExpanded) {
                submenu.classList.add('show')
                submenu.style.display = 'block'
                toggle.classList.add('active')
                if (arrow) arrow.style.transform = 'rotate(180deg)'
            } else {
                submenu.classList.remove('show')
                submenu.style.display = 'none'
                toggle.classList.remove('active')
                if (arrow) arrow.style.transform = 'rotate(0deg)'
            }
        }
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
            window.location.href = "/logout"
        }
    }

    // Public method to change role dynamically
    changeRole(newRole) {
        if (SIDEBAR_CONFIG[newRole]) {
            console.log(`🔄 Changing role from ${this.currentRole} to ${newRole}`)
            this.currentRole = newRole
            this.loadUserInfo()
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
    console.log('🎬 DOM loaded, initializing SidebarManager...')

    // Wait a bit for any Thymeleaf rendering
    setTimeout(() => {
        // Initialize sidebar manager
        window.sidebarManager = new SidebarManager()

        // Global functions for backward compatibility
        window.toggleSidebar = () => window.sidebarManager.toggleSidebar()
        window.initializeSidebar = (role) => {
            if (role && window.sidebarManager) {
                window.sidebarManager.changeRole(role)
            }
        }

        console.log('✅ SidebarManager initialized successfully')
    }, 100)
})

// Debug function
window.debugSidebar = () => {
    console.log('=== SIDEBAR DEBUG ===')
    console.log('Current path:', window.location.pathname)
    console.log('Detected role:', window.sidebarManager?.currentRole)
    console.log('Current user:', window.sidebarManager?.currentUser)
    console.log('Menu items count:', window.sidebarManager?.menuItems?.length)
    console.log('Sidebar instance:', window.sidebarManager)
}

// Export for module systems
if (typeof module !== "undefined" && module.exports) {
    module.exports = SidebarManager
}