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

/**
 * @author <a href='mailto:donbeave@gmail.com'>Alexey Zhokhov</a>
 */
includeTargets << grailsScript('_GrailsCreateArtifacts')
includeTargets << new File("${gwtPluginDir}/scripts/_GwtInternal.groovy")
includeTargets << grailsScript('_GrailsCompile')
includeTargets << new File("${gwtpPluginDir}/scripts/_GwtpCreate.groovy")

USAGE = """
    create-gwtp-presenter [--reveal-type=Root|RootLayout|RootPopup|SLOT_NAME] [--name-token=TOKEN] [--code-split] [--ui-handlers] PKG NAME

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

    boolean uiHandlers = argsMap && argsMap.containsKey('ui-handlers') ? true :
            grailsConsole.userInput('Add UiHandlers ?', ['y', 'n']) == 'y'

    if (uiHandlers)
        installGwtpUiHandlers(modulePackage, moduleName)
    installGwtpPresenter(modulePackage, moduleName, argsMap.'name-token', argsMap.'reveal-type',
            argsMap && argsMap.containsKey('code-split'), uiHandlers)
    installGwtpView(modulePackage, moduleName, uiHandlers)
    installGwtpViewUi(modulePackage, moduleName)
    installGwtpModule(modulePackage, moduleName)
}