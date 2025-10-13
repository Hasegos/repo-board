document.addEventListener("DOMContentLoaded", () => {
    let logPage = 0;

    const tabs = document.querySelectorAll(".admin__tab-button");
    const contents = document.querySelectorAll(".admin__tab-content");
    const logList = document.getElementById("logList");
    const loadMoreBtn = document.getElementById("loadMoreBtn");

    function openTab(tabId) {
        tabs.forEach(btn => btn.classList.remove("admin__tab-button--active"));
        contents.forEach(content => content.classList.remove("admin__tab-content--active"));

        document.querySelector(`.admin__tab-button[data-tab="${tabId}"]`)?.classList.add("admin__tab-button--active");
        document.getElementById(tabId)?.classList.add("admin__tab-content--active");
    }

    function loadLogs() {
        fetch(`/admin/logs?page=${logPage}`)
            .then(res => res.json())
            .then(lines => {
                if (lines.length === 0) {
                    loadMoreBtn.disabled = true;
                    loadMoreBtn.textContent = "더 이상 로그 없음";
                    return;
                }
                lines.forEach(line => {
                    const li = document.createElement("li");
                    li.textContent = line;
                    li.classList.add("admin__log-item");
                    logList.appendChild(li);
                });
                logPage++;
            });
    }

    // 탭 버튼 클릭 이벤트 등록
    tabs.forEach(btn => {
        btn.addEventListener("click", () => {
            const tab = btn.dataset.tab;
            history.replaceState(null, "", `#${tab}`);
            openTab(tab);
        });
    });

    // URL 해시로 기본 탭 열기
    const initialTab = window.location.hash.replace("#", "") || "list";
    openTab(initialTab);

    // 로그 탭 초기 로딩
    if (initialTab === "log") {
        loadLogs();
    }

    // 더보기 버튼 클릭 시 로그 로드
    if (loadMoreBtn) {
        loadMoreBtn.addEventListener("click", loadLogs);
    }
});