package com.app.truewebapp.ui.component.main.cart.cartdatabase

import android.content.Context
import com.google.gson.Gson

object CartStorage {
    private const val PREF_NAME = "local_cart_pref"
    private const val CART_KEY = "cart_data"

    fun saveCart(context: Context, cartJson: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(CART_KEY, cartJson).apply()
    }

    fun getSavedCart(context: Context): MutableMap<Int, Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cartJson = prefs.getString(CART_KEY, null)
        val result = mutableMapOf<Int, Int>()

        if (!cartJson.isNullOrEmpty()) {
            try {
                val cartMap = Gson().fromJson(cartJson, Map::class.java)
                val cartItems = cartMap["cart"] as? List<*>
                cartItems?.forEach { item ->
                    if (item is Map<*, *>) {
                        val id = (item["mvariant_id"] as? Double)?.toInt()
                        val qty = (item["quantity"] as? Double)?.toInt()
                        if (id != null && qty != null) result[id] = qty
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return result
    }

    fun clearCart(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(CART_KEY).apply()
    }
}
