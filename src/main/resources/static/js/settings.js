document.addEventListener('DOMContentLoaded', () => {
    const tabs = document.querySelectorAll('.settings__tab[href^="#"]');
    if (tabs.length === 0) return;

    const setActiveByHash = () => {
        const current = location.hash || tabs[0].getAttribute('href');
        tabs.forEach(a => {
            const active = a.getAttribute('href') === current;
            a.classList.toggle('settings__tab--active', active);
            a.setAttribute('aria-current', active ? 'page' : 'false');
        });
    };

    tabs.forEach(a => {
        a.addEventListener('click', (e) => {
            const target = document.querySelector(a.getAttribute('href'));
            if (!target) return;
            e.preventDefault();
            history.pushState(null, '', a.getAttribute('href'));
            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            setActiveByHash();
        });
    });

    window.addEventListener('hashchange', setActiveByHash);
    setActiveByHash();

    const form = document.getElementById('changePwForm');
    if (!form) return;

    const passPat = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/;
    const cur = form.querySelector('#cur');
    const npw = form.querySelector('#new');
    const cpw = form.querySelector('#new2');

  const curErr = document.getElementById('curError');
  const newErr = document.getElementById('newError');
  const cfmErr = document.getElementById('confirmError');

    const clearErrors = () => {
        curErr.textContent = '';
        newErr.textContent = '';
        cfmErr.textContent = '';
    };

    npw.addEventListener('input', () => {
        if (!npw.value) {
            newErr.textContent = '';
            return;
        }

        if (!passPat.test(npw.value)){
            newErr.textContent = '영문·숫자·특수문자 포함 8자 이상이어야 합니다.';
        } else if (npw.value === cur.value){
            newErr.textContent = '현재 비밀번호와 다르게 설정하세요.';
        }
        else {
            newErr.textContent = '';
        }
    });

    cpw.addEventListener('input', () => {
        if (!cpw.value) {
            cfmErr.textContent = '';
            return;
        }

        cfmErr.textContent = (cpw.value !== npw.value)
            ? '비밀번호가 일치하지 않습니다.'
            : '';
    });

    form.addEventListener('submit', (e) => {
        clearErrors();
        let value = true;

        if (!cur.value) {
            curErr.textContent = '현재 비밀번호를 입력하세요.';
            value = false;
        }

        if (!passPat.test(npw.value)) {
            newErr.textContent = '영문·숫자·특수문자 포함 8자 이상이어야 합니다.';
            value = false;
        } else if (npw.value === cur.value) {
            newErr.textContent = '현재 비밀번호와 동일하게 설정할 수 없습니다.';
            value = false;
        }

        if (cpw.value !== npw.value) {
            cfmErr.textContent = '비밀번호가 일치하지 않습니다.';
            value = false;
        }

        if (!value) {
            e.preventDefault();
            return;
        }

        if (!confirm('새 비밀번호로 변경합니다. 계속할까요?')){
            e.preventDefault();
        }
    });

    const deleteForm = document.getElementById('deleteAccountForm');
    if(deleteForm){
        deleteForm.addEventListener('submit', (e) => {
             console.log('삭제 취소됨');
            if(!confirm('계정 탈되시 모든 내용이 삭제됩니다.')){
                e.preventDefault();
            }
        });
    }
});