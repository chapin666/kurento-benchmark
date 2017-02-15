/*
 * (C) Copyright 2017 CodeUrjc (http://www.code.etsii.urjc.es/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

var ws = new WebSocket('wss://' + location.host + '/benchmark');
var video;
var webRtcPeer;
var mediaPipelineLatencies;
var filterLatencies;

window.onload = function() {
	console = new Console();
	console["debug"] = console.info;
	video = document.getElementById('video');
	disableStopButton();

	$('input[type=radio][name=removeFakeClients]').change(function() {
		$('#playTime').attr('disabled', this.value == 'false');
	});

}

window.onbeforeunload = function() {
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'presenterResponse':
	case 'viewerResponse':
		response(parsedMessage);
		break;
	case 'error':
		console.error("Error message from server: " + parsedMessage.message);
	case 'stopCommunication':
		dispose();
		break;
	case 'stopResponse':
		if (parsedMessage.mediaPipelineLatencies) {
			mediaPipelineLatencies = parsedMessage.mediaPipelineLatencies;
			filterLatencies = parsedMessage.filterLatencies;
		}
		break;
	case 'iceCandidate':
		webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
			if (error) {
				return console.error("Error adding candidate: " + error);
			}
		});
		break;
	case 'notEnoughResources':
		stop(false);
		$('#resourcesDialog').modal('show');
		break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}

function response(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
			if (error) {
				return console.error(error);
			}
		});
	}
}

function presenter() {
	if (!webRtcPeer) {
		showSpinner(video);

		var options = {
			localVideo : video,
			onicecandidate : onIceCandidate
		}
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
				function(error) {
					if (error) {
						return console.error(error);
					}
					webRtcPeer.generateOffer(onOfferPresenter);
				});

		enableStopButton();
	}
}

function onOfferPresenter(error, offerSdp) {
	if (error) {
		return console.error('Error generating the offer');
	}
	console.info('Invoking SDP offer callback function ' + location.host);

	var sessionNumber = document.getElementById('sessionNumber').value;
	var bandwidth = document.getElementById('bandwidth').value;
	var message = {
		id : 'presenter',
		sessionNumber : sessionNumber,
		sdpOffer : offerSdp,
		bandwidth : bandwidth
	}
	sendMessage(message);
}

function viewer() {
	if (!webRtcPeer) {
		showSpinner(video);

		var options = {
			remoteVideo : video,
			onicecandidate : onIceCandidate
		}
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
				function(error) {
					if (error) {
						return console.error(error);
					}
					this.generateOffer(onOfferViewer);
				});

		enableStopButton();
	}
}

function onOfferViewer(error, offerSdp) {
	if (error) {
		return console.error('Error generating the offer ' + error);
	}
	console.info('Invoking SDP offer callback function ' + location.host);

	var sessionNumber = document.getElementById('sessionNumber').value;
	var processing = document.getElementById('processing').value;
	var fakeClients = document.getElementById('fakeClients').value;
	var removeFakeClients = document.getElementsByName('removeFakeClients')[0].checked;
	var timeBetweenClients = document.getElementById('timeBetweenClients').value;
	var playTime = document.getElementById('playTime').value;
	var fakeClientsPerInstance = document
			.getElementById('fakeClientsPerInstance').value;
	var bandwidth = document.getElementById('bandwidth').value;

	var message = {
		id : 'viewer',
		sessionNumber : sessionNumber,
		sdpOffer : offerSdp,
		processing : processing,
		fakeClients : fakeClients,
		removeFakeClients : removeFakeClients,
		timeBetweenClients : timeBetweenClients,
		playTime : playTime,
		fakeClientsPerInstance : fakeClientsPerInstance,
		bandwidth : bandwidth
	}
	sendMessage(message);
}

function onIceCandidate(candidate) {
	console.log("Local candidate" + JSON.stringify(candidate));
	var sessionNumber = document.getElementById('sessionNumber').value;

	var message = {
		id : 'onIceCandidate',
		sessionNumber : sessionNumber,
		candidate : candidate
	};
	sendMessage(message);
}

function stop() {
	var sessionNumber = document.getElementById('sessionNumber').value;
	var message = {
		id : 'stop',
		sessionNumber : sessionNumber
	}
	sendMessage(message);
	dispose();
}

function dispose() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	hideSpinner(video);

	disableStopButton();
}

function disableStopButton() {
	enableButton('#presenter', 'presenter()');
	enableButton('#viewer', 'viewer()');
	disableButton('#stop');
}

function enableStopButton() {
	disableButton('#presenter');
	disableButton('#viewer');
	enableButton('#stop', 'stop()');
}

function disableButton(id) {
	$(id).attr('disabled', true);
	$(id).removeAttr('onclick');
}

function enableButton(id, functionName) {
	$(id).attr('disabled', false);
	$(id).attr('onclick', functionName);
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
