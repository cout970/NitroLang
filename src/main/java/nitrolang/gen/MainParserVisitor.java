// Generated from /home/cout970/Dev/Kotlin/NitroLang/MainParser.g4 by ANTLR 4.13.1
package nitrolang.gen;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MainParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MainParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link MainParser#parseFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParseFile(MainParser.ParseFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#parseTypeUsage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParseTypeUsage(MainParser.ParseTypeUsageContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#parseExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParseExpression(MainParser.ParseExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#parseFunctionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParseFunctionDefinition(MainParser.ParseFunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#upperName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpperName(MainParser.UpperNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#anyName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnyName(MainParser.AnyNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(MainParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#stringContents}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringContents(MainParser.StringContentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#string2Contents}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString2Contents(MainParser.String2ContentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefinition(MainParser.DefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(MainParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#annotationArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgs(MainParser.AnnotationArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#annotationArgEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgEntry(MainParser.AnnotationArgEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#annotationArgKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgKey(MainParser.AnnotationArgKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#definitionChoice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefinitionChoice(MainParser.DefinitionChoiceContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#includeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncludeDefinition(MainParser.IncludeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#aliasDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasDefinition(MainParser.AliasDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#useDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDefinition(MainParser.UseDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#useDefinitionConst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDefinitionConst(MainParser.UseDefinitionConstContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#useDefinitionType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDefinitionType(MainParser.UseDefinitionTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#useDefinitionFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDefinitionFunction(MainParser.UseDefinitionFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#useDefinitionExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDefinitionExtension(MainParser.UseDefinitionExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#moduleDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModuleDefinition(MainParser.ModuleDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#constDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDefinition(MainParser.ConstDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#structDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDefinition(MainParser.StructDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#structBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructBody(MainParser.StructBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#structField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructField(MainParser.StructFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#optionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionDefinition(MainParser.OptionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#optionDefinitionItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionDefinitionItem(MainParser.OptionDefinitionItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#tagDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTagDefinition(MainParser.TagDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#tagDefinitionFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTagDefinitionFunction(MainParser.TagDefinitionFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typeAliasDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAliasDefinition(MainParser.TypeAliasDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#enumDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDefinition(MainParser.EnumDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#enumFields}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumFields(MainParser.EnumFieldsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#enumField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumField(MainParser.EnumFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#enumValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValue(MainParser.EnumValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#enumValueInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueInit(MainParser.EnumValueInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#testDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTestDefinition(MainParser.TestDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(MainParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionHeader(MainParser.FunctionHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionReceiver}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionReceiver(MainParser.FunctionReceiverContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionReturnType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionReturnType(MainParser.FunctionReturnTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionParameter(MainParser.FunctionParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionBody(MainParser.FunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#statementBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementBlock(MainParser.StatementBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(MainParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#statementChoice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementChoice(MainParser.StatementChoiceContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#letStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetStatement(MainParser.LetStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(MainParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(MainParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#repeatStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRepeatStatement(MainParser.RepeatStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(MainParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#loopStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoopStatement(MainParser.LoopStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#deferStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeferStatement(MainParser.DeferStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#foreignBlockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeignBlockStatement(MainParser.ForeignBlockStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#foreignBlockStatementPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeignBlockStatementPart(MainParser.ForeignBlockStatementPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(MainParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#assignableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableExpression(MainParser.AssignableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#binopShl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinopShl(MainParser.BinopShlContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#binopShr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinopShr(MainParser.BinopShrContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#binopUshr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinopUshr(MainParser.BinopUshrContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#binaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOperator(MainParser.BinaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(MainParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionComplex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionComplex(MainParser.ExpressionComplexContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionOr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionOr(MainParser.ExpressionOrContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionAnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionAnd(MainParser.ExpressionAndContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionBinaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionBinaryOp(MainParser.ExpressionBinaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionSimple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionSimple(MainParser.ExpressionSimpleContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionWithSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionWithSuffix(MainParser.ExpressionWithSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionOrFunctionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionOrFunctionCall(MainParser.ExpressionOrFunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#earlyReturnSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEarlyReturnSuffix(MainParser.EarlyReturnSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#assertSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssertSuffix(MainParser.AssertSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#collectionIndexingSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionIndexingSuffix(MainParser.CollectionIndexingSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#structFieldAccessSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructFieldAccessSuffix(MainParser.StructFieldAccessSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(MainParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#expressionBase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionBase(MainParser.ExpressionBaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#templateLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateLiteral(MainParser.TemplateLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#jsonExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonExpr(MainParser.JsonExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#constExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstExpr(MainParser.ConstExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#whenExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenExpr(MainParser.WhenExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#whenEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenEntry(MainParser.WhenEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#whenKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenKey(MainParser.WhenKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#listExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListExpr(MainParser.ListExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#listEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListEntry(MainParser.ListEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#mapExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapExpr(MainParser.MapExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#mapEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapEntry(MainParser.MapEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#mapKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapKey(MainParser.MapKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#setExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetExpr(MainParser.SetExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#lambdaExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExpr(MainParser.LambdaExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#lambdaDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaDef(MainParser.LambdaDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#lambdaParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParams(MainParser.LambdaParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#lambdaReceiver}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaReceiver(MainParser.LambdaReceiverContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#lambdaReturn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaReturn(MainParser.LambdaReturnContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#lambdaArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaArgument(MainParser.LambdaArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#returnExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnExpr(MainParser.ReturnExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#sizeOfExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSizeOfExpr(MainParser.SizeOfExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#notExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpr(MainParser.NotExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#minusExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinusExpr(MainParser.MinusExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#plusExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusExpr(MainParser.PlusExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#ifExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfExpr(MainParser.IfExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#structInstanceExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructInstanceExpr(MainParser.StructInstanceExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#structInstanceEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructInstanceEntry(MainParser.StructInstanceEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#variableExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableExpr(MainParser.VariableExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#modulePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModulePath(MainParser.ModulePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionCallParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallParams(MainParser.FunctionCallParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionCallParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallParamList(MainParser.FunctionCallParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionCallEnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallEnd(MainParser.FunctionCallEndContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typeParamsDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParamsDef(MainParser.TypeParamsDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typeParamDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParamDef(MainParser.TypeParamDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typeParamArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParamArg(MainParser.TypeParamArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#varModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarModifier(MainParser.VarModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typeUsage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeUsage(MainParser.TypeUsageContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(MainParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#baseTypeUsage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseTypeUsage(MainParser.BaseTypeUsageContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionTypeUsage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypeUsage(MainParser.FunctionTypeUsageContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionTypeUsageParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypeUsageParam(MainParser.FunctionTypeUsageParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#functionTypeUsageReturn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypeUsageReturn(MainParser.FunctionTypeUsageReturnContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypePattern(MainParser.TypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#baseTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseTypePattern(MainParser.BaseTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typePatternArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypePatternArgs(MainParser.TypePatternArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#typePatternArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypePatternArg(MainParser.TypePatternArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#jsonValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonValue(MainParser.JsonValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#jsonObject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonObject(MainParser.JsonObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#jsonPair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonPair(MainParser.JsonPairContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#jsonArray}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonArray(MainParser.JsonArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link MainParser#commaOrNl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommaOrNl(MainParser.CommaOrNlContext ctx);
}