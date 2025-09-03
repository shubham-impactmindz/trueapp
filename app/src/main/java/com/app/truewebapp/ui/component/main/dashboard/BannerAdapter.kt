// Import required Android libraries
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.data.dto.browse.BrowseBanners
import com.app.truewebapp.databinding.ItemBannerBinding
import com.bumptech.glide.Glide

/**
 * BannerAdapter is a RecyclerView Adapter used to display banners
 * in a horizontal or vertical list.
 *
 * @param banners List of banner items to display
 * @param cdnURL Base URL for loading banner images
 * @param listener Callback interface for handling banner clicks
 */
class BannerAdapter(
    private val banners: List<BrowseBanners>,   // List of banner data objects
    private val cdnURL: String,                 // Base CDN URL to fetch images
    private val listener: OnBannerClickListener // Listener for banner click events
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    /**
     * Interface for handling banner click events.
     * Provides IDs to help identify what was clicked.
     */
    interface OnBannerClickListener {
        fun onBannerClick(mainCatid: String, catid: String, subcatid: String, productid: String)
    }

    /**
     * ViewHolder class for individual banner item.
     * Holds reference to the layout (ItemBannerBinding) and binds data to UI.
     */
    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds banner data to UI components.
         *
         * @param banner BrowseBanners object containing banner details
         */
        fun bind(banner: BrowseBanners) {
            // Load banner image using Glide
            Glide.with(binding.root.context)
                .load(cdnURL + banner.browsebanner_image) // Concatenate CDN URL with image path
                .thumbnail(0.1f)                         // Load a small thumbnail preview first
                .dontAnimate()                           // Disable animation for faster load
                .into(binding.imageView)                 // Load into ImageView in layout

            // Set click listener on banner image
            binding.imageView.setOnClickListener {
                // Provide haptic feedback when user clicks
                binding.imageView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore global haptic settings
                )

                // Notify listener with IDs related to this banner
                listener.onBannerClick(
                    banner.main_mcat_id.toString(),
                    banner.mcat_id.toString(),
                    banner.msubcat_id.toString(),
                    banner.mproduct_id.toString()
                )
            }
        }
    }

    /**
     * Inflates the banner item layout and creates a ViewHolder.
     *
     * @param parent Parent ViewGroup
     * @param viewType Type of view (not used here since all are same type)
     * @return A new BannerViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        // Inflate layout using ViewBinding
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    /**
     * Binds banner data to ViewHolder at the given position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position Position of the banner item in the list
     */
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position]) // Pass banner data to bind() method
    }

    /**
     * Returns total number of banner items.
     *
     * @return Number of banners in the list
     */
    override fun getItemCount(): Int = banners.size
}