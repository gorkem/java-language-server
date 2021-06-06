/*******************************************************************************
 * Copyright (c) 2016-2021 Red Hat Inc. and others.
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

import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getBoolean;
import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getInt;
import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getList;
import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getString;
import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getValue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.internal.resources.PreferenceInitializer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;
import org.eclipse.jdt.internal.core.manipulation.MembersOrderPreferenceCacheCommon;
import org.eclipse.jdt.ls.core.internal.IConstants;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ResourceUtils;
import org.eclipse.jdt.ls.core.internal.RuntimeEnvironment;
import org.eclipse.jdt.ls.core.internal.contentassist.TypeFilter;
import org.eclipse.lsp4j.MessageType;

/**
 * Preferences model
 *
 * @author Fred Bricon
 *
 */
public class Preferences {

	/**
	 * Specifies the folder path to the JDK .
	 */
	public static final String JAVA_HOME = "java.home";
	/**
	 * Preference key used to include getter, setter and builder/constructor when
	 * finding references.
	 */
	public static final String JAVA_REFERENCES_INCLUDE_ACCESSORS = "java.references.includeAccessors";

	/**
	 * Preference key used to include the decompiled sources when finding
	 * references.
	 */
	public static final String JAVA_REFERENCES_INCLUDE_DECOMPILED_SOURCES = "java.references.includeDecompiledSources";

	/**
	 * Include method declarations from source files in symbol search.
	 */
	public static final String JAVA_SYMBOLS_INCLUDE_SOURCE_METHOD_DECLARATIONS = "java.symbols.includeSourceMethodDeclarations";

	/**
	 * Insert spaces when pressing Tab
	 */
	public static final String JAVA_CONFIGURATION_INSERTSPACES = "java.format.insertSpaces";
	/**
	 * Tab Size
	 */
	public static final String JAVA_CONFIGURATION_TABSIZE = "java.format.tabSize";
	/**
	 * Specifies Java Execution Environments.
	 */
	public static final String JAVA_CONFIGURATION_RUNTIMES = "java.configuration.runtimes";
	public static final List<String> JAVA_CONFIGURATION_RUNTIMES_DEFAULT;
	/**
	 * Specifies the file path or url to the formatter xml url.
	 */
	public static final String JAVA_FORMATTER_URL = "java.format.settings.url";
	/**
	 * Specifies the formatter profile name.
	 */
	public static final String JAVA_FORMATTER_PROFILE_NAME = "java.format.settings.profile";
	/**
	 * Preference key used to include the comments during the formatting.
	 */
	public static final String JAVA_FORMAT_COMMENTS = "java.format.comments.enabled";
	/**
	 * Specifies the file path or url to the Java setting.
	 */
	public static final String JAVA_SETTINGS_URL = "java.settings.url";

	/**
	 * Specifies filter applied on projects to exclude some file system objects
	 * while populating the resources tree.
	 */
	public static final String JAVA_RESOURCE_FILTERS = "java.project.resourceFilters";
	public static final List<String> JAVA_RESOURCE_FILTERS_DEFAULT;
	/**
	 * Preference key to enable/disable gradle importer.
	 */
	public static final String IMPORT_GRADLE_ENABLED = "java.import.gradle.enabled";
	/**
	 * Preference key to enable/disable gradle offline mode.
	 */
	public static final String IMPORT_GRADLE_OFFLINE_ENABLED = "java.import.gradle.offline.enabled";
	/**
	 * Preference key to enable/disable gradle wrapper.
	 */
	public static final String GRADLE_WRAPPER_ENABLED = "java.import.gradle.wrapper.enabled";
	/**
	 * Preference key for gradle version to use when the gradle wrapper is not used.
	 */
	public static final String GRADLE_VERSION = "java.import.gradle.version";
	/**
	 * Preference key for arguments to pass to Gradle
	 */
	public static final String GRADLE_ARGUMENTS = "java.import.gradle.arguments";
	/**
	 * Preference key for JVM arguments to pass to Gradle
	 */
	public static final String GRADLE_JVM_ARGUMENTS = "java.import.gradle.jvmArguments";
	/**
	 * Preference key for setting GRADLE_HOME.
	 */
	public static final String GRADLE_HOME = "java.import.gradle.home";
	/**
	 * Preference key for the JVM used to run the Gradle daemon..
	 */
	public static final String GRADLE_JAVA_HOME = "java.import.gradle.java.home";
	/**
	 * Preference key for setting GRADLE_USER_HOME.
	 */
	public static final String GRADLE_USER_HOME = "java.import.gradle.user.home";
	/**
	 * Preference key to enable/disable maven importer.
	 */
	public static final String IMPORT_MAVEN_ENABLED = "java.import.maven.enabled";
	/**
	 * Preference key to enable/disable downloading Maven source artifacts.
	 */
	public static final String MAVEN_DOWNLOAD_SOURCES = "java.maven.downloadSources";
	/**
	 * Preference key to enable/disable downloading source artifacts for Eclipse
	 * projects.
	 */
	public static final String ECLIPSE_DOWNLOAD_SOURCES = "java.eclipse.downloadSources";
	/**
	 * Preference key to force update of Snapshots/Releases.
	 */
	public static final String MAVEN_UPDATE_SNAPSHOTS = "java.maven.updateSnapshots";
	/**
	 * Preference key to enable/disable reference code lenses.
	 */
	public static final String REFERENCES_CODE_LENS_ENABLED_KEY = "java.referencesCodeLens.enabled";

	/**
	 * Preference key to enable/disable implementation code lenses.
	 */
	public static final String IMPLEMENTATIONS_CODE_LENS_ENABLED_KEY = "java.implementationsCodeLens.enabled";

	/**
	 * Preference key to enable/disable formatter.
	 */
	public static final String JAVA_FORMAT_ENABLED_KEY = "java.format.enabled";

	/**
	 * Preference key to enable/disable formatter on-type.
	 */
	public static final String JAVA_FORMAT_ON_TYPE_ENABLED_KEY = "java.format.onType.enabled";

	/**
	 * Preference key to enable/disable organize imports on save
	 */
	public static final String JAVA_SAVE_ACTIONS_ORGANIZE_IMPORTS_KEY = "java.saveActions.organizeImports";

	/**
	 * Preference key to enable/disable signature help.
	 */
	public static final String SIGNATURE_HELP_ENABLED_KEY = "java.signatureHelp.enabled";

	/**
	 * Preference key to enable/disable rename.
	 */
	public static final String RENAME_ENABLED_KEY = "java.rename.enabled";

	/**
	 * Preference key to enable/disable executeCommand.
	 */
	public static final String EXECUTE_COMMAND_ENABLED_KEY = "java.executeCommand.enabled";

	/**
	 * Preference key to enable/disable the 'auto build'.
	 */
	public static final String AUTOBUILD_ENABLED_KEY = "java.autobuild.enabled";

	/**
	 * Preference key to set max concurrent build count.
	 */
	public static final String JAVA_MAX_CONCURRENT_BUILDS = "java.maxConcurrentBuilds";

	/**
	 * Preference key to exclude directories when importing projects.
	 */
	public static final String JAVA_IMPORT_EXCLUSIONS_KEY = "java.import.exclusions";
	public static final List<String> JAVA_IMPORT_EXCLUSIONS_DEFAULT;

	/**
	 * Preference key to specify the local libraries referenced by invisible project
	 */
	public static final String JAVA_PROJECT_REFERENCED_LIBRARIES_KEY = "java.project.referencedLibraries";
	public static final ReferencedLibraries JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT;

	/**
	 * Preference key to specify the output path of the invisible project
	 */
	public static final String JAVA_PROJECT_OUTPUT_PATH_KEY = "java.project.outputPath";

	/**
	 * Preference key to specify the source paths of the invisible project
	 */
	public static final String JAVA_PROJECT_SOURCE_PATHS_KEY = "java.project.sourcePaths";

	/**
	 * Preference key for project build/configuration update settings.
	 */
	public static final String CONFIGURATION_UPDATE_BUILD_CONFIGURATION_KEY = "java.configuration.updateBuildConfiguration";

	/**
	 * Preference key for incomplete classpath severity messages.
	 */
	public static final String ERRORS_INCOMPLETE_CLASSPATH_SEVERITY_KEY = "java.errors.incompleteClasspath.severity";

	/**
	 * Preference key for Maven user settings.xml location.
	 */
	public static final String MAVEN_USER_SETTINGS_KEY = "java.configuration.maven.userSettings";

	/**
	 * Preference key for Maven global settings.xml location.
	 */
	public static final String MAVEN_GLOBAL_SETTINGS_KEY = "java.configuration.maven.globalSettings";

	/**
	 * Preference key for Maven lifecycle-mapping-metadata.xml location.
	 */
	public static final String MAVEN_LIFECYCLE_MAPPING_METADATA_KEY = "java.configuration.maven.lifeCycleMappingMetadata";

	/**
	 * Preference key to enable/disable the 'completion'.
	 */
	public static final String COMPLETION_ENABLED_KEY = "java.completion.enabled";

	/**
	 * Preference key to enable/disable the 'foldingRange'.
	 */
	public static final String FOLDINGRANGE_ENABLED_KEY = "java.foldingRange.enabled";

	/**
	 * Preference key to enable/disable the selection range.
	 */
	public static final String SELECTIONRANGE_ENABLED_KEY = "java.selectionRange.enabled";

	/**
	 * A named preference that holds the allowed gradle wrapper sha256 checksums.
	 * <p>
	 * Value is of type <code>String</code>: list of checksums.
	 * </p>
	 */
	public static final String JAVA_GRADLE_WRAPPER_SHA256_KEY = "java.imports.gradle.wrapper.checksums";
	public static final List<String> JAVA_GRADLE_WRAPPER_SHA256_DEFAULT;

	/**
	 * A named preference that holds the favorite static members.
	 * <p>
	 * Value is of type <code>String</code>: list of favorites.
	 * </p>
	 */
	public static final String JAVA_COMPLETION_FAVORITE_MEMBERS_KEY = "java.completion.favoriteStaticMembers";
	public static final List<String> JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT;

	/**
	 * Preference key for maximum number of completion results to be returned.
	 * Defaults to 50.
	 */
	public static final String JAVA_COMPLETION_MAX_RESULTS_KEY = "java.completion.maxResults";
	public static final int JAVA_COMPLETION_MAX_RESULTS_DEFAULT = 50;

	/**
	 * A named preference that controls if the Java code assist only inserts
	 * completions. When set to true, code completion overwrites the current text.
	 * When set to false, code is simply added instead.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String JAVA_COMPLETION_OVERWRITE_KEY = "java.completion.overwrite";
	/**
	 * A named preference that controls if method arguments are guessed when a
	 * method is selected from as list of code assist proposal.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String JAVA_COMPLETION_GUESS_METHOD_ARGUMENTS_KEY = "java.completion.guessMethodArguments";

	/**
	 * A named preference that defines how member elements are ordered by code
	 * actions.
	 * <p>
	 * Value is of type <code>String</code>: A comma separated list of the following
	 * entries. Each entry must be in the list, no duplication. List order defines
	 * the sort order.
	 * <ul>
	 * <li><b>T</b>: Types</li>
	 * <li><b>C</b>: Constructors</li>
	 * <li><b>I</b>: Initializers</li>
	 * <li><b>M</b>: Methods</li>
	 * <li><b>F</b>: Fields</li>
	 * <li><b>SI</b>: Static Initializers</li>
	 * <li><b>SM</b>: Static Methods</li>
	 * <li><b>SF</b>: Static Fields</li>
	 * </ul>
	 * </p>
	 */
	public static final String MEMBER_SORT_ORDER = "java.memberSortOrder"; //$NON-NLS-1$

	/**
	 * A named preference that specifies the number of imports added before a
	 * star-import declaration is used.
	 * <p>
	 * Value is of type <code>Integer</code>: positive value specifying the number
	 * of non star-import is used
	 * </p>
	 */
	public static final String IMPORTS_ONDEMANDTHRESHOLD = "java.sources.organizeImports.starThreshold"; //$NON-NLS-1$

	/**
	 * A named preference that specifies the number of static imports added before a
	 * star-import declaration is used.
	 * <p>
	 * Value is of type <code>Integer</code>: positive value specifying the number
	 * of non star-import is used
	 * </p>
	 */
	public static final String IMPORTS_STATIC_ONDEMANDTHRESHOLD = "java.sources.organizeImports.staticStarThreshold"; //$NON-NLS-1$

	public static final int IMPORTS_ONDEMANDTHRESHOLD_DEFAULT = 99;

	public static final int IMPORTS_STATIC_ONDEMANDTHRESHOLD_DEFAULT = 99;

	/**
	 * Preference key for the id(s) of the preferred content provider(s).
	 */
	public static final String PREFERRED_CONTENT_PROVIDER_KEY = "java.contentProvider.preferred";

	/**
	 * A named preference that holds a list of package names. The list specifies the
	 * import order used by the "Organize Imports" operation.
	 * <p>
	 * Value is of type <code>String</code>: list of package names
	 * </p>
	 */
	public static final String JAVA_IMPORT_ORDER_KEY = "java.completion.importOrder";
	public static final List<String> JAVA_IMPORT_ORDER_DEFAULT;

	public static final String JAVA_COMPLETION_FILTERED_TYPES_KEY = "java.completion.filteredTypes";
	public static final List<String> JAVA_COMPLETION_FILTERED_TYPES_DEFAULT;

	// A named preference that defines whether to use Objects.hash and Objects.equals methods when generating the hashCode and equals methods.
	public static final String JAVA_CODEGENERATION_HASHCODEEQUALS_USEJAVA7OBJECTS = "java.codeGeneration.hashCodeEquals.useJava7Objects";
	// A named preference that defines whether to use 'instanceof' to compare types when generating the hashCode and equals methods.
	public static final String JAVA_CODEGENERATION_HASHCODEEQUALS_USEINSTANCEOF = "java.codeGeneration.hashCodeEquals.useInstanceof";
	// A named preference that defines whether to use blocks in 'if' statements when generating the methods.
	public static final String JAVA_CODEGENERATION_USEBLOCKS = "java.codeGeneration.useBlocks";
	// A named preference that defines whether to generate method comments when generating the methods.
	public static final String JAVA_CODEGENERATION_GENERATECOMMENTS = "java.codeGeneration.generateComments";
	// A named preference that defines the location to insert the code generated by source actions.
	public static final String JAVA_CODEGENERATION_INSERTIONLOCATION = "java.codeGeneration.insertionLocation";

	// Specifies the file header snippets for new Java file.
	public static final String JAVA_TEMPLATES_FILEHEADER = "java.templates.fileHeader";
	// Specifies the type comment snippets for new Java type.
	public static final String JAVA_TEMPLATES_TYPECOMMENT = "java.templates.typeComment";

	/**
	 * The preferences for generating toString method.
	 */
	public static final String JAVA_CODEGENERATION_TOSTRING_TEMPLATE = "java.codeGeneration.toString.template";
	public static final String JAVA_CODEGENERATION_TOSTRING_CODESTYLE = "java.codeGeneration.toString.codeStyle";
	public static final String JAVA_CODEGENERATION_TOSTRING_SKIPNULLVALUES = "java.codeGeneration.toString.skipNullValues";
	public static final String JAVA_CODEGENERATION_TOSTRING_LISTARRAYCONTENTS = "java.codeGeneration.toString.listArrayContents";
	public static final String JAVA_CODEGENERATION_TOSTRING_LIMITELEMENTS = "java.codeGeneration.toString.limitElements";

	public static final String TEXT_DOCUMENT_FORMATTING = "textDocument/formatting";
	public static final String TEXT_DOCUMENT_RANGE_FORMATTING = "textDocument/rangeFormatting";
	public static final String TEXT_DOCUMENT_ON_TYPE_FORMATTING = "textDocument/onTypeFormatting";
	public static final String TEXT_DOCUMENT_CODE_LENS = "textDocument/codeLens";
	public static final String TEXT_DOCUMENT_SIGNATURE_HELP = "textDocument/signatureHelp";
	public static final String TEXT_DOCUMENT_RENAME = "textDocument/rename";
	public static final String WORKSPACE_EXECUTE_COMMAND = "workspace/executeCommand";
	public static final String WORKSPACE_SYMBOL = "workspace/symbol";
	public static final String WORKSPACE_WATCHED_FILES = "workspace/didChangeWatchedFiles";
	public static final String DOCUMENT_SYMBOL = "textDocument/documentSymbol";
	public static final String COMPLETION = "textDocument/completion";
	public static final String CODE_ACTION = "textDocument/codeAction";
	public static final String DEFINITION = "textDocument/definition";
	public static final String TYPEDEFINITION = "textDocument/typeDefinition";
	public static final String HOVER = "textDocument/hover";
	public static final String REFERENCES = "textDocument/references";
	public static final String DOCUMENT_HIGHLIGHT = "textDocument/documentHighlight";
	public static final String FOLDINGRANGE = "textDocument/foldingRange";
	public static final String WORKSPACE_CHANGE_FOLDERS = "workspace/didChangeWorkspaceFolders";
	public static final String IMPLEMENTATION = "textDocument/implementation";
	public static final String SELECTION_RANGE = "textDocument/selectionRange";

	public static final String FORMATTING_ID = UUID.randomUUID().toString();
	public static final String FORMATTING_ON_TYPE_ID = UUID.randomUUID().toString();
	public static final String FORMATTING_RANGE_ID = UUID.randomUUID().toString();
	public static final String CODE_LENS_ID = UUID.randomUUID().toString();
	public static final String SIGNATURE_HELP_ID = UUID.randomUUID().toString();
	public static final String RENAME_ID = UUID.randomUUID().toString();
	public static final String EXECUTE_COMMAND_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_SYMBOL_ID = UUID.randomUUID().toString();
	public static final String DOCUMENT_SYMBOL_ID = UUID.randomUUID().toString();
	public static final String COMPLETION_ID = UUID.randomUUID().toString();
	public static final String CODE_ACTION_ID = UUID.randomUUID().toString();
	public static final String DEFINITION_ID = UUID.randomUUID().toString();
	public static final String TYPEDEFINITION_ID = UUID.randomUUID().toString();
	public static final String HOVER_ID = UUID.randomUUID().toString();
	public static final String REFERENCES_ID = UUID.randomUUID().toString();
	public static final String DOCUMENT_HIGHLIGHT_ID = UUID.randomUUID().toString();
	public static final String FOLDINGRANGE_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_CHANGE_FOLDERS_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_WATCHED_FILES_ID = UUID.randomUUID().toString();
	public static final String IMPLEMENTATION_ID = UUID.randomUUID().toString();
	public static final String SELECTION_RANGE_ID = UUID.randomUUID().toString();
	private static final String GRADLE_OFFLINE_MODE = "gradle.offline.mode";
	private static final int DEFAULT_TAB_SIZE = 4;

	private Map<String, Object> configuration;
	private Severity incompleteClasspathSeverity;
	private FeatureStatus updateBuildConfigurationStatus;
	private boolean referencesCodeLensEnabled;
	private boolean importGradleEnabled;
	private boolean importGradleOfflineEnabled;
	private boolean gradleWrapperEnabled;
	private String gradleVersion;
	private List<String> gradleArguments;
	private List<String> gradleJvmArguments;
	private String gradleHome;
	private String gradleJavaHome;
	private String gradleUserHome;
	private boolean importMavenEnabled;
	private boolean mavenDownloadSources;
	private boolean eclipseDownloadSources;
	private boolean mavenUpdateSnapshots;
	private boolean implementationsCodeLensEnabled;
	private boolean javaFormatEnabled;
	private boolean javaFormatOnTypeEnabled;
	private boolean javaSaveActionsOrganizeImportsEnabled;
	private boolean signatureHelpEnabled;
	private boolean renameEnabled;
	private boolean executeCommandEnabled;
	private boolean autobuildEnabled;
	private boolean completionEnabled;
	private boolean completionOverwrite;
	private boolean foldingRangeEnabled;
	private boolean selectionRangeEnabled;
	private boolean guessMethodArguments;
	private boolean javaFormatComments;
	private boolean hashCodeEqualsTemplateUseJava7Objects;
	private boolean hashCodeEqualsTemplateUseInstanceof;
	private boolean codeGenerationTemplateUseBlocks;
	private boolean codeGenerationTemplateGenerateComments;
	private String generateToStringTemplate;
	private String generateToStringCodeStyle;
	private boolean generateToStringSkipNullValues;
	private boolean generateToStringListArrayContents;
	private int generateToStringLimitElements;
	private String codeGenerationInsertionLocation;
	private List<String> preferredContentProviderIds;
	private boolean includeAccessors;
	private boolean includeDecompiledSources;
	private boolean includeSourceMethodDeclarations;

	private String mavenUserSettings;
	private String mavenGlobalSettings;
	private String workspaceLifecycleMappingMetadataFile;

	private List<String> javaCompletionFavoriteMembers;
	private List<?> gradleWrapperList;

	private List<String> javaImportExclusions = new LinkedList<>();
	private ReferencedLibraries referencedLibraries;
	private String invisibleProjectOutputPath;
	private List<String> invisibleProjectSourcePaths;
	private String javaHome;
	private List<String> importOrder;
	private List<String> filteredTypes;
	private String formatterUrl;
	private String settingsUrl;
	private String formatterProfileName;
	private Collection<IPath> rootPaths;
	private Collection<IPath> triggerFiles;
	private int parallelBuildsCount;
	private int maxCompletionResults;
	private int importOnDemandThreshold;
	private int staticImportOnDemandThreshold;
	private Set<RuntimeEnvironment> runtimes = new HashSet<>();
	private List<String> resourceFilters;

	private List<String> fileHeaderTemplate = new LinkedList<>();
	private List<String> typeCommentTemplate = new LinkedList<>();
	private boolean insertSpaces;
	private int tabSize;

	static {
		JAVA_IMPORT_EXCLUSIONS_DEFAULT = new LinkedList<>();
		JAVA_CONFIGURATION_RUNTIMES_DEFAULT = new ArrayList<>();
		JAVA_GRADLE_WRAPPER_SHA256_DEFAULT = new ArrayList<>();
		JAVA_IMPORT_EXCLUSIONS_DEFAULT.add("**/node_modules/**");
		JAVA_IMPORT_EXCLUSIONS_DEFAULT.add("**/.metadata/**");
		JAVA_IMPORT_EXCLUSIONS_DEFAULT.add("**/archetype-resources/**");
		JAVA_IMPORT_EXCLUSIONS_DEFAULT.add("**/META-INF/maven/**");
		JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT = new ReferencedLibraries();
		JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT.getInclude().add("lib/**");
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT = new ArrayList<>();
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT.add("org.junit.Assert.*");
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT.add("org.junit.Assume.*");
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT.add("org.junit.jupiter.api.Assertions.*");
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT.add("org.junit.jupiter.api.Assumptions.*");
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT.add("org.junit.jupiter.api.DynamicContainer.*");
		JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT.add("org.junit.jupiter.api.DynamicTest.*");
		JAVA_IMPORT_ORDER_DEFAULT = new ArrayList<>();
		JAVA_IMPORT_ORDER_DEFAULT.add("java");
		JAVA_IMPORT_ORDER_DEFAULT.add("javax");
		JAVA_IMPORT_ORDER_DEFAULT.add("com");
		JAVA_IMPORT_ORDER_DEFAULT.add("org");
		JAVA_COMPLETION_FILTERED_TYPES_DEFAULT = new ArrayList<>();
		JAVA_COMPLETION_FILTERED_TYPES_DEFAULT.add("java.awt.*");
		JAVA_COMPLETION_FILTERED_TYPES_DEFAULT.add("com.sun.*");
		JAVA_COMPLETION_FILTERED_TYPES_DEFAULT.add("sun.*");
		JAVA_RESOURCE_FILTERS_DEFAULT = Arrays.asList("node_modules", ".git");
	}

	public static enum Severity {
		ignore, log, info, warning, error;

		static Severity fromString(String value, Severity defaultSeverity) {
			if (value != null) {
				String val = value.toLowerCase();
				try {
					return valueOf(val);
				} catch(Exception e) {
					//fall back to default severity
				}
			}
			return defaultSeverity;
		}

		public MessageType toMessageType() {
			for (MessageType type : MessageType.values()) {
				if (name().equalsIgnoreCase(type.name())) {
					return type;
				}
			}
			//'ignore' has no MessageType equivalent
			return null;
		}
	}

	public static enum FeatureStatus {
		disabled, interactive, automatic ;

		static FeatureStatus fromString(String value, FeatureStatus defaultStatus) {
			if (value != null) {
				String val = value.toLowerCase();
				try {
					return valueOf(val);
				} catch(Exception e) {
					//fall back to default severity
				}
			}
			return defaultStatus;
		}
	}

	public static class ReferencedLibraries {
		private Set<String> include;
		private Set<String> exclude;
		private Map<String, String> sources;

		public ReferencedLibraries() {
			this(new HashSet<>(), new HashSet<>(), new HashMap<>());
		}

		public ReferencedLibraries(Set<String> include) {
			this(include, new HashSet<>(), new HashMap<>());
		}

		public ReferencedLibraries(Set<String> include, Set<String> exclude, Map<String, String> sources) {
			this.include = new HashSet<>() {

				@Override
				public boolean add(String e) {
					return super.add(ResourceUtils.expandPath(e));
				}

			};
			this.include.addAll(include);
			this.exclude = new HashSet<>() {

				@Override
				public boolean add(String e) {
					return super.add(ResourceUtils.expandPath(e));
				}

			};
			this.exclude.addAll(exclude);
			this.sources = new HashMap<>() {

				@Override
				public String put(String key, String value) {
					return super.put(ResourceUtils.expandPath(key), ResourceUtils.expandPath(value));
				}

			};
			this.sources.putAll(sources);
		}

		public Set<String> getInclude() {
			return include;
		}

		public Set<String> getExclude() {
			return exclude;
		}

		public Map<String, String> getSources() {
			return sources;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ReferencedLibraries other = (ReferencedLibraries) obj;
			return Objects.equals(include, other.include)
				&& Objects.equals(exclude, other.exclude)
				&& Objects.equals(sources, other.sources);
		}

	}

	public Preferences() {
		configuration = null;
		incompleteClasspathSeverity = Severity.warning;
		updateBuildConfigurationStatus = FeatureStatus.interactive;
		importGradleEnabled = true;
		importGradleOfflineEnabled = false;
		gradleWrapperEnabled = true;
		gradleVersion = null;
		gradleArguments = new ArrayList<>();
		gradleJvmArguments = new ArrayList<>();
		gradleHome = null;
		gradleJavaHome = null;
		gradleUserHome = null;
		importMavenEnabled = true;
		mavenDownloadSources = false;
		eclipseDownloadSources = false;
		mavenUpdateSnapshots = false;
		referencesCodeLensEnabled = true;
		implementationsCodeLensEnabled = false;
		javaFormatEnabled = true;
		javaFormatOnTypeEnabled = false;
		javaSaveActionsOrganizeImportsEnabled = false;
		signatureHelpEnabled = false;
		renameEnabled = true;
		executeCommandEnabled = true;
		autobuildEnabled = true;
		completionEnabled = true;
		completionOverwrite = true;
		foldingRangeEnabled = true;
		selectionRangeEnabled = true;
		guessMethodArguments = false;
		javaFormatComments = true;
		hashCodeEqualsTemplateUseJava7Objects = false;
		hashCodeEqualsTemplateUseInstanceof = false;
		codeGenerationTemplateUseBlocks = false;
		codeGenerationTemplateGenerateComments = false;
		generateToStringSkipNullValues = false;
		generateToStringListArrayContents = true;
		generateToStringLimitElements = 0;
		codeGenerationInsertionLocation = null;
		preferredContentProviderIds = null;
		javaImportExclusions = JAVA_IMPORT_EXCLUSIONS_DEFAULT;
		javaCompletionFavoriteMembers = JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT;
		javaHome = null;
		formatterUrl = null;
		settingsUrl = null;
		formatterProfileName = null;
		importOrder = JAVA_IMPORT_ORDER_DEFAULT;
		filteredTypes = JAVA_COMPLETION_FILTERED_TYPES_DEFAULT;
		parallelBuildsCount = PreferenceInitializer.PREF_MAX_CONCURRENT_BUILDS_DEFAULT;
		maxCompletionResults = JAVA_COMPLETION_MAX_RESULTS_DEFAULT;
		importOnDemandThreshold = IMPORTS_ONDEMANDTHRESHOLD_DEFAULT;
		staticImportOnDemandThreshold = IMPORTS_STATIC_ONDEMANDTHRESHOLD_DEFAULT;
		referencedLibraries = JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT;
		resourceFilters = JAVA_RESOURCE_FILTERS_DEFAULT;
		includeAccessors = true;
		includeDecompiledSources = true;
		includeSourceMethodDeclarations = false;
		insertSpaces = true;
		tabSize = DEFAULT_TAB_SIZE;
	}

	/**
	 * Create a {@link Preferences} model from a {@link Map} configuration.
	 */
	@SuppressWarnings("unchecked")
	public static Preferences createFrom(Map<String, Object> configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException("Configuration can not be null");
		}
		Preferences prefs = new Preferences();
		prefs.configuration = configuration;

		String incompleteClasspathSeverity = getString(configuration, ERRORS_INCOMPLETE_CLASSPATH_SEVERITY_KEY, null);
		prefs.setIncompleteClasspathSeverity(Severity.fromString(incompleteClasspathSeverity, Severity.warning));

		String updateBuildConfiguration = getString(configuration, CONFIGURATION_UPDATE_BUILD_CONFIGURATION_KEY, null);
		prefs.setUpdateBuildConfigurationStatus(
				FeatureStatus.fromString(updateBuildConfiguration, FeatureStatus.interactive));

		boolean importGradleEnabled = getBoolean(configuration, IMPORT_GRADLE_ENABLED, true);
		prefs.setImportGradleEnabled(importGradleEnabled);
		boolean insertSpaces = getBoolean(configuration, JAVA_CONFIGURATION_INSERTSPACES, true);
		prefs.setInsertSpaces(insertSpaces);
		int tabSize = getInt(configuration, JAVA_CONFIGURATION_TABSIZE, DEFAULT_TAB_SIZE);
		prefs.setTabSize(tabSize);
		boolean importGradleOfflineEnabled = getBoolean(configuration, IMPORT_GRADLE_OFFLINE_ENABLED, false);
		prefs.setImportGradleOfflineEnabled(importGradleOfflineEnabled);
		boolean gradleWrapperEnabled = getBoolean(configuration, GRADLE_WRAPPER_ENABLED, true);
		prefs.setGradleWrapperEnabled(gradleWrapperEnabled);
		String gradleVersion = getString(configuration, GRADLE_VERSION);
		prefs.setGradleVersion(gradleVersion);
		List<String> gradleArguments = getList(configuration, GRADLE_ARGUMENTS);
		prefs.setGradleArguments(gradleArguments);
		List<String> gradleJvmArguments = getList(configuration, GRADLE_JVM_ARGUMENTS);
		prefs.setGradleJvmArguments(gradleJvmArguments);
		String gradleHome = getString(configuration, GRADLE_HOME);
		prefs.setGradleHome(gradleHome);
		String gradleJavaHome = getString(configuration, GRADLE_JAVA_HOME);
		prefs.setGradleJavaHome(gradleJavaHome);
		String gradleUserHome = getString(configuration, GRADLE_USER_HOME);
		prefs.setGradleUserHome(gradleUserHome);
		boolean importMavenEnabled = getBoolean(configuration, IMPORT_MAVEN_ENABLED, true);
		prefs.setImportMavenEnabled(importMavenEnabled);
		boolean mavenDownloadSources = getBoolean(configuration, MAVEN_DOWNLOAD_SOURCES, false);
		prefs.setMavenDownloadSources(mavenDownloadSources);
		boolean eclipseDownloadSources = getBoolean(configuration, ECLIPSE_DOWNLOAD_SOURCES, false);
		prefs.setEclipseDownloadSources(eclipseDownloadSources);

		boolean updateSnapshots = getBoolean(configuration, MAVEN_UPDATE_SNAPSHOTS, false);
		prefs.setMavenUpdateSnapshots(updateSnapshots);
		boolean referenceCodelensEnabled = getBoolean(configuration, REFERENCES_CODE_LENS_ENABLED_KEY, true);
		prefs.setReferencesCodelensEnabled(referenceCodelensEnabled);
		boolean implementationCodeLensEnabled = getBoolean(configuration, IMPLEMENTATIONS_CODE_LENS_ENABLED_KEY, false);
		prefs.setImplementationCodelensEnabled(implementationCodeLensEnabled);

		boolean javaFormatEnabled = getBoolean(configuration, JAVA_FORMAT_ENABLED_KEY, true);
		prefs.setJavaFormatEnabled(javaFormatEnabled);

		boolean javaFormatOnTypeEnabled = getBoolean(configuration, JAVA_FORMAT_ON_TYPE_ENABLED_KEY, false);
		prefs.setJavaFormatOnTypeEnabled(javaFormatOnTypeEnabled);

		boolean javaSaveActionAutoOrganizeImportsEnabled = getBoolean(configuration, JAVA_SAVE_ACTIONS_ORGANIZE_IMPORTS_KEY, false);
		prefs.setJavaSaveActionAutoOrganizeImportsEnabled(javaSaveActionAutoOrganizeImportsEnabled);

		boolean signatureHelpEnabled = getBoolean(configuration, SIGNATURE_HELP_ENABLED_KEY, true);
		prefs.setSignatureHelpEnabled(signatureHelpEnabled);

		boolean renameEnabled = getBoolean(configuration, RENAME_ENABLED_KEY, true);
		prefs.setRenameEnabled(renameEnabled);

		boolean executeCommandEnable = getBoolean(configuration, EXECUTE_COMMAND_ENABLED_KEY, true);
		prefs.setExecuteCommandEnabled(executeCommandEnable);

		boolean autobuildEnable = getBoolean(configuration, AUTOBUILD_ENABLED_KEY, true);
		prefs.setAutobuildEnabled(autobuildEnable);

		boolean completionEnable = getBoolean(configuration, COMPLETION_ENABLED_KEY, true);
		prefs.setCompletionEnabled(completionEnable);
		boolean completionOverwrite = getBoolean(configuration, JAVA_COMPLETION_OVERWRITE_KEY, true);
		prefs.setCompletionOverwrite(completionOverwrite);

		boolean foldingRangeEnable = getBoolean(configuration, FOLDINGRANGE_ENABLED_KEY, true);
		prefs.setFoldingRangeEnabled(foldingRangeEnable);

		boolean selectionRangeEnabled = getBoolean(configuration, SELECTIONRANGE_ENABLED_KEY, true);
		prefs.setSelectionRangeEnabled(selectionRangeEnabled);

		boolean guessMethodArguments = getBoolean(configuration, JAVA_COMPLETION_GUESS_METHOD_ARGUMENTS_KEY, false);
		prefs.setGuessMethodArguments(guessMethodArguments);

		boolean hashCodeEqualsTemplateUseJava7Objects = getBoolean(configuration, JAVA_CODEGENERATION_HASHCODEEQUALS_USEJAVA7OBJECTS, false);
		prefs.setHashCodeEqualsTemplateUseJava7Objects(hashCodeEqualsTemplateUseJava7Objects);
		boolean hashCodeEqualsTemplateUseInstanceof = getBoolean(configuration, JAVA_CODEGENERATION_HASHCODEEQUALS_USEINSTANCEOF, false);
		prefs.setHashCodeEqualsTemplateUseInstanceof(hashCodeEqualsTemplateUseInstanceof);
		boolean codeGenerationTemplateUseBlocks = getBoolean(configuration, JAVA_CODEGENERATION_USEBLOCKS, false);
		prefs.setCodeGenerationTemplateUseBlocks(codeGenerationTemplateUseBlocks);
		boolean codeGenerationTemplateGenerateComments = getBoolean(configuration, JAVA_CODEGENERATION_GENERATECOMMENTS, false);
		prefs.setCodeGenerationTemplateGenerateComments(codeGenerationTemplateGenerateComments);

		String generateToStringTemplate = getString(configuration, JAVA_CODEGENERATION_TOSTRING_TEMPLATE);
		prefs.setGenerateToStringTemplate(generateToStringTemplate);
		String generateToStringCodeStyle = getString(configuration, JAVA_CODEGENERATION_TOSTRING_CODESTYLE, "STRING_CONCATENATION");
		prefs.setGenerateToStringCodeStyle(generateToStringCodeStyle);

		boolean generateToStringSkipNullValues = getBoolean(configuration, JAVA_CODEGENERATION_TOSTRING_SKIPNULLVALUES, false);
		prefs.setGenerateToStringSkipNullValues(generateToStringSkipNullValues);
		boolean generateToStringListArrayContents = getBoolean(configuration, JAVA_CODEGENERATION_TOSTRING_LISTARRAYCONTENTS, true);
		prefs.setGenerateToStringListArrayContents(generateToStringListArrayContents);
		int generateToStringLimitElements = getInt(configuration, JAVA_CODEGENERATION_TOSTRING_LIMITELEMENTS, 0);
		prefs.setGenerateToStringLimitElements(generateToStringLimitElements);

		String insertionLocation = getString(configuration, JAVA_CODEGENERATION_INSERTIONLOCATION);
		prefs.setCodeGenerationInsertionLocation(insertionLocation);

		List<String> javaImportExclusions = getList(configuration, JAVA_IMPORT_EXCLUSIONS_KEY, JAVA_IMPORT_EXCLUSIONS_DEFAULT);
		if (javaImportExclusions instanceof LinkedList) {
			prefs.setJavaImportExclusions(javaImportExclusions);
		} else {
			List<String> copy = new LinkedList<>(javaImportExclusions);
			prefs.setJavaImportExclusions(copy);
		}

		Object referencedLibraries = getValue(configuration, JAVA_PROJECT_REFERENCED_LIBRARIES_KEY);
		if (referencedLibraries == null) {
			prefs.setReferencedLibraries(JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT);
		} else if (referencedLibraries instanceof Map) {
			try {
				Map<String, Object> config = (Map<String, Object>) referencedLibraries;
				Set<String> include = new HashSet<>((List<String>) config.getOrDefault("include", new ArrayList<>()));
				Set<String> exclude = new HashSet<>((List<String>) config.getOrDefault("exclude", new ArrayList<>()));
				Map<String, String> sources = (Map<String, String>) config.getOrDefault("sources", new HashMap<>());
				prefs.setReferencedLibraries(new ReferencedLibraries(include, exclude, sources));
			} catch (Exception e) {
				prefs.setReferencedLibraries(JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT);
			}
		} else { // referencedLibraries is a shortcut array to represent include patterns
			try {
				Set<String> include = new HashSet<>((List<String>) referencedLibraries);
				prefs.setReferencedLibraries(new ReferencedLibraries(include));
			} catch (Exception e) {
				prefs.setReferencedLibraries(JAVA_PROJECT_REFERENCED_LIBRARIES_DEFAULT);
			}
		}

		String invisibleProjectOutputPath = getString(configuration, JAVA_PROJECT_OUTPUT_PATH_KEY, "");
		prefs.setInvisibleProjectOutputPath(invisibleProjectOutputPath);

		List<String> invisibleProjectSourcePaths = getList(configuration, JAVA_PROJECT_SOURCE_PATHS_KEY, null);
		prefs.setInvisibleProjectSourcePaths(invisibleProjectSourcePaths);

		List<String> javaCompletionFavoriteMembers = getList(configuration, JAVA_COMPLETION_FAVORITE_MEMBERS_KEY, JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT);
		prefs.setJavaCompletionFavoriteMembers(javaCompletionFavoriteMembers);

		List<?> gradleWrapperList = getList(configuration, JAVA_GRADLE_WRAPPER_SHA256_KEY, JAVA_GRADLE_WRAPPER_SHA256_DEFAULT);
		prefs.setGradleWrapperList(gradleWrapperList);

		String mavenUserSettings = getString(configuration, MAVEN_USER_SETTINGS_KEY, null);
		prefs.setMavenUserSettings(mavenUserSettings);

		String mavenGlobalSettings = getString(configuration, MAVEN_GLOBAL_SETTINGS_KEY, null);
		prefs.setMavenGlobalSettings(mavenGlobalSettings);

		String mavenLifecycleMappingMetadata = getString(configuration, MAVEN_LIFECYCLE_MAPPING_METADATA_KEY, null);
		prefs.setWorkspaceLifecycleMappingMetadataFile(mavenLifecycleMappingMetadata);

		String sortOrder = getString(configuration, MEMBER_SORT_ORDER, null);
		prefs.setMembersSortOrder(sortOrder);

		List<String> preferredContentProviders = getList(configuration, PREFERRED_CONTENT_PROVIDER_KEY);
		prefs.setPreferredContentProviderIds(preferredContentProviders);

		String javaHome = getString(configuration, JAVA_HOME);
		prefs.setJavaHome(javaHome);

		String formatterUrl = getString(configuration, JAVA_FORMATTER_URL);
		prefs.setFormatterUrl(formatterUrl);

		String settingsUrl = getString(configuration, JAVA_SETTINGS_URL);
		prefs.setSettingsUrl(settingsUrl);

		List<String> resourceFilters = getList(configuration, JAVA_RESOURCE_FILTERS);
		prefs.setResourceFilters(resourceFilters);

		String formatterProfileName = getString(configuration, JAVA_FORMATTER_PROFILE_NAME);
		prefs.setFormatterProfileName(formatterProfileName);

		boolean javaFormatComments = getBoolean(configuration, JAVA_FORMAT_COMMENTS, true);
		prefs.setJavaFormatComments(javaFormatComments);

		List<String> javaImportOrder = getList(configuration, JAVA_IMPORT_ORDER_KEY, JAVA_IMPORT_ORDER_DEFAULT);
		prefs.setImportOrder(javaImportOrder);

		List<String> javaFilteredTypes = getList(configuration, JAVA_COMPLETION_FILTERED_TYPES_KEY, JAVA_COMPLETION_FILTERED_TYPES_DEFAULT);
		prefs.setFilteredTypes(javaFilteredTypes);

		int maxConcurrentBuilds = getInt(configuration, JAVA_MAX_CONCURRENT_BUILDS, PreferenceInitializer.PREF_MAX_CONCURRENT_BUILDS_DEFAULT);
		maxConcurrentBuilds = maxConcurrentBuilds >= 1 ? maxConcurrentBuilds : 1;
		prefs.setMaxBuildCount(maxConcurrentBuilds);

		int maxCompletions = getInt(configuration, JAVA_COMPLETION_MAX_RESULTS_KEY, JAVA_COMPLETION_MAX_RESULTS_DEFAULT);
		prefs.setMaxCompletionResults(maxCompletions);

		int onDemandThreshold = getInt(configuration, IMPORTS_ONDEMANDTHRESHOLD, IMPORTS_ONDEMANDTHRESHOLD_DEFAULT);
		prefs.setImportOnDemandThreshold(onDemandThreshold);

		int staticOnDemandThreshold = getInt(configuration, IMPORTS_STATIC_ONDEMANDTHRESHOLD, IMPORTS_STATIC_ONDEMANDTHRESHOLD_DEFAULT);
		prefs.setStaticImportOnDemandThreshold(staticOnDemandThreshold);

		List<?> runtimeList = getList(configuration, JAVA_CONFIGURATION_RUNTIMES, JAVA_CONFIGURATION_RUNTIMES_DEFAULT);
		Set<RuntimeEnvironment> runtimes = new HashSet<>();
		boolean[] hasDefault = { false };
		for (Object object : runtimeList) {
			if (object instanceof Map) {
				RuntimeEnvironment runtime = new RuntimeEnvironment();
				Map<?, ?> map = (Map<?, ?>) object;
				map.forEach((k, v) -> {
					if (k instanceof String) {
						switch ((String) k) {
							case "name":
								if (v instanceof String) {
									runtime.setName((String) v);
								}
								break;
							case "path":
								if (v instanceof String) {
									runtime.setPath(ResourceUtils.expandPath((String) v));
								}
								break;
							case "javadoc":
								if (v instanceof String) {
									runtime.setJavadoc(ResourceUtils.expandPath((String) v));
								}
								break;
							case "sources":
								if (v instanceof String) {
									runtime.setSources(ResourceUtils.expandPath((String) v));
								}
								break;
							case "default":
								if (!hasDefault[0]) {
									if (v instanceof Boolean) {
										runtime.setDefault((Boolean) v);
									}
									hasDefault[0] = true;
								}
								break;
							default:
								break;
						}
					}
				});
				if (!runtimes.contains(runtime)) {
					if (runtime.isValid()) {
						runtimes.add(runtime);
					} else {
						JavaLanguageServerPlugin.logInfo("Runtime " + runtime + " is not valid.");
					}
				} else {
					JavaLanguageServerPlugin.logInfo("Multiple runtimes with name " + runtime.getName());
				}
			}
		}
		prefs.setRuntimes(runtimes);

		List<String> fileHeader = getList(configuration, JAVA_TEMPLATES_FILEHEADER);
		prefs.setFileHeaderTemplate(fileHeader);
		List<String> typeComment = getList(configuration, JAVA_TEMPLATES_TYPECOMMENT);
		prefs.setTypeCommentTemplate(typeComment);
		boolean includeAccessors = getBoolean(configuration, JAVA_REFERENCES_INCLUDE_ACCESSORS, true);
		prefs.setIncludeAccessors(includeAccessors);
		boolean includeDecompiledSources = getBoolean(configuration, JAVA_REFERENCES_INCLUDE_DECOMPILED_SOURCES, true);
		prefs.setIncludeDecompiledSources(includeDecompiledSources);
		boolean includeSourceMethodDeclarations = getBoolean(configuration, JAVA_SYMBOLS_INCLUDE_SOURCE_METHOD_DECLARATIONS, false);
		prefs.setIncludeSourceMethodDeclarations(includeSourceMethodDeclarations);
		return prefs;
	}

	public Preferences setJavaHome(String javaHome) {
		this.javaHome = javaHome;
		return this;
	}

	public Preferences setGradleVersion(String gradleVersion) {
		this.gradleVersion = (gradleVersion == null || gradleVersion.isEmpty()) ? null : gradleVersion;
		return this;
	}

	public Preferences setGradleArguments(List<String> arguments) {
		this.gradleArguments = arguments == null ? new ArrayList<>() : arguments;
		return this;
	}

	public Preferences setGradleJvmArguments(List<String> jvmArguments) {
		this.gradleJvmArguments = jvmArguments == null ? new ArrayList<>() : jvmArguments;
		return this;
	}

	public Preferences setGradleHome(String gradleHome) {
		this.gradleHome = ResourceUtils.expandPath(gradleHome);
		return this;
	}

	public Preferences setGradleJavaHome(String gradleJavaHome) {
		this.gradleJavaHome = ResourceUtils.expandPath(gradleJavaHome);
		return this;
	}

	public Preferences setGradleUserHome(String gradleUserHome) {
		this.gradleUserHome = ResourceUtils.expandPath(gradleUserHome);
		return this;
	}

	public Preferences setFormatterUrl(String formatterUrl) {
		this.formatterUrl = ResourceUtils.expandPath(formatterUrl);
		return this;
	}

	public Preferences setSettingsUrl(String settingsUrl) {
		this.settingsUrl = ResourceUtils.expandPath(settingsUrl);
		return this;
	}

	public Preferences setResourceFilters(List<String> resourceFilters) {
		this.resourceFilters = resourceFilters == null ? new ArrayList<>() : resourceFilters;
		return this;
	}

	public Preferences setJavaFormatComments(boolean javaFormatComments) {
		this.javaFormatComments = javaFormatComments;
		return this;
	}

	public boolean isJavaFormatComments() {
		return this.javaFormatComments;
	}

	public Preferences setFormatterProfileName(String formatterProfileName) {
		this.formatterProfileName = formatterProfileName;
		return this;
	}

	public Preferences setJavaImportExclusions(List<String> javaImportExclusions) {
		this.javaImportExclusions = javaImportExclusions;
		return this;
	}

	public Preferences setJavaCompletionFavoriteMembers(List<String> javaCompletionFavoriteMembers) {
		this.javaCompletionFavoriteMembers = (javaCompletionFavoriteMembers == null || javaCompletionFavoriteMembers.isEmpty()) ? JAVA_COMPLETION_FAVORITE_MEMBERS_DEFAULT : javaCompletionFavoriteMembers;
		return this;
	}

	private Preferences setMembersSortOrder(String sortOrder) {
		if (sortOrder != null) {
			IEclipsePreferences fPreferenceStore = InstanceScope.INSTANCE.getNode(IConstants.PLUGIN_ID);
			fPreferenceStore.put(MembersOrderPreferenceCacheCommon.APPEARANCE_MEMBER_SORT_ORDER, sortOrder);
		}
		return this;
	}

	private Preferences setPreferredContentProviderIds(List<String> preferredContentProviderIds) {
		this.preferredContentProviderIds = preferredContentProviderIds;
		return this;
	}

	private Preferences setReferencesCodelensEnabled(boolean enabled) {
		this.referencesCodeLensEnabled = enabled;
		return this;
	}

	public Preferences setImportGradleEnabled(boolean enabled) {
		this.importGradleEnabled = enabled;
		return this;
	}

	public Preferences setImportGradleOfflineEnabled(boolean enabled) {
		this.importGradleOfflineEnabled = enabled;
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("org.eclipse.buildship.core");
		boolean offlineMode = prefs.getBoolean(GRADLE_OFFLINE_MODE, false);
		if (offlineMode != enabled) {
			prefs.putBoolean(GRADLE_OFFLINE_MODE, enabled);
		}
		return this;
	}

	public Preferences setGradleWrapperEnabled(boolean enabled) {
		this.gradleWrapperEnabled = enabled;
		return this;
	}

	public Preferences setImportMavenEnabled(boolean enabled) {
		this.importMavenEnabled = enabled;
		return this;
	}

	public Preferences setMavenDownloadSources(boolean enabled) {
		this.mavenDownloadSources = enabled;
		return this;
	}

	public Preferences setMavenUpdateSnapshots(boolean enabled) {
		this.mavenUpdateSnapshots = enabled;
		return this;
	}

	private Preferences setSignatureHelpEnabled(boolean enabled) {
		this.signatureHelpEnabled = enabled;
		return this;
	}

	private Preferences setImplementationCodelensEnabled(boolean enabled) {
		this.implementationsCodeLensEnabled = enabled;
		return this;
	}

	private Preferences setRenameEnabled(boolean enabled) {
		this.renameEnabled = enabled;
		return this;
	}

	private Preferences setExecuteCommandEnabled(boolean enabled) {
		this.executeCommandEnabled = enabled;
		return this;
	}

	public Preferences setAutobuildEnabled(boolean enabled) {
		this.autobuildEnabled = enabled;
		return this;
	}

	public Preferences setCompletionEnabled(boolean enabled) {
		this.completionEnabled = enabled;
		return this;
	}

	public Preferences setCompletionOverwrite(boolean completionOverwrite) {
		this.completionOverwrite = completionOverwrite;
		return this;
	}

	public Preferences setFoldingRangeEnabled(boolean enabled) {
		this.foldingRangeEnabled = enabled;
		return this;
	}

	public Preferences setSelectionRangeEnabled(boolean enabled) {
		this.selectionRangeEnabled = enabled;
		return this;
	}

	public Preferences setGuessMethodArguments(boolean guessMethodArguments) {
		this.guessMethodArguments = guessMethodArguments;
		return this;
	}

	public Preferences setJavaFormatEnabled(boolean enabled) {
		this.javaFormatEnabled = enabled;
		return this;
	}

	public Preferences setJavaSaveActionAutoOrganizeImportsEnabled(boolean javaSaveActionAutoOrganizeImportsEnabled) {
		this.javaSaveActionsOrganizeImportsEnabled = javaSaveActionAutoOrganizeImportsEnabled;
		return this;
	}

	public Preferences setHashCodeEqualsTemplateUseJava7Objects(boolean hashCodeEqualsTemplateUseJ7Objects) {
		this.hashCodeEqualsTemplateUseJava7Objects = hashCodeEqualsTemplateUseJ7Objects;
		return this;
	}

	public Preferences setHashCodeEqualsTemplateUseInstanceof(boolean hashCodeEqualsTemplateUseInstanceof) {
		this.hashCodeEqualsTemplateUseInstanceof = hashCodeEqualsTemplateUseInstanceof;
		return this;
	}

	public Preferences setCodeGenerationTemplateUseBlocks(boolean codeGenerationTemplateUseBlocks) {
		this.codeGenerationTemplateUseBlocks = codeGenerationTemplateUseBlocks;
		return this;
	}

	public Preferences setCodeGenerationTemplateGenerateComments(boolean codeGenerationTemplateGenerateComments) {
		this.codeGenerationTemplateGenerateComments = codeGenerationTemplateGenerateComments;
		return this;
	}

	public Preferences setGenerateToStringTemplate(String generateToStringTemplate) {
		this.generateToStringTemplate = generateToStringTemplate;
		return this;
	}

	public Preferences setGenerateToStringCodeStyle(String generateToStringCodeStyle) {
		this.generateToStringCodeStyle = generateToStringCodeStyle;
		return this;
	}

	public Preferences setGenerateToStringSkipNullValues(boolean generateToStringSkipNullValues) {
		this.generateToStringSkipNullValues = generateToStringSkipNullValues;
		return this;
	}

	public Preferences setGenerateToStringListArrayContents(boolean generateToStringListArrayContents) {
		this.generateToStringListArrayContents = generateToStringListArrayContents;
		return this;
	}

	public Preferences setGenerateToStringLimitElements(int generateToStringLimitElements) {
		this.generateToStringLimitElements = generateToStringLimitElements;
		return this;
	}

	public Preferences setUpdateBuildConfigurationStatus(FeatureStatus status) {
		this.updateBuildConfigurationStatus = status;
		return this;
	}

	private Preferences setIncompleteClasspathSeverity(Severity severity) {
		this.incompleteClasspathSeverity = severity;
		return this;
	}

	public Preferences setImportOrder(List<String> importOrder) {
		this.importOrder = (importOrder == null || importOrder.size() == 0) ? JAVA_IMPORT_ORDER_DEFAULT : importOrder;
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(IConstants.PLUGIN_ID);
		pref.put(CodeStyleConfiguration.ORGIMPORTS_IMPORTORDER, String.join(";", importOrder));
		return this;
	}

	public Preferences setFilteredTypes(List<String> filteredTypes) {
		this.filteredTypes = (filteredTypes == null) ? Collections.emptyList() : filteredTypes;
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(IConstants.PLUGIN_ID);
		pref.put(TypeFilter.TYPEFILTER_ENABLED, String.join(";", filteredTypes));
		JavaLanguageServerPlugin.getInstance().getTypeFilter().dispose();
		return this;
	}

	public Preferences setMaxBuildCount(int maxConcurrentBuilds) {
		this.parallelBuildsCount = maxConcurrentBuilds;
		return this;
	}

	public Severity getIncompleteClasspathSeverity() {
		return incompleteClasspathSeverity;
	}

	public FeatureStatus getUpdateBuildConfigurationStatus() {
		return updateBuildConfigurationStatus;
	}

	public List<String> getJavaImportExclusions() {
		return javaImportExclusions;
	}

	public String[] getJavaCompletionFavoriteMembers() {
		return javaCompletionFavoriteMembers.toArray(new String[0]);
	}

	public String getJavaHome() {
		return javaHome;
	}

	public String getGradleVersion() {
		return gradleVersion;
	}

	public List<String> getGradleArguments() {
		return gradleArguments == null ? new ArrayList<>() : gradleArguments;
	}

	public List<String> getGradleJvmArguments() {
		return gradleJvmArguments == null ? new ArrayList<>() : gradleJvmArguments;
	}

	public String getGradleHome() {
		return gradleHome;
	}

	public String getGradleJavaHome() {
		return gradleJavaHome;
	}

	public String getGradleUserHome() {
		return gradleUserHome;
	}

	public String getFormatterUrl() {
		return formatterUrl;
	}

	public URI getFormatterAsURI() {
		return asURI(formatterUrl);
	}

	public String getSettingsUrl() {
		return settingsUrl;
	}

	public URI getSettingsAsURI() {
		return asURI(settingsUrl);
	}

	private URI asURI(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		try {
			URI uri = new URI(ResourceUtils.toClientUri(url));
			if (!uri.isAbsolute()) {
				return getURI(url);
			}
			return uri;
		} catch (URISyntaxException e1) {
			return getURI(url);
		}
	}

	private URI getURI(String path) {
		File file = findFile(path);
		if (file != null && file.isFile()) {
			return file.toURI();
		}
		return null;
	}

	private File findFile(String path) {
		File file = new File(path);
		if (file.exists()) {
			return file;
		}
		Collection<IPath> rootPaths = getRootPaths();
		if (rootPaths != null) {
			for (IPath rootPath : rootPaths) {
				File f = new File(rootPath.toOSString(), path);
				if (f.isFile()) {
					return f;
				}
			}
		}
		return null;
	}

	public List<String> getResourceFilters() {
		return resourceFilters;
	}

	public String getFormatterProfileName() {
		return formatterProfileName;
	}

	public List<String> getPreferredContentProviderIds() {
		return this.preferredContentProviderIds;
	}

	public boolean isReferencesCodeLensEnabled() {
		return referencesCodeLensEnabled;
	}

	public boolean isImportGradleEnabled() {
		return importGradleEnabled;
	}

	public boolean isImportGradleOfflineEnabled() {
		return importGradleOfflineEnabled;
	}

	public boolean isGradleWrapperEnabled() {
		return gradleWrapperEnabled;
	}

	public boolean isImportMavenEnabled() {
		return importMavenEnabled;
	}

	public boolean isMavenDownloadSources() {
		return mavenDownloadSources;
	}

	public boolean isMavenUpdateSnapshots() {
		return mavenUpdateSnapshots;
	}

	public boolean isImplementationsCodeLensEnabled() {
		return implementationsCodeLensEnabled;
	}

	public boolean isCodeLensEnabled() {
		return referencesCodeLensEnabled || implementationsCodeLensEnabled;
	}

	public boolean isJavaFormatEnabled() {
		return javaFormatEnabled;
	}

	public boolean isJavaSaveActionsOrganizeImportsEnabled() {
		return javaSaveActionsOrganizeImportsEnabled;
	}

	public boolean isSignatureHelpEnabled() {
		return signatureHelpEnabled;
	}

	public boolean isRenameEnabled() {
		return renameEnabled;
	}

	public boolean isExecuteCommandEnabled() {
		return executeCommandEnabled;
	}

	public boolean isAutobuildEnabled() {
		return autobuildEnabled;
	}

	public boolean isCompletionEnabled() {
		return completionEnabled;
	}

	public boolean isCompletionOverwrite() {
		return completionOverwrite;
	}

	public boolean isFoldingRangeEnabled() {
		return foldingRangeEnabled;
	}

	public boolean isSelectionRangeEnabled() {
		return selectionRangeEnabled;
	}

	public boolean isGuessMethodArguments() {
		return guessMethodArguments;
	}

	public boolean isHashCodeEqualsTemplateUseJava7Objects() {
		return hashCodeEqualsTemplateUseJava7Objects;
	}

	public boolean isHashCodeEqualsTemplateUseInstanceof() {
		return hashCodeEqualsTemplateUseInstanceof;
	}

	public boolean isCodeGenerationTemplateUseBlocks() {
		return codeGenerationTemplateUseBlocks;
	}

	public boolean isCodeGenerationTemplateGenerateComments() {
		return codeGenerationTemplateGenerateComments;
	}

	public String getGenerateToStringTemplate() {
		return generateToStringTemplate;
	}

	public String getGenerateToStringCodeStyle() {
		return generateToStringCodeStyle;
	}

	public boolean isGenerateToStringSkipNullValues() {
		return generateToStringSkipNullValues;
	}

	public boolean isGenerateToStringListArrayContents() {
		return generateToStringListArrayContents;
	}

	public int getGenerateToStringLimitElements() {
		return generateToStringLimitElements;
	}

	public String getCodeGenerationInsertionLocation() {
		return codeGenerationInsertionLocation;
	}

	public void setCodeGenerationInsertionLocation(String insertionLocation) {
		this.codeGenerationInsertionLocation = insertionLocation;
	}

	public Preferences setMavenUserSettings(String mavenUserSettings) {
		this.mavenUserSettings = ResourceUtils.expandPath(mavenUserSettings);
		return this;
	}

	public String getMavenUserSettings() {
		return mavenUserSettings;
	}

	public Preferences setMavenGlobalSettings(String mavenGlobalSettings) {
		this.mavenGlobalSettings = ResourceUtils.expandPath(mavenGlobalSettings);
		return this;
	}

	public String getMavenGlobalSettings() {
		return mavenGlobalSettings;
	}

	public Preferences setWorkspaceLifecycleMappingMetadataFile(String mavenLifecycleMetadataMapping) {
		this.workspaceLifecycleMappingMetadataFile = ResourceUtils.expandPath(mavenLifecycleMetadataMapping);
		return this;
	}

	public String getWorkspaceLifecycleMappingMetadataFile() {
		return workspaceLifecycleMappingMetadataFile;
	}

	public String[] getImportOrder() {
		return this.importOrder == null ? new String[0] : this.importOrder.toArray(new String[importOrder.size()]);
	}

	public String[] getFilteredTypes() {
		return this.filteredTypes == null ? new String[0] : this.filteredTypes.toArray(new String[filteredTypes.size()]);
	}

	public int getMaxConcurrentBuilds() {
		return parallelBuildsCount;
	}

	public Map<String, Object> asMap() {
		if (configuration == null) {
			return null;
		}
		return Collections.unmodifiableMap(configuration);
	}

	public Preferences setRootPaths(Collection<IPath> rootPaths) {
		this.rootPaths = rootPaths;
		return this;
	}

	public Collection<IPath> getRootPaths() {
		return rootPaths;
	}

	public Preferences setTriggerFiles(Collection<IPath> triggerFiles) {
		this.triggerFiles = triggerFiles;
		return this;
	}

	public Collection<IPath> getTriggerFiles() {
		return triggerFiles;
	}

	public boolean isJavaFormatOnTypeEnabled() {
		return javaFormatOnTypeEnabled;
	}

	public Preferences setJavaFormatOnTypeEnabled(boolean javaFormatOnTypeEnabled) {
		this.javaFormatOnTypeEnabled = javaFormatOnTypeEnabled;
		return this;
	}

	public int getMaxCompletionResults() {
		return maxCompletionResults;
	}

	/**
	 * Sets the maximum number of completion results (excluding snippets and Javadoc
	 * proposals). If maxCompletions is set to 0 or lower, then the completion limit
	 * is considered disabled, which could certainly severly impact performance in a
	 * negative way.
	 *
	 * @param maxCompletions
	 */
	public Preferences setMaxCompletionResults(int maxCompletions) {
		if (maxCompletions < 1) {
			this.maxCompletionResults = Integer.MAX_VALUE;
		} else {
			this.maxCompletionResults = maxCompletions;
		}
		return this;
	}

	public ReferencedLibraries getReferencedLibraries() {
		return referencedLibraries;
	}

	public Preferences setReferencedLibraries(ReferencedLibraries referencedLibraries) {
		this.referencedLibraries = referencedLibraries;
		return this;
	}

	public Set<RuntimeEnvironment> getRuntimes() {
		return runtimes;
	}

	public Preferences setRuntimes(Set<RuntimeEnvironment> runtimes) {
		this.runtimes = runtimes;
		return this;
	}

	public int getImportOnDemandThreshold() {
		return importOnDemandThreshold;
	}

	public Preferences setImportOnDemandThreshold(int importOnDemandThreshold) {
		if (importOnDemandThreshold <= 0) {
			this.importOnDemandThreshold = IMPORTS_ONDEMANDTHRESHOLD_DEFAULT;
		} else {
			this.importOnDemandThreshold = importOnDemandThreshold;
		}
		IEclipsePreferences defEclipsePrefs = DefaultScope.INSTANCE.getNode(IConstants.PLUGIN_ID);
		defEclipsePrefs.put(CodeStyleConfiguration.ORGIMPORTS_ONDEMANDTHRESHOLD, String.valueOf(this.importOnDemandThreshold));
		return this;
	}

	public int getStaticImportOnDemandThreshold() {
		return staticImportOnDemandThreshold;
	}

	public Preferences setStaticImportOnDemandThreshold(int staticImportOnDemandThreshold) {
		if (staticImportOnDemandThreshold <= 0) {
			this.staticImportOnDemandThreshold = IMPORTS_STATIC_ONDEMANDTHRESHOLD_DEFAULT;
		} else {
			this.staticImportOnDemandThreshold = staticImportOnDemandThreshold;
		}
		IEclipsePreferences defEclipsePrefs = DefaultScope.INSTANCE.getNode(IConstants.PLUGIN_ID);
		defEclipsePrefs.put(CodeStyleConfiguration.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, String.valueOf(this.staticImportOnDemandThreshold));
		return this;
	}

	public Preferences setGradleWrapperList(List<?> gradleWrapperList) {
		this.gradleWrapperList = gradleWrapperList;
		return this;
	}

	public List<?> getGradleWrapperList() {
		return this.gradleWrapperList == null ? Collections.emptyList() : this.gradleWrapperList;
	}

	public List<String> getFileHeaderTemplate() {
		return fileHeaderTemplate;
	}

	public Preferences setFileHeaderTemplate(List<String> fileHeaderTemplate) {
		this.fileHeaderTemplate = fileHeaderTemplate;
		return this;
	}

	public List<String> getTypeCommentTemplate() {
		return typeCommentTemplate;
	}

	public Preferences setTypeCommentTemplate(List<String> typeCommentTemplate) {
		this.typeCommentTemplate = typeCommentTemplate;
		return this;
	}

	public Preferences setIncludeAccessors(boolean includeAccessors) {
		this.includeAccessors = includeAccessors;
		return this;
	}

	public boolean isIncludeAccessors() {
		return this.includeAccessors;
	}
	public boolean isEclipseDownloadSources() {
		return eclipseDownloadSources;
	}

	public Preferences setEclipseDownloadSources(boolean enabled) {
		this.eclipseDownloadSources = enabled;
		return this;
	}

	public String getInvisibleProjectOutputPath() {
		return invisibleProjectOutputPath;
	}

	public void setInvisibleProjectOutputPath(String invisibleProjectOutputPath) {
		this.invisibleProjectOutputPath = ResourceUtils.expandPath(invisibleProjectOutputPath);
	}

	public List<String> getInvisibleProjectSourcePaths() {
		return invisibleProjectSourcePaths;
	}

	public void setInvisibleProjectSourcePaths(List<String> invisibleProjectSourcePaths) {
		if (invisibleProjectSourcePaths != null) {
			this.invisibleProjectSourcePaths = new ArrayList<>();
			for (String path : invisibleProjectSourcePaths) {
				this.invisibleProjectSourcePaths.add(ResourceUtils.expandPath(path));
			}
		} else {
			this.invisibleProjectSourcePaths = invisibleProjectSourcePaths;
		}
	}

	public Preferences setIncludeDecompiledSources(boolean includeDecompiledSources) {
		this.includeDecompiledSources = includeDecompiledSources;
		return this;
	}

	public boolean isIncludeDecompiledSources() {
		return this.includeDecompiledSources;
	}

	public boolean isIncludeSourceMethodDeclarations() {
		return this.includeSourceMethodDeclarations;
	}

	public void setIncludeSourceMethodDeclarations(boolean includeSourceMethodDeclarations) {
		this.includeSourceMethodDeclarations = includeSourceMethodDeclarations;
	}

	public Preferences setInsertSpaces(boolean insertSpaces) {
		this.insertSpaces = insertSpaces;
		return this;
	}

	public Preferences setTabSize(int tabSize) {
		this.tabSize = tabSize;
		return this;
	}

	public boolean isInsertSpaces() {
		return insertSpaces;
	}

	public int getTabSize() {
		return tabSize;
	}

	public void updateTabSizeInsertSpaces(Hashtable<String, String> options) {
		if (options == null) {
			return;
		}
		if (tabSize > 0) {
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, String.valueOf(tabSize));
		}
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, insertSpaces ? JavaCore.SPACE : JavaCore.TAB);
	}

}
