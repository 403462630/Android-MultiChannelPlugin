package fc.plugin.multi.channel

import org.gradle.api.Plugin
import org.gradle.api.Project

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
//                File apkFile = variant.outputs[0].outputFile
//                println("========" + variant.outputs[0].class + "========")
//                println("========" + variant.getPackageApplicationProvider().get().class + "========")
//                println("========" + apkFile.absolutePath + "========")
//                println("========" + variant.getPackageApplicationProvider().get().outputDirectory.absolutePath + File.separator + variant.getPackageApplicationProvider().get().getApkNames()[0] + "========")
                channelMaker.apkFile = new File(variant.getPackageApplicationProvider().get().outputDirectory.absolutePath + File.separator + variant.getPackageApplicationProvider().get().getApkNames()[0])
                channelMaker.setup()
                channelMaker.dependsOn "assemble${variantName}"
            }

            ChannelMaker channelMaker = project.tasks.create("packageMultiChannel", ChannelMaker)
            channelMaker.setup()
        }
    }
}
