/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from /org.eclipse.jdt.ui.tests/ui/org/eclipse/jdt/ui/tests/quickfix/UnresolvedTypesQuickFixTest.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.correction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ls.core.internal.JavaProjectHelper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class UnresolvedTypesQuickFixTest extends AbstractQuickFixTest {

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setup() throws Exception {
		fJProject1 = newEmptyProject();
		Hashtable<String, String> options = TestOptions.getDefaultOptions();
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);

		fJProject1.setOptions(options);

		fSourceFolder = fJProject1.getPackageFragmentRoot(fJProject1.getProject().getFolder("src"));
	}

	@Test
	public void testTypeInFieldDecl() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vector1 vec;\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector vec;\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'Vector' (java.util)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public class Vector1 {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface Vector1 {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public enum Vector1 {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<Vector1> {\n");
		buf.append("    Vector1 vec;\n");
		buf.append("}\n");
		Expected e5 = new Expected("Add type parameter 'Vector1' to 'E'", buf.toString());

		assertCodeActions(cu, e1, e5);
	}

	@Test
	public void testTypeInMethodArguments() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'Vector' (java.util)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public class Vect1or {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface Vect1or {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public enum Vect1or {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<Vect1or> {\n");
		buf.append("    void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e5 = new Expected("Add type parameter 'Vect1or' to 'E'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    <Vect1or> void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e6 = new Expected("Add type parameter 'Vect1or' to 'foo(Vect1or[])'", buf.toString());

		assertCodeActions(cu, e1, e5, e6);
	}

	@Test
	public void testTypeInMethodReturnType() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vect1or[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'Vector' (java.util)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public class Vect1or {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface Vect1or {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public enum Vect1or {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<Vect1or> {\n");
		buf.append("    Vect1or[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e5 = new Expected("Add type parameter 'Vect1or' to 'E'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    <Vect1or> Vect1or[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e6 = new Expected("Add type parameter 'Vect1or' to 'foo()'", buf.toString());

		assertCodeActions(cu, e1, e5, e6);
	}

	@Test
	@Ignore("Create type")
	public void testTypeInExceptionType() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws IOExcpetion {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class IOExcpetion extends Exception {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e2 = new Expected("Add all missing tags", buf.toString());
		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testTypeInVarDeclWithWildcard() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        XY v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        Runnable v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'Runnable' (java.lang)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public class XY {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface XY {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public enum XY {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E<XY> {\n");
		buf.append("    void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        XY v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e2 = new Expected("Add type parameter 'XY' to 'E'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    <XY> void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        XY v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e3 = new Expected("Add type parameter 'XY' to 'foo(ArrayList<? extends Runnable>)'", buf.toString());

		assertCodeActions(cu, e1, e2, e3);
	}

	@Test
	public void testTypeInStatement() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        ArrayList v= new ArrayListist();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        ArrayList v= new ArrayList();\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'ArrayList' (java.util)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("import java.util.ArrayList;\n");
		// buf.append("\n");
		// buf.append("public class ArrayListist extends ArrayList {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Create loop variable 'element'",
		// buf.toString());
		assertCodeActions(cu, e1);
	}

	@Test
	public void testArrayTypeInStatement() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		List<Expected> expected = new ArrayList<>(5);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new Serializable[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected.add(new Expected("Change to 'Serializable' (java.io)", buf.toString()));

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayList[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected.add(new Expected("Change to 'ArrayList' (java.util)", buf.toString()));

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("import java.io.Serializable;\n");
		// buf.append("\n");
		// buf.append("public class ArrayListExtra implements Serializable
		// {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("import java.io.Serializable;\n");
		// buf.append("\n");
		// buf.append("public interface ArrayListExtra extends Serializable
		// {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E<ArrayListExtra> {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected.add(new Expected("Add type parameter 'ArrayListExtra' to 'E'", buf.toString()));

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    <ArrayListExtra> void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected.add(new Expected("Add type parameter 'ArrayListExtra' to 'foo()'", buf.toString()));

		assertCodeActions(cu, expected);
	}

	@Test
	public void testQualifiedType() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        test2.Test t= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create class 'Test' in package 'test2'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public interface Test {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e2 = new Expected("Create interface 'Test' in package 'test2'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public enum Test {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e3 = new Expected("Create enum 'Test' in package 'test2'", buf.toString());

		assertCodeActions(cu, e1, e2, e3);
	}

	@Test
	public void testInnerType() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object object= new F.Inner() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object object= new Object() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'Object' (java.lang)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("public class F {\n");
		// buf.append("\n");
		// buf.append(" public class Inner {\n");
		// buf.append("\n");
		// buf.append(" }\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("public class F {\n");
		// buf.append("\n");
		// buf.append(" public interface Inner {\n");
		// buf.append("\n");
		// buf.append(" }\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());

		assertCodeActions(cu, e1);
	}

	@Test
	@Ignore("Create type")
	public void testTypeInCatchBlock() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("        } catch (XXX x) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XXX extends Exception {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Add all missing tags", buf.toString());

		assertCodeActionExists(cu, e1);
	}

	@Test
	public void testTypeInSuperType() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E extends XXX {\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XXX {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create class 'XXX'", buf.toString());

		assertCodeActionExists(cu, e1);
	}

	@Test
	public void testTypeInSuperInterface() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface E extends XXX {\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XXX {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create interface 'XXX'", buf.toString());

		assertCodeActionExists(cu, e1);
	}

	@Test
	public void testTypeInAnnotation() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@Xyz\n");
		buf.append("public interface E {\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public @interface Xyz {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create annotation 'Xyz'", buf.toString());

		assertCodeActionExists(cu, e1);
	}

	@Test
	public void testTypeInAnnotation_bug153881() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("a", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package a;\n");
		buf.append("public class SomeClass {\n");
		buf.append("        @scratch.Unimportant void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("SomeClass.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package scratch;\n");
		buf.append("\n");
		buf.append("public @interface Unimportant {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create annotation 'Unimportant' in package 'scratch'", buf.toString());

		assertCodeActions(cu, e1);
	}

	@Test
	public void testPrimitiveTypeInFieldDecl() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    floot vec= 1.0;\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<Expected> expected = new ArrayList<>(7);
		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    double vec= 1.0;\n");
		buf.append("}\n");
		expected.add(new Expected("Change to 'double'", buf.toString()));

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Float vec= 1.0;\n");
		buf.append("}\n");
		expected.add(new Expected("Change to 'Float' (java.lang)", buf.toString()));

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<floot> {\n");
		buf.append("    floot vec= 1.0;\n");
		buf.append("}\n");
		expected.add(new Expected("Add type parameter 'floot' to 'E'", buf.toString()));

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    float vec= 1.0;\n");
		buf.append("}\n");
		expected.add(new Expected("Change to 'float'", buf.toString()));

		assertCodeActions(cu, expected);
	}

	@Test
	public void testTypeInTypeArguments1() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<XYX> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<SomeType> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'SomeType' (test1.E)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public class XYX {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface XYX {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public enum XYX {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T, XYX> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<XYX> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e5 = new Expected("Add type parameter 'XYX' to 'E<T>'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    <XYX> void foo() {\n");
		buf.append("        E<XYX> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e6 = new Expected("Add type parameter 'XYX' to 'foo()'", buf.toString());

		assertCodeActions(cu, e1, e5, e6);
	}

	@Test
	public void testTypeInTypeArguments2() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<Map<String, ? extends SomeType>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change to 'SomeType' (test1.E)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("import test1.E.SomeType;\n");
		// buf.append("\n");
		// buf.append("public class XYX extends SomeType {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e2 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface XYX {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add all missing tags", buf.toString());
		//
		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public enum XYX {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e4 = new Expected("Add all missing tags", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T, XYX> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e5 = new Expected("Add type parameter 'XYX' to 'E<T>'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    <XYX> void foo() {\n");
		buf.append("        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e6 = new Expected("Add type parameter 'XYX' to 'foo()'", buf.toString());

		assertCodeActions(cu, e1, e5, e6);
	}

	@Test
	public void testParameterizedType1() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(XXY<String> b) {\n");
		buf.append("        b.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XXY<T> {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create class 'XXY<T>'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XXY<T> {\n");
		buf.append("\n");
		buf.append("}\n");
		Expected e2 = new Expected("Create interface 'XXY<T>'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testParameterizedType2() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType<S1, S2> { }\n");
		buf.append("    void foo() {\n");
		buf.append("        SomeType<String, String> list= new XXY<String, String>() { };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType<S1, S2> { }\n");
		buf.append("    void foo() {\n");
		buf.append("        XXY<String, String> list= new XXY<String, String>() { };\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Change type of 'list' to 'XXY<String, String>'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType<S1, S2> { }\n");
		buf.append("    void foo() {\n");
		buf.append("        SomeType<String, String> list= new SomeType<String, String>() { };\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e2 = new Expected("Change to 'SomeType' (test1.E)", buf.toString());

		// buf = new StringBuilder();
		// buf.append("package test1;\n");
		// buf.append("\n");
		// buf.append("public interface XXY<T1, T2> {\n");
		// buf.append("\n");
		// buf.append("}\n");
		// Expected e3 = new Expected("Add Javadoc comment", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	private void createSomeAmbiguity(boolean ifc, boolean isException) throws Exception {

		IPackageFragment pack3 = fSourceFolder.createPackageFragment("test3", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test3;\n");
		buf.append("public ");
		buf.append(ifc ? "interface" : "class");
		buf.append(" A ");
		buf.append(isException ? "extends Exception " : "");
		buf.append("{\n");
		buf.append("}\n");
		pack3.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test3;\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		pack3.createCompilationUnit("B.java", buf.toString(), false, null);

		IPackageFragment pack2 = fSourceFolder.createPackageFragment("test2", false, null);
		buf = new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public ");
		buf.append(ifc ? "interface" : "class");
		buf.append(" A ");
		buf.append(isException ? "extends Exception " : "");
		buf.append("{\n");
		buf.append("}\n");
		pack2.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);
	}

	@Test
	public void testAmbiguousTypeInSuperClass() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testAmbiguousTypeInInterface() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testAmbiguousTypeInField() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testAmbiguousTypeInArgument() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testAmbiguousTypeInReturnType() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public A foo() {");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public A foo() {");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public A foo() {");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testAmbiguousTypeInExceptionType() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	@Test
	public void testAmbiguousTypeInCatchBlock() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Explicitly import 'test2.A'", buf.toString());

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e2 = new Expected("Explicitly import 'test3.A'", buf.toString());

		assertCodeActions(cu, e1, e2);
	}

	/**
	 * Offers to raise visibility of method instead of class.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=94755
	 *
	 * @throws Exception
	 *             if anything goes wrong
	 * @since 3.9
	 */
	@Test
	@Ignore("Requires Modifier proposals")
	public void testIndirectRefDefaultClass() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2 = fSourceFolder.createPackageFragment("test2", false, null);

		StringBuilder buf = new StringBuilder();

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class B {\n");
		buf.append("    public Object get(Object c) {\n");
		buf.append("    	return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    B b = new B();\n");
		buf.append("    public B getB() {\n");
		buf.append("    	return b;\n");
		buf.append("    }\n");
		buf.append("}\n");
		cu = pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.A;\n");
		buf.append("public class C {\n");
		buf.append("    public Object getSide(A a) {\n");
		buf.append("    	return a.getB().get(this);\n");
		buf.append("    }\n");
		buf.append("}\n");
		cu = pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class B {\n");
		buf.append("    public Object get(Object c) {\n");
		buf.append("    	return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Add all missing tags", buf.toString());

		assertCodeActionExists(cu, e1);
	}

	@Test
	public void testForEachMissingType() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("pack", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.util.*;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(ArrayList<? extends HashSet<? super Integer>> list) {\n");
		buf.append("        for (element: list) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.util.*;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(ArrayList<? extends HashSet<? super Integer>> list) {\n");
		buf.append("        for (HashSet<? super Integer> element: list) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		Expected e1 = new Expected("Create loop variable 'element'", buf.toString());

		assertCodeActionExists(cu, e1);
	}

	@Test
	public void testDontImportTestClassesInMainCode() throws Exception {
		IPackageFragmentRoot testSourceFolder = JavaProjectHelper.addSourceContainer(fJProject1, "src-tests", new Path[0], new Path[0], "bin-tests",
				new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true") });

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("pp", false, null);
		StringBuilder buf1 = new StringBuilder();
		buf1.append("package pp;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    Tests at=new Tests();\n");
		buf1.append("}\n");
		ICompilationUnit cu1 = pack1.createCompilationUnit("C1.java", buf1.toString(), false, null);

		IPackageFragment pack2 = testSourceFolder.createPackageFragment("pt", false, null);
		StringBuilder buf2 = new StringBuilder();
		buf2.append("package pt;\n");
		buf2.append("public class Tests {\n");
		buf2.append("}\n");
		pack2.createCompilationUnit("Tests.java", buf2.toString(), false, null);

		assertCodeActionNotExists(cu1, "Import 'Tests' (pt)");
	}

	@Test
	public void testTypeInSealedTypeDeclaration() throws Exception {
		Map<String, String> options15 = new HashMap<>();
		JavaModelUtil.setComplianceOptions(options15, JavaCore.VERSION_15);
		options15.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
		options15.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.IGNORE);
		fJProject1.setOptions(options15);

		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public sealed interface E permits F {\n");
		buf.append("void methodE();\n");
		buf.append("}");
		ICompilationUnit cu = pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public final class F implements E {\n");
		buf.append("\n");
		buf.append("\t@Override\n");
		buf.append("\tpublic void methodE() {\n");
		buf.append("\t\t// TODO Auto-generated method stub\n");
		buf.append("\t\t\n");
		buf.append("\t}\n");
		buf.append("\n");
		buf.append("}\n");
		buf.append("\n");
		Expected e1 = new Expected("Create class 'F'", buf.toString());

		assertCodeActions(cu, e1);
	}

}