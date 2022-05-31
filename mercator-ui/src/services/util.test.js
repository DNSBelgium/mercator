import { cleanup } from '@testing-library/react'; 
import '@testing-library/jest-dom';

import { checkObjectIsFalsy } from './Util';

afterEach(cleanup);

// When given bad data, checkObjectIsFalsy returns true as in "The object IS FALSY".
// When given usable data, checkObjectIsFalsy returns false as in "The object IS NOT falsy".
// The boolean 'false' is not checked due to redundancy.

// Execute with "npm run test".

test("Assert checkObjectIsFalsy acts correctly with falsy data", () => {
  expect(checkObjectIsFalsy([])).toBe(true);
  expect(checkObjectIsFalsy({})).toBe(true);
  expect(checkObjectIsFalsy(null)).toBe(true);
  expect(checkObjectIsFalsy(undefined)).toBe(true);
  expect(checkObjectIsFalsy("")).toBe(true);
});

test("Assert checkObjectIsFalsy acts correctly with truthy data", () => {
  const jsonObject = 
  { 
    "foo": [
      { "first": "one" },
      { "second": "two" }
    ],
    "bar": [
      { "first": 1 },
      { "second": 2 }
    ]
  }
  const foo = { foo: "I am a foo." };
  const bar = "I am a bar.";
  const someArray = [1, 2, 3]

  expect(checkObjectIsFalsy(jsonObject)).toBe(false);
  expect(checkObjectIsFalsy(foo)).toBe(false);
  expect(checkObjectIsFalsy(bar)).toBe(false);
  expect(checkObjectIsFalsy(someArray)).toBe(false);
});