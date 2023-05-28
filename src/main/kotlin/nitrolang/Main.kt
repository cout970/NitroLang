package nitrolang

import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile

fun main() {
    println("Hello World!")

    val errors = ErrorCollector()
    val file = SourceFile.load("example.nl")
    val program = AstParser.parseFile(file, errors)
    println(errors.toString())
    println(program)
}