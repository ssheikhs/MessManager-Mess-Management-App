module.exports = {
  env: {
    es6: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
  ],
  parserOptions: {
    ecmaVersion: 2020,
  },
  rules: {
    // allow modern JS formatting
    "indent": "off",
    "max-len": "off",
    "require-jsdoc": "off",
    "valid-jsdoc": "off",
    "object-curly-spacing": "off",
    "comma-dangle": "off",
    "no-multi-spaces": "off",
  },
};
