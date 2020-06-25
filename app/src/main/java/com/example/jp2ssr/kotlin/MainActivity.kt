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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity(){

    private val TAG : String = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val imgView = findViewById<ImageView>(R.id.image)
        imgView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                imgView.viewTreeObserver.removeGlobalOnLayoutListener(this )

                val time = measureTimeMillis {
                    DecodeJp2AsyncTask(imgView).execute()
                }
                println("Execution time : " + time + "ms")

            }
        } )
    }

    // TODO: Replace AsyncTask below with an efficient coroutine-based implementation of asynchronous work.

    // UPDATE : For coroutine implementation, check out the coroutine-optimized branch.



     @SuppressLint("StaticFieldLeak")
     inner class DecodeJp2AsyncTask(private val view: ImageView) : AsyncTask<Void?, Void?, Bitmap?>() {
        private val width: Int
        private val height: Int

         init {
             width = view.width
             height = view.height
         }

         override fun doInBackground(vararg voids: Void? ): Bitmap? {
            Log.d(TAG, String.format("View resolution: %d x %d", width, height))
            var ret: Bitmap? = null
            var `in`: InputStream? = null
            try {
                `in` = assets.open("balloon.jp2")
                val decoder = JP2Decoder(`in`)
                val header = decoder.readHeader()
                println("Number of resolutions: " + header.numResolutions)
                println("Number of quality layers: " + header.numQualityLayers)
                var skipResolutions = 2
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

        override fun onPostExecute(bitmap: Bitmap?) {
            if (bitmap != null) {
                view.setImageBitmap(bitmap)
            }
        }


    }
    
    private fun close(stream : Closeable?) {
        try {
            stream?.close()
        } catch (e: IOException){
            e.printStackTrace()
        }

    }


}
