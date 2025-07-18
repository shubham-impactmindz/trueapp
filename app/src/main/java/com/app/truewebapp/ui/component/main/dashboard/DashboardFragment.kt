package com.app.truewebapp.ui.component.main.dashboard

import DashboardBannerAdapter
import NonScrollingBannerAdapter
import NonScrollingBannerDealsAdapter
import NonScrollingBannerFruitsAdapter
import NonScrollingBannerNewProductsAdapter
import NonScrollingBannerTopSellerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.brands.WishlistBrand
import com.app.truewebapp.data.dto.dashboard_banners.NewProductBanners
import com.app.truewebapp.data.dto.dashboard_banners.TopSellerBanners
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.databinding.FragmentDashboardBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.shop.NewProductTopSellerAdapterListener
import com.app.truewebapp.ui.component.main.shop.ShopFragment
import com.app.truewebapp.ui.viewmodel.BigBannersViewModel
import com.app.truewebapp.ui.viewmodel.BrandsViewModel
import com.app.truewebapp.ui.viewmodel.DealsBannersViewModel
import com.app.truewebapp.ui.viewmodel.FruitsBannersViewModel
import com.app.truewebapp.ui.viewmodel.NewProductsBannersViewModel
import com.app.truewebapp.ui.viewmodel.RoundBannersViewModel
import com.app.truewebapp.ui.viewmodel.SmallBannersViewModel
import com.app.truewebapp.ui.viewmodel.TopSellerBannersViewModel
import com.app.truewebapp.ui.viewmodel.WishlistViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class DashboardFragment : Fragment(), BigBannerListener, SmallBannerListener, RoundBannerListener, DealsBannerListener, FruitsBannerListener, NewProductTopSellerAdapterListener {

    lateinit var binding: FragmentDashboardBinding
    private lateinit var notificationsAdapter: NotificationsAdapter
    private var tabSwitcher: TabSwitcher? = null
    private var bannerAdapter: DashboardBannerAdapter? = null
    private var roundImageAdapter: RoundImageAdapter? = null
    private var nonScrollingBannerAdapter: NonScrollingBannerAdapter? = null
    private var nonScrollingBannerDealsAdapter: NonScrollingBannerDealsAdapter? = null
    private var nonScrollingBannerFruitsAdapter: NonScrollingBannerFruitsAdapter? = null
    private var nonScrollingBannerDrinksAdapter: NonScrollingBannerNewProductsAdapter? = null
    private var nonScrollingBannerTopSellerAdapter: NonScrollingBannerTopSellerAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private val AUTO_SCROLL_DELAY: Long = 3000 // Auto scroll every 3 seconds
    private lateinit var roundBannersViewModel: RoundBannersViewModel
    private lateinit var smallBannersViewModel: SmallBannersViewModel
    private lateinit var bigBannersViewModel: BigBannersViewModel
    private lateinit var dealsBannersViewModel: DealsBannersViewModel
    private lateinit var fruitsBannersViewModel: FruitsBannersViewModel
    private lateinit var newProductsBannersViewModel: NewProductsBannersViewModel
    private lateinit var topSellerBannersViewModel: TopSellerBannersViewModel
    private lateinit var brandsViewModel: BrandsViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private var originalCategoryList: List<NewProductBanners> = listOf()
    private var originalTopSellerList: List<TopSellerBanners> = listOf()
    private var originalBrandsList: List<WishlistBrand> = listOf()
    private var token = ""
    private var variantId = ""
    private var type = ""
    private var filters = ""


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabSwitcher) {
            tabSwitcher = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        tabSwitcher = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cartDao = CartDatabase.getInstance(requireContext()).cartDao()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            // Flow collector for cart count
            cartDao.getCartItemCount().collect { totalCount ->
                val actualCount = totalCount ?: 0

//                if (actualCount == 0) {
//                    binding.tvCartBadge.visibility = View.GONE
//                } else {
//                    binding.tvCartBadge.visibility = View.VISIBLE
//                    binding.tvCartBadge.text = actualCount.toString()
//                }
            }
        }
        roundBannersViewModel = ViewModelProvider(this)[RoundBannersViewModel::class.java]
        smallBannersViewModel = ViewModelProvider(this)[SmallBannersViewModel::class.java]
        bigBannersViewModel = ViewModelProvider(this)[BigBannersViewModel::class.java]
        dealsBannersViewModel = ViewModelProvider(this)[DealsBannersViewModel::class.java]
        fruitsBannersViewModel = ViewModelProvider(this)[FruitsBannersViewModel::class.java]
        newProductsBannersViewModel = ViewModelProvider(this)[NewProductsBannersViewModel::class.java]
        topSellerBannersViewModel = ViewModelProvider(this)[TopSellerBannersViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        brandsViewModel = ViewModelProvider(this)[BrandsViewModel::class.java]
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        observeRoundBanners()
        observeSmallBanners()
        observeBigBanners()
        observeDealsBanners()
        observeFruitsBanners()
        observeNewProducts()
        observeTopSeller()
        observeWishlist()
        observeBrands()
        loadInitialData()
        setupNotifications()


        binding.swipeRefreshLayout.setOnRefreshListener {
            stopAutoScroll()
            loadInitialData()
        }
    }

    override fun onResume() {
        super.onResume()

        nonScrollingBannerDrinksAdapter?.notifyDataSetChanged()
        nonScrollingBannerTopSellerAdapter?.notifyDataSetChanged()
    }


    private fun stopAutoScroll() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startAutoScroll() {
        stopAutoScroll()
        autoScrollRunnable?.let { handler.postDelayed(it, AUTO_SCROLL_DELAY) }
    }


    private fun initializeAutoScrollRunnable() {
        if (autoScrollRunnable == null) {
            autoScrollRunnable = object : Runnable {
                override fun run() {
                    val nextItem = (binding.viewPager.currentItem + 1) % (bannerAdapter?.itemCount ?: 1)
                    binding.viewPager.setCurrentItem(nextItem, true)
                    handler.postDelayed(this, AUTO_SCROLL_DELAY) // THIS works correctly here
                }
            }
        }
    }

    private fun observeBrands() {
        brandsViewModel.brandsResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    originalBrandsList = it.wishlistbrand
                }
            }
        }

        brandsViewModel.isLoading.observe(viewLifecycleOwner) {
//            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        }

        brandsViewModel.apiError.observe(viewLifecycleOwner) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        brandsViewModel.onFailure.observe(viewLifecycleOwner) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun loadInitialData() {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        roundBannersViewModel.roundBanners(token)
        smallBannersViewModel.smallBanners(token)
        bigBannersViewModel.bigBanners(token)
        dealsBannersViewModel.dealsBanners(token)
        fruitsBannersViewModel.fruitsBanners(token)
        newProductsBannersViewModel.newProductBanners(token)
        topSellerBannersViewModel.topSellerBanners(token)
        brandsViewModel.brands(token, preferences?.getString("userId", ""))
    }

    private fun observeRoundBanners() {
        roundBannersViewModel.roundBannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    val banners = it.roundSliders

                    if (banners.isEmpty()) {
                        binding.shimmerLayoutRoundImages.visibility = View.GONE

                    } else {
                        binding.shimmerLayoutRoundImages.visibility = View.GONE
                        binding.rvRoundImages.visibility = View.VISIBLE
                        roundImageAdapter = RoundImageAdapter(this,banners,it.cdnURL)

                        binding.rvRoundImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        binding.rvRoundImages.adapter = roundImageAdapter
                    }
                } else {

                }
            }
        }

        roundBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutRoundImages.visibility = View.VISIBLE
                binding.rvRoundImages.visibility = View.GONE
            }
        }

        roundBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutRoundImages.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        roundBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutRoundImages.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeSmallBanners() {
        smallBannersViewModel.smallBannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    val banners = it.smallSliders

                    if (banners.isEmpty()) {
                        binding.shimmerLayoutSmallBanner.visibility = View.GONE

                    } else {
                        binding.shimmerLayoutSmallBanner.visibility = View.GONE
                        binding.viewPagerNonScrolling.visibility = View.VISIBLE
                        nonScrollingBannerAdapter = NonScrollingBannerAdapter(this,it.smallSliders, it.cdnURL)
                        binding.viewPagerNonScrolling.adapter = nonScrollingBannerAdapter

                        binding.dotsIndicator.attachTo(binding.viewPagerNonScrolling)

                        // Set up smooth scrolling without large jumps
                        binding.viewPagerNonScrolling.offscreenPageLimit = 1
                        binding.viewPagerNonScrolling.setCurrentItem(0, false)
                    }
                } else {

                }
            }
        }

        smallBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutSmallBanner.visibility = View.VISIBLE
                binding.viewPagerNonScrolling.visibility = View.GONE
            }
        }

        smallBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutSmallBanner.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        smallBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutSmallBanner.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeBigBanners() {
        bigBannersViewModel.bigBannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    val banners = it.bigSliders

                    if (banners.isEmpty()) {

                        binding.shimmerLayoutBigBanner.visibility = View.GONE
                    } else {
                        binding.shimmerLayoutBigBanner.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE
                        bannerAdapter = DashboardBannerAdapter(this,banners, it.cdnURL, )
                        binding.viewPager.adapter = bannerAdapter


                        // Set up smooth scrolling without large jumps
                        binding.viewPager.offscreenPageLimit = 3
                        binding.viewPager.setCurrentItem(0, false)

                        val pageTransformer = CompositePageTransformer().apply {
                            addTransformer(MarginPageTransformer(30))
                            addTransformer { page, position ->
                                val scaleFactor = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
                                page.scaleY = scaleFactor
                            }
                        }
                        binding.viewPager.setPageTransformer(pageTransformer)

                        initializeAutoScrollRunnable()
                        startAutoScroll()
                    }
                } else {

                }
            }
        }

        bigBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutBigBanner.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
            }
        }

        bigBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutBigBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        bigBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutBigBanner.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeDealsBanners() {
        dealsBannersViewModel.dealsBannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    val banners = it.dealsSliders

                    if (banners.isEmpty()) {
                        binding.shimmerLayoutDealsBanner.visibility = View.GONE

                    } else {
                        binding.shimmerLayoutDealsBanner.visibility = View.GONE
                        binding.viewDeals.visibility = View.GONE
                        binding.recyclerViewDeals.visibility = View.VISIBLE
                        binding.tvDealsCenter.visibility = View.VISIBLE
                        binding.tvDealsCenter.text = it.slider_header
                        binding.recyclerViewDeals.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        // Setup ViewPager with adapter
                        nonScrollingBannerDealsAdapter = NonScrollingBannerDealsAdapter(this,banners, it.cdnURL)
                        binding.recyclerViewDeals.adapter = nonScrollingBannerDealsAdapter
                    }
                } else {

                }
            }
        }

        dealsBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutDealsBanner.visibility = View.VISIBLE
                binding.viewDeals.visibility = View.VISIBLE
                binding.recyclerViewDeals.visibility = View.GONE
                binding.tvDealsCenter.visibility = View.GONE
            }
        }

        dealsBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutDealsBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        dealsBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutDealsBanner.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeFruitsBanners() {
        fruitsBannersViewModel.fruitsBannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    val banners = it.fruitSliders

                    if (banners.isEmpty()) {
                        binding.shimmerLayoutFruitsBanner.visibility = View.GONE

                    } else {
                        binding.shimmerLayoutFruitsBanner.visibility = View.GONE
                        binding.viewFruits.visibility = View.GONE
                        binding.recyclerViewFruits.visibility = View.VISIBLE
                        binding.tvFruitsCenter.visibility = View.VISIBLE
                        binding.tvFruitsCenter.text = it.slider_header
                        binding.recyclerViewFruits.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        // Setup ViewPager with adapter
                        nonScrollingBannerFruitsAdapter = NonScrollingBannerFruitsAdapter(this, banners, it.cdnURL)
                        binding.recyclerViewFruits.adapter = nonScrollingBannerFruitsAdapter
                    }
                } else {

                }
            }
        }

        fruitsBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutFruitsBanner.visibility = View.VISIBLE
                binding.viewFruits.visibility = View.VISIBLE
                binding.recyclerViewFruits.visibility = View.GONE
                binding.tvFruitsCenter.visibility = View.GONE
            }
        }

        fruitsBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutFruitsBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        fruitsBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutFruitsBanner.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeNewProducts() {
        newProductsBannersViewModel.newProductsBannersResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    originalCategoryList = it.newProductBanners
                    val cdnUrl = it.cdnURL

                    if (originalCategoryList.isEmpty()) {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE

                    } else {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE
                        binding.viewNewProducts.visibility = View.GONE
                        binding.recyclerViewDrinks.visibility = View.VISIBLE
                        binding.tvNewProducts.visibility = View.VISIBLE
                        binding.tvNewProducts.text = it.slider_header
                        binding.recyclerViewDrinks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        nonScrollingBannerDrinksAdapter = NonScrollingBannerNewProductsAdapter(this,
                            originalCategoryList, "", cdnUrl,requireContext())
                        binding.recyclerViewDrinks.adapter = nonScrollingBannerDrinksAdapter

                    }
                } else {

                }
            }
        }

        newProductsBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutNewProducts.visibility = View.VISIBLE
                binding.viewNewProducts.visibility = View.VISIBLE
                binding.recyclerViewDrinks.visibility = View.GONE
                binding.tvNewProducts.visibility = View.GONE
            }
        }

        newProductsBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutNewProducts.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        newProductsBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutNewProducts.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeTopSeller() {
        topSellerBannersViewModel.topSellerBannersResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    originalTopSellerList = it.topSellerBanners
                    val cdnUrl = it.cdnURL

                    if (originalTopSellerList.isEmpty()) {
                        binding.shimmerLayoutTopSeller.visibility = View.GONE

                    } else {

                        binding.shimmerLayoutTopSeller.visibility = View.GONE
                        binding.viewTopSeller.visibility = View.GONE
                        binding.recyclerViewNutrition.visibility = View.VISIBLE
                        binding.tvTopSeller.visibility = View.VISIBLE
                        binding.tvTopSeller.text = it.slider_header


                        binding.recyclerViewNutrition.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        nonScrollingBannerTopSellerAdapter = NonScrollingBannerTopSellerAdapter(this,
                            originalTopSellerList, "", cdnUrl,requireContext())
                        binding.recyclerViewNutrition.adapter = nonScrollingBannerTopSellerAdapter
                    }
                } else {

                }
            }
        }

        topSellerBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutTopSeller.visibility = View.VISIBLE
                binding.viewTopSeller.visibility = View.VISIBLE
                binding.recyclerViewNutrition.visibility = View.GONE
                binding.tvTopSeller.visibility = View.GONE
            }
        }

        topSellerBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutTopSeller.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        topSellerBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutTopSeller.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeWishlist() {
        wishlistViewModel.wishlistResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    updateWishlistInAdapter(variantId)
                } else {
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        wishlistViewModel.isLoading.observe(viewLifecycleOwner) {
//            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        }

        wishlistViewModel.apiError.observe(viewLifecycleOwner) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        wishlistViewModel.onFailure.observe(viewLifecycleOwner) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)) // customize color

        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    private fun updateWishlistInAdapter(variantId: String) {
        var updated = false

        if (type == "New Product") {
            originalCategoryList.forEachIndexed { prodIndex, product ->
                if (product.mvariant_id.toString() == variantId) {
                    // Toggle wishlist flag
                    product.product.user_info_wishlist = !product.product.user_info_wishlist
                    val productIndex = prodIndex
                    nonScrollingBannerDrinksAdapter?.notifyItemChanged(productIndex)
                    updated = true
                }
            }

            if (updated) {
                nonScrollingBannerDrinksAdapter?.notifyDataSetChanged()
            }
        }else{

            originalTopSellerList.forEachIndexed { prodIndex, product ->
                if (product.mvariant_id.toString() == variantId) {
                    // Toggle wishlist flag
                    product.product.user_info_wishlist = !product.product.user_info_wishlist
                    val productIndex = prodIndex
                    nonScrollingBannerTopSellerAdapter?.notifyItemChanged(productIndex)
                    updated = true
                }
            }

            if (updated) {
                nonScrollingBannerTopSellerAdapter?.notifyDataSetChanged()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoScrollRunnable!!)
    }

    private fun setupNotifications() {
        binding.recentNotificationsRecycler.layoutManager = LinearLayoutManager(context)
        val options = listOf(
            NotificationOption("Yov've left products in your basket"),
            NotificationOption("Meltz Dubai Chocolate - STOCK UP TODAY!"),
            NotificationOption("Yov've left products in your basket"),
            NotificationOption("Meltz Dubai Chocolate - STOCK UP TODAY!"),
            NotificationOption("Yov've left products in your basket"),
        )

        notificationsAdapter = NotificationsAdapter(options) {
            val intent = Intent(context, NotificationDetailActivity::class.java)
            startActivity(intent)
        }
        binding.recentNotificationsRecycler.adapter = notificationsAdapter


        binding.viewOrderLayout.setOnClickListener {
            val intent = Intent(context, OrdersListActivity::class.java)
            startActivity(intent)
        }

        binding.shopLayout.setOnClickListener {
            tabSwitcher?.switchToShopTab()



            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            // 4. If found, call method
            shopFragment?.categoriesViewModel?.categories("", token, "")
        }

        binding.favLayout.setOnClickListener {
            tabSwitcher?.switchToShopTab()

            Handler(Looper.getMainLooper()).postDelayed({
                // 3. Try finding ShopFragment by tag
                val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
                val selectedIds = originalBrandsList
                    .map { it.mbrand_id }
                    .joinToString(",")

                filters = selectedIds
                // 4. If found, call method
                shopFragment?.categoriesViewModel?.categories("", token, filters)

            }, 500) // Delay 300ms to ensure tab is switched and view is ready
        }
    }

    override fun onUpdateBigBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Try finding ShopFragment by tag
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment

            // 4. If found, call method
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)

        }, 1000) // Delay 300ms to ensure tab is switched and view is ready
    }

    override fun onUpdateSmallBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Try finding ShopFragment by tag
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment

            // 4. If found, call method
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)

        }, 500) // Delay 300ms to ensure tab is switched and view is ready
    }

    override fun onUpdateRoundBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Try finding ShopFragment by tag
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment

            // 4. If found, call method
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)

        }, 500) // Delay 300ms to ensure tab is switched and view is ready
    }

    override fun onUpdateDealsBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Try finding ShopFragment by tag
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment

            // 4. If found, call method
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)

        }, 500) // Delay 300ms to ensure tab is switched and view is ready
    }

    override fun onUpdateFruitsBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            // 3. Try finding ShopFragment by tag
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment

            // 4. If found, call method
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)

        }, 500) // Delay 300ms to ensure tab is switched and view is ready
    }

    override fun onUpdateWishlist(mvariant_id: String, type: String) {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "").orEmpty()
        this.variantId = mvariant_id
        this.type = type
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    override fun onUpdateCart(totalItems: Int, productId: Int) {
        val cartDao by lazy { context?.let { CartDatabase.getInstance(it).cartDao() } }
        lifecycleScope.launch {
            cartDao?.getCartItemCount()?.collect { totalCount ->
                // totalCount will be null if the cart is empty, so handle that
                val actualCount = totalCount ?: 0
            }
        }
    }
}