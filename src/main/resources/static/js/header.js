document.addEventListener('DOMContentLoaded', function () {
    const menuToggle = document.getElementById('menuToggle');
    const mobileMenu = document.getElementById('mobileMenu');

     menuToggle?.addEventListener('click', () => {
        mobileMenu.classList.toggle('header__mobile-menu--active');
        const isOpen = mobileMenu.classList.contains('header__mobile-menu--active');
        menuToggle.setAttribute('aria-label', isOpen ? '메뉴 닫기' : '메뉴 열기');
      });

      const searchToggle = document.getElementById('searchToggle');
      const headerSearchForm = document.getElementById('headerSearchForm');

      searchToggle?.addEventListener('click', () => {
        headerSearchForm.classList.toggle('header__search--active');
        const isOpen = headerSearchForm.classList.contains('header__search--active');
        searchToggle.setAttribute('aria-label', isOpen ? '검색 닫기' : '검색 열기');
      });

    const root = document.querySelector('[data-menu-root]');
    if (root) {
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

        document.addEventListener('click', (e) => {
            if (!root.contains(e.target)) closeMenu();
        });

        document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeMenu();
        if (e.key === 'ArrowDown' && btn === document.activeElement && menu.hidden === true) {
          openMenu();
          const first = menu.querySelector('a,button');
          if (first) first.focus();
        }
        });
    }

    const form = document.querySelector('.header__search');
    form.addEventListener('submit', function (e) {
        const input = form.querySelector('[name="q"]');

        let v = input.value || '';
        v = v.replace(/<[^>]*>/g, '');
        v = v.replace(/[{}[\]<>\"']/g, '');

        input.value = v;
    });
});