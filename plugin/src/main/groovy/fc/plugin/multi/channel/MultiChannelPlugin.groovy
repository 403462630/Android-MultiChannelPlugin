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
                File apkFile = variant.outputs[0].outputFile
                channelMaker.apkFile = apkFile
                channelMaker.setup()
                channelMaker.dependsOn "assemble${variantName}"
            }

            ChannelMaker channelMaker = project.tasks.create("packageMultiChannel", ChannelMaker)
            channelMaker.setup()
        }
    }
}
