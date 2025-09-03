let observer;
document.addEventListener('DOMContentLoaded', function () {
    const metadata = document.getElementById('search-metadata');
    let currentPage = parseInt(metadata.dataset.currentPage) || 0;
    let currentLanguage = metadata.dataset.currentLanguage || 'java';

    let repoCache = {};
    let cachePages = new Set();
    let isLoading = false;

    const CACHE_PAGE_RANGE = 10;
    const languageColors = {
        'Java': '#b07219', 'JavaScript': '#f1e05a', 'Python': '#3572A5', 'TypeScript': '#2b7489',
        'Go': '#00ADD8', 'Rust': '#dea584', 'C++': '#f34b7d', 'C': '#555555', 'C#': '#239120',
        'PHP': '#4F5D95', 'Ruby': '#701516', 'Swift': '#ffac45', 'Kotlin': '#F18E33',
        'Dart': '#00B4AB', 'Shell': '#89e051', 'HTML': '#e34c26', 'CSS': '#1572B6'
    };

    /** 언어 색상 */
    function applyStylesToCards(cards) {
        cards.forEach(card => {
            const dot = card.querySelector('.language-dot');
            if (dot) {
                const lang = dot.dataset.language;
                if (lang && languageColors[lang]) {
                    dot.style.backgroundColor = languageColors[lang];
                }
            }
            card.classList.add('processed', 'repo-card--fade-in');
        });
    }

    /** 언어 변경 url */
    function changeLanguage(language) {
        if (currentLanguage === language) return;
        window.location.href = `/search/repositories?language=${language}`;
    }

    /** 캐싱 저장 + 범위 제한 */
    function addToCache(page,html){
        const cacheKey =  `lang:${currentLanguage}|page:${page}`;
        repoCache[cacheKey] = html;
        cachePages.add(page);

        for(const cachePage of Array.from(cachePages)){
            if(Math.abs(cachePage - page) > CACHE_PAGE_RANGE){
                const oldKey = `lang:${currentLanguage}|page:${cachePage}`;
                delete repoCache[oldKey];
                cachePages.delete(cachePage);
            }
        }
    }

    /** Github API Repo 호출 */
    function loadMoreRepositories() {
        if (isLoading) return;
        isLoading = true;

        const nextPage = currentPage + 1;
        const cacheKey =  `lang:${currentLanguage}|page:${nextPage}`;
        const spinner = document.getElementById('loading-spinner');
        const endMessage = document.getElementById('end-message');
        spinner.classList.add('loading-spinner--show');

        if(repoCache[cacheKey]){
            insertCachedHTML(repoCache[cacheKey],nextPage);
            isLoading = false;
            spinner.classList.remove('loading-spinner--show');
            return;
        }

        fetch(`/more?language=${currentLanguage}&page=${nextPage}`)
            .then(res => res.text())
            .then(html => {
                const repoGrid = document.getElementById('repo-grid');
                if (html.trim()) {
                    addToCache(nextPage, html);
                    insertCachedHTML(html,nextPage);
                } else {
                    if (loadMoreContainer) loadMoreContainer.style.display = 'none';
                    if (endMessage) endMessage.classList.remove('end-message--hidden');
                }
            })
            .catch(err => {
                console.error(err);
                alert('리포지토리를 불러오는 중 오류가 발생했습니다.');
                if (loadMoreBtn) loadMoreBtn.classList.remove('load-more__btn--hidden');
            })
            .finally(() => {
                isLoading = false;
                spinner.classList.remove('loading-spinner--show');
            });
    }

    /** HTML 삽입 및 스타일 적용 */
    function insertCachedHTML(html, nextPage){
        const repoGrid = document.getElementById('repo-grid');
        repoGrid.insertAdjacentHTML('beforeend', html);

        const newCards = repoGrid.querySelectorAll('.repo-card:not(.processed)');
        applyStylesToCards(newCards);
        currentPage = nextPage;

        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        const hasNext = tempDiv.children.length === 50;

        if (!hasNext) {
            const endMsg = document.getElementById('end-message');
            if (endMsg) endMsg.classList.remove('end-message--hidden');
            if(observer && observer.disconnect) observer.disconnect();
        }
    }

    applyStylesToCards(document.querySelectorAll('.repo-card'));
    window.changeLanguage = changeLanguage;

    /** 무한 스크롤 */
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if(entry.isIntersecting && !isLoading){
                loadMoreRepositories();
            }
        });
    }, {
        rootMargin:'300px',
        threshold: 1.0
    });

    const scrollTarget = document.getElementById('scroll-trigger');
    if(scrollTarget) observer.observe(scrollTarget);
});