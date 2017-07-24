/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.ls.core.internal.Util;
import org.junit.Test;

/**
 * Tests Javadoc to Markdown conversion
 *
 * @author Fred Bricon
 */
public class JavaDoc2MarkdownConverterTest {

	private static final String MARKDOWN_0 = "This Javadoc contains some `code`, a link to `IOException` and a table\n" +
			"\n" +
			"    | header 1 | header 2 |\n" +
			"    | -------- | -------- |\n" +
			"    | data 1   | data 2   |\n" +
			"\n" +
			"\n" +
			"x<sup>x+1</sup>. x<sub>i+1</sub>\n"   +
			"literally <b>literal</b> and now a list:\n" +
			"\n" +
			" *  **Coffee**\n" +
			"    \n" +
			"     *  Mocha\n" +
			"     *  Latte\n" +
			" *  Tea\n" +
			"    \n" +
			"     *  Darjeeling\n" +
			"     *  Early Grey\n" +
			"\n" +
			" *  **Parameters:**\n" +
			"    \n" +
			"     *  **param1** the first parameter\n" +
			"     *  **param2** the 2nd parameter\n" +
			"     *  **param3**\n" +
			" *  **Returns:**\n" +
			"    \n" +
			"     *  some kind of result\n" +
			" *  **Throws:**\n" +
			"    \n" +
			"     *  NastyException a nasty exception\n" +
			"     *  IOException another nasty exception\n" +
			" *  **Author:**\n" +
			"    \n" +
			"     *  [Ralf](mailto:foo@bar.com)\n" +
			"     *  [Andrew](mailto:bar@foo.com)\n" +
			" *  **Since:**\n" +
			"    \n" +
			"     *  1.0\n" +
			"     *  0\n" +
			" *  @unknown\n" +
			"    \n" +
			"     *  unknown tag\n" +
			" *  @unknown\n" +
			"    \n" +
			"     *  another unknown tag";

	/**
	 * This Javadoc contains some <code> code </code>, a link to
	 * {@link IOException} and a table
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>header 1</th>
	 * <th>header 2</th>
	 * </tr>
	 * </thead> <tbody>
	 * <tr>
	 * <td>data 1</td>
	 * <td>data 2</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * <br>x<sup>x+1</sup>. x<sub>i+1</sub>
	 * <br>
	 * {@literal <b>literal</b>} and now a list:
	 * <ul><li><b>Coffee</b>
	 * <ul>
	 * <li>Mocha</li>
	 * <li>Latte</li>
	 * </ul>
	 * </li>
	 * <li>Tea
	 * <ul>
	 * <li>Darjeeling</li>
	 * <li>Early Grey</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * <ul>
	 *
	 * @param param1
	 *            the first parameter
	 * @param param2
	 *            the 2nd parameter
	 * @param param3
	 * @since 1.0
	 * @since .0
	 * @author <a href=\"mailto:foo@bar.com\">Ralf</a>
	 * @author <a href=\"mailto:bar@foo.com\">Andrew</a>
	 * @exception NastyException
	 *                a\n nasty exception
	 * @throws IOException
	 *             another nasty exception
	 * @return some kind of result
	 * @unknown unknown tag
	 * @unknown another unknown tag
	 */
	private static final String RAW_JAVADOC_0 =
			"This Javadoc  contains some <code> code </code>, a link to {@link IOException} and a table \n" +
					"<table>\n" +
					"  <thead><tr><th>header 1</th><th>header 2</th></tr></thead>\n" +
					"  <tbody><tr><td>data 1</td><td>data 2</td></tr></tbody>\n" +
					"  </table>\n"+
					"<br>x<sup>x+1</sup>. x<sub>i+1</sub>" +
					"<br> literally {@literal <b>literal</b>} and now a list:\n"+
					"  <ul>"
					+ "<li><b>Coffee</b>" +
					"   <ul>" +
					"    <li>Mocha</li>" +
					"    <li>Latte</li>" +
					"   </ul>" +
					"  </li>" +
					"  <li>Tea" +
					"   <ul>" +
					"    <li>Darjeeling</li>" +
					"    <li>Early Grey</li>" +
					"   </ul>" +
					"  </li>" +
					"</ul>"+
					"\n"+
					" @param param1 the first parameter\n" +
					" @param param2 \n"+
					" the 2nd parameter\n" +
					" @param param3 \n"+
					" @since 1.0\n" +
					" @since .0\n" +
					" @author <a href=\"mailto:foo@bar.com\">Ralf</a>\n" +
					" @author <a href=\"mailto:bar@foo.com\">Andrew</a>\n" +
					" @exception NastyException a\n nasty exception\n" +
					" @throws \n"+
					"IOException another nasty exception\n" +
					" @return some kind of result\n"+
					" @unknown unknown tag\n"+
					" @unknown another unknown tag\n";

	@Test
	public void testBoundaries() throws IOException {
		assertTrue(new JavaDoc2MarkdownConverter("").getAsString().isEmpty());
		assertNull(new JavaDoc2MarkdownConverter((Reader)null).getAsString());
		assertNull(new JavaDoc2MarkdownConverter((Reader)null).getAsReader());
		assertNull(new JavaDoc2MarkdownConverter((String)null).getAsString());
		assertNull(new JavaDoc2MarkdownConverter((String)null).getAsReader());
	}

	@Test
	public void testGetAsString() throws IOException {
		String result = new JavaDoc2MarkdownConverter(RAW_JAVADOC_0).getAsString();
		assertEquals(Util.convertToIndependentLineDelimiter(MARKDOWN_0), Util.convertToIndependentLineDelimiter(result));
	}

	@Test
	public void testGetAsReader() throws IOException {
		JavaDoc2MarkdownConverter converter = new JavaDoc2MarkdownConverter(RAW_JAVADOC_0);
		Reader reader1 = converter.getAsReader();
		Reader reader2 = converter.getAsReader();
		assertNotSame(reader1, reader2);
	}
}
