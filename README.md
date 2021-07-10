[![Build Status](https://ci.eclipse.org/ls/buildStatus/icon?job=jdt-ls-master)](https://ci.eclipse.org/ls/job/jdt-ls-master)

Eclipse JDT Language Server
===========================

The Eclipse JDT Language Server is a Java language specific implementation of the [Language Server Protocol](https://github.com/Microsoft/language-server-protocol)
and can be used with any editor that supports the protocol, to offer good support for the Java Language. The server is based on:

* [Eclipse LSP4J](https://github.com/eclipse/lsp4j), the Java binding for the Language Server Protocol,
* [Eclipse JDT](http://www.eclipse.org/jdt/), which provides Java support (code completion, references, diagnostics...),
* [M2Eclipse](http://www.eclipse.org/m2e/), which provides Maven support,
* [Buildship](https://github.com/eclipse/buildship), which provides Gradle support.

Features
--------------
* Supports compiling projects from Java 1.5 through 16
* Maven pom.xml project support
* Limited Gradle support (Android projects are not supported)
* Standalone Java files support
* As-you-type reporting of syntax and compilation errors
* Code completion
* Javadoc hovers
* Organize imports
* Type search
* Code actions (quick fixes, source actions & refactorings)
* Code outline
* Code folding
* Code navigation
* Code lens (references/implementations)
* Code formatting (on-type/selection/file)
* Code snippets
* Highlights (semantic highlighting)
* Semantic selection
* Diagnostic tags
* Call Hierarchy
* Type Hierarchy
* Annotation processing support (automatic for Maven projects)
* Automatic source resolution for classes in jars with maven coordinates
* Extensibility


First Time Setup
--------------
**Pre-requisite: Java 11 must be installed on your machine and configured in Eclipse.**

0. Fork and clone the repository
1. Install [Eclipse IDE for Eclipse Committers](https://www.eclipse.org/downloads/packages/release/2018-09/r/eclipse-ide-eclipse-committers) that will have the most needed plugins already installed. Alternatively,
you can get the [Eclipse IDE for Java developers](https://www.eclipse.org/downloads/packages/release/2018-09/r/eclipse-ide-java-developers)
and just install Eclipse PDE from the Eclipse Marketplace.

2. Once installed use `File > Open Projects from File System...` and
point it at `eclipse.jdt.ls` and Eclipse should automatically
detect the projects and import it properly.

3. If, after importing the projects, you see an error on `pom.xml` about Tycho, you can use Quick Fix
(Ctrl+1) to install the Tycho maven integration.

4. At that point, some plug-ins should still be missing in order to build the project. You can either open `org.eclipse.jdt.ls.target/org.eclipse.jdt.ls.tp.target` in the Target Editor (which is the default editor) and click on `Set Target Platform`, or alternatively, open `Preferences > Plug-in Development > Target Platform` and select `Java Language Server Target Definition`). Eclipse will take some time to download all the required dependencies. It should then be able to compile all the projects in the workspace.

Building from the command line
----------------------------
**Pre-requisite: Java 11 must be installed on your machine and accessible in the PATH.**

The following command will install [Apache Maven](https://maven.apache.org/) if necessary, then build the server into the  `/org.eclipse.jdt.ls.product/target/repository` folder:

```bash
    $ ./mvnw clean verify
````

Running from the command line
------------------------------
1. Choose a connection type from "Managing connection types" section below, and then set those environment variables in your terminal or specify them as system properties with `-D` prior to continuing

2. Make sure to build the server using the steps above in the "Building from command line" section

3. `cd` into the build directory of the project: `/org.eclipse.jdt.ls.product/target/repository`

4. Prior to starting the server, make sure that your socket (TCP or sock file) server is running for both the IN and OUT sockets. You will get an error if the JDT server cannot connect on your ports/files specified in the environment variables

5. To start the server in the active terminal, run:
```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -Declipse.application=org.eclipse.jdt.ls.core.id1 -Dosgi.bundles.defaultStartLevel=4 -Declipse.product=org.eclipse.jdt.ls.core.product -Dlog.level=ALL -noverify -Xmx1G -jar ./plugins/org.eclipse.equinox.launcher_1.5.200.v20180922-1751.jar -configuration ./config_linux -data /path/to/data --add-modules=ALL-SYSTEM --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED
```

6. Choosing a value for `-configuration`: this is the path to your platform's configuration directory. For linux, use `./config_linux`. For windows, use `./config_win`. For mac/OS X, use `./config_mac`.

7. Choosing a value for `-data`: the value for your data directory, should be the directory where your active workspace is, and you wish for the java langserver to add in its default files. Should also be the absolute path to this directory, ie., /home/username/workspace

8. Notes about debugging: the `-agentlib:` is for connecting a java debugger agent to the process, and if you wish to debug the server from the start of execution, set `suspend=y` so that the JVM will wait for your debugger prior to starting the server

9. Notes on jar versions: the full name of the build jar file above, `org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar`, may change incrementally as the project version changes. If java complains about jar not found, then look for the latest version of the `org.eclipse.equinox.launcher_*` jar in the `/org.eclipse.jdt.ls.product/target/repository/plugins` directory and replace it in the command after the `-jar`

Customize compiler option and code assits 
------------------------------------------
You could customize compiler options and code assists of the server.

Under your project folder, modify the file .settings/org.eclipse.jdt.core.prefs with options presented at https://help.eclipse.org/neon/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/JavaCore.html .

Managing connection types
-------------------------
The Java Language server supports sockets, named pipes, and standard streams of the server process
to communicate with the client. Client can communicate its preferred connection methods
by setting up environment variables or alternatively using system properties (e.g. `-DCLIENT_PORT=...`)

* To use a **plain socket**, set the following environment variables or system properties before starting the server:
   * `CLIENT_PORT`: the port of the socket to connect to
   * `CLIENT_HOST`: the host name to connect to. If not set, defaults to `localhost`.

   The connection will be used for in and output.

* To use standard streams(stdin, stdout) of the server process do not set any
of the above environment variables and the server will fall back to standard streams.

For socket and named pipes, the client is expected to create the connections
and wait for the server to connect.


Feedback
---------

* File a bug in [GitHub Issues](https://github.com/eclipse/eclipse.jdt.ls/issues).
* Join the discussion on our [Mattermost channel](https://mattermost.eclipse.org/eclipse/channels/eclipsejdtls)
* [Tweet](https://twitter.com/GorkemErcan) [us](https://twitter.com/fbricon) with other feedback.

Clients
-------
This repository only contains the server implementation. Here are some known clients consuming this server:

* [vscode-java](https://github.com/redhat-developer/vscode-java) : an extension for Visual Studio Code
* [ide-java](https://github.com/atom/ide-java) : an extension for Atom
* [ycmd](https://github.com/Valloric/ycmd) : a code-completion and code-comprehension server for multiple clients
* [Oni](https://github.com/onivim/oni/wiki/Language-Support#java) : modern modal editing - powered by Neovim.
* [LSP Java](https://github.com/emacs-lsp/lsp-java) : a Java LSP client for Emacs
* [Eclipse Theia](https://github.com/theia-ide/theia) : Theia is a cloud & desktop IDE framework implemented in TypeScript
* [coc-java](https://github.com/neoclide/coc-java) : an extension for [coc.nvim](https://github.com/neoclide/coc.nvim)
* [MS Paint IDE](https://github.com/MSPaintIDE/MSPaintIDE) : an IDE for programming in MS Paint
* [nvim-jdtls](https://github.com/mfussenegger/nvim-jdtls) : an extension for Neovim

Continuous Integration Builds
-----------------------------
Our [CI server](https://ci.eclipse.org/ls/) publishes the server binaries to [http://download.eclipse.org/jdtls/snapshots/](http://download.eclipse.org/jdtls/snapshots/?d).

P2 repositories are available under [http://download.eclipse.org/jdtls/snapshots/repository/](http://download.eclipse.org/jdtls/snapshots/repository?d).

Milestone builds are available under [http://download.eclipse.org/jdtls/milestones/](http://download.eclipse.org/jdtls/milestones/?d).

License
-------
EPL 2.0, See [LICENSE](LICENSE) file.
