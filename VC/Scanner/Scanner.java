/*+*
 ***
 *+*	Scanner.java                        
 ***
 *escape character in string
 *escape character outside the string(maybe in spaceskip) see Q&A 9
 *+*/

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner { 

  private SourceFile sourceFile;
  private boolean debug;

  private ErrorReporter errorReporter;
  private StringBuffer currentSpelling;
  private char currentChar;
  private SourcePosition sourcePos;

  private int lineCounter;
  private int charStartCounter, charFinishCounter;
  
// =========================================================

  public Scanner(SourceFile source, ErrorReporter reporter) {
    sourceFile = source;
    errorReporter = reporter;
    currentChar = sourceFile.getNextChar();
    debug = false;

    // initialize counters for line and column numbers here
    
    lineCounter=1;
    charStartCounter=0;
    charFinishCounter=0;
  }

  public void enableDebugging() {
    debug = true;
  }

  // accept gets the next character from the source program.

  private void accept() {
	//save the current char in the currentSpellingBuffer
	currentSpelling.append(currentChar);
    currentChar = sourceFile.getNextChar();


  // you may also increment your line and column counters here
    charFinishCounter++;
    
  }

  // inspectChar returns the n-th character after currentChar
  // in the input stream. 
  //
  // If there are fewer than nthChar characters between currentChar 
  // and the end of file marker, SourceFile.eof is returned.
  
  private char inspectChar(int nthChar) {
    return sourceFile.inspectChar(nthChar);
  }

  private int nextToken() {
  // Tokens: separators, operators, literals, identifiers and keyworods
       
    switch (currentChar) {
    
    
    
    // separators 
    case '(':
	accept();
	return Token.LPAREN;
	
    case ')':
    accept();
    return Token.RPAREN;
    
    case '{':
    accept();
    return Token.LCURLY;
        
    case '}':
    accept();
    return Token.RCURLY;
    
    case '[':
    accept();
    return Token.LBRACKET;
            
    case ']':
    accept();
    return Token.RBRACKET;
        
    case ',':
    accept();
    return Token.COMMA;
        
    case ';':
    accept();
    return Token.SEMICOLON;
      
    // operators
    case '+':
    accept();
    return Token.PLUS;
    
    case '-':
    accept();
    return Token.MINUS;
    
    case '*':
    accept();
    return Token.MULT;
    
    case '/':
    accept();
    return Token.DIV;
    
    case '!':
    accept();
    if (currentChar == '=') {
    	accept();
    	return Token.NOTEQ;
    }else {
    	return Token.NOT;
    }
    
    case '=':
    accept();
    if (currentChar == '=') {
    	accept();
    	return Token.EQEQ;
    }else {
    	return Token.EQ;
    }
    
    case '<':
    accept();
    if (currentChar == '=') {
        accept();
        return Token.LTEQ;
    }else {
        return Token.LT;
    }    
    
    case '>':
    accept();
    if (currentChar == '=') {
        accept();
        return Token.GTEQ;
    }else {
        return Token.GT;
    }   
    
    case '&':	
    accept();
    if (currentChar == '&') {
        accept();
        return Token.ANDAND;
    } else {
        return Token.ERROR;
    }
    
    case '|':	
    accept();
    if (currentChar == '|') {
      	accept();
      	return Token.OROR;
    } else {
      	return Token.ERROR;
    }

    case '.':
    accept();
    if (currentChar <='0'|currentChar >='9') {
    	return Token.ERROR;
    }
    while ((currentChar >= '0' && currentChar <= '9') 
			| currentChar == 'e' | currentChar =='E') {
    	if ((currentChar == 'e' | currentChar == 'E')&&
    		(inspectChar(1) >= '0' && inspectChar(1) <= '9')) {
    		accept();
        	while (currentChar >= '0' && currentChar <= '9') {
        		accept();
        	}
        	return Token.FLOATLITERAL;
    	}else if ((currentChar == 'e' | currentChar == 'E')&&
    		(inspectChar(1) == '+' | inspectChar(1) == '-')&&
    		(inspectChar(2) >= '0' && inspectChar(2) <= '9')) {
    		accept();
        	accept();
        	while (currentChar >= '0' && currentChar <= '9') {
        		accept();
        	}
        	return Token.FLOATLITERAL;
    	}else {
    		if (currentChar == 'e' | currentChar == 'E') {
    			return Token.FLOATLITERAL;
    		}
    		accept();
    	}
    }
    return Token.FLOATLITERAL;
   
      	
    //When the scanner moves to the end
    case SourceFile.eof:	
	currentSpelling.append(Token.spell(Token.EOF));
	return Token.EOF;
    
    //if the currentChar is not an operator nor separator
    default:
    	// if the first char is a letter, it is a identifier
    	if (currentChar == '_' || (currentChar >= 'a' && currentChar <= 'z') ||
    	   (currentChar >= 'A' && currentChar <= 'Z')) {
    		accept();
    		while (currentChar == '_' || (currentChar >= 'a' && currentChar <= 'z')
    				|| (currentChar >= 'A' && currentChar <= 'Z') || 
    				(currentChar >= '0' && currentChar <= '9')) {
    			accept();
    		}
    		//if the character sequences are reserved keywords, then returns keyword
    		//if not returns identifier
    		switch (currentSpelling.toString()) {
    		
    		case "boolean":
    		return Token.BOOLEAN;
    		
    		case "break":
        	return Token.BREAK;
    		
    		case "continue":
            return Token.CONTINUE;
            
    		case "else":
        	return Token.ELSE;
    		
    		case "float":
            return Token.FLOAT;
            
    		case "for":
            return Token.FOR;
        	
    		case "if":
            return Token.IF;
            
    		case "int":
            return Token.INT;
            
    		case "return":
            return Token.RETURN;
            
    		case "void":
            return Token.VOID;
            
    		case "while":
            return Token.WHILE;
            
            //if character sequences are boolean-literal
    		case "true":
    		return Token.BOOLEANLITERAL;
    		
    		case "false":
    		return Token.BOOLEANLITERAL;
            
            default:
            return Token.ID;
           
    		}
    	//string literal  
    	}else if (currentChar == '"') {
    		currentChar = sourceFile.getNextChar();
    		charFinishCounter ++;
    		while (currentChar != '"') {
    			// if is escape character
    			if (currentChar == '\\') {
    				switch (inspectChar(1)) {
    				case 'b':
    					currentSpelling.append('\b');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case 'f':
    					currentSpelling.append('\f');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case 'n':
    					currentSpelling.append('\n');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case 'r':
    					currentSpelling.append('\r');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case '\'':
    					currentSpelling.append('\'');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case '\"':
    					currentSpelling.append('\"');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case 't':
    					currentSpelling.append('\t');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    				case '\\':
    					currentSpelling.append('\\');
    					currentChar = sourceFile.getNextChar();
    					currentChar = sourceFile.getNextChar();
    					charFinishCounter += 2;
    					break;
    					
    				case '\n':
    					accept();
    					break;
    				case SourceFile.eof:
    					accept();
    					break;
    				// illegal escape character !!!!!!!
    				default:
    					SourcePosition error_escape = new SourcePosition(lineCounter, charStartCounter, charStartCounter);
    					errorReporter.reportError("%: illegal escape character", Character.toString(currentChar) + Character.toString(inspectChar(1)), error_escape);
    					accept();
    					
    					
    				}
    				
    			//unterminated string
    			}else if (currentChar == '\n' | currentChar == SourceFile.eof) {
    				break;
    			}else {
    				
    				accept();
    			}
    		}
    		if (currentChar == '\n' | currentChar == SourceFile.eof) {			
    			SourcePosition error_string = new SourcePosition(lineCounter, charStartCounter, charStartCounter);
    			errorReporter.reportError("%: unterminated string", currentSpelling.toString(), error_string);
				return Token.STRINGLITERAL;
    		}else {
    			currentChar = sourceFile.getNextChar();
        		charFinishCounter ++;
        		return Token.STRINGLITERAL;
    		}
    	// numerical literal 
    	}else if(currentChar >= '0' && currentChar <= '9') {
    		accept();
    		while ((currentChar >= '0' && currentChar <= '9') | currentChar == '.'
    				| currentChar == 'e' | currentChar =='E') {
    			// if there are '.', 'e', 'E' , then it is float-literal
    			if (currentChar == '.') {
    				accept();
    				while ((currentChar >= '0' && currentChar <= '9')) {
						accept();
					}
    				if ((currentChar == 'e' | currentChar == 'E') && 
        					(inspectChar(1) >= '0' && inspectChar(1) <= '9')) {
        				accept();
        				while ((currentChar >= '0' && currentChar <= '9')) {
    						accept();
    					}
        				return Token.FLOATLITERAL;
        			}else if ((currentChar == 'e' | currentChar == 'E') &&
        					(inspectChar(1) == '+' | inspectChar(1) == '-') &&
        					(inspectChar(2) >= '0' && inspectChar(2) <= '9')) {
        				accept();
        				accept();
        				while ((currentChar >= '0' && currentChar <= '9')) {
    						accept();
    					}
        				return Token.FLOATLITERAL;
        			}
    				return Token.FLOATLITERAL;
    
    			}else if ((currentChar == 'e' | currentChar == 'E') && 
    					(inspectChar(1) >= '0' && inspectChar(1) <= '9')) {
    				accept();
    				while ((currentChar >= '0' && currentChar <= '9')) {
						accept();
					}
    				return Token.FLOATLITERAL;
    			}else if ((currentChar == 'e' | currentChar == 'E') &&
    					(inspectChar(1) == '+' | inspectChar(1) == '-') &&
    					(inspectChar(2) >= '0' && inspectChar(2) <= '9')) {
    				accept();
    				accept();
    				while ((currentChar >= '0' && currentChar <= '9')) {
						accept();
					}
    				return Token.FLOATLITERAL;
    			}else {
    				if (currentChar =='e' | currentChar =='E') {
    					return Token.INTLITERAL;
    				}else {
    					accept();
    				}
    			}
    		}
    		return Token.INTLITERAL;
    	}
    	
	break;
    }
    //
    
    //if none of the case, than currentChar is an illegal character
    accept(); 
    return Token.ERROR;
  }
  
  //need to fix if there is tab?
  void skip() {
	  switch (currentChar) {
	  
	  //skip space
	  case ' ':
		  currentChar = sourceFile.getNextChar();
		  charFinishCounter++;
		  skip();
		  break;
	  //skip comment
	  case '/':
		  if (inspectChar(1) == '/') {		//single line comment
			  currentChar = sourceFile.getNextChar();
			  currentChar = sourceFile.getNextChar();
			  charFinishCounter+=2;
			  while (currentChar != '\n') {
				  if (currentChar == SourceFile.eof){
					  break;
				  }else {
					  currentChar = sourceFile.getNextChar();
					  charFinishCounter++;
				  }
			  }
			  if (currentChar == SourceFile.eof) {
				  break;
			  }else {
				  lineCounter++;
				  charFinishCounter=0;
				  currentChar = sourceFile.getNextChar();
				  skip();
			  }
			  
		  }else if (inspectChar(1)=='*') {	//multiple lines comment
			  currentChar = sourceFile.getNextChar();
			  currentChar = sourceFile.getNextChar();
			  int commentStart = charFinishCounter + 1;
			  charFinishCounter += 2;
			  int commentLine = lineCounter;
			  while (currentChar != '*' || inspectChar(1) != '/') {
				  if (currentChar == '\n') {
					  lineCounter++;
					  charFinishCounter=0;
					  currentChar = sourceFile.getNextChar();
				  }else if(currentChar == SourceFile.eof) {		//unterminated comment
					  SourcePosition error_comment = new SourcePosition(commentLine, commentStart, commentStart);
					  errorReporter.reportError(": unterminated comment", "error", error_comment);
					  break;
				  }else {
					  currentChar = sourceFile.getNextChar();
					  charFinishCounter++;
				  }
				  
				  
			  }
			  charFinishCounter+=2;
			  currentChar = sourceFile.getNextChar();
			  currentChar = sourceFile.getNextChar();
			  skip();
		  }
		  break;
	  //skip return
	  case '\n':
		  currentChar = sourceFile.getNextChar();
		  charFinishCounter=0;
		  lineCounter++;
		  skip();
		  break;
	  //skip tab
	  case '\t':
		  currentChar = sourceFile.getNextChar();
		  charFinishCounter = charFinishCounter + (8 - charFinishCounter % 8);
		  skip();
		  break;
	  }
	 
	  
  }
  
  public Token getToken() {
    Token tok;
    int kind;

    // skip white space and comments and return

   skip();
   
   currentSpelling = new StringBuffer("");

   // Get the token position from the position counters
   // if reader reach EOF then start=finish=1
   // else reset the charCounters to the beginning of the token
   if (currentChar == SourceFile.eof) {
	   charStartCounter=1;
	   charFinishCounter=1;
   }else {
	   charStartCounter = charFinishCounter + 1;
   }
   
   
   kind = nextToken();
   
   sourcePos = new SourcePosition(lineCounter, charStartCounter, charFinishCounter);

   tok = new Token(kind, currentSpelling.toString(), sourcePos);

   // * do not remove these three lines
   if (debug)
     System.out.println(tok);
   return tok;
   }

}
