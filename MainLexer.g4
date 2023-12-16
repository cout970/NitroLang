
lexer grammar MainLexer ;

// Reserved
RESERVED : 'self' | 'Self' | 'function' | 'var' | 'val' | 'module' | 'class' | 'type' | 'recv' | 'receiver' | 'trait'
| 'interface' | 'either' | 'ref_mut' | 'ref' | 'copy' | 'final' | 'local' | 'object' | 'package' | 'super' | 'throw'
| 'try' | 'typeof' | 'finally' | 'then' | 'annotation' | 'inline' | 'expect' | 'infix' | 'private' | 'public' | 'protected'
| 'suspend' | 'vararg' ;

RANGE_IN                        : '..=' ;
RANGE_EX                        : '..<' ;
LAMBDA_START                    : '@{' ;
SET_START                       : '%[' ;
MAP_START                       : '#[' ;
STRUCT_START                    : '@[' ;
STRING_INTERP                   : '${' ;
DOUBLE_COLON                    : '::' ;
ARROW                           : '->' ;
OROR                            : '||' | 'or' ;
ANDAND                          : '&&' | 'and' ;
XORXOR                          : '^^' | 'xor' ;
COMPARE                         : '<=>' ;
EQ                              : '==' ;
NEQ                             : '!=' ;
LEQ                             : '<=' ;
GEQ                             : '>=' ;
BANGBANG                        : '!!' ;
ADD_ASSIGN                      : '+=' ;
SUB_ASSIGN                      : '-=' ;
MUL_ASSIGN                      : '*=' ;
DIV_ASSIGN                      : '/=' ;
MOD_ASSIGN                      : '%=' ;
OROR_ASSIGN                     : '||=' ;
ANDAND_ASSIGN                   : '&&=' ;
QUESTION_MARK                   : '?' ;
UNDERSCORE                      : '_' ;
DOT                             : '.' ;
LPAREN                          : '(' ;
RPAREN                          : ')' ;
LBRACE                          : '{' ;
LBRACKET                        : '[' ;
RBRACKET                        : ']' ;
COMMA                           : ',' ;
COLON                           : ':' ;
ADD                             : '+' ;
SUB                             : '-' ;
MUL                             : '*' ;
DIV                             : '/' ;
XOR                             : '^' ;
DOLAR                           : '$' ;
MOD                             : '%' ;
AT                              : '@' ;
HASH                            : '#' ;
OR                              : '|' ;
AND                             : '&' ;
NOT                             : 'not' | '!' ;
ASSIGN                          : '=' ;
LTH                             : '<' ;
GTH                             : '>' ;
THIS                            : 'this' ;
THIS_TYPE                       : 'This' ;
FUN                             : 'fun' ;
LET                             : 'let';
MODULE                          : 'mod';
STRUCT                          : 'struct';
RETURN                          : 'ret'     | 'return' ;
SIZE_OF                         : 'size_of' | 'sizeOf' | 'sizeof' ;
OPTION                          : 'option' ;
INTERNAL                        : 'internal' ;
REC                             : 'rec' ;
TAG                             : 'tag' ;
DEFER                           : 'defer' ;
TYPE_ALIAS                      : 'type_alias'  | 'typeAlias'   | 'typealias' ;
ENUM                            : 'enum' ;
NOTHING                         : 'nothing' ;
WHEN                            : 'when' ;
MATCH                           : 'match' ;
ALIAS                           : 'alias' ;
IF                              : 'if' ;
ELSE                            : 'else' ;
FOR                             : 'for' ;
IN                              : 'in' ;
WHILE                           : 'while' ;
REPEAT                          : 'repeat' ;
LOOP                            : 'loop' ;
IS                              : 'is' ;
AS                              : 'as' ;
TRUE                            : 'true' ;
FALSE                           : 'false' ;
NULL                            : 'null' ;
INCLUDE                         : 'include' ;
BREAK                           : 'break' ;
CONTINUE                        : 'continue' ;
USE                             : 'use' ;
MUT                             : 'mut' ;

// Added ! to void conflicts with variables of the same name
JSON                            : 'json!' ;
TEST                            : 'test!' ;

BLOCK_START                     : '```' (LOWER_IDENTIFIER|UPPER_IDENTIFIER)? -> pushMode(BLOCK_MODE) ;
WHITE_SPACE                     : [ \t\f]+ -> skip ;
NL                              : ('\n' WHITE_SPACE*)+ | ';' ;
LINE_COMMENT                    : '//' ~[\n]* -> type(NL) ;
DOC_COMMENT                     : '/**' (BLOCK_COMMENT|DOC_COMMENT|.)*? '*/' -> skip ;
BLOCK_COMMENT                   : '/*' (BLOCK_COMMENT|DOC_COMMENT|.)*? '*/' -> skip ;

fragment DIGIT                  : [0-9] ;
fragment DIGITS                 : DIGIT+ (UNDERSCORE DIGIT+)* ;
fragment INT_DECIMAL_NUMBER     : [+-]? DIGITS ;
fragment INT_BINARY_NUMBER      : '0b' [0-1]+ (UNDERSCORE [0-1]+)* ;
fragment INT_OCTAL_NUMBER       : '0o' [0-7]+ (UNDERSCORE [0-7]+)* ;
fragment INT_HEX_NUMBER         : '0x' [0-9a-fA-F]+ (UNDERSCORE [0-9a-fA-F]+)* ;
fragment FLOAT_OPTION           : DIGITS | DIGITS '.' DIGITS | '.' DIGITS ;
INT_NUMBER                      : INT_DECIMAL_NUMBER | INT_BINARY_NUMBER | INT_OCTAL_NUMBER | INT_HEX_NUMBER ;
LONG_NUMBER                     : INT_NUMBER [lL] ;
FLOAT_NUMBER                    : [+-]? FLOAT_OPTION ([eE][+-]?DIGITS)?[fFdD]? ;
UPPER_IDENTIFIER                : [A-Z][a-zA-Z0-9_]* ;
LOWER_IDENTIFIER                : [a-z][a-zA-Z0-9_]* ;

// Simple no interpolation string
PLAIN_STRING                    : '"' (~["$\\]|[\\].)* '"' ;
// String interpolation section
STRING_START                    : '"'  -> pushMode(STRING_MODE) ;
// String interpolation section
STRING2_START                   : 'r#"'-> pushMode(STRING2_MODE) ;
// Especial case to end string interpolation
RBRACE                          : '}' {
  if(_modeStack.size() > 0) {
      popMode();
      if(_mode != DEFAULT_MODE) {
          setType(STRING_INTERP_END);
      }
  }
};

ERROR_CHARACTER                 : . ;

mode STRING_MODE;
STRING_ESCAPE                   : '\\' . ;
STRING_INTERP_START             : '$' '{' -> pushMode(DEFAULT_MODE) ;
STRING_INTERP_END               : '}';
STRING_VAR                      : '$' LOWER_IDENTIFIER ;
STRING_BLOB                     : ~["$\\]+ ;
STRING_END                      : '"' -> popMode ;

mode STRING2_MODE;
STRING2_NL                       : [\n] ;
STRING2_BLOB                     : .+? ;
STRING2_END                      : '"#' -> popMode ;

mode BLOCK_MODE;
BLOCK_END                       : '```' -> popMode ;
BLOCK_BLOB                      : ~[`]+ ;
BLOCK_OTHER                     : [`] ;
