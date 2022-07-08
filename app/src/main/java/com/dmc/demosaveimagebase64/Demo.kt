package com.dmc.demosaveimagebase64

typealias Predicate<T> = (T) -> Boolean

fun foo(p: Predicate<Int>) = p(42)

fun main() {
    val f: (Int) -> Boolean = { it > 0 }
    println(foo(f)) // prints "true"

    val p: Predicate<Int> = { it > 0 }
    println(listOf(1, -2).filter(p)) // prints "[1]"

    val items = listOf(1, 2, 3, 4, 5)
    // Lambdas are code blocks enclosed in curly braces.
    items.fold(0) { acc: Int, i: Int ->
        // When a lambda has parameters, they go first, followed by '->'
        print("acc = $acc, i = $i, ")
        val result = acc + i
        println("result = $result")
        // The last expression in a lambda is considered the return value:
        result
    }

    // Parameter types in a lambda are optional if they can be inferred:
    val joinedToString = items.fold("Elements:") { acc, i -> "$acc $i" }
    println(joinedToString)
    // Function references can also be used for higher-order function calls:
    println(items.fold(1, Int::times))

    val onClick: (a: Int, b: Int) -> String = { a, b -> "ahihi: ${a + b}" }
    println(onClick(1, 2))

    val sum: (Int, Int) -> Unit = { a, b -> }
    val minus = { x: Int, y: Int -> x - y }

    println(items.fold(1, minus))

    val plus = fun(value: Int): Int = 123
    println(plus(1))

    class IntTransformer: (Int) -> Int {
        override operator fun invoke(x: Int): Int = x
    }
    val intFunction: (Int) -> Int = IntTransformer()
    println(intFunction(308))

    val repeatFun: String.(Int) -> String = { times -> this.repeat(times) }
    val twoParameters: (String, Int) -> String = repeatFun // OK

    fun runTransformation(f: (String, Int) -> String): String {
        return f("hello", 3)
    }
    val result = runTransformation(repeatFun) // OK
    println(result)

    val stringPlus: (String, String) -> String = String::plus
    val intPlus: Int.(Int) -> Int = Int::plus

    println(stringPlus.invoke("<-", "->"))
    println(stringPlus("Hello, ", "world!"))

    println(intPlus.invoke(1, 1))
    println(intPlus(1, 2))
    println(2.intPlus(3)) // extension-like call





}
