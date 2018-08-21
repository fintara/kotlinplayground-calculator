package de.lostmekka.kotlinplayground.calculator

import com.tsovedenski.parser.*

class Calculator : ICalculator {
    override fun evaluate(formula: String): Double {
        val exprP = ExprBuilder().build()
        val result = parse(exprP, formula)

        return when (result) {
            is Error   -> throw when (result.message) {
                "end of input",
                "positive real number" -> ParseException()
                else -> EvaluateException(result.message)
            }
            is Success ->
                if (result.rest.isEmpty()) result.value
                else throw ParseException()
        }
    }

}

class ExprBuilder : ParserBuilder<Double> {
    private val baselessFloatP = buildParser {
        char('.').ev()
        val frac = ulong.ev()
        "0.$frac".toDouble()
    }

    private val valueP = buildParser {
        val minuses = many(char('-')).ev()
        val base = (baselessFloatP or unumber).ev().toDouble()
        lookahead(noneOf('.') or eof).ev()
        val sign = if (minuses.size and 1 == 1) -1 else 1
        sign * base
    }

    override fun build(): Parser<Double> {
        val table: OperatorTable<Double> = listOf(
            listOf(
                prefix("-") { -it }
            ),
            listOf(
                binary("*") { x, y -> x * y },
                binary("/", ::divOrThrow)
            ),
            listOf(
                binary("+") { x, y -> x + y },
                binary("-") { x, y -> x - y }
            )
        )

        return skipSpaces andR buildExpressionParser(table, noSpaces(valueP), parens = true)
    }

    private fun divOrThrow(x: Double, y: Double) = if (y == 0.0) throw EvaluateException("div by zero") else x / y

    private fun <T> binary(symbol: String, f: (T, T) -> T): Operator<T>
        = Infix(noSpaces(string(symbol)).flatMap { just(f) }, Assoc.Left)

    private fun <T> prefix(symbol: String, f: (T) -> T): Operator<T>
        = Prefix(noSpaces(string(symbol)).flatMap { just(f) })

    private fun <T> noSpaces(parser: Parser<T>): Parser<T> = skipSpaces andR parser andL skipSpaces
}
