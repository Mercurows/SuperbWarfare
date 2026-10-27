// Layout only. Perk controls and weapon data remain static prototype placeholders.
function initResonanceLayout() {
    var frame = document.querySelector(".frame");
    if (!frame) return;

    function resizeLayout() {
        // These are AUI document coordinates; no Minecraft GUI-scale conversion.
        var width = window.innerWidth;
        var height = window.innerHeight;
        if (!(width > 0 && height > 0)) return;
        var scale = Math.min(width / 1600, height / 900);
        frame.style.width = (width / scale) + "px";
        frame.style.height = (height / scale) + "px";
        frame.style.transform = "scale(" + scale + ")";
    }

    resizeLayout();
    window.addEventListener("resize", resizeLayout);
}

document.addEventListener("DOMContentLoaded", initResonanceLayout);
