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
includeTargets << grailsScript('_GrailsCreateArtifacts')
includeTargets << new File("${gwtPluginDir}/scripts/_GwtInternal.groovy")
includeTargets << grailsScript('_GrailsCompile')
includeTargets << new File("${gwtpPluginDir}/scripts/_GwtpCreate.groovy")

USAGE = """
    create-gwtp-presenter [--reveal-type=Root|RootLayout|RootPopup|SLOT_NAME] [--name-token=TOKEN] [--code-split] PKG NAME

where
    PKG  = The root package name of the presenter.
    NAME = The name of the module.
"""

target(default: 'Creates a new GWTP presenter.') {
    depends(parseArguments)

    compile()
    event('GwtCompileStart', ['Starting to compile the GWT modules.'])

    // adding gwtClassesDir to classpath
    rootLoader.addURL(gwtClassesDir.toURL())

    if (classLoader)
        classLoader.addURL(gwtClassesDir.toURL())

    promptForName(type: '')

    // We support just the one argument.
    def params = argsMap['params']
    if (!params || params.size() > 2) {
        println 'Unexpected number of command arguments.'
        println()
        println "USAGE:${USAGE}"
        exit(1)
    } else if (!params[0]) {
        println 'A module name must be given.'
        exit(1)
    }

    def modulePackage = params[0]
    def moduleName = params[1]

    // We require a package for the module.
    if (!modulePackage) {
        println 'Please provide a package for the module.'
        exit(1)
    }

    def mapping = ['artifact.imports': '', 'artifact.revealSlot': 'Root']

    // use proxy place
    def nameToken = argsMap.'name-token'
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
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.proxy.Place;\n'
    }

    // reveal type
    def revealType = argsMap.'reveal-type'
    if (revealType == null) {
        mapping.'artifact.revealSlot' =
                grailsConsole.userInput('Reveal In? ', ['Root', 'RootLayout', 'RootPopup', 'SLOT'])
    }

    if (mapping.'artifact.revealSlot'.equals('SLOT')) {
        // TODO please select one slot from list
    } else {
        mapping.'artifact.revealSlot' = "RevealType.${mapping.'artifact.revealSlot'}"
    }

    // code split
    String proxyAnnotation = 'ProxyStandard'
    if (argsMap && argsMap.containsKey('code-split')) {
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;'
        proxyAnnotation = 'ProxyCodeSplit'
    } else {
        mapping.'artifact.imports' += 'import com.gwtplatform.mvp.client.annotations.ProxyStandard;'
    }

    String proxy = (nameToken ? "    @NameToken(NameTokens.${nameToken})\n" : '') +
            "    @${proxyAnnotation}\n" +
            "    public interface MyProxy extends Proxy${useToken ? 'Place' : ''}<${moduleName}Presenter> {\n" +
            '    }'

    mapping.'artifact.proxy' = proxy

    // Now copy the template client entry point over.
    installGwtpTemplate("${modulePackage}.${moduleName.toLowerCase()}", moduleName, 'NestedPresenter.java',
            "${moduleName}Presenter.java", mapping)
}