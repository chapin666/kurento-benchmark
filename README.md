
kurento基准测试
================

这是一个评估 Kurento Media Server 性能的基准应用程序。 根据配置，媒体逻辑具有不同的拓扑（一对一或一对多），具有不同的媒体处理（过滤，编码等）。

特性
--------

该程序是一个使用 Spring-Boot 和 KurentoClient 控制 Kurento Media Server（KMS）实例的Web应用。 应用程序界面具有不同的配置参数来调整其行为。 本节介绍了此应用程序的功能。

首先, 这个应用程序是**多拓扑**，这意味着可以根据配置，应用程序的行为是一个** one2one **或** one2many ** WebRTC视频通话.

其次，它可以是本**多会话**，这意味着可以有不同的同时会话。 每个会话由Web中的字段*Session number*标识。

另外，它可以是**多处理**。 每个WebRtcEndpoint都直接与另一个连接，但在它之间可以放置一个过滤器，具体如下：

	- GStreamerFilter (encoder)
	- FaceOverlayFilter
	- ImageOverlayFilter
	- ZBarFilter
	- PassThrough

该应用程序还支持**假客户端**。 它被设计为每个会话总是由两个真正的浏览器消耗：一个作为演示者，另一个作为观众。 这是one2one视频通话的情况。 为了将会话转换为one2many，web 界面参数*Number of fake clients*（在GUI中）的数目应该大于0. 在这种情况下，应用程序将变成一个，其中N-1个查看器是假的（即 ，它们不是消耗媒体的浏览器，而是由KMS的另一个实例提供的WebRtcEndpoint）。 可以从Web界面定制假客户端的行为：

- Rate between clients (milliseconds): 当前观众和下一个观众进入房间间隔
- Remove fake clients: 表示在给定时间之后是否应删除假客户端的布尔值 (下一个参数)
- Time with all fake clients together (seconds): 如果先前的值为true，则此字段设置所有假客户端正在使用介质的时间。 当此时间到期时，假客户端将被删除
- Number of fake clients per KMS instance: 每个KMS实例的假客户端数，当使用新的KMS（即创建一个新的KurentoClient）作为假的观众时，该值很有用

总而言之，根据Web界面的配置方式，应用程序的拓扑结构如下所示：

![](https://s3.cn-north-1.amazonaws.com.cn/lycam-resource/svg/topology.png)

除了应用程序本身，webrtc-benchmark库还附带了一个** JUnit test**。 该测试使用Kurento测试框架提供的功能（* kurento-test * Maven依赖）。 此测试是高度可配置的。 下表提供了配置参数的总结:

| Parameter                   | Default Value                | Description                                                                                                                                                                                                             |
|-----------------------------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| kms.ws.uri                  | ws://127.0.0.1:8888/kurento  | KMS (under test) websocket 地址                                                                                                                                                                                          |
| fake.kms.ws.uri             | ws://127.0.0.1:8888/kurento  | KMS (for fake clients) websocket URL. 用于假客户端的几个KMS实例，只需将URL与符号分离即可 ",", 例如: fake.kms.ws.uri=ws://10.0.0.1:8888/kurento,ws://10.0.0.2:8888/kurento |
| session.play.time           | 5                            | 会话时间 (in seconds)                                                                                                                                                                                               |
| session.rate.time           | 1000                         | 会话的输入输出率(in milliseconds)                                                                                                                                                                        |
| sessions.number             | 1                            | 并发会话数                                                                                                                                                                                           |
| init.session.number         | 0                            | session 会话初始数                                                                                                                                                                                |
| processing                  | None                         | 媒体处理元素. 有效的值为: None, Encoder, FaceOverlay, FilterImage, OverlayFilter, ZBarFilter, PassThrough                                                                                                         |
| fake.clients.number         | 10                           | 假客户端数量                                                                                                                                                                                                  |
| fake.clients.rate           | 1000                         | 假客户会话的输入输出率 (in milliseconds)                                                                                                                                                                    |
| fake.clients.remove         | false                        | 布尔标志，指示是否应删除假客户端的                                                                                                                                          |
| fake.clients.play.time      | 10                           | 客户端会话时间 (in seconds)                                                                                                                                                                                 |
| fake.clients.number.per.kms | 20                           | 每个KMS的假客户数量                                                                                                                                                                                          |
| video.quality.ssim          | false                        | 指示是否执行SSIM（视频质量）分析的布尔标志                                                                                                                             |
| video.quality.psnr          | false                        | 指示是否执行PSNR（视频质量）分析的布尔标志                                                                                                                             |
| output.folder               | .                            | 存储测试输出（CSV文件）的文件夹。 默认情况下，它在当前目录中，即项目的根目录                                                                                                |
| serialize.data              | false                        | 指示数据结构（用于端到端延迟和WebRTC统计信息）是否被序列化（在输出文件夹中存储为* .ser文件）的布尔标志。 这些文件仅用于调试目的  |
| webrtc.endpoint.kbps        | 500                          | 处理 WebRtcEndpoints 元素的带宽（以kbps为单位）配置                                                                                                                                    |
| monitor.kms                 | false                        | 指示是否使用KMS机器的资源监视器的布尔标志                                                                                                                     |
| monitor.sampling.rate       | 1000                         | 监视器的速率（以毫秒为单位）                                                                                                                                                                                   |
| kms.login             | -                            | 如果kms.ws.uri指向托管在远程机器（即不是127.0.0.1）的KMS，则测试连接到该主机的SSH以评估一些物理参数（CPU，内存等）。 在这种情况下，需要SSH凭据(用户名)       |
| kms.passwd            | -                            | 为了设置KMS SSH凭据，应该验证密码（或pem密钥）。 此参数设置SSH密码                                                                              |
| kms.pem               | -                            | 为了设置KMS SSH凭据，应该验证t密码（或pem密钥）。 该参数设置PEM密钥文件的路径                                                                  |

测试可以从命令行执行。 要做到这一点，首先应该从GitHub克隆这个存储库，它可以执行如下：

```bash
mvn test -Dparam1=value1 -Dparam2=value ...
```

此测试执行的第一个操作是启动Web应用程序（使用spring-boot实现），内部使用KurentoClient来控制KMS.

强烈建议您在Ubuntu 14.04机器上运行此测试。 这是由于测试使用本机Linux实用程序。 

在运行测试的机器中，我们必须安装以下依赖:

- Google Chrome (latest stable version). 在Ubuntu中，可以使用如下命令进行操作:

```bash
sudo apt-get install libxss1 libappindicator1 libindicator7
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo dpkg -i google-chrome*.deb
sudo apt-get install -f
```

设置 Google Chrome 为--no-sandbox

```
cd /opt/google/chrome/
vim google-chrome
```
修改 ``` exec -a "$0" "$HERE/chrome"  "$@" ``` 为 ``` exec -a "$0" "$HERE/chrome"  "$@" --no-sandbox ```


- [Tesseract-ocr]. Utility for OCR. 此工具执行端到端（浏览器到浏览器）延迟计算。 它可以通过以下命令安装在Ubuntu中:

```bash
sudo apt-get install tesseract-ocr
sudo mkdir -p /bytedeco/javacpp-presets/tesseract/cppbuild/linux-x86_64/share/tessdata/
sudo ln -s /usr/share/tesseract-ocr/tessdata/eng.traineddata /bytedeco/javacpp-presets/tesseract/cppbuild/linux-x86_64/share/tessdata/eng.traineddata
```

- [qpsnr]. 计算视频质量（SSIM/PSNR）的实用程序。 该工具执行视频质量评估（即，如果标志video.quality.ssim或video.quality.psnr被设置为真）。 为了将其安装在Ubuntu中，最新版本的deb软件包应该从qpsnr web下载，并且安装 dpkg 包管理器:

```bash
wget http://qpsnr.youlink.org/data/qpsnr_0.2.5_amd64.deb
sudo dpkg -i qpsnr_0.2.5_amd64.deb
```

- Xvfb (选择安装). 一个虚拟的X服务器环境, 没有提供GUI的服务器需要安装。

```
sudo apt-get install xvfb
Xvfb :40 -screen 0 1024x768x24 -extension RANDR
```

该测试能够收集托管KMS的机器的一些物理参数值。 这个组件叫做**monitor**。 为了启用此功能，如上表所述，配置“monitor.kms”为“true”。 monitor主要的参数如下：

- CPU consumption (%)
- RAM memory consumption (bytes and %)
- KMS threads number
- Network interfaces (up and down) usage

monitor组件有两个重要的要求：:

- 它已经在Java中创建，因此，正在测试的 KMS 的机器应该已经安装了 JRE
- 该测试使用端口12345打开组件的TCP套接字。因此，该端口应该打开

测试完成后，将通过测试创建一组CSV文件。 CSV文件是逗号分隔值文件，可以使用电子表格处理器（例如Microsoft Excel或LibreOffice Calc）打开它。 了解该文件的内容很重要。 约束如下:

- 每列是给定特征的一组值
- 每行是对应特征（列）的即时样本
- 每个样品（即每一行）每秒钟被取出

将有两种CSV文件:

1. WebRTC延迟统计CSV文件. 这些文件遵循以下模式：\ <NubomediaBenchmarkTest-latency-sessionX \>，其中X是会话数。 对于默认情况，即只有1个会话（sessions.number = 1），生成的CSV文件将是NubomediaBenchmarkTest-latency-session0.csv. 
2. 物理参数 (通过monitor收集)文件CSV. 这些文件遵循以下模式: \<NubomediaBenchmarkTest-monitor-sessionX\>,其中X是会话数.

第一种类型的CSV（延迟）中的列的格式如下（请参见下图中的示例快照）：
- First column (mandatory). Header name: E2ELatencyMs. 这组数据是以毫秒为单位的端到端延迟（即浏览器到浏览器）
- Second column (optional). Header name: avg_ssim. 视频质量度量的相似度。 当视频质量完美时，该度量的值为1，在最坏情况下为0.
- Third column (optional). Header name: avg_psnr. 视频质量度量（dB）的峰值。 可接受的质量损失值被认为是大约20 dB到25 dB.
- Next columns (mandatory). WebRTC stats. 每个列名称具有相同的模式: \<local|remote\>\<Audio|Video\>\<statName\>. 因此，我们可以在每一列中看到三个不同的部分:
	- \<local|remote\>. 如果统计数据从本地开始，这意味着度量标准在演示方。 如果该统计信息从远程开始，这意味着度量标准已经在主持人方.
	- \<Audio|Video\>. 此值区分音频和视频的度量.
	- \<statName\>. 最后一部分是WebRTC度量名称，例如 googEchoCancellationReturnLoss，JitterBufferMs，packetsLost等等。 这个WebRTC统计的完整列表是官方标准。 有关每个度量的更多信息，以及每个测量的单位，请参阅文档（https://www.w3.org/TR/webrtc-stats/）.

由于使用不同机制（即客户端WebRTC统计信息，KMS内部延迟，端到端延迟，视频质量）收集度量的事实，边界值很可能不一致。 换句话说，为了正确处理收集的数据，可能会丢弃边缘值（第一行和最后一行）.

[Tesseract-ocr]: https://github.com/tesseract-ocr
[qpsnr]: http://qpsnr.youlink.org/