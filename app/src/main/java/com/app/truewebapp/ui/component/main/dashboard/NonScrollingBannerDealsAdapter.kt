
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.data.dto.dashboard_banners.DealSlider
import com.app.truewebapp.databinding.ItemBannerDealBinding
import com.app.truewebapp.ui.component.main.dashboard.DealsBannerListener
import com.bumptech.glide.Glide

class NonScrollingBannerDealsAdapter(
    private val listener: DealsBannerListener, private val banners: List<DealSlider>, private val cdnURL: String) : RecyclerView.Adapter<NonScrollingBannerDealsAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(private val binding: ItemBannerDealBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: String, position: Int) {
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes)
                .thumbnail(0.1f)
                .dontAnimate()
                .into(binding.imageView)
            binding.imageView.setOnClickListener {
                binding.imageView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Optional flag
                )
                listener.onUpdateDealsBanner(banners[position].main_mcat_id.toString(), banners[position].mcat_id.toString(), banners[position].msubcat_id.toString(), banners[position].mproduct_id.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position].home_explore_deal_banner_image, position)
    }

    override fun getItemCount(): Int = banners.size
}
