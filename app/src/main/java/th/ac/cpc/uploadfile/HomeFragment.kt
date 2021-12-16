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
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.*
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    var editTextOrderID: TextView? = null
    var editTextPrice: TextView? = null
    var editTextComment: TextView? = null
    var imageViewSlipSelection: ImageView? = null
    var imageViewSlip: ImageView? = null
    var buttonPayment: Button? = null
    var file: File? = null
    var imageFilePath: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        //To run network operations on a main thread or as an synchronous task.
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //check permission
        permission()

        editTextOrderID = root.findViewById(R.id.editTextOrderID)
        editTextPrice = root.findViewById(R.id.editTextPrice)
        editTextComment = root.findViewById(R.id.editTextComment)
        imageViewSlipSelection = root.findViewById(R.id.imageViewSlipSelection)
        imageViewSlip = root.findViewById(R.id.imageViewSlip)
        buttonPayment = root.findViewById(R.id.buttonPayment)

        imageViewSlipSelection?.setImageResource(R.drawable.ic_baseline_photo_camera_24)
        imageViewSlipSelection?.setOnClickListener {
            val builder1 = AlertDialog.Builder(requireActivity())
            builder1.setMessage("ท่านต้องการเลือกรูปภาพที่มีอยู่แล้ว หรือ ถ่ายภาพใหม่?")
            builder1.setNegativeButton("เลือกรูปภาพ"
            ) { dialog, id -> //dialog.cancel();
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(intent, 100)
                imageViewSlip?.visibility = View.VISIBLE
            }
            builder1.setPositiveButton("ถ่ายภาพ"
            ) { dialog, id -> //dialog.cancel();
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                var imageURI: Uri? = null
                try {
                    imageURI = FileProvider.getUriForFile(requireActivity(),
                        BuildConfig.APPLICATION_ID.toString() + ".provider",
                        createImageFile()!!)
                } catch (e: IOException) { e.printStackTrace() }

                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
                startActivityForResult(intent, 200)
                imageViewSlip?.visibility = View.VISIBLE
            }

            val alert11 = builder1.create()
            alert11.show()
        }

        buttonPayment?.setOnClickListener { payment() }

        return root
    }

    private fun permission()
    {
        //Set permission to open camera and access a directory
        if ((ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(arrayOf(
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
            .addFormDataPart("orderID", editTextOrderID?.text.toString())
            .addFormDataPart("price", editTextPrice?.text.toString())
            .addFormDataPart("comment", editTextComment?.text.toString())
                
            .addFormDataPart("file", file?.name,
                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file!!))


            .build()

        val request: Request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                try {
                    val data = JSONObject(response.body!!.string())
                    if (data.length() > 0) {
                        Toast.makeText(requireContext(), "ทำการชำระเงินสำเร็จแล้ว",
                            Toast.LENGTH_LONG).show()
                    }

                } catch (e: JSONException) { e.printStackTrace() }

            } else { response.code }

        } catch (e: IOException) { e.printStackTrace() }
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && null != intent) {
            val uri = intent.data
            file = File(getFilePath(uri))
            val bitmap: Bitmap
            try {
                bitmap = Images.Media.getBitmap(requireActivity().contentResolver, uri)
                //show image
                imageViewSlip?.setImageBitmap(bitmap)
                imageViewSlip?.setImageURI(uri)
                imageViewSlip?.visibility = View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            val imageUri = Uri.parse("file:$imageFilePath")
            file = File(imageUri.path)
            try {
                val ims: InputStream = FileInputStream(file)
                var imageBitmap = BitmapFactory.decodeStream(ims)
                imageBitmap = resizeImage(imageBitmap, 1024, 1024) //resize image
                imageBitmap = resolveRotateImage(imageBitmap, imageFilePath!!) //Resolve auto rotate image

                //show image
                imageViewSlip?.setImageBitmap(imageBitmap)
                imageViewSlip?.visibility = View.VISIBLE
                getFileName(imageUri)

            } catch (e: FileNotFoundException) {
                return
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun createImageFile(): File? {
        // Create an image file name
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "")
        val image = File.createTempFile(
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()), ".png",
            storageDir)
        imageFilePath = image.absolutePath
        return image
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getFilePath(uri: Uri?): String? {
        var path = ""
        val wholeID = DocumentsContract.getDocumentId(uri)
        // Split at colon, use second item in the arraygetDocumentId(uri)
        val id = wholeID.split(":".toRegex()).toTypedArray()[1]
        val column = arrayOf(Images.Media.DATA)
        // where id is equal to
        val sel = Images.Media._ID + "=?"
        val cursor = requireActivity().contentResolver.query(
            Images.Media.EXTERNAL_CONTENT_URI,
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
            val cursor = requireActivity().contentResolver.query(
                uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
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

    private fun resolveRotateImage(bitmap: Bitmap?, photoPath: String): Bitmap? {
        val ei = ExifInterface(photoPath)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
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

    private fun rotateImage(source: Bitmap?, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source!!, 0, 0, source.width, source.height,
            matrix, true)
    }
}