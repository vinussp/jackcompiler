package br.ufma.ecp;

import br.ufma.ecp.SymbolTable.Kind;
import br.ufma.ecp.VMWriter.Command;
import br.ufma.ecp.VMWriter.Segment;
import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;
import static br.ufma.ecp.token.TokenType.AND;
import static br.ufma.ecp.token.TokenType.ASTERISK;
import static br.ufma.ecp.token.TokenType.BOOLEAN;
import static br.ufma.ecp.token.TokenType.CHAR;
import static br.ufma.ecp.token.TokenType.CLASS;
import static br.ufma.ecp.token.TokenType.COMMA;
import static br.ufma.ecp.token.TokenType.CONSTRUCTOR;
import static br.ufma.ecp.token.TokenType.DO;
import static br.ufma.ecp.token.TokenType.DOT;
import static br.ufma.ecp.token.TokenType.ELSE;
import static br.ufma.ecp.token.TokenType.EQ;
import static br.ufma.ecp.token.TokenType.FALSE;
import static br.ufma.ecp.token.TokenType.FIELD;
import static br.ufma.ecp.token.TokenType.FUNCTION;
import static br.ufma.ecp.token.TokenType.GT;
import static br.ufma.ecp.token.TokenType.IDENT;
import static br.ufma.ecp.token.TokenType.IF;
import static br.ufma.ecp.token.TokenType.INT;
import static br.ufma.ecp.token.TokenType.LBRACE;
import static br.ufma.ecp.token.TokenType.LBRACKET;
import static br.ufma.ecp.token.TokenType.LET;
import static br.ufma.ecp.token.TokenType.LPAREN;
import static br.ufma.ecp.token.TokenType.LT;
import static br.ufma.ecp.token.TokenType.METHOD;
import static br.ufma.ecp.token.TokenType.MINUS;
import static br.ufma.ecp.token.TokenType.NOT;
import static br.ufma.ecp.token.TokenType.NULL;
import static br.ufma.ecp.token.TokenType.NUMBER;
import static br.ufma.ecp.token.TokenType.OR;
import static br.ufma.ecp.token.TokenType.PLUS;
import static br.ufma.ecp.token.TokenType.RBRACE;
import static br.ufma.ecp.token.TokenType.RBRACKET;
import static br.ufma.ecp.token.TokenType.RETURN;
import static br.ufma.ecp.token.TokenType.RPAREN;
import static br.ufma.ecp.token.TokenType.SEMICOLON;
import static br.ufma.ecp.token.TokenType.SLASH;
import static br.ufma.ecp.token.TokenType.STATIC;
import static br.ufma.ecp.token.TokenType.STRING;
import static br.ufma.ecp.token.TokenType.THIS;
import static br.ufma.ecp.token.TokenType.TRUE;
import static br.ufma.ecp.token.TokenType.VAR;
import static br.ufma.ecp.token.TokenType.VOID;
import static br.ufma.ecp.token.TokenType.WHILE;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();
    private String className;

    private VMWriter vmWriter = new VMWriter();
    private SymbolTable symTable = new SymbolTable();

    private int ifLabelNum = 0 ;
    private int whileLabelNum = 0;

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }
    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }
    public void parse () {
         parseClass();
     }

    public void parseClass() {
        printNonTerminal("class");
        expectPeek(CLASS);
        expectPeek(IDENT);
        expectPeek(LBRACE);
        while (peekTokenIs(STATIC) || peekTokenIs(FIELD)) {
            parseClassVarDec();
        }
        while (peekTokenIs(FUNCTION) || peekTokenIs(CONSTRUCTOR) || peekTokenIs(METHOD)) {
            parseSubroutineDec();
        }
        expectPeek(RBRACE);
        printNonTerminal("/class");
    }

    public void parseSubroutineCall () {
        if (peekTokenIs (LPAREN)) {
            expectPeek(LPAREN);
            parseExpression();
            expectPeek(RPAREN);
        } else {
            // pode ser um metodo de um outro objeto ou uma função
            expectPeek(DOT);
            expectPeek(IDENT);
            expectPeek(LPAREN);
            parseExpression();
            expectPeek(RPAREN);
        }
    }


    void parseParameterList() {
        printNonTerminal("parameterList");

        SymbolTable.Kind kind = Kind.ARG;

        if (!peekTokenIs(RPAREN)) {
            expectPeek(INT, CHAR, BOOLEAN, IDENT);
            String type = currentToken.lexeme;

            expectPeek(IDENT);
            String name = currentToken.lexeme;
            symTable.define(name, type, kind);
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(INT, CHAR, BOOLEAN, IDENT);
                type = currentToken.lexeme;
                expectPeek(IDENT);
                name = currentToken.lexeme;
                symTable.define(name, type, kind);
            }
        }

        printNonTerminal("/parameterList");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");
        ifLabelNum = 0;
        whileLabelNum = 0;
        symTable.startSubroutine();

        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
        var subroutineType = currentToken.type;
        if (subroutineType == METHOD) {
            symTable.define("this", className, Kind.ARG);
        }
        // 'int' | 'char' | 'boolean' | className
        expectPeek(VOID, INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);

        expectPeek(LPAREN);
        parseParameterList();
        expectPeek(RPAREN);
        parseSubroutineBody();

        printNonTerminal("/subroutineDec");
    }
    

    void parseSubroutineBody() {
        printNonTerminal("subroutineBody");
        expectPeek(LBRACE);

        while (peekTokenIs(VAR)) {
            parseVarDec();
        }

        parseStatements();
        expectPeek(RBRACE);

        printNonTerminal("/subroutineBody");
    }

    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case NUMBER:
                expectPeek(NUMBER);
                vmWriter.writePush(Segment.CONST, Integer.parseInt(currentToken.lexeme));
                break;
            case STRING:
                expectPeek(TokenType.STRING);
                var strValue = currentToken.lexeme;
                vmWriter.writePush(Segment.CONST, strValue.length());
                vmWriter.writeCall("String.new", 1);
                for (int i = 0; i < strValue.length(); i++) {
                    vmWriter.writePush(Segment.CONST, strValue.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(FALSE, NULL, TRUE);
                vmWriter.writePush(Segment.CONST, 0);
                if (currentToken.type == TRUE)
                    vmWriter.writeArithmetic(Command.NOT);
                break;
            case THIS:
                expectPeek(THIS);
                vmWriter.writePush(Segment.POINTER, 0);
                break;
            case IDENT:
                expectPeek(IDENT);
    
                while (peekTokenIs(DOT) || peekTokenIs(LPAREN)) {
                    if (peekTokenIs(DOT)) {        
                        expectPeek(DOT);         
                        expectPeek(IDENT);
                    }
                    if (peekTokenIs(LPAREN)) {    
                        expectPeek(LPAREN);    
                        parseExpressionList();      
                        expectPeek(RPAREN);        
                    }
                }
                break;
                case LPAREN:
                expectPeek(LPAREN);
                parseExpression();
                expectPeek(RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(MINUS, NOT);
                var op = currentToken.type;
                parseTerm();
                if (op == MINUS)
                    vmWriter.writeArithmetic(Command.NEG);
                else
                    vmWriter.writeArithmetic(Command.NOT);
    
                break;
            default:
                throw error(peekToken, "term expected");
        }
        printNonTerminal("/term");
    }
    
    public void parseExpressionList() {
        printNonTerminal("expressionList");
        if (!peekTokenIs(RPAREN))
        {
            parseExpression();
        }

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            parseExpression();
        }
        printNonTerminal("/expressionList");
    }

      void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.lexeme)) {
            var ope = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();
            compileOperators(ope);
        }
        printNonTerminal("/expression");
    }

    void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(LET);
        expectPeek(IDENT);

        if (peekTokenIs(LBRACKET)) {
            expectPeek(LBRACKET);
            parseExpression();
            expectPeek(RBRACKET);
        }

        expectPeek(EQ);
        parseExpression();
        expectPeek(SEMICOLON);
        printNonTerminal("/letStatement");
    }

    void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(DO);
    
        parseVarName(); 
        if (peekTokenIs(DOT)) {
            expectPeek(DOT);
            parseVarName();
        }
    
        expectPeek(LPAREN);
        parseExpressionList();
        expectPeek(RPAREN);
        expectPeek(SEMICOLON);
    
        printNonTerminal("/doStatement");
    }

    

    void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(RETURN);
        if (!peekTokenIs(SEMICOLON)) {
            parseExpression();
        } else {
            vmWriter.writePush(Segment.CONST, 0);
        }
        expectPeek(SEMICOLON);
        vmWriter.writeReturn();
        printNonTerminal("/returnStatement");
    }

    void parseWhile() {
        printNonTerminal("whileStatement");
        
        var labelTrue = "WHILE_EXP" + whileLabelNum;
        var labelFalse = "WHILE_END" + whileLabelNum;
        whileLabelNum++;

        vmWriter.writeLabel(labelTrue);

        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(labelFalse);

        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        vmWriter.writeGoto(labelTrue); 
        vmWriter.writeLabel(labelFalse);
        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");
        var labelTrue = "IF_TRUE" + ifLabelNum;
        var labelFalse = "IF_FALSE" + ifLabelNum;
        var labelEnd = "IF_END" + ifLabelNum;

        ifLabelNum++;
        
        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);

        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);

        if (peekTokenIs(ELSE)){
            vmWriter.writeGoto(labelEnd);
        }
        vmWriter.writeLabel(labelFalse);

        if (peekTokenIs(ELSE)) {
            expectPeek(ELSE);
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
            vmWriter.writeLabel(labelEnd);
        }
        printNonTerminal("/ifStatement");
    }

    void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case RETURN:
                parseReturn();
                break;
            case DO:
                parseDo();
                break;
            default:
                throw error(peekToken, "Expected a statement");
        }
    }

    void parseStatements() {
        printNonTerminal("statements");
        while (peekToken.type == WHILE ||
                peekToken.type == IF ||
                peekToken.type == LET ||
                peekToken.type == DO ||
                peekToken.type == RETURN) {
            parseStatement();
        }
        printNonTerminal("/statements");
    }

    void parseVarDec() {
        printNonTerminal("varDec");

        SymbolTable.Kind kind = Kind.VAR;

        expectPeek(VAR);
        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        String type = currentToken.lexeme;
        expectPeek(IDENT);
        String name = currentToken.lexeme;
        symTable.define(name, type, kind);
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);
            name = currentToken.lexeme;
            symTable.define(name, type, kind);
        }

        expectPeek(SEMICOLON);
        printNonTerminal("/varDec");
    }

    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(FIELD, STATIC);

        SymbolTable.Kind kind = Kind.STATIC;
        if (currentTokenIs(FIELD))
            kind = Kind.FIELD;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        String type = currentToken.lexeme;
        expectPeek(IDENT);
        String name = currentToken.lexeme;
        symTable.define(name, type, kind);
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);
            name = currentToken.lexeme;
            symTable.define(name, type, kind);
        }

        expectPeek(SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    void parseType() {
        if (peekTokenIs(INT) || peekTokenIs(CHAR) || peekTokenIs(BOOLEAN)) {
            nextToken();
            xmlOutput.append(String.format("<keyword> %s </keyword>\r\n", currentToken.lexeme));
        } else if (peekTokenIs(IDENT)) {
            parseVarName();
        } else {
            throw error(peekToken, "Expected a type");
        }
    }

    void parseVarName() {
        expectPeek(IDENT);
    }
    

    static public boolean isOperator(String op) {
        return op != "" && "+-*/<>=~&|".contains(op);
    }

    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }
        throw error(peekToken, "Expected a statement");
    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected " + type.name());
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
    }

    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    public void compileOperators(TokenType type) {

        if (type == ASTERISK) {
            vmWriter.writeCall("Math.multiply", 2);
        } else if (type == SLASH) {
            vmWriter.writeCall("Math.divide", 2);
        } else {
            vmWriter.writeArithmetic(typeOperator(type));
        }
    }

    private Command typeOperator(TokenType type) {
        if (type == PLUS)
            return Command.ADD;
        if (type == MINUS)
            return Command.SUB;
        if (type == LT)
            return Command.LT;
        if (type == GT)
            return Command.GT;
        if (type == EQ)
            return Command.EQ;
        if (type == AND)
            return Command.AND;
        if (type == OR)
            return Command.OR;
        return null;
    }

    public String VMOutput() {
        return vmWriter.vmOutput();
    }

}