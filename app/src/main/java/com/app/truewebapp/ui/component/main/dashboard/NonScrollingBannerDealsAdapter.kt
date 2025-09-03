// Import required classes for haptic feedback, view inflation, and RecyclerView
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// Import the DealSlider data model (for deal banner details)
import com.app.truewebapp.data.dto.dashboard_banners.DealSlider
// Import ViewBinding for the deal banner layout
import com.app.truewebapp.databinding.ItemBannerDealBinding
// Import listener interface to handle deal banner click events
import com.app.truewebapp.ui.component.main.dashboard.DealsBannerListener
// Import Glide for efficient image loading and caching
import com.bumptech.glide.Glide

/**
 * NonScrollingBannerDealsAdapter is a RecyclerView adapter that displays
 * a list of deal banners in a non-scrolling horizontal/vertical layout.
 *
 * @param listener Listener to handle banner click events
 * @param banners List of DealSlider banners to display
 * @param cdnURL Base URL for loading banner images
 */
class NonScrollingBannerDealsAdapter(
    private val listener: DealsBannerListener,   // Listener for click interactions
    private val banners: List<DealSlider>,       // List of deal banners to display
    private val cdnURL: String                   // CDN base URL for images
) : RecyclerView.Adapter<NonScrollingBannerDealsAdapter.BannerViewHolder>() {

    /**
     * ViewHolder class that binds banner data to the UI.
     *
     * @param binding Binding object for the deal banner item layout
     */
    inner class BannerViewHolder(private val binding: ItemBannerDealBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds a deal banner image and sets up click listeners.
         *
         * @param imageRes Relative image path for the banner
         * @param position Position of the banner in the list
         */
        fun bind(imageRes: String, position: Int) {
            // Load the banner image using Glide
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes)  // Concatenate base URL with image path
                .thumbnail(0.1f)          // Show low-resolution preview first
                .dontAnimate()            // Disable animations for faster loading
                .into(binding.imageView)  // Load into the ImageView

            // Set click listener for the banner image
            binding.imageView.setOnClickListener {
                // Provide haptic feedback (vibration) when tapped
                binding.imageView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,              // Feedback type
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to override system setting
                )

                // Notify listener with IDs (category, subcategory, product) when banner is clicked
                listener.onUpdateDealsBanner(
                    banners[position].main_mcat_id.toString(),
                    banners[position].mcat_id.toString(),
                    banners[position].msubcat_id.toString(),
                    banners[position].mproduct_id.toString()
                )
            }
        }
    }

    /**
     * Inflates the layout and creates a new ViewHolder.
     *
     * @param parent Parent ViewGroup
     * @param viewType Type of view (only one type used here)
     * @return A new BannerViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        // Inflate the deal banner layout using ViewBinding
        val binding = ItemBannerDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding) // Return the new ViewHolder
    }

    /**
     * Binds data to the ViewHolder at the given position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // Bind the deal banner image to the ViewHolder
        holder.bind(banners[position].home_explore_deal_banner_image, position)
    }

    /**
     * Returns the total number of deal banners in the list.
     *
     * @return Number of banners
     */
    override fun getItemCount(): Int = banners.size
}