# baiduTtsDemo
实现Android下简单的中英TTS
使用百度免费的离在线语音合成SDK,支持在线和离线,第一次使用必须联网下载离线资源文件,才可使用离线功能.
可在初始化SpeechSynthesizer中通过 setParam方法设置语调 语速等.
存在的缺陷:在无网络情况下,打开app大概需要3秒时间初始化合成引擎.百度的demo也是同样情况!
使用前将apiId,apiKey,secretKey修改自己的.最好先看下技术文档!
百度语音合成文档:https://ai.baidu.com/docs#/TTS-Android-SDK/top
百度sdk下载:https://ai.baidu.com/sdk#tts
