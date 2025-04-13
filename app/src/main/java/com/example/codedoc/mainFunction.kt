package com.example.codedoc

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.crypto.Cipher
import android.widget.LinearLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import android.util.Base64
import java.util.Base64.getEncoder
import java.security.MessageDigest
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import android.Manifest
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.security.SecureRandom


class mainFunction : AppCompatActivity() {

    private lateinit var encryptButton: Button
    private lateinit var decryptButton: Button
    private lateinit var nextStepLayout: LinearLayout
    private lateinit var uploadButton: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var keyInputSection: LinearLayout
    private lateinit var keyInput: EditText
    private lateinit var nextButtonKey: Button
    private lateinit var downloadSection: LinearLayout
    private lateinit var downloadButton: Button
    private var currentStage: Int = 1 // 1 - первый этап, 2 - второй этап, 3 - третий этап

    // Переменная для хранения URI загруженного файла
    private var fileUri: Uri? = null
    private lateinit var publicKey: PublicKey
    private lateinit var privateKey: PrivateKey
    private var isEncryptMode: Boolean = true
    val READ_REQUEST_CODE = 42
    private val REQUEST_CODE_PERMISSION = 100
    private val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 101



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_sc)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        encryptButton = findViewById(R.id.encryptButton) // зашифровка
        decryptButton = findViewById(R.id.decryptButton) // расшифровка
        nextStepLayout = findViewById(R.id.nextStepLayout)
        uploadButton = findViewById(R.id.appload) // загрузить файл
        nextButton = findViewById(R.id.next) // кнопка "далее" на 1 этапе
        keyInputSection = findViewById(R.id.keyInputSection) // для ввода ключа
        keyInput = findViewById(R.id.keyInput)
        nextButtonKey = findViewById(R.id.nextButtonKey) // кнопка "далее" на 2 этапе
        downloadSection = findViewById(R.id.downloadSection) // скачать результат
        downloadButton = findViewById(R.id.downloadButton)



        // Зашифровка
        encryptButton.setOnClickListener {
            isEncryptMode = true
            if (currentStage == 1) {
                encryptButton.setBackgroundResource(R.drawable.zashifrovat_button)
                decryptButton.setBackgroundResource(R.drawable.button_background)
                nextStepLayout.visibility = View.VISIBLE
                uploadButton.visibility = View.VISIBLE
                nextButton.visibility = View.VISIBLE
            } else {
                resetToFirstStage()
                Toast.makeText(this, "Выберите файл заново", Toast.LENGTH_SHORT).show()
            }
        }

        // Дешифровка
        decryptButton.setOnClickListener {
            isEncryptMode = false
            if (currentStage == 1) {
                decryptButton.setBackgroundResource(R.drawable.zashifrovat_button)
                encryptButton.setBackgroundResource(R.drawable.button_background)
                nextStepLayout.visibility = View.VISIBLE
                uploadButton.visibility = View.VISIBLE
                nextButton.visibility = View.VISIBLE
            } else {
                resetToFirstStage()
                Toast.makeText(this, "Выберите файл заново", Toast.LENGTH_SHORT).show()
            }
        }



        uploadButton.setOnClickListener{
            openFilePicker()
        }

        // Переход на второй этап (ключ)
        nextButton.setOnClickListener {
            if (fileUri != null) { // Здесь подправить условие
                currentStage = 2
                uploadButton.visibility = View.GONE
                nextButton.visibility = View.GONE
                keyInputSection.visibility = View.VISIBLE
                nextButtonKey.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Сначала загрузите файл", Toast.LENGTH_SHORT).show()
            }
        }

        // Ввод ключа и переход на 3 этап
        nextButtonKey.setOnClickListener {
            val charArray = keyInput.text.toString().toCharArray()
            try{
                val key = String(charArray)
                if (key.length < 8) {
                    Toast.makeText(this, "Ключ должен быть не менее 8 символов", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                currentStage = 3
                Toast.makeText(this, "Ключ принят: $key", Toast.LENGTH_SHORT).show()
                keyInputSection.visibility = View.GONE
                nextButtonKey.visibility = View.GONE
                downloadSection.visibility = View.VISIBLE

                if (isEncryptMode) {
                    encryptFile(fileUri!!, key)
                } else {
                    decryptFile(fileUri!!, key)
                }

//                Toast.makeText(this, "Введите ключ", Toast.LENGTH_SHORT).show()
            }
            finally{
                java.util.Arrays.fill(charArray, '\u0000')
            }
        }

        downloadButton.setOnClickListener {
            val fileName = if (isEncryptMode) "encrypted_file.txt" else "decrypted_file.txt"
            val file = File(filesDir, fileName)

            if (file.exists()) {
                try {
                    val content = file.readBytes()
                    saveFileUniversal(fileName, content)
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFileUniversal(fileName: String, content: ByteArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Для Android 10+ (API 29+)
                saveViaMediaStore(fileName, content)
            } else {
                // Для Android 5.x–9.x
                if (checkStoragePermission()) {
                    saveLegacy(fileName, content)
                } else {
                    requestStoragePermission()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            REQUEST_CODE_WRITE_EXTERNAL_STORAGE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this, "Нужно разрешение для сохранения файлов", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_CODE_WRITE_EXTERNAL_STORAGE
        )
    }

//    private fun saveFile(fileName: String, content: ByteArray) {
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                // Android 10+ (MediaStore)
//                saveViaMediaStore(fileName, content)
//            } else {
//                // Android 5.x–9.x (устаревший метод)
//                saveLegacy(fileName, content)
//            }
//        } catch (e: Exception) {
//            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(fileName: String, content: ByteArray) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Не удалось создать файл")

            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content)
                showFileSavedNotification(fileName, "Сохранено в Downloads/$fileName")
            }
        } catch (e: Exception) {
            throw Exception("Ошибка MediaStore: ${e.message}")
        }
    }

    private fun saveLegacy(fileName: String, content: ByteArray) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(content)
                showFileSavedNotification(fileName, file.absolutePath)
            }
        } catch (e: Exception) {
            throw Exception("Ошибка сохранения: ${e.message}")
        }
    }

    private fun showFileSavedNotification(fileName: String, filePath: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                "Файл $fileName сохранён:\n$filePath",
                Toast.LENGTH_LONG
            ).show()

            // Также можно добавить уведомление
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "downloads",
                    "Загрузки",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, "downloads")
                .setContentTitle("Файл сохранён")
                .setContentText(filePath)
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .build()

            notificationManager.notify(fileName.hashCode(), notification)
        }
    }

//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun saveFileToDownloads(fileName: String, content: ByteArray) {
//        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
//            Toast.makeText(this, "Внешнее хранилище недоступно", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val resolver = contentResolver
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
//            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
//            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//        }
//
//        try {
//            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
//                ?: throw Exception("Не удалось создать файл")
//
//            resolver.openOutputStream(uri)?.use { outputStream ->
//                outputStream.write(content)
//                Toast.makeText(this, "Файл сохранён в папку Загрузки", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

//    private fun saveFileLegacy(fileName: String, content: ByteArray) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            saveFileToDownloads(fileName, content) // Используем MediaStore для API 29+
//            return
//        }
//
//        try {
//            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//            val file = File(downloadsDir, fileName)
//            FileOutputStream(file).use { it.write(content) }
//            Toast.makeText(this, "Файл сохранён в Downloads", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun resetToFirstStage() {
        fileUri = null
        currentStage = 1

        nextStepLayout.visibility = View.GONE
        uploadButton.visibility = View.VISIBLE
        nextButton.visibility = View.VISIBLE
        keyInputSection.visibility = View.GONE
        nextButtonKey.visibility = View.GONE
        downloadSection.visibility = View.GONE

        // Очищаем поле ввода ключа
        keyInput.setText("")

        encryptButton.setBackgroundResource(R.drawable.button_background)
        decryptButton.setBackgroundResource(R.drawable.button_background)
    }

    private fun openFilePicker(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // любой тип файла (text/plain)
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK){
            data?.data?.also{ uri ->
                fileUri = uri
                nextButton.setBackgroundResource(R.drawable.zashifrovat_button)
                Toast.makeText(this, "Файл выбран: ${uri.toString()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hashKey(key: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(key.toByteArray())
    }

    private fun stringToPublicKey(base64Key: String): PublicKey? {
        return try {
            // Декодируем Base64 строку в массив байт
            val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)

            // Создаем объект X509EncodedKeySpec
            val keySpec = X509EncodedKeySpec(keyBytes)

            // Получаем KeyFactory для алгоритма RSA
            val keyFactory = KeyFactory.getInstance("RSA")

            // Генерируем PublicKey
            keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка преобразования ключа: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun saveEncryptedFile(data: ByteArray) {
        try {
            val fileName = "encrypted_file.dat"
            FileOutputStream(File(filesDir, fileName)).use {
                it.write(data)
            }
            Toast.makeText(this, "Файл зашифрован и сохранен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDecryptedFile(data: ByteArray) {
        try {
            val fileName = "decrypted_file.txt"
            FileOutputStream(File(filesDir, fileName)).use {
                it.write(data)
            }
            Toast.makeText(this, "Файл дешифрован и сохранен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encryptFile(uri: Uri, key: String) {
        val charArray = key.toCharArray()
        try {
            val hashedKey = hashKey(charArray)
            val secretKeySpec = SecretKeySpec(hashedKey, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

            // Генерируем случайный IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(iv))

            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Файл не найден")
            val fileBytes = inputStream.readBytes()

            val encryptedBytes = cipher.doFinal(fileBytes)

            // Сохраняем IV вместе с зашифрованными данными
            val result = iv + encryptedBytes
            saveEncryptedFile(result)

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка шифрования: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hashKey(key: CharArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val byteBuffer = Charset.forName("UTF-8").encode(CharBuffer.wrap(key))
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return digest.digest(bytes)
    }

    private fun decryptFile(uri: Uri, key: String) {
        try {
            val hashedKey = hashKey(key)
            val secretKeySpec = SecretKeySpec(hashedKey, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Файл не найден")
            val encryptedData = inputStream.readBytes()

            // Извлекаем IV (первые 16 байт)
            if (encryptedData.size < 16) throw Exception("Некорректные данные")
            val iv = encryptedData.copyOfRange(0, 16)
            val actualData = encryptedData.copyOfRange(16, encryptedData.size)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(actualData)

            saveDecryptedFile(decryptedBytes)
            Toast.makeText(this, "Файл успешно дешифрован", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка дешифрования: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Метод для обновления состояния загруженного файла
    fun setFileUploaded(uri: Uri) {
        this.fileUri = uri
        nextButton.setBackgroundResource(R.drawable.zashifrovat_button)
    }
}