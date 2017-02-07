package fc.plugin.multi.channel

import org.gradle.api.Action

/**
 * Created by rjhy on 17-1-22.
 */
class PluginExtension {
    static final String DEFAULT_APK_NAME = 'app-{code}_test.apk'
    String storePassword
    File storeFile
    ChannelConfig channel
    String apkName = DEFAULT_APK_NAME

    PluginExtension(ChannelConfig channel) {
        this.channel = channel
    }

    void channel(Action<ChannelConfig> action) {
        action.execute(channel)
    }
}
