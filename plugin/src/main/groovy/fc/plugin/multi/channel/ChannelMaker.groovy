package fc.plugin.multi.channel

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by rjhy on 17-2-6.
 */
class ChannelMaker extends DefaultTask {
    private static final String PROPERTY_CHANNEL_IDS = 'channelIds'
    private static final String PROPERTY_EX_CHANNEL_IDS = 'exchannelIds'
    private static final String PROPERTY_APK_PATH = 'apkPath'

    public File apkFile

    public void setup() {
        description "Make Multi-Channel apk"
        group "Package"
    }

    @TaskAction
    public void channelPackaging() {
        PluginExtension multiChannel = project.multiChannel
        if (apkFile == null || !apkFile.exists()) {
            boolean hasApkPath = project.hasProperty(PROPERTY_APK_PATH)
            if (hasApkPath) {
                apkFile = new File(project.getProperties().get(PROPERTY_APK_PATH))
            }
        }
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
            String exChannelIds = ""
            if (project.hasProperty(PROPERTY_EX_CHANNEL_IDS)) {
                exChannelIds = project.getProperties().get(PROPERTY_EX_CHANNEL_IDS)
            }
            executePython(url, multiChannel.storePassword, multiChannel.storeFile.path, multiChannel.apkName, channelIds, exChannelIds)
        }
    }

    private boolean check(PluginExtension multiChannel) {
        if (apkFile == null || !apkFile.exists()) {
            throw new IllegalArgumentException("error: apkFile not exists, " + (apkFile == null ? "apkFile = null" : "apkPath: " + apkFile.path))
            return false
        }

        if (multiChannel == null) {
            throw new IllegalArgumentException("error: not config multiChannel in build.gradle")
            return false
        }
        if (multiChannel.storePassword == null || multiChannel.storePassword.trim().length() == 0) {
            throw new IllegalArgumentException("error: not config storePassword or storePassword is empty")
            return false
        }
        if (multiChannel.storeFile == null || !multiChannel.storeFile.exists()) {
            throw new IllegalArgumentException("error: not config storeFile or storeFile is not exists")
            return false
        }
        if (multiChannel.channel == null || multiChannel.channel.url == null || multiChannel.channel.url.trim().length() == 0) {
            throw new IllegalArgumentException("error: not config channel or url is empty")
            return false
        }
        return true
    }

    private void executePython(String url, String storePassword, String storeFilePath, String apkName, String channelIds, String exChannelIds) {
        def multiChannelPyFile = MultiChannelPlugin.class.getClassLoader().getResource("package_multi_channels.py")
        def file = project.file('build/package_multi_channels.py')
        file.text = multiChannelPyFile.text
        if (apkName == null || apkName.trim().length() == 0) {
            apkName = PluginExtension.DEFAULT_APK_NAME
        }

        StringBuffer buffer = new StringBuffer()
        buffer.append("python ${file.path}  -apk_path ${apkFile.path} -json_path ${url} -key_password ${storePassword}")
        if (storeFilePath != null && storeFilePath.trim().length() > 0) {
            buffer.append(" -key_store ${storeFilePath}")
        }
        if (apkName != null && apkName.trim().length() > 0) {
            buffer.append(" -default_apk_name ${apkName}")
        }
        if (channelIds != null && channelIds.trim().length() > 0) {
            buffer.append(" -channel_ids ${channelIds}")
        }
        if (exChannelIds != null && exChannelIds.trim().length() > 0) {
            buffer.append(" -ex_channel_ids ${exChannelIds}")
        }
        String exeStr =  buffer.toString()//"python ${file.path} -apk_path ${apkFile.path} -json_path ${url} -key_password ${storePassword} -key_store ${storeFilePath} -default_apk_name ${apkName} -channel_ids ${channelIds} -ex_channel_ids ${exChannelIds}"
        println '------start build multi channel apk------'
        def progress = exeStr.execute()

        progress.inputStream.eachLine { text ->
            println text
        }
        String errorText = progress.errorStream.text
        if (errorText != null && errorText.trim().length() > 0) {
            throw new IllegalArgumentException(errorText)
        }
//        progress.errorStream.eachLine {text ->
//            println text
//        }
        println '------end build multi channel apk------'
    }
}
