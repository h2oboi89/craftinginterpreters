//>= Evaluating Expressions 1
package com.craftinginterpreters.lox;

//>= Functions 1
import java.util.ArrayList;
//>= Classes 1
import java.util.HashMap;
//>= Statements and State 1
import java.util.List;
//>= Resolving and Binding 1
import java.util.Map;
//>= Statements and State 1

//>= Evaluating Expressions 1
// Tree-walk interpreter.
/*>= Evaluating Expressions 1 < Statements and State 1
class Interpreter implements Expr.Visitor<Object> {
*/
//>= Statements and State 1
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
//>= Evaluating Expressions 1
  private final ErrorReporter reporter;
/*>= Statements and State 1 < Functions 1

  private Environment environment = new Environment();
*/
//>= Functions 1

  final Environment globals = new Environment();
  private Environment environment = globals;
//>= Resolving and Binding 1

  private Map<Expr, Integer> locals;
//>= Evaluating Expressions 1

  Interpreter(ErrorReporter reporter) {
    this.reporter = reporter;

//>= Functions 1
    globals.define("clock", new NativeFunction(0, this::clock));
//>= Evaluating Expressions 1
  }

/*>= Evaluating Expressions 1 < Statements and State 1
  void interpret(Expr expression) {
    try {
      print(evaluate(expression));
    } catch (RuntimeError error) {
      reporter.runtimeError(error.token.line, error.getMessage());
    }
  }
*/
/*>= Statements and State 1 < Resolving and Binding 1
  void interpret(List<Stmt> statements) {
*/
//>= Resolving and Binding 1
  void interpret(List<Stmt> statements, Map<Expr, Integer> locals) {
    this.locals = locals;

//>= Statements and State 1
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      reporter.runtimeError(error.token.line, error.getMessage());
    }
  }
//>= Evaluating Expressions 1

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
//>= Statements and State 1

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }
//>= Statements and State 1

  void executeBody(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
//>= Statements and State 1

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBody(stmt.statements, environment.enterScope());
    return null;
  }
//>= Classes 1

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    environment.declare(stmt.name);

    Map<String, LoxFunction> methods = new HashMap<>();
//>= Inheritance 1
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.name,
            "Superclass must be a class.");
      }

      environment = environment.enterScope();
      environment.define("super", superclass);
    }

//>= Classes 1
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(method, environment,
          method.name.text.equals("init"));
        methods.put(method.name.text, function);
    }

/*>= Classes 1 < Inheritance 1
    LoxClass klass = new LoxClass(stmt.name.text, methods);
*/
//>= Inheritance 1
    LoxClass klass = new LoxClass(stmt.name.text,
        (LoxClass)superclass, methods);

    if (superclass != null) {
      environment = environment.enclosing;
    }

//>= Classes 1
    environment.assign(stmt.name, klass);
    return null;
  }
//>= Statements and State 1

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }
//>= Functions 1

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    environment.declare(stmt.name);
/*>= Functions 1 < Classes 1
    LoxFunction function = new LoxFunction(stmt, environment);
*/
//>= Classes 1
    LoxFunction function = new LoxFunction(stmt, environment, false);
//>= Functions 1
    environment.assign(stmt.name, function);
    return null;
  }
//>= Control Flow 1

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTrue(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }
//>= Statements and State 1

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

//>= Functions 1

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }
//>= Statements and State 1

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.text, value);
    return null;
  }
//>= Control Flow 1

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTrue(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }
//>= Statements and State 1

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

/*>= Statements and State 1 < Resolving and Binding 1
    environment.assign(expr.name, value);
*/
//>= Resolving and Binding 1
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
//>= Statements and State 1

    return value;
  }
//>= Evaluating Expressions 1

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);

      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;

      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;

      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;

      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;

      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }

        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }

        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");

      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;

      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
    }

    // Unreachable.
    return null;
  }
//>= Functions 1

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof Callable)) {
      // TODO: Change error message to not mention classes explicitly
      // since this shows up before classes are implemented.
      throw new RuntimeError(expr.paren,
          "Can only call functions and classes.");
    }

    Callable function = (Callable)callee;
    if (arguments.size() < function.requiredArguments()) {
      throw new RuntimeError(expr.paren, "Not enough arguments.");
    }

    return function.call(this, arguments);
  }
//>= Classes 1

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance) object).getProperty(expr.name);
    }

    throw new RuntimeError(expr.name,
        "Only instances have properties.");
  }
//>= Evaluating Expressions 1

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
//>= Control Flow 1

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR && isTrue(left)) {
      return left;
    }

    if (expr.operator.type == TokenType.AND && !isTrue(left)) {
      return left;
    }

    return evaluate(expr.right);
  }
//>= Classes 1

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object value = evaluate(expr.value);
    Object object = evaluate(expr.object);

    if (object instanceof LoxInstance) {
      ((LoxInstance)object).fields.put(expr.name.text, value);
      return value;
    }

    throw new RuntimeError(expr.name, "Only instances have fields.");
  }
//>= Inheritance 1

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass)environment.getAt(distance, "super");
    LoxInstance receiver = (LoxInstance)environment.getAt(distance, "this");

    LoxFunction method = superclass.findMethod(receiver, expr.method.text);
    if (method == null) {
      throw new RuntimeError(expr.method,
          "Undefined property '" + expr.method.text + "'.");
    }

    return method;
  }
//>= Classes 1

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }
//>= Evaluating Expressions 1

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTrue(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
    }

    // Unreachable.
    return null;
  }
//>= Statements and State 1

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
/*>= Statements and State 1 < Resolving and Binding 1
    return environment.get(expr.name);
*/
//>= Resolving and Binding 1
    return lookUpVariable(expr.name, expr);
//>= Statements and State 1
  }
//>= Resolving and Binding 1

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.text);
    } else {
      return globals.get(name);
    }
  }
//>= Evaluating Expressions 1

  private void checkNumberOperands(Token operator,
                                   Object left, Object right) {
    if (left instanceof Double && right instanceof Double) {
      return;
    }

    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private void checkNumberOperand(Token operator, Object left) {
    if (left instanceof Double) {
      return;
    }

    throw new RuntimeError(operator, "Operand must be a number.");
  }
//>= Evaluating Expressions 1

  private Object print(Object argument) {
    System.out.println(stringify(argument));
    return argument;
  }
//>= Functions 1

  private Object clock(List<Object> arguments) {
    return (double)System.currentTimeMillis() / 1000.0;
  }
//>= Evaluating Expressions 1

  private boolean isTrue(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    // nil is only equal to nil.
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }
//>= Evaluating Expressions 1

  private String stringify(Object object) {
    if (object == null) return "nil";

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
//>= Evaluating Expressions 1
}
