package com.example.codedoc

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
            val key = keyInput.text.toString()
            if (key.isNotEmpty()) {
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
            } else {
                Toast.makeText(this, "Введите ключ", Toast.LENGTH_SHORT).show()
            }
        }



        downloadButton.setOnClickListener {
            // Определяем имя файла в зависимости от режима (шифрование или дешифрование)
            val fileName = if (isEncryptMode) "encrypted_file.txt" else "decrypted_file.txt"

            // Получаем содержимое файла из внутреннего хранилища
            val file = File(filesDir, fileName)
            if (file.exists()) {
                try {
                    val content = file.readBytes() // Читаем содержимое файла
                    saveFileToDownloads(fileName, content)
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFileToDownloads(fileName: String, content: ByteArray) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content)
                }
                Toast.makeText(this, "Файл сохранен в папку Загрузки", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка сохранения файла: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
        }
    }



//    private fun saveFileToPublicStorage(fileName: String, content: ByteArray) {
//
//        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val file = File(downloadsDir, fileName)
//        try {
//            FileOutputStream(file).use { outputStream ->
//                outputStream.write(content)
//            }
//            Toast.makeText(this, "Файл сохранен в папку Загрузки", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            Toast.makeText(this, "Ошибка сохранения файла: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

//    private fun checkAndRequestPermissions() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
//        } else {
//            // Разрешение уже предоставлено, можно сохранять файл
//            saveFileToPublicStorage(fileName, content)
//        }
//    }

//    private fun saveFileToDevice() {
//        val fileName = if (isEncryptMode) "encrypted_file.txt" else "decrypted_file.txt"
//        val file = File(filesDir, fileName)
//        val intent = Intent(Intent.ACTION_SEND).apply {
//            type = "text/plain"
//            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
//        }
//        startActivity(Intent.createChooser(intent, "Сохранить файл"))
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

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Разрешение предоставлено, можно сохранять файл
//                saveFileToPublicStorage(fileName, content)
//            } else {
//                Toast.makeText(this, "Разрешение на запись в хранилище не предоставлено", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }



//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSION) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Разрешение предоставлено
//            } else {
//                Toast.makeText(this, "Разрешение на чтение файлов не предоставлено", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }


//    private fun generateKeyPair() {
//        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
//        keyPairGenerator.initialize(2048)
//        val keyPair = keyPairGenerator.generateKeyPair()
//        publicKey = keyPair.public
//        privateKey = keyPair.private
//    }

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

//    private fun stringToBase64(input: String): String {
//        return Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)
//    }

//    private fun publicKeyToString(publicKey: PublicKey): String {
//        val keyBytes = publicKey.encoded
//        return getEncoder().encodeToString(keyBytes)
//    }

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

    private fun saveEncryptedFile(encryptedBytes: ByteArray) {
        val fileName = "encrypted_file.txt"
        val outputStream: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
        outputStream.write(Base64.encode(encryptedBytes, Base64.NO_WRAP))
        outputStream.close()
        Toast.makeText(this, "Файл зашифрован и сохранен", Toast.LENGTH_SHORT).show()
    }

    private fun saveDecryptedFile(decryptedBytes: ByteArray) {
        val fileName = "decrypted_file.txt"
        val outputStream: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
        outputStream.write(decryptedBytes)
        outputStream.close()
        Toast.makeText(this, "Файл дешифрован и сохранен", Toast.LENGTH_SHORT).show()
    }

    private fun encryptFile(uri: Uri, key: String) {
        if (uri == null) {
            Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val hashedKey = hashKey(key)
            //val base64Key = Base64.encodeToString(hashedKey, Base64.NO_WRAP)
            // Преобразуем введенный ключ в PublicKey
            //val publicKey = stringToPublicKey(base64Key)
            val secretKeySpec = SecretKeySpec(hashedKey, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            if (secretKeySpec == null) {
                Toast.makeText(this, "Неверный формат ключа", Toast.LENGTH_SHORT).show()
                return
            }

            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            reader.useLines { lines ->
                lines.forEach { line -> stringBuilder.append(line).append("\n") }
            }
            val text = stringBuilder.toString()

            val encryptedBytes = cipher.doFinal(text.toByteArray())

            saveEncryptedFile(encryptedBytes)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка шифрования: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun decryptFile(uri: Uri, key: String) {
        try {
            // Хэшируем ключ
            val hashedKey = hashKey(key)

            // Создаем AES-ключ из хэшированного ключа
            val secretKeySpec = SecretKeySpec(hashedKey, "AES")

            // Расшифровываем данные с помощью AES
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)

            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            reader.useLines { lines ->
                lines.forEach { line -> stringBuilder.append(line).append("\n") }
            }
            val encryptedText = stringBuilder.toString()

            val decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP))

            // Сохраняем расшифрованный файл
            saveDecryptedFile(decryptedBytes)
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