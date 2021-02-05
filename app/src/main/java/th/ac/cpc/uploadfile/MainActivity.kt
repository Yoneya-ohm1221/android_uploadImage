package th.ac.cpc.uploadfile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.DocumentsContract.getDocumentId
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


var editTextPrice: TextView? = null
var editTextComment: TextView? = null
var imageViewSlipSelection: ImageView? = null
var imageViewSlip: ImageView? = null
var buttonPayment: Button? = null
var file: File? = null
var imageFilePath: String? = null
var orderID: String? = null


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //To run network operations on a main thread or as an synchronous task.
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //receive orderID from previous page (for fragment to fragment)
        //val bundle = this.arguments
        //orderID = bundle?.get("orderID").toString()
        orderID = "44"

        //check permission
        permission()

        editTextPrice = findViewById(R.id.editTextPrice)
        editTextComment = findViewById(R.id.editTextComment)
        imageViewSlipSelection = findViewById(R.id.imageViewSlipSelection)
        imageViewSlip = findViewById(R.id.imageViewSlip)
        buttonPayment = findViewById(R.id.buttonPayment)

        imageViewSlipSelection?.setImageResource(R.drawable.ic_baseline_photo_camera_24)
        imageViewSlipSelection?.setOnClickListener {
            val builder1 = AlertDialog.Builder(this)
            builder1.setMessage("ท่านต้องการเลือกรูปภาพที่มีอยู่แล้ว หรือ ถ่ายภาพใหม่?")
            builder1.setNegativeButton(
                    "เลือกรูปภาพ"
            ) { dialog, id -> //dialog.cancel();
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(intent, 100)
                imageViewSlip?.visibility = View.VISIBLE
            }
            builder1.setPositiveButton(
                    "ถ่ายภาพ"
            ) { dialog, id -> //dialog.cancel();
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                var imageURI: Uri? = null
                try {
                    imageURI = FileProvider.getUriForFile(this,
                            BuildConfig.APPLICATION_ID.toString() + ".provider",
                            createImageFile()!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
                startActivityForResult(intent, 200)
                //Log.d("mytest", "-2");
                imageViewSlip?.visibility = View.VISIBLE
            }

            val alert11 = builder1.create()
            alert11.show()
        }

        buttonPayment?.setOnClickListener {
            payment()
        }
    }

    private fun permission()
    {
        //Set permission to open camera and access a directory
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), 225)
        }
    }

    private fun payment()
    {
        var url: String = getString(R.string.root_url) + getString(R.string.payment_url)
        val okHttpClient = OkHttpClient()
        val formBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("orderID", orderID.toString())
                .addFormDataPart("price", editTextPrice?.text.toString())
                .addFormDataPart("comment", editTextComment?.text.toString())
                .addFormDataPart("file", file?.name,
                        RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file!!))
                .build()
        Log.d("mytest", "x1")
        Log.d("mytest", file?.name.toString())
        val request: Request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()
        try {
            Log.d("mytest", "x2")
            val response = okHttpClient.newCall(request).execute()
            Log.d("mytest", "x3")
            if (response.isSuccessful) {
                Log.d("mytest", "x4")
                try {
                    val data = JSONObject(response.body!!.string())
                    if (data.length() > 0) {
                        Toast.makeText(this, "ทำการชำระเงินสำเร็จแล้ว", Toast.LENGTH_LONG).show()
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                response.code
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && null != intent) {
            val uri = intent.data
            file = File(getFilePath(uri))
            val bitmap: Bitmap
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                //show image
                imageViewSlip?.setImageBitmap(bitmap)
                imageViewSlip?.setImageURI(uri)
                imageViewSlip?.visibility = View.VISIBLE

                //show file name
                //editImageFileName.setText(getFileName(uri));
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            val imageUri = Uri.parse("file:$imageFilePath")
            file = File(imageUri.path)
            try {
                //show image
                val ims: InputStream = FileInputStream(file)
                var imageBitmap = BitmapFactory.decodeStream(ims)
                imageBitmap = resizeImage(imageBitmap, 1024, 1024) //resize image
                imageBitmap = resolveRotateImage(imageBitmap, imageFilePath!!) //Resolve auto rotate image

                //show image
                imageViewSlip?.setImageBitmap(imageBitmap)
                imageViewSlip?.visibility = View.VISIBLE
                getFileName(imageUri)

                //show file name
                //editImageFileName.setText(getFileName(imageUri));
                //Log.d("mytest","b");
            } catch (e: FileNotFoundException) {
                return
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /*
        if (requestCode == 300 && resultCode == RESULT_OK) {
            String text = intent.getStringExtra("SCAN_RESULT");
        }
        */
    }

    //Create image file
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val storageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "")
        val image = File.createTempFile(
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()), ".png",
                storageDir)
        imageFilePath = image.absolutePath //imageFilePath = "file:" + image.getAbsolutePath();
        return image
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getFilePath(uri: Uri?): String? {
        var path = ""
        val wholeID = getDocumentId(uri)
        // Split at colon, use second item in the arraygetDocumentId(uri)
        val id = wholeID.split(":".toRegex()).toTypedArray()[1]
        val column = arrayOf(MediaStore.Images.Media.DATA)
        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null)
        var columnIndex = 0
        if (cursor != null) {
            columnIndex = cursor.getColumnIndex(column[0])
            if (cursor.moveToFirst()) {
                path = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return path
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    //Resize image
    private fun resizeImage(bm: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        val width = bm!!.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false)
        bm.recycle()
        return resizedBitmap
    }

    //Resolve auto rotate image problem
    @Throws(IOException::class)
    private fun resolveRotateImage(bitmap: Bitmap?, photoPath: String): Bitmap? {
        val ei = ExifInterface(photoPath)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED)
        var rotatedBitmap: Bitmap? = null
        rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }
        return rotatedBitmap
    }

    //Rotate image
    private fun rotateImage(source: Bitmap?, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source!!, 0, 0, source.width, source.height,
                matrix, true)
    }

    private fun adjustGravity(v: View) {
        if (v.id == com.google.android.material.R.id.smallLabel) {
            val parent = v.parent as ViewGroup
            parent.setPadding(0, 0, 0, 0)
            val params = parent.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            parent.layoutParams = params
        }
        if (v is ViewGroup) {
            val vg = v
            for (i in 0 until vg.childCount) {
                adjustGravity(vg.getChildAt(i))
            }
        }
    }

    private fun adjustWidth(nav: BottomNavigationView) {
        try {
            val menuViewField = nav.javaClass.getDeclaredField("mMenuView")
            menuViewField.isAccessible = true
            val menuView = menuViewField[nav]
            val itemWidth = menuView.javaClass.getDeclaredField("mActiveItemMaxWidth")
            itemWidth.isAccessible = true
            itemWidth.setInt(menuView, Int.MAX_VALUE)
        } catch (e: NoSuchFieldException) {
            // TODO
        } catch (e: IllegalAccessException) {
            // TODO
        }
    }
}