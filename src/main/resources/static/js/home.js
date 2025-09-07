let observer;
let isRefresh = false;
document.addEventListener('DOMContentLoaded', function () {
    let isLoading = false;
    let lastLoadAt = 0;
    let retryCount = 0;
    let isRateLimited = false;
    let rateLimitEndTime = 0;
    let isEnd = false;
    const repoCache = new Map();

    const MAX_CACHE_SIZE = 20;
    const MAX_RETRY_COUNT = 3;
    const RATE_LIMIT_RETRY_DELAY = 30000; // 30초
    const NORMAL_RETRY_DELAY = 3000; // 3초

    const metadata = document.getElementById('search-metadata');
    const url = new URL(window.location.href);
    isRefresh = url.searchParams.get('refresh') === 'true';

    if (isRefresh) {
        url.searchParams.delete('refresh');
        history.replaceState(null, '', url.toString());
    }

    let currentPage = parseInt(metadata.dataset.currentPage) || 0;
    let currentLanguage = metadata.dataset.currentLanguage || 'java';

    const refreshBtn = document.getElementById('refresh-btn');
    if(refreshBtn){
        refreshBtn.addEventListener('click', () => {
           const nextUrl = new URL(window.location.href);
           nextUrl.searchParams.set('refresh', 'true');
           nextUrl.searchParams.set('page', '0'); // 새로고침은 항상 첫 페이지로
           window.location.href = nextUrl.toString();
        })
    }

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
        window.location.href = `/?language=${language}`;
    }

     /** 스피너 메시지 업데이트 */
    function updateSpinnerMessage(isRateLimit = false) {
        const spinner = document.getElementById('loading-spinner');
        const spinnerText = spinner.querySelector('.loading-text') ||
                           spinner.querySelector('span') ||
                           document.createElement('span');

        if (!spinnerText.parentNode) {
            spinner.appendChild(spinnerText);
        }

        spinnerText.textContent = isRateLimit
        ? '리포지토리를 불러오는데 시간이 걸립니다.'
        : '더 많은 리포지토리 불러오는 중';
    }

     /** Rate Limit 체크 */
    function isCurrentlyRateLimited() {
        return isRateLimited && Date.now() < rateLimitEndTime;
    }

    /** Rate Limit 설정 */
    function setRateLimit() {
        isRateLimited = true;
        rateLimitEndTime = Date.now() + RATE_LIMIT_RETRY_DELAY;
        updateSpinnerMessage(true);
        console.log('Rate limit 감지: 30초 후 재시도');
    }

    /** Rate Limit 해제 */
    function clearRateLimit() {
        isRateLimited = false;
        rateLimitEndTime = 0;
        retryCount = 0;
        updateSpinnerMessage(false);
    }

    /** 캐싱 저장 + 범위 제한 (LRU 방식) */
    function addToCache(page,html){
        const cacheKey = `lang:${currentLanguage}|page:${page}|refresh:${isRefresh}`;

        if(repoCache.has(cacheKey)){
            repoCache.delete(cacheKey);
        }
        repoCache.set(cacheKey, html);

       if(repoCache.size > MAX_CACHE_SIZE){
            const oldestKey = repoCache.keys().next().value;
            repoCache.delete(oldestKey);
       }
    }

    /** Github API Repo 호출 */
    function loadMoreRepositories() {
        const now = Date.now();
        const nextPage = currentPage + 1;
        const cacheKey = `lang:${currentLanguage}|page:${nextPage}`;

        if(repoCache.has(cacheKey)){
          insertCachedHTML(repoCache.get(cacheKey),nextPage);
          return;
        }

        if (isEnd || isLoading || now - lastLoadAt < 1000 || isCurrentlyRateLimited()) return;
        isLoading = true;
        lastLoadAt = now;

        const spinner = document.getElementById('loading-spinner');
        const endMessage = document.getElementById('end-message');

        spinner.classList.add('loading-spinner--show');
        updateSpinnerMessage(isRateLimited);

        const requestUrl = `/api/repos?language=${currentLanguage}&page=${nextPage}`

        fetch(requestUrl)
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                }
                return res.text();
            })
            .then(html => {
                const repoGrid = document.getElementById('repo-grid');
                if (html.trim()) {
                    addToCache(nextPage, html);
                    insertCachedHTML(html,nextPage);
                    if (isRateLimited) {
                        clearRateLimit();
                    }
                    retryCount = 0;
                } else {
                    const loadMoreContainer = document.getElementById('load-more-container');
                    if (loadMoreContainer) loadMoreContainer.style.display = 'none';
                    if (endMessage) endMessage.classList.remove('end-message--hidden');
                    if (observer && observer.disconnect) observer.disconnect();
                    isEnd = true;
                }
            })
            .catch(err => {
                console.error("API 호출 실패 : ", err);
                handleApiError(err, nextPage);
            })
            .finally(() => {
                isLoading = false;
                spinner.classList.remove('loading-spinner--show');
            });
    }

    /** API 오류 처리 */
    function handleApiError(err, nextPage) {
        const isRateLimitError = err.message.includes('429') ||
                                err.message.includes('Rate') ||
                                err.message.includes('rate');
        if (isRateLimitError) {
            // Rate Limit 에러인 경우
            setRateLimit();
            scheduleRetry(nextPage, RATE_LIMIT_RETRY_DELAY);
        } else if (retryCount < MAX_RETRY_COUNT) {
            // 일반 오류인 경우 최대 3번 재시도
            retryCount++;
            console.warn(`재시도 ${retryCount}회...`);
            scheduleRetry(nextPage, NORMAL_RETRY_DELAY);
        } else {
            // 최대 재시도 횟수 초과
            console.error('최대 재시도 횟수 초과');
            updateSpinnerMessage(true);
            showErrorState();
        }
    }

    /** 재시도 스케줄링 */
    function scheduleRetry(nextPage, delay) {
        setTimeout(() => {
            if (isRateLimited && delay === RATE_LIMIT_RETRY_DELAY) {
                clearRateLimit();
            }

            // 사용자가 여전히 스크롤 영역에 있는지 확인
            const scrollTarget = document.getElementById('scroll-trigger');
            if (scrollTarget && isElementInViewport(scrollTarget)) {
                observer.observe(scrollTarget);
                loadMoreRepositories();
            }
        }, delay);
    }

    /** 요소가 뷰포트에 있는지 확인 */
    function isElementInViewport(el) {
        const rect = el.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    /** 오류 상태 표시 */
    function showErrorState() {
        const spinner = document.getElementById('loading-spinner');
        const endMessage = document.getElementById('end-message');

        spinner.classList.remove('loading-spinner--show');
        if (endMessage) {
            endMessage.textContent =  '리포지토리를 불러오는데 시간이 걸립니다.';
            endMessage.classList.remove('end-message--hidden');
        }
        if (observer && observer.disconnect) {
            observer.disconnect();
        }
        retryCount = 0;
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
            isEnd = true;
        }
    }

    applyStylesToCards(document.querySelectorAll('.repo-card'));
    window.changeLanguage = changeLanguage;

    /** 무한 스크롤 */
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if(entry.isIntersecting && !isLoading && !isCurrentlyRateLimited()){
                loadMoreRepositories();
            }
        });
    }, {
        rootMargin:'1500px',
        threshold: 0
    });

    const scrollTarget = document.getElementById('scroll-trigger');
    if(scrollTarget) observer.observe(scrollTarget);

    window.addEventListener('scroll', () => {
        if (isLoading) return;

        const scrollHeight = document.documentElement.scrollHeight;
        const scrollTop = window.scrollY;
        const clientHeight = document.documentElement.clientHeight;

        if (scrollTop + clientHeight >= scrollHeight - 300) {
            loadMoreRepositories();
        }
    });
});