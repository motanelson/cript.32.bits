import java.util.*;
import org.antlr.v4.runtime.tree.*;

public class CodeGenerator extends cBaseVisitor<String> {

    private StringBuilder text = new StringBuilder();
    private StringBuilder data = new StringBuilder();

    private Map<String, Integer> locals = new HashMap<>();
    private Map<String, String> stringTable = new HashMap<>();

    private int stackOffset = 0;
    private int labelCount = 0;
    private int stringCount = 0;

    private String newLabel() {
        return "L" + (labelCount++);
    }

    private String addString(String s) {
        if (stringTable.containsKey(s)) return stringTable.get(s);
        String label = "str" + (stringCount++);
        data.append(label).append(": .ascii \"")
            .append(s.replace("\n","\\n").replace("\"","\\\""))
            .append("\\0\"\n");
        stringTable.put(s, label);
        return label;
    }

    // ==========================
    // ENTRY
    // ==========================
    public String generate(ParseTree tree) {
        text.append(".intel_syntax noprefix\n");
        text.append(".global _start\n");
        text.append(".extern prints\n");
        text.append(".extern exitss\n\n");

        visit(tree);

        StringBuilder out = new StringBuilder();

        out.append(".section .data\n");
        out.append(data);

        out.append("\n.section .text\n");

        out.append("_start:\n");
        out.append("    call main\n");
        out.append("    push eax\n");
        out.append("    call exitss\n\n");

        out.append(text);

        return out.toString();
    }

    // ==========================
    // FUNCTIONS
    // ==========================
    @Override
    public String visitFunctionDefinition(cParser.FunctionDefinitionContext ctx) {
        String name = ctx.declarator().getText();
        String s="";
        locals.clear();
        stackOffset = 0;
        s=name.replace("(","");
        s=s.replace(")","");
        text.append(s).append(":\n");
        text.append("    push ebp\n");
        text.append("    mov ebp, esp\n");

        visit(ctx.compoundStatement());

        text.append("    mov esp, ebp\n");
        text.append("    pop ebp\n");
        text.append("    ret\n\n");

        return null;
    }

    // ==========================
    // BLOCK
    // ==========================
    @Override
    public String visitCompoundStatement(cParser.CompoundStatementContext ctx) {
        for (var item : ctx.blockItem()) {
            visit(item);
        }
        return null;
    }

    // ==========================
    // DECLARATION
    // ==========================
    @Override
    public String visitDeclaration(cParser.DeclarationContext ctx) {
        if (ctx.initDeclaratorList() == null) return null;

        for (var d : ctx.initDeclaratorList().initDeclarator()) {
            String name = d.declarator().getText();

            stackOffset += 4;
            locals.put(name, stackOffset);

            text.append("    sub esp, 4\n");

            if (d.initializer() != null) {
                visit(d.initializer());
                text.append("    mov [ebp-").append(stackOffset).append("], eax\n");
            }
        }
        return null;
    }

    // ==========================
    // RETURN
    // ==========================
    @Override
    public String visitJumpStatement(cParser.JumpStatementContext ctx) {
        if (ctx.getText().startsWith("return")) {
            if (ctx.expression() != null) {
                visit(ctx.expression());
            }
            text.append("    mov esp, ebp\n");
            text.append("    pop ebp\n");
            text.append("    ret\n");
        }
        return null;
    }

    // ==========================
    // IF
    // ==========================
    @Override
    public String visitSelectionStatement(cParser.SelectionStatementContext ctx) {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        visit(ctx.expression());

        text.append("    cmp eax, 0\n");
        text.append("    je ").append(elseLabel).append("\n");

        visit(ctx.statement(0));

        text.append("    jmp ").append(endLabel).append("\n");
        text.append(elseLabel).append(":\n");

        if (ctx.statement().size() > 1) {
            visit(ctx.statement(1));
        }

        text.append(endLabel).append(":\n");
        return null;
    }

    // ==========================
    // WHILE
    // ==========================
    @Override
    public String visitIterationStatement(cParser.IterationStatementContext ctx) {
        String start = newLabel();
        String end = newLabel();

        text.append(start).append(":\n");

        visit(ctx.expression(0));

        text.append("    cmp eax, 0\n");
        text.append("    je ").append(end).append("\n");

        visit(ctx.statement());

        text.append("    jmp ").append(start).append("\n");
        text.append(end).append(":\n");

        return null;
    }

    // ==========================
    // FUNCTION CALL
    // ==========================
    @Override
    public String visitPostfixExpression(cParser.PostfixExpressionContext ctx) {

        if (ctx.argumentExpressionList() != null) {
            String fname = ctx.primaryExpression().getText();

            var argLists = ctx.argumentExpressionList();

List<cParser.AssignmentExpressionContext> args = new ArrayList<>();

for (var list : argLists) {
    args.addAll(list.assignmentExpression());
}

            for (int i = args.size() - 1; i >= 0; i--) {
                visit(args.get(i));
                text.append("    push eax\n");
            }

            text.append("    call ").append(fname).append("\n");

            if (!args.isEmpty()) {
                text.append("    add esp, ").append(args.size() * 4).append("\n");
            }

            return null;
        }

        return visit(ctx.primaryExpression());
    }

    // ==========================
    // EXPRESSIONS
    // ==========================
    @Override
    public String visitPrimaryExpression(cParser.PrimaryExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            String name = ctx.Identifier().getText();

            if (locals.containsKey(name)) {
                text.append("    mov eax, [ebp-").append(locals.get(name)).append("]\n");
            }
            return null;
        }

        if (ctx.Constant() != null) {
            text.append("    mov eax, ").append(ctx.Constant().getText()).append("\n");
            return null;
        }

        if (ctx.StringLiteral() != null) {
            String label = addString(ctx.StringLiteral().getText().replace("\"",""));
            text.append("    mov eax, offset ").append(label).append("\n");
            return null;
        }

        return visitChildren(ctx);
    }

    @Override
    public String visitAdditiveExpression(cParser.AdditiveExpressionContext ctx) {
        visit(ctx.multiplicativeExpression(0));

        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            text.append("    push eax\n");

            visit(ctx.multiplicativeExpression(i));

            text.append("    mov ebx, eax\n");
            text.append("    pop eax\n");

            String op = ctx.getChild(2*i - 1).getText();

            if (op.equals("+")) text.append("    add eax, ebx\n");
            else text.append("    sub eax, ebx\n");
        }
        return null;
    }

    @Override
    public String visitMultiplicativeExpression(cParser.MultiplicativeExpressionContext ctx) {
        visit(ctx.castExpression(0));

        for (int i = 1; i < ctx.castExpression().size(); i++) {
            text.append("    push eax\n");

            visit(ctx.castExpression(i));

            text.append("    mov ebx, eax\n");
            text.append("    pop eax\n");

            String op = ctx.getChild(2*i - 1).getText();

            if (op.equals("*")) text.append("    imul eax, ebx\n");
            else if (op.equals("/")) {
                text.append("    cdq\n");
                text.append("    idiv ebx\n");
            }
        }
        return null;
    }

    // ==========================
    // ASSIGNMENT
    // ==========================
    @Override
    public String visitAssignmentExpression(cParser.AssignmentExpressionContext ctx) {

        if (ctx.assignmentOperator() != null) {
            String name = ctx.unaryExpression().getText();

            visit(ctx.assignmentExpression());

            if (locals.containsKey(name)) {
                text.append("    mov [ebp-").append(locals.get(name)).append("], eax\n");
            }

            return null;
        }

        return visit(ctx.conditionalExpression());
    }
}
