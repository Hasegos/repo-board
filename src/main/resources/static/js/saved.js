document.addEventListener('DOMContentLoaded', function (){
    async function toggleReadme(button) {
        const card = button.closest('.repo-card');
        const repoId = card.getAttribute('data-repo-id');
        const readmeContent = card.querySelector('.readme-content');
        const label = button.querySelector('.toggle-label');
        const arrow = button.querySelector('.arrow');

        const isOpen = card.classList.contains('open');

        if (!isOpen) {
            // 펼치기
            card.classList.add('open');
            card.classList.remove('collapsed');
            arrow.textContent = '▲';
            readmeContent.classList.remove('collapsed');

            // 브라우저 렌더링 한 프레임 기다리기
            await new Promise(resolve =>
                requestAnimationFrame(() => requestAnimationFrame(resolve)));

            if (!readmeContent.dataset.loaded) {
                readmeContent.textContent = '📄 README 불러오는 중...';
                try {
                    const res = await fetch(`/users/saved/repos/${repoId}/readme`);
                    const html = await res.text();
                    readmeContent.innerHTML = html;
                    readmeContent.dataset.loaded = 'true';
                    readmeContent.classList.remove('loading');
                } catch (e) {
                    readmeContent.textContent = '❌ README를 불러올 수 없습니다.';
                }
            }
        } else {
            // 접기
            card.classList.remove('open');
            card.classList.add('collapsed');
            readmeContent.classList.add('collapsed');
        }
    }
})