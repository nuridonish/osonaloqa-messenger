package tj.osonaloqa.chat.ui.screens.call

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.remote.WsMessage

/** Идоракунандаи як занги воқеӣ (WebRTC) */
class CallSession(
    private val context: Context,
    private val chatId: String,
    val outgoing: Boolean,
    val videoCall: Boolean,
) {
    companion object {
        @Volatile var eglBase: EglBase? = null
        @Volatile var factory: PeerConnectionFactory? = null

        fun ensureFactory(context: Context): PeerConnectionFactory {
            if (factory == null) {
                eglBase = EglBase.create()
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                factory = PeerConnectionFactory.builder()
                    .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true))
                    .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase!!.eglBaseContext))
                    .createPeerConnectionFactory()
            }
            return factory!!
        }
    }

    var peerConnection: PeerConnection? = null
    var videoCapturer: CameraVideoCapturer? = null
    var videoSource: VideoSource? = null
    var audioSource: AudioSource? = null
    var localVideoTrack: VideoTrack? = null
    var localAudioTrack: AudioTrack? = null
    var remoteVideoTrack: VideoTrack? = null
    var remoteRenderer: SurfaceViewRenderer? = null
    var localRenderer: SurfaceViewRenderer? = null

    private val ws get() = Dependencies.get().wsClient

    var started = false

    fun start() {
        if (started) return
        started = true
        val f = ensureFactory(context)
        peerConnection = f.createPeerConnection(rtcConfig(), Observer())
        audioSource = f.createAudioSource(MediaConstraints())
        localAudioTrack = f.createAudioTrack("a0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("s"))
        if (videoCall) {
            val names = Camera2Enumerator(context).deviceNames
            if (names.isNotEmpty()) {
                val capturer = Camera2Enumerator(context).createCapturer(names.first(), null)
                videoCapturer = capturer
                videoSource = f.createVideoSource(capturer.isScreencast)
                capturer.initialize(SurfaceTextureHelper.create("cap", eglBase!!.eglBaseContext), context, videoSource!!.capturerObserver)
                capturer.startCapture(1280, 720, 30)
                localVideoTrack = f.createVideoTrack("v0", videoSource)
                peerConnection?.addTrack(localVideoTrack, listOf("s"))
                localVideoTrack?.addSink(localRenderer)
            }
        }
    }

    private fun rtcConfig(): PeerConnection.RTCConfiguration {
        val stun = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        return PeerConnection.RTCConfiguration(listOf(stun)).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
    }

    fun callPeer() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(Simple(), desc)
                ws.send(WsMessage(type = "call.invite", chatId = chatId, video = videoCall, sdp = desc.description))
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    fun acceptAsCallee(incomingSdp: String?) {
        if (incomingSdp != null) {
            peerConnection?.setRemoteDescription(Simple(), SessionDescription(SessionDescription.Type.OFFER, incomingSdp))
            hasRemoteDesc = true
            flushIce()
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription) {
                    peerConnection?.setLocalDescription(Simple(), desc)
                    ws.send(WsMessage(type = "call.accept", chatId = chatId, sdp = desc.description))
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {}
                override fun onSetFailure(error: String?) {}
            }, MediaConstraints())
        }
    }

    fun answerArrived(answerSdp: String?) {
        if (answerSdp != null) {
            peerConnection?.setRemoteDescription(Simple(), SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
            hasRemoteDesc = true
            flushIce()
        }
    }

    private fun flushIce() {
        val q = pendingIce.toList()
        pendingIce.clear()
        q.forEach { peerConnection?.addIceCandidate(it) }
    }

    var onStateChanged: ((String) -> Unit)? = null

    private var hasRemoteDesc = false
    private val pendingIce = mutableListOf<IceCandidate>()

    fun iceArrived(candidate: String?, mid: String?, index: Int?) {
        if (candidate == null) return
        val c = IceCandidate(mid, index ?: 0, candidate)
        if (hasRemoteDesc) peerConnection?.addIceCandidate(c) else pendingIce.add(c)
    }

    fun toggleMic(): Boolean {
        localAudioTrack?.setEnabled(!(localAudioTrack?.enabled() ?: true))
        return localAudioTrack?.enabled() ?: true
    }

    fun toggleVideo(): Boolean {
        localVideoTrack?.setEnabled(!(localVideoTrack?.enabled() ?: true))
        return localVideoTrack?.enabled() ?: true
    }

    fun hangUp() {
        ws.send(WsMessage(type = "call.end", chatId = chatId))
        dispose()
    }

    fun dispose() {
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        runCatching { localRenderer?.release() }
        runCatching { remoteRenderer?.release() }
        peerConnection?.close()
        peerConnection = null
    }

    private inner class Observer : PeerConnection.Observer {
        override fun onIceCandidate(c: IceCandidate) {
            ws.send(WsMessage(type = "call.ice", chatId = chatId, candidate = c.sdp, sdpMid = c.sdpMid, sdpMLineIndex = c.sdpMLineIndex))
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {}
        override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) {
            val t = transceiver?.receiver?.track()
            if (t is VideoTrack) {
                remoteVideoTrack = t
                remoteRenderer?.let { t.addSink(it) }
            }
        }
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            if (state == PeerConnection.IceConnectionState.FAILED) onStateChanged?.invoke("ended")
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onRenegotiationNeeded() {}
        override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
    }


    private class Simple : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}

@Composable
fun CallScreen(
    chatId: String,
    peerName: String,
    outgoing: Boolean,
    videoCall: Boolean,
    onEnd: () -> Unit,
) {
    val context = LocalContext.current
    val container = Dependencies.get()
    var permissions by remember { mutableStateOf(false) }
    val session = remember { CallSession(context, chatId, outgoing, videoCall) }
    var status by remember { mutableStateOf(if (outgoing) "calling" else "incoming") }
    var micOn by remember { mutableStateOf(true) }
    var camOn by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }

    val callEvents by container.chatRepository.callEvents.collectAsState()

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissions = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        session.onStateChanged = { status = it }
        permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
    }

    LaunchedEffect(permissions) {
        if (permissions) {
            session.start()
            if (outgoing) session.callPeer()
        }
    }

    LaunchedEffect(callEvents?.type, callEvents?.sdp, callEvents?.candidate) {
        val ev = callEvents ?: return@LaunchedEffect
        if (ev.chatId != chatId) return@LaunchedEffect
        when (ev.type) {
            "call.accept" -> {
                if (outgoing) {
                    session.answerArrived(ev.sdp)
                    status = "connected"
                }
            }
            "call.sdp" -> {
                if (outgoing && status == "calling") {
                    session.answerArrived(ev.sdp)
                    status = "connected"
                }
            }
            "call.ice" -> {
                // Кандидатҳо фавран татбиқ мешаванд; агар remote ҳанӯз набошад — дар навбат мемонанд
                session.iceArrived(ev.candidate, ev.sdpMid, ev.sdpMLineIndex)
            }
            "call.reject", "call.end" -> {
                session.dispose()
                ended = true
            }
        }
    }

    LaunchedEffect(ended) { if (ended) onEnd() }

    DisposableEffect(Unit) {
        onDispose { session.dispose() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1410)),
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    init(CallSession.eglBase?.eglBaseContext, null)
                    session.remoteRenderer = this
                    session.remoteVideoTrack?.addSink(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (videoCall) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(CallSession.eglBase?.eglBaseContext, null)
                        setZOrderMediaOverlay(true)
                        session.localRenderer = this
                        session.localVideoTrack?.addSink(this)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp),
            )
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(peerName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                when (status) {
                    "calling" -> stringResource(R.string.call_calling)
                    "incoming" -> stringResource(R.string.call_incoming)
                    "connected" -> stringResource(R.string.call_connected)
                    else -> stringResource(R.string.call_ended)
                },
                color = Color(0xFF2BD46B),
                fontSize = 16.sp,
            )
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .background(Color(0xFF22302A), CircleShape)
                    .clickableNoIndication { micOn = session.toggleMic() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            if (videoCall) {
                Box(
                    Modifier
                        .size(60.dp)
                        .background(Color(0xFF22302A), CircleShape)
                        .clickableNoIndication { camOn = session.toggleVideo() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (camOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            if (!outgoing && status == "incoming") {
                Box(
                    Modifier
                        .size(68.dp)
                        .background(Color(0xFF2BD46B), CircleShape)
                        .clickableNoIndication {
                            session.start()
                            // SDP-и даъват аз pendingCall — на аз callEvents (ки ICE ҷойгирин мекунад)
                            val inviteSdp = container.chatRepository.pendingCall.value?.sdp
                            session.acceptAsCallee(inviteSdp)
                            // Кандидатҳои дар навбат мондашуда
                            val queued = container.chatRepository.iceQueue.toList()
                            container.chatRepository.iceQueue.clear()
                            queued.forEach { session.iceArrived(it.candidate, it.sdpMid, it.sdpMLineIndex) }
                            status = "connected"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            Box(
                Modifier
                    .size(68.dp)
                    .background(Color(0xFFD93025), CircleShape)
                    .clickableNoIndication {
                        if (!ended) {
                            ended = true
                            session.hangUp()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
