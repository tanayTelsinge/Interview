## char count 

```js

function charCount(str = 'tasty') {
  let map = {};
  for(const ch of str) {
        map[ch] = (map[ch] || 0) + 1;
  }
    console.log(map);
}

charCount(); 
```

## First and Last Capitalized

```js
function capitalizeFirstAndLast(str = 'hello world') {
    if (str.length === 0) return str;
    if (str.length === 1) return str.toUpperCase();
    
    const firstChar = str.charAt(0).toUpperCase();
    const lastChar = str.charAt(str.length - 1).toUpperCase();
    const middlePart = str.slice(1, -1);
    
    return firstChar + middlePart + lastChar;
}
console.log(capitalizeFirstAndLast()); 
```

## Remainder and Quotient without / and %

```js
function calculateRemainderAndQuotient(dividend, divisor) {
    if (divisor === 0) {
        throw new Error("Divisor cannot be zero");
    }

    let quotient = 0;
    let remainder = dividend;

    while (remainder >= divisor) {
        remainder -= divisor;
        quotient++;
    }

    return { quotient, remainder };
}
const result = calculateRemainderAndQuotient(13, 7);
console.log(`Quotient: ${result.quotient}, Remainder: ${result

.remainder}`);
```
## First Non Repeating Character

```js
function firstNonRepeatingCharacter(str = 'swiss') {
    const charCount = {};

    for (const char of str) {
        charCount[char] = (charCount[char] || 0) + 1;
    }

    for (const char of str) {
        if (charCount[char] === 1) {
            return char;
        }
    }

    return null; 
}
console.log(firstNonRepeatingCharacter()); 
```