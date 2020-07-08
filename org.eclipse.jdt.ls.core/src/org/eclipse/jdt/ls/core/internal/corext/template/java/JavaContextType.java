/*******************************************************************************
 * Copyright (c) 2019 Microsoft Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Microsoft Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.core.internal.corext.template.java;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.corext.template.java.AbstractJavaContextTypeCore;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.IJavaContext;
import org.eclipse.jdt.internal.corext.template.java.JavaContextCore;
import org.eclipse.jdt.internal.corext.template.java.LocalVarResolver;
import org.eclipse.jdt.internal.corext.template.java.VarResolver;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

/**
 * The context type for templates inside Java code. The same class is used for
 * several context types:
 * <dl>
 * <li>templates for all Java code locations</li>
 * <li>templates for member locations</li>
 * <li>templates for statement locations</li>
 * <li>templates for module-info.java files</li>
 * </dl>
 *
 * @see CodeSnippetTemplate
 */

@SuppressWarnings("restriction")
public class JavaContextType extends AbstractJavaContextTypeCore {

	/**
	 * The context type id for templates working on all Java code locations
	 */
	public static final String ID_ALL = "java"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on member locations
	 */
	public static final String ID_MEMBERS = "java-members"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on statement locations
	 */
	public static final String ID_STATEMENTS = "java-statements"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on module-info.java files
	 */
	public static final String ID_MODULE = "module"; //$NON-NLS-1$

	public JavaContextType() {
		setId(ID_STATEMENTS);
		setName(ID_STATEMENTS);
	}

	@Override
	protected void initializeContext(IJavaContext context) {
		// Separate 'module' context type from 'java' context type
		if (getId().equals(ID_MODULE)) {
			return;
		}
		if (!getId().equals(JavaContextType.ID_ALL)) { // a specific context must also allow the templates that work everywhere
			context.addCompatibleContextType(JavaContextType.ID_ALL);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.AbstractJavaContextTypeCore#initializeContextTypeResolvers()
	 */
	@Override
	public void initializeContextTypeResolvers() {
		super.initializeContextTypeResolvers();
		// TODO: Some of the resolvers are defined in org.eclipse.jdt.ui/plugin.xml, now we have to add them manually.
		// See: https://github.com/eclipse/eclipse.jdt.ui/blob/cf6c42522ee5a5ea21a34fcfdecf3504d4750a04/org.eclipse.jdt.ui/plugin.xml#L5619-L5625
		addResolver("var", new VarResolver());
		addResolver("localVar", new LocalVarResolver());
		addResolver("formattedLocalVar", new FormattedLocalVarResolver());
		addResolver("enumerated_method_arguments", new EnumeratedMethodArgumentsResolver());
	}

	public synchronized void addResolver(String type, TemplateVariableResolver resolver) {
		resolver.setType(type);
		addResolver(resolver);
	}

	/**
	 * Resolves ${enumerated_method_arguments} variable into:
	 *
	 * <pre>
	 * "arg1="+arg1+", arg2="+arg2+...+", argN="+argN
	 * </pre>
	 */
	static class EnumeratedMethodArgumentsResolver extends TemplateVariableResolver {

		@Override
		protected String resolve(TemplateContext context) {
			if (!(context instanceof CompilationUnitContext)) {
				return null;
			}
			IJavaElement element = ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element != null) {
				try {
					return ArgumentsHelper.format(((IMethod) element).getParameterNames());
				} catch (Exception O_o) {
					// ¯\_(ツ)_/¯
				}
			}
			return null;
		}
	}

	static class FormattedLocalVarResolver extends TemplateVariableResolver {

		@Override
		public String resolve(TemplateContext context) {
			String[] latestVariable = new String[1];
			if (context instanceof JavaContextCore) {
				JavaContextCore jc = (JavaContextCore) context;
				try {
					jc.getCompilationUnit().codeComplete(jc.getStart(), new CompletionRequestor() {
						@Override
						public void accept(CompletionProposal proposal) {
							if (CompletionProposal.LOCAL_VARIABLE_REF == proposal.getKind()) {
								latestVariable[0] = new String(proposal.getName());
							}
						}
					});
				} catch (Exception O_o) {
					// ¯\_(ツ)_/¯
				}
			}

			return ArgumentsHelper.format(latestVariable);
		}
	}
}
