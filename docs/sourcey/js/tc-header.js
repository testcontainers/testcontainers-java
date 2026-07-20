const mobileToggle = document.getElementById("mobile-menu-toggle");
const mobileSubToggle = document.getElementById("mobile-submenu-toggle");
function toggleMobileMenu() {
    document.body.classList.toggle('mobile-menu');
    document.body.classList.toggle("mobile-tc-header-active");
}
function toggleMobileSubmenu() {
    document.body.classList.toggle('mobile-submenu');
}
if (mobileToggle)
    mobileToggle.addEventListener("click", toggleMobileMenu);
if (mobileSubToggle)
    mobileSubToggle.addEventListener("click", toggleMobileSubmenu);

const allParentMenuItems = document.querySelectorAll("#site-header .menu-item.has-children");
function clearActiveMenuItem() {
    document.body.classList.remove("tc-header-active");
    allParentMenuItems.forEach((item) => {
        item.classList.remove("active");
    });
}
function setActiveMenuItem(e) {
    clearActiveMenuItem();
    e.currentTarget.closest(".menu-item").classList.add("active");
    document.body.classList.add("tc-header-active");
}
allParentMenuItems.forEach((item) => {
    const trigger = item.querySelector(":scope > a, :scope > button");

    trigger.addEventListener("click", (e) => {
        if (e.currentTarget.closest(".menu-item").classList.contains("active")) {
            clearActiveMenuItem();
        }  else {
            setActiveMenuItem(e);
        }
    });

    trigger.addEventListener("mouseenter", (e) => {
        setActiveMenuItem(e);
    });

    item.addEventListener("mouseleave", (e) => {
        clearActiveMenuItem();
    });
});