package com.example.palmcollector

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PalmPreview : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_preview)
        assert(
            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val kiki = intent.getStringExtra("preview_bitmap")
        var url = Uri.parse(kiki)
        var myImage = findViewById<ImageView>(R.id.dialog_imageview)
        val theUrl = Uri.fromFile(File(url.encodedPath));
        val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), theUrl)
        myImage.setImageBitmap(bitmap)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    }


