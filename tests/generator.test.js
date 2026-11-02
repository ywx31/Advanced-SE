import assert from 'node:assert/strict'
import test from 'node:test'
import { evaluateSimpleExpression, generateQuestions } from '../src/generator.js'

test('defaults can generate 300 unique questions', () => {
  const questions = generateQuestions(300)
  assert.equal(questions.length, 300)
  assert.equal(new Set(questions.map((item) => item.expression)).size, 300)
})

test('every expression has exactly three numbers and two operators', () => {
  const questions = generateQuestions(300)
  for (const question of questions) {
    assert.equal(question.numbers.length, 3)
    assert.equal(question.operators.length, 2)
    assert.equal((question.expression.match(/\d+/g) || []).length, 3)
    assert.equal((question.expression.match(/[+−×÷]/g) || []).length, 2)
  }
})

test('answers and intermediate values are legal integers from 0 to 100', () => {
  const questions = generateQuestions(300)
  for (const question of questions) {
    assert.ok(Number.isInteger(question.answer))
    assert.ok(question.answer >= 0 && question.answer <= 100)
    assert.equal(evaluateSimpleExpression(question.expression), question.answer)
  }
})

test('rejects invalid quantities', () => {
  assert.throws(() => generateQuestions(0), /1～300/)
  assert.throws(() => generateQuestions(301), /1～300/)
  assert.throws(() => generateQuestions(1.5), /1～300/)
})
