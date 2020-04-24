/**
 * Checker.java   
 * Mar 25 15:57:55 AEST 2020
 **/

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Checker implements Visitor {

  private String errMesg[] = {
    "*0: main function is missing",                            
    "*1: return type of main is not int",                    

    // defined occurrences of identifiers
    // for global, local and parameters
    "*2: identifier redeclared",                             
    "*3: identifier declared void",                         
    "*4: identifier declared void[]",                      

    // applied occurrences of identifiers
    "*5: identifier undeclared",                          

    // assignments
    "*6: incompatible type for =",                       
    "*7: invalid lvalue in assignment",                 

     // types for expressions 
    "*8: incompatible type for return",                
    "*9: incompatible type for this binary operator", 
    "*10: incompatible type for this unary operator",

     // scalars
     "*11: attempt to use an array/function as a scalar", 

     // arrays
     "*12: attempt to use a scalar/function as an array",
     "*13: wrong type for element in array initialiser",
     "*14: invalid initialiser: array initialiser for scalar",   
     "*15: invalid initialiser: scalar initialiser for array",  
     "*16: excess elements in array initialiser",              
     "*17: array subscript is not an integer",                
     "*18: array size missing",                              

     // functions
     "*19: attempt to reference a scalar/array as a function",

     // conditional expressions in if, for and while
    "*20: if conditional is not boolean",                    
    "*21: for conditional is not boolean",                  
    "*22: while conditional is not boolean",               

    // break and continue
    "*23: break must be in a while/for",                  
    "*24: continue must be in a while/for",              

    // parameters 
    "*25: too many actual parameters",                  
    "*26: too few actual parameters",                  
    "*27: wrong type for actual parameter",           

    // reserved for errors that I may have missed (J. Xue)
    "*28: misc 1",
    "*29: misc 2",

    // the following two checks are optional 
    "*30: statement(s) not reached",     
    "*31: missing return statement",    
  };


  private SymbolTable idTable;
  private static SourcePosition dummyPos = new SourcePosition();
  private ErrorReporter reporter;
  private int nesting_depth;    //use for recording nesting depth
  
  
  // Checks whether the source program, represented by its AST, 
  // satisfies the language's scope rules and type rules.
  // Also decorates the AST as follows:
  //  (1) Each applied occurrence of an identifier is linked to
  //      the corresponding declaration of that identifier.
  //  (2) Each expression and variable is decorated by its type.

  public Checker (ErrorReporter reporter) {
    this.reporter = reporter;
    this.idTable = new SymbolTable ();
    establishStdEnvironment();
    nesting_depth = 1;
  }

  public void check(AST ast) {
    ast.visit(this, null);
  }


  // auxiliary methods

  private void declareVariable(Ident ident, Decl decl) {
    IdEntry entry = idTable.retrieveOneLevel(ident.spelling);

    if (entry == null) {
      ; // no problem
    } else
      reporter.reportError(errMesg[2] + ": %", ident.spelling, ident.position);
    idTable.insert(ident.spelling, decl);
  }
  
  public boolean isNumber(Type t) {
	  return (t.isIntType() || t.isFloatType());
  }
  
  
  //Synthesised Expr type and resolve overload operator
  public Type TypeSynthesised(Type t1, String o, Type t2) {
	if(t1 == null) {return UnarySynthesised(o, t2);}
	else  {return BinarySynthesised(t1, o, t2);}
  }

  public Type UnarySynthesised(String o, Type t) {
    if (o.equals("+") || o.equals("-")) {
      if (isNumber(t)) return t;
      else return StdEnvironment.errorType;
    }else {
      if(t.isBooleanType()) return t;
      else return StdEnvironment.errorType;
    }
  }
  
  public Type BinarySynthesised(Type t1, String o, Type t2) {
	
	if (o.equals("=")) {						//assignment synthesised
      if(t1.assignable(t2) && !t2.isErrorType())  {return t1;}
      else  {return StdEnvironment.errorType;}
    }else if (o.equals("+") || o.equals("-") || o.equals("*") || o.equals("/")) {
      if (isNumber(t1) && isNumber(t2)) {
        if (t1.isIntType() && t2.isIntType()) return StdEnvironment.intType;
        else return StdEnvironment.floatType;
      }else return StdEnvironment.errorType;
    }else if (o.equals("<") || o.equals("<=") || o.equals(">") || o.equals(">=")) {
      if (isNumber(t1) && isNumber(t2)) {return StdEnvironment.booleanType;}
      else {return StdEnvironment.errorType;}
    }else if (o.equals("&&") || o.equals("||")) {
      if (t1.isBooleanType() && t2.isBooleanType()) {return t1;}
      else {return StdEnvironment.errorType;}
    }else {
      if ((t1.isBooleanType() && t2.isBooleanType()) || (isNumber(t1) && isNumber(t2))) 
    	return StdEnvironment.booleanType;
      else {return StdEnvironment.errorType;}
    }
  }
  
  
  public String resolveOverload(Type t1, String o, Type t2) {
    if (o.equals("&&") || o.equals("||") || o.equals("!"))  return ("i" + o);
    else if (o.equals("+") || o.equals("-") || o.equals("*") || o.equals("/")
    		 || o.equals("<") || o.equals("<=") || o.equals(">") || o.equals(">=")) {
      if (t1 instanceof FloatType || t2.isFloatType())  return ("f" + o);
      else   return ("i" + o);
    }else {
      if (t1 instanceof BooleanType || t2.isBooleanType())  return ("i" + o);
      else if (t1 instanceof FloatType || t2.isFloatType())  return ("f" + o);
      else return ("i" + o);
    }
  }
  
  
  public Expr TypeCoercion(Expr E) {
    Operator op = new Operator("i2f", dummyPos);
    UnaryExpr eAST = new UnaryExpr(op, E, dummyPos);
    eAST.type = StdEnvironment.floatType;
    return eAST;
  }
  
  // To check there is invalid initialiser or size parameter missing , true mean
  // there is error 
  public boolean InitialiserCheck(Type T, Ident I, Expr E) {
    if (!T.isArrayType()) {            //if it is a scalar declaration
      if (E instanceof InitExpr) {reporter.reportError(errMesg[14], "", E.position); return true;}
      else {return false;}
    }else {								// if it is a array declaratiion
      if (!(E instanceof InitExpr || E.isEmptyExpr())) {
        reporter.reportError(errMesg[15], "", E.position);  
        return true;
      }
      ArrayType AT = (ArrayType) T;
      if (AT.E.isEmptyExpr()) 		// empty size parameter, need to filled up 
    	if (E.isEmptyExpr()) {reporter.reportError(errMesg[18]+ ": %", I.spelling, I.position); return true;}
      return false;
    }
  }
  
  
  // Programs

  public Object visitProgram(Program ast, Object o) {
    ast.FL.visit(this, null);
    //To check main function exists
    
    Decl main_func = idTable.retrieve("main");
    if (main_func == null)
      reporter.reportError(errMesg[0], "", ast.position);
    else if (!main_func.T.isIntType())
      reporter.reportError(errMesg[1], "", ast.position);
    
    return null;
  }

  // Statements

  public Object visitCompoundStmt(CompoundStmt ast, Object o) {
    idTable.openScope();
    // o here is funcdecl
    // Go visit parameter first, since parameters are in the same scope of local var
    FuncDecl d = (FuncDecl) o;
    d.PL.visit(this, null);     //here maybe need to record the number of parameter
    ast.DL.visit(this, o);
    ast.SL.visit(this, o);
    idTable.closeScope();
    return null;
  }
  
  public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
	idTable.openScope();
	FuncDecl d = (FuncDecl) o;
	d.PL.visit(this, null);
    idTable.closeScope();
    return null;
  }

  public Object visitStmtList(StmtList ast, Object o) {
    ast.S.visit(this, o);
    if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList)
      reporter.reportError(errMesg[30], "", ast.SL.position);
    ast.SL.visit(this, o);
    return null;
  }

  public Object visitIfStmt(IfStmt ast, Object o) {
	Type T = (Type)ast.E.visit(this, o);
	if (!T.isBooleanType() )
	  reporter.reportError(errMesg[20] + " %", "(found: "+T.toString()+")", ast.E.position);
    ast.S1.visit(this,  o);
    ast.S2.visit(this,  o);
    return null;
  }
  
  public Object visitWhileStmt(WhileStmt ast, Object o) {
	Type T = (Type)ast.E.visit(this, o);
	if (!T.isBooleanType())
	  reporter.reportError(errMesg[22] + " %", "(found: "+T.toString()+")", ast.E.position);
    nesting_depth +=1;
	ast.S.visit(this, o);
	nesting_depth -=1;
    return null;
  }
  
  public Object visitForStmt(ForStmt ast, Object o) {
    ast.E1.visit(this, o);
    Type T2 = (Type)ast.E2.visit(this, o);
    if (!T2.isBooleanType()&& !(ast.E2 instanceof EmptyExpr))
  	  reporter.reportError(errMesg[21] + " %", "(found: "+T2.toString()+")", ast.E2.position);
    ast.E3.visit(this, o);
    nesting_depth +=1;
    ast.S.visit(this, o);
    nesting_depth -=1;
    return null;
  }
  
  public Object visitBreakStmt(BreakStmt ast, Object o) {
	if (nesting_depth <2)
	  reporter.reportError(errMesg[23], "", ast.position);
	return null;
  }
  
  public Object visitContinueStmt(ContinueStmt ast, Object o) {
	if (nesting_depth <2)
	  reporter.reportError(errMesg[24], "", ast.position);
    return null;
  }
  
  public Object visitReturnStmt(ReturnStmt ast, Object o) {
	FuncDecl d = (FuncDecl) o;
    Type t = (Type) ast.E.visit(this, d);
    
    
    //Type Coercion for return expr
    if (d.T.isFloatType() && t.isIntType()) {ast.E = TypeCoercion(ast.E);}
    
    if (!d.T.assignable(t) || t.isErrorType()) 
      reporter.reportError(errMesg[8], "", ast.position);
    
    return null;
  }
  public Object visitExprStmt(ExprStmt ast, Object o) {
    ast.E.visit(this, o);
    return null;
  }

  public Object visitEmptyStmt(EmptyStmt ast, Object o) {
    return null;
  }

  public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
    return null;
  }

  // Expressions

  // Returns the Type denoting the type of the expression. Does
  // not use the given object.
  
  public Object visitEmptyExprList(EmptyExprList ast, Object o) {
    
    return 0; 
  }

  public Object visitUnaryExpr(UnaryExpr ast, Object o) {
	ast.O.visit(this, o);
	Type T = (Type) ast.E.visit(this, o);
	
	// synthesised the type of the Unary Expr
    ast.type = TypeSynthesised(null, ast.O.spelling, T);
    // Report error the first time error type is synthesised
 	if (ast.type == StdEnvironment.errorType && T != StdEnvironment.errorType)
 		reporter.reportError(errMesg[10] + ": %", ast.O.spelling, ast.position);
 	ast.O.spelling = resolveOverload(null, ast.O.spelling, T);
    return ast.type;
  }
  
  public Object visitBinaryExpr(BinaryExpr ast, Object o) {
    Type T1 = (Type) ast.E1.visit(this, o);
    ast.O.visit(this, o);
    Type T2 = (Type) ast.E2.visit(this, o);
    
    // synthesised the type of the Binary Expr
	ast.type = TypeSynthesised(T1, ast.O.spelling, T2);
	// Type Coercion
	if ((T1.isFloatType() && T2.isIntType()) || (T2.isFloatType() 
	     && T1.isIntType())) {
	  if (T1.isIntType()) ast.E1 = TypeCoercion(ast.E1);
	  else ast.E2 = TypeCoercion(ast.E2);
	}
	// Report error the first time error type is synthesised
	if (ast.type == StdEnvironment.errorType && T1 != StdEnvironment.errorType
		&& T2 != StdEnvironment.errorType)
		reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
	ast.O.spelling = resolveOverload(T1, ast.O.spelling, T2);
	
	return ast.type;
  }
  
  public Object visitInitExpr(InitExpr ast, Object o) {
	int count = (int) ast.IL.visit(this, o);
    return count;
  }
  
  public Object visitExprList(ExprList ast, Object o) {		//only from InitExpr
    Type AT = (Type) o;
	Type ET = (Type) ast.E.visit(this, o);
	if (ET.isErrorType() || !(AT.equals(ET))) {
	  if (AT.isFloatType() && ET.isIntType()) {ast.E = TypeCoercion(ast.E);}
	  else {reporter.reportError(errMesg[13], "", ast.E.position);}
	}
    int count = (int) ast.EL.visit(this, o);
    return count + 1;
  }
  
  public Object visitArrayExpr(ArrayExpr ast, Object o) {
    ast.type = (Type) ast.V.visit(this, o);
    
    SimpleVar s = (SimpleVar) ast.V;
    Decl d = (Decl) s.I.visit(this, o);
   
    if (d != null && (d.isFuncDecl() || !d.T.isArrayType()))
      reporter.reportError(errMesg[12] + ": %", s.I.spelling, ast. position);
    //check the array subscript is integer
    Type E = (Type) ast.E.visit(this, o);
    if (!E.isIntType())
      reporter.reportError(errMesg[17], "", ast.position);
    
	return ast.type;	  
  }
  
  public Object visitCallExpr(CallExpr ast, Object o) {
	Decl d = (Decl) ast.I.visit(this, o);	//return pointer from the visitIdent
			
    if (d == null) { 
	  reporter.reportError(errMesg[5] + ": %", ast.I.spelling, ast.position);
	  ast.type = StdEnvironment.errorType;
	}else if (!d.isFuncDecl() ) {
	  reporter.reportError(errMesg[19] + ": %", ast.I.spelling, ast.I. position);
	  ast.type = d.T;
	}else {
	  ast.type = (Type) d.T.visit(this, o);
	  FuncDecl f = (FuncDecl) d;
	  ast.AL.visit(this, f.PL);
	}
      return ast.type;	
  }

  public Object visitAssignExpr(AssignExpr ast, Object o) {
	Type T1 = (Type) ast.E1.visit(this, o);
	
	// to check whether the left side of and assignment is a variable
	if (ast.E1 instanceof VarExpr) {
	  VarExpr E1 = (VarExpr) ast.E1;
	  SimpleVar v = (SimpleVar) E1.V;
	  Decl d = (Decl)v.I.visit(this, o);
	  if (d != null)
	    if (d.isFuncDecl()) {reporter.reportError(errMesg[7], "", ast.position);}
	}else if (ast.E1 instanceof ArrayExpr) {
	  ArrayExpr E1 = (ArrayExpr) ast.E1;
	  SimpleVar v = (SimpleVar) E1.V;
	  Decl d = (Decl)v.I.visit(this, o);
	  if (d.isFuncDecl() && d!=null) {reporter.reportError(errMesg[7], "", ast.position);}
	}else {reporter.reportError(errMesg[7], "", ast.position);}
	
	Type T2 = (Type) ast.E2.visit(this, o);
	
	// synthesised the type of the Assignment Expr
	ast.type = TypeSynthesised(T1,"=", T2);
	
	//Type Coercion
	if (T1.isFloatType() && T2.isIntType()) {ast.E2 = TypeCoercion(ast.E2);}
	// Report error the first time error type is synthesised
	if (ast.type == StdEnvironment.errorType && T1 != StdEnvironment.errorType
		&& T2 != StdEnvironment.errorType)
	  reporter.reportError(errMesg[6], "=", ast.position);
	
    return ast.type;
  }

  public Object visitEmptyExpr(EmptyExpr ast, Object o) {
    ast.type = StdEnvironment.errorType;
    return ast.type;
  }

  public Object visitBooleanExpr(BooleanExpr ast, Object o) {
    ast.type = StdEnvironment.booleanType;
    return ast.type;
  }

  public Object visitIntExpr(IntExpr ast, Object o) {
    ast.type = StdEnvironment.intType;
    return ast.type;
  }

  public Object visitFloatExpr(FloatExpr ast, Object o) {
    ast.type = StdEnvironment.floatType;
    return ast.type;
  }

  public Object visitStringExpr(StringExpr ast, Object o) {
    ast.type = StdEnvironment.stringType;
    return ast.type;
  }

  public Object visitVarExpr(VarExpr ast, Object o) {
    ast.type = (Type) ast.V.visit(this, null);	
    SimpleVar s = (SimpleVar) ast.V;
    Decl d = (Decl) s.I.visit(this, o);
    
    if (d != null && (d.isFuncDecl() || d.T.isArrayType())) {
      reporter.reportError(errMesg[11] + ": %", s.I.spelling, s.I. position);
      ast.type = d.T; 
    }
    return ast.type;
  }

  // Declarations

  // Always returns null. Does not use the given object.

  public Object visitFuncDecl(FuncDecl ast, Object o) {
	declareVariable(ast.I, ast); // Maybe need to be changed
    
    // Pass ast as the 2nd argument (as done below) so that the
    // formal parameters of the function an be extracted from ast when the
    // function body is later visited
    
    //first visit compound statement to openscope() and then visit paralist
    ast.S.visit(this, ast);
    return null;
  }

  public Object visitDeclList(DeclList ast, Object o) {
    ast.D.visit(this, null);
    ast.DL.visit(this, null);
    return null;
  }

  public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
    return null;
  }

  public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
    declareVariable(ast.I, ast);
    // fill the rest
    if (ast.T.isVoidType()) {
      reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
    } else if (ast.T.isArrayType()) {
    if (((ArrayType) ast.T).T.isVoidType())
      reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
    }
  
    //if there is no invalid initialiser or missing 
    if (!InitialiserCheck(ast.T, ast.I, ast.E)) {
      if (!(ast.T.isArrayType()) && !(ast.E.isEmptyExpr())) {  //is scalar and has initialiser
    	Type DT = (Type) ast.T.visit(this, o);
    	Type ET = (Type) ast.E.visit(this, o);
    	if (ET.isErrorType() || !(DT.equals(ET))) {
    	  if (DT.isFloatType() && ET.isIntType()) {ast.E = TypeCoercion(ast.E);}
    	  else {reporter.reportError(errMesg[6], "=", ast.position);}
    	}
      }else if (ast.T.isArrayType() && !(ast.E.isEmptyExpr())) {
        ArrayType DT = (ArrayType) ast.T;
        if(DT.E.isEmptyExpr()) {   //without size parameter
          int size = (int) ast.E.visit(this, DT.T);
          DT.E = new IntExpr(new IntLiteral(String.valueOf(size), dummyPos), dummyPos);
        }else {					    // check size parameter
          int count = (int) ast.E.visit(this, DT.T);  //send the type of array as 2nd object
          IntExpr size_para = (IntExpr) DT.E;
          if(Integer.valueOf(size_para.IL.spelling) < count)
            reporter.reportError(errMesg[16] + ": %", ast.I.spelling, ast.I.position);
        }
      }
      
    }
    
    return null;
    
  }

  public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
    declareVariable(ast.I, ast);
    // fill the rest
    if (ast.T.isVoidType()) {
      reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
    } else if (ast.T.isArrayType()) {
    if (((ArrayType) ast.T).T.isVoidType())
      reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
    }
    
  //if there is no invalid initialiser or missing 
    if (!InitialiserCheck(ast.T, ast.I, ast.E)) {
      if (!(ast.T.isArrayType()) && !(ast.E.isEmptyExpr())) {  //is scalar and has initialiser
    	Type DT = (Type) ast.T.visit(this, o);
    	Type ET = (Type) ast.E.visit(this, o);
    	if (ET.isErrorType() || !(DT.equals(ET))) {
    	  if (DT.isFloatType() && ET.isIntType()) {ast.E = TypeCoercion(ast.E);}
    	  else {reporter.reportError(errMesg[6], "=", ast.position);}
    	}
      }else if (ast.T.isArrayType() && !(ast.E.isEmptyExpr())) {
        ArrayType DT = (ArrayType) ast.T;
        if(DT.E.isEmptyExpr()) {   //without size parameter
          int size = (int) ast.E.visit(this, DT.T);
          DT.E = new IntExpr(new IntLiteral(String.valueOf(size), dummyPos), dummyPos);
        }else {					    // check size parameter
          int count = (int) ast.E.visit(this, DT.T);
          IntExpr size_para = (IntExpr) DT.E;
          if(Integer.valueOf(size_para.IL.spelling) < count)
            reporter.reportError(errMesg[16] + ": %", ast.I.spelling, ast.I.position);
        }
      }
    }
    return null;
  }

  // Parameters

 // Always returns null. Does not use the given object.

  public Object visitParaList(ParaList ast, Object o) {
    ast.P.visit(this, null);
    ast.PL.visit(this, null);
    return null;
  }

  public Object visitParaDecl(ParaDecl ast, Object o) {
     declareVariable(ast.I, ast);

    if (ast.T.isVoidType()) {
      reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
    } else if (ast.T.isArrayType()) {
     if (((ArrayType) ast.T).T.isVoidType())
        reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
    }
    return null;
  }

  public Object visitEmptyParaList(EmptyParaList ast, Object o) {
    return null;
  }

  // Arguments

  // Your visitor methods for arguments go here
  
  
  public Object visitEmptyArgList(EmptyArgList ast, Object o) {
    if (o instanceof ParaList)
      reporter.reportError(errMesg[26], "", ast.position);
	return null;  //can be changed
  }
  
  public Object visitArgList(ArgList ast, Object o) {
	if (o instanceof EmptyParaList)
	  reporter.reportError(errMesg[25], "", ast.position);
	else {
	  ParaList pl = (ParaList) o;
	  ast.A.visit(this, pl.P);	  // sending the parameter to argument to match
	  ast.AL.visit(this, pl.PL);  // sending the resting paralist to arglist
	}
    return null;
  }
  
  public Object visitArg(Arg ast, Object o) {
	ParaDecl p = (ParaDecl) o;
	if (ast.E instanceof VarExpr) { 	
      VarExpr E = (VarExpr) ast.E;
      SimpleVar V = (SimpleVar) E.V;
      Decl binding = idTable.retrieve(V.I.spelling);
     
      if (binding != null && binding.T.isArrayType()) {    //when the argument is the name of the array
        if (p.T.isArrayType()){								// when the parameter is array type
          Type PT = (Type) p.T.visit(this, o);
          Type AT = (Type) binding.T.visit(this, o);
          V.I.decl = binding;
          if (!PT.equals(AT)) {
            if (PT.isFloatType() && AT.isIntType()) { return null;}
            else {reporter.reportError(errMesg[27] + ": %", p.I.spelling, ast.position); return null;}
          }else {return null;}
        }else {reporter.reportError(errMesg[11] + ": %", p.I.spelling, ast.position); return null;} //when the parameter is scalar
      }
    }		// if the argument is not the name of any array, treat as usual
	//if (p.T.isArrayType()) {reporter.reportError(errMesg[12] , "", ast.position);return null;}
	Type t = (Type) ast.E.visit(this, o);
    if (t.isErrorType() || !p.T.assignable(t))  //error type argument or argument not assignable
      reporter.reportError(errMesg[27] + ": %", p.I.spelling, ast.position);
    else {
      if (p.T.isFloatType() && t.isIntType())	//Type Coercion
        ast.E = TypeCoercion(ast.E);
    }
    return null;
  }

  
  // Types 

  // Returns the type predefined in the standard environment. 

  public Object visitErrorType(ErrorType ast, Object o) {
    return StdEnvironment.errorType;
  }

  public Object visitBooleanType(BooleanType ast, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntType(IntType ast, Object o) {
    return StdEnvironment.intType;
  }

  public Object visitFloatType(FloatType ast, Object o) {
    return StdEnvironment.floatType;
  }

  public Object visitStringType(StringType ast, Object o) {
    return StdEnvironment.stringType;
  }

  public Object visitVoidType(VoidType ast, Object o) {
    return StdEnvironment.voidType;
  }
  
  public Object visitArrayType(ArrayType ast, Object o) {
    return ast.T.visit(this, o);
  }

  // Literals, Identifiers and Operators

  public Object visitIdent(Ident I, Object o) {
    Decl binding = idTable.retrieve(I.spelling);
    if (binding != null)
      I.decl = binding;
    return binding;				//return pointer
  }

  public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntLiteral(IntLiteral IL, Object o) {
    return StdEnvironment.intType;
  }

  public Object visitFloatLiteral(FloatLiteral IL, Object o) {
    return StdEnvironment.floatType;
  }

  public Object visitStringLiteral(StringLiteral IL, Object o) {
    return StdEnvironment.stringType;
  }

  public Object visitOperator(Operator O, Object o) {
    return null;
  }

  // Variables
  public Object visitSimpleVar(SimpleVar ast, Object o) {   //here we did'nt set var.type if not declared
	Decl d = (Decl) ast.I.visit(this, o);	//return pointer from the visitIdent
	if (d == null) { 
	  reporter.reportError(errMesg[5] + ": %", ast.I.spelling, ast.I.position);
	  return StdEnvironment.errorType; //
	}else {ast.type = (Type) d.T.visit(this, o);/*ast.type = d.T;*/ return ast.type;}
	
  }
  
  // Creates a small AST to represent the "declaration" of each built-in
  // function, and enters it in the symbol table.

  private FuncDecl declareStdFunc (Type resultType, String id, List pl) {
    FuncDecl binding;

    binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl, 
           new EmptyStmt(dummyPos), dummyPos);
    idTable.insert (id, binding);
    return binding;
  }

  // Creates small ASTs to represent "declarations" of all 
  // build-in functions.
  // Inserts these "declarations" into the symbol table.

  private final static Ident dummyI = new Ident("x", dummyPos);

  private void establishStdEnvironment () {

    // Define four primitive types
    // errorType is assigned to ill-typed expressions

    StdEnvironment.booleanType = new BooleanType(dummyPos);
    StdEnvironment.intType = new IntType(dummyPos);
    StdEnvironment.floatType = new FloatType(dummyPos);
    StdEnvironment.stringType = new StringType(dummyPos);
    StdEnvironment.voidType = new VoidType(dummyPos);
    StdEnvironment.errorType = new ErrorType(dummyPos);

    // enter into the declarations for built-in functions into the table

    StdEnvironment.getIntDecl = declareStdFunc( StdEnvironment.intType,
	"getInt", new EmptyParaList(dummyPos)); 
    StdEnvironment.putIntDecl = declareStdFunc( StdEnvironment.voidType,
	"putInt", new ParaList(
	new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putIntLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putIntLn", new ParaList(
	new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.getFloatDecl = declareStdFunc( StdEnvironment.floatType,
	"getFloat", new EmptyParaList(dummyPos)); 
    StdEnvironment.putFloatDecl = declareStdFunc( StdEnvironment.voidType,
	"putFloat", new ParaList(
	new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putFloatLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putFloatLn", new ParaList(
	new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putBoolDecl = declareStdFunc( StdEnvironment.voidType,
	"putBool", new ParaList(
	new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putBoolLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putBoolLn", new ParaList(
	new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 

    StdEnvironment.putStringLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putStringLn", new ParaList(
	new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 

    StdEnvironment.putStringDecl = declareStdFunc( StdEnvironment.voidType,
	"putString", new ParaList(
	new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 

    StdEnvironment.putLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putLn", new EmptyParaList(dummyPos));

  }
  
  


}
