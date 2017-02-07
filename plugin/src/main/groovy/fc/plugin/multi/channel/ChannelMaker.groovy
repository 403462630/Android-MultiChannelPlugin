package fc.plugin.multi.channel

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by rjhy on 17-2-6.
 */
class ChannelMaker extends DefaultTask {
    private static final String PROPERTY_CHANNEL_IDS = 'channelIds'
    public File apkFile

    public void setup() {
        description "Make Multi-Channel apk"
        group "Package"
    }

    @TaskAction
    public void channelPackaging() {
        PluginExtension multiChannel = project.multiChannel
        if (check(multiChannel)) {
            String url = multiChannel.channel.url
            if (url.startsWith("file:")) {
                url = url.substring("file:".length())
                File tempFile = project.file(url)
                url = tempFile.path
            }

            boolean hasIds = project.hasProperty(PROPERTY_CHANNEL_IDS)
            String channelIds = ""
            if (hasIds) {
                channelIds = project.getProperties().get(PROPERTY_CHANNEL_IDS)
            }
            executePython(url, multiChannel.storePassword, multiChannel.storeFile.path, multiChannel.apkName, channelIds)
        }
    }

    private boolean check(PluginExtension multiChannel) {
        if (multiChannel == null) {
            println "error: not config multiChannel in build.gradle"
            return false
        }
        if (multiChannel.storePassword == null || multiChannel.storePassword.trim().length() == 0) {
            println "error: not config storePassword or storePassword is empty"
            return false
        }
        if (multiChannel.storeFile == null || !multiChannel.storeFile.exists()) {
            println "error: not config storeFile or storeFile is not exists"
            return false
        }
        if (multiChannel.channel == null || multiChannel.channel.url == null || multiChannel.channel.url.trim().length() == 0) {
            println "error: not config channel or url is empty"
            return false
        }
        return true
    }

    private void executePython(String url, String storePassword, String storeFilePath, String apkName, String channelIds) {
        def multiChannelPyFile = MultiChannelPlugin.class.getClassLoader().getResource("package_multi_channels.py")
        def file = project.file('build/package_multi_channels.py')
        file.text = multiChannelPyFile.text
        if (apkName == null || apkName.trim().length() == 0) {
            apkName = PluginExtension.DEFAULT_APK_NAME
        }
        def progress = "python ${file.path} ${apkFile.path} ${url} ${storePassword} ${storeFilePath} ${apkName} ${channelIds}".execute()

        progress.inputStream.eachLine { text ->
            println text
        }
        progress.errorStream.eachLine {text ->
            println text
        }
    }
}
