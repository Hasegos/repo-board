document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-menu-root]');
    if (!root) return;

    const btn = root.querySelector('[data-menu-button]');
    const menu = root.querySelector('[data-menu]');

    function openMenu() {
        btn.setAttribute('aria-expanded', 'true');
        menu.hidden = false;
    }
    function closeMenu() {
        btn.setAttribute('aria-expanded', 'false');
        menu.hidden = true;
    }
    function toggleMenu() {
        const expanded = btn.getAttribute('aria-expanded') === 'true';
        expanded ? closeMenu() : openMenu();
    }

    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleMenu();
    });

    // 바깥 클릭 시 닫기
    document.addEventListener('click', (e) => {
        if (!root.contains(e.target)) closeMenu();
    });

    // ESC 닫기 + 메뉴 첫 항목 포커스 이동
    document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeMenu();
    if (e.key === 'ArrowDown' && btn === document.activeElement && menu.hidden === true) {
      openMenu();
      const first = menu.querySelector('a,button');
      if (first) first.focus();
    }
    });

    // 검색어 필터링
    const form = document.querySelector('.header__search');
    form.addEventListener('submit', function (e) {
        const input = form.querySelector('[name="q"]');

        let v = input.value || '';
        v = v.replace(/<[^>]*>/g, '');
        v = v.replace(/[{}[\]<>\"']/g, '');

        input.value = v;
    });
});