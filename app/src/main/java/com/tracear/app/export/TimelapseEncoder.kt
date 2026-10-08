package com.tracear.app.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hardware-accelerated H.264 (video/avc) video encoder that takes a sequence of
 * JPEG image files and encodes them into a standard MP4 file using MediaCodec and MediaMuxer.
 */
object TimelapseEncoder {

    private const val TAG = "TimelapseEncoder"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val TIMEOUT_USEC = 10_000L

    /**
     * Encodes the provided list of JPEG frame files into an MP4 file.
     * Runs off-thread on Dispatchers.IO.
     *
     * @param frameFiles sorted list of temporary JPEG files.
     * @param outputFile destination .mp4 file.
     * @param width output video width (even number).
     * @param height output video height (even number).
     * @param fps playback frame rate (10 to 30 fps).
     * @param cancelFlag atomic boolean checked before each frame to allow responsive cancel.
     * @param onProgress callback invoked with progress from 0.0 to 1.0.
     * @return true on success, false on failure or cancel.
     */
    suspend fun encodeFrames(
        frameFiles: List<File>,
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int = 24,
        cancelFlag: AtomicBoolean = AtomicBoolean(false),
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (frameFiles.isEmpty()) {
            Log.w(TAG, "No frame files provided for timelapse encoding")
            return@withContext false
        }

        val (evenW, evenH) = ExportMath.ensureEvenDimensions(width, height)
        val bitRate = (evenW * evenH * 3.5f).toInt().coerceIn(2_000_000, 12_000_000)

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var eglHelper: EglHelper? = null
        var glRenderer: GlQuadRenderer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            // 1. Configure MediaCodec video encoder
            val format = MediaFormat.createVideoFormat(MIME_TYPE, evenW, evenH).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            // 2. Set up EGL context connected to encoder inputSurface
            eglHelper = EglHelper(inputSurface)
            eglHelper.makeCurrent()
            glRenderer = GlQuadRenderer()

            // 3. Initialize MediaMuxer
            if (outputFile.exists()) {
                outputFile.delete()
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val totalFrames = frameFiles.size

            // 4. Encode each frame
            for (i in 0 until totalFrames) {
                if (cancelFlag.get()) {
                    Log.i(TAG, "Timelapse encoding cancelled by user at frame $i/$totalFrames")
                    return@withContext false
                }

                val frameFile = frameFiles[i]
                if (!frameFile.exists()) continue

                // Decode bitmap with inSampleSize if file is larger than video dimension
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(frameFile.absolutePath, options)
                val sampleSize = calculateInSampleSize(options, evenW, evenH)
                options.inJustDecodeBounds = false
                options.inSampleSize = sampleSize
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                val bitmap = BitmapFactory.decodeFile(frameFile.absolutePath, options) ?: continue

                try {
                    // Render bitmap to encoder surface
                    GLES20.glViewport(0, 0, evenW, evenH)
                    glRenderer.drawBitmap(bitmap)

                    val ptsNs = (i.toLong() * 1_000_000_000L / fps.toLong())
                    eglHelper.setPresentationTime(ptsNs)
                    eglHelper.swapBuffers()

                    // Drain output buffers from encoder
                    while (true) {
                        val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                        if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break
                        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (muxerStarted) {
                                throw IllegalStateException("Format changed twice")
                            }
                            val newFormat = encoder.outputFormat
                            trackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        } else if (outputBufferId >= 0) {
                            val encodedData = encoder.getOutputBuffer(outputBufferId)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outputBufferId, false)
                        }
                    }
                } finally {
                    bitmap.recycle()
                }

                onProgress((i + 1).toFloat() / totalFrames)
            }

            // 5. Signal End-of-Stream to encoder
            encoder.signalEndOfInputStream()

            // Drain remaining EOS output buffers
            var eosReached = false
            var drainAttempts = 0
            while (!eosReached && drainAttempts < 100) {
                drainAttempts++
                val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outputBufferId >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferId)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReached = true
                    }
                    encoder.releaseOutputBuffer(outputBufferId, false)
                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (drainAttempts > 15) break
                }
            }

            Log.i(TAG, "Timelapse video successfully encoded: ${outputFile.length()} bytes, $totalFrames frames")
            return@withContext true

        } catch (e: Throwable) {
            Log.e(TAG, "Error encoding timelapse video: ${e.message}", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return@withContext false
        } finally {
            try {
                if (muxerStarted && muxer != null) {
                    muxer.stop()
                }
            } catch (ignored: Throwable) {}
            try { muxer?.release() } catch (ignored: Throwable) {}
            try { encoder?.stop() } catch (ignored: Throwable) {}
            try { encoder?.release() } catch (ignored: Throwable) {}
            try { glRenderer?.release() } catch (ignored: Throwable) {}
            try { eglHelper?.release() } catch (ignored: Throwable) {}
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val (h, w) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    // ── Internal EGL Management ──────────────────────────────
    private class EglHelper(surface: android.view.Surface) {
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        init {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("unable to get EGL14 display")
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw RuntimeException("unable to initialize EGL14")
            }

            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                0x3142, 1, // EGL_RECORDABLE_ANDROID
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
            if (numConfigs[0] <= 0 || configs[0] == null) {
                throw RuntimeException("unable to find suitable EGLConfig")
            }

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) throw RuntimeException("null context")

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) throw RuntimeException("null surface")
        }

        fun makeCurrent() {
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw RuntimeException("eglMakeCurrent failed")
            }
        }

        fun setPresentationTime(nsecs: Long) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        }

        fun swapBuffers() {
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        fun release() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    // ── Internal OpenGL Full-Screen Quad Shader ───────────────
    private class GlQuadRenderer {
        private val vertexShaderSource = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        private val fragmentShaderSource = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """.trimIndent()

        private var program = 0
        private var textureId = 0
        private val vertexBuffer: FloatBuffer
        private val texCoordBuffer: FloatBuffer

        init {
            // Full-screen quad in NDC: (-1, -1) to (1, 1)
            val vertices = floatArrayOf(
                -1f, -1f,
                 1f, -1f,
                -1f,  1f,
                 1f,  1f
            )
            // UV texture coordinates: flipped vertically so top-left of image matches OpenGL
            val texCoords = floatArrayOf(
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
            )

            vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(vertices); position(0)
            }
            texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(texCoords); position(0)
            }

            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
            program = GLES20.glCreateProgram().also {
                GLES20.glAttachShader(it, vs)
                GLES20.glAttachShader(it, fs)
                GLES20.glLinkProgram(it)
            }

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }

        fun drawBitmap(bitmap: Bitmap) {
            GLES20.glUseProgram(program)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            val posLoc = GLES20.glGetAttribLocation(program, "aPosition")
            val texLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
            val sampLoc = GLES20.glGetUniformLocation(program, "uTexture")

            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

            GLES20.glEnableVertexAttribArray(texLoc)
            GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

            GLES20.glUniform1i(sampLoc, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(posLoc)
            GLES20.glDisableVertexAttribArray(texLoc)
        }

        fun release() {
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            if (textureId != 0) {
                val textures = intArrayOf(textureId)
                GLES20.glDeleteTextures(1, textures, 0)
                textureId = 0
            }
        }

        private fun compileShader(type: Int, shaderCode: String): Int {
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
        }
    }
}
