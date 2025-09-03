// Import required classes for haptic feedback, view inflation, and RecyclerView
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// Import the SmallSlider data model (for small banner details)
import com.app.truewebapp.data.dto.dashboard_banners.SmallSlider
// Import ViewBinding for banner item layout
import com.app.truewebapp.databinding.ItemBannerBinding
// Import listener interface to handle banner click events
import com.app.truewebapp.ui.component.main.dashboard.SmallBannerListener
// Import Glide for image loading
import com.bumptech.glide.Glide

/**
 * NonScrollingBannerAdapter is a RecyclerView adapter that displays
 * a list of small banners without enabling horizontal scrolling.
 *
 * @param listener Listener to handle banner click events
 * @param banners List of SmallSlider banners to display
 * @param cdnURL Base URL for loading banner images
 */
class NonScrollingBannerAdapter(
    private val listener: SmallBannerListener,     // Listener for click interactions
    private val banners: List<SmallSlider>,        // List of banners to display
    private val cdnURL: String                     // CDN base URL for images
) : RecyclerView.Adapter<NonScrollingBannerAdapter.BannerViewHolder>() {

    /**
     * ViewHolder class that binds banner data to the UI.
     *
     * @param binding Binding object for the banner item layout
     */
    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds a banner image and sets up click listeners.
         *
         * @param imageRes Image path (relative to cdnURL) for the banner
         * @param position Position of the banner in the list
         */
        fun bind(imageRes: String, position: Int) {
            // Load banner image using Glide
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes)   // Concatenate base URL with image path
                .thumbnail(0.1f)           // Show low-resolution preview first
                .dontAnimate()             // Disable animation for faster loading
                .into(binding.imageView)   // Load into the ImageView

            // Set click listener for the banner image
            binding.imageView.setOnClickListener {
                // Provide haptic feedback on touch (vibration effect)
                binding.imageView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,              // Feedback type
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag to override settings
                )

                // Notify listener with banner IDs when clicked
                listener.onUpdateSmallBanner(
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
     * @param viewType Type of view (not used here since all items are same type)
     * @return A new BannerViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        // Inflate the banner layout using ViewBinding
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding) // Return new ViewHolder
    }

    /**
     * Binds data to the ViewHolder at the given position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // Bind the banner image to the ViewHolder
        holder.bind(banners[position].home_small_banner_image, position)
    }

    /**
     * Returns the total number of banners in the list.
     *
     * @return Number of items
     */
    override fun getItemCount(): Int = banners.size
}
