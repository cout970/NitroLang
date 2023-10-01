// Generated from /Data/Dev/Kotlin/NitroLang/MainParser.g4 by ANTLR 4.12.0
package nitrolang.gen;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MainParser}.
 */
public interface MainParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MainParser#parseFile}.
	 * @param ctx the parse tree
	 */
	void enterParseFile(MainParser.ParseFileContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#parseFile}.
	 * @param ctx the parse tree
	 */
	void exitParseFile(MainParser.ParseFileContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#parseTypeUsage}.
	 * @param ctx the parse tree
	 */
	void enterParseTypeUsage(MainParser.ParseTypeUsageContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#parseTypeUsage}.
	 * @param ctx the parse tree
	 */
	void exitParseTypeUsage(MainParser.ParseTypeUsageContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#parseExpression}.
	 * @param ctx the parse tree
	 */
	void enterParseExpression(MainParser.ParseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#parseExpression}.
	 * @param ctx the parse tree
	 */
	void exitParseExpression(MainParser.ParseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#parseFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterParseFunctionDefinition(MainParser.ParseFunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#parseFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitParseFunctionDefinition(MainParser.ParseFunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#nameToken}.
	 * @param ctx the parse tree
	 */
	void enterNameToken(MainParser.NameTokenContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#nameToken}.
	 * @param ctx the parse tree
	 */
	void exitNameToken(MainParser.NameTokenContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#declaredNameToken}.
	 * @param ctx the parse tree
	 */
	void enterDeclaredNameToken(MainParser.DeclaredNameTokenContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#declaredNameToken}.
	 * @param ctx the parse tree
	 */
	void exitDeclaredNameToken(MainParser.DeclaredNameTokenContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(MainParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(MainParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#stringContents}.
	 * @param ctx the parse tree
	 */
	void enterStringContents(MainParser.StringContentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#stringContents}.
	 * @param ctx the parse tree
	 */
	void exitStringContents(MainParser.StringContentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#definition}.
	 * @param ctx the parse tree
	 */
	void enterDefinition(MainParser.DefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#definition}.
	 * @param ctx the parse tree
	 */
	void exitDefinition(MainParser.DefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(MainParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(MainParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#annotationArgs}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgs(MainParser.AnnotationArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#annotationArgs}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgs(MainParser.AnnotationArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#annotationArgEntry}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgEntry(MainParser.AnnotationArgEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#annotationArgEntry}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgEntry(MainParser.AnnotationArgEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#annotationArgKey}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgKey(MainParser.AnnotationArgKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#annotationArgKey}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgKey(MainParser.AnnotationArgKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#definitionChoice}.
	 * @param ctx the parse tree
	 */
	void enterDefinitionChoice(MainParser.DefinitionChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#definitionChoice}.
	 * @param ctx the parse tree
	 */
	void exitDefinitionChoice(MainParser.DefinitionChoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#includeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterIncludeDefinition(MainParser.IncludeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#includeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitIncludeDefinition(MainParser.IncludeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#aliasDefinition}.
	 * @param ctx the parse tree
	 */
	void enterAliasDefinition(MainParser.AliasDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#aliasDefinition}.
	 * @param ctx the parse tree
	 */
	void exitAliasDefinition(MainParser.AliasDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#useDefinition}.
	 * @param ctx the parse tree
	 */
	void enterUseDefinition(MainParser.UseDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#useDefinition}.
	 * @param ctx the parse tree
	 */
	void exitUseDefinition(MainParser.UseDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#useDefinitionConst}.
	 * @param ctx the parse tree
	 */
	void enterUseDefinitionConst(MainParser.UseDefinitionConstContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#useDefinitionConst}.
	 * @param ctx the parse tree
	 */
	void exitUseDefinitionConst(MainParser.UseDefinitionConstContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#useDefinitionType}.
	 * @param ctx the parse tree
	 */
	void enterUseDefinitionType(MainParser.UseDefinitionTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#useDefinitionType}.
	 * @param ctx the parse tree
	 */
	void exitUseDefinitionType(MainParser.UseDefinitionTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#useDefinitionFunction}.
	 * @param ctx the parse tree
	 */
	void enterUseDefinitionFunction(MainParser.UseDefinitionFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#useDefinitionFunction}.
	 * @param ctx the parse tree
	 */
	void exitUseDefinitionFunction(MainParser.UseDefinitionFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#useDefinitionExtension}.
	 * @param ctx the parse tree
	 */
	void enterUseDefinitionExtension(MainParser.UseDefinitionExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#useDefinitionExtension}.
	 * @param ctx the parse tree
	 */
	void exitUseDefinitionExtension(MainParser.UseDefinitionExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#moduleDefinition}.
	 * @param ctx the parse tree
	 */
	void enterModuleDefinition(MainParser.ModuleDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#moduleDefinition}.
	 * @param ctx the parse tree
	 */
	void exitModuleDefinition(MainParser.ModuleDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#constDefinition}.
	 * @param ctx the parse tree
	 */
	void enterConstDefinition(MainParser.ConstDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#constDefinition}.
	 * @param ctx the parse tree
	 */
	void exitConstDefinition(MainParser.ConstDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#structDefinition}.
	 * @param ctx the parse tree
	 */
	void enterStructDefinition(MainParser.StructDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#structDefinition}.
	 * @param ctx the parse tree
	 */
	void exitStructDefinition(MainParser.StructDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#structBody}.
	 * @param ctx the parse tree
	 */
	void enterStructBody(MainParser.StructBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#structBody}.
	 * @param ctx the parse tree
	 */
	void exitStructBody(MainParser.StructBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#structField}.
	 * @param ctx the parse tree
	 */
	void enterStructField(MainParser.StructFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#structField}.
	 * @param ctx the parse tree
	 */
	void exitStructField(MainParser.StructFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#optionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterOptionDefinition(MainParser.OptionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#optionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitOptionDefinition(MainParser.OptionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#optionDefinitionItem}.
	 * @param ctx the parse tree
	 */
	void enterOptionDefinitionItem(MainParser.OptionDefinitionItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#optionDefinitionItem}.
	 * @param ctx the parse tree
	 */
	void exitOptionDefinitionItem(MainParser.OptionDefinitionItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#tagDefinition}.
	 * @param ctx the parse tree
	 */
	void enterTagDefinition(MainParser.TagDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#tagDefinition}.
	 * @param ctx the parse tree
	 */
	void exitTagDefinition(MainParser.TagDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#tagDefinitionFunction}.
	 * @param ctx the parse tree
	 */
	void enterTagDefinitionFunction(MainParser.TagDefinitionFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#tagDefinitionFunction}.
	 * @param ctx the parse tree
	 */
	void exitTagDefinitionFunction(MainParser.TagDefinitionFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(MainParser.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(MainParser.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionHeader}.
	 * @param ctx the parse tree
	 */
	void enterFunctionHeader(MainParser.FunctionHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionHeader}.
	 * @param ctx the parse tree
	 */
	void exitFunctionHeader(MainParser.FunctionHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionReceiver}.
	 * @param ctx the parse tree
	 */
	void enterFunctionReceiver(MainParser.FunctionReceiverContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionReceiver}.
	 * @param ctx the parse tree
	 */
	void exitFunctionReceiver(MainParser.FunctionReceiverContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionReturnType}.
	 * @param ctx the parse tree
	 */
	void enterFunctionReturnType(MainParser.FunctionReturnTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionReturnType}.
	 * @param ctx the parse tree
	 */
	void exitFunctionReturnType(MainParser.FunctionReturnTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionParameter}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParameter(MainParser.FunctionParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionParameter}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParameter(MainParser.FunctionParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void enterFunctionBody(MainParser.FunctionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void exitFunctionBody(MainParser.FunctionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#statementBlock}.
	 * @param ctx the parse tree
	 */
	void enterStatementBlock(MainParser.StatementBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#statementBlock}.
	 * @param ctx the parse tree
	 */
	void exitStatementBlock(MainParser.StatementBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(MainParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(MainParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#statementChoice}.
	 * @param ctx the parse tree
	 */
	void enterStatementChoice(MainParser.StatementChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#statementChoice}.
	 * @param ctx the parse tree
	 */
	void exitStatementChoice(MainParser.StatementChoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#letStatement}.
	 * @param ctx the parse tree
	 */
	void enterLetStatement(MainParser.LetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#letStatement}.
	 * @param ctx the parse tree
	 */
	void exitLetStatement(MainParser.LetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(MainParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(MainParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(MainParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(MainParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#repeatStatement}.
	 * @param ctx the parse tree
	 */
	void enterRepeatStatement(MainParser.RepeatStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#repeatStatement}.
	 * @param ctx the parse tree
	 */
	void exitRepeatStatement(MainParser.RepeatStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(MainParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(MainParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void enterLoopStatement(MainParser.LoopStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void exitLoopStatement(MainParser.LoopStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#foreignBlockStatement}.
	 * @param ctx the parse tree
	 */
	void enterForeignBlockStatement(MainParser.ForeignBlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#foreignBlockStatement}.
	 * @param ctx the parse tree
	 */
	void exitForeignBlockStatement(MainParser.ForeignBlockStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#foreignBlockStatementPart}.
	 * @param ctx the parse tree
	 */
	void enterForeignBlockStatementPart(MainParser.ForeignBlockStatementPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#foreignBlockStatementPart}.
	 * @param ctx the parse tree
	 */
	void exitForeignBlockStatementPart(MainParser.ForeignBlockStatementPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(MainParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(MainParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#assignableExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignableExpression(MainParser.AssignableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#assignableExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignableExpression(MainParser.AssignableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#binopShl}.
	 * @param ctx the parse tree
	 */
	void enterBinopShl(MainParser.BinopShlContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#binopShl}.
	 * @param ctx the parse tree
	 */
	void exitBinopShl(MainParser.BinopShlContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#binopShr}.
	 * @param ctx the parse tree
	 */
	void enterBinopShr(MainParser.BinopShrContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#binopShr}.
	 * @param ctx the parse tree
	 */
	void exitBinopShr(MainParser.BinopShrContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#binopUshr}.
	 * @param ctx the parse tree
	 */
	void enterBinopUshr(MainParser.BinopUshrContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#binopUshr}.
	 * @param ctx the parse tree
	 */
	void exitBinopUshr(MainParser.BinopUshrContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#binaryOperator}.
	 * @param ctx the parse tree
	 */
	void enterBinaryOperator(MainParser.BinaryOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#binaryOperator}.
	 * @param ctx the parse tree
	 */
	void exitBinaryOperator(MainParser.BinaryOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(MainParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(MainParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionComplex}.
	 * @param ctx the parse tree
	 */
	void enterExpressionComplex(MainParser.ExpressionComplexContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionComplex}.
	 * @param ctx the parse tree
	 */
	void exitExpressionComplex(MainParser.ExpressionComplexContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionBinaryOp}.
	 * @param ctx the parse tree
	 */
	void enterExpressionBinaryOp(MainParser.ExpressionBinaryOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionBinaryOp}.
	 * @param ctx the parse tree
	 */
	void exitExpressionBinaryOp(MainParser.ExpressionBinaryOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionSimple}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSimple(MainParser.ExpressionSimpleContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionSimple}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSimple(MainParser.ExpressionSimpleContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionWithSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExpressionWithSuffix(MainParser.ExpressionWithSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionWithSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExpressionWithSuffix(MainParser.ExpressionWithSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionOrFunctionCall}.
	 * @param ctx the parse tree
	 */
	void enterExpressionOrFunctionCall(MainParser.ExpressionOrFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionOrFunctionCall}.
	 * @param ctx the parse tree
	 */
	void exitExpressionOrFunctionCall(MainParser.ExpressionOrFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#collectionIndexingSuffix}.
	 * @param ctx the parse tree
	 */
	void enterCollectionIndexingSuffix(MainParser.CollectionIndexingSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#collectionIndexingSuffix}.
	 * @param ctx the parse tree
	 */
	void exitCollectionIndexingSuffix(MainParser.CollectionIndexingSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#structFieldAccessSuffix}.
	 * @param ctx the parse tree
	 */
	void enterStructFieldAccessSuffix(MainParser.StructFieldAccessSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#structFieldAccessSuffix}.
	 * @param ctx the parse tree
	 */
	void exitStructFieldAccessSuffix(MainParser.StructFieldAccessSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(MainParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(MainParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionBase}.
	 * @param ctx the parse tree
	 */
	void enterExpressionBase(MainParser.ExpressionBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionBase}.
	 * @param ctx the parse tree
	 */
	void exitExpressionBase(MainParser.ExpressionBaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#jsonExpr}.
	 * @param ctx the parse tree
	 */
	void enterJsonExpr(MainParser.JsonExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#jsonExpr}.
	 * @param ctx the parse tree
	 */
	void exitJsonExpr(MainParser.JsonExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstExpr(MainParser.ConstExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#constExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstExpr(MainParser.ConstExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#nothingExpression}.
	 * @param ctx the parse tree
	 */
	void enterNothingExpression(MainParser.NothingExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#nothingExpression}.
	 * @param ctx the parse tree
	 */
	void exitNothingExpression(MainParser.NothingExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#expressionLiteral}.
	 * @param ctx the parse tree
	 */
	void enterExpressionLiteral(MainParser.ExpressionLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#expressionLiteral}.
	 * @param ctx the parse tree
	 */
	void exitExpressionLiteral(MainParser.ExpressionLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#constExpressionLiteral}.
	 * @param ctx the parse tree
	 */
	void enterConstExpressionLiteral(MainParser.ConstExpressionLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#constExpressionLiteral}.
	 * @param ctx the parse tree
	 */
	void exitConstExpressionLiteral(MainParser.ConstExpressionLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#whenExpr}.
	 * @param ctx the parse tree
	 */
	void enterWhenExpr(MainParser.WhenExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#whenExpr}.
	 * @param ctx the parse tree
	 */
	void exitWhenExpr(MainParser.WhenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#whenEntry}.
	 * @param ctx the parse tree
	 */
	void enterWhenEntry(MainParser.WhenEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#whenEntry}.
	 * @param ctx the parse tree
	 */
	void exitWhenEntry(MainParser.WhenEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#whenKey}.
	 * @param ctx the parse tree
	 */
	void enterWhenKey(MainParser.WhenKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#whenKey}.
	 * @param ctx the parse tree
	 */
	void exitWhenKey(MainParser.WhenKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#listExpr}.
	 * @param ctx the parse tree
	 */
	void enterListExpr(MainParser.ListExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#listExpr}.
	 * @param ctx the parse tree
	 */
	void exitListExpr(MainParser.ListExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#listEntry}.
	 * @param ctx the parse tree
	 */
	void enterListEntry(MainParser.ListEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#listEntry}.
	 * @param ctx the parse tree
	 */
	void exitListEntry(MainParser.ListEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#mapExpr}.
	 * @param ctx the parse tree
	 */
	void enterMapExpr(MainParser.MapExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#mapExpr}.
	 * @param ctx the parse tree
	 */
	void exitMapExpr(MainParser.MapExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#mapEntry}.
	 * @param ctx the parse tree
	 */
	void enterMapEntry(MainParser.MapEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#mapEntry}.
	 * @param ctx the parse tree
	 */
	void exitMapEntry(MainParser.MapEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#mapKey}.
	 * @param ctx the parse tree
	 */
	void enterMapKey(MainParser.MapKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#mapKey}.
	 * @param ctx the parse tree
	 */
	void exitMapKey(MainParser.MapKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#setExpr}.
	 * @param ctx the parse tree
	 */
	void enterSetExpr(MainParser.SetExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#setExpr}.
	 * @param ctx the parse tree
	 */
	void exitSetExpr(MainParser.SetExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#lambdaExpr}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExpr(MainParser.LambdaExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#lambdaExpr}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExpr(MainParser.LambdaExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#lambdaDef}.
	 * @param ctx the parse tree
	 */
	void enterLambdaDef(MainParser.LambdaDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#lambdaDef}.
	 * @param ctx the parse tree
	 */
	void exitLambdaDef(MainParser.LambdaDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#lambdaParams}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParams(MainParser.LambdaParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#lambdaParams}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParams(MainParser.LambdaParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#lambdaReceiver}.
	 * @param ctx the parse tree
	 */
	void enterLambdaReceiver(MainParser.LambdaReceiverContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#lambdaReceiver}.
	 * @param ctx the parse tree
	 */
	void exitLambdaReceiver(MainParser.LambdaReceiverContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#lambdaReturn}.
	 * @param ctx the parse tree
	 */
	void enterLambdaReturn(MainParser.LambdaReturnContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#lambdaReturn}.
	 * @param ctx the parse tree
	 */
	void exitLambdaReturn(MainParser.LambdaReturnContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#lambdaArgument}.
	 * @param ctx the parse tree
	 */
	void enterLambdaArgument(MainParser.LambdaArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#lambdaArgument}.
	 * @param ctx the parse tree
	 */
	void exitLambdaArgument(MainParser.LambdaArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void enterReturnExpr(MainParser.ReturnExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void exitReturnExpr(MainParser.ReturnExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#sizeOfExpr}.
	 * @param ctx the parse tree
	 */
	void enterSizeOfExpr(MainParser.SizeOfExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#sizeOfExpr}.
	 * @param ctx the parse tree
	 */
	void exitSizeOfExpr(MainParser.SizeOfExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#notExpr}.
	 * @param ctx the parse tree
	 */
	void enterNotExpr(MainParser.NotExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#notExpr}.
	 * @param ctx the parse tree
	 */
	void exitNotExpr(MainParser.NotExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#ifExpr}.
	 * @param ctx the parse tree
	 */
	void enterIfExpr(MainParser.IfExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#ifExpr}.
	 * @param ctx the parse tree
	 */
	void exitIfExpr(MainParser.IfExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#structInstanceExpr}.
	 * @param ctx the parse tree
	 */
	void enterStructInstanceExpr(MainParser.StructInstanceExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#structInstanceExpr}.
	 * @param ctx the parse tree
	 */
	void exitStructInstanceExpr(MainParser.StructInstanceExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#structInstanceEntry}.
	 * @param ctx the parse tree
	 */
	void enterStructInstanceEntry(MainParser.StructInstanceEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#structInstanceEntry}.
	 * @param ctx the parse tree
	 */
	void exitStructInstanceEntry(MainParser.StructInstanceEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#variableExpr}.
	 * @param ctx the parse tree
	 */
	void enterVariableExpr(MainParser.VariableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#variableExpr}.
	 * @param ctx the parse tree
	 */
	void exitVariableExpr(MainParser.VariableExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#modulePath}.
	 * @param ctx the parse tree
	 */
	void enterModulePath(MainParser.ModulePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#modulePath}.
	 * @param ctx the parse tree
	 */
	void exitModulePath(MainParser.ModulePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionCallParams}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallParams(MainParser.FunctionCallParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionCallParams}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallParams(MainParser.FunctionCallParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionCallParamList}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallParamList(MainParser.FunctionCallParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionCallParamList}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallParamList(MainParser.FunctionCallParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#functionCallEnd}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallEnd(MainParser.FunctionCallEndContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#functionCallEnd}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallEnd(MainParser.FunctionCallEndContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#typeParamsDef}.
	 * @param ctx the parse tree
	 */
	void enterTypeParamsDef(MainParser.TypeParamsDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#typeParamsDef}.
	 * @param ctx the parse tree
	 */
	void exitTypeParamsDef(MainParser.TypeParamsDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#typeParamDef}.
	 * @param ctx the parse tree
	 */
	void enterTypeParamDef(MainParser.TypeParamDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#typeParamDef}.
	 * @param ctx the parse tree
	 */
	void exitTypeParamDef(MainParser.TypeParamDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#typeParamArg}.
	 * @param ctx the parse tree
	 */
	void enterTypeParamArg(MainParser.TypeParamArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#typeParamArg}.
	 * @param ctx the parse tree
	 */
	void exitTypeParamArg(MainParser.TypeParamArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(MainParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(MainParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#refModifier}.
	 * @param ctx the parse tree
	 */
	void enterRefModifier(MainParser.RefModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#refModifier}.
	 * @param ctx the parse tree
	 */
	void exitRefModifier(MainParser.RefModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#typeUsage}.
	 * @param ctx the parse tree
	 */
	void enterTypeUsage(MainParser.TypeUsageContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#typeUsage}.
	 * @param ctx the parse tree
	 */
	void exitTypeUsage(MainParser.TypeUsageContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#baseTypeUsage}.
	 * @param ctx the parse tree
	 */
	void enterBaseTypeUsage(MainParser.BaseTypeUsageContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#baseTypeUsage}.
	 * @param ctx the parse tree
	 */
	void exitBaseTypeUsage(MainParser.BaseTypeUsageContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#jsonValue}.
	 * @param ctx the parse tree
	 */
	void enterJsonValue(MainParser.JsonValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#jsonValue}.
	 * @param ctx the parse tree
	 */
	void exitJsonValue(MainParser.JsonValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#jsonObject}.
	 * @param ctx the parse tree
	 */
	void enterJsonObject(MainParser.JsonObjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#jsonObject}.
	 * @param ctx the parse tree
	 */
	void exitJsonObject(MainParser.JsonObjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#jsonPair}.
	 * @param ctx the parse tree
	 */
	void enterJsonPair(MainParser.JsonPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#jsonPair}.
	 * @param ctx the parse tree
	 */
	void exitJsonPair(MainParser.JsonPairContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#jsonArray}.
	 * @param ctx the parse tree
	 */
	void enterJsonArray(MainParser.JsonArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#jsonArray}.
	 * @param ctx the parse tree
	 */
	void exitJsonArray(MainParser.JsonArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link MainParser#commaOrNl}.
	 * @param ctx the parse tree
	 */
	void enterCommaOrNl(MainParser.CommaOrNlContext ctx);
	/**
	 * Exit a parse tree produced by {@link MainParser#commaOrNl}.
	 * @param ctx the parse tree
	 */
	void exitCommaOrNl(MainParser.CommaOrNlContext ctx);
}