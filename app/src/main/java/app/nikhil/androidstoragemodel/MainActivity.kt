package app.nikhil.androidstoragemodel

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.nikhil.androidstoragemodel.databinding.ActivityMainBinding
import java.io.File
import java.lang.StringBuilder
import kotlin.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val MIME_TYPE_IMAGE = "image/*"
        private const val REQUEST_CODE_IMAGE_PICKER = 1001
        private const val REQUEST_CODE_DOC_PICKER = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.requestReadPermission.setOnClickListener { requestReadStoragePermission() }
        binding.requestWritePermission.setOnClickListener { requestWriteStoragePermission() }

        binding.testReadInternalAppSpecific.setOnClickListener { tryReadInternalAppSpecificFiles() }
        binding.testReadInternalAppSpecificCache.setOnClickListener { tryReadInternalAppSpecificCache() }
        binding.testReadInternalFolder.setOnClickListener { tryReadInternalRootFolder() }
        binding.testWriteInternalFolder.setOnClickListener { tryWriteInternalRootFolder() }
        binding.testWriteInternalAppFolder.setOnClickListener { tryWriteInternalAppSpecificFolder() }
        binding.printEnvironmentDetails.setOnClickListener { printEnvironmentStorageDetails() }

        binding.testReadExternalFolder.setOnClickListener { tryReadExternalFolder() }
        binding.testWriteExternalFolder.setOnClickListener { tryWriteExternalFolderUsingFileAPIs() }
        binding.testReadExternalAppSpecificFolder.setOnClickListener { tryReadExternalAppSpecificFolder() }
        binding.testWriteExternalAppSpecificFolder.setOnClickListener { tryWriteExternalAppSpecificFolder() }
        binding.testWriteExternalFileLocal.setOnClickListener { tryWriteExternalFileUsingMediaStoreAPIs() }
        binding.testWriteImageFolderFileApi.setOnClickListener { testWriteImageIntoExternalPicturesUsingFileAPIs() }

        binding.pickImage.setOnClickListener { pickImage() }
        binding.pickDocument.setOnClickListener { pickDocument() }
        binding.queryImages.setOnClickListener { queryImages() }

        refreshPermissionStatus()
    }

    private fun queryImages() {
        trySafe {
            showMessage("Querying!")
            val imagesCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor = contentResolver.query(
                imagesCollection,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                null,
                null,
                null
            )
            var totalItems = 0
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    totalItems += 1
                }
                showMessage("Found $totalItems images.")
            } else {
                showMessage("Unable to query")
            }
        }
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = MIME_TYPE_IMAGE
        }
        startActivityForResult(intent, REQUEST_CODE_DOC_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DOC_PICKER) {
            showMessage("Picked ${data?.data?.toString()}")
        } else if (requestCode == REQUEST_CODE_IMAGE_PICKER) {
            showMessage("Picked ${data?.data?.toString()}")
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }

    private fun refreshPermissionStatus() {
        val readPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writePermissionGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        binding.readPermissionStatus.setTextColor(getColorForPermission(readPermissionGranted))
        binding.writePermissionStatus.setTextColor(getColorForPermission(writePermissionGranted))
    }

    private fun getColorForPermission(granted: Boolean): Int {
        val colorRes = when {
            granted -> android.R.color.holo_green_dark
            else -> android.R.color.holo_red_dark
        }
        return ContextCompat.getColor(this, colorRes)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage("Read Permission Granted!")
            } else {
                showMessage("Read Permission Denied!")
            }
        } else if (requestCode == 1002) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage("Write Permission Granted!")
            } else {
                showMessage("Write Permission Denied!")
            }
        }
        refreshPermissionStatus()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            1001
        )
    }

    private fun requestWriteStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1002
        )
    }

    private fun printEnvironmentStorageDetails() {
        trySafe {
            val dataDirectory = Environment.getDataDirectory()
            val downloadCacheDirectory = Environment.getDownloadCacheDirectory()
            val rootDirectory = Environment.getRootDirectory()
            val stringBuilder = StringBuilder()
                .appendLine("Data directory ${dataDirectory.absolutePath}")
                .appendLine("Download directory ${downloadCacheDirectory.absolutePath}")
                .appendLine("Root directory ${rootDirectory.absolutePath}")
            showMessage(stringBuilder.toString())
        }
    }

    private fun tryWriteInternalAppSpecificFolder() {
        trySafe {
            val file = File(filesDir, "My Folder")
            if (!file.exists()) {
                val created = file.mkdir()
                showMessage("Created ${file.absolutePath} = $created")
            } else {
                showMessage("Folder already exists.")
            }
        }
    }

    private fun tryReadInternalRootFolder() {
        trySafe {
            val file = File(filesDir?.parentFile?.parentFile, "DCIM")
            showMessage("Able to access ${file.absolutePath} = ${file.canRead()}")
        }
    }

    private fun tryWriteInternalRootFolder() {
        trySafe {
            val file = File(filesDir.parentFile?.parentFile, "AndroidStorageModel")
            if (!file.exists()) {
                val success = file.mkdirs()
                showMessage("Able to create ${file.absolutePath} = $success")
            } else {
                showMessage("Already present ${file.absolutePath}")
            }
        }
    }

    private fun tryReadInternalAppSpecificCache() {
        trySafe {
            val file = cacheDir
            showMessage("Able to access ${file.absolutePath} = ${file.canRead()}")
        }
    }

    private fun tryReadInternalAppSpecificFiles() {
        trySafe {
            val file = filesDir
            showMessage("Able to access ${file.absolutePath} = ${file.canRead()}")
        }
    }

    private fun tryReadExternalAppSpecificFolder() {
        trySafe {
            val file = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            showMessage("Able to access ${file?.absolutePath} = ${file?.canRead()}")
        }
    }

    private fun tryWriteExternalAppSpecificFolder() {
        trySafe {
            val file =
                File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "My External Folder")
            if (!file.exists()) {
                val created = file.mkdir()
                showMessage("Created ${file.absolutePath} = $created")
            } else {
                showMessage("Already present ${file.absolutePath}")
            }
        }
    }

    private fun tryWriteExternalFolderUsingFileAPIs() {
        trySafe {
            val parentFolder = File(
                externalCacheDir?.parentFile?.parentFile?.parentFile?.parentFile,
                "My App Folder"
            )
            if (!parentFolder.exists()) {
                val created = parentFolder.mkdir()
                showMessage("Created ${parentFolder.absolutePath} = $created")
                if (created) {
                    internalCreateImageUsingFileAPIs(parentFolder)
                }
            } else {
                showMessage("Already present ${parentFolder.absolutePath}")
                internalCreateImageUsingFileAPIs(parentFolder)
            }
        }
    }

    private fun testWriteImageIntoExternalPicturesUsingFileAPIs() {
        trySafe {
            val parentFolder =
                File(
                    externalCacheDir?.parentFile?.parentFile?.parentFile?.parentFile,
                    "Pictures/AndroidStorageModel"
                )
            if (parentFolder.exists()) {
                internalCreateImageUsingFileAPIs(parentFolder)
            } else {
                val created = parentFolder.mkdir()
                if (created) {
                    internalCreateImageUsingFileAPIs(parentFolder)
                } else {
                    showMessage("Unable to create ${parentFolder.absolutePath}")
                }
            }
        }
    }

    private fun tryWriteExternalFileUsingMediaStoreAPIs() {
        trySafe {
            // Use MediaStore for Android 11 and above.
            val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.ImageColumns.DISPLAY_NAME, "StorageModelTest.jpeg")
            }
            val finalUri = contentResolver.insert(collectionUri, contentValues)
            val asset = assets.open("image.jpeg")
            val byteArray = asset.readBytes()
            contentResolver.openOutputStream(finalUri!!)?.write(byteArray)
            showMessage("Created $finalUri")
        }
    }

    private fun internalCreateImageUsingFileAPIs(parentFolder: File) {
        val file = File(parentFolder, "StorageModelTest.jpeg")
        val imageCreated = file.createNewFile()
        val asset = assets.open("image.jpeg")
        val byteArray = asset.readBytes()
        file.writeBytes(byteArray)
        if (imageCreated) {
            showMessage("Created ${file.absolutePath}")
        } else {
            showMessage("Unable to create ${file.absolutePath}")
        }
    }

    private fun tryDeleteExternalFolder() {
        trySafe {
            val file = File(
                externalCacheDir?.parentFile?.parentFile?.parentFile?.parentFile,
                "My App Folder"
            )
            if (!file.exists()) {
                showMessage("Folder ${file.absolutePath} doesn't exist")
            } else {
                val deleted = file.delete()
                showMessage("Deleted ${file.absolutePath} = $deleted")
            }
        }
    }

    private fun tryReadExternalFolder() {
        trySafe {
            val file =
                File(externalCacheDir?.parentFile?.parentFile?.parentFile?.parentFile, "DCIM")
            if (file.exists()) {
                showMessage("Able to access ${file.absolutePath} = ${file.canRead()}")
            } else {
                showMessage("${file.absolutePath} doesn't exist")
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private inline fun trySafe(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            showMessage("Problem: ${e.message}")
        }
    }
}