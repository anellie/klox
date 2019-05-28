package xyz.angm.lox.tool

import java.io.PrintWriter
import java.util.Arrays.asList


fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        System.exit(1)
    }
    val outputDir = args[0]

    defineAst(
        outputDir, "Expression", asList(
            "Assign     : Token name, Expression value",
            "Binary     : Expression left, Token operator, Expression right",
            "Grouping   : Expression expression",
            "Literal    : Any? value",
            "Unary      : Token operator, Expression right",
            "Ternary    : Expression condition, Expression isTrue, Expression isFalse",
            "Variable   : Token name"
        )
    )

    defineAst(
        outputDir, "Statement", asList(
            "Expression : xyz.angm.lox.Expression expression",
            "Print      : xyz.angm.lox.Expression expression",
            "Var        : Token name, xyz.angm.lox.Expression? initializer"
        )
    )
}

private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

    writer.println("package xyz.angm.lox")
    writer.println()
    writer.println("/** This file is autogenerated using [xyz.angm.lox.tool.defineAst]. DO NOT EDIT DIRECTLY! */")
    writer.println("abstract class $baseName {")
    writer.println()

    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println()
    defineVisitor(writer, baseName, types)

    types.forEach { type ->
        val className = type.split(":")[0].trim()
        val fields = type.split(":")[1].trim()
        defineType(writer, baseName, className, fields)
    }

    writer.println("}")
    writer.close()
}

private fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    writer.println("   class $className(")

    val fields = fieldList.split(", ")
    fields.forEachIndexed { index, it ->
        val type = it.split(" ")[0]
        val name = it.split(" ")[1]
        if (index == fields.size - 1) writer.println("        val $name: $type")
        else writer.println("        val $name: $type,")
    }

    writer.println("    ): $baseName() {")
    writer.println("    override fun <R> accept(visitor: Visitor<R>) = visitor.visit$className$baseName(this)")
    writer.println("    }")
    writer.println()
}

private fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("    interface Visitor<R> {")
    types.forEach { type ->
        val typeName = type.split(":")[0].trim()
        writer.println("    fun visit$typeName$baseName(${baseName.toLowerCase()}: $typeName): R")
    }
    writer.println("    }")
}