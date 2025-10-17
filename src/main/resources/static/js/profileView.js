document.addEventListener("DOMContentLoaded", () => {
    const checkbox = document.getElementById('profile-visibility-checkbox');

    if (checkbox) {
        checkbox.addEventListener('change', () => {
            const form = checkbox.closest('form');
            if (form) form.submit();
        });
    }
});