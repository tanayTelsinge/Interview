document.addEventListener('input', handleBackgroundChange)


function handleBackgroundChange() {
    let val = document.getElementById('num').value;
    if(isNaN(val)) return;
    val = Number(val);
    if (val != 0) {
        document.body.className = val % 2 == 0 ? 'yellow' : 'green';
    } else {
        document.body.style.backgroundColor = 'white';
    }
}