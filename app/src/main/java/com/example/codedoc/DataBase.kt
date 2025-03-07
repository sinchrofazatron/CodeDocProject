package com.example.codedoc

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Класс БД
// В (конструктор) передаем context, factory
// ? - для корректроной рбработки null - значений
// Наследуем от класса SQLiteOpenHelper, в (конструктор)
// которого передаем context, "имя БД", factory, версия БД

class DataBase(val context: Context, val factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, "app", factory, 1) {

    override fun onCreate(db: SQLiteDatabase?) {

        // query - SQL - команда
        // !! - для корректной обработки null
        // id регулируется сам по себе

        val query = "CREATE TABLE users (id INT PRIMARY KEY, login TEXT, password TEXT, email TEXT)"
        db!!.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        // очищаем бд и заново пересоздаем

        db!!.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    // Регистрация пользователя
    // В values будут подставлены определенные логин, пароль и почта

    fun addUser(user: User){
        val values = ContentValues()
        values.put("login", user.login)
        values.put("password", user.password)
        values.put("email", user.email)

        // Обращаемся к ткущей базе данных для записи
        val db = this.writableDatabase
        db.insert("users", null, values)
        db.close()
    }

    // Передаем логин и пароль в функцию
    fun getUser(login: String, password: String): Boolean{
        val db = this.readableDatabase

        // Ищем запись в таблице users у которой логин и пароль совпадают с переданными данными
        val result = db.rawQuery("SELECT * FROM users WHERE login = '$login' AND password = '$password'", null)

        // Запись существует - возвращает true, нет - false
        return result.moveToFirst()
    }


}