package com.app.truewebapp.ui.component.main.shop

import NonScrollingBannerAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.truewebapp.R
import com.app.truewebapp.databinding.FragmentShopBinding
import com.app.truewebapp.ui.component.main.cart.CartActivity
import com.app.truewebapp.utils.JsonUtils

class ShopFragment : Fragment(), ProductAdapterListener {

    lateinit var binding: FragmentShopBinding
    private lateinit var adapter: ShopCategoryAdapter
    private var filterOverlay: View? = null
    private var nonScrollingBannerAdapter: NonScrollingBannerAdapter? = null
    private val viewModel: ShopViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupShopUI()
        observeCartUpdates()
        setUpNonScrollingBannerAdapter()
        binding.filterButton.setOnClickListener {
            showFilterOverlay()
        }
        binding.cart.setOnClickListener {
            val intent = Intent(context, CartActivity::class.java)
            startActivity(intent)
        }
        binding.cancelLayout.setOnClickListener {
            showFilterOverlay()
        }
        binding.applyLayout.setOnClickListener {
            showFilterOverlay()
        }

        // Setup Brands RecyclerView
        val brandsRecyclerView = binding.brandsRecyclerView
        val brandsAdapter = BrandsAdapter()
        brandsRecyclerView.layoutManager = GridLayoutManager(context, 3) // 3 columns
        brandsRecyclerView.adapter = brandsAdapter
    }


    private fun setUpNonScrollingBannerAdapter() {

        // List of banner images
        val banners: MutableList<Int> = ArrayList()
        banners.add(R.drawable.sale)
        banners.add(R.drawable.sale2)
        banners.add(R.drawable.sale3)

        // Setup ViewPager with adapter

        // Setup ViewPager with adapter
        nonScrollingBannerAdapter = NonScrollingBannerAdapter(banners)
        binding.viewPagerNonScrolling.adapter = nonScrollingBannerAdapter


        // Set up smooth scrolling without large jumps
        binding.viewPagerNonScrolling.offscreenPageLimit = 1
        binding.viewPagerNonScrolling.setCurrentItem(0, false)

    }

    private fun showFilterOverlay() {
        if (binding.filterLayout.visibility == View.VISIBLE) {
            binding.filterLayout.visibility = View.GONE
            binding.shopCategoryRecycler.visibility = View.VISIBLE
        } else {
            binding.filterLayout.visibility = View.VISIBLE
            binding.shopCategoryRecycler.visibility = View.GONE
        }
//        if (filterOverlay == null) {
//            filterOverlay = LayoutInflater.from(context).inflate(R.layout.filter_overlay, binding.root, false)
//            binding.root.addView(filterOverlay)
//
//            // Setup Brands RecyclerView
//            val brandsRecyclerView = filterOverlay?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.brandsRecyclerView)
//            val brands = createBrandsList() // Replace with your data
//            val brandsAdapter = BrandsAdapter(brands)
//            brandsRecyclerView?.layoutManager = GridLayoutManager(context, 3) // 3 columns
//            brandsRecyclerView?.adapter = brandsAdapter
//
//            // Handle overlay click to close
//            filterOverlay?.setOnClickListener {
//                hideFilterOverlay()
//            }
//        }
//        filterOverlay?.visibility = View.VISIBLE
    }

    private fun hideFilterOverlay() {
        filterOverlay?.visibility = View.GONE
    }

    private fun setupShopUI() {
        binding.shopCategoryRecycler.layoutManager = LinearLayoutManager(context)

        val categoryList = JsonUtils.loadCategoriesFromAsset(requireContext())

        if (categoryList != null && !categoryList.categories.isNullOrEmpty()) {
            Log.d("setupShopUI", "Categories Loaded: ${categoryList.categories.size}") // 🔴 Log number of categories

            adapter = ShopCategoryAdapter(this, categoryList)
            binding.shopCategoryRecycler.adapter = adapter
        } else {
            Log.e("setupShopUI", "com.app.truewebapp.ui.component.main.shop.Category list is null or empty")
        }
    }




    private fun observeCartUpdates() {
        viewModel.cartLiveData.observe(viewLifecycleOwner) { cartData ->
            if (cartData.totalItems > 0) { // Only show bottom sheet if cart has items
                binding.linearCartDetail.visibility = View.GONE
            }
        }
    }

    override fun onAddToCartClicked(totalItems: Int, productName: String) {
        viewModel.updateCart(totalItems, productName)
    }
}
