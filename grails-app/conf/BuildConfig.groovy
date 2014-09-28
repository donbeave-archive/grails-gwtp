grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global') {
    }
    log 'warn'
    repositories {
        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
        mavenRepo 'https://raw.github.com/donbeave/mavenrepo/master/'
    }
    dependencies {

    }

    plugins {
        build(':tomcat:7.0.55',
                ':release:3.0.1',
                ':rest-client-builder:2.0.3') {
            export = false
        }
        compile ':gwt:2.0-SNAPSHOT', {
            transitive = false
        }
    }
}
