package fc.plugin.multi.channel

import org.gradle.api.Action

/**
 * Created by rjhy on 17-1-22.
 */
class PluginExtension {
    static final String DEFAULT_APK_NAME = 'app-{code}_test.apk'
    String storePassword
    File storeFile
    String keyAlias
    String keyAliasPassword
    ChannelConfig channel
    String apkName = DEFAULT_APK_NAME
    JiaGuConfig jiagu

    PluginExtension(ChannelConfig channel, JiaGuConfig jiagu) {
        this.channel = channel
        this.jiagu = jiagu
    }

    void channel(Action<ChannelConfig> action) {
        action.execute(channel)
    }

    void jiagu(Action<JiaGuConfig> action) {
        action.execute(jiagu)
    }
}
