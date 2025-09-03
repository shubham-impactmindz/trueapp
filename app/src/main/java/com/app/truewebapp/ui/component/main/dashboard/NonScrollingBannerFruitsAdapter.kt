// Import for haptic feedback (vibration effect when user taps)
import android.view.HapticFeedbackConstants
// Import to inflate XML layout files into View objects
import android.view.LayoutInflater
import android.view.ViewGroup
// Import RecyclerView base classes
import androidx.recyclerview.widget.RecyclerView
// Import FruitSlider data model (contains banner image + IDs)
import com.app.truewebapp.data.dto.dashboard_banners.FruitSlider
// Import ViewBinding for banner item layout
import com.app.truewebapp.databinding.ItemBannerDealBinding
// Import listener interface to handle fruit banner clicks
import com.app.truewebapp.ui.component.main.dashboard.FruitsBannerListener
// Import Glide for image loading
import com.bumptech.glide.Glide

/**
 * RecyclerView Adapter for displaying non-scrolling fruit banners.
 *
 * @param listener Callback listener for banner click events.
 * @param banners List of [FruitSlider] objects containing banner data.
 * @param cdnURL Base CDN URL to prepend to image paths.
 */
class NonScrollingBannerFruitsAdapter(
    private val listener: FruitsBannerListener, // Handles click actions on banners
    private val banners: List<FruitSlider>,     // List of banner items
    private val cdnURL: String                  // Base image CDN URL
) : RecyclerView.Adapter<NonScrollingBannerFruitsAdapter.BannerViewHolder>() {

    /**
     * ViewHolder class for holding and binding banner views.
     *
     * @property binding View binding for [ItemBannerDealBinding].
     */
    inner class BannerViewHolder(private val binding: ItemBannerDealBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the banner image and sets click behavior.
         *
         * @param imageRes Relative image path from the API.
         * @param position Position of the banner in the list.
         */
        fun bind(imageRes: String, position: Int) {
            // Load banner image with Glide
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes)   // Full CDN path
                .thumbnail(0.1f)           // Show low-res version while loading
                .dontAnimate()             // Disable animation for performance
                .into(binding.imageView)   // Display in ImageView

            // Handle image click
            binding.imageView.setOnClickListener {
                // Provide tactile feedback on tap
                binding.imageView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,               // Type of vibration feedback
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ensures feedback even if system setting is off
                )

                // Pass clicked banner details to listener
                listener.onUpdateFruitsBanner(
                    banners[position].main_mcat_id.toString(), // Main category ID
                    banners[position].mcat_id.toString(),      // Category ID
                    banners[position].msubcat_id.toString(),   // Subcategory ID
                    banners[position].mproduct_id.toString()   // Product ID
                )
            }
        }
    }

    /**
     * Creates a new [BannerViewHolder] when RecyclerView needs one.
     *
     * @param parent The parent ViewGroup.
     * @param viewType View type (not used here, single type).
     * @return A new [BannerViewHolder] instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        // Inflate layout with ViewBinding
        val binding = ItemBannerDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    /**
     * Binds data to a [BannerViewHolder] at the given position.
     *
     * @param holder The ViewHolder instance.
     * @param position The position in the list.
     */
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position].home_fruit_banner_image, position)
    }

    /**
     * Returns the total number of items in the list.
     */
    override fun getItemCount(): Int = banners.size
}