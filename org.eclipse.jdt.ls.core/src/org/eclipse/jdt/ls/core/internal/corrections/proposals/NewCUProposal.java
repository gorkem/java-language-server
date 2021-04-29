/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Originally copied from org.eclipse.jdt.internal.ui.text.correction.proposals.NewCUUsingWizardProposal
 *
 * Contributors:
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt;
 *     IBM Corporation - updates
 *     Microsoft Corporation - copy and modify to decouple from UI
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.corrections.proposals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.fix.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.UnimplementedCodeFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.nls.changes.CreateFileChange;
import org.eclipse.jdt.ls.core.internal.corrections.CorrectionMessages;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * This proposal is listed in the corrections list for a "type not found"
 * problem. It offers to create a new type.
 *
 * @see UnresolvedElementsSubProcessor#addNewTypeProposals(ICompilationUnit,
 *      Name, int, int, Collection)
 */
public class NewCUProposal extends ChangeCorrectionProposal {

	public static final int K_CLASS = 1;
	public static final int K_INTERFACE = 2;
	public static final int K_ENUM = 3;
	public static final int K_ANNOTATION = 4;

	private Name fNode;
	private ICompilationUnit fCompilationUnit;
	private int fTypeKind;
	private IJavaElement fTypeContainer; // IType or IPackageFragment
	private String fTypeNameWithParameters;
	/**
	 * Construct a new compilation unit proposal.
	 *
	 * @param cu
	 *            current compilation unit.
	 * @param node
	 *            {@link Name} corresponding to the compilation unit to be created.
	 * @param typeKind
	 *            possible values: { K_CLASS, K_INTERFACE, K_ENUM, K_ANNOTATION }
	 * @param typeContainer
	 *            enclosing {@link IJavaElement} of the target compilation unit, can
	 *            be {@link IType} or {@link IPackageFragment}.
	 * @param relevance
	 *            the relevance of this proposal
	 */
	public NewCUProposal(ICompilationUnit cu, Name node, int typeKind, IJavaElement typeContainer, int relevance) {
		super("", CodeActionKind.QuickFix, null, relevance); //$NON-NLS-1$

		fCompilationUnit = cu;
		fNode = node;
		fTypeKind = typeKind;
		fTypeContainer = typeContainer;
		if (fNode != null) {
			fTypeNameWithParameters = getTypeName(typeKind, node);
		}

		setDisplayName();
	}

	private void setDisplayName() {
		String containerName;
		if (fNode != null) {
			containerName = ASTNodes.getQualifier(fNode);
		} else {
			if (fTypeContainer instanceof IPackageFragment) {
				containerName = ((IPackageFragment) fTypeContainer).getElementName();
			} else {
				containerName = ""; //$NON-NLS-1$
			}
		}
		String typeName = fTypeNameWithParameters;
		String containerLabel = BasicElementLabels.getJavaElementName(containerName);
		String typeLabel = BasicElementLabels.getJavaElementName(typeName);
		boolean isInnerType = fTypeContainer instanceof IType;
		switch (fTypeKind) {
			case K_CLASS:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewclass_inpackage_description, containerLabel));
				}
				break;
			case K_INTERFACE:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinterface_inpackage_description, containerLabel));
				}
				break;
			case K_ENUM:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewenum_inpackage_description, containerLabel));
				}
				break;
			case K_ANNOTATION:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewannotation_inpackage_description, containerLabel));
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown type kind"); //$NON-NLS-1$
		}
	}

	private static boolean isParameterizedType(int typeKind, Name node) {
		if (typeKind == K_CLASS || typeKind == K_INTERFACE) {
			return node.getParent().getLocationInParent() == ParameterizedType.TYPE_PROPERTY;
		}
		return false;
	}

	private static String getTypeName(int typeKind, Name node) {
		String name = ASTNodes.getSimpleNameIdentifier(node);

		if (isParameterizedType(typeKind, node)) {
			ASTNode parent = node.getParent();
			String typeArgBaseName = getGenericTypeArgBaseName(name);
			int nTypeArgs = ((ParameterizedType) parent.getParent()).typeArguments().size();
			StringBuilder buf = new StringBuilder(name);
			buf.append('<');
			if (nTypeArgs == 1) {
				buf.append(typeArgBaseName);
			} else {
				for (int i = 0; i < nTypeArgs; i++) {
					if (i != 0) {
						buf.append(", "); //$NON-NLS-1$
					}
					buf.append(typeArgBaseName).append(i + 1);
				}
			}
			buf.append('>');
			return buf.toString();
		}
		return name;
	}

	private TypeDeclaration findEnclosingTypeDeclaration(ASTNode node, String typeName) {
		Iterator<ASTNode> iter;
		if (node instanceof CompilationUnit) {
			iter = ((CompilationUnit) node).types().iterator();
		} else if (node instanceof TypeDeclaration) {
			if (Objects.equals(typeName, ((TypeDeclaration) node).getName().toString())) {
				return (TypeDeclaration) node;
			}
			iter = ((TypeDeclaration) node).bodyDeclarations().iterator();
		} else {
			return null;
		}

		while (iter.hasNext()) {
			TypeDeclaration decl = findEnclosingTypeDeclaration(iter.next(), typeName);
			if (decl != null) {
				return decl;
			}
		}
		return null;
	}

	@Override
	protected Change createChange() throws CoreException {
		IType targetType;
		if (fTypeContainer instanceof IType) {
			IType enclosingType = (IType) fTypeContainer;
			ICompilationUnit parentCU = enclosingType.getCompilationUnit();

			CompilationUnitChange cuChange = new CompilationUnitChange(fName, parentCU);
			TextEdit edit = constructEnclosingTypeEdit(parentCU);
			cuChange.setEdit(edit);
			return cuChange;
		} else if (fTypeContainer instanceof IPackageFragment && ((IPackageFragment) fTypeContainer).getKind() == IPackageFragmentRoot.K_SOURCE) {
			String name = ASTNodes.getSimpleNameIdentifier(fNode);
			ICompilationUnit parentCU = ((IPackageFragment) fTypeContainer).getCompilationUnit(getCompilationUnitName(name));
			targetType = parentCU.getType(name);
			CompositeChange change = new CompositeChange(fName);
			change.add(new CreateFileChange(targetType.getResource().getRawLocation(), "", ""));
			change.add(constructNewCUChange(parentCU));

			return change;
		} else {
			return null;
		}
	}

	private TextEdit constructEnclosingTypeEdit(ICompilationUnit icu) throws CoreException {
		ASTParser astParser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		astParser.setSource(icu);
		CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
		TypeDeclaration enclosingDecl = findEnclosingTypeDeclaration(cu, fTypeContainer.getElementName());
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		final AbstractTypeDeclaration newDeclaration;
		switch (fTypeKind) {
			case K_CLASS:
				newDeclaration = ast.newTypeDeclaration();
				((TypeDeclaration) newDeclaration).setInterface(false);
				break;
			case K_INTERFACE:
				newDeclaration = ast.newTypeDeclaration();
				((TypeDeclaration) newDeclaration).setInterface(true);
				break;
			case K_ENUM:
				newDeclaration = ast.newEnumDeclaration();
				break;
			case K_ANNOTATION:
				newDeclaration = ast.newAnnotationTypeDeclaration();
				break;
			default:
				return null;
		}
		newDeclaration.setJavadoc(null);
		newDeclaration.setName(ast.newSimpleName(ASTNodes.getSimpleNameIdentifier(fNode)));
		newDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		if (isParameterizedType(fTypeKind, fNode)) {
			addTypeParameters((TypeDeclaration) newDeclaration);
		}

		ListRewrite lrw = rewrite.getListRewrite(enclosingDecl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		lrw.insertLast(newDeclaration, null);
		return rewrite.rewriteAST();
	}

	private void addTypeParameters(TypeDeclaration newDeclaration) {
		if (isParameterizedType(fTypeKind, fNode)) {
			String typeArgBaseName = getGenericTypeArgBaseName(ASTNodes.getSimpleNameIdentifier(fNode));
			int nTypeArgs = ((ParameterizedType) fNode.getParent().getParent()).typeArguments().size();
			String[] typeArgNames = new String[nTypeArgs];
			if (nTypeArgs == 1) {
				typeArgNames[0] = typeArgBaseName;
			} else {
				for (int i = 0; i < nTypeArgs; i++) {
					StringBuilder buf = new StringBuilder(typeArgBaseName);
					buf.append(i + 1);
					typeArgNames[i] = buf.toString();
				}
			}

			AST ast = newDeclaration.getAST();
			for (String typeArgName : typeArgNames) {
				TypeParameter typeArg = ast.newTypeParameter();
				typeArg.setName(ast.newSimpleName(typeArgName));
				newDeclaration.typeParameters().add(typeArg);
			}
		}

	}

	private static String getGenericTypeArgBaseName(String typeName) {
		return typeName.startsWith(String.valueOf('T')) ? String.valueOf('S') : String.valueOf('T'); // use 'S' or 'T'
	}

	private CompilationUnitChange constructNewCUChange(ICompilationUnit cu) throws CoreException {
		String lineDelimiter = StubUtility.getLineDelimiterUsed(fCompilationUnit.getJavaProject());
		String typeStub = constructTypeStub(cu, fTypeNameWithParameters, Flags.AccPublic, lineDelimiter);
		String cuContent = constructCUContent(cu, typeStub, lineDelimiter);

		IType cuType = fCompilationUnit.findPrimaryType();
		String[] permittedNames = cuType.getPermittedSubtypeNames();
		boolean isPermitted = Arrays.asList(permittedNames).stream().anyMatch(p -> fTypeNameWithParameters.equals(p));

		if (isPermitted) {
			cu.createType(typeStub, null, false, null);
			ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setSource(cu);
			parser.setResolveBindings(true);
			CompilationUnit cuRoot = (CompilationUnit) parser.createAST(null);

			AddUnimplementedMethodsOperation operation = new AddUnimplementedMethodsOperation((ASTNode) cuRoot.types().get(0));
			if (operation.getMethodsToImplement().length > 0) {
				IProposableFix fix = new UnimplementedCodeFixCore(CorrectionMessages.UnimplementedMethodsCorrectionProposal_description, cuRoot, new CompilationUnitRewriteOperation[] { operation });
				return fix.createChange(null);
			}
		}

		CompilationUnitChange cuChange = new CompilationUnitChange("", cu);
		cuChange.setEdit(new InsertEdit(0, cuContent));
		return cuChange;
	}

	// fileComment/typeComment not enabled.
	private String constructCUContent(ICompilationUnit cu, String typeContent, String lineDelimiter) throws CoreException {
		//		String fileComment = getFileComment(cu, lineDelimiter);
		//		String typeComment = getTypeComment(cu, lineDelimiter);
		IPackageFragment pack = (IPackageFragment) cu.getParent();
		String content = CodeGeneration.getCompilationUnitContent(cu, null/*fileComment*/, null/*typeComment*/, typeContent, lineDelimiter);
		if (content != null) {

			ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setProject(cu.getJavaProject());
			parser.setSource(content.toCharArray());
			CompilationUnit unit = (CompilationUnit) parser.createAST(null);
			if ((pack.isDefaultPackage() || unit.getPackage() != null) && !unit.types().isEmpty()) {
				return content;
			}
		}
		StringBuilder buf = new StringBuilder();
		if (!pack.isDefaultPackage()) {
			buf.append("package ").append(pack.getElementName()).append(';'); //$NON-NLS-1$
		}
		buf.append(lineDelimiter).append(lineDelimiter);
		//		if (typeComment != null) {
		//			buf.append(typeComment).append(lineDelimiter);
		//		}
		buf.append(typeContent);
		return buf.toString();
	}

	/*
	 * Called from createType to construct the source for this type
	 */
	private String constructTypeStub(ICompilationUnit parentCU, String name, int modifiers, String lineDelimiter) throws CoreException {
		StringBuilder buf = new StringBuilder();

		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}

		IType cuType = fCompilationUnit.findPrimaryType();
		String[] permittedNames = cuType.getPermittedSubtypeNames();
		boolean isPermitted = Arrays.asList(permittedNames).stream().anyMatch(p -> name.equals(p));
		if (isPermitted) {
			buf.append("final ");
		}

		String type = ""; //$NON-NLS-1$
		String templateID = ""; //$NON-NLS-1$
		String superType = ""; //$NON-NLS-1$
		switch (fTypeKind) {
			case K_CLASS:
				type = "class "; //$NON-NLS-1$
				templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;
				superType = cuType.isInterface() ? "implements " : "extends ";
				break;
			case K_INTERFACE:
				type = "interface "; //$NON-NLS-1$
				templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
				superType = "extends ";
				break;
			case K_ENUM:
				type = "enum "; //$NON-NLS-1$
				templateID = CodeGeneration.ENUM_BODY_TEMPLATE_ID;
				break;
			case K_ANNOTATION:
				type = "@interface "; //$NON-NLS-1$
				templateID = CodeGeneration.ANNOTATION_BODY_TEMPLATE_ID;
				break;
		}
		buf.append(type);
		buf.append(name);
		if (isPermitted) {
			buf.append(' ');
			buf.append(superType);
			buf.append(cuType.getElementName());
		}

		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		String typeBody = CodeGeneration.getTypeBody(templateID, parentCU, name, lineDelimiter);
		if (typeBody != null) {
			buf.append(typeBody);
		} else {
			buf.append(lineDelimiter);
		}
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}

	private static String getSimpleName(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().getIdentifier();
		} else {
			return ((SimpleName) name).getIdentifier();
		}
	}

	private static String getCompilationUnitName(String typeName) {
		return typeName + JavaModelUtil.DEFAULT_CU_SUFFIX;
	}

}
