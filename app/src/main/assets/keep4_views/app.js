window.onload = function () {
  var STORAGE_SWITCH_KEY = "STORAGE_SWITCH_KEY";

  var switchEl = document.querySelector("#switch");
  var mainItemEl = document.querySelector("#main-item");
  var tipsDialogEl = document.querySelector(".example-headline-slot");
  var moreBtnEl = document.querySelector(".more-btn");
  var switchCache = JSON.parse(
    localStorage.getItem(STORAGE_SWITCH_KEY) || "true"
  );
  switchEl.checked = switchCache;
  mainItemEl.addEventListener("click", () => {
    switchEl.checked = !switchEl.checked;
    localStorage.setItem(STORAGE_SWITCH_KEY, JSON.stringify(switchEl.checked));
  });

  switchEl.addEventListener("click", (event) => {
    event.stopPropagation();
    setTimeout(() => {
      localStorage.setItem(
        STORAGE_SWITCH_KEY,
        JSON.stringify(switchEl.checked)
      );
    });
  });

  moreBtnEl.addEventListener("click", () => {
    tipsDialogEl.open = true;
  });
};
