
document.addEventListener('input', handleChange);


function handleChange() {
    let num1 = document.getElementById('firstnum').value;
    let num2 = document.getElementById('secondnum').value;

    if (isNaN(num1) || isNaN(num2)) return; 
    num1 = Number(num1);
    num2 = Number(num2);
    let operation = document.querySelector('input[type=radio][name=operation]:checked');
    let res = null;
    if (operation) {
        switch (operation.id) {
            case 'add':
                res = num1 + num2;
                break;
            case 'substract':
                res = num1 - num2;
                break;
            case 'multiply':
                res = num1 * num2;
                break;
            case 'divide':
                res = num1 / num2;
                break;
        }
    }
    if (res != null) {
        document.getElementById('result').value = res;
    }
}
