/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
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
package org.eclipse.jdt.ls.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.changes.RenamePackageChange;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.nls.changes.CreateFileChange;
import org.eclipse.jdt.ls.core.internal.corext.util.JavaElementUtil;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.CreateFileOptions;
import org.eclipse.lsp4j.DeleteFile;
import org.eclipse.lsp4j.DeleteFileOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameFile;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Utility methods for converting Refactoring changes.
 *
 * @author Valeriy Svydenko
 *
 */
public class ChangeUtil {

	private static final String TEMP_FILE_NAME = ".temp";
	private static final Range ZERO_RANGE = new Range(new Position(), new Position());

	/**
	 * Converts Change to WorkspaceEdit for further consumption.
	 *
	 * @param change
	 *            {@link Change} to convert
	 * @return {@link WorkspaceEdit} converted from the change
	 * @throws CoreException
	 */
	public static WorkspaceEdit convertToWorkspaceEdit(Change change) throws CoreException {
		WorkspaceEdit edit = new WorkspaceEdit();
		if (change instanceof CompositeChange) {
			convertCompositeChange((CompositeChange) change, edit);
		} else {
			convertSingleChange(change, edit);
		}
		return edit;
	}

	private static void convertSingleChange(Change change, WorkspaceEdit edit) throws CoreException {
		if (change instanceof CompositeChange) {
			return;
		}

		if (change instanceof TextChange) {
			convertTextChange((TextChange) change, edit);
		} else if (change instanceof ResourceChange) {
			convertResourceChange((ResourceChange) change, edit);
		}
	}

	private static void convertCompositeChange(CompositeChange change, WorkspaceEdit edit) throws CoreException {
		Change[] changes = change.getChildren();
		for (Change ch : changes) {
			if (ch instanceof CompositeChange) {
				convertCompositeChange((CompositeChange) ch, edit);
			} else {
				convertSingleChange(ch, edit);
			}
		}
	}

	/**
	 * Converts changes to resource operations if resource operations are supported
	 * by the client otherwise converts to TextEdit changes.
	 *
	 * @param resourceChange
	 *            resource changes after Refactoring operation
	 * @param edit
	 *            instance of workspace edit changes
	 * @throws CoreException
	 */
	private static void convertResourceChange(ResourceChange resourceChange, WorkspaceEdit edit) throws CoreException {
		if (!JavaLanguageServerPlugin.getPreferencesManager().getClientPreferences().isResourceOperationSupported()) {
			return;
		}

		List<Either<TextDocumentEdit, ResourceOperation>> changes = edit.getDocumentChanges();
		if (changes == null) {
			changes = new ArrayList<>();
			edit.setDocumentChanges(changes);
		}

		// Resource change is needed and supported by client
		if (resourceChange instanceof RenameCompilationUnitChange) {
			convertCUResourceChange(edit, (RenameCompilationUnitChange) resourceChange);
		} else if (resourceChange instanceof RenamePackageChange) {
			convertRenamePackcageChange(edit, (RenamePackageChange) resourceChange);
		} else if (resourceChange instanceof MoveCompilationUnitChange) {
			convertMoveCompilationUnitChange(edit, (MoveCompilationUnitChange) resourceChange);
		} else if (resourceChange instanceof CreateFileChange) {
			convertCreateFileChange(edit, (CreateFileChange) resourceChange);
		} else if (resourceChange instanceof CreateCompilationUnitChange) {
			convertCreateCompilationUnitChange(edit, (CreateCompilationUnitChange) resourceChange);
		}
	}

	private static void convertMoveCompilationUnitChange(WorkspaceEdit edit, MoveCompilationUnitChange change) throws JavaModelException {
		IPackageFragment newPackage = change.getDestinationPackage();
		ICompilationUnit unit = change.getCu();
		CompilationUnit astCU = RefactoringASTParser.parseWithASTProvider(unit, true, new NullProgressMonitor());
		ASTRewrite rewrite = ASTRewrite.create(astCU.getAST());

		IPackageDeclaration[] packDecls = unit.getPackageDeclarations();
		String oldPackageName = packDecls.length > 0 ? packDecls[0].getElementName() : "";
		if (!Objects.equals(oldPackageName, newPackage.getElementName())) {
			// update the package declaration
			if (updatePackageStatement(astCU, newPackage.getElementName(), rewrite, unit)) {
				convertTextEdit(edit, unit, rewrite.rewriteAST());
			}
		}

		RenameFile cuResourceChange = new RenameFile();
		cuResourceChange.setOldUri(JDTUtils.toURI(unit));
		IPath newCUPath = newPackage.getResource().getLocation().append(unit.getPath().lastSegment());
		String newUri = ResourceUtils.fixURI(newCUPath.toFile().toURI());
		cuResourceChange.setNewUri(newUri);
		edit.getDocumentChanges().add(Either.forRight(cuResourceChange));
	}

	private static void convertCreateCompilationUnitChange(WorkspaceEdit edit, CreateCompilationUnitChange change) {
		ICompilationUnit unit = change.getCu();
		CreateFile createFile = new CreateFile();
		createFile.setUri(JDTUtils.toURI(unit));
		createFile.setOptions(new CreateFileOptions(false, true));
		edit.getDocumentChanges().add(Either.forRight(createFile));
		InsertEdit textEdit = new InsertEdit(0, change.getPreview());
		convertTextEdit(edit, unit, textEdit);
	}

	private static void convertRenamePackcageChange(WorkspaceEdit edit, RenamePackageChange packageChange) throws CoreException {
		IPackageFragment pack = (IPackageFragment) packageChange.getModifiedElement();
		IPath newPackageFragment = new Path(packageChange.getNewName().replace('.', IPath.SEPARATOR));
		IPath oldPackageFragment = new Path(packageChange.getOldName().replace('.', IPath.SEPARATOR));
		IPath newPackagePath = pack.getResource().getLocation().removeLastSegments(oldPackageFragment.segmentCount()).append(newPackageFragment);
		if (packageChange.getRenameSubpackages()) {
			IPackageFragment[] allPackages = JavaElementUtil.getPackageAndSubpackages(pack);
			String oldPrefix = packageChange.getOldName();
			for (IPackageFragment currentPackage : allPackages) {
				String newPkgName = packageChange.getNewName() + currentPackage.getElementName().substring(oldPrefix.length());
				//update package's declaration
				convertPackageUpdateEdit(currentPackage.getCompilationUnits(), newPkgName, edit);
			}

			RenameFile renameFile = new RenameFile();
			renameFile.setNewUri(ResourceUtils.fixURI(newPackagePath.toFile().toURI()));
			renameFile.setOldUri(ResourceUtils.fixURI(pack.getResource().getRawLocationURI()));
			edit.getDocumentChanges().add(Either.forRight(renameFile));
		} else {
			//update package's declaration
			convertPackageUpdateEdit(pack.getCompilationUnits(), packageChange.getNewName(), edit);
		
			CreateFile createFile = new CreateFile();
			createFile.setUri(ResourceUtils.fixURI(newPackagePath.append(TEMP_FILE_NAME).toFile().toURI()));
			createFile.setOptions(new CreateFileOptions(false, true));
			edit.getDocumentChanges().add(Either.forRight(createFile));

			for (ICompilationUnit unit : pack.getCompilationUnits()) {
				RenameFile cuResourceChange = new RenameFile();
				cuResourceChange.setOldUri(ResourceUtils.fixURI(unit.getResource().getLocationURI()));
				IPath newCUPath = newPackagePath.append(unit.getPath().lastSegment());
				cuResourceChange.setNewUri(ResourceUtils.fixURI(newCUPath.toFile().toURI()));
				edit.getDocumentChanges().add(Either.forRight(cuResourceChange));
			}

			// Workaround: https://github.com/Microsoft/language-server-protocol/issues/272
			DeleteFile deleteFile = new DeleteFile();
			deleteFile.setUri(ResourceUtils.fixURI(newPackagePath.append(TEMP_FILE_NAME).toFile().toURI()));
			deleteFile.setOptions(new DeleteFileOptions(false, true));
			edit.getDocumentChanges().add(Either.forRight(deleteFile));

		}
	}

	private static void convertCUResourceChange(WorkspaceEdit edit, RenameCompilationUnitChange cuChange) {
		ICompilationUnit modifiedCU = (ICompilationUnit) cuChange.getModifiedElement();
		RenameFile rf = new RenameFile();
		String newCUName = cuChange.getNewName();
		IPath currentPath = modifiedCU.getResource().getLocation();
		rf.setOldUri(ResourceUtils.fixURI(modifiedCU.getResource().getRawLocationURI()));
		IPath newPath = currentPath.removeLastSegments(1).append(newCUName);
		rf.setNewUri(ResourceUtils.fixURI(newPath.toFile().toURI()));
		edit.getDocumentChanges().add(Either.forRight(rf));
	}

	private static void convertCreateFileChange(WorkspaceEdit edit, CreateFileChange createFileChange) {
		CreateFile createFile = new CreateFile();
		createFile.setUri(ResourceUtils.fixURI(createFileChange.getPath().toFile().toURI()));
		createFile.setOptions(new CreateFileOptions(false, true));
		edit.getDocumentChanges().add(Either.forRight(createFile));
	}

	private static void convertTextChange(TextChange textChange, WorkspaceEdit rootEdit) {
		Object modifiedElement = textChange.getModifiedElement();

		TextEdit textEdits = textChange.getEdit();
		if (textEdits == null) {
			return;
		}

		if (!(modifiedElement instanceof IJavaElement)) {
			TextEdit edit = textChange.getEdit();
			if (textChange instanceof TextFileChange && edit instanceof InsertEdit) {
				IFile file = ((TextFileChange) textChange).getFile();
				String fileUri = JDTUtils.getFileURI(file);

				InsertEdit insertEdit = (InsertEdit) edit;
				org.eclipse.lsp4j.TextEdit te = new org.eclipse.lsp4j.TextEdit();
				te.setNewText(insertEdit.getText());
				Position pos = new Position(0, 0);
				te.setRange(new Range(pos, pos));

				if (JavaLanguageServerPlugin.getPreferencesManager().getClientPreferences().isResourceOperationSupported()) {
					List<Either<TextDocumentEdit, ResourceOperation>> changes = rootEdit.getDocumentChanges();
					if (changes == null) {
						changes = new ArrayList<>();
						rootEdit.setDocumentChanges(changes);
					}

					VersionedTextDocumentIdentifier identifier = new VersionedTextDocumentIdentifier(fileUri, null);
					TextDocumentEdit documentEdit = new TextDocumentEdit(identifier, Arrays.asList(te));
					changes.add(Either.forLeft(documentEdit));
				} else {
					Map<String, List<org.eclipse.lsp4j.TextEdit>> changes = rootEdit.getChanges();
					List<org.eclipse.lsp4j.TextEdit> edits = new ArrayList<>();
					edits.add(te);
					changes.put(fileUri, edits);
				}
			}
		} else {
			ICompilationUnit compilationUnit = (ICompilationUnit) ((IJavaElement) modifiedElement).getAncestor(IJavaElement.COMPILATION_UNIT);
			convertTextEdit(rootEdit, compilationUnit, textEdits);
		}
	}

	private static void convertTextEdit(WorkspaceEdit root, ICompilationUnit unit, TextEdit edit) {
		if (edit == null) {
			return;
		}

		TextEditConverter converter = new TextEditConverter(unit, edit);
		List<org.eclipse.lsp4j.TextEdit> textEdits = filterTextEdits(converter.convert());
		if (textEdits == null || textEdits.isEmpty()) {
			return;
		}

		String uri = JDTUtils.toURI(unit);
		if (JavaLanguageServerPlugin.getPreferencesManager().getClientPreferences().isResourceOperationSupported()) {
			List<Either<TextDocumentEdit, ResourceOperation>> changes = root.getDocumentChanges();
			if (changes == null) {
				changes = new ArrayList<>();
				root.setDocumentChanges(changes);
			}

			VersionedTextDocumentIdentifier identifier = new VersionedTextDocumentIdentifier(uri, null);
			TextDocumentEdit documentEdit = new TextDocumentEdit(identifier, textEdits);
			changes.add(Either.forLeft(documentEdit));
		} else {
			Map<String, List<org.eclipse.lsp4j.TextEdit>> changes = root.getChanges();
			if (changes.containsKey(uri)) {
				changes.get(uri).addAll(textEdits);
			} else {
				changes.put(uri, textEdits);
			}
		}
	}

	private static List<org.eclipse.lsp4j.TextEdit> filterTextEdits(List<org.eclipse.lsp4j.TextEdit> textEdits) {
		if (textEdits == null || textEdits.isEmpty()) {
			return textEdits;
		}

		return textEdits.stream().filter(textEdit -> !isEmpty(textEdit)).collect(Collectors.toList());
	}

	private static boolean isEmpty(org.eclipse.lsp4j.TextEdit textEdit) {
		if (textEdit == null || textEdit.getRange() == null) {
			return true;
		}

		Position start = textEdit.getRange().getStart();
		Position end = textEdit.getRange().getEnd();
		return StringUtils.isEmpty(textEdit.getNewText())
			&& start.getCharacter() == end.getCharacter()
			&& start.getLine() == end.getLine();
	}

	private static ICompilationUnit getNewCompilationUnit(IType type, String newName) {
		ICompilationUnit cu = type.getCompilationUnit();
		if (isPrimaryType(type)) {
			IPackageFragment parent = type.getPackageFragment();
			String renamedCUName = JavaModelUtil.getRenamedCUName(cu, newName);
			return parent.getCompilationUnit(renamedCUName);
		} else {
			return cu;
		}
	}

	private static boolean isPrimaryType(IType type) {
		String cuName = type.getCompilationUnit().getElementName();
		String typeName = type.getElementName();
		return type.getDeclaringType() == null && JavaCore.removeJavaLikeExtension(cuName).equals(typeName);
	}

	private static void convertPackageUpdateEdit(ICompilationUnit[] cus, String newPkgName, WorkspaceEdit rootEdit) throws JavaModelException {
		for (ICompilationUnit cu : cus) {
			convertPackageUpdateEdit(cu, newPkgName, rootEdit);
		}
	}

	private static void convertPackageUpdateEdit(ICompilationUnit cu, String newPkgName, WorkspaceEdit rootEdit) throws JavaModelException {
		CompilationUnit unit = new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(cu, true);		
		ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
		if (updatePackageStatement(unit, newPkgName, rewrite, cu)) {
			TextEdit textEdit = rewrite.rewriteAST();
			convertTextEdit(rootEdit, cu, textEdit);
		}
	}

	private static boolean updatePackageStatement(CompilationUnit astCU, String pkgName, ASTRewrite rewriter, ICompilationUnit cu) throws JavaModelException {
		boolean defaultPackage = pkgName.isEmpty();
		AST ast = astCU.getAST();
		if (defaultPackage) {
			// remove existing package statement
			PackageDeclaration pkg = astCU.getPackage();
			if (pkg == null) {
				return false;
			}

			int pkgStart;
			Javadoc javadoc = pkg.getJavadoc();
			if (javadoc != null) {
				pkgStart = javadoc.getStartPosition() + javadoc.getLength() + 1;
			} else {
				pkgStart = pkg.getStartPosition();
			}
			int extendedStart = astCU.getExtendedStartPosition(pkg);
			if (pkgStart != extendedStart) {
				String commentSource = cu.getSource().substring(extendedStart, pkgStart);
				ASTNode comment = rewriter.createStringPlaceholder(commentSource, ASTNode.PACKAGE_DECLARATION);
				rewriter.set(astCU, CompilationUnit.PACKAGE_PROPERTY, comment, null);
			} else {
				rewriter.set(astCU, CompilationUnit.PACKAGE_PROPERTY, null, null);
			}
		} else {
			org.eclipse.jdt.core.dom.PackageDeclaration pkg = astCU.getPackage();
			if (pkg != null) {
				// Skip if the new package name is same as the existing package name.
				if (Objects.equals(pkg.getName().getFullyQualifiedName(), pkgName)) {
					return false;
				}

				// rename package statement
				Name name = ast.newName(pkgName);
				rewriter.set(pkg, PackageDeclaration.NAME_PROPERTY, name, null);
			} else {
				// create new package statement
				pkg = ast.newPackageDeclaration();
				pkg.setName(ast.newName(pkgName));
				rewriter.set(astCU, CompilationUnit.PACKAGE_PROPERTY, pkg, null);
			}
		}

		return true;
	}

	/**
	 * @return <code>true</code> if a {@link WorkspaceEdit} contains any actual
	 *         changes, <code>false</code> otherwise.
	 */
	public static boolean hasChanges(WorkspaceEdit edit) {
		if (edit == null) {
			return false;
		}
		if (edit.getDocumentChanges() != null && !edit.getDocumentChanges().isEmpty()) {
			return true;
		}
		boolean hasChanges = false;
		//@formatter:off
		if ((edit.getChanges() != null && !edit.getChanges().isEmpty())) {
			hasChanges = edit.getChanges().values().stream()
					.filter(changes -> changes != null && !changes.isEmpty() && hasChanges(changes))
					.findFirst()
					.isPresent();
		}
		//@formatter:on
		return hasChanges;
	}

	/**
	 * @return <code>true</code> if a list of {@link org.eclipse.lsp4j.TextEdit}
	 *         contains any actual changes, <code>false</code> otherwise.
	 */
	public static boolean hasChanges(List<org.eclipse.lsp4j.TextEdit> edits) {
		if (edits == null) {
			return false;
		}
		//@formatter:off
		return edits.stream()
				.filter(edit -> (!edit.getRange().equals(ZERO_RANGE) || !"".equals(edit.getNewText())))
				.findFirst()
				.isPresent();
		//@formatter:on
	}

	/**
	 * Merge the changes of two workspace edits to a new edit.
	 */
	public static WorkspaceEdit mergeChanges(WorkspaceEdit editA, WorkspaceEdit editB) {
		return mergeChanges(editA, editB, false);
	}

	/**
	 * Merge the changes of two workspace edits to a new edit.
	 */
	public static WorkspaceEdit mergeChanges(WorkspaceEdit editA, WorkspaceEdit editB, boolean ignoreResourceChange) {
		if (editA == null && editB == null) {
			return null;
		}

		WorkspaceEdit result = new WorkspaceEdit();
		appendChanges(result, editA, ignoreResourceChange);
		appendChanges(result, editB, ignoreResourceChange);
		return result;
	}

	private static void appendChanges(WorkspaceEdit root, WorkspaceEdit child, boolean ignoreResourceChange) {
		if (root == null || child == null) {
			return;
		}

		if (child.getChanges() != null && !child.getChanges().isEmpty()) {
			if (root.getChanges() == null) {
				root.setChanges(new LinkedHashMap<>());
			}

			for (Entry<String, List<org.eclipse.lsp4j.TextEdit>> entry : child.getChanges().entrySet()) {
				root.getChanges().computeIfAbsent(entry.getKey(), (key -> new ArrayList<>()));
				root.getChanges().get(entry.getKey()).addAll(entry.getValue());
			}
		}

		if (child.getDocumentChanges() != null && !child.getDocumentChanges().isEmpty()) {
			if (root.getDocumentChanges() == null) {
				root.setDocumentChanges(new ArrayList<>());
			}

			if (ignoreResourceChange) {
				root.getDocumentChanges().addAll(
					child.getDocumentChanges().stream().filter((change) -> change.isLeft()).collect(Collectors.toList())
				);
			} else {
				root.getDocumentChanges().addAll(child.getDocumentChanges());
			}
		}
	}
}
