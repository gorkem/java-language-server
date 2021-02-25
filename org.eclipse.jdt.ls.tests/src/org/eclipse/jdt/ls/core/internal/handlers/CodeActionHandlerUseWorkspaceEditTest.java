/*******************************************************************************
 * Copyright (c) 2019 mikoto2000 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     mikoto2000 - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.handlers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.ls.core.internal.CodeActionUtil;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection;
import org.eclipse.jdt.ls.core.internal.JavaCodeActionKind;
import org.eclipse.jdt.ls.core.internal.LanguageServerWorkingCopyOwner;
import org.eclipse.jdt.ls.core.internal.WorkspaceHelper;
import org.eclipse.jdt.ls.core.internal.preferences.ClientPreferences;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Gorkem Ercan
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CodeActionHandlerUseWorkspaceEditTest extends AbstractCompilationUnitBasedTest {

	@Mock
	private JavaClientConnection connection;


	@Override
	@Before
	public void setup() throws Exception{
		importProjects("eclipse/hello");
		project = WorkspaceHelper.getProject("hello");
		wcOwner = new LanguageServerWorkingCopyOwner(connection);
		server = new JDTLanguageServer(projectsManager, this.preferenceManager);
	}

	@Test
	public void testCodeAction_removeUnusedImport() throws Exception{
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"import java.sql.*; \n" +
						"public class Foo {\n"+
						"	void foo() {\n"+
						"	}\n"+
				"}\n");

		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "java.sql");
		params.setRange(range);
		params.setContext(new CodeActionContext(Arrays.asList(getDiagnostic(Integer.toString(IProblem.UnusedImport), range))));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Assert.assertTrue(codeActions.size() >= 3);
		Assert.assertEquals(codeActions.get(0).getRight().getKind(), CodeActionKind.QuickFix);
		Assert.assertEquals(codeActions.get(1).getRight().getKind(), CodeActionKind.QuickFix);
		Assert.assertEquals(codeActions.get(2).getRight().getKind(), CodeActionKind.SourceOrganizeImports);
		WorkspaceEdit w = codeActions.get(0).getRight().getEdit();
		Assert.assertNotNull(w);
	}

	@Test
	public void testCodeAction_removeUnterminatedString() throws Exception{
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"public class Foo {\n"+
						"	void foo() {\n"+
						"String s = \"some str\n" +
						"	}\n"+
				"}\n");

		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "some str");
		params.setRange(range);
		params.setContext(new CodeActionContext(Arrays.asList(getDiagnostic(Integer.toString(IProblem.UnterminatedString), range))));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Assert.assertFalse(codeActions.isEmpty());
		Assert.assertEquals(codeActions.get(0).getRight().getKind(), CodeActionKind.QuickFix);
		WorkspaceEdit w = codeActions.get(0).getRight().getEdit();
		Assert.assertNotNull(w);
	}

	@Test
	@Ignore
	public void testCodeAction_superfluousSemicolon() throws Exception{
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"public class Foo {\n"+
						"	void foo() {\n"+
						";" +
						"	}\n"+
				"}\n");

		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, ";");
		params.setRange(range);
		params.setContext(new CodeActionContext(Arrays.asList(getDiagnostic(Integer.toString(IProblem.SuperfluousSemicolon), range))));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Assert.assertEquals(1, codeActions.size());
		Assert.assertEquals(codeActions.get(0), CodeActionKind.QuickFix);
		Command c = CodeActionHandlerTest.getCommand(codeActions.get(0));
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
		Assert.assertNotNull(c.getArguments());
		Assert.assertTrue(c.getArguments().get(0) instanceof WorkspaceEdit);
		WorkspaceEdit we = (WorkspaceEdit) c.getArguments().get(0);
		List<org.eclipse.lsp4j.TextEdit> edits = we.getChanges().get(JDTUtils.toURI(unit));
		Assert.assertEquals(1, edits.size());
		Assert.assertEquals("", edits.get(0).getNewText());
		Assert.assertEquals(range, edits.get(0).getRange());
	}

	private List<Either<Command, CodeAction>> getCodeActions(CodeActionParams params) {
		return server.codeAction(params).join();
	}

	private Diagnostic getDiagnostic(String code, Range range){
		Diagnostic $ = new Diagnostic();
		$.setCode(code);
		$.setRange(range);
		$.setSeverity(DiagnosticSeverity.Error);
		$.setMessage("Test Diagnostic");
		return $;
	}

	protected ClientPreferences initPreferenceManager(boolean supportClassFileContents) {
		PreferenceManager.initialize();
		when(preferenceManager.getPreferences()).thenReturn(preferences);
		when(preferenceManager.getPreferences(any())).thenReturn(preferences);
		when(preferenceManager.isClientSupportsClassFileContent()).thenReturn(supportClassFileContents);
		ClientPreferences clientPreferences = mock(ClientPreferences.class);
		when(clientPreferences.isProgressReportSupported()).thenReturn(true);
		when(clientPreferences.isSemanticHighlightingSupported()).thenReturn(true);
		when(preferenceManager.getClientPreferences()).thenReturn(clientPreferences);
		when(clientPreferences.isSupportedCodeActionKind(anyString())).thenReturn(true);
		when(clientPreferences.isOverrideMethodsPromptSupported()).thenReturn(true);
		when(clientPreferences.isHashCodeEqualsPromptSupported()).thenReturn(true);
		when(clientPreferences.isGenerateToStringPromptSupported()).thenReturn(true);
		when(clientPreferences.isAdvancedGenerateAccessorsSupported()).thenReturn(true);
		when(clientPreferences.isGenerateConstructorsPromptSupported()).thenReturn(true);
		when(clientPreferences.isGenerateDelegateMethodsPromptSupported()).thenReturn(true);
		when(clientPreferences.isWorkspaceApplyEditSupported()).thenReturn(true);
		return clientPreferences;
	}
}
