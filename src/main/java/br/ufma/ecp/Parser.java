package br.ufma.ecp;

import br.ufma.ecp.SymbolTable.Kind;
import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;


public class Parser {

    private static class ParseError extends RuntimeException {}
 
     private Scanner scan;
     private Token currentToken;
     private Token peekToken;
     private StringBuilder xmlOutput = new StringBuilder();
     private String className;
     private SymbolTable symbolTable;
     private VMWriter vmWriter;
 
     public Parser(byte[] input) {
         scan = new Scanner(input);
         symbolTable = new SymbolTable();
         vmWriter = new VMWriter();
         nextToken();
     }
 
     private void nextToken() {
         currentToken = peekToken;
         peekToken = scan.nextToken();
     }
 
 
     public void parse () {
         
     }

     void parseParameterList() {
        printNonTerminal("parameterList");

        SymbolTable.Kind kind = Kind.ARG;

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            String type = currentToken.value();

            expectPeek(TokenType.IDENT);
            String name = currentToken.value();
            symbolTable.define(name, type, kind);

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                type = currentToken.value();

                expectPeek(TokenType.IDENT);
                name = currentToken.value();

                symbolTable.define(name, type, kind);
            }

        }

        printNonTerminal("/parameterList");
    }

     void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
          case NUMBER:
            expectPeek(TokenType.NUMBER);
            break;
          case STRING:
            expectPeek(TokenType.STRING);
            break;
          case FALSE:
          case NULL:
          case TRUE:
            expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
            break;
          case THIS:
            expectPeek(TokenType.THIS);
            break;
          case IDENT:
            expectPeek(TokenType.IDENT);
            break;
          default:
            throw error(peekToken, "term expected");
        }
    
        printNonTerminal("/term");
      }

      void parseExpression() {
        printNonTerminal("expression");
        parseTerm ();
        while (isOperator(peekToken.lexeme)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
      }

     void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        if (peekTokenIs(TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
        }

        expectPeek(TokenType.EQ);
        parseExpression();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");
     }

     void parseSubroutineBody(String functionName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");
        expectPeek(TokenType.LBRACE);
        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }
        var nlocals = symbolTable.varCount(Kind.VAR);

        vmWriter.writeFunction(functionName, nlocals);

        if (subroutineType == TokenType.CONSTRUCTOR) {
            vmWriter.writePush(VMWriter.Segment.POINTER, symbolTable.varCount(Kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        }

        if (subroutineType == TokenType.METHOD) {
            vmWriter.writePush(VMWriter.Segment.POINTER, 0);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        }
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
     }
 
     // funções auxiliares

     static public boolean isOperator(String op) {
        return op!= "" && "+-*/<>=~&|".contains(op);
    
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
             throw error(peekToken, "Expected "+type.name());
         }
     }
 
 
     private static void report(int line, String where,
         String message) {
             System.err.println(
             "[line " + line + "] Error" + where + ": " + message);
     }
 
 
     private ParseError error(Token token, String message) {
         if (token.type == TokenType.EOF) {
             report(token.line, " at end", message);
         } else {
             report(token.line, " at '" + token.lexeme + "'", message);
         }
         return new ParseError();
     }
 
 
 }