package com.app.truewebapp.ui.component.main.shop

import BannerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.app.truewebapp.R
import com.app.truewebapp.databinding.FragmentShopBinding
import com.app.truewebapp.ui.component.main.cart.CartActivity
import com.app.truewebapp.utils.JsonUtils

class ShopFragment : Fragment(), ProductAdapterListener {

    lateinit var binding: FragmentShopBinding
    private lateinit var adapter: ShopCategoryAdapter
    private var filterOverlay: View? = null
    private var bannerAdapter: BannerAdapter? = null
    private val viewModel: ShopViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private val AUTO_SCROLL_DELAY: Long = 3000 //

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
        banners.add(R.drawable.bannersmall6)
        banners.add(R.drawable.bannersmall2)
        banners.add(R.drawable.bannersmall3)
        banners.add(R.drawable.bannersmall1)
        banners.add(R.drawable.bannersmall5)
        banners.add(R.drawable.bannersmall4)

        // Setup ViewPager with adapter
        bannerAdapter = BannerAdapter(banners)
        binding.viewPager.adapter = bannerAdapter
        binding.dotsIndicator.setViewPager2(binding.viewPager)


        // Set up smooth scrolling without large jumps
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.setCurrentItem(0, false)

        val pageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(30)) // Adds margin between items
            addTransformer { page, position ->
                val scaleFactor = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
                page.scaleY = scaleFactor
            }
        }
        binding.viewPager.setPageTransformer(pageTransformer)

        // Optimized Auto-Scroll Logic
        autoScrollRunnable = object : Runnable {
            override fun run() {
                val nextItem = (binding.viewPager.currentItem + 1) % banners.size
                binding.viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, AUTO_SCROLL_DELAY)
            }
        }
        handler.postDelayed(autoScrollRunnable!!, AUTO_SCROLL_DELAY)

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
            Log.d(
                "setupShopUI",
                "Categories Loaded: ${categoryList.categories.size}"
            ) // 🔴 Log number of categories
            val images = listOf(
                R.drawable.category1, R.drawable.category2,
                R.drawable.category3, R.drawable.category4,
                R.drawable.category5, R.drawable.category6,

                )
            adapter = ShopCategoryAdapter(this, categoryList, images)
            binding.shopCategoryRecycler.adapter = adapter
        } else {
            Log.e("setupShopUI", "Category list is null or empty")
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
