## 1. Fundamentals

- JS in single threaded with event loop.
- var, let, const differences - var is function scoped, let and const are block scoped. const cannot be reassigned.
- Data types in JS - primitive (string, number, boolean, null, undefined, symbol) and non-primitive (object, array, function).
- null vs undefined - null is an assigned value, undefined means a variable has been declared but not assigned a value.
```js
// let a; // undefined
// let b = null; // null
```
- Immutability - primitive data types can be mutable, objects and arrays are mutable.

```js
// Example of mutable and immutable
const a = [1, 2, 3]; // mutable
a.push(4); // valid
console.log(a); // [1, 2, 3, 4]
const b = 5; // immutable
b = 6; // invalid
```

- `==` vs `===` 
- `==` checks for value equality with type conversion, `===` checks for both value and type equality without type conversion.

- Note : Prefer `===`
- Common cases where `==` returns true for different types:
```js
0 == "0"      // true   (string "0" is converted to number 0)
false == 0    // true   (false becomes 0)
"" == 0        // true   (empty string becomes 0)

-----

0 === "0"     // false  (number vs string)
false === 0   // false  (boolean vs number)
"" === 0       // false  (string vs number)

```
---
### 2. Hoisting
- declarations moved to top (JS default behavior).
- only declarations are hoisted, not initializations.
- `var` becomes `undefined`
- `let` and `const` are in `Temporal Dead Zone` (TDZ) until initialized.
- means if u try to access let/const before initialization, it throws ReferenceError. ***`var` does not have TDZ.***
- Functions declared with `function` are hoisted completely (declaration + definition), ***not function expressions or arrow functions.***
```js
console.log(x); // Error: Cannot access 'x' before initialization
let x = 10;

console.log(a); // undefined (allowed)
var a = 5;
```

```js
console.log(foo()); // "Hello"
function foo() {
    return "Hello";
}

console.log(bar()); // TypeError: bar is not a function
var bar = function() {
    return "World";
};

//arrow function
console.log(baz()); // ReferenceError: Cannot access 'baz' before initialization
const baz = () => {
    return "!";
};
```
---
### 3. Scopes and Closures

- Scopes - Block < Function < Global.
- Closure - inner function remembers outer function's scope even after outer function has executed.
```js
function outer() {
    let count = 0;
    return function inner() {
        count++;
        return count;
    };
}

const counter = outer();
console.log(counter()); // 1
console.log(counter()); // 2
```
- Used for:
    - Data privacy -  keep variables private, use getter/setter functions.
    - Currying - fn has too many arguments, break into smaller functions.
    - Function factories - function that create other function with custom params.
    - Debouncing/throttling  - control rate of function execution.

```js
//data privacy
function makeCounter() {
    let count = 0;  // private

    return {
        inc: function() { count++; return count; },
        get: function() { return count; }
    };
}

let c = makeCounter();
c.inc();   // 1
c.inc();   // 2
c.get();   // 2 (count is private)

//currying
function add(a) {
    return function(b) {
        return a + b;
    };
}
const add5 = add(5);
console.log(add5(3)); // 8

//function factory
function makeMultiplier(factor) {
    return function(x) {
        return x * factor;
    };
}

const double = makeMultiplier(2);
console.log(double(5)); // 10

//debouncing
function debounce(func, delay) {
    let timeoutId;
    return function(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            func.apply(this, args);
        }, delay);
    };
}
const log = debounce((msg) => console.log(msg), 1000);
log("Hello"); // logs "Hello" after 1 second

//throttling 
function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

const logThrottled = throttle((msg) => console.log(msg), 2000);
logThrottled("Hello"); // logs "Hello" immediately
logThrottled("World"); // ignored if called within 2 seconds
```
---

## 4. This keyword
- Based from which context function is called.
- In global context, `this` refers to the global object (window in browsers).
- In object method, `this` refers to the object.
- In constructor function, `this` refers to the newly created object.
- In arrow functions, `this` is lexically bound (inherits from surrounding scope).
- Can be explicitly set using `call`, `apply`, or `bind`.
- `call` and `apply` invoke the function immediately, `bind` returns a new function with `this` bound.
```js
//global context
console.log(this); // window (in browsers)

//object method
const obj = {
    name: "Alice",
    greet: function() {
        console.log(this.name);
    }
};
obj.greet(); // "Alice"

//constructor function
function Person(name) {
    this.name = name;
}

const p = new Person("Bob");
console.log(p.name); // "Bob"

//arrow function
const arrowFunc = () => {
    console.log(this); // inherits from surrounding scope
};
arrowFunc();
```
```js

//explicitly setting this
function greet() {
    console.log(this.name);
}

const person1 = { name: "Charlie" };
const person2 = { name: "Dave" };
greet.call(person1); // "Charlie" - call invokes immediately
greet.apply(person2); // "Dave" - apply invokes immediately
//call vs apply - call takes args as comma separated, apply takes args as array
const boundGreet = greet.bind(person1); //bind returns new function
boundGreet(); // "Charlie"
```
---

## 5. Prototypes and OOP
- JS uses prototype inheritance.
- Every object has a hidden `__proto__` pointer to its prototype. (shared properties/methods).
- Classes are syntactic sugar over prototypes.
- Key concepts:
    - Encapsulation - bundling data and methods.
    - Inheritance - child class inherits from parent class.
    - Polymorphism - same method behaves differently based on object.
    - Abstraction - hide complex implementation details.

```js
//prototype inheritance
function Vehicle(name) {
  this.name = name;
} 
Vehicle.prototype.drive = function() {
  console.log(`${this.name} is being driven`);
};

const car = new Vehicle('car'); 
car.drive();  // car is being driven
// car.__proto__ points to Vehicle.prototype
```

## 6. Async JS
- JS uses event loop + task queue + microtask queue.
- microtasks (Promises) execute before tasks (setTimeout).
- Event loop checks call stack, if empty, processes microtasks then tasks.
- Due to this, Promises resolve before setTimeout even with 0ms delay.
- Async behaviors:
    - Callbacks - functions passed as arguments, executed later.
    - Promises - represent future value. 3 states -> pending, resolved, rejected.    
    - Async/Await - syntactic sugar over Promises, makes async code look synchronous.
```js
//callback
function fetchData(callback) {
    setTimeout(() => {
        callback("Data received");
    }, 1000);
}
fetchData((data) => {
    console.log(data); // "Data received" after 1 second
});
```
```js
//promise
function fetchData() {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            resolve("Data received");
        }, 1000);
    });
}
fetchData()
    .then((data) => {
        console.log(data); // "Data received" after 1 second
    })
    .catch((error) => {
        console.error(error);
    });
```
```js
//async/await
async function fetchData() {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            resolve("Data received");
        }, 1000);
    });
}

async function getData() {
    try {
        const data = await fetchData();
        console.log(data); // "Data received" after 1 second
    } catch (error) {
        console.error(error);
    }
}
getData();
```
## 7. DOM Manipulation
- DOM (Document Object Model) represents HTML as a tree of nodes.
- Event phases : capturing, target, bubbling.
- `stopPropagation` when event like click, it goes up HTML structure (bubbling) or down (capturing). This method stops further propagation.
- `preventDefault` stops default browser behavior (like clicking href, opens link).

- Common methods:
    - `getElementById`, `getElementsByClassName`, `getElementsByTagName` - select elements.
    - `querySelector`, `querySelectorAll` - select elements using CSS selectors.
    - `createElement`, `appendChild`, `removeChild` - create and manipulate elements.
    - `addEventListener` - attach event handlers.

- Storage:
    - `localStorage` - persistent storage, data remains after browser is closed.
    - `sessionStorage` - data cleared when session ends (browser/tab closed).
    - `cookies` - small data stored with expiration, sent with every HTTP request.

- Fetch basics:
    - `fetch` API for making network requests, returns a Promise.
    - Can handle JSON, text, blobs, etc.
```js
//selecting elements
const element = document.getElementById("myId");
const elements = document.getElementsByClassName("myClass");
const items = document.querySelectorAll(".item");
//creating and appending elements
const newDiv = document.createElement("div");
newDiv.textContent = "Hello World";
document.body.appendChild(newDiv);
//event handling
element.addEventListener("click", function(event) {
    event.preventDefault(); // prevent default action - e.g., link navigation, form submission
    console.log("Element clicked");
});

//stop propagation example
const parent = document.getElementById("parent");
const child = document.getElementById("child");
parent.addEventListener("click", function() {
    console.log("Parent clicked");
});
child.addEventListener("click", function(event) {
    event.stopPropagation(); // stops event from bubbling to parent
    console.log("Child clicked");
});
```
```js
//localStorage and sessionStorage
localStorage.setItem("key", "value");
console.log(localStorage.getItem("key")); // "value"

sessionStorage.setItem("sessionKey", "sessionValue");
console.log(sessionStorage.getItem("sessionKey")); // "sessionValue"
```
```js
//fetch API
fetch("https://api.example.com/data")
    .then(response => response.json())
    .then(data => {
        console.log(data); // handle the data
    })
    .catch(error => {
        console.error("Error fetching data:", error);
    });
```
---

## 8. Array Skills
- Common methods:
    - `forEach` - iterate over array, no return value.
    - `map` - transform array, returns new array.
    - `filter` - filter elements based on condition, returns new array.
    - `reduce` - accumulate values into single output.
    - `find` - find first element matching condition.
    - `some` - checks if at least one element matches condition.
    - `every` - checks if all elements match condition.
```js

const numbers = [1, 2, 3, 4, 5];
//forEach
numbers.forEach(num => console.log(num)); // logs each number
//map
const doubled = numbers.map(num => num * 2);
console.log(doubled); // [2, 4, 6, 8, 10]
//filter
const evens = numbers.filter(num => num % 2 === 0);
console.log(evens); // [2, 4]
//reduce
const sum = numbers.reduce((acc, num) => acc + num, 0);
console.log(sum); // 15
//find
const firstEven = numbers.find(num => num % 2 === 0);
console.log(firstEven); // 2
//some
const hasEven = numbers.some(num => num % 2 === 0);
console.log(hasEven); // true
//every
const allPositive = numbers.every(num => num > 0);
console.log(allPositive); // true
```
---

## 9. Loops

- `for` loop - traditional loop with initialization, condition, increment.
- `while` loop - continues while condition is true.
- `do...while` loop - executes at least once, then continues while condition is true.
- `for...in` loop - iterates over enumerable properties of an object (not recommended for arrays).
- `for...of` loop - iterates over iterable objects (arrays, strings, etc.).
```js
//for loop
for (let i = 0; i < 5; i++) {
    console.log(i); // 0, 1, 2, 3, 4
}
```
```js
//while loop
let i = 0;
while (i < 5) {
    console.log(i); // 0, 1, 2, 3, 4
    i++;
}
```
```js
//do...while loop
let j = 0;
do {
    console.log(j); // 0, 1, 2, 3, 4
    j++;
} while (j < 5);
```
```js

//for...in loop
const obj = { a: 1, b: 2, c: 3 };
for (let key in obj) {
    console.log(`${key}: ${obj[key]}`); // a: 1, b: 2, c: 3
} 
```
```js
//for...of loop
const arr = [10, 20, 30];
for (let value of arr) {
    console.log(value); // 10, 20, 30
}
```
---

## 10. ES6+ Features
- Arrow functions - shorter syntax, lexically binds `this`.
```js
const add = (a, b) => a + b;
console.log(add(2, 3)); // 5
```
- Template literals - multi-line strings, embedded expressions using backticks ``.
```js
const name = "Alice";
const greeting = `Hello, ${name}! Welcome to ES6.`;
console.log(greeting); // Hello, Alice! Welcome to ES6.
```

- Destructuring - extract values from arrays/objects into variables.
```js   
//array destructuring
const [x, y] = [1, 2];
console.log(x, y); // 1 2

//object destructuring
const {name, age} = {name: "Bob", age: 25};
console.log(name, age); // Bob 25
```

- Spread/rest operator - expand/collapse arrays/objects.
```js
//spread operator
const arr1 = [1, 2];
const arr2 = [...arr1, 3, 4];
console.log(arr2); // [1, 2, 3, 4]

//rest operator
function sum(...args) {
    return args.reduce((acc, val) => acc + val, 0);
}
console.log(sum(1, 2, 3)); // 6
```
- Default parameters - set default values for function parameters.
```js 
function greet(name = "Guest") {
    console.log(`Hello, ${name}!`);
}

greet(); // Hello, Guest!
greet("Alice"); // Hello, Alice!
```
- Modules - `import` and `export` for modular code.
```js
//module.js
export const pi = 3.14;
export function area(radius) {
    return pi * radius * radius;
}
```
```js
//main.js
import { pi, area } from './module.js';
console.log(pi); // 3.14
console.log(area(5)); // 78.5
```
---