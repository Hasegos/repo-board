const form = document.getElementById('signupForm');
form.addEventListener('submit', function(e) {
    let valid = true;
    const emailPat = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    const passPat  = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/;

    const email = this.username.value.trim();
    const emailErrEl = document.getElementById('usernameError');
    if (!emailPat.test(email)) {
      emailErrEl.textContent = '유효한 이메일 형식을 입력해주세요.';
      valid = false;
    } else {
      emailErrEl.textContent = '';
    }

    const pw = this.password.value;
    const pwErrEl = document.getElementById('passwordError');
    if (!passPat.test(pw)) {
      pwErrEl.textContent = '비밀번호는 영문·숫자·특수문자 포함 8자 이상이어야 합니다.';
      valid = false;
    } else {
      pwErrEl.textContent = '';
    }

    const cpw = document.getElementById('passwordConfirm').value;
    const cpwErrEl = document.getElementById('confirmError');
    if (cpw !== pw) {
      cpwErrEl.textContent = '비밀번호가 일치하지 않습니다.';
      valid = false;
    } else {
      cpwErrEl.textContent = '';
    }

    if (!valid) e.preventDefault();
});