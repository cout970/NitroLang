
lexer grammar MainLexer ;

RANGE_IN                        : '..=' ;
RANGE_EX                        : '..<' ;
LAMBDA_START                    : '#{' ;
SET_START                       : '%[' ;
MAP_START                       : '@[' ;
LIST_START                      : '#[' ;
STRUCT_START                    : '$[' ;
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
THIS                            : 'this'        | 'self' ;
THIS_TYPE                       : 'This'        | 'Self' ;
FUN                             : 'fun'         | 'function' ;
LET                             : 'let'         | 'var'         | 'val' ;
MODULE                          : 'mod'         | 'module' ;
STRUCT                          : 'struct'      | 'class' ;
RETURN                          : 'ret'         | 'return' ;
SIZE_OF                         : 'size_of'     | 'sizeOf' ;
OPTION                          : 'type'        | 'option';
REC                             : 'rec'         | 'recv'        | 'receiver' ;
TAG                             : 'interface'   | 'trait'       | 'tag' ;
NOTHING                         : 'nothing' ;
WHEN                            : 'when' ;
MATCH                           : 'match' ;
EITHER                          : 'either' ;
ALIAS                           : 'alias' ;
IF                              : 'if' ;
ELSE                            : 'else' ;
FOR                             : 'for' ;
IN                              : 'in' ;
NOT_IN                          : '!in' ;
WHILE                           : 'while' ;
REPEAT                          : 'repeat' ;
LOOP                            : 'loop' ;
IS                              : 'is' ;
NOT_IS                          : '!is' ;
AS                              : 'as' ;
TRUE                            : 'true' ;
FALSE                           : 'false' ;
NULL                            : 'null' ;
INCLUDE                         : 'include' ;
BREAK                           : 'break' ;
CONTINUE                        : 'continue' ;
JSON                            : 'json' ;
USE                             : 'use' ;
MUT                             : 'mut' ;
REF                             : 'ref' ;
REF_MUT                         : 'ref_mut' ;
COPY                            : 'copy' ;
BLOCK_START                     : '```' IDENTIFIER? -> pushMode(BLOCK_MODE) ;

WHITE_SPACE                     : [ \t\f]+ -> skip ;
NL                              : ('\n' WHITE_SPACE*)+ | ';' ;
LINE_COMMENT                    : '//' ~[\n]* -> type(NL) ;
DOC_COMMENT                     : '/**' (BLOCK_COMMENT|DOC_COMMENT|.)*? '*/' -> skip ;
BLOCK_COMMENT                   : '/*' (BLOCK_COMMENT|DOC_COMMENT|.)*? '*/' -> skip ;

fragment DIGIT                  : [0-9] ;
fragment INT_DECIMAL_NUMBER     : [+-]? DIGIT+ ;
fragment INT_BINARY_NUMBER      : '0b' [0-1]+ ;
fragment INT_OCTAL_NUMBER       : '0o' [0-7]+ ;
fragment INT_HEX_NUMBER         : '0x' [0-9a-fA-F]+ ;
fragment FLOAT_OPTION           : DIGIT+ | DIGIT+ '.' DIGIT+ | '.' DIGIT+ ;
INT_NUMBER                      : INT_DECIMAL_NUMBER | INT_BINARY_NUMBER | INT_OCTAL_NUMBER | INT_HEX_NUMBER ;
FLOAT_NUMBER                    : [+-]? FLOAT_OPTION ([eE][+-]?DIGIT+)?[fFdD]? ;
IDENTIFIER                      : [a-zA-Z][a-zA-Z0-9_]* ;

// Simple no interpolation string
PLAIN_STRING                    : '"' (~["$]|[\\]["]|[\\][$])* '"' ;
// String interpolation section
STRING_START                    : '"'  -> pushMode(STRING_MODE) ;
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
STRING_ESCAPE                   : '\\' '"' | '\\' '$' | '\\' '\\' ;
STRING_INTERP_START             : '$' '{' -> pushMode(DEFAULT_MODE) ;
STRING_INTERP_END               : '}';
STRING_VAR                      : '$' IDENTIFIER ;
STRING_BLOB                     : ~["$\\]+ ;
STRING_END                      : '"' -> popMode ;

mode BLOCK_MODE;
BLOCK_END                       : '```' -> popMode ;
BLOCK_BLOB                      : ~[`]+ ;
BLOCK_OTHER                     : [`] ;
