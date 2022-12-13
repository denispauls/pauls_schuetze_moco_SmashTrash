package com.example.smashtrash

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_hinz_trash.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

@Suppress("DEPRECATION")
class HinzTrashActivity : AppCompatActivity(), View.OnClickListener {
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hinz_trash)

        setSupportActionBar(toolbar_add_trash)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_trash.setNavigationOnClickListener {
            onBackPressed()
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            updateDateinView()
        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)

    }
    private fun updateDateinView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    private fun choosePhotoFromGallery(){
       Dexter.withContext(this)
           .withPermissions(
               android.Manifest.permission.READ_EXTERNAL_STORAGE,
               android.Manifest.permission.WRITE_EXTERNAL_STORAGE
           )
           .withListener(object : MultiplePermissionsListener {
               override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                   if(p0!!.areAllPermissionsGranted()){
                       val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                       startActivityForResult(galleryIntent, GALLERY)
                   }
               }

               override fun onPermissionRationaleShouldBeShown(
                   p0: MutableList<PermissionRequest>?,
                   p1: PermissionToken?
               ) {
                   showRationalDialogForPermissions()
               }
           }).onSameThread()
           .check()
    }

    private fun takePhotoFromCamera(){
        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if(p0!!.areAllPermissionsGranted()){
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(cameraIntent, CAMERA)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("Keine Berechtigungen. In Appeinstellungen ändern")
            .setPositiveButton("Zu den Einstellungen") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Abbrechen"){dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
        val wrapper = ContextWrapper(applicationContext)

        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try{
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()

            stream.close()
        } catch(e: IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    override fun onClick(v: View?) {
       when(v!!.id){
           R.id.et_date -> {
               DatePickerDialog(this@HinzTrashActivity, dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
           }
           R.id.tv_add_image -> {
               val pictureDialog = AlertDialog.Builder(this)
               pictureDialog.setTitle("Aktion auswählen")
               val pictureDialogItems = arrayOf("Foto aus der Gallerie", "Foto aufnehmen")
               pictureDialog.setItems(pictureDialogItems){ dialog, which ->
                   when(which) {
                       0 -> choosePhotoFromGallery()
                       1 -> takePhotoFromCamera()
                   }

               }
               pictureDialog.show()

           }
       }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                if(data != null){
                    val contentURI = data.data
                    try{
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                        val saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved Image : ", "Path: $saveImageToInternalStorage")

                        iv_place_image!!.setImageBitmap(selectedImageBitmap)
                    } catch(e : IOException){
                        e.printStackTrace()
                        Toast.makeText(this@HinzTrashActivity, "Fehlgeschlagen", Toast.LENGTH_LONG).show()
                    }
                }
            } else if(requestCode == CAMERA){
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                val saveImageToInternalStore = saveImageToInternalStorage(thumbnail)
                Log.e("Saved Image : ", "Path: $saveImageToInternalStore")
                iv_place_image!!.setImageBitmap(thumbnail)
            }
        } else if (resultCode == Activity.RESULT_CANCELED){
            Log.e("Canceled","Canceled")
        }
    }

    companion object {
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "TrashImages"
    }
}