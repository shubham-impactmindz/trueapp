package com.app.truewebapp.ui.component.main.dashboard

import DashboardBannerAdapter
import NonScrollingBannerAdapter
import NonScrollingBannerDealsAdapter
import NonScrollingBannerFruitsAdapter
import NonScrollingBannerTopSellerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.brands.WishlistBrand
import com.app.truewebapp.data.dto.dashboard_banners.ProductBanner
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.databinding.FragmentDashboardBinding
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartRepository
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartViewModel
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartViewModelFactory
import com.app.truewebapp.ui.component.main.shop.NewProductTopSellerAdapterListener
import com.app.truewebapp.ui.component.main.shop.ShopFragment
import com.app.truewebapp.ui.viewmodel.BrandsViewModel
import com.app.truewebapp.ui.viewmodel.HomeBannersViewModel
import com.app.truewebapp.ui.viewmodel.ProductBannersViewModel
import com.app.truewebapp.ui.viewmodel.WishlistViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar


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
    private lateinit var productBannersViewModel: ProductBannersViewModel
    private lateinit var homeBannersViewModel: HomeBannersViewModel
    private lateinit var brandsViewModel: BrandsViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var cartViewModel: CartViewModel

    private var originalCategoryList: List<ProductBanner> = listOf()
    private var originalTopSellerList: List<ProductBanner> = listOf()
    private var originalBrandsList: List<WishlistBrand> = listOf()
    private var token = ""
    private var variantId = ""
    private var type = ""
    private var filters = ""
    private var cdnUrl = ""
    private var lastRefreshTime: Long = 0
    private val refreshDebounceTime = 1000L // 1 second in milliseconds
    private val cartDao by lazy { CartDatabase.getInstance(requireContext()).cartDao() }


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
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This single collector is sufficient. It respects the view's lifecycle and will
        // automatically pause and resume collecting.
//        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
//            cartDao.getCartItemCount().collect { totalCount ->
//                updateCartBadge(totalCount ?: 0)
//            }
//        }

        productBannersViewModel = ViewModelProvider(this)[ProductBannersViewModel::class.java]
        homeBannersViewModel = ViewModelProvider(this)[HomeBannersViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        brandsViewModel = ViewModelProvider(this)[BrandsViewModel::class.java]
        val cartDao = CartDatabase.getInstance(requireContext()).cartDao()
        val repo = CartRepository(cartDao)
        cartViewModel = ViewModelProvider(this, CartViewModelFactory(repo)).get(CartViewModel::class.java)

        // Observe total cart count for badge
        cartViewModel.totalCount.observe(viewLifecycleOwner) { total ->
            updateCartBadge(total)
        }

        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
        observeProductsBanners()
        observeHomeBanners()
        observeWishlist()
        observeBrands()
        loadInitialData()
        setupNotifications()

        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRefreshTime > refreshDebounceTime) {
                lastRefreshTime = currentTime
                stopAutoScroll()
                loadInitialData()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//
//        nonScrollingBannerDrinksAdapter?.notifyDataSetChanged()
//        nonScrollingBannerTopSellerAdapter?.notifyDataSetChanged()
//    }


    private fun stopAutoScroll() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startAutoScroll() {
        stopAutoScroll() // Ensure any existing runnable is stopped before starting a new one.
        autoScrollRunnable?.let { handler.postDelayed(it, AUTO_SCROLL_DELAY) }
    }



    private fun initializeAutoScrollRunnable() {
        if (autoScrollRunnable == null) {
            autoScrollRunnable = object : Runnable {
                override fun run() {
                    val itemCount = bannerAdapter?.itemCount ?: 0
                    if (itemCount > 1) {
                        val nextItem = (binding.viewPager.currentItem + 1) % itemCount
                        binding.viewPager.setCurrentItem(nextItem, true)
                    }
                    // The runnable re-posts itself to create the loop.
                    handler.postDelayed(this, AUTO_SCROLL_DELAY)
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
        productBannersViewModel.productBanners(token)
        homeBannersViewModel.homeBanners(token)
        brandsViewModel.brands(token, preferences?.getString("userId", ""))
    }

    private fun observeHomeBanners() {
        homeBannersViewModel.homeSlidersResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    val banners = it.roundSliders

                    if (banners.isEmpty()) {
                        binding.shimmerLayoutRoundImages.visibility = View.GONE
                        binding.shimmerLayoutSmallBanner.visibility = View.GONE
                        binding.shimmerLayoutBigBanner.visibility = View.GONE
                        binding.shimmerLayoutDealsBanner.visibility = View.GONE
                        binding.shimmerLayoutFruitsBanner.visibility = View.GONE

                    } else {
                        binding.shimmerLayoutRoundImages.visibility = View.GONE
                        binding.rvRoundImages.visibility = View.VISIBLE
                        roundImageAdapter = RoundImageAdapter(this,banners,it.cdnURL)

                        binding.rvRoundImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        binding.rvRoundImages.adapter = roundImageAdapter

                        binding.shimmerLayoutSmallBanner.visibility = View.GONE
                        binding.viewPagerNonScrolling.visibility = View.VISIBLE
                        nonScrollingBannerAdapter = NonScrollingBannerAdapter(this,it.smallSliders, it.cdnURL)
                        binding.viewPagerNonScrolling.adapter = nonScrollingBannerAdapter

                        binding.dotsIndicator.attachTo(binding.viewPagerNonScrolling)

                        // Set up smooth scrolling without large jumps
                        binding.viewPagerNonScrolling.offscreenPageLimit = 1
                        binding.viewPagerNonScrolling.setCurrentItem(0, false)

                        binding.shimmerLayoutBigBanner.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE
                        bannerAdapter = DashboardBannerAdapter(this,it.bigSliders, it.cdnURL, )
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


                        binding.shimmerLayoutDealsBanner.visibility = View.GONE
                        binding.viewDeals.visibility = View.GONE
                        binding.recyclerViewDeals.visibility = View.VISIBLE
                        binding.tvDealsCenter.visibility = View.VISIBLE
                        binding.tvDealsCenter.text = it.dealsHeader
                        binding.recyclerViewDeals.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        // Setup ViewPager with adapter
                        nonScrollingBannerDealsAdapter = NonScrollingBannerDealsAdapter(this,it.dealsSliders, it.cdnURL)
                        binding.recyclerViewDeals.adapter = nonScrollingBannerDealsAdapter


                        binding.shimmerLayoutFruitsBanner.visibility = View.GONE
                        binding.viewFruits.visibility = View.GONE
                        binding.recyclerViewFruits.visibility = View.VISIBLE
                        binding.tvFruitsCenter.visibility = View.VISIBLE
                        binding.tvFruitsCenter.text = it.fruitHeader
                        binding.recyclerViewFruits.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        // Setup ViewPager with adapter
                        nonScrollingBannerFruitsAdapter = NonScrollingBannerFruitsAdapter(this, it.fruitSliders, it.cdnURL)
                        binding.recyclerViewFruits.adapter = nonScrollingBannerFruitsAdapter
                    }
                } else {

                }
            }
        }

        homeBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutRoundImages.visibility = View.VISIBLE
                binding.rvRoundImages.visibility = View.GONE
                binding.shimmerLayoutSmallBanner.visibility = View.VISIBLE
                binding.viewPagerNonScrolling.visibility = View.GONE
                binding.shimmerLayoutBigBanner.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
                binding.shimmerLayoutDealsBanner.visibility = View.VISIBLE
                binding.viewDeals.visibility = View.VISIBLE
                binding.recyclerViewDeals.visibility = View.GONE
                binding.tvDealsCenter.visibility = View.GONE
                binding.shimmerLayoutFruitsBanner.visibility = View.VISIBLE
                binding.viewFruits.visibility = View.VISIBLE
                binding.recyclerViewFruits.visibility = View.GONE
                binding.tvFruitsCenter.visibility = View.GONE
            }
        }

        homeBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutRoundImages.visibility = View.GONE
            binding.shimmerLayoutSmallBanner.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            binding.shimmerLayoutBigBanner.visibility = View.GONE
            binding.shimmerLayoutDealsBanner.visibility = View.GONE
            binding.shimmerLayoutFruitsBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        homeBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutRoundImages.visibility = View.GONE
            binding.shimmerLayoutSmallBanner.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            binding.shimmerLayoutBigBanner.visibility = View.GONE
            binding.shimmerLayoutDealsBanner.visibility = View.GONE
            binding.shimmerLayoutFruitsBanner.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeProductsBanners() {
        productBannersViewModel.productSlidersResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    originalCategoryList = it.newProductBanners
                    originalTopSellerList = it.topSellerBanners
                    cdnUrl = it.cdnURL

                    if (originalCategoryList.isEmpty()) {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE
                        binding.shimmerLayoutTopSeller.visibility = View.GONE
                    } else {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE
                        binding.viewNewProducts.visibility = View.GONE
                        binding.recyclerViewDrinks.visibility = View.VISIBLE
                        binding.tvNewProducts.visibility = View.VISIBLE
                        binding.tvNewProducts.text = it.newProductHeader
                        binding.shimmerLayoutTopSeller.visibility = View.GONE
                        binding.viewTopSeller.visibility = View.GONE
                        binding.recyclerViewNutrition.visibility = View.VISIBLE
                        binding.tvTopSeller.visibility = View.VISIBLE
                        binding.tvTopSeller.text = it.topSellerHeader

                        setupNewProductsAdapter()
                        setupTopSellerAdapter()

                        // UPDATE the existing adapter's data
                    }
                } else {

                }
            }
        }

        productBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutNewProducts.visibility = View.VISIBLE
                binding.viewNewProducts.visibility = View.VISIBLE
                binding.recyclerViewDrinks.visibility = View.GONE
                binding.tvNewProducts.visibility = View.GONE
                binding.shimmerLayoutTopSeller.visibility = View.VISIBLE
                binding.viewTopSeller.visibility = View.VISIBLE
                binding.recyclerViewNutrition.visibility = View.GONE
                binding.tvTopSeller.visibility = View.GONE
            }
        }

        productBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutNewProducts.visibility = View.GONE
            binding.shimmerLayoutTopSeller.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        productBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutNewProducts.visibility = View.GONE
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

    // FIX: This function is now more efficient.
    private fun updateWishlistInAdapter(variantId: String) {
        var updated = false

        if (type == "New Product") {
            originalCategoryList.forEachIndexed { prodIndex, product ->
                if (product.mvariant_id.toString() == variantId) {
                    // Toggle wishlist flag
                    product.product.user_info_wishlist = !product.product.user_info_wishlist
                    // Use the more efficient notifyItemChanged to update only the affected item.
                    nonScrollingBannerDrinksAdapter?.notifyItemChanged(prodIndex)
                    updated = true
                    return@forEachIndexed // Exit the loop once the item is found and updated
                }
            }
            // FIX: Removed the redundant and inefficient call to notifyDataSetChanged().
            // The notifyItemChanged() call above is sufficient.

        } else {
            originalTopSellerList.forEachIndexed { prodIndex, product ->
                if (product.mvariant_id.toString() == variantId) {
                    product.product.user_info_wishlist = !product.product.user_info_wishlist
                    nonScrollingBannerTopSellerAdapter?.notifyItemChanged(prodIndex)
                    updated = true
                    return@forEachIndexed // Exit the loop once the item is found and updated
                }
            }
            // FIX: Removed the redundant and inefficient call to notifyDataSetChanged().
            // The notifyItemChanged() call above is sufficient.
        }
    }

    private fun setupNewProductsAdapter() {
        val adapter = NonScrollingBannerNewProductsAdapter(
            listener = this,
            products = originalCategoryList,
            cdnURL = cdnUrl,
            context = requireContext(),
            cartViewModel = cartViewModel,
            lifecycleOwner = viewLifecycleOwner
        )
        binding.recyclerViewDrinks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewDrinks.adapter = adapter
    }

    private fun setupTopSellerAdapter() {
        val topSellerAdapter = NonScrollingBannerTopSellerAdapter(
            listener = this,
            products = originalTopSellerList,
            cdnURL = cdnUrl,
            context = requireContext(),
            cartViewModel = cartViewModel,
            lifecycleOwner = viewLifecycleOwner
        )
        binding.recyclerViewNutrition.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewNutrition.adapter = topSellerAdapter
    }

    override fun onUpdateCart(totalItems: Int, productId: Int) {
        // Live badge updated already via LiveData; optional additional UI updates done here
        updateCartBadge(totalItems)
    }

    override fun onUpdateWishlist(mvariant_id: String, type: String) {
        val userId = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
            ?.getString("userId", "") ?: ""
        variantId = mvariant_id
        wishlistViewModel.wishlist("Bearer " + userId, WishlistRequest(userId, mvariant_id))
    }

    private fun updateCartBadge(count: Int) {
        activity?.runOnUiThread {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Use a safe call `?.let` to prevent a NullPointerException if autoScrollRunnable is null.
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
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
            binding.viewOrderLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val intent = Intent(context, OrdersListActivity::class.java)
            startActivity(intent)
        }

        binding.shopLayout.setOnClickListener {
            binding.shopLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            tabSwitcher?.switchToShopTab()



            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            // 4. If found, call method
            shopFragment?.categoriesViewModel?.categories("", token, "")
        }

        binding.favLayout.setOnClickListener {
            binding.favLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
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

    override fun refreshAdapters() {
        // Only refresh visible items
        nonScrollingBannerDrinksAdapter?.notifyDataSetChanged()
        nonScrollingBannerTopSellerAdapter?.notifyDataSetChanged()
    }
}