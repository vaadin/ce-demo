window.addEventListener('resize', e => window._splitLayout && updateSplitLayout(window._splitLayout));

window._setSplitLayout = function (splitLayout) {
    window._splitLayout = splitLayout;
    updateSplitLayout(splitLayout);
}

function updateSplitLayout(splitLayout) {
    const narrowScreen = window.innerWidth < 600;
    splitLayout.orientation = narrowScreen ? 'vertical' : 'horizontal';
}
