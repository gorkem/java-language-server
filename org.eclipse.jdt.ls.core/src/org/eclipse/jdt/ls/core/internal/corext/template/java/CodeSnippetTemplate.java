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

import org.eclipse.jface.text.templates.Template;

public enum CodeSnippetTemplate {

	//@formatter:off
	SYSOUT(TemplatePreferences.SYSOUT_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.SYSOUT_CONTENT, TemplatePreferences.SYSOUT_DESCRIPTION),
	SYSERR(TemplatePreferences.SYSERR_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.SYSERR_CONTENT, TemplatePreferences.SYSERR_DESCRIPTION),
	SYSTRACE(TemplatePreferences.SYSTRACE_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.SYSTRACE_CONTENT, TemplatePreferences.SYSTRACE_DESCRIPTION),
	SYSOUTV(TemplatePreferences.SYSOUTV_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.SYSOUTV_CONTENT, TemplatePreferences.SYSOUTV_DESCRIPTION),
	SYSOUTP(TemplatePreferences.SYSOUTP_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.SYSOUTP_CONTENT, TemplatePreferences.SYSOUTP_DESCRIPTION),
	FOREACH(TemplatePreferences.FOREACH_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.FOREACH_CONTENT, TemplatePreferences.FOREACH_DESCRIPTION),
	FORI(TemplatePreferences.FORI_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.FORI_CONTENT, TemplatePreferences.FORI_DESCRIPTION),
	WHILE(TemplatePreferences.WHILE_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.WHILE_CONTENT, TemplatePreferences.WHILE_DESCRIPTION),
	DOWHILE(TemplatePreferences.DOWHILE_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.DOWHILE_CONTENT, TemplatePreferences.DOWHILE_DESCRIPTION),
	IF(TemplatePreferences.IF_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.IF_CONTENT, TemplatePreferences.IF_DESCRIPTION),
	IFELSE(TemplatePreferences.IFELSE_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.IFELSE_CONTENT, TemplatePreferences.IFELSE_DESCRIPTION),
	IFNULL(TemplatePreferences.IFNULL_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.IFNULL_CONTENT, TemplatePreferences.IFNULL_DESCRIPTION),
	IFNOTNULL(TemplatePreferences.IFNOTNULL_ID, JavaContextType.ID_STATEMENTS, TemplatePreferences.IFNOTNULL_CONTENT, TemplatePreferences.IFNOTNULL_DESCRIPTION);
	//@formatter:on

	private final String templateId;
	private final String contextType;
	private final String defaultContent;
	private final String description;

	private CodeSnippetTemplate(String templatesId, String contextType, String defaultContent, String description) {
		this.templateId = templatesId;
		this.contextType = contextType;
		this.defaultContent = defaultContent;
		this.description = description;
	}

	public Template createTemplate() {
		return new Template(this.name().toLowerCase(), this.description, this.contextType, this.defaultContent, false);
	}

	public String getId() {
		return this.templateId;
	}
}

class TemplatePreferences {
	// IDs
	public static final String SYSOUT_ID = "org.eclipse.jdt.ls.templates.sysout";
	public static final String SYSERR_ID = "org.eclipse.jdt.ls.templates.syserr";
	public static final String SYSTRACE_ID = "org.eclipse.jdt.ls.templates.systrace";
	public static final String SYSOUTV_ID = "org.eclipse.jdt.ls.templates.sysoutv";
	public static final String SYSOUTP_ID = "org.eclipse.jdt.ls.templates.sysoutp";
	public static final String FOREACH_ID = "org.eclipse.jdt.ls.templates.for_array";
	public static final String FORI_ID = "org.eclipse.jdt.ls.templates.for_iterable";
	public static final String WHILE_ID = "org.eclipse.jdt.ls.templates.while_condition";
	public static final String DOWHILE_ID = "org.eclipse.jdt.ls.templates.do";
	public static final String IF_ID = "org.eclipse.jdt.ls.templates.if";
	public static final String IFELSE_ID = "org.eclipse.jdt.ls.templates.ifelse";
	public static final String IFNULL_ID = "org.eclipse.jdt.ls.templates.ifnull";
	public static final String IFNOTNULL_ID = "org.eclipse.jdt.ls.templates.ifnotnull";

	// DefaultContents
	public static final String SYSOUT_CONTENT = "System.out.println($${0});";
	public static final String SYSERR_CONTENT = "System.err.println($${0});";
	public static final String SYSTRACE_CONTENT = "System.out.println(\"${enclosing_type}.${enclosing_method}()\");";
	public static final String SYSOUTV_CONTENT = "System.out.println(${formattedLocalVar}$${0});";
	public static final String SYSOUTP_CONTENT = "System.out.println($${0}${enumerated_method_arguments});";
	public static final String FOREACH_CONTENT = "for ($${1:${iterable_type}} $${2:${iterable_element}} : $${3:${iterable}}) {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "}";
	public static final String FORI_CONTENT = "for ($${1:int} $${2:${index}} = $${3:0}; $${2:${index}} < $${4:${array}.length}; $${2:${index}}++) {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "}";
	public static final String WHILE_CONTENT = "while ($${1:${condition:var(boolean)}}) {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "}";
	public static final String DOWHILE_CONTENT = "do {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "} while ($${1:${condition:var(boolean)}});";
	public static final String IF_CONTENT = "if ($${1:${condition:var(boolean)}}) {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "}";
	public static final String IFELSE_CONTENT = "if ($${1:${condition:var(boolean)}}) {\n" + "\t$${2}\n" + "} else {\n" + "\t$${0}\n" + "}";
	public static final String IFNULL_CONTENT = "if ($${1:${name:var}} == null) {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "}";
	public static final String IFNOTNULL_CONTENT = "if ($${1:${name:var}} != null) {\n" + "\t$$TM_SELECTED_TEXT$${0}\n" + "}";

	// Descriptions
	public static final String SYSOUT_DESCRIPTION = "print to standard out";
	public static final String SYSERR_DESCRIPTION = "print to standard err";
	public static final String SYSTRACE_DESCRIPTION = "print current method to standard out";
	public static final String SYSOUTV_DESCRIPTION = "print last variable to standard out";
	public static final String SYSOUTP_DESCRIPTION = "print method parameters to standard out";
	public static final String FOREACH_DESCRIPTION = "iterate over an array or Iterable";
	public static final String FORI_DESCRIPTION = "iterate over array";
	public static final String WHILE_DESCRIPTION = "while statement";
	public static final String DOWHILE_DESCRIPTION = "do-while statement";
	public static final String IF_DESCRIPTION = "if statement";
	public static final String IFELSE_DESCRIPTION = "if-else statement";
	public static final String IFNULL_DESCRIPTION = "if statement checking for null";
	public static final String IFNOTNULL_DESCRIPTION = "if statement checking for not null";
}
