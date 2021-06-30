/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.jdt.ls.core.internal.correction;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.JavaCodeActionKind;
import org.eclipse.jdt.ls.core.internal.handlers.CodeActionHandler;
import org.eclipse.jdt.ls.core.internal.text.correction.ActionMessages;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MissingEnumQuickFixTest extends AbstractQuickFixTest {
	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setup() throws Exception {
		fJProject = newEmptyProject();
		fJProject.setOptions(TestOptions.getDefaultOptions());
		fSourceFolder = fJProject.getPackageFragmentRoot(fJProject.getProject().getFolder("src"));
		setIgnoredKind(CodeActionKind.Refactor, JavaCodeActionKind.SOURCE_OVERRIDE_METHODS, JavaCodeActionKind.SOURCE_GENERATE_TO_STRING, JavaCodeActionKind.SOURCE_GENERATE_CONSTRUCTORS, JavaCodeActionKind.SOURCE_GENERATE_FINAL_MODIFIERS,
				JavaCodeActionKind.REFACTOR_EXTRACT_FIELD, JavaCodeActionKind.REFACTOR_EXTRACT_VARIABLE, JavaCodeActionKind.REFACTOR_INTRODUCE_PARAMETER, JavaCodeActionKind.REFACTOR_EXTRACT_METHOD, CodeActionKind.RefactorInline);
	}

	@Test
	public void testMissingEnumConstant() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("   public enum Numbers { One, Two};\n");
		buf.append("    public void testing() {\n");
		buf.append("        Numbers n = Numbers.One;\n");
		buf.append("        switch (n) {\n");
		buf.append("        case Two:\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		Range range = new Range(new Position(5, 16), new Position(5, 17));
		setIgnoredCommands(ActionMessages.GenerateConstructorsAction_ellipsisLabel, ActionMessages.GenerateConstructorsAction_label);
		List<Either<Command, CodeAction>> codeActions = evaluateCodeActions(cu, range);
		assertEquals(3, codeActions.size());
		Either<Command, CodeAction> codeAction = codeActions.get(0);
		CodeAction action = codeAction.getRight();
		assertEquals(CodeActionKind.QuickFix, action.getKind());
		assertEquals("Add 'default' case", action.getTitle());
		TextEdit edit = getTextEdit(codeAction);
		assertEquals("\n        default:\n            break;", edit.getNewText());
		codeAction = codeActions.get(1);
		action = codeAction.getRight();
		assertEquals(CodeActionKind.QuickFix, action.getKind());
		assertEquals("Add missing case statements", action.getTitle());
		edit = getTextEdit(codeAction);
		assertEquals("\n        case One:\n            break;\n        default:\n            break;", edit.getNewText());
	}

	@Test
	public void testMissingEnumConstantDespiteDefault() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("   public enum Numbers { One, Two};\n");
		buf.append("    public void testing() {\n");
		buf.append("        Numbers n = Numbers.One;\n");
		buf.append("        switch (n) {\n");
		buf.append("        case Two:\n");
		buf.append("            return;\n");
		buf.append("        default:\n");
		buf.append("            break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		Range range = new Range(new Position(5, 16), new Position(5, 17));
		setIgnoredCommands(ActionMessages.GenerateConstructorsAction_ellipsisLabel, ActionMessages.GenerateConstructorsAction_label);
		List<Either<Command, CodeAction>> codeActions = evaluateCodeActions(cu, range);
		assertEquals(0, codeActions.size());
		Map<String, String> options = fJProject.getOptions(true);
		options.put(JavaCore.COMPILER_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT, JavaCore.ENABLED);
		fJProject.setOptions(options);
		codeActions = evaluateCodeActions(cu, range);
		assertEquals(3, codeActions.size());
		Either<Command, CodeAction> codeAction = codeActions.get(0);
		CodeAction action = codeAction.getRight();
		assertEquals(CodeActionKind.QuickFix, action.getKind());
		assertEquals("Add missing case statements", action.getTitle());
		TextEdit edit = getTextEdit(codeAction);
		assertEquals("\n        case One:", edit.getNewText());
		codeAction = codeActions.get(1);
		action = codeAction.getRight();
		assertEquals(CodeActionKind.QuickFix, action.getKind());
		assertEquals("Insert '//$CASES-OMITTED$'", action.getTitle());
		edit = getTextEdit(codeAction);
		assertEquals("            //$CASES-OMITTED$\n        ", edit.getNewText());
	}

	private TextEdit getTextEdit(Either<Command, CodeAction> codeAction) {
		Command c = codeAction.isLeft() ? codeAction.getLeft() : codeAction.getRight().getCommand();
		Assert.assertEquals(CodeActionHandler.COMMAND_ID_APPLY_EDIT, c.getCommand());
		Assert.assertNotNull(c.getArguments());
		Assert.assertTrue(c.getArguments().get(0) instanceof WorkspaceEdit);
		WorkspaceEdit we = (WorkspaceEdit) c.getArguments().get(0);
		Iterator<Entry<String, List<TextEdit>>> editEntries = we.getChanges().entrySet().iterator();
		Entry<String, List<TextEdit>> entry = editEntries.next();
		TextEdit edit = entry.getValue().get(0);
		return edit;
	}

}
