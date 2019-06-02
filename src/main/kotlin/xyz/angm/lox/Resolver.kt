package xyz.angm.lox

import java.util.*
import kotlin.collections.HashMap

enum class FunctionType {
    NONE, FUNCTION
}

class Resolver(private val interpreter: Interpreter) : Expression.Visitor<Unit>, Statement.Visitor<Unit> {

    private val scopes = Stack<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE;

    override fun visitBlockStatement(statement: Statement.Block) {
        beginScope()
        resolve(statement.statements)
        endScope()
    }

    override fun visitClassStatement(statement: Statement.Class) {
        declare(statement.name)
        define(statement.name)
    }

    override fun visitExpressionStatement(statement: Statement.Expression) = resolve(statement.expression)

    override fun visitIfStatement(statement: Statement.If) {
        resolve(statement.condition)
        resolve(statement.thenBranch)
        resolve(statement.elseBranch ?: return)
    }

    override fun visitFunctionStatement(statement: Statement.Function) {
        declare(statement.name)
        define(statement.name)
        resolveFunction(statement, FunctionType.FUNCTION)
    }

    override fun visitReturnStatement(statement: Statement.Return) = if (statement.value != null) resolve(statement.value) else Unit

    override fun visitVarStatement(statement: Statement.Var) {
        declare(statement.name)
        if (statement.initializer != null) resolve(statement.initializer)
        define(statement.name)
    }

    override fun visitWhileStatement(statement: Statement.While) {
        resolve(statement.condition)
        resolve(statement.body)
    }

    override fun visitBinaryExpression(expression: Expression.Binary) {
        resolve(expression.left)
        resolve(expression.right)
    }

    override fun visitCallExpression(expression: Expression.Call) {
        resolve(expression.callee)
        expression.arguments.forEach(::resolve)
    }

    override fun visitGroupingExpression(expression: Expression.Grouping) = resolve(expression.expression)

    override fun visitLiteralExpression(expression: Expression.Literal) = Unit

    override fun visitLogicalExpression(expression: Expression.Logical) {
        resolve(expression.left)
        resolve(expression.right)
    }

    override fun visitTernaryExpression(expression: Expression.Ternary) {
        resolve(expression.condition)
        resolve(expression.isTrue)
        resolve(expression.isFalse)
    }

    override fun visitUnaryExpression(expression: Expression.Unary) = resolve(expression.right)

    override fun visitVariableExpression(expression: Expression.Variable) {
        if (scopes.isNotEmpty() && scopes.peek()[expression.name.lexeme] == false)
            Lox.error(expression.name, "Cannot read local variable in its own initializer.")
        resolveLocal(expression, expression.name)
    }

    override fun visitAssignExpression(expression: Expression.Assign) {
        resolve(expression.value)
        resolveLocal(expression, expression.name)
    }

    fun resolve(statements: List<Statement>) = statements.forEach(::resolve)
    private fun resolve(statement: Statement) = statement.accept(this)
    private fun resolve(expression: Expression) = expression.accept(this)

    private fun beginScope() = scopes.push(HashMap())
    private fun endScope() = scopes.pop()

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        if (scopes.peek().containsKey(name.lexeme)) Lox.error(name, "Variable '${name.lexeme}' already declared in this scope!")
        scopes.peek()[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expression: Expression, name: Token) {
        if (scopes.isEmpty()) return // 0..0 operator below would cause a OOB exception
        for (i in scopes.size..0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expression, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Statement.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }
}