/*
 * Copyright (c) 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.lang.reflect.Modifier

/**
 * @author <a href='mailto:donbeave@gmail.com'>Alexey Zhokhov</a>
 */
includeTargets << grailsScript('_GrailsEvents')

grailsSrcPath = 'src/java'
gwtSrcPath = 'src/gwt'

/**
 * Takes a fully qualified class name and returns the package name and
 * simple class name as a pair (in that order). If the class name does
 * not include a package, then the package part is <tt>null</tt>.
 */
packageAndName = { String fullClassName ->
    def name = fullClassName
    def pkg = null
    def pos = fullClassName.lastIndexOf('.')
    if (pos != -1) {
        // Extract the name and the package.
        pkg = fullClassName.substring(0, pos)
        name = fullClassName.substring(pos + 1)
    }

    [pkg, name]
}

/**
 * Converts a package name (with '.' separators) to a file path (with
 * '/' separators). If the package is <tt>null</tt>, this returns an
 * empty string.
 */
packageToPath = { String pkg ->
    pkg != null ? '/' + pkg.replace('.' as char, '/' as char) : ''
}

pathToClass = { String path ->
    if (path.contains(grailsSrcPath))
        path = path.substring(path.indexOf(grailsSrcPath) + grailsSrcPath.length())
    if (path.contains(gwtSrcPath))
        path = path.substring(path.indexOf(gwtSrcPath) + gwtSrcPath.length())
    if (path.startsWith('/'))
        path = path.replaceFirst('/', '')
    path = path.replace('.java', '')
    path != null ? path.replace('/' as char, '.' as char) : ''
}

/**
 * Installs a template file using the given arguments to populate the
 * template and determine where it goes.
 */
installFile = { File targetFile, File templateFile, Map tokens ->
    // Check whether the target file exists already.
    if (targetFile.exists()) {
        // It does, so find out whether the user wants to overwrite
        // the existing copy.
        ant.input(
                addProperty: "${targetFile.name}.overwrite",
                message: "GWTP: ${targetFile.name} already exists. Overwrite? [y/n]")

        if (ant.antProject.properties."${targetFile.name}.overwrite" == 'n') {
            // User doesn't want to overwrite, so stop the script.
            return
        }
    }

    // Now copy over the template file and replace the various tokens
    // with the appropriate values.
    ant.copy(file: templateFile, tofile: targetFile, overwrite: true)
    ant.replace(file: targetFile) {
        tokens.each { key, value ->
            ant.replacefilter(token: "@${key}@", value: value)
        }
    }

    // The file was created.
    event('CreatedFile', [targetFile])
}

/**
 *
 */
installGwtpTemplate = { String pkg, String name, String templateName, String targetName, Map data = null,
                        String srcDir = gwtSrcPath ->
    // First, work out the name of the target file from the name of the
    // template.
    def targetFile = new File("${basedir}/${srcDir}${packageToPath(pkg)}", targetName ?: templateName)
    def templateFile = new File("${gwtpPluginDir}/src/templates/artifacts", templateName)

    def mapping = ['artifact.package': pkg, 'artifact.name': name]
    if (data)
        mapping.putAll(data)

    installFile(targetFile, templateFile, mapping)
}

findNameTokens = {
    def classes = []
    def files = []

    new File(grailsSrcPath).eachDirRecurse() { dir ->
        dir.eachFileMatch(~/.*NameTokens.java/) { file ->
            files.add(file)
        }
    }
    new File(gwtSrcPath).eachDirRecurse() { dir ->
        dir.eachFileMatch(~/.*NameTokens.java/) { file ->
            files.add(file)
        }
    }

    files.each {
        classes.add(pathToClass(it.absolutePath))
    }

    classes
}

installGwtpPresenter = { modulePackage, moduleName, nameToken, revealType, codeSplit, uiHandlers ->
    def mapping = ['artifact.imports'       : '', 'artifact.revealSlot': 'Root', 'artifact.presenterImplements': '',
                   'artifact.viewImplements': '', 'artifact.initContent': '']

    // use proxy place
    boolean useToken = nameToken != null ? true : grailsConsole.userInput('Is a Place? ', ['y', 'n']) == 'y'

    if (useToken) {
        def tokenFiles = findNameTokens()

        if (!tokenFiles) {
            println 'NameTokens class not found.'
            exit(1)
        }

        while (!nameToken) {
            tokenFiles.each { tokenClass ->
                List tokens = classLoader.loadClass(tokenClass, true).getDeclaredFields().findAll {
                    Modifier.isStatic(it.getModifiers()) && it.genericType.typeName.equals('java.lang.String')
                }.collect { it.name }
                tokens << '->'

                if (!nameToken) {
                    nameToken = grailsConsole.userInput("[${tokenClass}] Select name token: ", tokens)

                    if (nameToken.equals('->'))
                        nameToken = null
                }

                if (nameToken)
                    mapping.'artifact.imports' += "import ${tokenClass};\n"
            }
        }

        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.annotations.NameToken;\n' +
                'import com.gwtplatform.mvp.client.proxy.ProxyPlace;\n'
    } else {
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.proxy.Proxy;\n'
    }

    // reveal type
    if (revealType == null) {
        mapping.'artifact.revealSlot' =
                grailsConsole.userInput('Reveal In? ', ['Root', 'RootLayout', 'RootPopup', 'SLOT'])
    }

    if (mapping.'artifact.revealSlot'.equals('SLOT')) {
        // TODO please select one slot from list
        println 'Not implemented.'
        exit(1)
    } else {
        mapping.'artifact.revealSlot' = "RevealType.${mapping.'artifact.revealSlot'}"
    }

    // UiHandlers
    if (uiHandlers) {
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.HasUiHandlers;\n'
        mapping.'artifact.presenterImplements' = " implements ${moduleName}UiHandlers"
        mapping.'artifact.viewImplements' = ", HasUiHandlers<${moduleName}UiHandlers>"
        mapping.'artifact.initContent' = 'getView().setUiHandlers(this);'
    }

    // code split
    String proxyAnnotation = 'ProxyStandard'
    if (codeSplit) {
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;'
        proxyAnnotation = 'ProxyCodeSplit'
    } else {
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.annotations.ProxyStandard;'
    }

    String proxy = "    @${proxyAnnotation}\n" +
            (nameToken ? "    @NameToken(NameTokens.${nameToken})\n" : '') +
            "    interface MyProxy extends Proxy${useToken ? 'Place' : ''}<${moduleName}Presenter> {\n" +
            '    }'

    mapping.'artifact.proxy' = proxy

    installGwtpTemplate("${modulePackage}.${moduleName.toLowerCase()}", moduleName, 'NestedPresenter.java',
            "${moduleName}Presenter.java", mapping)
}

installGwtpView = { modulePackage, moduleName, uiHandlers ->
    def mapping = ['artifact.imports': 'import com.gwtplatform.mvp.client.ViewImpl;',
                   'artifact.extends': uiHandlers ? "ViewWithUiHandlers<${moduleName}UiHandlers>" : 'ViewImpl']

    if (uiHandlers)
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.ViewWithUiHandlers;'

    installGwtpTemplate("${modulePackage}.${moduleName.toLowerCase()}", moduleName, 'View.java',
            "${moduleName}View.java", mapping)
}

installGwtpViewUi = { modulePackage, moduleName ->
    installGwtpTemplate("${modulePackage}.${moduleName.toLowerCase()}", moduleName, 'View.ui.xml',
            "${moduleName}View.ui.xml")
}

installGwtpUiHandlers = { modulePackage, moduleName ->
    installGwtpTemplate("${modulePackage}.${moduleName.toLowerCase()}", moduleName, 'UiHandlers.java',
            "${moduleName}UiHandlers.java")
}

installGwtpModule = { modulePackage, moduleName ->
    installGwtpTemplate("${modulePackage}.${moduleName.toLowerCase()}", moduleName, 'Module.java',
            "${moduleName}Module.java")
}