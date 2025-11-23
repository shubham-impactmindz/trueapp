package com.app.truewebapp.ui.component.main.dashboard

// Importing custom adapters for banners and product sections

// Android core and UI components

// AppCompat for compatibility support

// Project-specific resources and constants
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
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DashboardFragment
 *
 * This fragment represents the home dashboard of the app.
 * Responsibilities:
 * - Display promotional banners (big, small, round, deals, fruits, etc.)
 * - Manage product sections (new products, top sellers, brands)
 * - Handle swipe-to-refresh and auto-scrolling banners
 * - Observe LiveData from multiple ViewModels (MVVM pattern)
 */
class DashboardFragment : Fragment(),
    BigBannerListener,
    SmallBannerListener,
    RoundBannerListener,
    DealsBannerListener,
    FruitsBannerListener,
    NewProductTopSellerAdapterListener {

    // View binding for accessing layout views
    lateinit var binding: FragmentDashboardBinding

    // Notifications adapter for handling in-app notifications
    private lateinit var notificationsAdapter: NotificationsAdapter

    // Reference to TabSwitcher interface implemented by parent activity
    private var tabSwitcher: TabSwitcher? = null

    // Adapters for different banner and product sections
    private var bannerAdapter: DashboardBannerAdapter? = null
    private var roundImageAdapter: RoundImageAdapter? = null
    private var nonScrollingBannerAdapter: NonScrollingBannerAdapter? = null
    private var nonScrollingBannerDealsAdapter: NonScrollingBannerDealsAdapter? = null
    private var nonScrollingBannerFruitsAdapter: NonScrollingBannerFruitsAdapter? = null
    private var nonScrollingBannerDrinksAdapter: NonScrollingBannerNewProductsAdapter? = null
    private var nonScrollingBannerTopSellerAdapter: NonScrollingBannerTopSellerAdapter? = null

    // Handler for scheduling auto-scrolling of banners
    private val handler = Handler(Looper.getMainLooper())

    // Runnable reference for auto-scroll task
    private var autoScrollRunnable: Runnable? = null

    // Delay for auto-scroll (3 seconds)
    private val AUTO_SCROLL_DELAY: Long = 2000
    
    // Store original banner count for infinite scroll calculation
    private var originalBannerCount: Int = 0

    // ViewModels for handling business logic and API calls
    private lateinit var productBannersViewModel: ProductBannersViewModel
    private lateinit var homeBannersViewModel: HomeBannersViewModel
    private lateinit var brandsViewModel: BrandsViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var cartViewModel: CartViewModel

    // Lists to hold original API data
    private var originalCategoryList: List<ProductBanner> = listOf()
    private var originalTopSellerList: List<ProductBanner> = listOf()
    private var originalBrandsList: List<WishlistBrand> = listOf()

    // Tokens, identifiers, and filtering parameters
    private var token = ""
    private var variantId = ""
    private var type = ""
    private var filters = ""
    private var cdnUrl = ""

    // Variables for refresh debounce (avoid multiple quick refreshes)
    private var lastRefreshTime: Long = 0
    private val refreshDebounceTime = 1000L // 1 second

    // Lazy initialization of Cart DAO
    private val cartDao by lazy { CartDatabase.getInstance(requireContext()).cartDao() }
    
    // Track shown deals to prevent duplicate popups
    private val shownDeals = mutableMapOf<Int, Int>() // variantId to quantity when deal was shown

    /**
     * Called when fragment is attached to its parent context.
     * Used to check if parent implements TabSwitcher interface.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabSwitcher) {
            tabSwitcher = context
        }
    }

    /**
     * Called when fragment is detached from its parent context.
     * Clears tabSwitcher reference to avoid memory leaks.
     */
    override fun onDetach() {
        super.onDetach()
        tabSwitcher = null
    }

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater LayoutInflater
     * @param container Optional parent container
     * @param savedInstanceState Saved state if available
     * @return Root view of fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after onCreateView().
     * Initializes ViewModels, observes LiveData, sets up adapters and refresh listener.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModels initialization using ViewModelProvider
        productBannersViewModel = ViewModelProvider(this)[ProductBannersViewModel::class.java]
        homeBannersViewModel = ViewModelProvider(this)[HomeBannersViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        brandsViewModel = ViewModelProvider(this)[BrandsViewModel::class.java]

        // Initialize CartViewModel with repository
        val cartDao = CartDatabase.getInstance(requireContext()).cartDao()
        val repo = CartRepository(cartDao)
        cartViewModel = ViewModelProvider(this, CartViewModelFactory(repo)).get(CartViewModel::class.java)

        // Observe total cart count → update cart badge
        cartViewModel.totalCount.observe(viewLifecycleOwner) { total ->
            updateCartBadge(total)
        }

        // Get auth token from SharedPreferences
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()

        // Observe data from ViewModels
        observeProductsBanners()
        observeHomeBanners()
        observeWishlist()
        observeBrands()

        // Load initial API data
        loadInitialData()

        // Setup notifications adapter
        setupNotifications()

        // Swipe-to-refresh listener with debounce logic
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

    /**
     * Stops auto-scrolling banners by removing handler callbacks.
     */
    private fun stopAutoScroll() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
    }

    /**
     * Starts auto-scrolling banners by posting a delayed runnable.
     */
    private fun startAutoScroll() {
        stopAutoScroll() // Ensure no duplicate runnables
        autoScrollRunnable?.let { handler.postDelayed(it, AUTO_SCROLL_DELAY) }
    }

    /**
     * Initializes the auto-scroll runnable for banners if not already set.
     */
    private fun initializeAutoScrollRunnable() {
        if (autoScrollRunnable == null) {
            autoScrollRunnable = object : Runnable {
                override fun run() {
                    val itemCount = bannerAdapter?.itemCount ?: 0
                    if (itemCount > 1 && originalBannerCount > 0) {
                        val currentItem = binding.viewPager.currentItem
                        val nextItem = currentItem + 1
                        
                        // Check if we're near the end (within last 5% of items)
                        // This ensures we jump to middle before reaching the actual end
                        // This creates seamless infinite forward scrolling
                        val jumpThreshold = (itemCount * 0.95).toInt()
                        
                        if (nextItem >= jumpThreshold) {
                            // Jump to middle position without animation for seamless infinite loop
                            // Calculate middle position aligned to original banner count
                            val middlePosition = (itemCount / 2) - ((itemCount / 2) % originalBannerCount)
                            binding.viewPager.post {
                                binding.viewPager.setCurrentItem(middlePosition, false)
                            }
                        } else {
                            // Move to next item with smooth forward animation
                            binding.viewPager.setCurrentItem(nextItem, true)
                        }
                    }
                    // Re-post runnable to create infinite looping auto-scroll (forward only)
                    handler.postDelayed(this, AUTO_SCROLL_DELAY)
                }
            }
        }
    }

    /**
     * Observes brand API responses from BrandsViewModel.
     */
    private fun observeBrands() {
        brandsViewModel.brandsResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    // Store original brands list (with null safety)
                    originalBrandsList = it.wishlistbrand ?: emptyList()
                }
            }
        }

        brandsViewModel.isLoading.observe(viewLifecycleOwner) {
            // Can toggle progress bar visibility here if needed
        }

        brandsViewModel.apiError.observe(viewLifecycleOwner) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        brandsViewModel.onFailure.observe(viewLifecycleOwner) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    /**
     * Loads initial API data for banners and brands.
     */
    private fun loadInitialData() {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        productBannersViewModel.productBanners(token)
        homeBannersViewModel.homeBanners(token)
        brandsViewModel.brands(token, preferences?.getString("userId", ""))
    }
    /**
     * Observes Home Banners API responses and updates the UI accordingly.
     * Handles round banners, small banners, big banners, deals, and fruits sections.
     */
    private fun observeHomeBanners() {
        // Observe LiveData from ViewModel for Home Sliders
        homeBannersViewModel.homeSlidersResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                // Stop the swipe refresh loader once response is received
                binding.swipeRefreshLayout.isRefreshing = false

                // Check if API call was successful
                if (it.status) {
                    val banners = it.roundSliders // Get round banner list

                    // ---------- Round Images ----------
                    if ((banners.isEmpty())) {
                        // Hide round banners if list is empty
                        binding.shimmerLayoutRoundImages.visibility = View.GONE
                        binding.rvRoundImages.visibility = View.GONE
                    } else {
                        // Show round banners if data available
                        binding.shimmerLayoutRoundImages.visibility = View.GONE
                        binding.rvRoundImages.visibility = View.VISIBLE
                        // Initialize adapter with banners
                        roundImageAdapter = RoundImageAdapter(this, banners, it.cdnURL)
                        binding.rvRoundImages.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        binding.rvRoundImages.adapter = roundImageAdapter
                    }

                    // ---------- Small Banners ----------
                    if ((it.smallSliders.isEmpty())) {
                        // Hide shimmer and show placeholder
                        binding.shimmerLayoutSmallBanner.visibility = View.GONE
                        binding.viewPagerNonScrolling.visibility = View.VISIBLE
                    } else {
                        // Show banners in ViewPager2
                        binding.shimmerLayoutSmallBanner.visibility = View.GONE
                        binding.viewPagerNonScrolling.visibility = View.VISIBLE
                        binding.shimmerLayoutSmallBanner.visibility = View.GONE
                        binding.viewPagerNonScrolling.visibility = View.VISIBLE
                        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false) // 2 columns
                        binding.viewPagerNonScrolling.layoutManager = layoutManager

// Set the adapter as before
                        val adapter = NonScrollingBannerAdapter(this, it.smallSliders, it.cdnURL)
                        binding.viewPagerNonScrolling.adapter = adapter
                    }

                    // ---------- Big Banners ----------
                    if ((it.bigSliders.isEmpty())) {
                        // Hide shimmer and placeholder if empty
                        binding.shimmerLayoutBigBanner.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE
                    } else {
                        // Show banners in ViewPager2
                        binding.shimmerLayoutBigBanner.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE
                        
                        // Store original banner count for infinite scroll calculations
                        originalBannerCount = it.bigSliders.size
                        
                        // Create infinite list by duplicating banners multiple times (for infinite forward scrolling)
                        // Use a large multiplier to create enough items for smooth infinite scrolling
                        val bigSlidersList = it.bigSliders
                        val infiniteBanners = if (bigSlidersList.isNotEmpty()) {
                            (0 until 1000).flatMap { bigSlidersList }
                        } else {
                            bigSlidersList
                        }
                        
                        bannerAdapter = DashboardBannerAdapter(this, infiniteBanners, it.cdnURL)
                        binding.viewPager.adapter = bannerAdapter

                        // Configure ViewPager2 scrolling
                        binding.viewPager.offscreenPageLimit = 3
                        
                        // Start at middle position for infinite forward scrolling
                        // This ensures we have enough room to scroll forward before needing to jump
                        val startPosition = if (originalBannerCount > 0) {
                            (infiniteBanners.size / 2) - ((infiniteBanners.size / 2) % originalBannerCount)
                        } else {
                            0
                        }
                        binding.viewPager.setCurrentItem(startPosition, false)

                        // Apply page transformer for scaling effect
                        val pageTransformer = CompositePageTransformer().apply {
                            addTransformer(MarginPageTransformer(30))
                            addTransformer { page, position ->
                                val scaleFactor = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
                                page.scaleY = scaleFactor
                            }
                        }
                        binding.viewPager.setPageTransformer(pageTransformer)

                        // Initialize and start auto-scrolling
                        initializeAutoScrollRunnable()
                        startAutoScroll()
                    }

                    // ---------- Deals Section ----------
                    if ((it.dealsSliders.isEmpty())) {
                        // Hide deals section if no data
                        binding.shimmerLayoutDealsBanner.visibility = View.GONE
                        binding.frameDeals.visibility = View.GONE
                        binding.recyclerViewDeals.visibility = View.GONE
//                        binding.viewDeals.visibility = View.GONE
                    } else {
                        // Show deals data
                        binding.shimmerLayoutDealsBanner.visibility = View.GONE
//                        binding.viewDeals.visibility = View.GONE
                        binding.recyclerViewDeals.visibility = View.VISIBLE
                        binding.frameDeals.visibility = View.VISIBLE
                        binding.tvDealsCenter.visibility = View.VISIBLE
                        binding.tvDealsCenter.text = it.dealsHeader
                        binding.recyclerViewDeals.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        // Attach deals adapter
                        nonScrollingBannerDealsAdapter = NonScrollingBannerDealsAdapter(this, it.dealsSliders, it.cdnURL)
                        binding.recyclerViewDeals.adapter = nonScrollingBannerDealsAdapter
                    }

                    // ---------- Fruits Section ----------
                    if ((it.fruitSliders.isEmpty())) {
                        // Hide fruit section if no data
                        binding.shimmerLayoutFruitsBanner.visibility = View.GONE
                        binding.recyclerViewFruits.visibility = View.GONE
                        binding.viewFruits.visibility = View.GONE
                        binding.frameFruits.visibility = View.GONE
                    } else {
                        // Show fruit banners
                        binding.shimmerLayoutFruitsBanner.visibility = View.GONE
                        binding.viewFruits.visibility = View.GONE
                        binding.recyclerViewFruits.visibility = View.VISIBLE
                        binding.frameFruits.visibility = View.VISIBLE
                        binding.tvFruitsCenter.visibility = View.VISIBLE
                        binding.tvFruitsCenter.text = it.fruitHeader
                        binding.recyclerViewFruits.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                        // Attach fruits adapter
                        nonScrollingBannerFruitsAdapter = NonScrollingBannerFruitsAdapter(this, it.fruitSliders, it.cdnURL)
                        binding.recyclerViewFruits.adapter = nonScrollingBannerFruitsAdapter
                    }
                } else {
                    // Log error if response status is false
                    Log.e("TAG", "observeHomeBanners: ")
                }
            }
        }

        // Observe loading state for shimmer visibility
        homeBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == true) {
                // Show all shimmer placeholders, hide content
                binding.shimmerLayoutRoundImages.visibility = View.VISIBLE
                binding.rvRoundImages.visibility = View.GONE
                binding.shimmerLayoutSmallBanner.visibility = View.VISIBLE
                binding.viewPagerNonScrolling.visibility = View.GONE
                binding.shimmerLayoutBigBanner.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
                binding.shimmerLayoutDealsBanner.visibility = View.VISIBLE
//                binding.viewDeals.visibility = View.VISIBLE
                binding.recyclerViewDeals.visibility = View.GONE
                binding.tvDealsCenter.visibility = View.GONE
                binding.shimmerLayoutFruitsBanner.visibility = View.VISIBLE
                binding.viewFruits.visibility = View.VISIBLE
                binding.recyclerViewFruits.visibility = View.GONE
                binding.tvFruitsCenter.visibility = View.GONE
            }
        }

        // Observe API error responses
        homeBannersViewModel.apiError.observe(viewLifecycleOwner) {
            // Hide all shimmer loaders and indicators
            binding.shimmerLayoutRoundImages.visibility = View.GONE
            binding.shimmerLayoutSmallBanner.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            binding.shimmerLayoutBigBanner.visibility = View.GONE
            binding.shimmerLayoutDealsBanner.visibility = View.GONE
            binding.shimmerLayoutFruitsBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe API failure (e.g., network failure, timeout)
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

    /**
     * Observes Product Banners API and sets up adapters for
     * new products and top seller products.
     */
    private fun observeProductsBanners() {
        productBannersViewModel.productSlidersResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    // Assign lists and CDN URL
                    originalCategoryList = it.newProductBanners
                    originalTopSellerList = it.topSellerBanners
                    cdnUrl = it.cdnURL

                    // ---------- New Products ----------
                    if (originalCategoryList.isEmpty()) {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE
                        binding.frameProducts.visibility = View.GONE
                    } else {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE
                        binding.viewNewProducts.visibility = View.GONE
                        binding.recyclerViewDrinks.visibility = View.VISIBLE
                        binding.frameProducts.visibility = View.VISIBLE
                        binding.llNewProducts.visibility = View.VISIBLE
                        binding.tvNewProducts.visibility = View.VISIBLE
                        binding.tvNewProducts.text = it.newProductHeader

                        setupNewProductsAdapter()

                        val layoutManager = binding.recyclerViewDrinks.layoutManager as? LinearLayoutManager

                        // Left Arrow click → scroll left
                        binding.ivLeftArrow.setOnClickListener {
                            val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
                            if (firstVisible > 0) {
                                binding.recyclerViewDrinks.smoothScrollToPosition(firstVisible - 1)
                            }
                        }

                        // Right Arrow click → scroll right
                        binding.ivRightArrow.setOnClickListener {
                            val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: 0
                            val totalItems = layoutManager?.itemCount ?: 0
                            if (lastVisible < totalItems - 1) {
                                binding.recyclerViewDrinks.smoothScrollToPosition(lastVisible + 1)
                            }
                        }
                    }

                    // ---------- Top Sellers ----------
                    if (originalTopSellerList.isEmpty()) {
                        binding.shimmerLayoutNewProducts.visibility = View.GONE
                        binding.frameSeller.visibility = View.GONE
                    } else {
                        binding.shimmerLayoutTopSeller.visibility = View.GONE
                        binding.viewTopSeller.visibility = View.GONE
                        binding.recyclerViewNutrition.visibility = View.VISIBLE
                        binding.frameSeller.visibility = View.VISIBLE
                        binding.tvTopSeller.visibility = View.VISIBLE
                        binding.llTopProducts.visibility = View.VISIBLE
                        binding.tvTopSeller.text = it.topSellerHeader
                        setupTopSellerAdapter()

                        val layoutManager = binding.recyclerViewNutrition.layoutManager as? LinearLayoutManager

                        // Left Arrow click → scroll left
                        binding.ivLeftArrowTopSeller.setOnClickListener {
                            val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
                            if (firstVisible > 0) {
                                binding.recyclerViewNutrition.smoothScrollToPosition(firstVisible - 1)
                            }
                        }

                        // Right Arrow click → scroll right
                        binding.ivRightArrowTopSeller.setOnClickListener {
                            val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: 0
                            val totalItems = layoutManager?.itemCount ?: 0
                            if (lastVisible < totalItems - 1) {
                                binding.recyclerViewNutrition.smoothScrollToPosition(lastVisible + 1)
                            }
                        }
                    }
                }
            }
        }

        // Observe loading state
        productBannersViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == true) {
                // Show shimmer, hide recyclers
                binding.shimmerLayoutNewProducts.visibility = View.VISIBLE
                binding.viewNewProducts.visibility = View.VISIBLE
                binding.recyclerViewDrinks.visibility = View.GONE
                binding.tvNewProducts.visibility = View.GONE
                binding.llTopProducts.visibility = View.GONE
                binding.llNewProducts.visibility = View.GONE
                binding.shimmerLayoutTopSeller.visibility = View.VISIBLE
                binding.viewTopSeller.visibility = View.VISIBLE
                binding.recyclerViewNutrition.visibility = View.GONE
                binding.tvTopSeller.visibility = View.GONE
            }
        }

        // Observe API error
        productBannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutNewProducts.visibility = View.GONE
            binding.shimmerLayoutTopSeller.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe API failure
        productBannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutNewProducts.visibility = View.GONE
            binding.shimmerLayoutTopSeller.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    /**
     * Observes Wishlist API responses and updates product items accordingly.
     */
    private fun observeWishlist() {
        wishlistViewModel.wishlistResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    updateWishlistInAdapter(variantId)
                    // Refresh brands API to get updated wishlist state
                    val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
                    brandsViewModel.brands(token, preferences?.getString("userId", ""))
                } else {

                }
            }
        }

        wishlistViewModel.isLoading.observe(viewLifecycleOwner) {
            // Loader visibility if needed
            // binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        }

        wishlistViewModel.apiError.observe(viewLifecycleOwner) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        wishlistViewModel.onFailure.observe(viewLifecycleOwner) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    /**
     * Displays a custom Snackbar at the bottom with customized styles.
     */
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM // Snackbar at bottom
        params.bottomMargin = 50 // Add margin
        view.layoutParams = params

        // Set background color
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))

        // Customize text
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    /**
     * Updates wishlist items in adapters efficiently by using notifyItemChanged().
     */
    private fun updateWishlistInAdapter(variantId: String) {
        var updated = false

        // Check New Products first
        originalCategoryList.forEachIndexed { prodIndex, product ->
            if (product.mvariant_id.toString() == variantId) {
                // Toggle wishlist flag
                product.product.user_info_wishlist = !product.product.user_info_wishlist
                nonScrollingBannerDrinksAdapter?.notifyItemChanged(prodIndex) // Efficient update
                updated = true
                return@forEachIndexed
            }
        }

        // If not found in New Products, check Top Sellers
        if (!updated) {
            originalTopSellerList.forEachIndexed { prodIndex, product ->
                if (product.mvariant_id.toString() == variantId) {
                    product.product.user_info_wishlist = !product.product.user_info_wishlist
                    nonScrollingBannerTopSellerAdapter?.notifyItemChanged(prodIndex) // Efficient update
                    updated = true
                    return@forEachIndexed
                }
            }
        }
    }

    /**
     * Sets up RecyclerView adapter for New Products.
     */
    private fun setupNewProductsAdapter() {
        nonScrollingBannerDrinksAdapter = NonScrollingBannerNewProductsAdapter(
            listener = this,
            products = originalCategoryList,
            cdnURL = cdnUrl,
            context = requireContext(),
            cartViewModel = cartViewModel,
            lifecycleOwner = viewLifecycleOwner
        )
        binding.recyclerViewDrinks.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewDrinks.adapter = nonScrollingBannerDrinksAdapter
    }

    /**
     * Sets up RecyclerView adapter for Top Seller products.
     */
    private fun setupTopSellerAdapter() {
        nonScrollingBannerTopSellerAdapter = NonScrollingBannerTopSellerAdapter(
            listener = this,
            products = originalTopSellerList,
            cdnURL = cdnUrl,
            context = requireContext(),
            cartViewModel = cartViewModel,
            lifecycleOwner = viewLifecycleOwner
        )
        binding.recyclerViewNutrition.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewNutrition.adapter = nonScrollingBannerTopSellerAdapter
    }

    /**
     * Updates cart badge when cart count changes.
     */
    override fun onUpdateCart(totalItems: Int, productId: Int) {
        updateCartBadge(totalItems)
        // Check for deal applications when cart is updated
        checkAndShowDealPopup(productId)
    }

    /**
     * Handles wishlist update when user taps wishlist button.
     */
    override fun onUpdateWishlist(mvariant_id: String, type: String) {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "") ?: ""
        variantId = mvariant_id
        this.type = type
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    /**
     * Updates cart badge on UI thread.
     */
    private fun updateCartBadge(count: Int) {
        activity?.runOnUiThread {
            // Optional UI badge update logic
        }
    }
    
    // Check if any deals are triggered and show popup
    private fun checkAndShowDealPopup(updatedProductId: Int) {
        lifecycleScope.launch {
            // Add small delay to ensure database update is complete
            kotlinx.coroutines.delay(100)
            
            val cartItems = withContext(Dispatchers.IO) {
                cartDao.getAllItems().first()
            }
            
            // Find the product that was just updated
            val updatedProduct = findProductByVariantId(updatedProductId) ?: return@launch
            
            // Check if this product has a deal
            if (updatedProduct.deal_type.isNullOrEmpty()) return@launch
            
            // Find the cart item for this product
            val cartItem = cartItems.find { it.variantId == updatedProductId } ?: return@launch
            
            // Calculate deal application
            val dealResult = calculateDealApplication(updatedProduct, cartItem.quantity)
            
            // Show popup only when a new deal threshold is crossed
            if (dealResult.isTriggered) {
                val threshold = getDealThreshold(updatedProduct)
                if (threshold > 0) {
                    // Check if current quantity has crossed a new threshold
                    val currentThreshold = (cartItem.quantity / threshold) * threshold
                    val lastShownThreshold = shownDeals[updatedProductId] ?: 0
                    
                    // Show popup only when crossing a new threshold
                    if (currentThreshold > lastShownThreshold && currentThreshold >= threshold) {
                        // Recalculate deal with threshold quantity for accurate free items
                        val thresholdDealResult = calculateDealApplication(updatedProduct, currentThreshold)
                        showDealAppliedDialog(thresholdDealResult)
                        shownDeals[updatedProductId] = currentThreshold
                    }
                }
            } else {
                // If deal is no longer triggered (quantity dropped below threshold), reset shownDeals
                // This allows popup to show again when user adds items back
                shownDeals.remove(updatedProductId)
            }
        }
    }
    
    // Find product by variant ID in the current product lists
    private fun findProductByVariantId(variantId: Int): com.app.truewebapp.data.dto.dashboard_banners.Product? {
        // Check in new products
        originalCategoryList.forEach { productBanner ->
            if (productBanner.mvariant_id == variantId) {
                return productBanner.product
            }
        }
        // Check in top sellers
        originalTopSellerList.forEach { productBanner ->
            if (productBanner.mvariant_id == variantId) {
                return productBanner.product
            }
        }
        return null
    }
    
    // Get the deal threshold (minimum quantity needed to trigger deal)
    private fun getDealThreshold(product: com.app.truewebapp.data.dto.dashboard_banners.Product): Int {
        return when (product.deal_type) {
            "buy_x_get_y" -> product.deal_buy_quantity ?: 0
            "volume_discount" -> product.deal_quantity ?: 0
            else -> 0
        }
    }
    
    // Calculate if deal is triggered and how many free items
    private fun calculateDealApplication(product: com.app.truewebapp.data.dto.dashboard_banners.Product, quantity: Int): DealResult {
        return when (product.deal_type) {
            "buy_x_get_y" -> {
                val buyQty = product.deal_buy_quantity ?: 0
                val getQty = product.deal_get_quantity ?: 0
                
                if (buyQty > 0 && getQty > 0 && quantity >= buyQty) {
                    val freeItems = (quantity / buyQty) * getQty
                    val dealName = "Buy $buyQty Get $getQty Free"
                    DealResult(true, freeItems, dealName)
                } else {
                    DealResult(false, 0, "")
                }
            }
            "volume_discount" -> {
                val dealQty = product.deal_quantity ?: 0
                val dealPrice = product.deal_price ?: 0.0
                
                if (dealQty > 0 && dealPrice > 0 && quantity >= dealQty) {
                    val dealName = "Any $dealQty for £%.2f".format(dealPrice)
                    DealResult(true, 0, dealName, isVolumeDiscount = true)
                } else {
                    DealResult(false, 0, "")
                }
            }
            else -> DealResult(false, 0, "")
        }
    }
    
    // Show deal applied dialog
    private fun showDealAppliedDialog(dealResult: DealResult) {
        // Set title with emojis
        binding.tvDealDialogTitle.text = "🎉 Deal Applied! 🎉"
        
        // Set description text based on deal type - matching screenshot format
        val descriptionText = if (dealResult.isVolumeDiscount) {
            "Special deal: ${dealResult.dealName}"
        } else {
            // Format: "You got X free item! Buy Y Get Z Free deal applied."
            val itemText = if (dealResult.freeItems == 1) "item" else "items"
            "You got ${dealResult.freeItems} free $itemText! ${dealResult.dealName} deal applied."
        }
        
        binding.tvDealDialogMessage.text = descriptionText
        
        // Show dialog background and container
        binding.dealDialogBackground.visibility = View.VISIBLE
        binding.dealDialogContainer.visibility = View.VISIBLE
        
        // OK button click listener
        binding.btnDealDialogOk.setOnClickListener {
            binding.dealDialogBackground.visibility = View.GONE
            binding.dealDialogContainer.visibility = View.GONE
        }
    }
    
    // Data class to hold deal calculation results
    private data class DealResult(
        val isTriggered: Boolean,
        val freeItems: Int,
        val dealName: String,
        val isVolumeDiscount: Boolean = false
    )

    /**
     * Lifecycle cleanup → removes callbacks for auto-scroll runnable.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
        shownDeals.clear() // Clear deal tracking
    }

    /**
     * Sets up notifications UI and related click listeners.
     * Initializes RecyclerView for recent notifications, adapters, and navigation to different screens.
     */
    private fun setupNotifications() {
        // Set a vertical LinearLayoutManager for recent notifications RecyclerView
        binding.recentNotificationsRecycler.layoutManager = LinearLayoutManager(context)

        // Create a static list of notification options (mock/demo data)
        val options = listOf(
            NotificationOption("Yov've left products in your basket"),
            NotificationOption("Meltz Dubai Chocolate - STOCK UP TODAY!"),
            NotificationOption("Yov've left products in your basket"),
            NotificationOption("Meltz Dubai Chocolate - STOCK UP TODAY!"),
            NotificationOption("Yov've left products in your basket"),
        )

        // Initialize adapter with the options list and set click listener
        notificationsAdapter = NotificationsAdapter(options) {
            // Open Notification detail screen when item clicked
            val intent = Intent(context, NotificationDetailActivity::class.java)
            startActivity(intent)
        }

        // Attach adapter to RecyclerView
        binding.recentNotificationsRecycler.adapter = notificationsAdapter

        // Handle "View Order" layout click
        binding.viewOrderLayout.setOnClickListener {
            // Provide haptic feedback on click
            binding.viewOrderLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to override global haptics
            )
            // Navigate to Orders List screen
            val intent = Intent(context, OrdersListActivity::class.java)
            startActivity(intent)
        }

        // Handle "Shop" layout click
        binding.shopLayout.setOnClickListener {
            // Provide haptic feedback on click
            binding.shopLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            // Switch tab to Shop
            tabSwitcher?.switchToShopTab()

            // Try to find the ShopFragment and call its categories API
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            shopFragment?.categoriesViewModel?.categories("", token, "")
        }

        // Handle "Favorites" layout click
        binding.favLayout.setOnClickListener {
            // Provide haptic feedback on click
            binding.favLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            // Switch tab to Shop
            tabSwitcher?.switchToShopTab()

            // Delay to ensure Shop tab is fully loaded before accessing it
            Handler(Looper.getMainLooper()).postDelayed({
                // Find ShopFragment and call the public method to set Favourites filter
                val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
                shopFragment?.setFavouritesFilter()
            }, 500) // Delay 500ms for stability
        }
    }

    /**
     * Callback when a Big Banner is updated (clicked).
     * Switches to Shop tab and scrolls to the specific product in ShopFragment.
     */
    override fun onUpdateBigBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        // Switch to Shop tab
        tabSwitcher?.switchToShopTab()

        // Delay to ensure Shop tab UI is ready
        Handler(Looper.getMainLooper()).postDelayed({
            // Find ShopFragment
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            // Scroll to the specific product
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)

        }, 1000) // Delay 1000ms for Big Banner
    }

    /**
     * Callback when a Small Banner is updated.
     */
    override fun onUpdateSmallBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)
        }, 500) // Delay 500ms for stability
    }

    /**
     * Callback when a Round Banner is updated.
     */
    override fun onUpdateRoundBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)
        }, 500)
    }

    /**
     * Callback when a Deals Banner is updated.
     */
    override fun onUpdateDealsBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)
        }, 500)
    }

    /**
     * Callback when a Fruits Banner is updated.
     */
    override fun onUpdateFruitsBanner(
        main_mcat_id: String,
        mcat_id: String,
        msubcat_id: String,
        mproduct_id: String
    ) {
        tabSwitcher?.switchToShopTab()

        Handler(Looper.getMainLooper()).postDelayed({
            val shopFragment = parentFragmentManager.findFragmentByTag("f1") as? ShopFragment
            shopFragment?.scrollToProduct(main_mcat_id, mcat_id, msubcat_id, mproduct_id)
        }, 500)
    }

    /**
     * Refreshes adapters for products and top sellers.
     * Uses notifyDataSetChanged() to refresh visible items only.
     */
    override fun refreshAdapters() {
        nonScrollingBannerDrinksAdapter?.notifyDataSetChanged()
        nonScrollingBannerTopSellerAdapter?.notifyDataSetChanged()
    }
}