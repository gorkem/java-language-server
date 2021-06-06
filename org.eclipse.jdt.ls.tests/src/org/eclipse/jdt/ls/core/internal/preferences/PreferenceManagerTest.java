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
package org.eclipse.jdt.ls.core.internal.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PreferenceManagerTest {

	@Mock
	private IMavenConfiguration mavenConfig;

	private StandardPreferenceManager preferenceManager;

	@Before
	public void setUp() {
		preferenceManager = new StandardPreferenceManager();
		preferenceManager.setMavenConfiguration(mavenConfig);
	}

	@Test
	public void testUpdateMavenSettings() throws Exception {
		String path = "/foo/bar.xml";
		Preferences preferences = Preferences.createFrom(Collections.singletonMap(Preferences.MAVEN_USER_SETTINGS_KEY, path));
		preferenceManager.update(preferences);
		verify(mavenConfig).setUserSettingsFile(path);


		//check setting the same path doesn't call Maven's config update
		reset(mavenConfig);
		when(mavenConfig.getUserSettingsFile()).thenReturn(path);
		preferenceManager.update(preferences);
		verify(mavenConfig, never()).setUserSettingsFile(anyString());

		//check setting null is allowed
		reset(mavenConfig);
		when(mavenConfig.getUserSettingsFile()).thenReturn(path);
		preferences.setMavenUserSettings(null);
		preferenceManager.update(preferences);
		verify(mavenConfig).setUserSettingsFile(null);
	}

	@Test
	public void testUpdateMavenGlobalSettings() throws Exception {
		String path = "/foo/bar.xml";
		Preferences preferences = Preferences.createFrom(Collections.singletonMap(Preferences.MAVEN_GLOBAL_SETTINGS_KEY, path));
		preferenceManager.update(preferences);
		verify(mavenConfig).setGlobalSettingsFile(path);

		//check setting the same path doesn't call Maven's config update
		reset(mavenConfig);
		when(mavenConfig.getGlobalSettingsFile()).thenReturn(path);
		preferenceManager.update(preferences);
		verify(mavenConfig, never()).setGlobalSettingsFile(anyString());

		//check setting null is allowed
		reset(mavenConfig);
		when(mavenConfig.getGlobalSettingsFile()).thenReturn(path);
		preferences.setMavenGlobalSettings(null);
		preferenceManager.update(preferences);
		verify(mavenConfig).setGlobalSettingsFile(null);
	}

	@Test
	public void testUpdateMavenWorkspaceLifecycleMappingMetadata() throws Exception {
		String path = "/foo/bar.xml";
		Preferences preferences = Preferences.createFrom(Collections.singletonMap(Preferences.MAVEN_GLOBAL_SETTINGS_KEY, path));
		preferenceManager.update(preferences);
		verify(mavenConfig).setWorkspaceLifecycleMappingMetadataFile(path);

		//check setting the same path doesn't call Maven's config update
		reset(mavenConfig);
		when(mavenConfig.getWorkspaceLifecycleMappingMetadataFile()).thenReturn(path);
		preferenceManager.update(preferences);
		verify(mavenConfig, never()).setWorkspaceLifecycleMappingMetadataFile(anyString());

		//check setting null is allowed
		reset(mavenConfig);
		when(mavenConfig.getWorkspaceLifecycleMappingMetadataFile()).thenReturn(path);
		preferences.setWorkspaceLifecycleMappingMetadataFile(null);
		preferenceManager.update(preferences);
		verify(mavenConfig).setWorkspaceLifecycleMappingMetadataFile(null);
	}

	@Test
	public void testInitialize() throws Exception {
		preferenceManager.initialize();
		assertEquals(JavaCore.ENABLED, JavaCore.getOptions().get(JavaCore.CODEASSIST_VISIBILITY_CHECK));
		assertEquals(JavaCore.IGNORE, JavaCore.getOptions().get(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN));
	}

	@Test
	public void testPreferencesChangeListener() throws Exception {
		preferenceManager.initialize();
		boolean called[] = new boolean[1];
		called[0] = false;
		IPreferencesChangeListener listener = new IPreferencesChangeListener() {

			@Override
			public void preferencesChange(Preferences oldPreferences, Preferences newPreferences) {
				called[0] = true;
			}
		};
		preferenceManager.addPreferencesChangeListener(listener);
		Preferences preferences = new Preferences();
		preferenceManager.update(preferences);
		assertTrue("No one listener has been called", called[0]);
		preferenceManager.removePreferencesChangeListener(listener);
		called[0] = false;
		preferences = new Preferences();
		preferenceManager.update(preferences);
		assertFalse("A listener has been called", called[0]);
	}

	@Test
	public void testUpdateFileHeaderTemplate() {
		preferenceManager.initialize();

		Template template = JavaManipulation.getCodeTemplateStore().findTemplateById(CodeTemplateContextType.FILECOMMENT_ID);
		assertNull(template);

		Preferences preferences = new Preferences();
		preferences.setFileHeaderTemplate(Arrays.asList("/** */"));
		preferenceManager.update(preferences);

		template = JavaManipulation.getCodeTemplateStore().findTemplateById(CodeTemplateContextType.FILECOMMENT_ID);
		assertNotNull(template);
		assertEquals("/** */", template.getPattern());
	}

	@Test
	public void testUpdateTypeCommentTemplate() {
		preferenceManager.initialize();

		Template template = JavaManipulation.getCodeTemplateStore().findTemplateById(CodeTemplateContextType.TYPECOMMENT_ID);
		assertNotNull(template);
		assertEquals(CodeTemplatePreferences.CODETEMPLATE_TYPECOMMENT_DEFAULT, template.getPattern());

		Preferences preferences = new Preferences();
		preferences.setTypeCommentTemplate(Arrays.asList("/** */"));
		preferenceManager.update(preferences);

		template = JavaManipulation.getCodeTemplateStore().findTemplateById(CodeTemplateContextType.TYPECOMMENT_ID);
		assertNotNull(template);
		assertEquals("/** */", template.getPattern());
	}

	@Test
	public void testInsertSpacesTabSize() {
		preferenceManager.initialize();
		Preferences preferences = new Preferences();
		preferenceManager.update(preferences);
		assertTrue(preferenceManager.getPreferences().isInsertSpaces());
		assertEquals(4, preferenceManager.getPreferences().getTabSize());
		Map<String, String> eclipseOptions = JavaCore.getOptions();
		String tabSize = eclipseOptions.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
		assertEquals(4, Integer.valueOf(tabSize).intValue());
		String insertSpaces = eclipseOptions.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
		assertEquals(JavaCore.SPACE, insertSpaces);
	}
}
