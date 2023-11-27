package com.milkcocoa.info.milkrop.sample

import android.os.Bundle
import android.widget.Button
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.milkcocoa.info.milkrop.view.GestureImageView

/**
 * MainActivity
 * @author keita
 * @since 2023/11/23 00:06
 */

/**
 *
 */
class MainActivity: AppCompatActivity() {


    var picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
        it?.let {
            findViewById<GestureImageView>(R.id.scalable_image_view).setImageUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.pick).setOnClickListener {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
