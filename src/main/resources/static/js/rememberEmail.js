document.addEventListener('DOMContentLoaded', function() {
    const rememberedEmail = localStorage.getItem('rememberEmail');
    if (rememberedEmail) {
        document.getElementById('email').value = rememberedEmail;
        document.getElementById('rememberEmailCheckbox').checked =true;
    }
});