package com.example.palmcollector

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import com.google.mediapipe.components.TextureFrameConsumer
import com.google.mediapipe.framework.TextureFrame
import com.google.mediapipe.solutioncore.ErrorListener
import com.google.mediapipe.solutioncore.ResultListener
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class CameraActivity : AppCompatActivity(){

    private val TAG = "MainActivity"

    private var hands: Hands? = null

    private var palmOrBack: String? = null

    // Run the pipeline and the model inference on GPU or CPU.
    private val RUN_ON_GPU = true

    private var imageView: HandsResultImageView? = null

    private var leftOrRight: String? = null

    private var framecount = 0

    //private var flashButton = findViewById<ImageButton>(R.id.btn_flash_toggle)

    private enum class InputSource {
        CAMERA
    }

    private var glSurfaceView: CustomSurfaceView<HandsResult>? = null

    // Live camera demo UI and camera components.
    private var cameraInput: CustomCameraInput? = null

    private var inputSource: InputSource = InputSource.CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // hide the action bar
        supportActionBar?.hide()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        setupLiveDemoUiComponents()
        val toggle: ToggleButton = findViewById(R.id.btn_toggle_flash)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            Log.e("toggles", "to")
            if(isChecked){
                cameraInput!!.torchOn()
            } else {
                cameraInput!!.torchOff()
            }
            //flash = !isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = CustomCameraInput(this)
            cameraInput!!.setNewFrameListener { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            }
            glSurfaceView?.post { startCamera() }
            glSurfaceView?.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.visibility = View.GONE
            cameraInput!!.close()
        }
    }

    //Hands code

    private fun setupLiveDemoUiComponents() {
        stopCurrentPipeline()
        setupStreamingModePipeline(InputSource.CAMERA)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupStreamingModePipeline(inputSource: InputSource) {
        this.inputSource = inputSource
        // Initializes a new MediaPipe Hands solution instance in the streaming mode.
        hands = Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build()
        )
        hands!!.setErrorListener(ErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG,
                "MediaPipe Hands error:$message"
            )
        })
        if (inputSource == InputSource.CAMERA) {
            cameraInput = CustomCameraInput(this)
            cameraInput!!.setNewFrameListener(TextureFrameConsumer { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            })
        }

        val guide = findViewById<TextView>(R.id.camera_capture_button)

//         Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
        glSurfaceView = CustomSurfaceView<HandsResult>(
            this,
            hands!!.glContext,
            hands!!.glMajorVersion
        )
        glSurfaceView!!.setSolutionResultRenderer(HandsResultGlRenderer())
        glSurfaceView!!.setRenderInputImage(true)
        hands!!.setResultListener(
            ResultListener { handsResult: HandsResult? ->
                logWristLandmark(handsResult!!,  /*showPixelValues=*/false)
                glSurfaceView!!.setRenderData(handsResult)
                glSurfaceView!!.requestRender()
                //
                //
                imageView = HandsResultImageView(this)
                logWristLandmark(handsResult,  /*showPixelValues=*/true)
                imageView?.setHandsResult(handsResult)
                runOnUiThread { imageView?.update() }





                if (handsResult.let { NativeInterface().display(it).landmarksize } == 21 && imageView!!.frontOrBack(handsResult) ){

                    val handPointList = handsResult.multiHandLandmarks()?.get(0)?.landmarkList


                    Log.e("coorvalue", "${handPointList!![17].x}" )

//                   Left hand

                    if(imageView?.calculatehandedness(handsResult) == false){
                        if(handPointList!![0].y < 1.0 && handPointList!![2].x > 0.2 && handPointList!![17].x < 0.8  && handPointList!![5].y > 0.0  && ((handPointList!![5].z) - (handPointList!![17].z)) in 0.0..0.1){

                            var area = 0.0

                            for(k in 0..3){
                                var a = sqrt(Math.pow(((handPointList!![0].x - handPointList.get(4*k+1).x).toDouble()),2.0) + Math.pow(((handPointList.get(0).y - handPointList.get(4*k+1).y).toDouble()),2.0))
                                var b = sqrt(Math.pow(((handPointList.get(0).x - handPointList.get(4*(k+1)+1).x).toDouble()),2.0) + Math.pow(((handPointList.get(0).y - handPointList.get(4*(k+1)+1).y).toDouble()),2.0))
                                var c = sqrt(Math.pow(((handPointList.get(4*k+1).x - handPointList.get(4*(k+1)+1).x).toDouble()),2.0) + Math.pow(((handPointList.get(4*k+1).y - handPointList.get(4*(k+1)+1).y).toDouble()),2.0))
                                var s = a+b+c
                                area += (s*(s-a)*(s-b)*(s-c))
                            }

                            Log.e("wrist xylo areavalue", "$area")

                            //runOnUiThread {
                            //if(area in 1.5..5.0){
                            if(area > 0.4){
                                framecount++
                                Log.i("beluga_isPalm", "${imageView?.frontOrBack(handsResult)}")
                                Log.i("walter_isLeft", "${imageView?.calculatehandedness(handsResult)}")
                                runOnUiThread{guide.text = "Hold still"}

//                        if(flash){
//                            cameraInput!!.torchOn()
//                        }

                                //stopCurrentPipeline()
                                //imageanalysis
                                if(framecount>15){
                                    var bitmap = handsResult.inputBitmap()
                                    if (imageView?.calculatehandedness(handsResult) == true) {
                                        leftOrRight = "right"
                                    } else {
                                        leftOrRight = "left"
                                    }
                                    if (imageView?.frontOrBack(handsResult) == true) {
                                        palmOrBack = "palm"
                                    } else {
                                        palmOrBack = "back"
                                    }
                                    //imageanalysis
                                    val matrix = Matrix()

                                    matrix.postRotate(180f)
                                    val cx = bitmap.width / 2f
                                    val cy = bitmap.height / 2f

                                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
                                    val rotatedBitmap = Bitmap.createBitmap(
                                        scaledBitmap,
                                        0,
                                        0,
                                        scaledBitmap.width,
                                        scaledBitmap.height,
                                        matrix,
                                        true
                                    )
                                    val flippedBitmap = rotatedBitmap.flip(-1f, 1f, cx, cy)

                                    handPointList!!.get(0).x
                                    handPointList.get(0).y
                                    handPointList.get(0).z

                                    Log.e("wrist xylo x", "${handPointList.get(0).x}")
                                    Log.e("wrist xylo y", "${handPointList.get(0).y}")
                                    Log.e("wrist xylo z", "${handPointList.get(0).z}")

                                    val preview = Preview.Builder().build()

//                            val whiteu = findViewById<ImageView>(R.id.capture_screen_flash)
//                            FadeIn(whiteu, 1, 2, 5000, true)

                                    var uri = SaveImage(flippedBitmap)
                                    var i = Intent(this, AddSubjectActivity::class.java)
                                    i.putExtra("bitmapURI_intent", uri.toString())
                                    i.putExtra("handedness_intent", leftOrRight)
                                    i.putExtra("frontOrBack_intent", palmOrBack)
                                    setResult(78, i)
                                    finish()
                                }

                            } else if (area in 0.0..1.5) {
                                runOnUiThread{guide.text = "Bring palm closer"}
                                framecount = 0
                                //cameraInput!!.torchOff()
                            } else if (area > 5.0){
                                runOnUiThread{guide.text = "Place palm further"}
                                framecount = 0
                                //cameraInput!!.torchOff()
                            }
                        } else {
                            framecount = 0
                            runOnUiThread{guide.text = "No Palm detected"}
                            //cameraInput!!.torchOff()
                        }
                    }

                    //Right hand

                    if(imageView?.calculatehandedness(handsResult) == true){
                        if(handPointList!![0].y < 1.0 && handPointList!![2].x < 0.8 && handPointList!![17].x > 0.2 && handPointList!![5].y > 0.0 && ((handPointList!![5].z) - (handPointList!![17].z)) in 0.0..0.1){

                            var area = 0.0

                            Log.e("dgp", "${(handPointList!![5].z) - (handPointList!![17].z) }" )

                            for(k in 0..3){
                                var a = sqrt(Math.pow(((handPointList!![0].x - handPointList.get(4*k+1).x).toDouble()),2.0) + Math.pow(((handPointList.get(0).y - handPointList.get(4*k+1).y).toDouble()),2.0))
                                var b = sqrt(Math.pow(((handPointList.get(0).x - handPointList.get(4*(k+1)+1).x).toDouble()),2.0) + Math.pow(((handPointList.get(0).y - handPointList.get(4*(k+1)+1).y).toDouble()),2.0))
                                var c = sqrt(Math.pow(((handPointList.get(4*k+1).x - handPointList.get(4*(k+1)+1).x).toDouble()),2.0) + Math.pow(((handPointList.get(4*k+1).y - handPointList.get(4*(k+1)+1).y).toDouble()),2.0))
                                var s = a+b+c
                                area += (s*(s-a)*(s-b)*(s-c))
                            }

                            Log.e("wrist xylo areavalue", "$area")

                            //runOnUiThread {
                            //if(area in 1.5..5.0){
                            if(area > 0.4){
                                framecount++
                                Log.i("beluga_isPalm", "${imageView?.frontOrBack(handsResult)}")
                                Log.i("walter_isLeft", "${imageView?.calculatehandedness(handsResult)}")
                                runOnUiThread{guide.text = "Hold still"}

//                        if(flash){
//                            cameraInput!!.torchOn()
//                        }

                                //stopCurrentPipeline()
                                //imageanalysis
                                if(framecount>15){
                                    var bitmap = handsResult.inputBitmap()
                                    if (imageView?.calculatehandedness(handsResult) == true) {
                                        leftOrRight = "right"
                                    } else {
                                        leftOrRight = "left"
                                    }
                                    if (imageView?.frontOrBack(handsResult) == true) {
                                        palmOrBack = "palm"
                                    } else {
                                        palmOrBack = "back"
                                    }
                                    //imageanalysis
                                    val matrix = Matrix()

                                    matrix.postRotate(180f)
                                    val cx = bitmap.width / 2f
                                    val cy = bitmap.height / 2f

                                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
                                    val rotatedBitmap = Bitmap.createBitmap(
                                        scaledBitmap,
                                        0,
                                        0,
                                        scaledBitmap.width,
                                        scaledBitmap.height,
                                        matrix,
                                        true
                                    )
                                    val flippedBitmap = rotatedBitmap.flip(-1f, 1f, cx, cy)

                                    handPointList!!.get(0).x
                                    handPointList.get(0).y
                                    handPointList.get(0).z

                                    Log.e("wrist xylo x", "${handPointList.get(0).x}")
                                    Log.e("wrist xylo y", "${handPointList.get(0).y}")
                                    Log.e("wrist xylo z", "${handPointList.get(0).z}")

                                    val preview = Preview.Builder().build()

//                            val whiteu = findViewById<ImageView>(R.id.capture_screen_flash)
//                            FadeIn(whiteu, 1, 2, 5000, true)

                                    var uri = SaveImage(flippedBitmap)
                                    var i = Intent(this, AddSubjectActivity::class.java)
                                    i.putExtra("bitmapURI_intent", uri.toString())
                                    i.putExtra("handedness_intent", leftOrRight)
                                    i.putExtra("frontOrBack_intent", palmOrBack)
                                    setResult(78, i)
                                    finish()
                                }

                            } else if (area in 0.0..1.5) {
                                runOnUiThread{guide.text = "Bring palm closer"}
                                framecount = 0
                                //cameraInput!!.torchOff()
                            } else if (area > 5.0){
                                runOnUiThread{guide.text = "Place palm further"}
                                framecount = 0
                                //cameraInput!!.torchOff()
                            }
                        } else {
                            framecount = 0
                            runOnUiThread{guide.text = "No Palm detected"}
                            //cameraInput!!.torchOff()
                        }
                    }
                }
            })


        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.post(Runnable { startCamera() })
        }

        // Updates the preview layout.
        val previewView = findViewById<PreviewView>(R.id.preview_display_layout)
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        //imageView.setVisibility(View.GONE)
        previewView.removeAllViewsInLayout()
        previewView.addView(glSurfaceView)

        glSurfaceView!!.visibility = View.VISIBLE
        previewView.requestLayout()

//        previewView.afterMeasured {
//            val autoFocusPoint = SurfaceOrientedMeteringPointFactory(1f, 1f)
//                .createPoint(.5f, .5f)
//
//            val autoFocusAction = FocusMeteringAction.Builder(
//                autoFocusPoint,
//                FocusMeteringAction.FLAG_AF
//            ).apply {
//                //start auto-focusing after 2 seconds
//                setAutoCancelDuration(2, TimeUnit.SECONDS)
//            }.build()
//            //camera.cameraControl.startFocusAndMetering(autoFocusAction)
//            //cameraInput!!.cameraObj(autoFocusAction)
//        }

        //
        previewView!!.setOnTouchListener(View.OnTouchListener { view: View, motionEvent: MotionEvent ->
            //Log.e("camtouch", "touch")
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@OnTouchListener true
                MotionEvent.ACTION_UP -> {
                    // Get the MeteringPointFactory from PreviewView
                    //val factory = previewView.meteringPointFactory
//                    final MeteringPointFactory factory = previewView.createMeteringPointFactory(cameraSelector);
//                    final MeteringPoint point = factory.createPoint(event.getX(), motionEvent.getY());
//                    final FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
//                    cameraControl.startFocusAndMetering(action)
                    val display = getDisplay()

                    val factory = DisplayOrientedMeteringPointFactory(display!!, cameraInput!!.cameraInfo() , previewView.width.toFloat(), previewView.height.toFloat() )

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(motionEvent.x, motionEvent.y)

                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode

                    //val action = FocusMeteringAction.Builder(point).build()
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .addPoint(point, FocusMeteringAction.FLAG_AE)
                        .addPoint(point, FocusMeteringAction.FLAG_AWB)
                        .build()
                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    //CustomCameraXPreviewHelper.camera.cameraControl.startFocusAndMetering(action)
                    cameraInput!!.cameraObj(action)
                    Log.e("camtouchaction", "${action.toString()}")
                    Log.e("camtouchx", "${motionEvent.x}")
                    Log.e("camtouchy", "${motionEvent.y}")

                    return@OnTouchListener true
                }
                else -> return@OnTouchListener false
            }
        })
        //

//        glSurfaceView!!.setOnTouchListener { _, motionEvent ->
//            val actionMasked = motionEvent.actionMasked // Or action
//            if (actionMasked != MotionEvent.ACTION_DOWN) {
//                return@setOnTouchListener false
//            }
//            val x = motionEvent.x
//            val y = motionEvent.y
//            // More code shown below
//            val factory =
//            val point = factory.createPoint(x, y)
//            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
//                .addPoint(point, FocusMeteringAction.FLAG_AE)
//                .addPoint(point, FocusMeteringAction.FLAG_AWB)
//                .build()
//        }
    }

//    inline fun View.afterMeasured(crossinline block: () -> Unit) {
//        if (measuredWidth > 0 && measuredHeight > 0) {
//            block()
//        } else {
//            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//                override fun onGlobalLayout() {
//                    if (measuredWidth > 0 && measuredHeight > 0) {
//                        viewTreeObserver.removeOnGlobalLayoutListener(this)
//                        block()
//                    }
//                }
//            })
//        }
//    }


    private fun Bitmap.flip(x: Float, y: Float, cx: Float, cy: Float): Bitmap {
        val matrix = Matrix().apply { postScale(x, y, cx, cy) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun logWristLandmark(result: HandsResult, showPixelValues: Boolean) {
        if (result.multiHandLandmarks().isEmpty()) {
            return
        }
        val wristLandmark = result.multiHandLandmarks()[0].landmarkList[HandLandmark.WRIST]
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            Log.i(
                TAG, String.format(
                    "MediaPipe Hand wrist coordinates (pixel values): x=%f, y=%f",
                    wristLandmark.x * width, wristLandmark.y * height
                )
            )
        } else {
            Log.i(
                TAG, String.format(
                    "MediaPipe Hand wrist normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    wristLandmark.x, wristLandmark.y
                )
            )
        }
        if (result.multiHandWorldLandmarks().isEmpty()) {
            return
        }
        val wristWorldLandmark =
            result.multiHandWorldLandmarks()[0].landmarkList[HandLandmark.WRIST]
        Log.i(
            TAG, String.format(
                "MediaPipe Hand wrist world coordinates (in meters with the origin at the hand's"
                        + " approximate geometric center): x=%f m, y=%f m, z=%f m",
                wristWorldLandmark.x, wristWorldLandmark.y, wristWorldLandmark.z
            )
        )
    }


    fun FadeIn(
        v: ImageView,
        begin_alpha: Int, end_alpha: Int, time: Int,
        toggleVisibility: Boolean
    ) {
        if (Integer.valueOf(Build.VERSION.SDK_INT) >= Build.VERSION_CODES.JELLY_BEAN) v.imageAlpha =
            begin_alpha else v.setAlpha(begin_alpha)
        if (toggleVisibility) {
            runOnUiThread {
                if (v.visibility === View.GONE) v.visibility = View.VISIBLE else v.visibility =
                    View.GONE
            }
        }
        val a: Animation = object : Animation() {
            override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation?
            ) {
                if (interpolatedTime == 1f) {
                    if (Integer.valueOf(Build.VERSION.SDK_INT) >= Build.VERSION_CODES.JELLY_BEAN) v.imageAlpha =
                        end_alpha else v.setAlpha(end_alpha)
                    if (toggleVisibility) {
                        if (v.visibility === View.GONE) v.visibility = View.VISIBLE else v.visibility =
                            View.GONE
                    }
                } else {
                    val new_alpha =
                        (begin_alpha + interpolatedTime * (end_alpha - begin_alpha)).toInt()
                    if (Integer.valueOf(Build.VERSION.SDK_INT) >= Build.VERSION_CODES.JELLY_BEAN) v.imageAlpha =
                        new_alpha else v.setAlpha(new_alpha)
                    v.requestLayout()
                }
            }
            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        a.duration = time.toLong()
        v.startAnimation(a)
    }

    private fun flashToggle() : Boolean{
        val toggle: ToggleButton = findViewById(R.id.btn_toggle_flash)
        var bool = false
        toggle.setOnCheckedChangeListener { _, isChecked ->
            Log.e("toggles", "to")
            bool = isChecked
        }
        return bool
        }



//    private fun setCamFocusMode() {
//        if (null == cameraInput) {
//            return
//        }
//
//        /* Set Auto focus */
//        val parameters: Camera.Parameters = cameraInput.getParameters()
//        val focusModes: List<String> = parameters.getSupportedFocusModes()
//        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
//        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO)
//        }
//        cameraInput.setParameters(parameters)
//    }

    private fun startCamera() {
        cameraInput!!.start(
            this,
            hands!!.glContext,
            CustomCameraInput.CameraFacing.BACK,
            glSurfaceView!!.width,
            glSurfaceView!!.height
        )
    }

    private fun stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput!!.setNewFrameListener(null)
            cameraInput!!.close()
        }
        if (glSurfaceView != null) {
            glSurfaceView!!.visibility = View.GONE
        }
        if (hands != null) {
            hands!!.close()
        }
    }

    companion object {
        var count = 1
        //var flash = true
    }

    private fun SaveImage(finalBitmap: Bitmap) : Uri {
        val fname = "temp_cam$count.png"
        count++
        val file = File(externalCacheDir!!.path, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.fromFile(file)
    }
}
