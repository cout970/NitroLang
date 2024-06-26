parser grammar MainParser;

// https://github.com/antlr/antlr4

options { tokenVocab = MainLexer; }

// Entry points
parseFile : NL* definition* EOF;
parseTypeUsage : NL* typeUsage EOF;
parseExpression : NL* expression EOF;
parseFunctionDefinition : NL* annotation* functionDefinition EOF;

// Identifiers
upperName : UPPER_IDENTIFIER ;
anyName : LOWER_IDENTIFIER | UPPER_IDENTIFIER ;

string
    : PLAIN_STRING
    | ASCII_STRING
    | STRING_START stringContents* STRING_END
    | STRING2_START string2Contents* STRING2_END
    ;

stringContents
    : STRING_BLOB
    | STRING_ESCAPE
    | STRING_VAR
    | STRING_INTERP_START expression (STRING_INTERP_END|RBRACE)
    ;

string2Contents
    : STRING2_BLOB
    | STRING2_NL
    ;

// Definitions
definition
    : annotation* definitionChoice NL* ;

// @Extern $[lib: "core", name: "println"]
annotation
    : AT upperName annotationArgs? NL* ;

annotationArgs
    : LBRACKET NL* (annotationArgEntry (commaOrNl annotationArgEntry)* COMMA?)? NL* RBRACKET ;

annotationArgEntry
    : annotationArgKey COLON NL* constExpr ;

annotationArgKey
    : anyName
    | PLAIN_STRING
    ;

definitionChoice
    : structDefinition
    | optionDefinition
    | functionDefinition
    | includeDefinition
    | useDefinition // Deprecated
    | moduleDefinition
    | constDefinition
    | tagDefinition
    | typeAliasDefinition
    | enumDefinition
    | testDefinition
    ;

// E.g. include "core:optional.nitro"
includeDefinition
    : INCLUDE PLAIN_STRING ;

useDefinition
    : USE modulePath (useDefinitionConst | useDefinitionType | useDefinitionFunction | useDefinitionExtension) ;

// E.g. use module::var
useDefinitionConst
    : anyName ;

// E.g. use module::<core::Type>
useDefinitionType
    : (LTH | LBRACE) NL* modulePath? anyName NL* (GTH | RBRACE) ;

// E.g. use module::plus()
useDefinitionFunction
    : anyName LPAREN RPAREN ;

// E.g. use module::Int.plus()
useDefinitionExtension
    : typeUsage DOT anyName LPAREN RPAREN ;

// E.g. mod core {}
moduleDefinition
    : MODULE modulePath? anyName NL* LBRACE NL* definition* RBRACE ;

// E.g. let pi: Float = 3.14
constDefinition
    : LET modulePath? anyName COLON typeUsage ASSIGN NL* expression ;

// E.g. struct List<Int> { }
structDefinition
    : STRUCT upperName typeParamsDef? NL* structBody;

// E.g. { a: Int, b: Int }
structBody
    : LBRACE NL* (structField (commaOrNl structField)* COMMA?)? NL* RBRACE ;

// E.g. value: Int,
structField
    : INTERNAL? anyName COLON typeUsage;

// E.g. type Optional<T> {}
optionDefinition
    : OPTION upperName typeParamsDef? NL* LBRACE NL*
    (optionDefinitionItem (commaOrNl optionDefinitionItem)* COMMA?)? NL*
    RBRACE ;

// E.g. Some { value: T },
optionDefinitionItem
    : upperName structBody?;

// E.g. tag ToString { fun to_string(): String }
tagDefinition
    : TAG upperName LBRACE NL* (tagDefinitionFunction (NL+ tagDefinitionFunction)*)? NL* RBRACE ;

tagDefinitionFunction
    : annotation* functionHeader;

// E.g. type_alias Meters = Float
// E.g. type_alias MultiMap<#Key, #Value> = Map<#Key, List<#Value>>
typeAliasDefinition
    : TYPE_ALIAS upperName typeParamsDef? ASSIGN NL* typeUsage ;

// E.g. enum Direction { Up, Down, Left, Right, Front, Back }
enumDefinition
    : ENUM upperName NL* LBRACE NL* enumFields? NL* enumValue (commaOrNl enumValue)* COMMA? NL* RBRACE
    | ENUM upperName NL* LBRACE NL* enumValue (commaOrNl enumValue)* COMMA? NL* enumFields? NL* RBRACE
    ;

enumFields
    : enumField (commaOrNl enumField)* COMMA? ;

// E.g. let name: String = "John"
enumField
    : LET anyName COLON typeUsage (ASSIGN NL* constExpr)? ;

// E.g. Up, Down, Left, Right, Front, Back
// E.g. Red $[rgb: 0xFF0000], Green $[rgb: 0x00FF00], Blue $[rgb: 0x0000FF
enumValue
    : anyName
    | anyName STRUCT_START NL* (enumValueInit (commaOrNl enumValueInit)* COMMA?)? NL* RBRACKET
    ;

// E.g. rgb: 0xFF0000
enumValueInit
    : anyName COLON NL* expression ;

// E.g. test! "Check that everything works" { }
testDefinition
    : TEST NL* PLAIN_STRING NL* statementBlock ;

// E.g. fun Int.sum(other: Int): Int {}
functionDefinition
    : functionHeader NL* functionBody
    | functionHeader
    ;

functionHeader
    : FUN NL* functionReceiver? modulePath? anyName NL* typeParamsDef? NL*
        LPAREN NL* (functionParameter (commaOrNl functionParameter)* COMMA?)? NL* RPAREN NL* functionReturnType?
    | FUN NL* typeParamsDef? NL* functionReceiver? modulePath? anyName NL*
        LPAREN NL* (functionParameter (commaOrNl functionParameter)* COMMA?)? NL* RPAREN NL* functionReturnType?;

// E.g. Int.
functionReceiver
    : varModifier typeUsage DOT ;

// E.g. : Int
functionReturnType
    : COLON typeUsage
    | ARROW typeUsage
    ;

// E.g. count: Int,
functionParameter
    : varModifier anyName NL* COLON NL* typeUsage
    | varModifier UNDERSCORE NL* COLON NL* typeUsage;

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
    | testDefinition
    ;

// E.g. let a: Int = 0
letStatement
    : LET varModifier anyName (COLON typeUsage)? (ASSIGN NL* expression)? ;

// E.g. if true {} else {}
ifStatement
    : IF NL* expression NL* statementBlock (NL* ELSE NL* statementBlock)? ;

// E.g. for item in list {}
forStatement
    : FOR NL* anyName NL* IN NL* expression NL* statementBlock ;

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
// E.g. i += 1
// E.g. 1 + 2
expressionStatement
    : assignableExpression ASSIGN NL* expression
    | assignableExpression ADD_ASSIGN NL* expression
    | assignableExpression SUB_ASSIGN NL* expression
    | assignableExpression MUL_ASSIGN NL* expression
    | assignableExpression DIV_ASSIGN NL* expression
    | assignableExpression MOD_ASSIGN NL* expression
    | expression
    ;

// E.g. struct.field = value
// E.g. list[index] = value
// E.g. list[] = value
// E.g. core::var = value
assignableExpression
    : expression DOT anyName
    | expression collectionIndexingSuffix
    | expression LBRACKET RBRACKET
    | modulePath? anyName
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
    : expressionSimple (binaryOperator NL* expressionSimple)* ;

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
    | expressionWithSuffix NOT IS typePattern
    | expressionWithSuffix IN expressionWithSuffix
    | expressionWithSuffix NOT IN expressionWithSuffix
    | expressionWithSuffix
    | notExpr
    | minusExpr
    | plusExpr
    ;

// E.g. expr @{ }
// E.g. expr[index]
// E.g. expr.field
// E.g. expr
// E.g. true()
// E.g. expr.field()
// E.g. func()()
// E.g. break()
// E.g. result!!
// E.g. result?
expressionWithSuffix
    : expressionWithSuffix earlyReturnSuffix
    | expressionWithSuffix assertSuffix
    | expressionWithSuffix collectionIndexingSuffix
    | expressionWithSuffix structFieldAccessSuffix
    | expressionWithSuffix NL? DOT modulePath? anyName functionCallParams functionCallEnd?
    | expressionWithSuffix NL? DOT modulePath? anyName functionCallEnd
    | expressionOrFunctionCall
    ;

expressionOrFunctionCall
    : modulePath? anyName functionCallEnd
    | modulePath? anyName functionCallParams functionCallEnd?
    | parenthesizedExpression functionCallParams functionCallEnd?
    | string functionCallParams functionCallEnd?
    | constExpr functionCallParams functionCallEnd?
    | structInstanceExpr functionCallParams functionCallEnd?
    | THIS functionCallParams functionCallEnd?
    | THIS functionCallEnd
    | expressionBase
    ;

earlyReturnSuffix
    : QUESTION_MARK ;

assertSuffix
    : BANGBANG ;

collectionIndexingSuffix
    : LBRACKET NL* expression NL* (COMMA NL* expression NL*)* RBRACKET ;

structFieldAccessSuffix
    : NL? DOT anyName ;

parenthesizedExpression
    : LPAREN NL* expression NL* RPAREN ;

expressionBase
    : parenthesizedExpression
    | constExpr
    | string
    | whenExpr
    | listExpr
    | mapExpr
    | setExpr
    | lambdaExpr
    | structInstanceExpr
    | sizeOfExpr
    | variableExpr
    | jsonExpr
    | templateLiteral
    | THIS
    | BREAK
    | CONTINUE
    ;

templateLiteral
    : anyName typeParamArg? STRING_START stringContents* STRING_END
    | anyName typeParamArg? PLAIN_STRING
    ;

// json {"key": ["val1", 1, true, null]}
jsonExpr
    : JSON jsonValue ;

// Constant value that can be evaluated at compile time
constExpr
    : INT_NUMBER
    | LONG_NUMBER
    | FLOAT_NUMBER
    | PLAIN_STRING
    | ASCII_STRING
    | TRUE
    | FALSE
    | NULL
    | NOTHING
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

// [item1, item2]
listExpr
    : LBRACKET NL* (listEntry (commaOrNl listEntry)* COMMA?)? NL* RBRACKET ;

// item
listEntry
    : expression;

// #[key: value, "key": value, ("key" + 1): value]
mapExpr
    : MAP_START NL* (mapEntry (commaOrNl mapEntry)* COMMA?)? NL* RBRACKET ;

mapEntry
    : mapKey COLON NL* expression ;

mapKey
    : LPAREN NL* expression NL* RPAREN
    | anyName
    | string
    ;

// %[1, 2, 3]
setExpr
    : SET_START NL* (listEntry (commaOrNl listEntry)* COMMA?)? NL* RBRACKET ;

// @{ a -> a + 1 }
// @{}
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
    : anyName (COLON typeUsage)?
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
    : modulePath? upperName typeParamArg? STRUCT_START NL* (structInstanceEntry (commaOrNl structInstanceEntry)* COMMA?)? NL* RBRACKET ;

structInstanceEntry
    : anyName COLON NL* expression
    | variableExpr
    ;

variableExpr
    : modulePath? anyName ;

modulePath
    : (anyName DOUBLE_COLON)+ ;

// Function call

functionCallParams
    : typeParamArg? LPAREN NL* functionCallParamList? RPAREN ;

functionCallParamList
    : expression (commaOrNl expression)* COMMA? NL* ;

functionCallEnd
    : lambdaExpr
//    | listExpr
//    | mapExpr
//    | setExpr
    ;

// Types
typeParamsDef
    : LTH NL* typeParamDef (commaOrNl typeParamDef)* COMMA? NL* GTH ;

typeParamDef
    : typeParameter COLON typeUsage (OR typeUsage)*
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
    : HASH upperName ;

// Int, List<Int>
baseTypeUsage
    : modulePath? upperName typeParamArg? ;

// (Int) -> Int
functionTypeUsage
    : LPAREN RPAREN ARROW functionTypeUsageReturn
    | LPAREN functionTypeUsageParam (commaOrNl functionTypeUsageParam)* RPAREN ARROW functionTypeUsageReturn
    ;

// a, a: Int
functionTypeUsageParam
    : typeUsage
    | anyName COLON typeUsage
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
    : modulePath? upperName typePatternArgs? ;

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
   : (anyName|string) NL* COLON NL* jsonValue ;

jsonArray
   : LBRACKET NL* (jsonValue (commaOrNl jsonValue)* COMMA?)? NL* RBRACKET ;

commaOrNl
    : COMMA NL*
    | NL+
    ;