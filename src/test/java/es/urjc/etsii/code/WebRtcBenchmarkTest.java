/*
 * (C) Copyright 2017 CodeUrjc (http://www.code.etsii.urjc.es/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package es.urjc.etsii.code;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.base.BrowserTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.services.Service;
import org.kurento.test.services.WebServerService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * WebRtc benchmark test.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.6.1
 */
public class WebRtcBenchmarkTest extends BrowserTest<WebPage> {

	@Service
	public static WebServerService webServer = new WebServerService(
			BenchmarkApp.class);

	private final Logger log = LoggerFactory
			.getLogger(WebRtcBenchmarkTest.class);

	private static final String APP_URL_PROP = "app.url";
	private static final String APP_URL_DEFAULT = "https://localhost:8443/";
	private static final String FAKE_CLIENTS_NUMBER_PROP = "fake.clients.number";
	private static final int FAKE_CLIENTS_NUMBER_DEFAULT = 0;
	private static final String FAKE_CLIENTS_RATE_PROP = "fake.clients.rate";
	private static final int FAKE_CLIENTS_RATE_DEFAULT = 1000;
	private static final String FAKE_CLIENTS_PER_KMS_PROP = "fake.clients.number.per.kms";
	private static final int FAKE_CLIENTS_PER_KMS_DEFAULT = 75;

	private static final String SESSION_PLAYTIME_PROP = "session.play.time";
	private static final int SESSION_PLAYTIME_DEFAULT = 30;
	private static final String SESSION_RATE_PROP = "session.rate.time";
	private static final int SESSION_RATE_DEFAULT = 1000;
	private static final String SESSIONS_NUMBER_PROP = "sessions.number";
	private static final int SESSIONS_NUMBER_DEFAULT = 1;
	private static final String INIT_SESSION_NUMBER_PROP = "init.session.number";
	private static final int INIT_SESSION_NUMBER_DEFAULT = 0;
	private static final String MEDIA_PROCESSING_PROP = "processing";
	private static final String MEDIA_PROCESSING_DEFAULT = "None";
	private static final String FAKE_CLIENTS_REMOVE_PROP = "fake.clients.remove";
	private static final boolean FAKE_CLIENTS_REMOVE_DEFAULT = false;
	private static final String FAKE_CLIENTS_TOGETHER_TIME_PROP = "fake.clients.play.time";
	private static final int FAKE_CLIENTS_TOGETHER_TIME_DEFAULT = FAKE_CLIENTS_NUMBER_DEFAULT
			* (FAKE_CLIENTS_RATE_DEFAULT / 1000);
	private static final String VIDEO_QUALITY_SSIM_PROP = "video.quality.ssim";
	private static final boolean VIDEO_QUALITY_SSIM_DEFAULT = false;
	private static final String VIDEO_QUALITY_PSNR_PROP = "video.quality.psnr";
	private static final boolean VIDEO_QUALITY_PSNR_DEFAULT = false;
	private static final String OUTPUT_FOLDER_PROP = "output.folder";
	private static final String OUTPUT_FOLDER_DEFAULT = ".";
	private static final String SERIALIZE_DATA_PROP = "serialize.data";
	private static final boolean SERIALIZE_DATA_DEFAULT = false;
	private static final String BANDWIDTH_PROP = "webrtc.endpoint.kbps";
	private static final int BANDWIDTH_DEFAULT = 500;
	private static final String NATIVE_DOWNLOAD_METHOD_PROP = "native.download.method";
	private static final boolean NATIVE_DOWNLOAD_METHOD_DEFAULT = false;
	private static final String DOWNLOADS_FOLDER_NAME_PROP = "download.folder.name";
	private static final String DOWNLOADS_FOLDER_NAME_DEFAULT = "Downloads";
	private static final String MONITOR_KMS_PROP = "monitor.kms";
	private static final boolean MONITOR_KMS_DEFAULT = false;
	private static final String SAMPLING_RATE_PROP = "sampling.rate";
	private static final int SAMPLING_RATE_DEFAULT = 100; // milliseconds

	private int index = 0;
	private Table<Integer, Integer, String> csvTable = null;
	private int extraTimePerFakeClients = 0;
	private SystemMonitorManager monitor;

	private boolean getSsim = getProperty(VIDEO_QUALITY_SSIM_PROP,
			VIDEO_QUALITY_SSIM_DEFAULT);
	private boolean getPsnr = getProperty(VIDEO_QUALITY_PSNR_PROP,
			VIDEO_QUALITY_PSNR_DEFAULT);
	private String outputFolder = getProperty(OUTPUT_FOLDER_PROP,
			OUTPUT_FOLDER_DEFAULT);
	private boolean serializeData = getProperty(SERIALIZE_DATA_PROP,
			SERIALIZE_DATA_DEFAULT);
	private boolean nativeDownload = getProperty(NATIVE_DOWNLOAD_METHOD_PROP,
			NATIVE_DOWNLOAD_METHOD_DEFAULT);
	private String downloadsFolderName = getProperty(DOWNLOADS_FOLDER_NAME_PROP,
			DOWNLOADS_FOLDER_NAME_DEFAULT);
	private boolean monitorKms = getProperty(MONITOR_KMS_PROP,
			MONITOR_KMS_DEFAULT);
	private int samplingRate = getProperty(SAMPLING_RATE_PROP,
			SAMPLING_RATE_DEFAULT);

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		String appUrl = getProperty(APP_URL_PROP, APP_URL_DEFAULT);
		int sessionsNumber = getProperty(SESSIONS_NUMBER_PROP,
				SESSIONS_NUMBER_DEFAULT);

		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.PRESENTER,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.numInstances(sessionsNumber).scope(BrowserScope.LOCAL)
						.url(appUrl).build());
		test.addBrowser(BrowserConfig.VIEWER,
				new Browser.Builder().browserType(BrowserType.CHROME)
						.numInstances(sessionsNumber).scope(BrowserScope.LOCAL)
						.url(appUrl).build());
		return Arrays.asList(new Object[][] { { test } });
	}

	@Before
	public void setup() {
		// Set defaults in all browsers
		int sessionsNumber = getProperty(SESSIONS_NUMBER_PROP,
				SESSIONS_NUMBER_DEFAULT);
		int initSessionNumber = getProperty(INIT_SESSION_NUMBER_PROP,
				INIT_SESSION_NUMBER_DEFAULT);
		for (int i = 0; i < sessionsNumber; i++) {
			WebDriver[] webDrivers = {
					getPresenter(i).getBrowser().getWebDriver(),
					getViewer(i).getBrowser().getWebDriver() };

			for (WebDriver webDriver : webDrivers) {
				// Session number
				WebElement sessionNumberWe = webDriver
						.findElement(By.id("sessionNumber"));
				sessionNumberWe.clear();
				sessionNumberWe.sendKeys(String.valueOf(i + initSessionNumber));

				// Media processing
				String processing = getProperty(MEDIA_PROCESSING_PROP,
						MEDIA_PROCESSING_DEFAULT);
				Select processingSelect = new Select(
						webDriver.findElement(By.id("processing")));
				processingSelect.selectByValue(processing);

				// Bandwidth
				String bandwidth = String.valueOf(
						getProperty(BANDWIDTH_PROP, BANDWIDTH_DEFAULT));
				WebElement bandwidthWe = webDriver
						.findElement(By.id("bandwidth"));
				bandwidthWe.clear();
				bandwidthWe.sendKeys(bandwidth);

				// Number of fake clients
				int fakeClientsInt = getProperty(FAKE_CLIENTS_NUMBER_PROP,
						FAKE_CLIENTS_NUMBER_DEFAULT);
				String fakeClients = String.valueOf(fakeClientsInt);
				WebElement fakeClientsWe = webDriver
						.findElement(By.id("fakeClients"));
				fakeClientsWe.clear();
				fakeClientsWe.sendKeys(fakeClients);

				// Rate between clients (milliseconds)
				int timeBetweenClients = getProperty(FAKE_CLIENTS_RATE_PROP,
						FAKE_CLIENTS_RATE_DEFAULT);
				WebElement timeBetweenClientsWe = webDriver
						.findElement(By.id("timeBetweenClients"));
				timeBetweenClientsWe.clear();
				timeBetweenClientsWe
						.sendKeys(String.valueOf(timeBetweenClients));

				if (fakeClientsInt > 0) {
					extraTimePerFakeClients = fakeClientsInt
							* timeBetweenClients / 1000;
				}

				// Remove fake clients
				boolean removeFakeClients = getProperty(
						FAKE_CLIENTS_REMOVE_PROP, FAKE_CLIENTS_REMOVE_DEFAULT);
				List<WebElement> removeFakeClientsList = webDriver
						.findElements(By.name("removeFakeClients"));
				removeFakeClientsList.get(removeFakeClients ? 0 : 1).click();

				// Time with all fake clients together (seconds)
				if (removeFakeClients) {
					int playTime = getProperty(FAKE_CLIENTS_TOGETHER_TIME_PROP,
							FAKE_CLIENTS_TOGETHER_TIME_DEFAULT);
					WebElement playTimeWe = webDriver
							.findElement(By.id("playTime"));
					playTimeWe.clear();
					playTimeWe.sendKeys(String.valueOf(playTime));

					extraTimePerFakeClients = (extraTimePerFakeClients * 2)
							+ playTime;
				}

				// Number of fake clients per KMS instance
				String fakeClientsPerInstance = String
						.valueOf(getProperty(FAKE_CLIENTS_PER_KMS_PROP,
								FAKE_CLIENTS_PER_KMS_DEFAULT));
				WebElement fakeClientsPerInstanceWe = webDriver
						.findElement(By.id("fakeClientsPerInstance"));
				fakeClientsPerInstanceWe.clear();
				fakeClientsPerInstanceWe.sendKeys(fakeClientsPerInstance);

			}
		}

		if (!outputFolder.endsWith(File.separator)) {
			outputFolder += File.separator;
		}
	}

	@Test
	public void test() throws Exception {
		final int sessionsNumber = getProperty(SESSIONS_NUMBER_PROP,
				SESSIONS_NUMBER_DEFAULT);
		final int sessionPlayTime = getProperty(SESSION_PLAYTIME_PROP,
				SESSION_PLAYTIME_DEFAULT);
		final int sessionRateTime = getProperty(SESSION_RATE_PROP,
				SESSION_RATE_DEFAULT);

		final CountDownLatch latch = new CountDownLatch(sessionsNumber);
		ExecutorService executor = Executors.newFixedThreadPool(sessionsNumber);
		for (int i = 0; i < sessionsNumber; i++) {
			if (i != 0) {
				waitMilliSeconds(sessionRateTime);
			}
			index = i;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						exercise(sessionsNumber, sessionPlayTime,
								sessionRateTime);
					} catch (Exception e) {
						log.error("Exception in session {}", index, e);
					} finally {
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		executor.shutdown();
	}

	private void exercise(int sessionsNumber, int sessionPlayTime,
			int sessionRateTime) throws Exception {
		log.info("[Session {}] Starting test", index);

		// Sync presenter and viewer time
		log.info(
				"[Session {}] Synchronizing presenter and viewer ... please wait",
				index);
		WebPage[] browsers = { getPresenter(index), getViewer(index) };
		String[] videoTags = { "video", "video" };
		String[] peerConnections = { "webRtcPeer.peerConnection",
				"webRtcPeer.peerConnection" };
		syncTimeForOcr(browsers, videoTags, peerConnections);

		// Start presenter
		getPresenter(index).getBrowser().getWebDriver()
				.findElement(By.id("presenter")).click();
		getPresenter(index).subscribeEvent("video", "playing");
		getPresenter(index).waitForEvent("playing");

		// Start viewer
		getViewer(index).getBrowser().getWebDriver()
				.findElement(By.id("viewer")).click();
		getViewer(index).subscribeEvent("video", "playing");
		getViewer(index).waitForEvent("playing");

		log.info(
				"[Session {}] Media in presenter and viewer, starting OCR and recording",
				index);

		// KMS Monitor (CPU, memory, etc)
		if (monitorKms) {
			monitor = new SystemMonitorManager();
			monitor.setSamplingTime(samplingRate);
			monitor.startMonitoring();
		}

		// Start OCR
		getPresenter(index).startOcr();
		getViewer(index).startOcr();

		// Start recordings
		if (getSsim || getPsnr) {
			getPresenter(index).startRecording(
					"webRtcPeer.peerConnection.getLocalStreams()[0]");
			getViewer(index).startRecording(
					"webRtcPeer.peerConnection.getRemoteStreams()[0]");
		}

		// Play video
		int playTime = ((sessionsNumber - index - 1) * sessionRateTime / 1000)
				+ sessionPlayTime;
		playTime += extraTimePerFakeClients;
		log.info(
				"[Session {}] Total play time {} seconds (extra time because of fake clients {})",
				index, playTime, extraTimePerFakeClients);
		waitSeconds(playTime);

		// Get OCR results and statistics
		log.info("[Session {}] Get OCR results and statistics", index);
		Map<String, Map<String, Object>> presenterMap = getPresenter(index)
				.getOcrMap();
		Map<String, Map<String, Object>> viewerMap = getViewer(index)
				.getOcrMap();

		// Stop recordings
		if (getSsim || getPsnr) {
			log.info("[Session {}] Stop recordings", index);
			getPresenter(index).stopRecording();
			getViewer(index).stopRecording();
		}

		// Serialize data
		if (serializeData) {
			log.info("[Session {}] Serialize data", index);
			serializeObject(presenterMap, outputFolder + "presenter.ser");
			serializeObject(viewerMap, outputFolder + "viewer.ser");
		}

		// Finish OCR
		log.info("[Session {}] Finish OCR", index);
		getPresenter(index).endOcr();
		getViewer(index).endOcr();

		// Store recordings
		File presenterFileRec = null, viewerFileRec = null;
		if (getSsim || getPsnr) {
			log.info("[Session {}] Store recordings", index);
			String presenterRecName = "presenter-session" + index + ".webm";
			String viewerRecName = "viewer-session" + index + ".webm";

			if (nativeDownload) {
				String downloadsFolder = System.getProperty("user.home")
						+ File.separator + downloadsFolderName + File.separator;

				// Delete if exists previously (since browsers does not
				// overwrite it)
				presenterFileRec = new File(downloadsFolder + presenterRecName);
				if (presenterFileRec.exists()) {
					log.info("[Session {}] {} already exists ... deleting {} ",
							index, presenterFileRec, presenterFileRec.delete());
				}
				viewerFileRec = new File(downloadsFolder + viewerRecName);
				if (viewerFileRec.exists()) {
					log.info("[Session {}] {} already exists ... deleting {} ",
							index, viewerFileRec, viewerFileRec.delete());
				}

				presenterFileRec = getPresenter(index)
						.saveRecordingToDisk(presenterRecName, downloadsFolder);
				viewerFileRec = getViewer(index)
						.saveRecordingToDisk(viewerRecName, downloadsFolder);

			} else {
				presenterFileRec = getPresenter(index)
						.getRecording(outputFolder + presenterRecName);
				viewerFileRec = getViewer(index)
						.getRecording(outputFolder + viewerRecName);
			}
		}

		// Stop presenter and viewer(s)
		log.info("[Session {}] Stop presenter and viewer(s)", index);
		getPresenter(index).getBrowser().getWebDriver()
				.findElement(By.id("stop")).click();
		getViewer(index).getBrowser().getWebDriver().findElement(By.id("stop"))
				.click();

		// Close browsers
		log.info("[Session {}] Close browsers", index);
		getPresenter(index).close();
		getViewer(index).close();

		// Stop monitor
		if (monitorKms) {
			monitor.stop();
			monitor.writeResults(outputFolder + this.getClass().getSimpleName()
					+ "-monitor.csv");
			monitor.destroy();
		}

		// Get E2E latency and statistics
		log.info("[Session {}] Calculating latency and collecting stats",
				index);
		csvTable = processOcrAndStats(presenterMap, viewerMap);
		int columnIndex = 1;

		// Get quality metrics (SSIM, PSNR)
		if (getSsim) {
			log.info("[Session {}] Calculating quality of video (SSIM)", index);
			Multimap<String, Object> ssim = getSsim(presenterFileRec,
					viewerFileRec);
			columnIndex++;
			addColumnsToTable(csvTable, ssim, columnIndex);
		}
		if (getPsnr) {
			log.info("[Session {}] Calculating quality of video (PSNR)", index);
			Multimap<String, Object> psnr = getPsnr(presenterFileRec,
					viewerFileRec);
			columnIndex++;
			addColumnsToTable(csvTable, psnr, columnIndex);
		}

	}

	@After
	public void writeCsv() throws IOException {
		if (csvTable != null) {
			String outputCsvFile = outputFolder
					+ this.getClass().getSimpleName() + "-session" + index
					+ ".csv";
			writeCSV(outputCsvFile, csvTable);
		}

		log.info("[Session {}] End of test", index);
	}

}
