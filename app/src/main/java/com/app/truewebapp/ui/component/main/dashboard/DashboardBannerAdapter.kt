// Import required Android libraries and dependencies
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.data.dto.dashboard_banners.BigSlider
import com.app.truewebapp.databinding.ItemBannerBinding
import com.app.truewebapp.ui.component.main.dashboard.BigBannerListener
import com.bumptech.glide.Glide

/**
 * DashboardBannerAdapter is a RecyclerView adapter responsible
 * for displaying large banners on the dashboard screen.
 *
 * @param listener Callback listener to handle banner click events
 * @param banners List of BigSlider objects representing banner data
 * @param cdnURL Base CDN URL used to load banner images
 */
class DashboardBannerAdapter(
    private val listener: BigBannerListener,   // Listener for handling banner clicks
    private val banners: List<BigSlider>,      // List of banner data
    private val cdnURL: String,                // Base URL for banner images
) : RecyclerView.Adapter<DashboardBannerAdapter.BannerViewHolder>() {

    /**
     * ViewHolder class representing a single banner item in RecyclerView.
     *
     * @param binding ViewBinding object for the banner layout
     */
    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds banner data (image + click actions) to the item view.
         *
         * @param imageRes Banner image path from the API
         * @param position Position of the item in the list
         */
        fun bind(imageRes: String, position: Int) {
            // Load banner image using Glide with CDN URL prefix
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes) // Append CDN URL with image resource
                .thumbnail(0.1f)         // Load a low-resolution thumbnail first
                .dontAnimate()           // Disable animation for smoother loading
                .into(binding.imageView) // Set the loaded image into the ImageView

            // Handle click events on the banner image
            binding.imageView.setOnClickListener {
                // Trigger haptic feedback (vibration) for better UX
                binding.imageView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore global haptic settings
                )

                // Notify listener with IDs related to the clicked banner
                listener.onUpdateBigBanner(
                    banners[position].main_mcat_id.toString(),
                    banners[position].mcat_id.toString(),
                    banners[position].msubcat_id.toString(),
                    banners[position].mproduct_id.toString()
                )
            }
        }
    }

    /**
     * Inflates the banner layout and creates a ViewHolder.
     *
     * @param parent Parent ViewGroup where the ViewHolder will be attached
     * @param viewType Type of the view (not used here since all banners are same type)
     * @return A new BannerViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        // Inflate layout using ViewBinding
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    /**
     * Binds data to the ViewHolder at the given position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position Position of the item in the list
     */
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // Pass banner image and position to bind() method
        holder.bind(banners[position].home_large_banner_image, position)
    }

    /**
     * Returns the total number of banner items in the list.
     *
     * @return Number of banners
     */
    override fun getItemCount(): Int = banners.size
}