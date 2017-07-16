package fc.plugin.multi.channel

/**
 * Created by fangcan on 2017/7/16.
 */
class JiaGuConfig {
    boolean isEnable
    String username
    String password
    String path

    void path(String path) {
        this.path = path
    }

    void isEnable(boolean isEnable) {
        this.isEnable = isEnable
    }

    void username(String username) {
        this.username = username
    }

    void password(String password) {
        this.password = password
    }
}
