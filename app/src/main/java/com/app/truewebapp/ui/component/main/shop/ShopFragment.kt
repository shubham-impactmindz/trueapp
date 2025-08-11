package com.app.truewebapp.ui.component.main.shop

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
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.app.truewebapp.R
import com.app.truewebapp.SHARED_PREF_NAME
import com.app.truewebapp.data.dto.browse.BrowseBanners
import com.app.truewebapp.data.dto.browse.MainCategories
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

class ShopFragment : Fragment(), ProductAdapterListener {

    private lateinit var binding: FragmentShopBinding
    lateinit var categoriesViewModel: CategoriesViewModel
    private lateinit var bannersViewModel: BannersViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var brandsViewModel: BrandsViewModel
    private var adapter: ShopMainCategoryAdapter? = null
    private var bannerAdapter: BannerAdapter? = null
    private var tabSwitcher: TabSwitcher? = null
    private var cartUpdateListener: CartUpdateListener? = null
    private var originalCategoryList: List<MainCategories> = listOf()
    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private val AUTO_SCROLL_DELAY: Long = 3000
    private var search = ""
    private var token = ""
    private var variantId = ""
    var filters = ""
    var filtersType = "All"
    var applyFilter = false
    var cdnUrl = ""

    // Add a flag to track the visibility state of content when filter is shown
    private var wasContentVisibleBeforeFilter = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cartDao = CartDatabase.getInstance(requireContext()).cartDao()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            // Collect cart count in a separate coroutine
            launch {
                cartDao.getCartItemCount().collect { totalCount ->
                    val actualCount = totalCount ?: 0

                    if (actualCount == 0) {
                        binding.tvCartBadge.visibility = View.GONE
                    } else {
                        binding.tvCartBadge.visibility = View.VISIBLE
                        binding.tvCartBadge.text = actualCount.toString()
                    }
                }
            }

            // Collect all cart items in another coroutine
            launch {
                cartDao.getAllItems().collectLatest { cartItems ->
                    Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")

                    if (cartItems.isNotEmpty()) {
                        updateTotalAmount(cartItems) // Update total price
                    } else {
                        binding.tvTotalAmount.text = "£0.00" // Reset total price for empty cart
                    }
                }
            }
        }

        initializeViewModels()
        setupViews()
        setupObservers()
        loadInitialData()
    }

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

    override fun onDetach() {
        super.onDetach()
        tabSwitcher = null
        cartUpdateListener = null
    }

    private fun initializeViewModels() {
        categoriesViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        bannersViewModel = ViewModelProvider(this)[BannersViewModel::class.java]
        wishlistViewModel = ViewModelProvider(this)[WishlistViewModel::class.java]
        brandsViewModel = ViewModelProvider(this)[BrandsViewModel::class.java]

        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        token = "Bearer " + preferences?.getString("token", "").orEmpty()
    }

    private fun setupViews() {
        binding.filterButton.setOnClickListener {
            binding.filterButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            toggleFilterOverlay()
        }
        binding.cart.setOnClickListener {
            binding.cart.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            tabSwitcher?.switchToCartTab()
        }
        binding.cancelLayout.setOnClickListener {
            binding.cancelLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            toggleFilterOverlay()
            applyFilter = false
        }

        binding.applyLayout.setOnClickListener {
            binding.applyLayout.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
            )
            val selectedIds = brandsViewModel.brandsResponse.value?.mbrands
                ?.filter { it.isSelected }
                ?.map { it.mbrand_id }
                ?.joinToString(",").orEmpty()

            filters = if (filtersType == "Favourites") selectedIds else ""
            applyFilter = true
            categoriesViewModel.categories(search, token, filters)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            stopAutoScroll()
            applyFilter = false
            loadInitialData()
        }

        setupSearchView()
        setupRadioGroup()
    }

    override fun onResume() {
        super.onResume()

        adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll() // Stop auto-scrolling when the fragment is paused
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchView() {
        val closeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
        val searchIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                search = s.toString().trim()
                binding.searchInput.setCompoundDrawablesWithIntrinsicBounds(
                    if (search.isEmpty()) searchIcon else null,
                    null,
                    if (search.isNotEmpty()) closeIcon else null,
                    null
                )
                categoriesViewModel.categories(search, token, filters)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.searchInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.searchInput.compoundDrawables[2]
                drawableEnd?.let {
                    val drawableWidth = it.bounds.width()
                    val touchAreaStart = binding.searchInput.width - binding.searchInput.paddingEnd - drawableWidth
                    if (event.x > touchAreaStart) {
                        binding.searchInput.text?.clear()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun setupRadioGroup() {
        binding.allRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            filtersType = when (checkedId) {
                R.id.radioAllProducts -> "All"
                R.id.radioFavourites -> "Favourites"
                else -> "All"
            }

            val response = brandsViewModel.brandsResponse.value ?: return@setOnCheckedChangeListener

            if (filtersType == "Favourites") {
                val wishlistIds = response.wishlistbrand.map { it.mbrand_id }
                response.mbrands.forEach { brand ->
                    brand.isSelected = wishlistIds.contains(brand.mbrand_id)
                }
            } else {
                response.mbrands.forEach { brand ->
                    brand.isSelected = false
                }
            }

            val brandsAdapter = BrandsAdapter(
                response.mbrands,
                response.cdnURL,
                filtersType,
                response
            )
            binding.brandsRecyclerView.layoutManager = GridLayoutManager(context, 5)
            binding.brandsRecyclerView.adapter = brandsAdapter
        }
    }

    private fun loadInitialData() {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        categoriesViewModel.categories(search, token, filters)
        bannersViewModel.banners(token)
        brandsViewModel.brands(token, preferences?.getString("userId", ""))
    }

    private fun setupObservers() {
        observeCategories()
        observeBanners()
        observeWishlist()
        observeBrands()
//        observeCart()
    }

    private fun observeCategories() {
        categoriesViewModel.categoriesModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    binding.shimmerLayoutMainCategory.visibility = View.GONE
                    if (applyFilter) toggleFilterOverlay()

                    if (it.main_categories.isEmpty()) {
                        showNoDataView(true)
                    } else {
                        cdnUrl = it.cdnURL
                        binding.shimmerLayoutMainCategory.visibility = View.GONE
                        showNoDataView(false)
                        originalCategoryList = it.main_categories
                        setupShopUI(originalCategoryList, it.cdnURL)
                    }
                }
            }
        }

        categoriesViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it == true) {
                binding.shimmerLayoutMainCategory.visibility = View.VISIBLE
                binding.shopMainCategoryRecycler.visibility = View.GONE
            }
        }

        categoriesViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutMainCategory.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        categoriesViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutMainCategory.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeBanners() {
        bannersViewModel.bannersModel.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    binding.shimmerLayoutBanner.visibility = View.GONE
                    val banners = it.browseBanners

                    if (banners.isEmpty()) {
                        showBannerView(false)
                    } else {
                        binding.shimmerLayoutBanner.visibility = View.GONE
                        binding.viewPager.visibility = View.VISIBLE
                        showBannerView(true)
                        setUpNonScrollingBannerAdapter(banners, it.cdnURL)
                    }
                } else {
                    showBannerView(false)
                }
            }
        }

        bannersViewModel.isLoading.observe(viewLifecycleOwner) {
            // Show shimmer while loading, hide ViewPager
            if (it == true) {
                binding.shimmerLayoutBanner.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
            }
        }

        bannersViewModel.apiError.observe(viewLifecycleOwner) {
            binding.shimmerLayoutBanner.visibility = View.GONE
            showTopSnackBar(it ?: "Unexpected API error")
        }

        bannersViewModel.onFailure.observe(viewLifecycleOwner) {
            binding.shimmerLayoutBanner.visibility = View.GONE
            showTopSnackBar(ApiFailureTypes().getFailureMessage(it, context))
        }
    }

    private fun observeWishlist() {
        wishlistViewModel.wishlistResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
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

    private fun observeBrands() {
        brandsViewModel.brandsResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.swipeRefreshLayout.isRefreshing = false
                if (it.status) {
                    if (it.mbrands.isNotEmpty()) {
                        binding.brandsRecyclerView.visibility = View.VISIBLE
                        val brandsAdapter = BrandsAdapter(
                            it.mbrands,
                            it.cdnURL,
                            filtersType,
                            it
                        )
                        binding.brandsRecyclerView.layoutManager = GridLayoutManager(context, 5)
                        binding.brandsRecyclerView.adapter = brandsAdapter
                    } else {
                        binding.brandsRecyclerView.visibility = View.GONE
                    }
                } else {
                    binding.brandsRecyclerView.visibility = View.GONE
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

    private fun updateWishlistInAdapter(variantId: String) {
        originalCategoryList.forEachIndexed { mainCatIndex, mainCategory ->
            mainCategory.categories.forEachIndexed { catIndex, category ->
                category.subcategories.forEachIndexed { subCatIndex, subCategory ->
                    subCategory.products.forEachIndexed { prodIndex, product ->
                        if (product.mvariant_id.toString() == variantId) {
                            product.user_info_wishlist = !product.user_info_wishlist

                            (binding.shopMainCategoryRecycler.findViewHolderForAdapterPosition(mainCatIndex) as? ShopMainCategoryAdapter.MainCategoryViewHolder)?.let { mainCatViewHolder ->
                                (mainCatViewHolder.getCategoryRecycler().findViewHolderForAdapterPosition(catIndex) as? ShopCategoryAdapter.CategoryViewHolder)?.let { categoryViewHolder ->
                                    (categoryViewHolder.getSubCategoryRecycler().findViewHolderForAdapterPosition(subCatIndex) as? SubCategoryAdapter.SubCategoryViewHolder)?.let { subCatViewHolder ->
                                        subCatViewHolder.getProductAdapter()?.notifyItemChanged(prodIndex)
                                    }
                                }
                            }
                            return@forEachIndexed
                        }
                    }
                }
            }
        }
    }

    private fun setUpNonScrollingBannerAdapter(browseBanners: List<BrowseBanners>, cdnURL: String) {
        stopAutoScroll()

        bannerAdapter = BannerAdapter(browseBanners, cdnURL, object : BannerAdapter.OnBannerClickListener {
            override fun onBannerClick(mainCatid: String, catid: String, subcatid: String, productid: String) {
                scrollToProduct(mainCatid,catid, subcatid, productid)
            }
        })

        binding.viewPager.adapter = bannerAdapter
        binding.dotsIndicator.setViewPager2(binding.viewPager)

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

    fun scrollToProduct(mainCatid: String, catid: String, subcatid: String, productid: String) {
        val mainCatIndex = originalCategoryList.indexOfFirst {
            it.main_mcat_id.toString() == mainCatid
        }
        if (mainCatIndex == -1) return

        val currentlyExpandedIndex = adapter?.getExpandedCategoryIndex()
        if (currentlyExpandedIndex != mainCatIndex) {
            adapter?.collapseCategory()
            binding.shopMainCategoryRecycler.postDelayed({
                proceedScrollTo(mainCatIndex, catid, subcatid, productid)
            }, 350)
        } else {
            proceedScrollTo(mainCatIndex, catid, subcatid, productid)
        }
    }

    private fun proceedScrollTo(mainCatIndex: Int, catid: String, subcatid: String, productid: String) {
        adapter?.expandCategory(mainCatIndex)
        binding.shopMainCategoryRecycler.scrollToPosition(mainCatIndex)

        binding.shopMainCategoryRecycler.postDelayed({
            val mainCatVH = binding.shopMainCategoryRecycler
                .findViewHolderForAdapterPosition(mainCatIndex) as? ShopMainCategoryAdapter.MainCategoryViewHolder
                ?: return@postDelayed

            val shopCatAdapter = mainCatVH.getCategoryAdapter()
            val shopCatIndex = shopCatAdapter?.getCategoryIndex(catid) ?: -1
            if (shopCatIndex == -1) return@postDelayed

            val categoryId = shopCatAdapter?.getCategoryIdAt(shopCatIndex)
            if (categoryId != null) {
                shopCatAdapter.expandCategory(categoryId)
            }

            mainCatVH.getCategoryRecycler().scrollToPosition(shopCatIndex)

            mainCatVH.getCategoryRecycler().postDelayed({
                val shopCatVH = mainCatVH.getCategoryRecycler()
                    .findViewHolderForAdapterPosition(shopCatIndex) as? ShopCategoryAdapter.CategoryViewHolder
                    ?: return@postDelayed

                val subCatAdapter = shopCatVH.getSubCategoryAdapter()
                val subCatIndex = subCatAdapter?.getSubCategoryIndex(subcatid) ?: -1
                if (subCatIndex == -1) return@postDelayed

                subCatAdapter?.expandSubCategory(subCatIndex)
                shopCatVH.getSubCategoryRecycler().scrollToPosition(subCatIndex)

                shopCatVH.getSubCategoryRecycler().postDelayed({
                    val subCatVH = shopCatVH.getSubCategoryRecycler()
                        .findViewHolderForAdapterPosition(subCatIndex) as? SubCategoryAdapter.SubCategoryViewHolder
                        ?: return@postDelayed

                    val productAdapter = subCatVH.getProductAdapter()
                    val productIndex = productAdapter?.getProductIndex(productid) ?: -1
                    if (productIndex == -1) return@postDelayed

                    Log.e("TAG", "proceedScrollTo: "+productIndex)
                    subCatVH.getProductRecycler().post {
                        (subCatVH.getProductRecycler().layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(productIndex, 100)
                    }
                }, 300)
            }, 350)
        }, 400)
    }



    private fun stopAutoScroll() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startAutoScroll() {
        stopAutoScroll()
        autoScrollRunnable?.let { handler.postDelayed(it, AUTO_SCROLL_DELAY) }
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

    private fun toggleFilterOverlay() {
        val isFilterVisible = binding.filterLayout.visibility == View.VISIBLE

        if (isFilterVisible) {
            // Filter is currently visible, so hide it
            binding.filterLayout.visibility = View.GONE
            // Restore visibility of other views based on their state before the filter was shown
            binding.shopMainCategoryRecycler.visibility = if (binding.noDataTextView.visibility == View.GONE) View.VISIBLE else View.GONE
            binding.shopCategoryLayout.visibility = if (binding.noDataTextView.visibility == View.GONE) View.VISIBLE else View.GONE
            // Restore banner visibility based on its own data/loading state, not just a fixed value
            // This is handled better by observeBanners() and showBannerView()
            showBannerView(bannerAdapter?.itemCount ?: 0 > 0) // Re-evaluate banner visibility
        } else {
            // Filter is currently hidden, so show it
            // Before showing the filter, store the current visibility of content views
            wasContentVisibleBeforeFilter = binding.shopMainCategoryRecycler.visibility == View.VISIBLE || binding.layoutBanner.visibility == View.VISIBLE

            binding.filterLayout.visibility = View.VISIBLE
            // Hide the content views when the filter is active
            binding.shopMainCategoryRecycler.visibility = View.GONE
            binding.shopCategoryLayout.visibility = View.GONE
            binding.layoutBanner.visibility = View.GONE
        }
    }


    private fun showNoDataView(show: Boolean) {
        binding.noDataTextView.visibility = if (show) View.VISIBLE else View.GONE
        // Ensure main category recycler is hidden if no data, and visible otherwise,
        // unless the filter is active
        if (binding.filterLayout.visibility == View.GONE) {
            binding.shopMainCategoryRecycler.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun showBannerView(show: Boolean) {
        binding.viewPager.visibility = if (show) View.VISIBLE else View.GONE
        // Ensure layoutBanner visibility is tied to its content,
        // but only if the filter is not currently active.
        if (binding.filterLayout.visibility == View.GONE) {
            binding.layoutBanner.visibility = if (show) View.VISIBLE else View.GONE
        } else {
            binding.layoutBanner.visibility = View.GONE // Keep hidden if filter is active
        }
    }


    private fun setupShopUI(categories: List<MainCategories>, cdnUrl: String) {
        if (adapter == null) {
            adapter = ShopMainCategoryAdapter(this, categories, cdnUrl)
            binding.shopMainCategoryRecycler.layoutManager = LinearLayoutManager(context)
            binding.shopMainCategoryRecycler.adapter = adapter
        } else {
            adapter?.updateCategoriesPreserveExpansion(categories)
        }
        // Ensure visibility is correct after setting up UI, unless filter is active
        if (binding.filterLayout.visibility == View.GONE) {
            binding.shopMainCategoryRecycler.visibility = View.VISIBLE
        }
    }

    override fun onUpdateWishlist(mvariant_id: String) {
        val preferences = context?.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        val userId = preferences?.getString("userId", "").orEmpty()
        this.variantId = mvariant_id
        wishlistViewModel.wishlist(token, WishlistRequest(userId, mvariant_id))
    }

    override fun onUpdateCart(totalItems: Int, productId: Int) {
        val dao = context?.let { CartDatabase.getInstance(it).cartDao() } ?: return

        lifecycleScope.launch {
            launch {
                dao.getCartItemCount().collect { totalCount ->
                    val actualCount = totalCount ?: 0

                    if (actualCount == 0) {
                        binding.tvCartBadge.visibility = View.GONE
                    } else {
                        binding.tvCartBadge.visibility = View.VISIBLE
                        binding.tvCartBadge.text = actualCount.toString()
                    }
                    cartUpdateListener?.onCartItemsUpdated(actualCount)
                }
            }

            launch {
                dao.getAllItems().collectLatest { cartItems ->
                    Log.d("CartFragment", "Received ${cartItems.size} items from database Flow.")

                    if (cartItems.isNotEmpty()) {
                        updateTotalAmount(cartItems)
                    } else {
                        binding.tvTotalAmount.text = "£0.00"
                    }
                }
            }
        }
    }

    private fun updateTotalAmount(cartItems: List<CartItemEntity>) {
        val totalAmount = cartItems.sumOf { it.price * it.quantity }
        binding.tvTotalAmount.text = "£%.2f".format(totalAmount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        handler.removeCallbacksAndMessages(null)
    }
}