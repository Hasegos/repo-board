(function () {
  const sidebar = document.querySelector('.settings__sidebar');
  if (!sidebar) return;

  const tabs = Array.from(sidebar.querySelectorAll('.settings__tab[href^="#"]'));
  if (tabs.length === 0) return;

  const setActiveByHash = () => {
    // 현재 해시가 없으면 첫 탭을 기본으로
    const current = location.hash || tabs[0].getAttribute('href');
    tabs.forEach(a => {
      const active = a.getAttribute('href') === current;
      a.classList.toggle('settings__tab--active', active);
      a.setAttribute('aria-current', active ? 'page' : 'false');
    });
  };

  // 클릭 시 부드럽게 스크롤 + 해시 갱신 + active 즉시 반영
  tabs.forEach(a => {
    a.addEventListener('click', (e) => {
      const target = document.querySelector(a.getAttribute('href'));
      if (!target) return;  // 브라우저 기본 동작으로 둬도 됨
      e.preventDefault();
      history.pushState(null, '', a.getAttribute('href'));
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      setActiveByHash();
    });
  });

  // 해시가 주소창에서 바뀌는 경우(back/forward 등)
  window.addEventListener('hashchange', setActiveByHash);

  // 초기 상태 세팅
  setActiveByHash();
})();

/* 비밀번호 변경 메서드 */
(function () {
  const form = document.getElementById('changePwForm');
  if (!form) return;

  const passPat = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/;
  const cur = form.querySelector('#cur');
  const npw = form.querySelector('#new');
  const cpw = form.querySelector('#new2');

  const curErr = document.getElementById('curError');
  const newErr = document.getElementById('newError');
  const cfmErr = document.getElementById('confirmError');

  const clear = () => { curErr.textContent = ''; newErr.textContent = ''; cfmErr.textContent = ''; };

  // 실시간 피드백(선택)
  npw.addEventListener('input', () => {
    if (!npw.value) { newErr.textContent = ''; return; }
    if (!passPat.test(npw.value)) newErr.textContent = '영문·숫자·특수문자 포함 8자 이상이어야 합니다.';
    else if (npw.value === cur.value) newErr.textContent = '현재 비밀번호와 다르게 설정하세요.';
    else newErr.textContent = '';
  });
  cpw.addEventListener('input', () => {
    if (!cpw.value) { cfmErr.textContent = ''; return; }
    cfmErr.textContent = (cpw.value !== npw.value) ? '비밀번호가 일치하지 않습니다.' : '';
  });

  form.addEventListener('submit', (e) => {
    let ok = true; clear();

    if (!cur.value) { curErr.textContent = '현재 비밀번호를 입력하세요.'; ok = false; }

    if (!passPat.test(npw.value)) {
      newErr.textContent = '영문·숫자·특수문자 포함 8자 이상이어야 합니다.'; ok = false;
    } else if (npw.value === cur.value) {
      newErr.textContent = '현재 비밀번호와 동일하게 설정할 수 없습니다.'; ok = false;
    }

    if (cpw.value !== npw.value) { cfmErr.textContent = '비밀번호가 일치하지 않습니다.'; ok = false; }

    if (!ok) { e.preventDefault(); return; }

    // 안전 확인(원하면 유지)
    if (!confirm('새 비밀번호로 변경합니다. 계속할까요?')) e.preventDefault();
  });
})();