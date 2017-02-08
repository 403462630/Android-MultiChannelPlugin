# Android-MultiChannelPlugin
多渠道打包插件，支持添加额外的信息extInfo，并提供library 读取渠道id和extInfo，用法如下：

 [json数据格式](channel.json)

### 怎么使用plugin
1 . 在buildscript中添加如下配置
```
buildscript {
    repositories {
        maven { url "http://fc.bintray.com/maven" }
    }
    dependencies {
        classpath 'fc.multi.channel:plugin:1.0.3'
    }
}
```

2 . 在app的build.gradle中配置如下：
```
apply plugin: 'multiChannel'

multiChannel {
    //签名密码
    storePassword "123456"
    //签名证书
    storeFile file('../test.jks')
    channel {
        //本地多渠道配置json文件
        url 'file:../channel.json'
        //或者是http地址
//        url "http://xxxx"
    }
    //生成的apk名称，code、id、name对应json中的key
    apkName "app-{code}-{id}-{name}-${android.defaultConfig.versionName}.apk"
}

```
3 . 执行如下gradle命令：
```
打debug包的多渠道包
./gradlew assembleDebugMultiChannel 
打release包的多渠道包
./gradlew assembleReleaseMultiChannel 
还可以使用-PchannelIds参数指定打指定渠道包(多个用,隔开)
./gradlew assembleReleaseMultiChannel -PchannelIds=1,2
```

### 怎么读取渠道和extInfo信息
1. 在build.gradle中配置如下：
```
repositories {
    maven { url "http://fc.bintray.com/maven" }
}
dependencies {
    compile 'fc.multi.channel:library:1.0.0'
}
```
2. 使用api
```
//只需要初始化一次
ChannelReader.init(this);
//获取渠道id
ChannelReader.getChannelId(this)
//获取渠道ExtInfo
Map map = ChannelReader.getExtInfo(this);
```


## 注意
本插件在使用了python和shell脚本，请保证你的电脑支持python和shell
