// "Forgot password" link: the token arrives in the fragment (#token=...), which the browser never sends to a server
// (no access log, no Referer). Copied into the form, then removed from the address bar.
document.addEventListener('DOMContentLoaded', function () {
  var field = document.getElementById('token');
  if (!field) return;
  var hash = window.location.hash;
  if (hash.indexOf('#token=') === 0) {
    field.value = decodeURIComponent(hash.substring('#token='.length));
    history.replaceState(null, '', window.location.pathname);
  }
  if (!field.value) {
    document.getElementById('no-token').hidden = false;
    document.getElementById('reset-form').hidden = true;
  }
});
