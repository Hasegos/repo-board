async function toggleReadme(button) {
    const card = button.closest('.repo-card');
    const repoId = card.getAttribute('data-repo-id');
    const readmeBox = card.querySelector('.repo-card__readme');
    const readmeContent = readmeBox.querySelector('.readme-content');
    const label = button.querySelector('.toggle-label');
    const arrow = button.querySelector('.arrow');

    const isVisible = readmeBox.style.display === 'block';

    if (isVisible) {
        // 접기
        readmeBox.style.display = 'none';
        label.textContent = 'Readme 상세히 보기';
        arrow.textContent = '▼';
    } else {
        // 펼치기
        readmeBox.style.display = 'block';
        label.textContent = 'Readme 닫기';
        arrow.textContent = '▲';

        if (!readmeContent.dataset.loaded) {
            readmeContent.textContent = '📄 README 불러오는 중...';
            try {
                const res = await fetch(`/users/saved/repos/${id}/readme`);
                const text = await res.text();
                readmeContent.textContent = text;
                readmeContent.dataset.loaded = 'true';
                readmeContent.classList.remove('loading');
            } catch (e) {
                readmeContent.textContent = '❌ README를 불러올 수 없습니다.';
            }
        }
    }
}