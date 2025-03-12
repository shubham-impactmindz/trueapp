package com.app.truewebapp.utils

import android.content.Context
import android.util.Log
import com.app.truewebapp.ui.component.main.shop.CategoryListModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.IOException

object JsonUtils {
    fun loadCategoriesFromAsset(context: Context): CategoryListModel? {
        return try {
            val jsonString = context.assets.open("categories.json").bufferedReader().use { it.readText() }

            Log.d("JsonUtils", "RAW JSON: $jsonString") // Log raw JSON before parsing

            val gson: Gson = GsonBuilder().setLenient().create()
            val categoryListModel = gson.fromJson(jsonString, CategoryListModel::class.java)

            Log.d("JsonUtils", "Parsed JSON: $categoryListModel") // Log parsed object

            if (categoryListModel.categories.isNullOrEmpty()) {
                Log.e("JsonUtils", "Category list is null or empty")
            } else {
                Log.d("JsonUtils", "Categories loaded: ${categoryListModel.categories.size}")
            }

            categoryListModel
        } catch (e: IOException) {
            Log.e("JsonUtils", "Error reading JSON file: ${e.message}")
            null
        } catch (e: JsonSyntaxException) {
            Log.e("JsonUtils", "Invalid JSON format: ${e.message}")
            null
        }
    }
}






