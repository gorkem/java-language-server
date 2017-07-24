/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.google.common.collect.Sets;
import com.overzealous.remark.IgnoredHtmlElement;
import com.overzealous.remark.Options;
import com.overzealous.remark.Options.Tables;
import com.overzealous.remark.Remark;

/**
 * Converts JavaDoc tags into Markdown equivalent.
 *
 * @author Fred Bricon
 */
public class JavaDoc2MarkdownConverter {

	private static Remark remark;

	private String markDown;

	static {
		Options options = new Options();
		options.tables = Tables.CONVERT_TO_CODE_BLOCK;
		options.hardwraps = true;
		options.inlineLinks = true;
		options.autoLinks = true;
		options.reverseHtmlSmartPunctuation = true;
		options.ignoredHtmlElements = Sets.newHashSet(IgnoredHtmlElement.create("sup"), IgnoredHtmlElement.create("sub"));
		remark = new Remark(options);
	}

	private JavaDoc2HTMLTextReader reader;

	private boolean read;

	public JavaDoc2MarkdownConverter(Reader reader) {
		setJavaDoc2HTMLTextReader(reader);
	}


	public JavaDoc2MarkdownConverter(String javadoc) {
		setJavaDoc2HTMLTextReader(javadoc== null? null:new StringReader(javadoc));
	}

	private void setJavaDoc2HTMLTextReader(Reader reader) {
		if (reader == null || reader instanceof JavaDoc2HTMLTextReader)  {
			this.reader = (JavaDoc2HTMLTextReader) reader;
		} else {
			this.reader = new JavaDoc2HTMLTextReader(reader);
		}
	}

	public String getAsString() throws IOException {
		if (!read && reader != null) {
			String rawHtml = reader.getString();
			markDown = remark.convert(rawHtml);
		}
		return markDown;
	}

	public Reader getAsReader() throws IOException {
		String m = getAsString();
		return m == null ? null : new StringReader(m);
	}
}
