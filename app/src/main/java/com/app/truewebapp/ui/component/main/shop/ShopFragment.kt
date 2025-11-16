package com.app.truewebapp.ui.component.main.shop

// Import necessary classes, adapters, Android framework libraries, ViewModels, utilities, etc.
import BannerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.browse.BrowseBanners
import com.app.truewebapp.data.dto.browse.MainCategories
import com.app.truewebapp.data.dto.browse.Product
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.databinding.FragmentShopBinding
import com.app.truewebapp.ui.component.main.cart.CartUpdateListener
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartDatabase
import com.app.truewebapp.ui.component.main.cart.cartdatabase.CartItemEntity
import com.app.truewebapp.ui.component.main.dashboard.TabSwitcher
import com.app.truewebapp.ui.viewmodel.BannersViewModel
import com.app.truewebapp.ui.viewmodel.BrandsViewModel
import com.app.truewebapp.ui.viewmodel.CategoriesViewModel
import com.app.truewebapp.ui.viewmodel.WishlistViewModel
import com.app.truewebapp.utils.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ShopFragment displays categories, banners, brands and products.
 * - Integrates with ViewModels for categories, banners, brands, and wishlist
 * - Handles search, filters, swipe refresh, auto-scrolling banners
 * - Provides cart and wishlist updates
 */
class ShopFragment : Fragment(), ProductAdapterListener {

    // View binding object for accessing views in fragment_shop.xml
    lateinit var binding: FragmentShopBinding

    // ViewModels for categories, banners, wishlist, and brands
    lateinit var categoriesViewModel: CategoriesViewModel
    private lateinit var bannersViewModel: BannersViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var brandsViewModel: BrandsViewModel

    // RecyclerView adapters
    private var adapter: ShopMainCategoryAdapter? = null
    private var bannerAdapter: BannerAdapter? = null

    // Interfaces for tab switching and cart updates
    private var tabSwitcher: TabSwitcher? = null
    private var cartUpdateListener: CartUpdateListener? = null

    // Data holders
    private var originalCategoryList: List<MainCategories> = listOf() // Stores original categories
    private val handler = Handler(Looper.getMainLooper())             // Handler for auto-scroll
    private var autoScrollRunnable: Runnable? = null                  // Runnable for auto-scroll
    private val AUTO_SCROLL_DELAY: Long = 3000                        // Auto-scroll delay in ms
    private var search = ""                                           // Search query
    private var token = ""                                            // Auth token
    private var variantId = ""                                        // Product variant ID
    var filters = ""                                                  // Selected filters
    var filtersType = "All"                                           // Filter type (All / Favourites)
    var applyFilter = false                                           // Flag for applied filter
    var cdnUrl = ""                                                   // CDN base URL for images

    // Tracks content visibility state when filter overlay is toggled
    private var wasContentVisibleBeforeFilter = false
    
    // Track shown deals to prevent duplicate popups
    private val shownDeals = mutableMapOf<Int, Int>() // variantId to quantity when deal was shown

    /**
     * Inflate the fragment layout using view binding
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called when view is created.
     * - Initializes cart listeners using Room DB
     * - Initializes ViewModels, views, observers, and loads initial data
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access cart DAO
        val cartDao = CartDatabase.getInstance(requireContext()).cartDao()

        // Launch coroutines tied to fragment lifecycle
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            // Collect cart items and update badge (including free items)
            launch {
                cartDao.getAllItems().collect { cartItems ->
                    val totalCount = calculateTotalCount(cartItems)
                    if (totalCount == 0) {
                        binding.tvCartBadge.visibility = View.GONE
                    } else {
                        binding.tvCartBadge.visibility = View.VISIBLE
                        binding.tvCartBadge.text = totalCount.toString()
                    }
                }
            }

            // Collect all cart items and update total price
            launch {
                cartDao.getAllItems().collectLatest { cartItems ->
                    Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")
                    if (cartItems.isNotEmpty()) {
                        updateTotalAmount(cartItems)
                    } else {
                        binding.tvTotalAmount.text = "£0.00"
                    }
                }
            }
        }

        // Setup ViewModels, views, observers, and load data
        initializeViewModels()
        setupViews()
        setupObservers()
        loadInitialData()
    }

    /**
     * Called when fragment is attached to activity
     * - Ensures context implements TabSwitcher and CartUpdateListener
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabSwitcher) {
            tabSwitcher = context
        }
        if (context is CartUpdateListener) {
            cartUpdateListener = context
        } else {
            throw RuntimeException("$context must implement CartUpdateListener")
        }
    }

    /**
     * Called when fragment is detached
     * - Nullifies interface references
     */
    override fun onDetach() {
        super.onDetach()
        tabSwitcher = null
        cartUpdateListener = null
    }

    /**
     * Initialize ViewModels using ViewModelProvider
     * Also retrieves auth token from SharedPreferences
     */
    private fun initializeViewModels() {
        categoriesViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        bannersViewModel = ViewModelProvider(this)[BannersViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        brandsViewModel = ViewModelProvider(this)[BrandsViewModel::class.java]

        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
    }

    /**
     * Setup UI interactions like filter, cart, cancel, apply, refresh, search, radio buttons
     */
    private fun setupViews() {
        // Filter button click
        binding.filterButton.setOnClickListener {
            binding.filterButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            toggleFilterOverlay()
        }

        // Cart button click
        binding.cart.setOnClickListener {
            binding.cart.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            tabSwitcher?.switchToCartTab()
        }

        // Cancel filter click
        binding.cancelLayout.setOnClickListener {
            binding.cancelLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            toggleFilterOverlay()
            applyFilter = false
        }

        // Apply filter click
        binding.applyLayout.setOnClickListener {
            binding.applyLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val selectedIds = brandsViewModel.brandsResponse.value?.mbrands
                ?.filter { it.isSelected }
                ?.map { it.mbrand_id }
                ?.joinToString(",").orEmpty()

            if (applyFilter) toggleFilterOverlay()
            filters = if (filtersType == "Favourites") selectedIds else ""
            applyFilter = true
            val wishlistParam = if (filtersType == "Favourites") true else null
            categoriesViewModel.categories(search, token, filters, wishlistParam)
        }

        // Swipe refresh reloads data
        binding.swipeRefreshLayout.setOnRefreshListener {
            stopAutoScroll()
            // Don't reset applyFilter - preserve current filter state
            loadInitialData()
        }

        setupSearchView()
        setupRadioGroup()
    }

    // Called when the Fragment comes to the foreground and becomes interactive
    override fun onResume() {
        super.onResume() // Always call the parent class's onResume()

        // Reset to default state when Browse tab is tapped
        resetToDefaultState()

        // Refresh the adapter data when returning to this screen
        adapter?.notifyDataSetChanged()
    }

    // Called when the Fragment is no longer in the foreground
    override fun onPause() {
        super.onPause() // Always call the parent class's onPause()
        stopAutoScroll() // Stop auto-scrolling when the fragment is paused (to save resources)
    }

    @SuppressLint("ClickableViewAccessibility") // Suppresses warning about touch accessibility
    private fun setupSearchView() {
        // Load drawable icons for close and search buttons
        val closeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
        val searchIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)

        // Add a text change listener to the search input field
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            // Called after the text is changed
            override fun afterTextChanged(s: Editable?) {
                // Save the current search query (trimmed)
                search = s.toString().trim()

                // Dynamically set left (search) and right (close) icons based on text presence
                binding.searchInput.setCompoundDrawablesWithIntrinsicBounds(
                    if (search.isEmpty()) searchIcon else null, // Show search icon when empty
                    null,
                    if (search.isNotEmpty()) closeIcon else null, // Show close icon when text exists
                    null
                )

                // Trigger API call to fetch filtered categories based on search input
                val wishlistParam = if (applyFilter && filtersType == "Favourites") true else null
                categoriesViewModel.categories(search, token, filters, wishlistParam)
            }

            // Not used but must be overridden: before text changes
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Not used but must be overridden: while text is changing
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Handle touch events inside the search input field
        binding.searchInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) { // When finger lifts
                val drawableEnd = binding.searchInput.compoundDrawables[2] // Right drawable (close icon)
                drawableEnd?.let {
                    val drawableWidth = it.bounds.width()
                    // Calculate touch area where the drawable (close button) is located
                    val touchAreaStart = binding.searchInput.width - binding.searchInput.paddingEnd - drawableWidth
                    if (event.x > touchAreaStart) {
                        // Clear the search input when close icon is tapped
                        binding.searchInput.text?.clear()
                        return@setOnTouchListener true
                    }
                }
            }
            false // Allow other touch events to proceed
        }
    }

    // Setup filtering options using a RadioGroup
    private fun setupRadioGroup() {
        binding.allRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            // Determine which filter option is selected
            filtersType = when (checkedId) {
                R.id.radioAllProducts -> "All"
                R.id.radioFavourites -> "Favourites"
                else -> "All"
            }

            // Get the latest response from brandsViewModel (if null, exit)
            val response = brandsViewModel.brandsResponse.value ?: return@setOnCheckedChangeListener

            // Add null safety check for mbrands
            val mbrands = response.mbrands ?: return@setOnCheckedChangeListener

            // Apply filter logic based on selected type
            if (filtersType == "Favourites") {
                // Collect IDs of brands in the wishlist (with null safety)
                val wishlistIds = (response.wishlistbrand ?: emptyList()).map { it.mbrand_id }
                // Mark brands as selected if they exist in wishlist
                mbrands.forEach { brand ->
                    brand.isSelected = wishlistIds.contains(brand.mbrand_id)
                }
            } else {
                // Reset selection for all brands when "All" is selected
                mbrands.forEach { brand ->
                    brand.isSelected = false
                }
            }

            // Create and set the adapter with updated brand list
            val brandsAdapter = BrandsAdapter(
                mbrands,
                response.cdnURL,
                filtersType,
                response
            )
            binding.brandsRecyclerView.layoutManager = GridLayoutManager(context, 5) // 5 columns grid
            binding.brandsRecyclerView.adapter = brandsAdapter
        }
    }

    // Load required initial data when the fragment starts
    private fun loadInitialData() {
        // Retrieve SharedPreferences for persistent data
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)

        // Preserve current filter state when refreshing
        val currentFilters = if (applyFilter) filters else ""
        val wishlistParam = if (applyFilter && filtersType == "Favourites") true else null
        
        // Trigger API calls to fetch initial data with preserved filters
        categoriesViewModel.categories(search, token, currentFilters, wishlistParam)
        bannersViewModel.banners(token)
        brandsViewModel.brands(token, preferences?.getString("userId", "")) // Pass stored userId if available
    }

    // Attach observers for ViewModels to handle LiveData changes
    private fun setupObservers() {
        observeCategories() // Observe category changes
        observeBanners()    // Observe banner updates
        observeWishlist()   // Observe wishlist changes
        observeBrands()     // Observe brand updates
//        observeCart()     // (Currently disabled) Observe cart updates
    }

    // Observe category LiveData updates
    private fun observeCategories() {
        // Observe categories response
        categoriesViewModel.categoriesModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                // Stop pull-to-refresh loader
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    // Hide shimmer loader
                    binding.shimmerLayoutMainCategory.visibility = View.GONE

                    // If no categories found, show "no data" UI
                    if (it.main_categories.isEmpty()) {
                        showNoDataView(true)
                    } else {
                        // Categories found → update UI
                        cdnUrl = it.cdnURL
                        binding.shimmerLayoutMainCategory.visibility = View.GONE
                        showNoDataView(false)
                        originalCategoryList = it.main_categories
                        setupShopUI(originalCategoryList, it.cdnURL) // Populate RecyclerView
                    }
                }
            }
        }

        // Observe loading state
        categoriesViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == true) {
                // Show shimmer loader while loading data
                binding.shimmerLayoutMainCategory.visibility = View.VISIBLE
                binding.shopMainCategoryRecycler.visibility = View.GONE
            }
        }

        // Observe API error messages
        categoriesViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutMainCategory.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe general failures
        categoriesViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutMainCategory.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    // Observe banner LiveData updates
    private fun observeBanners() {
        bannersViewModel.bannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    // Hide shimmer loader
                    binding.shimmerLayoutBanner.visibility = View.GONE
                    val banners = it.browseBanners

                    // Show/hide banner section based on data availability
                    if (banners.isEmpty()) {
                        showBannerView(false)
                    } else {
                        binding.shimmerLayoutBanner.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE
                        showBannerView(true)
                        // Setup adapter with fetched banners
                        setUpNonScrollingBannerAdapter(banners, it.cdnURL)
                    }
                } else {
                    showBannerView(false)
                }
            }
        }

        // Observe loading state for banners
        bannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide banner ViewPager
            if (it == true) {
                binding.shimmerLayoutBanner.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
            }
        }

        // Observe API error messages for banners
        bannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe general failures for banners
        bannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutBanner.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    // Observe wishlist LiveData updates
    private fun observeWishlist() {
        wishlistViewModel.wishlistResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                if (it.status) {
                    // Update wishlist in adapter if response is successful
                    updateWishlistInAdapter(variantId)
                    // Refresh brands API to get updated wishlist state
                    val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
                    brandsViewModel.brands(token, preferences?.getString("userId", ""))
                } else {
                    // Show error message if operation failed
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe loading state for wishlist (currently commented UI update)
        wishlistViewModel.isLoading.observe(viewLifecycleOwner) {
//            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        }

        // Observe API error messages for wishlist
        wishlistViewModel.apiError.observe(viewLifecycleOwner) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe general failures for wishlist
        wishlistViewModel.onFailure.observe(viewLifecycleOwner) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    // Observe brand LiveData updates
    private fun observeBrands() {
        brandsViewModel.brandsResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    // Use let block to safely handle nullable mbrands
                    it.mbrands?.let { mbrands ->
                        // Only proceed if mbrands is not empty
                        if (mbrands.isNotEmpty()) {
                            // Show brands if available
                            binding.brandsRecyclerView.visibility = View.VISIBLE
                            
                            // Restore radio button selection if filters are applied
                            if (applyFilter && filtersType == "Favourites") {
                                binding.radioFavourites.isChecked = true
                                // Apply wishlist filter to brands (with null safety)
                                val wishlistIds = (it.wishlistbrand ?: emptyList()).map { brand -> brand.mbrand_id }
                                mbrands.forEach { brand ->
                                    brand.isSelected = wishlistIds.contains(brand.mbrand_id)
                                }
                            } else if (applyFilter && filtersType == "All") {
                                binding.radioAllProducts.isChecked = true
                            }
                            
                            val brandsAdapter = BrandsAdapter(
                                mbrands,
                                it.cdnURL,
                                filtersType,
                                it
                            )
                            binding.brandsRecyclerView.layoutManager = GridLayoutManager(context, 5)
                            binding.brandsRecyclerView.adapter = brandsAdapter
                        } else {
                            // Hide brands if empty
                            binding.brandsRecyclerView.visibility = View.GONE
                        }
                    } ?: run {
                        // Hide brands if mbrands is null
                        binding.brandsRecyclerView.visibility = View.GONE
                    }
                } else {
                    // Hide brands if API response failed
                    binding.brandsRecyclerView.visibility = View.GONE
                }
            }
        }

        // Observe loading state for brands (currently commented UI update)
        brandsViewModel.isLoading.observe(viewLifecycleOwner) {
//            binding.progressBarLayout.visibility = if (it == true) View.VISIBLE else View.GONE
        }

        // Observe API error messages for brands
        brandsViewModel.apiError.observe(viewLifecycleOwner) {
            showTopSnackBar(it ?: "Unexpected API error")
        }

        // Observe general failures for brands
        brandsViewModel.onFailure.observe(viewLifecycleOwner) {
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    // Function to update wishlist status for a product in the nested adapters
    private fun updateWishlistInAdapter(variantId: String) {
        // Loop through the list of main categories with their indices
        originalCategoryList.forEachIndexed { mainCatIndex, mainCategory ->
            // Loop through the child categories of each main category
            mainCategory.categories.forEachIndexed { catIndex, category ->
                // Loop through the subcategories of each category
                category.subcategories.forEachIndexed { subCatIndex, subCategory ->
                    // Loop through the products of each subcategory
                    subCategory.products.forEachIndexed { prodIndex, product ->
                        // Check if the product's variant ID matches the one passed
                        if (product.mvariant_id.toString() == variantId) {
                            // Toggle wishlist state (add/remove)
                            product.user_info_wishlist = !product.user_info_wishlist

                            // Find the ViewHolder for the main category
                            (binding.shopMainCategoryRecycler.findViewHolderForAdapterPosition(mainCatIndex)
                                    as? ShopMainCategoryAdapter.MainCategoryViewHolder)?.let { mainCatViewHolder ->
                                // Find the ViewHolder for the category inside the main category
                                (mainCatViewHolder.getCategoryRecycler().findViewHolderForAdapterPosition(catIndex)
                                        as? ShopCategoryAdapter.CategoryViewHolder)?.let { categoryViewHolder ->
                                    // Find the ViewHolder for the subcategory inside the category
                                    (categoryViewHolder.getSubCategoryRecycler().findViewHolderForAdapterPosition(subCatIndex)
                                            as? SubCategoryAdapter.SubCategoryViewHolder)?.let { subCatViewHolder ->
                                        // Notify product adapter to refresh the changed item
                                        subCatViewHolder.getProductAdapter()?.notifyItemChanged(prodIndex)
                                    }
                                }
                            }
                            // Exit the loop once the product is found and updated
                            return@forEachIndexed
                        }
                    }
                }
            }
        }
    }

    // Function to set up the banner ViewPager with custom adapter and auto-scroll
    private fun setUpNonScrollingBannerAdapter(browseBanners: List<BrowseBanners>, cdnURL: String) {
        // Stop any existing auto-scrolling before setting a new adapter
        stopAutoScroll()

        // Initialize the banner adapter with click listener
        bannerAdapter = BannerAdapter(browseBanners, cdnURL, object : BannerAdapter.OnBannerClickListener {
            // Handle banner click event
            override fun onBannerClick(mainCatid: String, catid: String, subcatid: String, productid: String) {
                scrollToProduct(mainCatid, catid, subcatid, productid)
            }
        })

        // Attach adapter and indicator to ViewPager
        binding.viewPager.adapter = bannerAdapter
        binding.dotsIndicator.setViewPager2(binding.viewPager)

        // Set ViewPager properties
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.setCurrentItem(0, false)

        // Add custom page transformer for scaling and margin effect
        val pageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(30))
            addTransformer { page, position ->
                val scaleFactor = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
                page.scaleY = scaleFactor
            }
        }
        binding.viewPager.setPageTransformer(pageTransformer)

        // Initialize and start auto-scroll for banners
        initializeAutoScrollRunnable()
        startAutoScroll()
    }

    // Function to create a runnable for auto-scrolling banners
    private fun initializeAutoScrollRunnable() {
        if (autoScrollRunnable == null) {
            autoScrollRunnable = object : Runnable {
                override fun run() {
                    // Move to next item cyclically
                    val nextItem = (binding.viewPager.currentItem + 1) % (bannerAdapter?.itemCount ?: 1)
                    binding.viewPager.setCurrentItem(nextItem, true)
                    // Post again after delay
                    handler.postDelayed(this, AUTO_SCROLL_DELAY)
                }
            }
        }
    }

    // Function to programmatically scroll to a product in nested RecyclerViews
    fun scrollToProduct(mainCatid: String, catid: String, subcatid: String, productid: String) {
        // Find index of main category matching the given ID
        val mainCatIndex = originalCategoryList.indexOfFirst {
            it.main_mcat_id.toString() == mainCatid
        }
        if (mainCatIndex == -1) return // Return if not found

        // Check if currently expanded category is the same
        val currentlyExpandedIndex = adapter?.getExpandedCategoryIndex()
        if (currentlyExpandedIndex != mainCatIndex) {
            // Collapse current and expand new after delay
            adapter?.collapseCategory()
            binding.shopMainCategoryRecycler.postDelayed({
                proceedScrollTo(mainCatIndex, catid, subcatid, productid)
            }, 350)
        } else {
            // Directly proceed if already expanded
            proceedScrollTo(mainCatIndex, catid, subcatid, productid)
        }
    }

    // Function that handles expanding categories/subcategories and scrolling to the product
    private fun proceedScrollTo(mainCatIndex: Int, catid: String, subcatid: String, productid: String) {
        // Expand the main category
        adapter?.expandCategory(mainCatIndex)
        binding.shopMainCategoryRecycler.scrollToPosition(mainCatIndex)

        binding.shopMainCategoryRecycler.postDelayed({
            // Find main category ViewHolder
            val mainCatVH = binding.shopMainCategoryRecycler
                .findViewHolderForAdapterPosition(mainCatIndex) as? ShopMainCategoryAdapter.MainCategoryViewHolder
                ?: return@postDelayed

            // Get category adapter and find index of category
            val shopCatAdapter = mainCatVH.getCategoryAdapter()
            val shopCatIndex = shopCatAdapter?.getCategoryIndex(catid) ?: -1
            if (shopCatIndex == -1) return@postDelayed

            // Expand selected category
            val categoryId = shopCatAdapter?.getCategoryIdAt(shopCatIndex)
            if (categoryId != null) {
                shopCatAdapter.expandCategory(categoryId)
            }

            mainCatVH.getCategoryRecycler().scrollToPosition(shopCatIndex)

            mainCatVH.getCategoryRecycler().postDelayed({
                // Find category ViewHolder
                val shopCatVH = mainCatVH.getCategoryRecycler()
                    .findViewHolderForAdapterPosition(shopCatIndex) as? ShopCategoryAdapter.CategoryViewHolder
                    ?: return@postDelayed

                // Get subcategory adapter and find index
                val subCatAdapter = shopCatVH.getSubCategoryAdapter()
                val subCatIndex = subCatAdapter?.getSubCategoryIndex(subcatid) ?: -1
                if (subCatIndex == -1) return@postDelayed

                // Expand subcategory and scroll to position
                subCatAdapter?.expandSubCategory(subCatIndex)
                shopCatVH.getSubCategoryRecycler().scrollToPosition(subCatIndex)

                shopCatVH.getSubCategoryRecycler().postDelayed({
                    // Try to find subcategory ViewHolder
                    val subCatVH = shopCatVH.getSubCategoryRecycler()
                        .findViewHolderForAdapterPosition(subCatIndex) as? SubCategoryAdapter.SubCategoryViewHolder
                    
                    // If not found, retry with delay
                    if (subCatVH == null) {
                        shopCatVH.getSubCategoryRecycler().postDelayed({
                            val retrySubCatVH = shopCatVH.getSubCategoryRecycler()
                                .findViewHolderForAdapterPosition(subCatIndex) as? SubCategoryAdapter.SubCategoryViewHolder
                            
                            if (retrySubCatVH == null) {
                                Log.e("TAG", "SubCategory ViewHolder not found after retry")
                                return@postDelayed
                            }
                            
                            proceedWithProductScroll(retrySubCatVH, productid, mainCatIndex, shopCatIndex, subCatIndex, mainCatVH, shopCatVH)
                        }, 200)
                        return@postDelayed
                    }
                    
                    proceedWithProductScroll(subCatVH, productid, mainCatIndex, shopCatIndex, subCatIndex, mainCatVH, shopCatVH)
                }, 600)
            }, 350)
        }, 400)
    }
    
    // Helper function to proceed with product scrolling
    private fun proceedWithProductScroll(
        subCatVH: SubCategoryAdapter.SubCategoryViewHolder,
        productid: String,
        mainCatIndex: Int,
        shopCatIndex: Int,
        subCatIndex: Int,
        mainCatVH: ShopMainCategoryAdapter.MainCategoryViewHolder,
        shopCatVH: ShopCategoryAdapter.CategoryViewHolder
    ) {

        // Find product index inside product adapter
        val productAdapter = subCatVH.getProductAdapter()
        val productIndex = productAdapter?.getProductIndex(productid) ?: -1
        if (productIndex == -1) {
            Log.e("TAG", "Product not found: productid=$productid")
            return
        }

        Log.e("TAG", "proceedScrollTo: productIndex=$productIndex, productid=$productid, totalProducts=${productAdapter?.itemCount}")
        
        // Get the product RecyclerView
        val productRecycler = subCatVH.getProductRecycler()
        val layoutManager = productRecycler.layoutManager
        
        if (layoutManager == null) {
            Log.e("TAG", "LayoutManager is null for product RecyclerView")
            return
        }
        
        // Wait for RecyclerView to be fully laid out using ViewTreeObserver
        if (productRecycler.isLaidOut && productRecycler.height > 0) {
            Log.e("TAG", "RecyclerView is laid out, scrolling immediately")
            scrollToProductInRecyclerView(
                productRecycler, 
                layoutManager, 
                productIndex,
                mainCatIndex,
                shopCatIndex,
                subCatIndex,
                mainCatVH,
                shopCatVH
            )
        } else {
            Log.e("TAG", "RecyclerView not laid out yet, waiting...")
            // Wait for RecyclerView to be laid out
            productRecycler.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (productRecycler.isLaidOut && productRecycler.height > 0) {
                        productRecycler.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        Log.e("TAG", "RecyclerView is now laid out, scrolling")
                        scrollToProductInRecyclerView(
                            productRecycler, 
                            layoutManager, 
                            productIndex,
                            mainCatIndex,
                            shopCatIndex,
                            subCatIndex,
                            mainCatVH,
                            shopCatVH
                        )
                    }
                }
            })
        }
    }

    // Helper function to scroll to product in RecyclerView
    private fun scrollToProductInRecyclerView(
        productRecycler: RecyclerView,
        layoutManager: RecyclerView.LayoutManager?,
        productIndex: Int,
        mainCatIndex: Int,
        shopCatIndex: Int,
        subCatIndex: Int,
        mainCatVH: ShopMainCategoryAdapter.MainCategoryViewHolder,
        shopCatVH: ShopCategoryAdapter.CategoryViewHolder
    ) {
        if (layoutManager == null) {
            Log.e("TAG", "LayoutManager is null")
            return
        }
        
        Log.e("TAG", "scrollToProductInRecyclerView: productIndex=$productIndex, layoutManager=${layoutManager.javaClass.simpleName}")
        
        // First, ensure all parent RecyclerViews are scrolled to correct positions
        binding.shopMainCategoryRecycler.post {
            binding.shopMainCategoryRecycler.smoothScrollToPosition(mainCatIndex)
        }
        
        mainCatVH.getCategoryRecycler().postDelayed({
            mainCatVH.getCategoryRecycler().smoothScrollToPosition(shopCatIndex)
        }, 200)
        
        shopCatVH.getSubCategoryRecycler().postDelayed({
            shopCatVH.getSubCategoryRecycler().smoothScrollToPosition(subCatIndex)
        }, 300)
        
        // Find the NestedScrollView parent and scroll it to bring the product RecyclerView into view
        productRecycler.postDelayed({
            // Use binding to get NestedScrollView
            val nestedScrollView = binding.scrollView
            
            // Scroll NestedScrollView to bring product RecyclerView into view
            productRecycler.post {
                // Get the location of the product RecyclerView relative to its parent
                val location = IntArray(2)
                productRecycler.getLocationInWindow(location)
                
                // Get NestedScrollView location
                val scrollViewLocation = IntArray(2)
                nestedScrollView.getLocationInWindow(scrollViewLocation)
                
                // Calculate scroll position (relative to NestedScrollView)
                val scrollY = location[1] - scrollViewLocation[1] - 200 // Add padding from top
                
                Log.e("TAG", "Scrolling NestedScrollView to Y=$scrollY (productRecycler Y=${location[1]}, scrollView Y=${scrollViewLocation[1]})")
                nestedScrollView.smoothScrollTo(0, maxOf(0, scrollY))
            }
            
            // Wait for RecyclerView to be fully measured and laid out, then scroll to product
            productRecycler.postDelayed({
                Log.e("TAG", "Starting scroll to productIndex=$productIndex, RecyclerView height=${productRecycler.height}, width=${productRecycler.width}")
                
                // Ensure RecyclerView is visible and has size
                if (productRecycler.height == 0 || productRecycler.width == 0) {
                    Log.e("TAG", "RecyclerView has no size, waiting more...")
                    productRecycler.postDelayed({
                        performScrollToProduct(productRecycler, layoutManager, productIndex)
                    }, 300)
                    return@postDelayed
                }
                
                performScrollToProduct(productRecycler, layoutManager, productIndex)
            }, 800) // Wait for NestedScrollView to scroll first
        }, 400)
    }
    
    // Separate function to perform the actual scroll
    private fun performScrollToProduct(
        productRecycler: RecyclerView,
        layoutManager: RecyclerView.LayoutManager,
        productIndex: Int
    ) {
        Log.e("TAG", "performScrollToProduct: productIndex=$productIndex, layoutManager=${layoutManager.javaClass.simpleName}")
        
        when (layoutManager) {
            is GridLayoutManager -> {
                Log.e("TAG", "GridLayoutManager: scrolling to productIndex=$productIndex, spanCount=${layoutManager.spanCount}, itemCount=${productRecycler.adapter?.itemCount}")
                
                // First, try direct scroll
                layoutManager.scrollToPosition(productIndex)
                
                // Then use smooth scroll
                val smoothScroller = object : LinearSmoothScroller(productRecycler.context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }
                smoothScroller.targetPosition = productIndex
                layoutManager.startSmoothScroll(smoothScroller)
                
                // Verify after smooth scroll completes
                productRecycler.postDelayed({
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    Log.e("TAG", "After smooth scroll: firstVisible=$firstVisible, lastVisible=$lastVisible, productIndex=$productIndex")
                    
                    if (productIndex < firstVisible || productIndex > lastVisible) {
                        Log.e("TAG", "Product not visible, forcing scroll again")
                        layoutManager.scrollToPosition(productIndex)
                        
                        // One more attempt
                        productRecycler.postDelayed({
                            val firstVisibleAfter = layoutManager.findFirstVisibleItemPosition()
                            val lastVisibleAfter = layoutManager.findLastVisibleItemPosition()
                            Log.e("TAG", "Final check: firstVisible=$firstVisibleAfter, lastVisible=$lastVisibleAfter")
                            if (productIndex < firstVisibleAfter || productIndex > lastVisibleAfter) {
                                Log.e("TAG", "Still not visible, using RecyclerView.scrollToPosition")
                                productRecycler.scrollToPosition(productIndex)
                            }
                        }, 300)
                    } else {
                        Log.e("TAG", "✓ Product is visible at position $productIndex")
                    }
                }, 1000) // Wait longer for smooth scroll to complete
            }
            is LinearLayoutManager -> {
                Log.e("TAG", "LinearLayoutManager: scrolling to position $productIndex")
                
                // Use LinearSmoothScroller for LinearLayoutManager
                val smoothScroller = object : LinearSmoothScroller(productRecycler.context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }
                smoothScroller.targetPosition = productIndex
                layoutManager.startSmoothScroll(smoothScroller)
                
                // Fallback verification
                productRecycler.postDelayed({
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    if (productIndex < firstVisible || productIndex > lastVisible) {
                        layoutManager.scrollToPositionWithOffset(productIndex, 0)
                    }
                }, 1000)
            }
            else -> {
                Log.e("TAG", "Unknown layout manager, using scrollToPosition")
                productRecycler.scrollToPosition(productIndex)
            }
        }
    }

    // Function to stop banner auto-scrolling
    private fun stopAutoScroll() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
    }

    // Function to start banner auto-scrolling
    private fun startAutoScroll() {
        stopAutoScroll() // Stop existing before starting again
        autoScrollRunnable?.let { handler.postDelayed(it, AUTO_SCROLL_DELAY) }
    }

    // Function to show a snackbar at the bottom with custom styling
    private fun showTopSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)

        val view = snackBar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        params.bottomMargin = 50
        view.layoutParams = params

        // Customize background color
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))

        // Customize text color and alignment
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snackBar.show()
    }

    // Function to toggle the filter overlay view
    private fun toggleFilterOverlay() {
        val isFilterVisible = binding.filterLayout.visibility == View.VISIBLE

        if (isFilterVisible) {
            // Hide filter layout and restore other views
            binding.filterLayout.visibility = View.GONE
            binding.shopMainCategoryRecycler.visibility = if (binding.noDataTextView.visibility == View.GONE) View.VISIBLE else View.GONE
            binding.shopCategoryLayout.visibility = if (binding.noDataTextView.visibility == View.GONE) View.VISIBLE else View.GONE
            showBannerView(bannerAdapter?.itemCount ?: 0 > 0)
        } else {
            // Show filter layout and hide other content
            wasContentVisibleBeforeFilter = binding.shopMainCategoryRecycler.visibility == View.VISIBLE || binding.layoutBanner.visibility == View.VISIBLE
            binding.filterLayout.visibility = View.VISIBLE
            binding.shopMainCategoryRecycler.visibility = View.GONE
            binding.shopCategoryLayout.visibility = View.GONE
            binding.layoutBanner.visibility = View.GONE
        }
    }

    // Function to show or hide "No Data" view
    private fun showNoDataView(show: Boolean) {
        binding.noDataTextView.visibility = if (show) View.VISIBLE else View.GONE
        if (binding.filterLayout.visibility == View.GONE) {
            binding.shopMainCategoryRecycler.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    // Function to show or hide banner section
    private fun showBannerView(show: Boolean) {
        binding.viewPager.visibility = if (show) View.VISIBLE else View.GONE
        if (binding.filterLayout.visibility == View.GONE) {
            binding.layoutBanner.visibility = if (show) View.VISIBLE else View.GONE
        } else {
            binding.layoutBanner.visibility = View.GONE
        }
    }

    // Function to setup Shop UI with categories
    private fun setupShopUI(categories: List<MainCategories>, cdnUrl: String) {
        if (adapter == null) {
            adapter = ShopMainCategoryAdapter(this, categories, cdnUrl)
            binding.shopMainCategoryRecycler.layoutManager = LinearLayoutManager(context)
            binding.shopMainCategoryRecycler.adapter = adapter
        } else {
            adapter?.updateCategoriesPreserveExpansion(categories)
        }
        if (binding.filterLayout.visibility == View.GONE) {
            binding.shopMainCategoryRecycler.visibility = View.VISIBLE
        }
    }

    // Callback to update wishlist from adapter
    override fun onUpdateWishlist(mvariant_id: String) {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "").orEmpty()
        this.variantId = mvariant_id
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    // Callback to update cart and observe cart DB changes
    override fun onUpdateCart(totalItems: Int, productId: Int) {
        val dao = context?.let { CartDatabase.getInstance(it).cartDao() } ?: return

        lifecycleScope.launch {
            // Collect cart items and update badge (including free items)
            launch {
                dao.getAllItems().collect { cartItems ->
                    val totalCount = calculateTotalCount(cartItems)
                    if (totalCount == 0) {
                        binding.tvCartBadge.visibility = View.GONE
                    } else {
                        binding.tvCartBadge.visibility = View.VISIBLE
                        binding.tvCartBadge.text = totalCount.toString()
                    }
                    cartUpdateListener?.onCartItemsUpdated(totalCount)
                }
            }

            // Collect cart items and calculate total
            launch {
                dao.getAllItems().collectLatest { cartItems ->
                    Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")
                    if (cartItems.isNotEmpty()) {
                        updateTotalAmount(cartItems)
                        // Check for deal applications
                        checkAndShowDealPopup(cartItems, productId)
                    } else {
                        binding.tvTotalAmount.text = "£0.00"
                    }
                }
            }
        }
    }

    // Calculate total count including free items
    private fun calculateTotalCount(cartItems: List<CartItemEntity>): Int {
        // Calculate paid quantity
        val paidQuantity = cartItems.sumOf { it.quantity }
        
        // Calculate free items for each cart item with deals
        var freeQuantity = 0
        cartItems.forEach { item ->
            if (item.dealType == "buy_x_get_y") {
                val buyQty = item.dealBuyQuantity ?: 0
                val getQty = item.dealGetQuantity ?: 0
                if (buyQty > 0 && getQty > 0 && item.quantity >= buyQty) {
                    freeQuantity += (item.quantity / buyQty) * getQty
                }
            }
        }
        
        return paidQuantity + freeQuantity
    }

    // Function to calculate and update total cart amount
    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalAmount = cartItems.sumOf { item ->
            calculateItemPrice(item)
        }
        binding.tvTotalAmount.text = "£%.2f".format(totalAmount)
    }
    
    // Calculate item price considering volume discounts
    private fun calculateItemPrice(item: CartItemEntity): Double {
        return when (item.dealType) {
            "volume_discount" -> {
                val dealQty = item.dealQuantity ?: 0
                val dealPrice = item.dealPrice ?: 0.0
                
                if (dealQty > 0 && dealPrice > 0 && item.quantity >= dealQty) {
                    // Calculate complete deal sets
                    val completeSets = item.quantity / dealQty
                    // Calculate remaining items after complete sets
                    val remainingItems = item.quantity % dealQty
                    // Total = (complete sets * deal price) + (remaining items * item price)
                    (completeSets * dealPrice) + (remainingItems * item.price)
                } else {
                    // No deal applied, use regular pricing
                    item.price * item.quantity
                }
            }
            else -> {
                // For buy_x_get_y or no deal, use regular pricing
                item.price * item.quantity
            }
        }
    }
    
    // Check if any deals are triggered and show popup
    private fun checkAndShowDealPopup(cartItems: List<CartItemEntity>, updatedProductId: Int) {
        // Find the product that was just updated
        val updatedProduct = findProductByVariantId(updatedProductId) ?: return
        
        // Check if this product has a deal
        if (updatedProduct.deal_type.isNullOrEmpty()) return
        
        // Find the cart item for this product
        val cartItem = cartItems.find { it.variantId == updatedProductId } ?: return
        
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
    
    // Find product by variant ID in the current category list
    private fun findProductByVariantId(variantId: Int): Product? {
        originalCategoryList.forEach { mainCategory ->
            mainCategory.categories.forEach { category ->
                category.subcategories.forEach { subcategory ->
                    subcategory.products.forEach { product ->
                        if (product.mvariant_id == variantId) {
                            return product
                        }
                    }
                }
            }
        }
        return null
    }
    
    // Get the deal threshold (minimum quantity needed to trigger deal)
    private fun getDealThreshold(product: Product): Int {
        return when (product.deal_type) {
            "buy_x_get_y" -> product.deal_buy_quantity ?: 0
            "volume_discount" -> product.deal_quantity ?: 0
            else -> 0
        }
    }
    
    // Calculate if deal is triggered and how many free items
    private fun calculateDealApplication(product: Product, quantity: Int): DealResult {
        return when (product.deal_type) {
            "buy_x_get_y" -> {
                val buyQty = product.deal_buy_quantity ?: 0
                val getQty = product.deal_get_quantity ?: 0
                
                if (buyQty > 0 && getQty > 0 && quantity >= buyQty) {
                    val freeItems = (quantity / buyQty) * getQty
                    val dealName = "Buy $buyQty Get $getQty"
                    DealResult(true, freeItems, dealName)
                } else {
                    DealResult(false, 0, "")
                }
            }
            "volume_discount" -> {
                val dealQty = product.deal_quantity ?: 0
                val dealPrice = product.deal_price ?: 0.0
                
                if (dealQty > 0 && dealPrice > 0 && quantity >= dealQty) {
                    val dealName = "Buy $dealQty for £%.2f".format(dealPrice)
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

    // Reset fragment to default state (show all categories)
    private fun resetToDefaultState() {
        // Only reset if filters are currently applied
        if (applyFilter) {
            // Reset filter variables
            applyFilter = false
            filters = ""
            filtersType = "All"
            search = ""
            
            // Reset UI elements
            binding.searchInput.text?.clear()
            binding.radioAllProducts.isChecked = true
            
            // Load all categories without filters
            categoriesViewModel.categories("", token, "")
        }
        
        // Clear deal tracking when resetting
        shownDeals.clear()
    }

    // Lifecycle callback to clean up when view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll() // Stop banner auto-scrolling
        handler.removeCallbacksAndMessages(null) // Remove all handler messages
        shownDeals.clear() // Clear deal tracking
    }
}