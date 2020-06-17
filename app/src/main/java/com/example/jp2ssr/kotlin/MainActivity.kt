package com.example.jp2ssr.kotlin

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.jp2ssr.R
import com.gemalto.jp2.JP2Decoder
import kotlinx.coroutines.*
import java.io.Closeable
import java.io.IOException
import java.io.InputStream


class MainActivity : AppCompatActivity(){

    private val TAG : String = "MainActivity"

    /*
     This variable represents the scope that works with the main thread.
      La variable ci-dessous represente le contexte dans lequel les coroutines qui
        interagissent avec les elements graphiques(visuels) s'executent.
     */
    private val uiScope : CoroutineScope = CoroutineScope(Dispatchers.Main)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val imgView = findViewById<ImageView>(R.id.image)
        imgView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                imgView.viewTreeObserver.removeGlobalOnLayoutListener(this )
                 val startTime = System.nanoTime().div(1_000_000)
                 uiScope.launch {
                     val processedImage = async(Dispatchers.Default) { processImage(imgView) }

                     imgView.setImageBitmap(processedImage.await())
                 }
                 val end = System.nanoTime().div(1_000_000)
                println("Execution time : " + (end - startTime) + "ms")

            }
        } )
    }

    // TODO: Optimize the coroutine implementation below.
    // TODO : Add tests for measuring coroutine performance.

    // Initial draft of coroutine implementation

    fun processImage(view: ImageView) : Bitmap? {
        val width = view.width
        val height = view.height

        Log.d(TAG, String.format("View resolution: %d x %d", width, height))
        var ret: Bitmap? = null
        var `in`: InputStream? = null
        try {
            `in` = assets.open("balloon.jp2")
            val decoder = JP2Decoder(`in`)
            val header = decoder.readHeader()
            println("Number of resolutions: " + header.numResolutions)
            println("Number of quality layers: " + header.numQualityLayers)
            var skipResolutions = 1
            var imgWidth = header.width
            var imgHeight = header.height
            Log.d(TAG, String.format("JP2 resolution: %d x %d", imgWidth, imgHeight))
            while (skipResolutions < header.numResolutions) {
                imgWidth = imgWidth shr 1
                imgHeight = imgHeight shr 1
                if (imgWidth < width || imgHeight < height) break else skipResolutions++
            }
            //we break the loop when skipResolutions goes over the correct value
            skipResolutions--
            Log.d(TAG, String.format("Skipping %d resolutions", skipResolutions))
            if (skipResolutions > 0) decoder.setSkipResolutions(skipResolutions)
            ret = decoder.decode()
            Log.d(TAG, String.format("Decoded at resolution: %d x %d", ret.width, ret.height))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            close(`in`)
        }
        return ret

    }
    
    private fun close(stream : Closeable?) {
        try {
            stream?.close()
        } catch (e: IOException){
            e.printStackTrace()
        }

    }


}
