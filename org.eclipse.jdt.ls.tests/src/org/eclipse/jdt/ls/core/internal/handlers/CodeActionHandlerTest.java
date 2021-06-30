/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.handlers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.ls.core.internal.CodeActionUtil;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection;
import org.eclipse.jdt.ls.core.internal.JavaCodeActionKind;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.LanguageServerWorkingCopyOwner;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.jdt.ls.core.internal.WorkspaceHelper;
import org.eclipse.jdt.ls.core.internal.codemanipulation.AbstractSourceTestCase;
import org.eclipse.jdt.ls.core.internal.correction.AbstractQuickFixTest;
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
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Gorkem Ercan
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CodeActionHandlerTest extends AbstractCompilationUnitBasedTest {

	@Mock
	private JavaClientConnection connection;
	private ClientPreferences clientPreferences;

	@Override
	@Before
	public void setup() throws Exception{
		importProjects("eclipse/hello");
		project = WorkspaceHelper.getProject("hello");
		wcOwner = new LanguageServerWorkingCopyOwner(connection);
		server = new JDTLanguageServer(projectsManager, this.preferenceManager);
	}

	@Override
	protected ClientPreferences initPreferenceManager(boolean supportClassFileContents) {
		clientPreferences = super.initPreferenceManager(supportClassFileContents);
		return clientPreferences;
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
		Assert.assertEquals(codeActions.get(2).getRight().getKind(), CodeActionKind.QuickFix);
		Assert.assertEquals(codeActions.get(3).getRight().getKind(), CodeActionKind.SourceOrganizeImports);
		Command c = codeActions.get(0).getRight().getCommand();
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
	}

	@Test
	public void testCodeAction_sourceActionsOnly() throws Exception {
		//@formatter:off
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"import java.sql.*; \n" +
				"public class Foo {\n"+
				"	void foo() {\n"+
				"	}\n"+
				"}\n");
		//@formatter:on
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "foo()");
		params.setRange(range);
		params.setContext(new CodeActionContext(Collections.emptyList(), Collections.singletonList(CodeActionKind.Source)));
		List<Either<Command, CodeAction>> sourceActions = getCodeActions(params);

		Assert.assertNotNull(sourceActions);
		Assert.assertFalse("No source actions were found", sourceActions.isEmpty());
		for (Either<Command, CodeAction> codeAction : sourceActions) {
			Assert.assertTrue("Unexpected kind:" + codeAction.getRight().getKind(), codeAction.getRight().getKind().startsWith(CodeActionKind.Source));
		}
	}

	@Test
	public void testCodeAction_organizeImportsSourceActionOnly() throws Exception {
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"import java.util.List;\n"+
				"public class Foo {\n"+
				"	void foo() {\n"+
				"		String bar = \"astring\";"+
				"	}\n"+
				"}\n");
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "bar");
		params.setRange(range);
		CodeActionContext context = new CodeActionContext(
			Arrays.asList(getDiagnostic(Integer.toString(IProblem.LocalVariableIsNeverUsed), range)),
			Collections.singletonList(CodeActionKind.SourceOrganizeImports)
		);
		params.setContext(context);
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);

		Assert.assertNotNull(codeActions);
		Assert.assertFalse("No organize imports actions were found", codeActions.isEmpty());
		for (Either<Command, CodeAction> codeAction : codeActions) {
			Assert.assertTrue("Unexpected kind:" + codeAction.getRight().getKind(), codeAction.getRight().getKind().startsWith(CodeActionKind.SourceOrganizeImports));
		}
	}

	@Test
	public void testCodeAction_refactorActionsOnly() throws Exception {
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"public class Foo {\n"+
				"	void foo() {\n"+
				"		String bar = \"astring\";"+
				"	}\n"+
				"}\n");
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "bar");
		params.setRange(range);
		CodeActionContext context = new CodeActionContext(
			Arrays.asList(getDiagnostic(Integer.toString(IProblem.LocalVariableIsNeverUsed), range)),
			Collections.singletonList(CodeActionKind.Refactor)
		);
		params.setContext(context);
		List<Either<Command, CodeAction>> refactorActions = getCodeActions(params);

		Assert.assertNotNull(refactorActions);
		Assert.assertFalse("No refactor actions were found", refactorActions.isEmpty());
		for (Either<Command, CodeAction> codeAction : refactorActions) {
			Assert.assertTrue("Unexpected kind:" + codeAction.getRight().getKind(), codeAction.getRight().getKind().startsWith(CodeActionKind.Refactor));
		}
	}

	@Test
	public void testCodeAction_errorFromOtherSources() throws Exception {
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"public class Foo {\n"+
				"	void foo() {\n"+
				"		Integer bar = 2000;"+
				"	}\n"+
				"}\n");
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "bar");
		params.setRange(range);
		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setCode("MagicNumberCheck");
		diagnostic.setRange(range);
		diagnostic.setSeverity(DiagnosticSeverity.Error);
		diagnostic.setMessage("'2000' is a magic number.");
		diagnostic.setSource("Checkstyle");
		CodeActionContext context = new CodeActionContext(
			Arrays.asList(diagnostic),
			Collections.singletonList(CodeActionKind.Refactor)
		);
		params.setContext(context);
		List<Either<Command, CodeAction>> refactorActions = getCodeActions(params);

		Assert.assertNotNull(refactorActions);
		Assert.assertFalse("No refactor actions were found", refactorActions.isEmpty());
		for (Either<Command, CodeAction> codeAction : refactorActions) {
			Assert.assertTrue("Unexpected kind:" + codeAction.getRight().getKind(), codeAction.getRight().getKind().startsWith(CodeActionKind.Refactor));
		}
	}

	@Test
	public void testCodeAction_quickfixActionsOnly() throws Exception {
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"public class Foo {\n"+
				"	void foo() {\n"+
				"		String bar = \"astring\";"+
				"	}\n"+
				"}\n");
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "bar");
		params.setRange(range);
		CodeActionContext context = new CodeActionContext(
			Arrays.asList(getDiagnostic(Integer.toString(IProblem.LocalVariableIsNeverUsed), range)),
			Collections.singletonList(CodeActionKind.QuickFix)
		);
		params.setContext(context);
		List<Either<Command, CodeAction>> quickfixActions = getCodeActions(params);

		Assert.assertNotNull(quickfixActions);
		Assert.assertFalse("No quickfix actions were found", quickfixActions.isEmpty());
		for (Either<Command, CodeAction> codeAction : quickfixActions) {
			Assert.assertTrue("Unexpected kind:" + codeAction.getRight().getKind(), codeAction.getRight().getKind().startsWith(CodeActionKind.QuickFix));
		}
	}

	@Test
	public void testCodeAction_allKindsOfActions() throws Exception {
		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"public class Foo {\n"+
				"	void foo() {\n"+
				"		String bar = \"astring\";"+
				"	}\n"+
				"}\n");
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "bar");
		params.setRange(range);
		CodeActionContext context = new CodeActionContext(
			Arrays.asList(getDiagnostic(Integer.toString(IProblem.LocalVariableIsNeverUsed), range))
		);
		params.setContext(context);
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);

		Assert.assertNotNull(codeActions);
		Assert.assertFalse("No code actions were found", codeActions.isEmpty());
		boolean hasQuickFix = codeActions.stream().anyMatch(codeAction -> codeAction.getRight().getKind().startsWith(CodeActionKind.QuickFix));
		assertTrue("No quickfix actions were found", hasQuickFix);
		boolean hasRefactor = codeActions.stream().anyMatch(codeAction -> codeAction.getRight().getKind().startsWith(CodeActionKind.Refactor));
		assertTrue("No refactor actions were found", hasRefactor);
		boolean hasSource = codeActions.stream().anyMatch(codeAction -> codeAction.getRight().getKind().startsWith(CodeActionKind.Source));
		assertTrue("No source actions were found", hasSource);

		List<String> baseKinds = codeActions.stream().map(codeAction -> getBaseKind(codeAction.getRight().getKind())).collect(Collectors.toList());
		assertTrue("quickfix actions should be ahead of refactor actions",  baseKinds.lastIndexOf(CodeActionKind.QuickFix) < baseKinds.indexOf(CodeActionKind.Refactor));
		assertTrue("refactor actions should be ahead of source actions",  baseKinds.lastIndexOf(CodeActionKind.Refactor) < baseKinds.indexOf(CodeActionKind.Source));
	}

	@Test
	public void testCodeAction_NPEInNewCUProposal() throws Exception {
		ICompilationUnit unit = getWorkingCopy(
		//@formatter:off
				"src/org/sample/Foo.java",
				"package org.sample;\n"+
				"public class Foo {\n"+
				"	public static void main(String[] args) {\n"+
				"		new javax.activity\n"+
				"	}\n"+
				"}\n");
		//@formatter:off
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "javax.activity");
		params.setRange(range);
		CodeActionContext context = new CodeActionContext(
			Arrays.asList(getDiagnostic(Integer.toString(IProblem.UndefinedType), range))
		);
		params.setContext(context);
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
	}

	private static String getBaseKind(String codeActionKind) {
		if (codeActionKind.contains(".")) {
			return codeActionKind.substring(0, codeActionKind.indexOf('.'));
		} else {
			return codeActionKind;
		}
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
		Command c = codeActions.get(0).getRight().getCommand();
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
	}

	@Test
	public void testCodeAction_exception() throws JavaModelException {
		URI uri = project.getFile("nopackage/Test.java").getRawLocationURI();
		ICompilationUnit cu = JDTUtils.resolveCompilationUnit(uri);
		try {
			cu.becomeWorkingCopy(new NullProgressMonitor());
			CodeActionParams params = new CodeActionParams();
			params.setTextDocument(new TextDocumentIdentifier(uri.toString()));
			final Range range = new Range();
			range.setStart(new Position(0, 17));
			range.setEnd(new Position(0, 17));
			params.setRange(range);
			CodeActionContext context = new CodeActionContext();
			context.setDiagnostics(Collections.emptyList());
			params.setContext(context);
			List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
			Assert.assertNotNull(codeActions);
		} finally {
			cu.discardWorkingCopy();
		}
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
		Command c = getCommand(codeActions.get(0));
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
		Assert.assertNotNull(c.getArguments());
		Assert.assertTrue(c.getArguments().get(0) instanceof WorkspaceEdit);
		WorkspaceEdit we = (WorkspaceEdit) c.getArguments().get(0);
		List<org.eclipse.lsp4j.TextEdit> edits = we.getChanges().get(JDTUtils.toURI(unit));
		Assert.assertEquals(1, edits.size());
		Assert.assertEquals("", edits.get(0).getNewText());
		Assert.assertEquals(range, edits.get(0).getRange());
	}

	@Test
	public void test_noUnnecessaryCodeActions() throws Exception{
		//@formatter:off
		ICompilationUnit unit = getWorkingCopy(
				"src/org/sample/Foo.java",
				"package org.sample;\n"+
				"\n"+
				"public class Foo {\n"+
				"	private String foo;\n"+
				"	public String getFoo() {\n"+
				"	  return foo;\n"+
				"	}\n"+
				"   \n"+
				"	public void setFoo(String newFoo) {\n"+
				"	  foo = newFoo;\n"+
				"	}\n"+
				"}\n");
		//@formatter:on
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "String foo;");
		params.setRange(range);
		params.setContext(new CodeActionContext(Collections.emptyList()));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Assert.assertFalse("No need for organize imports action", containsKind(codeActions, CodeActionKind.SourceOrganizeImports));
		Assert.assertFalse("No need for generate getter and setter action", containsKind(codeActions, JavaCodeActionKind.SOURCE_GENERATE_ACCESSORS));
	}

	@Test
	public void test_filterTypes() throws Exception {
		//@formatter:off
		ICompilationUnit unit = getWorkingCopy(
				"src/org/sample/Foo.java",
				"package org.sample;\n"+
				"\n"+
				"public class Foo {\n"+
				"	List foo;\n"+
				"}\n");
		//@formatter:on
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "List");
		params.setRange(range);
		params.setContext(new CodeActionContext(Collections.emptyList()));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Assert.assertTrue("No organize imports action", containsKind(codeActions, CodeActionKind.SourceOrganizeImports));
		try {
			List<String> filteredTypes = new ArrayList<>();
			filteredTypes.add("java.util.*");
			PreferenceManager.getPrefs(null).setFilteredTypes(filteredTypes);
			codeActions = getCodeActions(params);
			assertNotNull(codeActions);
			Assert.assertFalse("No need for organize imports action", containsKind(codeActions, CodeActionKind.SourceOrganizeImports));
		} finally {
			PreferenceManager.getPrefs(null).setFilteredTypes(Collections.emptyList());
		}
	}

	@Test
	public void testCodeAction_ignoringOtherDiagnosticWithoutCode() throws Exception {
		ICompilationUnit unit = getWorkingCopy("src/java/Foo.java", "import java.sql.*; \n" + "public class Foo {\n" + "	void foo() {\n" + "	}\n" + "}\n");
		//unit.save(new NullProgressMonitor(), true);
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "java.sql");
		params.setRange(range);

		Diagnostic diagnosticWithoutCode = new Diagnostic(new Range(new Position(0, 0), new Position(0, 1)), "fake dignostic without code");

		params.setContext(new CodeActionContext(Arrays.asList(diagnosticWithoutCode, getDiagnostic(Integer.toString(IProblem.UnusedImport), range))));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Assert.assertTrue(codeActions.size() >= 3);
		Assert.assertEquals(codeActions.get(0).getRight().getKind(), CodeActionKind.QuickFix);
		Assert.assertEquals(codeActions.get(1).getRight().getKind(), CodeActionKind.QuickFix);
		Assert.assertEquals(codeActions.get(2).getRight().getKind(), CodeActionKind.QuickFix);
		Assert.assertEquals(codeActions.get(3).getRight().getKind(), CodeActionKind.SourceOrganizeImports);
		Command c = codeActions.get(0).getRight().getCommand();
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
	}

	@Test
	public void testCodeAction_customFileFormattingOptions() throws Exception {
		when(clientPreferences.isWorkspaceConfigurationSupported()).thenReturn(true);
		when(connection.configuration(Mockito.any())).thenReturn(Arrays.asList(4, true/*Indent using Spaces*/));
		server.setClientConnection(connection);
		JavaLanguageServerPlugin.getInstance().setProtocol(server);
		IJavaProject javaProject = ProjectUtils.getJavaProject(project);
		Map<String, String> projectOptions = javaProject.getOptions(false);
		projectOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB); // Indent using Tabs
		projectOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		javaProject.setOptions(projectOptions);

		IPackageFragmentRoot sourceFolder = javaProject.getPackageFragmentRoot(project.getFolder("src"));
		IPackageFragment pack1 = sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder builder = new StringBuilder();
		builder.append("package test1;\n");
		builder.append("interface I {\n");
		builder.append("	void method();\n");
		builder.append("}\n");
		builder.append("public class E {\n");
		builder.append("	void bar(I i) {\n");
		builder.append("	}\n");
		builder.append("	void foo() {\n");
		builder.append("		bar(() /*[*//*]*/-> {\n");
		builder.append("		});\n");
		builder.append("	}\n");
		builder.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", builder.toString(), false, null);

		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(cu)));
		final Range range = CodeActionUtil.getRange(cu, "/*[*//*]*/");
		params.setRange(range);
		params.setContext(new CodeActionContext(Collections.emptyList(), Arrays.asList(CodeActionKind.Refactor)));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);
		Assert.assertNotNull(codeActions);
		Optional<Either<Command, CodeAction>> found = codeActions.stream().filter((codeAction) -> {
			return codeAction.isRight() && Objects.equals("Convert to anonymous class creation", codeAction.getRight().getTitle());
		}).findAny();
		Assert.assertTrue(found.isPresent());

		Either<Command, CodeAction> codeAction = found.get();
		Command c = codeAction.isLeft() ? codeAction.getLeft() : codeAction.getRight().getCommand();
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
		Assert.assertNotNull(c.getArguments());
		Assert.assertTrue(c.getArguments().get(0) instanceof WorkspaceEdit);
		WorkspaceEdit edit = (WorkspaceEdit) c.getArguments().get(0);
		String actual = AbstractQuickFixTest.evaluateWorkspaceEdit(edit);
		builder = new StringBuilder();
		builder.append("package test1;\n");
		builder.append("interface I {\n");
		builder.append("	void method();\n");
		builder.append("}\n");
		builder.append("public class E {\n");
		builder.append("	void bar(I i) {\n");
		builder.append("	}\n");
		builder.append("	void foo() {\n");
		builder.append("		bar(new I() {\n");
		builder.append("            @Override\n");
		builder.append("            public void method() {\n");
		builder.append("            }\n");
		builder.append("        });\n");
		builder.append("	}\n");
		builder.append("}\n");
		AbstractSourceTestCase.compareSource(builder.toString(), actual);
	}

	@Test
	public void testCodeAction_ignoreCompilerIssue() throws Exception{
		when(clientPreferences.isResourceOperationSupported()).thenReturn(true);

		ICompilationUnit unit = getWorkingCopy(
				"src/java/Foo.java",
				"package java;\n"
				+ "public class Foo {\n"
				+ "  @SuppressWarnings(\"deprecation\")\n"
				+ "  public void test () {\n"
				+ "  }\n"
				+ "}");

		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(JDTUtils.toURI(unit)));
		final Range range = CodeActionUtil.getRange(unit, "deprecation");
		params.setRange(range);
		params.setContext(new CodeActionContext(Arrays.asList(getDiagnostic(Integer.toString(IProblem.UnusedWarningToken), range))));
		List<Either<Command, CodeAction>> codeActions = getCodeActions(params);

		Assert.assertNotNull(codeActions);
		Assert.assertFalse(codeActions.isEmpty());
		Assert.assertEquals(codeActions.get(0).getRight().getKind(), CodeActionKind.QuickFix);
		Command c = codeActions.get(0).getRight().getCommand();
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());

		IFile settingsFile = unit.getJavaProject().getProject().getFolder(ProjectUtils.WORKSPACE_LINK).getFolder(ProjectUtils.SETTINGS).getFile("org.eclipse.jdt.core.prefs");
		Assert.assertNotNull(c.getArguments());
		Assert.assertTrue(c.getArguments().get(0) instanceof WorkspaceEdit);
		WorkspaceEdit we = (WorkspaceEdit) c.getArguments().get(0);
		Assert.assertEquals(2, we.getDocumentChanges().size());

		ResourceOperation createFile = we.getDocumentChanges().get(0).getRight();
		Assert.assertEquals(ResourceOperationKind.Create, createFile.getKind());

		TextDocumentEdit textDocEdit = we.getDocumentChanges().get(1).getLeft();
		Assert.assertEquals(JDTUtils.getFileURI(settingsFile), textDocEdit.getTextDocument().getUri());
		Assert.assertEquals("org.eclipse.jdt.core.compiler.problem.unusedWarningToken=ignore\n", textDocEdit.getEdits().get(0).getNewText());
	}

	private List<Either<Command, CodeAction>> getCodeActions(CodeActionParams params) {
		return server.codeAction(params).join();
	}

	public static Command getCommand(Either<Command, CodeAction> codeAction) {
		return codeAction.isLeft() ? codeAction.getLeft() : codeAction.getRight().getCommand();
	}

	private Diagnostic getDiagnostic(String code, Range range){
		Diagnostic $ = new Diagnostic();
		$.setCode(code);
		$.setRange(range);
		$.setSeverity(DiagnosticSeverity.Error);
		$.setMessage("Test Diagnostic");
		$.setSource(JavaLanguageServerPlugin.SERVER_SOURCE_ID);
		return $;
	}

	public static boolean containsKind(List<Either<Command, CodeAction>> codeActions, String kind) {
		for (Either<Command, CodeAction> action : codeActions) {
			String actionKind = action.getLeft() == null ? action.getRight().getKind() : action.getLeft().getCommand();
			if (Objects.equals(actionKind, kind)) {
				return true;
			}
		}

		return false;
	}

	public static Either<Command, CodeAction> findAction(List<Either<Command, CodeAction>> codeActions, String kind) {
		Optional<Either<Command, CodeAction>> any = codeActions.stream().filter((action) -> Objects.equals(kind, action.getLeft() == null ? action.getRight().getKind() : action.getLeft().getCommand())).findFirst();
		return any.isPresent() ? any.get() : null;
	}
}
