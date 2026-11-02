const MIN_RESULT = 0
const MAX_RESULT = 100
const NUMBER_MIN = 1
const NUMBER_MAX = 50
const OPERATORS = ['+', '−', '×', '÷']

let candidateCache = null

function canonicalStep(left, operator, right) {
  if (operator === '+' || operator === '×') {
    const parts = [left, right].sort()
    return `${operator}(${parts[0]},${parts[1]})`
  }
  return `${operator}(${left},${right})`
}

function applyOperation(left, operator, right) {
  if (operator === '+') return left + right
  if (operator === '−') return left - right
  if (operator === '×') return left * right
  if (operator === '÷') {
    if (right === 0 || left % right !== 0) return null
    return left / right
  }
  return null
}

function buildCandidates() {
  const candidates = []
  const keys = new Set()

  for (const firstOperator of OPERATORS) {
    for (const secondOperator of OPERATORS) {
      for (let first = NUMBER_MIN; first <= NUMBER_MAX; first += 1) {
        for (let second = NUMBER_MIN; second <= NUMBER_MAX; second += 1) {
          const intermediate = applyOperation(first, firstOperator, second)
          if (intermediate === null || intermediate < MIN_RESULT || intermediate > MAX_RESULT) continue

          for (let third = NUMBER_MIN; third <= NUMBER_MAX; third += 1) {
            const result = applyOperation(intermediate, secondOperator, third)
            if (result === null || result < MIN_RESULT || result > MAX_RESULT) continue

            const firstKey = canonicalStep(String(first), firstOperator, String(second))
            const key = canonicalStep(firstKey, secondOperator, String(third))
            if (keys.has(key)) continue
            keys.add(key)

            candidates.push({
              expression: `(${first} ${firstOperator} ${second}) ${secondOperator} ${third}`,
              answer: result,
              numbers: [first, second, third],
              operators: [firstOperator, secondOperator],
            })
          }
        }
      }
    }
  }

  return candidates
}

function shuffle(items, random) {
  const result = [...items]
  for (let index = result.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(random() * (index + 1))
    ;[result[index], result[swapIndex]] = [result[swapIndex], result[index]]
  }
  return result
}

export function generateQuestions(count, options = {}) {
  if (!Number.isInteger(count) || count < 1 || count > 300) {
    throw new Error('题目数量必须是 1～300 的整数')
  }

  candidateCache ??= buildCandidates()
  if (candidateCache.length < count) {
    throw new Error(`当前规则只能生成 ${candidateCache.length} 道不重复题目`)
  }

  const random = options.random ?? Math.random
  return shuffle(candidateCache, random)
    .slice(0, count)
    .map((question, index) => ({
      number: index + 1,
      ...question,
    }))
}

export function getCandidateCount() {
  candidateCache ??= buildCandidates()
  return candidateCache.length
}

export function evaluateSimpleExpression(expression) {
  const match = expression.match(/^\((\d+) ([+−×÷]) (\d+)\) ([+−×÷]) (\d+)$/)
  if (!match) throw new Error(`无法解析表达式：${expression}`)
  const [, firstText, firstOperator, secondText, secondOperator, thirdText] = match
  const first = Number(firstText)
  const second = Number(secondText)
  const third = Number(thirdText)
  const intermediate = applyOperation(first, firstOperator, second)
  if (intermediate === null) throw new Error('中间结果为分数或除数为 0')
  return applyOperation(intermediate, secondOperator, third)
}
