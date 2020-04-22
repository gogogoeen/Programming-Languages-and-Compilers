/*
 * Parser.java            
 *
 
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;
import VC.ASTs.*;

public class Parser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;
  private SourcePosition previousTokenPosition;
  private SourcePosition dummyPos = new SourcePosition();

  public Parser (Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;

    previousTokenPosition = new SourcePosition();

    currentToken = scanner.getToken();
  }

// match checks to see f the current token matches tokenExpected.
// If so, fetches the next token.
// If not, reports a syntactic error.

  void match(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      previousTokenPosition = currentToken.position;
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  void accept() {
    previousTokenPosition = currentToken.position;
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw(new SyntaxError());
  }

// start records the position of the start of a phrase.
// This is defined to be the position of the first
// character of the first token of the phrase.

  void start(SourcePosition position) {
    position.lineStart = currentToken.position.lineStart;
    position.charStart = currentToken.position.charStart;
  }

// finish records the position of the end of a phrase.
// This is defined to be the position of the last
// character of the last token of the phrase.

  void finish(SourcePosition position) {
    position.lineFinish = previousTokenPosition.lineFinish;
    position.charFinish = previousTokenPosition.charFinish;
  }

  void copyStart(SourcePosition from, SourcePosition to) {
    to.lineStart = from.lineStart;
    to.charStart = from.charStart;
  }

// ========================== PROGRAMS ========================

  public Program parseProgram() {

    Program programAST = null;
    
    SourcePosition programPos = new SourcePosition();
    start(programPos);

    try {
      List dlAST = parseFuncVarDeclList();
      finish(programPos);
      programAST = new Program(dlAST, programPos); 
      if (currentToken.kind != Token.EOF) {
        syntacticError("\"%\" unknown type", currentToken.spelling);
      }
    }
    catch (SyntaxError s) { return null; }
    return programAST;
  }

// ========================== DECLARATIONS ========================

  // Declaration List for function declaration and  global variable declaration
  List parseFuncVarDeclList() throws SyntaxError {
    List dlAST = null;
    Decl dAST = null;

    SourcePosition fvPos = new SourcePosition();
    start(fvPos);
    if (currentToken.kind != Token.EOF) {
      int currentType = currentToken.kind;
      dAST = parseFuncVarDecl();
      //
      
      if (currentToken.kind == Token.COMMA) {
        accept();
        dlAST = parseInitDeclaratorList(currentType, true);
        finish(fvPos);
        dlAST = new DeclList(dAST, dlAST, fvPos);
      }else {
    	
        if (isType()) {
          dlAST = parseFuncVarDeclList();
          finish(fvPos);
          dlAST = new DeclList(dAST, dlAST, fvPos);
        }else if (dAST != null) {
            finish(fvPos);
            dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), fvPos);
        }
      }
     
    }
    if (dlAST == null) 
      dlAST = new EmptyDeclList(dummyPos);

    return dlAST;
  }

  Decl parseFuncVarDecl() throws SyntaxError {

    Decl fvAST = null; 
    
    SourcePosition funcPos = new SourcePosition();
    start(funcPos);

    
    Type tAST = parseType();
    finish(funcPos);				//record the position of type
    Ident iAST = parseIdent();
    if (currentToken.kind == Token.LPAREN) {		//if is function declaration
      List fplAST = parseParaList();
      Stmt cAST = parseCompoundStmt();
      finish(funcPos);
      fvAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
    }else {
      fvAST = parseInitDeclarator(tAST, iAST, funcPos, true);
      
    }
    
    return fvAST;
  }
  
  Decl parseInitDeclarator(Type t, Ident i, SourcePosition p, Boolean global) throws SyntaxError {
    Decl vAST = null;
    Expr eAST = null;
	Type tAST = parseDeclarator(t, i, p);
	if (currentToken.kind == Token.EQ) {
	  accept();
	  eAST = parseInitialiser();
	  if (currentToken.kind != Token.COMMA) {match(Token.SEMICOLON);}
	  finish(p);
	  if (global == true) {vAST = new GlobalVarDecl(tAST, i, eAST, p);}
	  else {vAST = new LocalVarDecl(tAST, i, eAST, p);}
	}else {
      if (currentToken.kind != Token.COMMA) {match(Token.SEMICOLON);}
	  finish(p);
	  if (global == true) {vAST = new GlobalVarDecl(tAST, i, new EmptyExpr(dummyPos), p);}
	  else {vAST = new LocalVarDecl(tAST, i, new EmptyExpr(dummyPos), p);}
	}
	return vAST;
  }
  
  Type parseDeclarator(Type t, Ident i, SourcePosition p) throws SyntaxError {
    Type tAST = null;
    Expr ieAST = null;
    SourcePosition tPos = new SourcePosition();
    copyStart(p, tPos);
    
    if (currentToken.kind == Token.LBRACKET) {
      accept();
	  if (currentToken.kind == Token.INTLITERAL) {	//array with number
	    SourcePosition iePos = new SourcePosition();
	    start(iePos);
	    IntLiteral ilAST = parseIntLiteral();
	    finish(iePos);
	    match(Token.RBRACKET);
	    ieAST = new IntExpr(ilAST, iePos);
      }else {										//array without number
		match(Token.RBRACKET);
		ieAST = new EmptyExpr(dummyPos);
	  }
	  finish(tPos);
	  tAST = new ArrayType(t, ieAST , tPos);
    }else {											//variable is not array type
      tAST = t;
    }
    return tAST;
  }
  
  Expr parseInitialiser() throws SyntaxError {
    Expr eAST = null;
    
    SourcePosition exprPos = new SourcePosition();
    start(exprPos);
    if (currentToken.kind == Token.LCURLY) {
      accept();
      List lAST = parseExprList();
      match(Token.RCURLY);
      finish(exprPos);
      eAST = new InitExpr(lAST, exprPos);
    }else {
      eAST = parseExpr();
    }
    return eAST;
  }
  
  List parseInitDeclaratorList(int vType, Boolean global) throws SyntaxError {
    List dlAST = null;
    
    SourcePosition mvPos = new SourcePosition();
    start(mvPos);
    
    Decl dAST = parseMultiInitDeclarator(vType, global);
    
    if (currentToken.kind == Token.COMMA) {
      accept();
      dlAST = parseInitDeclaratorList(vType, global);
      finish(mvPos);
      dlAST = new DeclList(dAST, dlAST, mvPos);
    }else if (isType()) {
      if (global == true) {
        dlAST = parseFuncVarDeclList();
        finish(mvPos);
        dlAST = new DeclList(dAST, dlAST, mvPos);
      }else {
    	dlAST = parseLocalVarDeclList();
        finish(mvPos);
        dlAST = new DeclList(dAST, dlAST, mvPos);
      }
    }else if(dAST != null) {
      finish(mvPos);
      dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), mvPos);
    }
    if (dlAST == null) {dlAST = new EmptyDeclList(dummyPos);}
    
    return dlAST;
  }
  
  Decl parseMultiInitDeclarator(int vType, boolean global) throws SyntaxError {
    Decl vAST = null;
    Type typeAST = null;
    Ident iAST = null;
    
    SourcePosition varPos = new SourcePosition();
    start(varPos);
    iAST = parseIdent();
    
    
    switch (vType) {
    case Token.VOID: typeAST = new VoidType(dummyPos); break;
    case Token.BOOLEAN: typeAST = new BooleanType(dummyPos); break;
    case Token.INT: typeAST = new IntType(dummyPos); break;
    case Token.FLOAT: typeAST = new FloatType(dummyPos); break;
    }
    
    vAST = parseInitDeclarator(typeAST, iAST, varPos, global);
    
    return vAST;
  }
  
//Declaration List for local variable declaration
  List parseLocalVarDeclList() throws SyntaxError {
    List dlAST = null;
    Decl dAST = null;
  
    SourcePosition fvPos = new SourcePosition();
    start(fvPos);
   
    if (isType()) {
      int currentType = currentToken.kind;
      dAST = parseLocalVarDecl();
   
      
      if (currentToken.kind == Token.COMMA) {
        accept();
        dlAST = parseInitDeclaratorList(currentType, false);
        finish(fvPos);
        dlAST = new DeclList(dAST, dlAST, fvPos);
      }else {
        if (isType()) {
          dlAST = parseLocalVarDeclList();
          finish(fvPos);
          dlAST = new DeclList(dAST, dlAST, fvPos);
        }else if (dAST != null) {
          finish(fvPos);
          dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), fvPos);
        }
      }
    }
    if (dlAST == null) 
      dlAST = new EmptyDeclList(dummyPos);

    return dlAST;
 }
 
 Decl parseLocalVarDecl() throws SyntaxError {
   Decl vAST = null;
   SourcePosition funcPos = new SourcePosition();
   start(funcPos);
   Type tAST = parseType();
   finish(funcPos);				//record the position of type
   Ident iAST = parseIdent();
   vAST = parseInitDeclarator(tAST, iAST, funcPos, false);
   
   return vAST;
   
 }
 
//  ======================== TYPES ==========================

  Type parseType() throws SyntaxError {
    Type typeAST = null;

    SourcePosition typePos = new SourcePosition();
    start(typePos);
    switch (currentToken.kind) {
    case Token.VOID:
      accept();
      finish(typePos);
      typeAST = new VoidType(typePos);
      break;
    case Token.BOOLEAN:
      accept();
      finish(typePos);
      typeAST = new BooleanType(typePos);
      break;
    case Token.INT:
      accept();
      finish(typePos);
      typeAST = new IntType(typePos);
      break;
    case Token.FLOAT:
      accept();
      finish(typePos);
      typeAST = new FloatType(typePos);
      break;	
    }
    return typeAST;
    }

// ======================= STATEMENTS ==============================

  Stmt parseCompoundStmt() throws SyntaxError {
    Stmt cAST = null; 

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    match(Token.LCURLY);

    
    List vlAST = parseLocalVarDeclList();
    
    List slAST = parseStmtList();
    match(Token.RCURLY);
    finish(stmtPos);

    
    if (vlAST instanceof EmptyDeclList && slAST instanceof EmptyStmtList) 
      cAST = new EmptyCompStmt(stmtPos);
    else
      cAST = new CompoundStmt(vlAST, slAST, stmtPos);
    
    return cAST;
  }


  List parseStmtList() throws SyntaxError {
    List slAST = null; 

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind != Token.RCURLY) {
      Stmt sAST = parseStmt();
      {
        if (currentToken.kind != Token.RCURLY) {
          slAST = parseStmtList();
          finish(stmtPos);
          slAST = new StmtList(sAST, slAST, stmtPos);
        } else {
          finish(stmtPos);
          slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
        }
      }
    }
    else
      slAST = new EmptyStmtList(dummyPos);
    
    return slAST;
  }

  Stmt parseStmt() throws SyntaxError {
    Stmt sAST = null;
    
    switch (currentToken.kind) {
    case Token.LCURLY: sAST = parseCompoundStmt(); break;
    case Token.IF: sAST = parseIfStmt(); break;
    case Token.FOR: sAST = parseForStmt(); break;
    case Token.WHILE: sAST = parseWhileStmt(); break;
    case Token.BREAK: sAST = parseBreakStmt(); break;
    case Token.CONTINUE: sAST = parseContinueStmt(); break;
    case Token.RETURN: sAST = parseReturnStmt(); break;
    default: sAST = parseExprStmt();
    }
    
    return sAST;
  }
  
  Stmt parseIfStmt() throws SyntaxError {
    Stmt sAST = null;
    Expr exprAST = null;
    Stmt s2AST = null;
    Stmt s3AST = null;
    
    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);
    
    match(Token.IF);
    match(Token.LPAREN);
    exprAST = parseExpr();
    match(Token.RPAREN);
    s2AST = parseStmt();
    if (currentToken.kind == Token.ELSE) {
      accept();
      s3AST = parseStmt();
      finish(stmtPos);
      sAST = new IfStmt(exprAST, s2AST, s3AST, stmtPos);
    }else {
      finish(stmtPos);
      sAST = new IfStmt(exprAST, s2AST, stmtPos);
    }
    
    return sAST;
  }
  
  
  Stmt parseForStmt() throws SyntaxError {
    Stmt sAST = null;
    Expr expr1AST = null;
    Expr expr2AST = null;
    Expr expr3AST = null;
    Stmt s2AST = null;
    
    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);
    
    match(Token.FOR);
    match(Token.LPAREN);
    if (ExprFirstSet()) {expr1AST = parseExpr();}
    else {expr1AST = new EmptyExpr(dummyPos);}
    match(Token.SEMICOLON);
    if (ExprFirstSet()) {expr2AST = parseExpr();}
    else {expr2AST = new EmptyExpr(dummyPos);}
    match(Token.SEMICOLON);
    if (ExprFirstSet()) {expr3AST = parseExpr();}
    else {expr3AST = new EmptyExpr(dummyPos);}
    match(Token.RPAREN);
    s2AST = parseStmt();
    finish(stmtPos);
    sAST = new ForStmt(expr1AST, expr2AST, expr3AST, s2AST, stmtPos);
    
    return sAST;
  }
  
  Stmt parseWhileStmt() throws SyntaxError {
    Stmt sAST = null;
   
    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);
    
    match(Token.WHILE);
    match(Token.LPAREN);
    Expr exprAST = parseExpr();
    match(Token.RPAREN);
    Stmt s2AST = parseStmt();
    finish(stmtPos);
    sAST = new WhileStmt(exprAST, s2AST, stmtPos);
   
    return sAST;
  }
  
  Stmt parseBreakStmt() throws SyntaxError {
    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);
    match(Token.BREAK);
    match(Token.SEMICOLON);
    finish(stmtPos);
    Stmt sAST = new BreakStmt(stmtPos);
    
    return sAST;
  }
  
  Stmt parseContinueStmt() throws SyntaxError {
    SourcePosition stmtPos = new SourcePosition();
	start(stmtPos);
	match(Token.CONTINUE);
	match(Token.SEMICOLON);
	finish(stmtPos);
	Stmt sAST = new ContinueStmt(stmtPos);

	return sAST;
  }
  
  Stmt parseReturnStmt() throws SyntaxError {
    Expr exprAST = null;
    
    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);
    
    match(Token.RETURN);
    if (ExprFirstSet()) {exprAST = parseExpr();}
    else {exprAST = new EmptyExpr(dummyPos);}
    match(Token.SEMICOLON);
    finish(stmtPos);
    
    Stmt sAST = new ReturnStmt(exprAST, stmtPos);
   
    return sAST;
  }
  
  
  Stmt parseExprStmt() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (ExprFirstSet()) {exprAST = parseExpr();}
    else {exprAST = new EmptyExpr(dummyPos);}
    match(Token.SEMICOLON);
    finish(stmtPos);
    
    Stmt sAST = new ExprStmt(exprAST, stmtPos);
    
    return sAST;
  }


// ======================= EXPRESSIONS ======================


  List parseExprList() throws SyntaxError {
    List elAST = null;
    
    SourcePosition exprPos = new SourcePosition();
    start(exprPos);
    Expr exprAST = parseExpr();
    if (currentToken.kind == Token.COMMA) {
      accept();
      elAST = parseExprList();
      finish(exprPos);
      elAST = new ExprList(exprAST, elAST, exprPos);
    }else {
      finish(exprPos);
      elAST = new ExprList(exprAST, new EmptyExprList(dummyPos), exprPos);
    }
    
    return elAST;
  }
  
  
  Expr parseExpr() throws SyntaxError {
    Expr exprAST = null;
    exprAST = parseAssignmentExpr();
    return exprAST;
  }
  
  Expr parseAssignmentExpr() throws SyntaxError {
    Expr exprAST = null;
    
    SourcePosition assignPos = new SourcePosition();
    start(assignPos);
    
    exprAST = parseCondOrExpr();
    
    if (currentToken.kind == Token.EQ) {
      accept();
      Expr expr2AST = parseAssignmentExpr();
      finish(assignPos);
      exprAST = new AssignExpr(exprAST, expr2AST, assignPos);
      
    }
    return exprAST;
  }
  
  Expr parseCondOrExpr() throws SyntaxError {
    Expr exprAST = null;
    
    SourcePosition condorPos = new SourcePosition();
    start(condorPos);
    
    exprAST = parseCondAndExpr();
    while (currentToken.kind == Token.OROR) {
      Operator opAST = acceptOperator();
      Expr expr2AST = parseCondAndExpr();
      finish(condorPos);
      exprAST = new BinaryExpr(exprAST, opAST, expr2AST, condorPos);
    }
    
    return exprAST;
  }
  
  Expr parseCondAndExpr() throws SyntaxError {
    Expr exprAST = null;
    
    SourcePosition condandPos = new SourcePosition();
    start(condandPos);
    
    exprAST = parseEqualityExpr();
    while (currentToken.kind == Token.ANDAND) {
      Operator opAST = acceptOperator();
      Expr expr2AST = parseEqualityExpr();
      finish(condandPos);
      exprAST = new BinaryExpr(exprAST, opAST, expr2AST, condandPos);
    }
    
    return exprAST;
  }
  
  Expr parseEqualityExpr() throws SyntaxError {
    Expr exprAST = null;
    
    SourcePosition equaPos = new SourcePosition();
    start(equaPos);
    
    exprAST = parseRelExpr();
    while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
      Operator opAST = acceptOperator();
      Expr expr2AST = parseRelExpr();
      finish(equaPos);
      exprAST = new BinaryExpr(exprAST, opAST, expr2AST, equaPos);
    }
    
    return exprAST;
  }
  
  Expr parseRelExpr() throws SyntaxError {
    Expr exprAST = null;
    
    SourcePosition relPos = new SourcePosition();
    start(relPos);
    
    exprAST = parseAdditiveExpr();
    while (currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ
     	   || currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ) {
      Operator opAST = acceptOperator();
      Expr expr2AST = parseAdditiveExpr();
      finish(relPos);
      exprAST = new BinaryExpr(exprAST, opAST, expr2AST, relPos);
    }
   
    return exprAST;
  }
  

  Expr parseAdditiveExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition addStartPos = new SourcePosition();
    start(addStartPos);

    exprAST = parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS
           || currentToken.kind == Token.MINUS) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseMultiplicativeExpr();

      SourcePosition addPos = new SourcePosition();
      copyStart(addStartPos, addPos);
      finish(addPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
    }
    return exprAST;
  }

  Expr parseMultiplicativeExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition multStartPos = new SourcePosition();
    start(multStartPos);

    exprAST = parseUnaryExpr();
    while (currentToken.kind == Token.MULT
           || currentToken.kind == Token.DIV) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseUnaryExpr();
      SourcePosition multPos = new SourcePosition();
      copyStart(multStartPos, multPos);
      finish(multPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
    }
    return exprAST;
  }

  Expr parseUnaryExpr() throws SyntaxError {

    Expr exprAST = null;
    Operator opAST = null;
    Expr e2AST = null;

    SourcePosition unaryPos = new SourcePosition();
    start(unaryPos);

    switch (currentToken.kind) {
      case Token.PLUS:
        opAST = acceptOperator();
        e2AST = parseUnaryExpr();
        finish(unaryPos);
        exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        break;
      case Token.MINUS:
        opAST = acceptOperator();
        e2AST = parseUnaryExpr();
        finish(unaryPos);
        exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        break;
      case Token.NOT:
    	opAST = acceptOperator();
        e2AST = parseUnaryExpr();
        finish(unaryPos);
        exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        break;
      default:
        exprAST = parsePrimaryExpr();
        break;
    }
    return exprAST;
  }

  Expr parsePrimaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition primPos = new SourcePosition();
    start(primPos);

    switch (currentToken.kind) {

      case Token.ID:
        SourcePosition simvarPos = new SourcePosition();
        start(simvarPos);
    	Ident iAST = parseIdent();
        if (currentToken.kind == Token.LPAREN) {
          List aplAST = parseArgList();
          finish(primPos);
          exprAST = new CallExpr(iAST, aplAST, primPos);
        }else if (currentToken.kind == Token.LBRACKET) {
          finish(simvarPos);
          Var simVST = new SimpleVar(iAST, simvarPos);
          accept();
          exprAST = parseExpr();
          match(Token.RBRACKET);
          finish(primPos);
          exprAST = new ArrayExpr(simVST, exprAST, primPos);
        }else {
          finish(primPos);
          Var simVAST = new SimpleVar(iAST, primPos);
          exprAST = new VarExpr(simVAST, primPos);
        }
        break;

      case Token.LPAREN:
        {
          accept();
          exprAST = parseExpr();
	      match(Token.RPAREN);
        }
        break;

      case Token.INTLITERAL:
        IntLiteral ilAST = parseIntLiteral();
        finish(primPos);
        exprAST = new IntExpr(ilAST, primPos);
        break;
      case Token.FLOATLITERAL:
        FloatLiteral flAST = parseFloatLiteral();
        finish(primPos);
        exprAST = new FloatExpr(flAST, primPos);
        break;
      case Token.BOOLEANLITERAL:
        BooleanLiteral blAST = parseBooleanLiteral();
        finish(primPos);
        exprAST = new BooleanExpr(blAST, primPos);
        break;
      case Token.STRINGLITERAL:
        StringLiteral slAST = parseStringLiteral();
        finish(primPos);
        exprAST = new StringExpr(slAST, primPos);
        break;
      default:
        syntacticError("illegal primary expression", currentToken.spelling);
        break;
    }
    return exprAST;
  }

// ========================== ID, OPERATOR and LITERALS ========================

  Ident parseIdent() throws SyntaxError {

    Ident I = null; 

    if (currentToken.kind == Token.ID) {
      previousTokenPosition = currentToken.position;
      String spelling = currentToken.spelling;
      I = new Ident(spelling, previousTokenPosition);
      currentToken = scanner.getToken();
    } else 
      syntacticError("identifier expected here", "");
    return I;
  }

// acceptOperator parses an operator, and constructs a leaf AST for it

  Operator acceptOperator() throws SyntaxError {
    Operator O = null;

    previousTokenPosition = currentToken.position;
    String spelling = currentToken.spelling;
    O = new Operator(spelling, previousTokenPosition);
    currentToken = scanner.getToken();
    return O;
  }


  IntLiteral parseIntLiteral() throws SyntaxError {
    IntLiteral IL = null;

    if (currentToken.kind == Token.INTLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      IL = new IntLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("integer literal expected here", "");
    return IL;
  }

  FloatLiteral parseFloatLiteral() throws SyntaxError {
    FloatLiteral FL = null;

    if (currentToken.kind == Token.FLOATLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      FL = new FloatLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("float literal expected here", "");
    return FL;
  }

  BooleanLiteral parseBooleanLiteral() throws SyntaxError {
    BooleanLiteral BL = null;

    if (currentToken.kind == Token.BOOLEANLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      BL = new BooleanLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("boolean literal expected here", "");
    return BL;
  }
  
  StringLiteral parseStringLiteral() throws SyntaxError {
    StringLiteral SL = null;

	if (currentToken.kind == Token.STRINGLITERAL) {
	  String spelling = currentToken.spelling;
	  accept();
	  SL = new StringLiteral(spelling, previousTokenPosition);
	} else 
	  syntacticError("string literal expected here", "");
	return SL;
  }
 
//======================= PARAMETERS =======================

  List parseParaList() throws SyntaxError {
    List plAST = null;

    SourcePosition plPos = new SourcePosition();
    start(plPos);

    match(Token.LPAREN);
    if (currentToken.kind == Token.RPAREN) {
      match(Token.RPAREN);
      finish(plPos);
      plAST = new EmptyParaList (plPos);
    }else {
      plAST = parseProperParaList();
      match(Token.RPAREN);
    }
    
    return plAST;
  }
  
  List parseProperParaList() throws SyntaxError {
    List pplAST = null;
    ParaDecl pAST = null;
    
    SourcePosition pPos = new SourcePosition();
    start(pPos);
    
    pAST = parseParaDecl();
    if (currentToken.kind == Token.COMMA) {
      accept();
      pplAST = parseProperParaList();
      finish(pPos);
      pplAST = new ParaList(pAST, pplAST, pPos);
    }else if (pAST != null) {
      finish(pPos);
      pplAST = new ParaList(pAST, new EmptyParaList(dummyPos), pPos);
    }
    
    return pplAST;
  }
  
  ParaDecl parseParaDecl() throws SyntaxError {
    ParaDecl pAST = null;
    
    SourcePosition pdPos = new SourcePosition();
    start(pdPos);
    
    Type tAST = parseType();
    finish(pdPos);
    Ident iAST = parseIdent();
    tAST = parseDeclarator(tAST, iAST, pdPos);
    finish(pdPos);
    pAST = new ParaDecl(tAST, iAST, pdPos);
    
    return pAST;
  }
  
  List parseArgList() throws SyntaxError {
    List alAST = null;
    
    SourcePosition alPos = new SourcePosition();
    start(alPos);
    
    match(Token.LPAREN);
    if (currentToken.kind == Token.RPAREN) {
      match(Token.RPAREN);
      finish(alPos);
      alAST = new EmptyArgList (alPos);
    }else {
      alAST = parseProperArgList();
      match(Token.RPAREN);
    } 
    
    return alAST;
  }
  
  List parseProperArgList() throws SyntaxError {
    List palAST = null;
    Arg aAST = null;
    
    SourcePosition aPos = new SourcePosition();
    start(aPos);
    
    aAST = parseArg();
    if (currentToken.kind == Token.COMMA) {
      accept();
      palAST = parseProperArgList();
      finish(aPos);
      palAST = new ArgList(aAST, palAST, aPos);
    }else if (aAST != null) {
      finish(aPos);
      palAST = new ArgList(aAST, new EmptyArgList(dummyPos), aPos);
    }
   
    return palAST;
  }
  
  Arg parseArg() throws SyntaxError {
    SourcePosition aPos = new SourcePosition();
    start(aPos);
	Expr exprAST = parseExpr();
	finish(aPos);
	Arg aAST = new Arg(exprAST, aPos);
    return aAST;
  }

//=======================function===========================
  boolean ExprFirstSet() {
    if (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS
	    || currentToken.kind == Token.NOT || currentToken.kind == Token.ID
	    || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.FLOATLITERAL
	    || currentToken.kind == Token.BOOLEANLITERAL || currentToken.kind == Token.STRINGLITERAL
	    || currentToken.kind == Token.LPAREN) {return true;}
	else return false;
  }
  
  boolean isType() {
    if (currentToken.kind ==Token.VOID || currentToken.kind == Token.BOOLEAN
    	|| currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT) { return true;}
    else return false;
  }
  
}





