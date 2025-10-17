async function toggleReadme(button) {
    const card = button.closest('.repo-card');
    const repoId = card.getAttribute('data-repo-id');
    const readmeContent = card.querySelector('.readme-content');
    const label = button.querySelector('.toggle-label');
    const arrow = button.querySelector('.arrow');

    const isOpen = card.classList.contains('open');

    if (!isOpen) {
        card.classList.add('open');
        card.classList.remove('collapsed');
        arrow.textContent = '▲';
        readmeContent.classList.remove('collapsed');

        await new Promise(resolve =>
            requestAnimationFrame(() => requestAnimationFrame(resolve)));

        if (!readmeContent.dataset.loaded) {
            readmeContent.textContent = '📄 README 불러오는 중...';
            try {
                const res = await fetch(`/users/saved/repos/${repoId}/readme`);
                const html = await res.text();

                if(!res.ok){
                    throw new Error(html);
                }

                readmeContent.innerHTML = html;
                readmeContent.dataset.loaded = 'true';
                readmeContent.classList.remove('loading');
            } catch (e) {
                readmeContent.textContent = '❌ ${e.message}.';
            }
        }
    } else {
        card.classList.remove('open');
        card.classList.add('collapsed');
        readmeContent.classList.add('collapsed');
    }
}