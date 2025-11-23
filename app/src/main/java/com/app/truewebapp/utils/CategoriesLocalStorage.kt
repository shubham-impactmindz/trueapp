package com.app.truewebapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.browse.CategoriesResponse
import com.google.gson.Gson

object CategoriesLocalStorage {
    private const val KEY_CATEGORIES_DATA = "categories_data"
    private const val KEY_CATEGORIES_TIMESTAMP = "categories_timestamp"
    
    /**
     * Save CategoriesResponse to local storage
     */
    fun saveCategories(context: Context, response: CategoriesResponse) {
        val preferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        
        try {
            val gson = Gson()
            val json = gson.toJson(response)
            editor.putString(KEY_CATEGORIES_DATA, json)
            editor.putLong(KEY_CATEGORIES_TIMESTAMP, System.currentTimeMillis())
            editor.apply()
        } catch (e: Exception) {
            android.util.Log.e("CategoriesLocalStorage", "Error saving categories", e)
        }
    }
    
    /**
     * Load CategoriesResponse from local storage
     */
    fun loadCategories(context: Context): CategoriesResponse? {
        val preferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val json = preferences.getString(KEY_CATEGORIES_DATA, null)
        
        if (json == null) {
            return null
        }
        
        return try {
            val gson = Gson()
            gson.fromJson(json, CategoriesResponse::class.java)
        } catch (e: Exception) {
            android.util.Log.e("CategoriesLocalStorage", "Error loading categories", e)
            null
        }
    }
    
    /**
     * Check if categories data exists in local storage
     */
    fun hasCategoriesData(context: Context): Boolean {
        val preferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        return preferences.contains(KEY_CATEGORIES_DATA)
    }
    
    /**
     * Clear categories data from local storage
     */
    fun clearCategories(context: Context) {
        val preferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.remove(KEY_CATEGORIES_DATA)
        editor.remove(KEY_CATEGORIES_TIMESTAMP)
        editor.apply()
    }
}


