package com.example.nts_pim.data.repository.providers

import android.content.Context
import com.google.gson.GsonBuilder

 class ModelPreferences(context: Context) {
     private val preferences = context.getSharedPreferences("MODEL_PREFERENCES", Context.MODE_PRIVATE)
     private val editor = preferences.edit()
     private val gson = GsonBuilder().create()

     /**
      * Saves object into the Preferences.
      * Only the fields are stored. Methods, Inner classes, Nested classes and inner interfaces are not stored.
      **/
     fun <T> putObject(key: String, y: T) {
         //Convert object to JSON String.
         val inString = gson.toJson(y)
         //Save that String in SharedPreferences
         editor.putString(key, inString).commit()
     }

     /**
      * Saves collection of objects into the Preferences.
      * Only the fields are stored. Methods, Inner classes, Nested classes and inner interfaces are not stored.
      **/

     fun <T> getObject(key: String, c: Class<T>): T? {
         //We read JSON String which was saved.
         val value = preferences.getString(key, null)
         if (value != null) {
//            //JSON String was found which means object can be read.
//            //We convert this JSON String to model object. Parameter "c" (of
//            type Class<T>" is used to cast.
             return gson.fromJson(value, c)
         }
         println("no object was saved/created")
         return null
     }
 }