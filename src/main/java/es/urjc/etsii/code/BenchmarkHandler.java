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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.internal.NotEnoughResourcesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Handler (application and media logic).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.6.1
 */
public class BenchmarkHandler extends TextWebSocketHandler {

	private final Logger log = LoggerFactory.getLogger(BenchmarkHandler.class);

	private Map<String, Map<String, UserSession>> viewers = new ConcurrentHashMap<>();
	private Map<String, UserSession> presenters = new ConcurrentHashMap<>();

	@Override
	public void handleTextMessage(WebSocketSession wsSession,
			TextMessage message) throws Exception {
		String sessionNumber = null;
		try {
			JsonObject jsonMessage = new GsonBuilder().create()
					.fromJson(message.getPayload(), JsonObject.class);

			sessionNumber = jsonMessage.get("sessionNumber").getAsString();
			log.debug("[Session number {} - WS session {}] Incoming message {}",
					sessionNumber, wsSession.getId(), jsonMessage);

			switch (jsonMessage.get("id").getAsString()) {
			case "presenter":
				presenter(wsSession, sessionNumber, jsonMessage);
				break;
			case "viewer":
				viewer(wsSession, sessionNumber, jsonMessage);
				break;
			case "onIceCandidate":
				onIceCandidate(wsSession, sessionNumber, jsonMessage);
				break;
			case "stop":
				stop(wsSession, sessionNumber);
			default:
				break;
			}

		} catch (NotEnoughResourcesException e) {
			log.warn("[Session number {} - WS session {}] Not enough resources",
					sessionNumber, wsSession.getId(), e);
			notEnoughResources(wsSession, sessionNumber);

		} catch (Throwable t) {
			log.error(
					"[Session number {} - WS session {}] Exception in handler",
					sessionNumber, wsSession.getId(), t);
			handleErrorResponse(wsSession, sessionNumber, t);
		}
	}

	private synchronized void presenter(WebSocketSession wsSession,
			String sessionNumber, JsonObject jsonMessage) {
		if (presenters.containsKey(sessionNumber)) {
			JsonObject response = new JsonObject();
			response.addProperty("id", "presenterResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message",
					"Another user is currently acting as sender for session "
							+ sessionNumber
							+ ". Chose another session number ot try again later ...");
			sendMessage(wsSession, sessionNumber,
					new TextMessage(response.toString()));

		} else {
			int bandwidth = jsonMessage.getAsJsonPrimitive("bandwidth")
					.getAsInt();
			UserSession presenterSession = new UserSession(wsSession,
					sessionNumber, this, bandwidth);
			presenters.put(sessionNumber, presenterSession);

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer")
					.getAsString();
			presenterSession.initPresenter(sdpOffer);
		}
	}

	private synchronized void viewer(WebSocketSession wsSession,
			String sessionNumber, JsonObject jsonMessage)
			throws InterruptedException {
		String wsSessionId = wsSession.getId();

		if (presenters.containsKey(sessionNumber)) {
			// Entry for viewers map
			Map<String, UserSession> viewersPerPresenter;
			if (viewers.containsKey(sessionNumber)) {
				viewersPerPresenter = viewers.get(sessionNumber);
			} else {
				viewersPerPresenter = new ConcurrentHashMap<>();
				viewers.put(sessionNumber, viewersPerPresenter);
			}

			if (viewersPerPresenter.containsKey(wsSessionId)) {
				JsonObject response = new JsonObject();
				response.addProperty("id", "viewerResponse");
				response.addProperty("response", "rejected");
				response.addProperty("message",
						"You are already viewing in session number "
								+ viewersPerPresenter
								+ ". Use a different browser/tab to add additional viewers.");
				sendMessage(wsSession, sessionNumber,
						new TextMessage(response.toString()));
			} else {
				int bandwidth = jsonMessage.getAsJsonPrimitive("bandwidth")
						.getAsInt();
				UserSession viewerSession = new UserSession(wsSession,
						sessionNumber, this, bandwidth);
				viewersPerPresenter.put(wsSessionId, viewerSession);

				viewerSession.initViewer(presenters.get(sessionNumber),
						jsonMessage);
			}

		} else {
			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message",
					"No active presenter for sesssion number " + sessionNumber
							+ " now. Become sender or try again later ...");
			sendMessage(wsSession, sessionNumber,
					new TextMessage(response.toString()));
		}
	}

	private synchronized void stop(WebSocketSession wsSession,
			String sessionNumber) {
		String wsSessionId = wsSession.getId();
		Map<String, UserSession> viewersPerPresenter = null;
		UserSession userSession = findUserByWsSession(wsSession);
		log.info("[Session number {} - WS session {}] Stopping session",
				sessionNumber, wsSessionId);

		if (presenters.containsKey(sessionNumber)
				&& presenters.get(sessionNumber).getWebSocketSession() != null
				&& userSession.getWebSocketSession() != null
				&& presenters.get(sessionNumber).getWebSocketSession().getId()
						.equals(userSession.getWebSocketSession().getId())) {
			// Case 1. Stop arrive from presenter
			log.info("[Session number {} - WS session {}] Releasing presenter",
					sessionNumber, wsSessionId);

			if (viewers.containsKey(sessionNumber)) {
				viewersPerPresenter = viewers.get(sessionNumber);
				logViewers(sessionNumber, wsSessionId, viewersPerPresenter);

				for (UserSession viewer : viewersPerPresenter.values()) {
					log.info(
							"[Session number {} - WS session {}] Sending stopCommunication message to viewer",
							sessionNumber,
							viewer.getWebSocketSession().getId());

					// Send stopCommunication to viewer
					JsonObject response = new JsonObject();
					response.addProperty("id", "stopCommunication");
					sendMessage(viewer.getWebSocketSession(), sessionNumber,
							new TextMessage(response.toString()));
					sendStopResponse(viewer.getWebSocketSession(),
							viewer.getSessionNumber());

					// Release viewer
					viewersPerPresenter
							.get(viewer.getWebSocketSession().getId())
							.releaseViewer();
					viewersPerPresenter
							.remove(viewer.getWebSocketSession().getId());
				}
				// Remove viewer session from map
				viewers.remove(sessionNumber);
			}

			// Release presenter session
			presenters.get(sessionNumber).releasePresenter();

			// Remove presenter session from map
			presenters.remove(sessionNumber);

		} else if (viewers.containsKey(sessionNumber)
				&& viewers.get(sessionNumber).containsKey(wsSessionId)) {
			// Case 2. Stop arrive from viewer

			// Send latencies
			sendStopResponse(wsSession, sessionNumber);

			viewersPerPresenter = viewers.get(sessionNumber);
			logViewers(sessionNumber, wsSessionId, viewersPerPresenter);
			viewersPerPresenter.get(wsSessionId).releaseViewer();
			viewersPerPresenter.remove(wsSessionId);
		}

		logViewers(sessionNumber, wsSessionId, viewersPerPresenter);
	}

	private void sendStopResponse(WebSocketSession wsSession,
			String sessionNumber) {
		log.info(
				"[Session number {} - WS session {}] Sending stopResponse message to viewer",
				sessionNumber, wsSession.getId());

		JsonObject stopResponse = new JsonObject();
		stopResponse.addProperty("id", "stopResponse");
		sendMessage(wsSession, sessionNumber,
				new TextMessage(stopResponse.toString()));
	}

	private void logViewers(String sessionNumber, String wsSessionId,
			Map<String, UserSession> viewers) {
		if (viewers != null) {
			log.info(
					"[Session number {} - WS session {}] There are {} real viewer(s) at this moment",
					sessionNumber, wsSessionId, viewers.size());
		}
	}

	private void onIceCandidate(WebSocketSession wsSession,
			String sessionNumber, JsonObject jsonMessage) {
		JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
		String wsSessionId = wsSession.getId();
		UserSession userSession = findUserByWsSession(wsSession);

		if (userSession != null) {
			userSession.addCandidate(candidate);
		} else {
			log.warn(
					"[Session number {} - WS session {}] ICE candidate not valid: {}",
					sessionNumber, wsSessionId, candidate);
		}
	}

	private void handleErrorResponse(WebSocketSession wsSession,
			String sessionNumber, Throwable throwable) {
		// Send error message to client
		JsonObject response = new JsonObject();
		response.addProperty("id", "error");
		response.addProperty("response", "rejected");
		response.addProperty("message", throwable.getMessage());
		sendMessage(wsSession, sessionNumber,
				new TextMessage(response.toString()));
		log.error("[Session number {} - WS session {}] Error handling message",
				sessionNumber, wsSession.getId(), throwable);

		// Release media session
		stop(wsSession, sessionNumber);
	}

	private void notEnoughResources(WebSocketSession wsSession,
			String sessionNumber) {
		// Send notEnoughResources message to client
		JsonObject response = new JsonObject();
		response.addProperty("id", "notEnoughResources");
		sendMessage(wsSession, sessionNumber,
				new TextMessage(response.toString()));

		// Release media session
		stop(wsSession, sessionNumber);
	}

	private UserSession findUserByWsSession(WebSocketSession wsSession) {
		String wsSessionId = wsSession.getId();
		// Find WS session in presenters
		for (String sessionNumber : presenters.keySet()) {
			if (presenters.get(sessionNumber).getWebSocketSession().getId()
					.equals(wsSessionId)) {
				return presenters.get(sessionNumber);
			}
		}

		// Find WS session in viewers
		for (String sessionNumber : viewers.keySet()) {
			for (UserSession userSession : viewers.get(sessionNumber)
					.values()) {
				if (userSession.getWebSocketSession().getId()
						.equals(wsSessionId)) {
					return userSession;
				}
			}
		}
		return null;
	}

	public synchronized void sendMessage(WebSocketSession session,
			String sessionNumber, TextMessage message) {
		try {
			log.debug(
					"[Session number {} - WS session {}] Sending message {} in session {}",
					sessionNumber, session.getId(), message.getPayload());
			session.sendMessage(message);

		} catch (IOException e) {
			log.error(
					"[Session number {} - WS session {}] Exception sending message",
					sessionNumber, session.getId(), e);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession wsSession,
			CloseStatus status) throws Exception {
		String wsSessionId = wsSession.getId();
		String sessionNumber = "-";
		UserSession userSession = findUserByWsSession(wsSession);

		if (userSession != null) {
			sessionNumber = userSession.getSessionNumber();
			stop(wsSession, sessionNumber);
		}
		log.info("[Session number {} - WS session {}] WS connection closed",
				sessionNumber, wsSessionId);
	}

}
