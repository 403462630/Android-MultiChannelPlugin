package fc.plugin.multi.channel

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Field

/**
 * Created by rjhy on 17-1-22.
 */
class MultiChannelPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("multiChannel", PluginExtension, new ChannelConfig(), new JiaGuConfig())
        project.tasks.each { task ->
            println "name: ${task.name}, group:${task.group}"
        }
        project.afterEvaluate {
            project.android.applicationVariants.all { variant ->
                def variantName = variant.name.capitalize()
                ChannelMaker channelMaker = project.tasks.create("assemble${variantName}MultiChannel", ChannelMaker)
//                println("====print: " + variant.outputs[0].getDirName() + ", " + variant.outputs[0].getBaseName())
//                printProperties(variant.outputs[0])
                def apkDir = variant.getPackageApplicationProvider().get().getOutputDirectory().getAsFile().get()
                def apkName = variant.outputs[0].outputFileName
//                println("文件夹: $apkDir")
//                println("文件名: $apkName")
//                printProperties(variant.getPackageApplicationProvider().get())
                channelMaker.apkFile = new File(apkDir, apkName)
//                println("*********" + channelMaker.apkFile.absolutePath)
                channelMaker.setup()
                channelMaker.dependsOn "assemble${variantName}"
            }

            ChannelMaker channelMaker = project.tasks.create("packageMultiChannel", ChannelMaker)
            channelMaker.setup()
        }
    }

    private void printProperties(Object object) {
        println("====printProperties: " + object.class)
        Field[] fields = object.getClass().getFields()
        fields.each { field ->
            println(field.getName() + ", " + field.type)
        }
        println("====printMethods: " + object.class)
        object.getClass().getMethods().each { method ->
            println(method.getName() + ", " + method.getReturnType())
        }
    }
}
