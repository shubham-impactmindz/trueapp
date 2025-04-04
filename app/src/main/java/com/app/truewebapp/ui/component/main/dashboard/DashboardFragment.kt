package com.app.truewebapp.ui.component.main.dashboard

import BannerAdapter
import NonScrollingBannerAdapter
import NonScrollingBannerDealsAdapter
import NonScrollingBannerDrinksAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.app.truewebapp.R
import com.app.truewebapp.databinding.FragmentDashboardBinding
import com.app.truewebapp.ui.component.main.account.WalletActivity
import com.app.truewebapp.utils.JsonUtils


class DashboardFragment : Fragment() {

    lateinit var binding: FragmentDashboardBinding
    private lateinit var notificationsAdapter: NotificationsAdapter
    private var tabSwitcher: TabSwitcher? = null
    private var bannerAdapter: BannerAdapter? = null
    private var roundImageAdapter: RoundImageAdapter? = null
    private var nonScrollingBannerAdapter: NonScrollingBannerAdapter? = null
    private var nonScrollingBannerDealsAdapter: NonScrollingBannerDealsAdapter? = null
    private var nonScrollingBannerDrinksAdapter: NonScrollingBannerDrinksAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private val AUTO_SCROLL_DELAY: Long = 3000 // Auto scroll every 3 seconds


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

        setupNotifications()
        setUpBannerAdapter()
        setUpRoundImageAdapter()
        setUpNonScrollingBannerAdapter()
        setUpNonScrollingBannerDealsAdapter()
        setUpNonScrollingBannerDrinksAdapter()
        setUpNonScrollingBannerFruitsAdapter()
        setUpNonScrollingBannerNutritionAdapter()
    }

    private fun setUpRoundImageAdapter() {
        val images = listOf(
            R.drawable.image1, R.drawable.image2,
            R.drawable.image3, R.drawable.image4,
            R.drawable.image5, R.drawable.image6,
            R.drawable.image7, R.drawable.image8,
            R.drawable.image9, R.drawable.image10,
            R.drawable.image11, R.drawable.image12,

        )

//        val brands = listOf(
//            "Hayati", "Oxva", "Oreo", "Fanta", "Lost Mary", "Coca Cola", "Dr Pepper", "Jolly Rancher", "Ske Crystal", "IVG",
//            "Crystal Bling", "Crystal Prime"
//
//        )

        val adapter = RoundImageAdapter(images)

        binding.rvRoundImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRoundImages.adapter = adapter

    }

    private fun setUpBannerAdapter() {

        // List of banner images
        val banners: MutableList<Int> = ArrayList()
        banners.add(R.drawable.banner1)
        banners.add(R.drawable.banner2)
        banners.add(R.drawable.banner3)
        banners.add(R.drawable.banner4)
        banners.add(R.drawable.banner5)
        banners.add(R.drawable.banner6)
        banners.add(R.drawable.banner7)
        banners.add(R.drawable.banner8)

        // Setup ViewPager with adapter
        bannerAdapter = BannerAdapter(banners)
        binding.viewPager.adapter = bannerAdapter
        binding.dotsIndicator.attachTo(binding.viewPager)


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

        // Setup ViewPager with adapter
        nonScrollingBannerAdapter = NonScrollingBannerAdapter(banners)
        binding.viewPagerNonScrolling.adapter = nonScrollingBannerAdapter


        // Set up smooth scrolling without large jumps
        binding.viewPagerNonScrolling.offscreenPageLimit = 1
        binding.viewPagerNonScrolling.setCurrentItem(0, false)

    }
    private fun setUpNonScrollingBannerDealsAdapter() {

        // List of banner images
        val banners: MutableList<Int> = ArrayList()
        banners.add(R.drawable.deal1)
        banners.add(R.drawable.deal2)
        banners.add(R.drawable.deal3)
        banners.add(R.drawable.deal4)
        banners.add(R.drawable.deal5)
        banners.add(R.drawable.deal6)

        binding.recyclerViewDeals.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Setup ViewPager with adapter
        nonScrollingBannerDealsAdapter = NonScrollingBannerDealsAdapter(banners)
        binding.recyclerViewDeals.adapter = nonScrollingBannerDealsAdapter

    }
    private fun setUpNonScrollingBannerDrinksAdapter() {

        val categoryList = JsonUtils.loadCategoriesFromAsset(requireContext())

        binding.recyclerViewDrinks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Setup ViewPager with adapter
        if (categoryList != null) {
            nonScrollingBannerDrinksAdapter = NonScrollingBannerDrinksAdapter(categoryList.categories?.get(4)?.subCats?.get(0)?.products,
                categoryList.categories?.get(4)?.subCats?.get(0)?.title)
        }
        binding.recyclerViewDrinks.adapter = nonScrollingBannerDrinksAdapter

    }

    private fun setUpNonScrollingBannerNutritionAdapter() {

        val categoryList = JsonUtils.loadCategoriesFromAsset(requireContext())

        binding.recyclerViewNutrition.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Setup ViewPager with adapter
        if (categoryList != null) {
            nonScrollingBannerDrinksAdapter = NonScrollingBannerDrinksAdapter(categoryList.categories?.get(3)?.subCats?.get(0)?.products,
                categoryList.categories?.get(3)?.subCats?.get(0)?.title)
        }
        binding.recyclerViewNutrition.adapter = nonScrollingBannerDrinksAdapter

    }
    private fun setUpNonScrollingBannerFruitsAdapter() {

        // List of banner images
        val banners: MutableList<Int> = ArrayList()
        banners.add(R.drawable.fruits1)
        banners.add(R.drawable.fruits2)
        banners.add(R.drawable.fruits3)
        banners.add(R.drawable.fruits4)
        banners.add(R.drawable.fruits5)
        banners.add(R.drawable.fruits6)

        binding.recyclerViewFruits.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Setup ViewPager with adapter
        nonScrollingBannerDealsAdapter = NonScrollingBannerDealsAdapter(banners)
        binding.recyclerViewFruits.adapter = nonScrollingBannerDealsAdapter

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
        }

        binding.favLayout.setOnClickListener {
            tabSwitcher?.switchToShopTab()
        }

        binding.creditLayout.setOnClickListener {
            val intent = Intent(context, WalletActivity::class.java)
            startActivity(intent)
        }

        binding.referralSection.setOnClickListener {
            val intent = Intent(context, ReferralActivity::class.java)
            startActivity(intent)
        }
    }
}