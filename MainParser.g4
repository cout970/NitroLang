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
    | typeAliasDefinition
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
    : STRUCT declaredNameToken typeParamsDef? NL* structBody;

// E.g. { a: Int, b: Int }
structBody
    : LBRACE NL* (structField (commaOrNl structField)* COMMA?)? NL* RBRACE ;

// E.g. value: Int,
structField
    : nameToken COLON typeUsage;

// E.g. type Optional<T> {}
optionDefinition
    : OPTION declaredNameToken typeParamsDef? NL* LBRACE NL*
    (optionDefinitionItem (commaOrNl optionDefinitionItem)* COMMA?)? NL*
    RBRACE ;

// E.g. Some { value: T },
optionDefinitionItem
    : declaredNameToken structBody?;

// E.g. tag ToString { fun to_string(): String }
tagDefinition
    : TAG declaredNameToken LBRACE NL* (tagDefinitionFunction (NL+ tagDefinitionFunction)*)? NL* RBRACE ;

tagDefinitionFunction
    : annotation* functionHeader;

// E.g. type_alias Meters = Float
// E.g. type_alias MultiMap<#Key, #Value> = Map<#Key, List<#Value>>
typeAliasDefinition
    : TYPE_ALIAS declaredNameToken typeParamsDef? ASSIGN NL* typeUsage ;

// E.g. fun Int.sum(other: Int): Int {}
functionDefinition
    : functionHeader NL* functionBody
    | functionHeader
    ;

functionHeader
    : FUN NL* functionReceiver? modulePath? declaredNameToken NL* typeParamsDef? NL*
        LPAREN NL* (functionParameter (commaOrNl functionParameter)* COMMA?)? NL* RPAREN NL* functionReturnType?
    | FUN NL* typeParamsDef? NL* functionReceiver? modulePath? declaredNameToken NL*
        LPAREN NL* (functionParameter (commaOrNl functionParameter)* COMMA?)? NL* RPAREN NL* functionReturnType?;

// E.g. Int.
functionReceiver
    : varModifier typeUsage DOT ;

// E.g. : Int
functionReturnType
    : COLON typeUsage ;

// E.g. count: Int,
functionParameter
    : varModifier nameToken NL* COLON NL* typeUsage;

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
    | deferStatement
    | expressionStatement
    | foreignBlockStatement
    ;

// E.g. let a: Int = 0
letStatement
    : LET varModifier nameToken (COLON typeUsage)? (ASSIGN NL* expression)? ;

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

deferStatement
    : DEFER expression
    | DEFER statementBlock
    ;

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
    : MUL | DIV | MOD | ADD | SUB | RANGE_IN | RANGE_EX | binopShl | binopShr | binopUshr | AND | XOR | OR | LTH | GTH | LEQ | GEQ | COMPARE | EQ | NEQ | XORXOR ;

expression
    : expressionComplex ;

// E.g. 1 + 2
// E.g. if x > 5 { 1 } else { 0 }
// E.g. ret 5
// E.g. 1 || 2 && 3
expressionComplex
    : expressionOr
    | ifExpr
    | returnExpr
    ;

// E.g. 1 || 2
expressionOr
    : expressionAnd (OROR expressionAnd)* ;

// E.g. 1 && 2
expressionAnd
    : expressionBinaryOp (ANDAND expressionBinaryOp)* ;

// E.g. 1.0 + math::PI * 4.0
expressionBinaryOp
    : expressionSimple (binaryOperator expressionSimple)* ;

// E.g. var as Int
// E.g. var is Int
// E.g. var !is Int
// E.g. var in %[1, 2, 3]
// E.g. var !in %[1, 2, 3]
// E.g. not var
// E.g. -var
expressionSimple
    : expressionWithSuffix AS typeUsage
    | expressionWithSuffix IS typePattern
    | expressionWithSuffix NOT_IS typePattern
    | expressionWithSuffix IN expressionWithSuffix
    | expressionWithSuffix NOT_IN expressionWithSuffix
    | expressionWithSuffix
    | notExpr
    | minusExpr
    | plusExpr
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
    : expressionWithSuffix assertSuffix
    | expressionWithSuffix collectionIndexingSuffix
    | expressionWithSuffix structFieldAccessSuffix
    | expressionWithSuffix NL? DOT nameToken functionCallParams functionCallEnd?
    | expressionWithSuffix NL? DOT nameToken functionCallEnd
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

assertSuffix
    : BANGBANG ;

collectionIndexingSuffix
    : LBRACKET NL* expression NL* RBRACKET ;

structFieldAccessSuffix
    : NL? DOT nameToken ;

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
    : SET_START NL* (listEntry (commaOrNl listEntry)* COMMA?)? NL* RBRACKET ;

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
    : nameToken (COLON typeUsage)?
    | UNDERSCORE (COLON typeUsage)?
    ;

// E.g. ret 1
returnExpr
    : RETURN expression? ;

// E.g. size_of<Int>()
sizeOfExpr
    : SIZE_OF LTH NL* typeUsage NL* GTH
    | SIZE_OF LTH NL* typeUsage NL* GTH LPAREN RPAREN
    ;

// E.g. not true
notExpr
    : NOT expressionSimple ;

// E.g. -x
minusExpr
    : SUB expressionSimple ;

// E.g. +x
plusExpr
    : ADD expressionSimple ;

ifExpr
    : IF NL* expression NL* statementBlock NL* ELSE NL* statementBlock ;

structInstanceExpr
    : modulePath? nameToken typeParamArg? STRUCT_START NL* (structInstanceEntry (commaOrNl structInstanceEntry)* COMMA?)? NL* RBRACKET ;

structInstanceEntry
    : nameToken COLON NL* expression
    | variableExpr
    ;

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
typeParamsDef
    : LTH NL* typeParamDef (commaOrNl typeParamDef)* COMMA? NL* GTH ;
    
typeParamDef
    : typeParameter COLON typeUsage (commaOrNl typeUsage)*
    | typeParameter
    ;

typeParamArg
    : LTH NL* typeUsage (commaOrNl typeUsage)* COMMA? NL* GTH ;

// mut
varModifier
    : MUT? ;

// Types

typeUsage
    : THIS_TYPE
    | LTH NL* typeUsage NL* GTH
    | baseTypeUsage
    | baseTypeUsage DOT functionTypeUsage
    | functionTypeUsage
    | typeParameter
    ;

// #T, #A, #B, List<#A>
typeParameter
    : HASH nameToken ;

// Int, List<Int>
baseTypeUsage
    : modulePath? nameToken typeParamArg? ;

// (Int) -> Int
functionTypeUsage
    : LPAREN RPAREN ARROW functionTypeUsageReturn
    | LPAREN functionTypeUsageParam (commaOrNl functionTypeUsageParam)* RPAREN ARROW functionTypeUsageReturn
    ;

// a, a: Int
functionTypeUsageParam
    : typeUsage
    | nameToken COLON typeUsage
    ;

// Int
functionTypeUsageReturn
    : typeUsage ;

// Patterns

typePattern
    : THIS_TYPE
    | LTH NL* typePattern NL* GTH
    | baseTypePattern
    | typeParameter
    ;

baseTypePattern
    : modulePath? nameToken typePatternArgs? ;

typePatternArgs
    : LTH NL* typePatternArg (commaOrNl typePatternArg)* COMMA? NL* GTH ;

typePatternArg
    : typePattern
    | MUL
    ;

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