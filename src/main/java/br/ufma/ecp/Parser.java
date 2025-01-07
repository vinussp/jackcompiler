package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;
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
import static br.ufma.ecp.token.TokenType.IDENT;
import static br.ufma.ecp.token.TokenType.IF;
import static br.ufma.ecp.token.TokenType.INT;
import static br.ufma.ecp.token.TokenType.LBRACE;
import static br.ufma.ecp.token.TokenType.LBRACKET;
import static br.ufma.ecp.token.TokenType.LET;
import static br.ufma.ecp.token.TokenType.LPAREN;
import static br.ufma.ecp.token.TokenType.METHOD;
import static br.ufma.ecp.token.TokenType.MINUS;
import static br.ufma.ecp.token.TokenType.NOT;
import static br.ufma.ecp.token.TokenType.NULL;
import static br.ufma.ecp.token.TokenType.NUMBER;
import static br.ufma.ecp.token.TokenType.RBRACE;
import static br.ufma.ecp.token.TokenType.RBRACKET;
import static br.ufma.ecp.token.TokenType.RETURN;
import static br.ufma.ecp.token.TokenType.RPAREN;
import static br.ufma.ecp.token.TokenType.SEMICOLON;
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
    private int ifLabelNum;

    private VMWriter vmWriter = new VMWriter();

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

        if (!peekTokenIs(RPAREN)) {
            parseType();
            parseVarName();
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                parseType();
                parseVarName();
            }
        }

        printNonTerminal("/parameterList");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");
        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
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
                break;
            case STRING:
                expectPeek(STRING);
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(FALSE, NULL, TRUE);
                break;
            case THIS:
                expectPeek(THIS);
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
                parseTerm();
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
            expectPeek(peekToken.type);
            parseTerm();
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
        }
        expectPeek(SEMICOLON);
        printNonTerminal("/returnStatement");
    }

    void parseWhile() {
        printNonTerminal("whileStatement");
        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");
        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        if (peekTokenIs(ELSE)) {
            expectPeek(ELSE);
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
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
        expectPeek(VAR);
        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);
        }

        expectPeek(SEMICOLON);
        printNonTerminal("/varDec");
    }

    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(FIELD, STATIC);
        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);
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

    public String VMOutput() {
        return vmWriter.vmOutput();
}

}