parser grammar MainParser;

// https://github.com/antlr/antlr4

options { tokenVocab = MainLexer; }

// Entry points
parseFile : NL* definition* EOF;
parseTypeUsage : NL* typeUsage EOF;
parseExpression : NL* expression EOF;
parseFunctionDefinition : NL* annotation* functionDefinition EOF;

// Identifiers
nameToken
    : IDENTIFIER ;

declaredNameToken
    : nameToken ;

string
    : PLAIN_STRING
    | STRING_START stringContents* STRING_END
    ;

stringContents
    : STRING_BLOB
    | STRING_ESCAPE
    | STRING_VAR
    | STRING_INTERP_START expression STRING_INTERP_END
    ;

// Definitions
definition
    : annotation* definitionChoice NL* ;

// @Extern $[lib: "core", name: "println"]
annotation
    : AT nameToken annotationArgs? NL* ;

annotationArgs
    : STRUCT_START NL* (annotationArgEntry (commaOrNl annotationArgEntry)* COMMA?)? NL* RBRACKET ;

annotationArgEntry
    : annotationArgKey COLON NL* constExpr ;

annotationArgKey
    : nameToken
    | PLAIN_STRING
    ;

definitionChoice
    : structDefinition
    | optionDefinition
    | functionDefinition
    | includeDefinition
    | useDefinition
    | aliasDefinition
    | moduleDefinition
    | constDefinition
    | tagDefinition
    ;

// E.g. include "core:optional.nl"
includeDefinition
    : INCLUDE PLAIN_STRING ;

// E.g. alias Int32 = Int
aliasDefinition
    : ALIAS declaredNameToken ASSIGN typeUsage;

useDefinition
    : USE modulePath (useDefinitionConst | useDefinitionType | useDefinitionFunction | useDefinitionExtension) ;

// E.g. use module::var
useDefinitionConst
    : declaredNameToken ;

// E.g. use module::<core::Type>
useDefinitionType
    : (LTH | LBRACE) NL* modulePath? declaredNameToken NL* (GTH | RBRACE) ;

// E.g. use module::plus()
useDefinitionFunction
    : declaredNameToken LPAREN RPAREN ;

// E.g. use module::Int.plus()
useDefinitionExtension
    : typeUsage DOT declaredNameToken LPAREN RPAREN ;

// E.g. mod core {}
moduleDefinition
    : MODULE modulePath? declaredNameToken NL* LBRACE NL* definition* RBRACE ;

// E.g. let pi: Float = 3.14
constDefinition
    : LET declaredNameToken COLON typeUsage ASSIGN NL* expression ;

// E.g. struct List<Int> { }
structDefinition
    : STRUCT declaredNameToken typeParamDef? NL* structBody;

// E.g. { a: Int, b: Int }
structBody
    : LBRACE NL* (structField (commaOrNl structField)* COMMA?)? NL* RBRACE ;

// E.g. value: Int,
structField
    : nameToken COLON typeUsage;

// E.g. type Optional<T> {}
optionDefinition
    : OPTION declaredNameToken typeParamDef? NL* LBRACE NL*
    (optionDefinitionItem (commaOrNl optionDefinitionItem)* COMMA?)? NL*
    RBRACE ;

// E.g. Some { value: T },
optionDefinitionItem
    : declaredNameToken structBody?;

tagDefinition
    : TAG declaredNameToken LBRACE NL* (tagDefinitionFunction (NL+ tagDefinitionFunction)*)? NL* RBRACE ;

tagDefinitionFunction
    : annotation* functionHeader;

// E.g. fun Int.sum(other: Int): Int {}
functionDefinition
    : functionHeader NL* functionBody;

functionHeader
    : FUN NL* functionReceiver? declaredNameToken NL* typeParamDef? NL*
    LPAREN NL* (functionParameter (commaOrNl functionParameter)* COMMA?)? NL* RPAREN NL* functionReturnType? ;

// E.g. Int.
functionReceiver
    : typeUsage DOT ;

// E.g. : Int
functionReturnType
    : COLON typeUsage ;

// E.g. count: Int,
functionParameter
    : nameToken NL* COLON NL* typeUsage;

// E.g. {}
// E.g. = 3.14
functionBody
    : statementBlock
    | ASSIGN NL* expression
    ;

// Statements
// E.g. {}
// E.g. { ret 0 }
statementBlock
    : LBRACE NL* statement (NL+ statement)* NL* RBRACE
    | LBRACE NL* RBRACE
    ;

statement
    : statementChoice ;

statementChoice
    : letStatement
    | ifStatement
    | forStatement
    | repeatStatement
    | whileStatement
    | loopStatement
    | whenExpr
    | expressionStatement
    | foreignBlockStatement
    ;

// E.g. let a: Int = 0
letStatement
    : LET nameToken (COLON typeUsage)? (ASSIGN NL* expression)? ;

// E.g. if true {} else {}
ifStatement
    : IF NL* expression NL* statementBlock (NL* ELSE NL* statementBlock)? ;

// E.g. for item in list {}
forStatement
    : FOR NL* nameToken NL* IN NL* expression NL* statementBlock ;

// E.g. repeat 5 {}
repeatStatement
    : REPEAT NL* expression NL* statementBlock ;

// E.g. while condition {}
whileStatement
    : WHILE NL* expression NL* statementBlock ;

// E.g. loop {}
loopStatement
    : LOOP NL* statementBlock ;

foreignBlockStatement
    : BLOCK_START foreignBlockStatementPart* BLOCK_END ;

foreignBlockStatementPart
    : BLOCK_BLOB
    | BLOCK_OTHER
    ;

// E.g. a = 0
// E.g. 1 + 2
expressionStatement
    : assignableExpression ASSIGN NL* expression
    | expression
    ;

// E.g. struct.field = value
// E.g. list[index] = value
// E.g. list[] = value
// E.g. core::var = value
assignableExpression
    : expression DOT nameToken
    | expression collectionIndexingSuffix
    | expression LBRACKET RBRACKET
    | modulePath? nameToken
    ;

// Expressions
binopShl
    : LTH LTH;
binopShr
    : GTH GTH ;
binopUshr
    : GTH GTH GTH;
binaryOperator
    : MUL | DIV | MOD | ADD | SUB | RANGE_IN | RANGE_EX | binopShl | binopShr | binopUshr | AND | XOR | OR | LTH | GTH | LEQ | GEQ | COMPARE | EQ | NEQ | ANDAND | OROR | XORXOR ;

expression
    : expressionComplex ;

// Ej. 1 + 2
// Ej. if x > 5 { 1 } else { 0 }
// Ej. ret 5
// Ej. not true
expressionComplex
    : expressionBinaryOp
    | ifExpr
    | returnExpr
    | notExpr
    ;

// E.g. 1.0 + math::PI * 4.0
expressionBinaryOp
    : expressionSimple (binaryOperator expressionSimple)* ;

// E.g. var as Int
// E.g. var is Int
// E.g. var !is Int
// E.g. var in %[1, 2, 3]
// E.g. var !in %[1, 2, 3]
expressionSimple
    : expressionWithSuffix AS typeUsage
    | expressionWithSuffix IS typeUsage
    | expressionWithSuffix NOT_IS typeUsage
    | expressionWithSuffix IN expressionWithSuffix
    | expressionWithSuffix NOT_IN expressionWithSuffix
    | expressionWithSuffix
    ;

// E.g. expr() #[]
// E.g. expr[index]
// E.g. expr.field
// E.g. expr
// E.g. true()
// E.g. expr.field()
// E.g. func()()
// E.g. break()
expressionWithSuffix
    : expressionWithSuffix collectionIndexingSuffix
    | expressionWithSuffix structFieldAccessSuffix
    | expressionWithSuffix DOT nameToken functionCallParams functionCallEnd?
    | expressionWithSuffix DOT nameToken functionCallEnd
    | expressionOrFunctionCall
    ;

expressionOrFunctionCall
    : modulePath? nameToken functionCallEnd
    | modulePath? nameToken functionCallParams functionCallEnd?
    | parenthesizedExpression functionCallParams functionCallEnd?
    | expressionLiteral functionCallParams functionCallEnd?
    | structInstanceExpr functionCallParams functionCallEnd?
    | sizeOfExpr functionCallParams functionCallEnd?
    | THIS functionCallParams functionCallEnd?
    | THIS functionCallEnd
    | expressionBase
    ;

collectionIndexingSuffix
    : LBRACKET NL* expression NL* RBRACKET ;

structFieldAccessSuffix
    : DOT nameToken ;

parenthesizedExpression
    : LPAREN NL* expression NL* RPAREN ;

expressionBase
    : parenthesizedExpression
    | nothingExpression
    | expressionLiteral
    | whenExpr
    | listExpr
    | mapExpr
    | setExpr
    | lambdaExpr
    | structInstanceExpr
    | sizeOfExpr
    | ptrOfExpr
    | memoryWriteExpr
    | memoryReadExpr
    | variableExpr
    | jsonExpr
    | THIS
    | BREAK
    | CONTINUE
    ;

// json {"key": ["val1", 1, true, null]}
jsonExpr
    : JSON jsonValue ;

// Constant value that can be evaluated at compile time
constExpr
    : nothingExpression
    | constExpressionLiteral
    ;

nothingExpression
    : NOTHING ;

expressionLiteral
    : INT_NUMBER
    | FLOAT_NUMBER
    | string
    | TRUE
    | FALSE
    | NULL
    ;

constExpressionLiteral
    : INT_NUMBER
    | FLOAT_NUMBER
    | PLAIN_STRING
    | TRUE
    | FALSE
    | NULL
    ;

whenExpr
    : WHEN NL* expression? NL* LBRACE NL* (whenEntry (commaOrNl whenEntry)* COMMA?)? NL* RBRACE ;

whenEntry
    : whenKey ARROW expression
    | whenKey ARROW statementBlock
    ;

whenKey
    : expression
    | ELSE
    ;

// #[item1, item2]
listExpr
    : LIST_START NL* (listEntry (commaOrNl listEntry)* COMMA?)? NL* RBRACKET ;

// item
listEntry
    : expression;

// @[key: value, "key": value, ("key" + 1): value]
mapExpr
    : MAP_START NL* (mapEntry (commaOrNl mapEntry)* COMMA?)? NL* RBRACKET ;

mapEntry
    : mapKey COLON NL* expression ;

mapKey
    : LPAREN NL* expression NL* RPAREN
    | nameToken
    | string
    ;

// %[1, 2, 3]
setExpr
    : SET_START NL* listEntry* RBRACKET ;

// #{ a -> a + 1 }
// #{}
lambdaExpr
    : LAMBDA_START lambdaDef? NL* statement (NL+ statement)* NL* RBRACE
    | LAMBDA_START lambdaDef? NL* RBRACE
    ;

// a, b, rec Int, ret Int ->
lambdaDef
    : lambdaReceiver COMMA lambdaParams COMMA lambdaReturn ARROW
    | lambdaReceiver COMMA lambdaParams                    ARROW
    | lambdaReceiver                    COMMA lambdaReturn ARROW
    | lambdaReceiver                                       ARROW
    |                      lambdaParams COMMA lambdaReturn ARROW
    |                      lambdaParams                    ARROW
    |                                         lambdaReturn ARROW
    ;

lambdaParams
    : lambdaArgument (COMMA lambdaArgument)* ;

lambdaReceiver
    : REC typeUsage;

lambdaReturn
    : RETURN typeUsage;

lambdaArgument
    : nameToken COLON typeUsage ;

// E.g. ret 1
returnExpr
    : RETURN expression ;

// E.g. size_of<Int>
sizeOfExpr
    : SIZE_OF LTH NL* typeUsage NL* GTH ;

// E.g. ptr_of<Int>
ptrOfExpr
    : PTR_OF LPAREN NL* expression NL* RPAREN ;

// E.g. memory_write<Int>(ptr, value)
memoryWriteExpr
    : MEMORY_WRITE LTH NL* typeUsage NL* GTH NL* LPAREN NL* expression commaOrNl expression NL* RPAREN ;

// E.g. memory_read<Int>(ptr)
memoryReadExpr
    : MEMORY_READ LTH NL* typeUsage NL* GTH NL* LPAREN NL* expression NL* RPAREN ;

// E.g. not true
notExpr
    : NOT expressionBase ;

ifExpr
    : IF NL* expression NL* statementBlock NL* ELSE NL* statementBlock ;

structInstanceExpr
    : modulePath? nameToken typeParamArg? STRUCT_START NL* (structInstanceEntry (commaOrNl structInstanceEntry)* COMMA?)? NL* RBRACKET ;

structInstanceEntry
    : nameToken (COLON NL* expression)? ;

variableExpr
    : modulePath? nameToken ;

modulePath
    : (nameToken DOUBLE_COLON)+ ;

// Function call

functionCallParams
    : typeParamArg? LPAREN NL* functionCallParamList? RPAREN ;

functionCallParamList
    : expression (commaOrNl expression)* COMMA? NL* ;

functionCallEnd
    : lambdaExpr
    | listExpr
    | mapExpr
    | setExpr
    ;

// Types
typeParamDef
    : LTH NL* typeParameter (commaOrNl typeParameter)* COMMA? NL* GTH ;

typeParamArg
    : LTH NL* typeUsage (commaOrNl typeUsage)* COMMA? NL* GTH ;

// #T, #A, #B, List<#A>
typeParameter
    : HASH nameToken ;

refModifier
    : REF
    | MUT
    ;

typeUsage
    : typeParameter
    | baseTypeUsage
    | THIS_TYPE
    | LTH NL* typeUsage NL* GTH
    ;

baseTypeUsage
    : refModifier? modulePath? nameToken typeParamArg? ;

// JSON value
jsonValue
   : string
   | INT_NUMBER
   | FLOAT_NUMBER
   | TRUE
   | FALSE
   | NULL
   | jsonObject
   | jsonArray
   | LPAREN NL* expression NL* RPAREN
   ;

jsonObject
   : LBRACE NL* (jsonPair (commaOrNl jsonPair)* COMMA?)? NL* RBRACE ;

jsonPair
   : (nameToken|string) NL* COLON NL* jsonValue ;

jsonArray
   : LBRACKET NL* (jsonValue (commaOrNl jsonValue)* COMMA?)? NL* RBRACKET ;

commaOrNl
    : COMMA NL*
    | NL+
    ;