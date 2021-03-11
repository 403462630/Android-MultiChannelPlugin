package fc.plugin.multi.channel

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.swing.plaf.TextUI

/**
 * Created by rjhy on 17-2-6.
 */
class ChannelMaker extends DefaultTask {
    private static final String PROPERTY_CHANNEL_IDS = 'channelIds'
    private static final String PROPERTY_JIAGU_CHANNEL_IDS = 'jiaguChannelIds'
    private static final String PROPERTY_EX_CHANNEL_IDS = 'exchannelIds'
    private static final String PROPERTY_JIAGU_EX_CHANNEL_IDS = 'jiaguExchannelIds'
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

            boolean hasJiaguIds = project.hasProperty(PROPERTY_JIAGU_CHANNEL_IDS)
            String jiaguChannelIds = ""
            if (hasJiaguIds) {
                jiaguChannelIds = project.getProperties().get(PROPERTY_JIAGU_CHANNEL_IDS)
            }
            String jiaguExChannelIds = ""
            if (project.hasProperty(PROPERTY_JIAGU_EX_CHANNEL_IDS)) {
                exChannelIds = project.getProperties().get(PROPERTY_JIAGU_EX_CHANNEL_IDS)
            }
            boolean isEnableJiagu = false
            if (multiChannel.jiagu && multiChannel.jiagu.isEnable) {
                executeJiagu(multiChannel.jiagu.username, multiChannel.jiagu.password, multiChannel.jiagu.path, apkFile.path, multiChannel.storeFile.path, multiChannel.storePassword, multiChannel.keyAlias, multiChannel.keyAliasPassword)
                isEnableJiagu = true
            }

            executePython(url, multiChannel.storePassword, multiChannel.storeFile.path, multiChannel.apkName, channelIds, exChannelIds, jiaguChannelIds, jiaguExChannelIds, isEnableJiagu)
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

    private void executeJiagu(String username, String password, String jiaguPath, String apkPath, String storeFilePath, String storePassword, String keyAlias, String keyAliasPassword) {
        def jiaguPyFile = MultiChannelPlugin.class.getClassLoader().getResource("jiagu.py")
        def file = project.file('build/jiagu.py')
        file.text = jiaguPyFile.text

        StringBuffer buffer = new StringBuffer()
        buffer.append("python3 ${file.path} ")

        if (username == null || username.trim().length() <= 0) {
            throw IllegalArgumentException("error: username is null")
        }
        buffer.append(" -username ${username}")

        if (password == null || password.trim().length() <= 0) {
            throw IllegalArgumentException("error: password is null")
        }
        buffer.append(" -password ${password}")

        if (jiaguPath == null || jiaguPath.trim().length() <= 0) {
            throw IllegalArgumentException("error: jiagu_path is null")
        }
        buffer.append(" -jiagu_path ${jiaguPath}")

        if (apkPath == null || apkPath.trim().length() <= 0) {
            throw IllegalArgumentException("error: apk_path is null")
        }
        buffer.append(" -apk_path ${apkPath}")

        if (storeFilePath == null || storeFilePath.trim().length() <= 0) {
            throw IllegalArgumentException("error: key_store is null")
        }
        buffer.append(" -key_store ${storeFilePath}")

        if (storePassword == null || storePassword.trim().length() <= 0) {
            throw IllegalArgumentException("error: key_password is null")
        }
        buffer.append(" -key_password ${storePassword}")

        if (keyAlias == null || keyAlias.trim().length() <= 0) {
            throw IllegalArgumentException("error: key_alias is null")
        }
        buffer.append(" -key_alias ${keyAlias}")

        if (keyAliasPassword == null || keyAliasPassword.trim().length() <= 0) {
            throw IllegalArgumentException("error: key_alias_password is null")
        }
        buffer.append(" -key_alias_password ${keyAliasPassword}")

        String exeStr =  buffer.toString()

        println '------start jiagu apk------'
        def progress = exeStr.execute()

        progress.inputStream.eachLine { text ->
            println text
        }
        String errorText = progress.errorStream.text
        if (errorText != null && errorText.trim().length() > 0) {
            throw new IllegalArgumentException(errorText)
        }
        println '------end jiagu apk------'
    }

    private void executePython(String url, String storePassword, String storeFilePath, String apkName, String channelIds, String exChannelIds, String jiaguChannelIds, String jiaguExChannelIds, boolean isEnableJiagu) {
        def multiChannelPyFile = MultiChannelPlugin.class.getClassLoader().getResource("package_multi_channels.py")
        def file = project.file('build/package_multi_channels.py')
        file.text = multiChannelPyFile.text
        if (apkName == null || apkName.trim().length() == 0) {
            apkName = PluginExtension.DEFAULT_APK_NAME
        }

        StringBuffer buffer = new StringBuffer()
        buffer.append("python3 ${file.path}  -apk_path ${apkFile.path} -json_path ${url} -key_password ${storePassword} ")
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
        
        if (jiaguChannelIds != null && jiaguChannelIds.trim().length() > 0) {
            buffer.append(" -jiagu_channel_ids ${jiaguChannelIds}")
        }
        if (jiaguExChannelIds != null && jiaguExChannelIds.trim().length() > 0) {
            buffer.append(" -jiagu_ex_channel_ids ${jiaguExChannelIds}")
        }
        if (isEnableJiagu) {
            buffer.append(" -is_enable_jiagu ${isEnableJiagu}")
        }

        String exeStr =  buffer.toString()//"python ${file.path} -apk_path ${apkFile.path} -json_path ${url} -key_password ${storePassword} -key_store ${storeFilePath} -default_apk_name ${apkName} -channel_ids ${channelIds} -ex_channel_ids ${exChannelIds}"
        println "----${exeStr}"
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
