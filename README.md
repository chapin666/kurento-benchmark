[![][CodeUrjc Logo]][CodeUrjc]

Copyright © 2017 [CodeUrjc]. Licensed under [Apache 2.0 License].

webrtc-benchmark
================

This is a benchmark application aimed to assess the performance of Kurento Media
Server. Depending of the configuration, the media logic has different
topologies (one-to-one or one-to-many) and different media processing (none,
filtering, encoding, etc).

Features
--------

This repository contains a web application using Spring-Boot and KurentoClient to control an instance of Kurento Media Server (KMS). The application GUI has different configuration parameters to tune its behaviour. The features of this application are described in this section.

First of all, this application is **multi-topology**, meaning that depending on the configuration, the application behaves as a **one2one** or **one2many** WebRTC video call.

In addition it can ben **multi-session**, meaning that there can be different simultaneous sessions. Each session is identified by the field *Session number* in the GUI.

Then, it can be **multi-processing**. Each WebRtcEndpoint is connected with the other directly, but in between it can be placed a Filter, concretely one the following:

	- GStreamerFilter (encoder)
	- FaceOverlayFilter
	- ImageOverlayFilter
	- ZBarFilter
	- PassThrough

The application also supports **fake clients**. It has been designed to be consumed alwasy by two real browsers per session: one acting as presenter and other acting as viewer. This is the case of a one2one video call. In order to convert the session in a one2many, the GUI parameter *Number of fake clients* (in GUI) should be greater that 0. In that case, the application becomes in a one2many, in which N-1 viewers are fake (i.e., they are not browsers consuming the media, but WebRtcEndpoint provided by another instance of KMS). The behaviour of the fake clients can be customized from the GUI:

- Rate between clients (milliseconds): Time among a fake viewer and the next one
- Remove fake clients: Boolean value that indicates whether or not the fake clients should be removed after a given time (next vale)
- Time with all fake clients together (seconds): If the previous value is true, this field sets the time in which all the fake clients are consuming media. When this time expires, the fake clients are removed using the same shrinking time used for inclusion
- Number of fake clients per KMS instance. This value is useful to establish when use a new KMS (i.e., create a new KurentoClient) for fake viewers

All in all, and depending how the GUI is configured, the topology of the application is one of the following:

![](https://raw.githubusercontent.com/codeurjc/webrtc-benchmark/master/src/main/resources/static/img/topology.png)

In addition to the application itself, the webrtc-benchmark repository is shipped with an **JUnit test**. This test uses the features provided by the Kurento Testing Framework (*kurento-test* Maven dependency). This test is highly configurable. The following table provides a summary of the configuration parameters:

| Parameter                   | Default Value | Description                                                                                                                                                                                                            |
|-----------------------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| session.play.time           | 5             | Session time (in seconds)                                                                                                                                                                                              |
| session.rate.time           | 1000          | Input-output rate for sessions (in milliseconds)                                                                                                                                                                       |
| sessions.number             | 1             | Number of concurrent sessions                                                                                                                                                                                          |
| init.session.number         | 0             | Points for the KMSs for media sessions                                                                                                                                                                                 |
| processing                  | None          | Media processing. Valid values: None, Encoder, FaceOverlay, FilterImage, OverlayFilter, ZBarFilter, PassThrough                                                                                                        |
| fake.clients.number         | 10            | Number of fake clients                                                                                                                                                                                                 |
| fake.clients.rate           | 1000          | Input-output rate for fake clients (in milliseconds)                                                                                                                                                                   |
| fake.clients.remove         | false         | Boolean flag that indicates whether or not the fake clients should be removed                                                                                                                                          |
| fake.clients.play.time      | 10            | Play time for fake clients (in seconds)                                                                                                                                                                                |
| fake.clients.number.per.kms | 20            | Number of fake clients per KMS                                                                                                                                                                                         |
| video.quality.ssim          | false         | Boolean flag that indicates whether or not the SSIM (video quality) analysis is carried out                                                                                                                            |
| video.quality.psnr          | false         | Boolean flag that indicates whether or not the PSNR (video quality) analysis is carried out                                                                                                                            |
| output.folder               | .             | Folder where the test output (CSV files) is stored. By default it is in the current directory, i.e. the root the project                                                                                               |
| serialize.data              | false         | Boolean flag that indicates whether or not the data structures (for end-to-end latency and WebRTC stats) are serialized (stored as *.ser files in the output folder). These files are used for debugging purposes only |
| webrtc.endpoint.kbps        | 500           | Bandwidth (in kbps) to configure the WebRtcEndpoints elements handled by the app                                                                                                                                       |
| monitor.kms                 | false         | Boolean flaf taht indicates whether or not a resources monitor for the machine hosting KMS is used                                                                                                                     |
| monitor.sampling.rate       | 1000          | Rate for the monitor in milliseconds                                                                                                                                                                                   |  

The test can be executed from the command line. To do that, first of all it should be cloned from GitHub, and the it can be executed as follows:

```bash
mvn test -Dparam1=value1 -Dparam2=value ...
```

It is highly recommended to run this test in an Ubuntu 14.04 machine. This is due the fact the test uses native Linux utilities. Therefore, this application must be installed in the machine running the test:

- Google Chrome (latest stable version). In Ubuntu, it can be done with the command line as follows:

```bash
sudo apt-get install libxss1 libappindicator1 libindicator7
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo dpkg -i google-chrome*.deb
sudo apt-get install -f
```

- [Tesseract-ocr]. Utility for OCR. This tool is mandatory to carry out the end-to-end (browser to browser) latency calculation. It can be installed in Ubuntu with the following command:

```bash
sudo apt-get install tesseract-ocr
sudo mkdir -p /bytedeco/javacpp-presets/tesseract/cppbuild/linux-x86_64/share/tessdata/
sudo ln -s /usr/share/tesseract-ocr/tessdata/eng.traineddata /bytedeco/javacpp-presets/tesseract/cppbuild/linux-x86_64/share/tessdata/eng.traineddata
```

- [qpsnr]. Utility to calculate video quality (SSIM/PSNR). This tool is mandatory to carry out the video quality evaluation (i.e., if the flag video.quality.ssim or video.quality.psnr are set to true). In order to install it in Ubuntu, the latest version of the deb package should be downloaded from the qpsnr web, and the install it with the Debian package manager:

```bash
sudo dpkg -i qpsnr_0.2.5_amd64.deb
```

When the test finishes, a set of CSV files will be created by the test. These files follows the following pattern following: \<NubomediaBenchmarkTest-latency-sessionX\>, where X is the number of session. For the default case, i.e. only 1 session (sessions.number=1), the resulting CSV file will be NubomediaBenchmarkTest-session0.csv. A CSV file is a comma separated values file, and it can be opened using a spreadsheet processor, for instance Microsoft Excel or LibreOffice Calc. It is important to be aware of the content of this file. The constraints are the following:

- Each column is a set of values of a given feature
- Each row is an instant sample of the corresponding feature (column)
- Each sample (i.e. each row) is taken each second

The format of the columns are the following (see an example snapshot in the picture below):

- First column (mandatory). Header name: E2ELatencyMs. This set of data is the end-to-end latency (i.e. browser to browser) in milliseconds
- Second column (optional). Header name: avg_ssim. Results for the structural similarity video quality metric. The value of this metric is 1 when the video quality is perfect, to 0 in the worst case.
- Third column (optional). Header name: avg_psnr. Results for the peak signal to noise relation video quality metric in dB. Acceptable values for quality loss are considered to be about 20 dB to 25 dB.
- Next columns (mandatory). WebRTC stats. Each column name has the same pattern: \<local|remote\>\<Audio|Video\>\<statName\>. Therefore, we can read three different parts in each column:
	- \<local|remote\>. If the stat start with local that means that the metric has taken in presenter side. If the stat start with remote that means that the metric has taken in presenter side.
	- \<Audio|Video\>. This value distinguish metrics for Audio and Video.
	- \<statName\>. The final part of the column is the WebRTC metric name, e.g. googEchoCancellationReturnLoss, JitterBufferMs, packetsLost, among many others. The complete list of this WebRTC stats is the official standard. See the documentation (https://www.w3.org/TR/webrtc-stats/) for further information on each metric, and also the unit of each measurement.

Due to the fact the metrics are gathered using different mechanisms (i.e. client WebRTC stats, KMS internal latencies, end-to-end latency, video quality) it is very likely that the boundary values are not consistent. In other words, in order to process correctly the gathered data, the edge values (first and lasts rows) might be discarded.

News
----

Follow us on Twitter @[CodeUrjc Twitter].

Licensing and distribution
--------------------------

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[CodeUrjc Logo]: https://raw.githubusercontent.com/codeurjc/webrtc-benchmark/master/src/main/resources/static/img/code.png
[CodeUrjc Twitter]: https://twitter.com/codeurjc
[CodeUrjc]: http://www.code.etsii.urjc.es/
[Tesseract-ocr]: https://github.com/tesseract-ocr
[qpsnr]: http://qpsnr.youlink.org/